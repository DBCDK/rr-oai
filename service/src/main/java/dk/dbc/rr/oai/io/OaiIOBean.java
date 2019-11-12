/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part oaiResponseOf rr-oai-service
 *
 * rr-oai-service is free software: you can redistribute it and/or modify
 * it under the terms oaiResponseOf the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 oaiResponseOf the License, or
 * (at your option) any later version.
 *
 * rr-oai-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty oaiResponseOf
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy oaiResponseOf the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.io;

import dk.dbc.oai.pmh.OAIPMH;
import dk.dbc.rr.oai.Config;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import static dk.dbc.rr.oai.io.OaiResponse.O;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
public class OaiIOBean {

    @Inject
    public Config config;

    /**
     * Create an response prepared for sending to the client
     *
     * @param baseUrl           The request URI
     * @param requestParameters a multi valued map as provided by
     *                          {@link UriInfo}
     * @return Newly constructed OaiResponse
     */
    public OaiResponse oaiResponseOf(String baseUrl, MultivaluedMap<String, String> requestParameters) {
        OAIPMH oaipmh = O.createOAIPMH();
        OaiRequest request = oaiRequestOf(oaipmh, requestParameters);
        return new OaiResponse(baseUrl, request, requestParameters, oaipmh);
    }

    public OaiRequest oaiRequestOf(OAIPMH oaipmh, MultivaluedMap<String, String> map) {
        return new OaiRequest.Parser(oaipmh)
                .parse(map)
                .parseResumptionToken(this::resumptionTokenOf)
                .validateArguments()
                .asOaiRequest();
    }

    public OaiResumptionToken resumptionTokenOf(String content) {
        return OaiResumptionToken.of(content, config.getXorBytes());
    }

}
