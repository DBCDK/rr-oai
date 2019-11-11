/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-formatter
 *
 * rr-oai-formatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-formatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.formatter;

import dk.dbc.formatter.js.MarcXChangeWrapper;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.ws.rs.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * Bean for communicating with RawRepo Record Service
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Startup
public class RawRepo {

    private static final Logger log = LoggerFactory.getLogger(RawRepo.class);

    private static final int COMMON_AGENCY = 870970;

    @Inject
    Config config;

    private static final RecordServiceConnector.Params PARAMS = new RecordServiceConnector.Params()
            .withAllowDeleted(true)
            .withExcludeAutRecords(true)
            .withExcludeDbcFields(true)
            .withExpand(true)
            .withKeepAutFields(false)
            .withMode(RecordServiceConnector.Params.Mode.EXPANDED)
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
     * Create a list of the record given and all it's parents
     *
     * @param agencyId              record identifier
     * @param bibliographicRecordId record identifier
     * @return array of records
     * @throws ServerErrorException if the record cannot be fetched
     */
    public MarcXChangeWrapper[] getRecordsFor(int agencyId, String bibliographicRecordId) {
        ArrayList<RecordId> recordIds = new ArrayList<>(3); // At most volume, section and head

        RecordId id = new RecordId(bibliographicRecordId, agencyId);
        while (id != null) {
            recordIds.add(id);
            id = getParentOf(id);
        }

        return recordIds.stream()
                .map(this::marcXChangeWrapper)
                .toArray(MarcXChangeWrapper[]::new);
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

    private MarcXChangeWrapper marcXChangeWrapper(RecordId id) {
        return new MarcXChangeWrapper(getDataOf(id), getChildrenOf(id));
    }

    private String getDataOf(RecordId id) {
        try {
            byte[] content = connector.getRecordContent(id.getAgencyId(), id.getBibliographicRecordId(), PARAMS);
            return new String(content, UTF_8);
        } catch (RecordServiceConnectorException ex) {
            log.error("Error getting children of: {}: {}", id, ex.getMessage());
            log.debug("Error getting children of: {}: ", id, ex);
            throw new ServerErrorException(INTERNAL_SERVER_ERROR);
        }
    }

    private RecordId[] getChildrenOf(RecordId id) {
        try {
            RecordId[] recordParents = connector.getRecordChildren(id.getAgencyId(), id.getBibliographicRecordId());
            return Arrays.stream(recordParents)
                    .filter(r -> r.getAgencyId() == COMMON_AGENCY)
                    .toArray(RecordId[]::new);
        } catch (RecordServiceConnectorException ex) {
            log.error("Error getting children of: {}: {}", id, ex.getMessage());
            log.debug("Error getting children of: {}: ", id, ex);
            throw new ServerErrorException(INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get parent record for fetching section/head
     *
     * @param id recordId
     * @return null if no parent or parent id
     */
    private RecordId getParentOf(RecordId id) {
        try {
            RecordId[] recordParents = connector.getRecordParents(id.getAgencyId(), id.getBibliographicRecordId());
            return Arrays.stream(recordParents)
                    .filter(r -> r.getAgencyId() == COMMON_AGENCY)
                    .findFirst()
                    .orElse(null);
        } catch (RecordServiceConnectorException ex) {
            log.error("Error getting children of: {}: {}", id, ex.getMessage());
            log.debug("Error getting children of: {}: ", id, ex);
            throw new ServerErrorException(INTERNAL_SERVER_ERROR);
        }
    }
}
