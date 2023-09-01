package dk.dbc.rr.oai.setmatcher;

import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.queue.QueueItem;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.sql.DataSource;

public class WorkerTask implements Callable<QueueItem> {

    private static final Logger log = LoggerFactory.getLogger(WorkerTask.class);

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

    private final QueueItem job;
    private final RawRepo rr;
    private final DataSource rawRepoOai;
    private final JavaScriptPool js;
    private final SimpleTimer timer;

    public WorkerTask(QueueItem job, RawRepo rr, DataSource rawRepoOai, JavaScriptPool js, SimpleTimer timer) {
        this.job = job;
        this.rr = rr;
        this.rawRepoOai = rawRepoOai;
        this.js = js;
        this.timer = timer;
    }

    @Override
    public QueueItem call() throws Exception {
        try (SimpleTimer.Context timed = timer.time()) {
            int agencyId = job.getAgencyId();
            String bibliographicRecordId = job.getBibliographicRecordId();
            String pid = agencyId + "-" + bibliographicRecordId;
            if (!js.isEligible(agencyId)) {
                log.info("Skipping pid: {} (not eligible)", pid);
                return job;
            }
            log.info("Processing pid: {}", pid);
            RecordDTO recordData = rr.getContentFor(agencyId, bibliographicRecordId);
            boolean deleted = recordData.isDeleted();
            Set<String> sets = js.getOaiSets(agencyId, recordData.getContent());
            setPidInDatabase(pid, deleted, sets);
            return job;
        }
    }

    /**
     * Update OAI database
     *
     * @param pid     identifier
     * @param deleted is the record is deleted
     * @param sets    which sets it is contained in
     * @throws SQLException If there's problems communicating with the database
     */
    public void setPidInDatabase(String pid, boolean deleted, Collection<String> sets) throws SQLException {
        try (Connection connection = rawRepoOai.getConnection();
             PreparedStatement setsGoneStmt = connection.prepareStatement(SETS_GONE);
             PreparedStatement recordsStmt = connection.prepareStatement(UPSERT_RECORD);
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
