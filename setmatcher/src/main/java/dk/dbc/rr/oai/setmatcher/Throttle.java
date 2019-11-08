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

import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Lock(LockType.READ)
public class Throttle {

    private static final Logger log = LoggerFactory.getLogger(Throttle.class);

    @Inject
    public Config config;

    private final ReentrantLock lock = new ReentrantLock(true);

    private Iterator<Config.ThrottleRule> throttleRules;
    private long sleep = 0;
    private long stepUpIn;

    public QueueItem fetchJob(RawRepoQueueDAO dao) throws QueueException, InterruptedException {
        lock.lockInterruptibly();
        try {
            throttle();
            QueueItem item = dao.dequeue(config.getQueueName());
            if (item != null) {
                success();
                return item;
            }
        } finally {
            lock.unlock();
        }
        failure();
        return null;
    }

    void success() {
        log.debug("Got a job - throttle reset");
        sleep = 0;
    }

    void failure() {
        log.debug("Got no job - throttle activate");
        if (sleep == 0L) {
            throttleRules = config.getThrottle().iterator();
            stepUpIn = 0;
        } else {
            stepUpIn--;
        }
        if (stepUpIn == 0) {
            Config.ThrottleRule rule = throttleRules.next();
            sleep = rule.getMillis();
            stepUpIn = rule.getCount();
        }
    }

    long getSleep() {
        return sleep;
    }

    void throttle() throws InterruptedException {
        if (sleep != 0L) {
            log.debug("Sleeping for {}ms", sleep);
            Thread.sleep(sleep);
        }
    }
}
