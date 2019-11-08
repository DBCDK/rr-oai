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

import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Startup
public class Worker {

    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    private static final String SETS_GONE = "UPDATE oairecordsets SET gone=TRUE WHERE pid=?";
    private static final String UPSERT_RECORD =
            "INSERT INTO oairecords(pid, deleted, changed)" +
            " values(?, ?, current_timestamp)" +
            " ON CONFLICT (pid)" +
            " DO UPDATE SET deleted = EXCLUDED.deleted, changed = EXCLUDED.changed";
    private static final String UPSERT_SETS =
            "INSERT INTO oairecordsets(pid, setspec, gone)" +
            " values(?, ?, FALSE)" +
            " ON CONFLICT (pid, setspec)" +
            " DO UPDATE SET setspec = EXCLUDED.setspec, gone = EXCLUDED.gone";

    @Inject
    public Config config;
    @Inject
    public JavaScriptPool js;

    @Inject
    public RawRepo rr;

    @Inject
    public Throttle throttle;

    @Resource(lookup = "jdbc/rr")
    DataSource rawRepo;

    @Resource(lookup = "jdbc/rroai")
    DataSource rawRepoOai;

    private final ConcurrentHashMap<Long, Thread> threads;

    private ExecutorService es;
    private boolean inBadState = false;

    public Worker() {
        this.threads = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        log.info("init");
        int threadCount = config.getThreads();
        log.debug("threadCount = {}", threadCount);
        es = Executors.newFixedThreadPool(threadCount);
        log.debug("threadCount = {}", threadCount);
        for (int i = 0 ; i < threadCount ; i++) {
            log.info("Adding runner: {}", i);
            es.submit(this::runner);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            threads.values().forEach(Thread::interrupt);
            es.shutdown();
            es.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Error while awaiting termination of executor service: {}", ex.getMessage());
            log.debug("Error while awaiting termination of executor service: ", ex);
        }
    }

    public boolean isInBadState() {
        return inBadState;
    }

    public void runner() {
        Thread me = Thread.currentThread();
        log.debug("me = {} ({})", me, me.getId());
        threads.put(me.getId(), me);
        try {
            for (;;) {
                connectLoop();
            }
        } catch (QueueException ex) {
            log.error("We got a RawRepoQueueDao problem - shutting down: {}", ex.getMessage());
            log.debug("We got a RawRepoQueueDao problem - shutting down: ", ex);
            inBadState = true;
        } catch (InterruptedException ex) {
            log.error("We are beeing shut down: {}", ex.getMessage());
            log.debug("We are beeing shut down: ", ex);
            inBadState = true;
        } catch (RuntimeException ex) {
            log.error("Exception: {}", ex.getMessage());
            log.debug("Exception: ", ex);
        } finally {
            threads.remove(me.getId());
        }
    }

    private void connectLoop() throws QueueException, InterruptedException {
        try (Connection rrConnection = rawRepo.getConnection() ;
             Connection rroaiConnection = rawRepoOai.getConnection()) {
            rrConnection.setAutoCommit(false);
            rroaiConnection.setAutoCommit(false);
            RawRepoQueueDAO dao = RawRepoQueueDAO.builder(rrConnection).build();
            processLoop(dao, rrConnection, rroaiConnection);
        } catch (SQLException ex) {
            log.error("Error connecting to database: {}", ex.getMessage());
            log.debug("Error connecting to database: ", ex);
            log.warn("Sleeping before trying again");
            throttle.failure();
            throttle.throttle();
        }
    }

    private void processLoop(RawRepoQueueDAO dao, Connection rrConnection, Connection rroaiConnection) throws InterruptedException {
        try {
            while (!inBadState) {
                QueueItem job = throttle.fetchJob(dao);
                if (job != null) {
                    int agencyId = job.getAgencyId();
                    String bibliographicRecordId = job.getBibliographicRecordId();
                    String pid = agencyId + ":" + bibliographicRecordId;
                    log.info("Processing pid: {}", pid);
                    RecordData recordData = rr.getContentFor(agencyId, bibliographicRecordId);
                    boolean deleted = recordData.isDeleted();
                    Set<String> sets = js.getOaiSets(agencyId, recordData.getContent());
                    setPidInDatabase(pid, deleted, sets, rroaiConnection);
                    rroaiConnection.commit();
                }
                rrConnection.commit();
            }
        } catch (QueueException ex) {
            log.error("We got a RawRepoQueueDao dequeue problem - disconnecting and trying again: {}", ex.getMessage());
            log.debug("We got a RawRepoQueueDao dequeue problem - disconnecting and trying again: ", ex);
        } catch (SQLException ex) {
            log.error("We got an sql problem disconnecting and trying again: {}", ex.getMessage());
            log.debug("We got an sql problem disconnecting and trying again: ", ex);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("We got a: {}", ex.getMessage());
            log.debug("We got a: ", ex);
        }
    }

    static void setPidInDatabase(String pid, boolean deleted, Collection<String> sets, Connection connection) throws SQLException {
        try (PreparedStatement setsGoneStmt = connection.prepareStatement(SETS_GONE) ;
             PreparedStatement recordsStmt = connection.prepareStatement(UPSERT_RECORD) ;
             PreparedStatement setsStmt = connection.prepareStatement(UPSERT_SETS)) {
            setsGoneStmt.setString(1, pid);
            setsGoneStmt.executeUpdate();
            recordsStmt.setString(1, pid);
            recordsStmt.setBoolean(2, deleted);
            recordsStmt.executeUpdate();
            setsStmt.setString(1, pid);
            for (String set : sets) {
                setsStmt.setString(2, set.toLowerCase(Locale.ROOT));
                setsStmt.executeUpdate();
            }
        }
    }

}
