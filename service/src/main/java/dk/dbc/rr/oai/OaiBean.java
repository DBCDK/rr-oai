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

import dk.dbc.rr.oai.fetch.forsrights.ForsRights;
import dk.dbc.rr.oai.io.OaiIOBean;
import dk.dbc.rr.oai.io.OaiRequest;
import dk.dbc.rr.oai.io.OaiResponse;
import dk.dbc.rr.oai.worker.OaiWorker;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.*;
import static javax.ws.rs.core.Response.Status.*;

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
    public ForsRights forsRights;

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
                .entity(processOaiRequest(allowedSets, params))
                .build();
    }

    /**
     * Create xml document from request parameters
     *
     * @param allowedSets This sets the user has access to
     * @param params      The request params
     * @return XML bytes
     */
    byte[] processOaiRequest(Set<String> allowedSets, MultivaluedMap<String, String> params) {

        OaiResponse response = oaiIO.oaiResponseOf(config.getExposedUrl(), params);
        OaiRequest request = response.getRequest();
        try {
            if (request != null) {
                log.debug("request.getVerb = {}", request.getVerb());
                switch (request.getVerb()) {
                    case GET_RECORD:
                        oaiWorker.getRecord(response, request, allowedSets);
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
                        oaiWorker.listRecords(response, request, allowedSets);
                        break;
                    case LIST_SETS:
                        oaiWorker.listSets(response);
                        break;
                    default:
                        throw new ServerErrorException("Verb not implemented", INTERNAL_SERVER_ERROR);
                }
            }
        } catch (SQLException ex) {
            log.error("Error communicating with the database: {}", ex.getMessage());
            log.debug("Error communicating with the database: ", ex);
            throw new ServerErrorException(INTERNAL_SERVER_ERROR);
        }
        return response.content();
    }

    /**
     * Look up using ForsRights the allowed sets for the current user
     * <p>
     * This returns an empty set if a login has been presented, but fails.
     * If no triple has ben supplied then the default set will be returned.
     *
     * @param triple   optional user:group:password
     * @param clientIp remote ip
     * @return collection of setspec names
     * @throws ServerErrorException If ForsRights could not be contacted
     */
    Set<String> getAllowedSets(String triple, String clientIp) throws ServerErrorException {
        Set<String> allowedSets = EMPTY_SET;
        if (config.isAuthenticationDisabled()) {
            allowedSets = config.getAllForsRightsSets();
        } else {
            try {
                // This is returns empty if invalid login and no ip-based access
                allowedSets = forsRights.authorized(triple, clientIp);
            } catch (IOException | WebApplicationException ex) {
                log.error("Error validating user: {}", ex.getMessage());
                log.debug("Error validating user: ", ex);
                throw new ServerErrorException("Error validating user", INTERNAL_SERVER_ERROR);
            }
        }
        // Anonymous access (no login/ip-based access)
        if (allowedSets.isEmpty() && triple == null)
            allowedSets = new HashSet<>(config.getForsRightsRules()
                    .getOrDefault("*", EMPTY_LIST));
        return allowedSets;
    }
}
