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
package dk.dbc.rr.oai.io;

import dk.dbc.oai.pmh.OAIPMH;
import dk.dbc.oai.pmh.ResumptionTokenType;
import dk.dbc.rr.oai.Config;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

import static dk.dbc.rr.oai.io.OaiResponse.O;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
public class OaiIOBean {

    private static final Logger log = LoggerFactory.getLogger(OaiIOBean.class);

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

    private OaiRequest oaiRequestOf(OAIPMH oaipmh, MultivaluedMap<String, String> map) {
        return new OaiRequest.Parser(oaipmh)
                .parse(map)
                .parseResumptionToken(this::resumptionTokenOf)
                .validateArguments()
                .asOaiRequest();
    }

    public OaiResumptionToken resumptionTokenOf(String content) {
        return OaiResumptionToken.of(content, config.getXorBytes());
    }

    /**
     * Build a new resumption token for adding to response
     *
     * @param from       When the harvesting should start
     * @param identifier Identifier to continue from (first in response not
     *                   supplied to user)
     * @param until      When the harvesting should stop
     * @param set        set to harvest from
     * @return XmlNode
     */
    public ResumptionTokenType resumptionTokenFor(OaiTimestamp from, OaiIdentifier identifier, OaiTimestamp until, String set) {
        try {
            OaiResumptionToken token = new OaiResumptionToken(from, identifier, until, set);
            long ttl = config.getResumptionTokenTimeoutInSeconds();
            return token.toXML(Instant.now().plusSeconds(ttl),
                               config.getXorBytes());
        } catch (IOException ex) {
            log.error("Error building resumption token: {}", ex.getMessage());
            log.debug("Error building resumption token: ", ex);
            throw new ServerErrorException("Cannot generate resumptionToken", INTERNAL_SERVER_ERROR);
        }
    }

}
