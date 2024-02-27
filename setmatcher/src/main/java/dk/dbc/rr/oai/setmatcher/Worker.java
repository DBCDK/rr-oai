package dk.dbc.rr.oai.setmatcher;

import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Worker class responsible for processing queued rawrepo jobs
 */
@Singleton
public class Worker {

    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    @Inject
    public Config config;

    @Inject
    public JavaScriptPool js;

    @Inject
    public RawRepo rr;

    @Inject
    MetricRegistry metricRegistry;

    @Resource(lookup = "jdbc/rawrepo")
    DataSource rawRepo;

    @Resource(lookup = "jdbc/rawrepo-oai")
    DataSource rawRepoOai;

    @Resource(lookup = "java:comp/DefaultManagedExecutorService", type = ManagedExecutorService.class)
    ExecutorService executor;

    private Counter rawrepoRecordServiceErrorsCounter;
    private Timer workerTaskDurationTimer;

    @PostConstruct
    public void init() {
        rawrepoRecordServiceErrorsCounter = metricRegistry.counter("rawrepo_record_service_errors");
        workerTaskDurationTimer = metricRegistry.timer("worker_task");
    }

    /**
     * Processes a number of jobs up to at most the configured batch size.
     * Returns if the queue becomes empty before the batch size is fulfilled.
     * Runs in its own transactional scope to be able to commit in batches.
     *
     * @return number of jobs processed
     * @throws IllegalStateException on failures which must trigger a rollback
     * @throws InterruptedException  if interrupted while waiting for unfinished tasks
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int processJobs() throws IllegalStateException, InterruptedException {
        int numberOfJobsCompleted = 0;

        while (numberOfJobsCompleted < config.getMaxBatchSize()) {
            // Get a number of jobs corresponding (at most) to the configured number of threads
            final int batchSizeDelta = config.getMaxBatchSize() - numberOfJobsCompleted;
            final int atMostNumberOfJobs = batchSizeDelta > config.getThreads() ? config.getThreads() : batchSizeDelta;
            final List<QueueItem> jobs = dequeue(atMostNumberOfJobs);
            if (jobs.isEmpty()) {
                // Queue is empty...
                break;
            }

            // Create worker tasks from jobs and execute them concurrently
            final List<WorkerTask> tasks = new ArrayList<>(config.getThreads());
            for (QueueItem job : jobs) {
                tasks.add(new WorkerTask(job, rr, rawRepoOai, js, workerTaskDurationTimer));
            }
            final List<Future<QueueItem>> jobFutures = executor.invokeAll(tasks);

            /* Since invokeAll is guaranteed to return its futures in the same sequential
               order as produced by the iterator for the given task list, we can use a
               simple index counter to get the original job in case of error. */
            int i = 0;
            for (Future<QueueItem> job : jobFutures) {
                try {
                    // Wait for task completion
                    job.get(config.getMaxProcessingTime(), TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof RecordServiceConnectorException) {
                        /* Something went wrong when fetching the record.
                           Odds are that this type of error won't benefit
                           from rollback/retry, so we simply fail it. */
                        log.error("failing job {}", jobs.get(i), e);
                        rawrepoRecordServiceErrorsCounter.inc();
                    } else {
                        throw new IllegalStateException(cause);
                    }
                } catch (TimeoutException e) {
                    throw new IllegalStateException(e);
                }
                i++;
            }

            numberOfJobsCompleted += jobFutures.size();
        }

        return numberOfJobsCompleted;
    }

    private List<QueueItem> dequeue(int atMostNumberOfJobs) {
        try (Connection connection = rawRepo.getConnection()) {
            final RawRepoQueueDAO queueDAO = RawRepoQueueDAO.builder(connection).build();
            final List<QueueItem> jobs = new ArrayList<>(atMostNumberOfJobs);
            do {
                final QueueItem job = queueDAO.dequeue(config.getQueueName());
                if (job == null) {
                    // queue is empty
                    break;
                }
                jobs.add(job);
            } while (jobs.size() < atMostNumberOfJobs);

            return jobs;
        } catch (SQLException | QueueException e) {
            throw new IllegalStateException("error dequeueing job", e);
        }
    }

    @PreDestroy
    public void destroy() {
    }

}
