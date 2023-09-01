package dk.dbc.rr.oai.setmatcher;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enterprise Java Bean responsible for activating workers at scheduled intervals
 */
@Singleton
@Startup
@Liveness
public class WorkerScheduler implements HealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerScheduler.class);
    private static final String HEALTH_CHECK_CONSECUTIVE_SERVER_ERRORS = "consecutive server errors";

    @Inject
    Config config;

    @Inject
    MetricRegistry metricRegistry;

    @EJB
    Worker worker;

    @Resource(lookup = "java:comp/DefaultManagedScheduledExecutorService")
    ManagedScheduledExecutorService ses;

    private ConcurrentGauge consecutiveServerErrorsGauge;
    private Counter serverErrorsCounter;

    @PostConstruct
    public void init() {
        consecutiveServerErrorsGauge = metricRegistry.concurrentGauge("consecutive_server_errors");
        serverErrorsCounter = metricRegistry.counter("server_errors");
        ses.scheduleAtFixedRate(this::activateWorker, 0, config.getPollRate(), TimeUnit.SECONDS);
    }

    @Lock(LockType.READ)
    public void activateWorker() {
        try {
            while (true) {
                LOGGER.info("activating worker");

                final int jobsProcessed = worker.processJobs();

                if (jobsProcessed < config.getMaxBatchSize()) {
                    /* Job queue is empty, so we throttle ourselves by not
                       activating another worker until the next scheduled execution */
                    break;
                }
                // else do another iteration until job queue is empty
            }

            // hmm, a gauge reset method would be nice
            while (consecutiveServerErrorsGauge.getCount() > 0) {
                consecutiveServerErrorsGauge.dec();
            }

        } catch (InterruptedException | RuntimeException e) {
            consecutiveServerErrorsGauge.inc();
            serverErrorsCounter.inc();

            LOGGER.error("unhandled exception caught by scheduler", e);
        }
    }

    @Override
    @Lock(LockType.READ)
    public HealthCheckResponse call() {
        if (consecutiveServerErrorsGauge.getCount() < config.getMaxConsecutiveServerErrors()) {
            return HealthCheckResponse.up(HEALTH_CHECK_CONSECUTIVE_SERVER_ERRORS);
        }
        return HealthCheckResponse.down(HEALTH_CHECK_CONSECUTIVE_SERVER_ERRORS);
    }
}
