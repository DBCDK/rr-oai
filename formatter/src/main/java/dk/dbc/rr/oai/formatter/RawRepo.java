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
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.rawrepo.RecordServiceConnectorNoContentStatusCodeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;

import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        final RetryPolicy rp = new RetryPolicy()
                .retryOn(Collections.singletonList(ProcessingException.class))
                .retryIf((Response r) -> r.getStatus() == 500 || r.getStatus() == 404 || r.getStatus() == 502)
                .withDelay(2, TimeUnit.SECONDS)
                .withMaxRetries(1);
        final FailSafeHttpClient fsc = FailSafeHttpClient.create(config.getHttpClient(), rp);
        this.connector = new RecordServiceConnector(fsc, config.getRawrepoRecordService(), connectorLogLevel);
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
        log.debug("Wrapping id {}", id);
        return new MarcXChangeWrapper(getDataOf(id), getChildrenOf(id));
    }

    private String getDataOf(RecordId id) {
        try {
            byte[] content = connector.getRecordContent(id.getAgencyId(), id.getBibliographicRecordId(), PARAMS);
            return new String(content, UTF_8);
        } catch (RecordServiceConnectorNoContentStatusCodeException ex) {
            log.error("Error getting data of: {} no content: {}", id, ex.getMessage());
            log.debug("Error getting data of: {} no content: ", id, ex);
            throw new InternalServerErrorException();
        } catch (RecordServiceConnectorException ex) {
            log.error("Error getting data of: {}: {}", id, ex.getMessage());
            log.debug("Error getting data of: {}: ", id, ex);
            throw new InternalServerErrorException();
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
            throw new InternalServerErrorException();
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
            log.debug("Get Parent Of: {}", id);
            RecordId[] recordParents = connector.getRecordParents(id.getAgencyId(), id.getBibliographicRecordId());
            log.debug("Got Parent Of: {}", id);
            return Arrays.stream(recordParents)
                    .filter(r -> r.getAgencyId() == COMMON_AGENCY)
                    .findFirst()
                    .orElse(null);
        } catch (RecordServiceConnectorException ex) {
            log.error("Error getting parent of: {}: {}", id, ex.getMessage());
            log.debug("Error getting parent of: {}: ", id, ex);
            throw new InternalServerErrorException();
        }
    }
}
