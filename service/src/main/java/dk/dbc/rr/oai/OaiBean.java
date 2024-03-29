/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-service
 *
 * rr-oai-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai;

import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.log.LogWith;
import dk.dbc.oai.pmh.VerbType;
import dk.dbc.rr.oai.fetch.IdpRights;
import dk.dbc.rr.oai.io.OaiIOBean;
import dk.dbc.rr.oai.io.OaiRequest;
import dk.dbc.rr.oai.io.OaiResponse;
import dk.dbc.rr.oai.worker.OaiWorker;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static java.util.Collections.EMPTY_SET;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
@Path("oai")
public class OaiBean {

    private static final Logger log = LoggerFactory.getLogger(OaiBean.class);

    @Inject
    public Config config;

    @Inject
    public IdpRights idpRights;

    @Inject
    public IndexHtml indexHtml;

    @Inject
    public OaiIOBean oaiIO;

    @Inject
    public OaiWorker oaiWorker;

    @Inject
    public RemoteIp remoteIp;

    @GET
    @Timed
    public Response oai(@Context UriInfo uriInfo,
                        @Context HttpServletRequest httpRequest,
                        @Context HttpHeaders headers) {

        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        if (params.isEmpty()) {
            log.info("index.html");
            return Response.ok()
                    .type(MediaType.TEXT_HTML_TYPE)
                    .entity(indexHtml.getInputStream())
                    .build();
        }

        String trackingId = params.getFirst("trackingId");
        if (trackingId == null)
            trackingId = UUID.randomUUID().toString();

        try ( LogWith logWith = LogWith.track(trackingId)) {
            String triple = params.getFirst("identity");
            if (triple == null || triple.split(":", 3).length != 3)
                triple = headers.getHeaderString("Identity");
            String clientIp = remoteIp.clientIp(httpRequest.getRemoteAddr(),
                                                headers.getHeaderString("X-Forwarded-For"));
            log.trace("triple = {}; clientIp = {}", triple, clientIp);
            Set<String> allowedSets = getAllowedSets(triple, clientIp);

            if (allowedSets.isEmpty())
                throw new ClientErrorException(UNAUTHORIZED);

            return Response.ok()
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .entity(processOaiRequest(allowedSets, params, trackingId))
                    .build();
        }
    }

    /**
     * Create xml document from request parameters
     *
     * @param allowedSets This sets the user has access to
     * @param params      The request params
     * @param trackingId  The tracking id (to be used in different threads)
     * @return XML bytes
     */
    byte[] processOaiRequest(Set<String> allowedSets, MultivaluedMap<String, String> params, String trackingId) {

        OaiResponse response = oaiIO.oaiResponseOf(config.getExposedUrl(), params);
        OaiRequest request = response.getRequest();
        VerbType verb = request.getVerb();
        if (verb != null) {
            try ( DBCTrackedLogContext logWith = LogWith.track(trackingId)
                    .with("verb", verb.toString())) {
                switch (verb) {
                    case GET_RECORD:
                        oaiWorker.getRecord(response, request, allowedSets, trackingId);
                        break;
                    case IDENTIFY:
                        oaiWorker.identify(response);
                        break;
                    case LIST_IDENTIFIERS:
                        oaiWorker.listIdentifiers(response, request, allowedSets);
                        break;
                    case LIST_METADATA_FORMATS:
                        oaiWorker.listMetadataFormats(response, request, allowedSets);
                        break;
                    case LIST_RECORDS:
                        oaiWorker.listRecords(response, request, allowedSets, trackingId);
                        break;
                    case LIST_SETS:
                        oaiWorker.listSets(response);
                        break;
                    default:
                        throw new ServerErrorException("Verb not implemented", INTERNAL_SERVER_ERROR);
                }
            } catch (SQLException ex) {
                log.error("Error communicating with the database: {}", ex.getMessage());
                log.debug("Error communicating with the database: ", ex);
                throw new ServerErrorException(INTERNAL_SERVER_ERROR);
            }
        }
        String comment = new StringBuilder()
                .append(" trackingId: ")
                .append(trackingId)
                .append(' ').toString();
        return response.content(comment);
    }

    /**
     * Look up using IdpRights the allowed sets for the current user
     * <p>
     * This returns an empty set if a login has been presented, but fails.
     * If no triple has ben supplied then the default set will be returned.
     *
     * @param triple   optional user:group:password
     * @param clientIp remote ip
     * @return collection of setspec names
     * @throws ServerErrorException If IdpRights could not be contacted
     */
    Set<String> getAllowedSets(String triple, String clientIp) throws ServerErrorException {
        Set<String> allowedSets = EMPTY_SET;
        if (config.isAuthenticationDisabled()) {
            allowedSets = config.getAllIdpRightsSets();
            log.debug("allowedSets = {} (default)", allowedSets);
        } else {
            try {
                allowedSets = idpRights.authorized(triple, clientIp);
                log.debug("allowedSets = {}", allowedSets);
            } catch (IOException | WebApplicationException ex) {
                log.error("Error validating user: {}", ex.getMessage());
                log.debug("Error validating user: ", ex);
                throw new ServerErrorException("Error validating user", INTERNAL_SERVER_ERROR);
            }
        }
        return allowedSets;
    }
}
