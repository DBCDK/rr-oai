/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-setmatcher
 *
 * rr-oai-setmatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-setmatcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.setmatcher;

import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
public class RawRepo {

    private static final Logger log = LoggerFactory.getLogger(RawRepo.class);

    @Inject
    public Config config;

    private static final RecordServiceConnector.Params PARAMS = new RecordServiceConnector.Params()
            .withAllowDeleted(true)
            .withExcludeAutRecords(true)
            .withExcludeDbcFields(true)
            .withExpand(true)
            .withKeepAutFields(false)
            .withMode(RecordServiceConnector.Params.Mode.RAW)
            .withUseParentAgency(false);

    RecordServiceConnector connector;

    @PostConstruct
    public void init() {
        RecordServiceConnector.TimingLogLevel connectorLogLevel =
                log.isDebugEnabled() ?
                RecordServiceConnector.TimingLogLevel.DEBUG :
                RecordServiceConnector.TimingLogLevel.INFO;
        this.connector = new RecordServiceConnector(
                config.getHttpClient(),
                config.getRawrepoRecordService(), connectorLogLevel);
    }

    @PreDestroy
    public void close() {
        connector.close();
    }

    /**
     * Extract record content from service
     *
     * @param agencyId              Library Number
     * @param bibliographicRecordId Record id
     * @return content object (including deleted status)
     * @throws RecordServiceConnectorException In case the communication fails
     */
    public RecordDTO getContentFor(int agencyId, String bibliographicRecordId) throws RecordServiceConnectorException {
        return connector.getRecordData(agencyId, bibliographicRecordId, PARAMS);
    }

    /**
     * Check if connection is good (record service responds)
     *
     * @return if record-service is up
     */
    public boolean ping() {
        try {
            connector.recordExists("999999", "health-check");
            return true;
        } catch (RecordServiceConnectorException ex) {
            return false;
        }
    }

}
