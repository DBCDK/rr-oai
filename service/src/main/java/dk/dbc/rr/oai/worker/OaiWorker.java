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
package dk.dbc.rr.oai.worker;

import dk.dbc.oai.pmh.DeletedRecordType;
import dk.dbc.oai.pmh.GranularityType;
import dk.dbc.oai.pmh.IdentifyType;
import dk.dbc.oai.pmh.MetadataFormatType;
import dk.dbc.oai.pmh.OAIPMHerrorcodeType;
import dk.dbc.rr.oai.Config;
import dk.dbc.rr.oai.io.OaiRequest;
import dk.dbc.rr.oai.io.OaiResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.rr.oai.io.OaiResponse.O;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class OaiWorker {

    private static final Logger log = LoggerFactory.getLogger(OaiWorker.class);

    @Inject
    public Config config;

    @Inject
    public OaiDatabaseFormats oaiDatabaseFormats;

    @Inject
    public OaiDatabaseWorker oaiDatabaseWorker;

    public void identify(OaiResponse response) {

        IdentifyType identify = response.identify();
        identify.setRepositoryName(config.getRepoName());
        identify.setBaseURL(config.getExposedUrl());
        identify.setProtocolVersion("2.0");
        identify.getAdminEmails().add(config.getAdminEmail());
        identify.setEarliestDatestamp("1970-01-01T00:00:00Z"); // Epoch
        identify.setDeletedRecord(DeletedRecordType.TRANSIENT); // Cannot guarantee against database wipes
        identify.setGranularity(GranularityType.YYYY_MM_DD_THH_MM_SS_Z);

    }

    public void listMetadataFormats(OaiResponse response, OaiRequest request, Set<String> allowedSets) throws SQLException {
        log.info("listMetadataFormats");
        if (request.getIdentifier() != null) {
            Set<String> sets = oaiDatabaseWorker.getSetsForId(request.getIdentifier());
            sets.retainAll(allowedSets);
            if (sets.isEmpty()) {
                response.error(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "No such record");
                return;
            }
        }
        List<MetadataFormatType> metadataFormats = response.listMetadataFormats()
                .getMetadataFormats();
        oaiDatabaseFormats.getFormats()
                .forEach(format -> {
                    MetadataFormatType xml = O.createMetadataFormatType();
                    xml.setMetadataPrefix(format.getPrefix());
                    xml.setSchema(format.getSchema());
                    xml.setMetadataNamespace(format.getNamespace());
                    metadataFormats.add(xml);
                });
    }

}
