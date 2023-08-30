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
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Startup
public class Worker {

    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    private static final String SETS_GONE = "UPDATE oairecordsets SET gone=TRUE, changed=CURRENT_TIMESTAMP WHERE pid=? AND NOT gone";
    private static final String UPSERT_RECORD =
            "INSERT INTO oairecords(pid, deleted)" +
            " values(?, ?)" +
            " ON CONFLICT (pid)" +
            " DO UPDATE SET deleted = EXCLUDED.deleted";
    private static final String UPSERT_SETS =
            "INSERT INTO oairecordsets(pid, setspec, changed, gone)" +
            " values(?, ?, CURRENT_TIMESTAMP, FALSE)" +
            " ON CONFLICT (pid, setspec)" +
            " DO UPDATE SET changed = EXCLUDED.changed, setspec = EXCLUDED.setspec, gone = EXCLUDED.gone";

    @Inject
    public Config config;

    @Inject
    public JavaScriptPool js;

    @Inject
    public RawRepo rr;

    @Inject
    public Throttle throttle;

    private AtomicLong lastDequeue = new AtomicLong(System.currentTimeMillis());

    @Resource(lookup = "jdbc/rawrepo")
    DataSource rawRepo;

    @Resource(lookup = "jdbc/rawrepo-oai")
    DataSource rawRepoOai;

    private final ConcurrentHashMap<Long, Thread> threads;

    private ExecutorService es;
    private boolean inBadState = false;

    public Worker() {
        this.threads = new ConcurrentHashMap<>();
    }

    /**
     * Start n threads for harvesting
     */
    @PostConstruct
    public void init() {
        log.info("init");
        int threadCount = config.getThreads();
        es = Executors.newFixedThreadPool(threadCount);
        for (int i = 0 ; i < threadCount ; i++) {
            es.submit(this::runner);
        }
    }

    /**
     * Interrupt the threads running
     * <p>
     * Then wait an adequate amount of time, for everything to complete
     */
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

    /**
     * Has anything reported we're in a state we cant quite understand
     *
     * @return is status is "not ok"
     */
    public boolean isInBadState() {
        return inBadState;
    }

    /**
     * Determine if queue is stalled
     *
     * @return if queue is stalled, and service should answer "liveness-failed"
     */
    public boolean queueStalled() {
        int queueStalledAfter = config.getQueueStalledAfter();
        if (System.currentTimeMillis() - lastDequeue.get() < queueStalledAfter * 1_000L) {
            return false; // dequeued within the last n sec.
        }
        try (Connection connection = rawRepo.getConnection() ;
             PreparedStatement stmt = connection.prepareStatement("SELECT EXTRACT('EPOCH' FROM NOW() - queued)::INT FROM queue WHERE worker = ? ORDER BY queued LIMIT 1")) {
            stmt.setString(1, config.getQueueName());
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    int seconds = resultSet.getInt(1);
                    if (seconds > queueStalledAfter) {
                        log.warn("Queue stalled for: {} seconds", seconds);
                        return true; // Nothing dequeued within last n sec, and oldest id more that n sec old
                    }
                } else { // Queue is empty, reset check time
                    lastDequeue.set(System.currentTimeMillis());
                }
            }
            return false;
        } catch (SQLException ex) {
            log.error("Error querying rawrepo oldest queue entry: {}", ex.getMessage());
            log.debug("Error querying rawrepo oldest queue entry: ", ex);
            return true; // Queue is probably bad.
        }
    }

    /**
     * The thread function
     * <p>
     * Registers thread for terminating
     * and loops until {@link #connectLoop()} fails with an exception
     */
    private void runner() {
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

    /**
     * This connects to the databases and tries to process data
     *
     * @throws QueueException       In case the newly created connection to the
     *                              RawRepo database is somehow bad
     * @throws InterruptedException If were shutting down
     */
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

    /**
     * Tries to process as many records as possible
     *
     * @param dao             Queue interface
     * @param rrConnection    connection behind dao (for transaction control)
     * @param rroaiConnection connection to OAI database
     * @throws InterruptedException If were shutting down
     * @throws QueueException       If a job failed and cannot reported to the
     *                              queue
     */
    private void processLoop(RawRepoQueueDAO dao, Connection rrConnection, Connection rroaiConnection) throws InterruptedException, QueueException {
        QueueItem job = null;
        try {
            while (!inBadState) {
                job = throttle.fetchJob(dao, rrConnection);
                if (job != null) {
                    lastDequeue.set(System.currentTimeMillis());
                    int agencyId = job.getAgencyId();
                    String bibliographicRecordId = job.getBibliographicRecordId();
                    String pid = agencyId + "-" + bibliographicRecordId;
                    if (!js.isEligible(agencyId)) {
                        log.info("Skipping pid: {} (not eligible)", pid);
                        continue;
                    }
                    log.info("Processing pid: {}", pid);
                    RecordDTO recordData = rr.getContentFor(agencyId, bibliographicRecordId);
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
            log.error("We got a failing item: {}", ex.getMessage());
            log.debug("We got a failing item: ", ex);
            if (job != null)
                dao.queueFail(job, ex.getMessage());
        }
    }

    /**
     * Update OAI database
     *
     * @param pid        identifier
     * @param deleted    is the record is deleted
     * @param sets       which sets it is contained in
     * @param connection the database connection
     * @throws SQLException If there's problems communicating with the database
     */
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
