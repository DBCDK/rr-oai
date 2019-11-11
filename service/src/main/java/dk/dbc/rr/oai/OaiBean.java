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
import java.io.IOException;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.EMPTY_SET;
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
    public RemoteIp remoteIp;

    @Inject
    public ForsRights forsRights;

    @Inject
    public IndexHtml indexHtml;

    @GET
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

        return Response.ok()
                .type(MediaType.APPLICATION_XML_TYPE)
                .entity(processOaiRequest(triple, clientIp, params))
                .build();
    }

    byte[] processOaiRequest(String triple, String clientIp, MultivaluedMap<String, String> params) {
        Set<String> allowedSets = getAllowedSets(triple, clientIp);

        if (allowedSets.isEmpty())
            throw new ServerErrorException(UNAUTHORIZED);

        throw new ServerErrorException("Not implemented yet!", INTERNAL_SERVER_ERROR);
    }

    Set<String> getAllowedSets(String triple, String clientIp) throws ServerErrorException {
        Set<String> allowedSets = EMPTY_SET;
        if (config.isAuthenticationDisabled()) {
            allowedSets = config.getAllForsRightsSets();
        } else {
            try {
                allowedSets = forsRights.authorized(triple, clientIp);
            } catch (IOException | WebApplicationException ex) {
                log.error("Error validating user: {}", ex.getMessage());
                log.debug("Error validating user: ", ex);
                throw new ServerErrorException(INTERNAL_SERVER_ERROR);
            }
        }
        return allowedSets;
    }
}
