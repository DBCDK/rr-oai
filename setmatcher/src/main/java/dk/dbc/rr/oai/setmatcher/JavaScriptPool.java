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
package dk.dbc.rr.oai.setmatcher;

import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A pool of JavaScript environments
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Startup
public class JavaScriptPool {

    private static final Logger log = LoggerFactory.getLogger(JavaScriptPool.class);

    @Inject
    Config config;

    private ObjectPool<JavaScriptSetMatcher> pool;
    private boolean inBadState;

    public JavaScriptPool() {
        inBadState = false;
    }

    @PostConstruct
    public void init() {
        try {
            pool = makePool();
        } catch (Exception ex) {
            log.error("Error preparint JavaScript pool: {}", ex.getMessage());
            log.debug("Error preparint JavaScript pool: ", ex);
            inBadState = true;
        }
    }

    /**
     * Delegate call to a JavaScriptInstance
     *
     * @param agencyId agency id
     * @param content  marcxchange content for record
     * @return collection of sets record is in
     * @throws Exception
     */
    public Set<String> getOaiSets(int agencyId, byte[] content) throws Exception {
        try (Lease lease = new Lease()) {
            return lease.get().getOaiSets(agencyId, new String(content, UTF_8));
        }
    }

    /**
     * Has something gone wrong with the pool or the JavaScript environments
     *
     * @return are we in a failed state
     */
    public boolean isInBadState() {
        return inBadState;
    }

    /**
     * Make a pool (Apache-pool) of JavaScriptSetMatchers
     *
     * @return new Pool
     */
    private GenericObjectPool<JavaScriptSetMatcher> makePool() throws Exception {
        GenericObjectPool<JavaScriptSetMatcher> newPool = new GenericObjectPool<>(new BasePooledObjectFactory<JavaScriptSetMatcher>() {
            @Override
            public JavaScriptSetMatcher create() throws Exception {
                try {
                    return new JavaScriptSetMatcher();
                } catch (Exception ex) {
                    inBadState = true;
                    log.error("Error generating javascript environment: {}", ex.getMessage());
                    log.debug("Error generating javascript environment: ", ex);
                    throw new IllegalStateException("Cannot make javascript environment");
                }
            }

            @Override
            public PooledObject<JavaScriptSetMatcher> wrap(JavaScriptSetMatcher t) {
                return new DefaultPooledObject<>(t);
            }
        });
        newPool.setMinIdle(config.getPoolMinIdle());
        newPool.setMaxIdle(config.getPoolMaxIdle());
        newPool.preparePool();
        return newPool;
    }

    /**
     * Lease helper class (autoclosable)
     */
    private final class Lease implements AutoCloseable {

        private final JavaScriptSetMatcher obj;

        public Lease() throws Exception {
            this.obj = pool.borrowObject();
        }

        public JavaScriptSetMatcher get() {
            return obj;
        }

        @Override
        public void close() throws Exception {
            pool.returnObject(obj);
        }
    }
}
