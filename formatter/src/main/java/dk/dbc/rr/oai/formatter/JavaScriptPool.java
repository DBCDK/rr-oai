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

import dk.dbc.formatter.js.JavaScriptFormatter;
import dk.dbc.formatter.js.MarcXChangeWrapper;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private ObjectPool<JavaScriptFormatter> pool;
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
     * Use JavaScript to convert into an XML string
     *
     * @param records record array of volume, section and head
     * @param format  name of format (currently only oai_dc or marcx)
     * @param sets    which sets the record is in
     * @return XML string
     */
    public String format(MarcXChangeWrapper[] records, String format, String sets) {
        try (Lease lease = new Lease()) {
            JavaScriptFormatter formatter = lease.get();
            try {
                return formatter.format(records, format, sets);
            } catch (Exception e) {
                log.error("Cannot format record(s): {}", e.getMessage());
                log.debug("Cannot format record(s): ", e);
                throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex) {
            inBadState = true;
            throw new IllegalStateException("Cannot lease object from pool", ex);
        }
    }

    /**
     * Check against JavaScript if the format is valid
     *
     * @param format name of format
     * @return of the format is known
     */
    public boolean checkFormat(String format) {
        try (Lease lease = new Lease()) {
            return lease.get().checkFormat(format);
        } catch (Exception ex) {
            inBadState = true;
            throw new IllegalStateException("Cannot lease object from pool", ex);
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
     * Make a pool (Apache-pool) of JavaScriptFormatters
     *
     * @return new Pool
     */
    private GenericObjectPool<JavaScriptFormatter> makePool() throws Exception {
        GenericObjectPool<JavaScriptFormatter> newPool = new GenericObjectPool<>(new BasePooledObjectFactory<JavaScriptFormatter>() {
            @Override
            public JavaScriptFormatter create() throws Exception {
                try {
                    return new JavaScriptFormatter();
                } catch (Exception ex) {
                    inBadState = true;
                    log.error("Error generating javascript environment: {}", ex.getMessage());
                    log.debug("Error generating javascript environment: ", ex);
                    throw new IllegalStateException("Cannot make javascript environment");
                }
            }

            @Override
            public PooledObject<JavaScriptFormatter> wrap(JavaScriptFormatter t) {
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

        private final JavaScriptFormatter obj;

        public Lease() throws Exception {
            this.obj = pool.borrowObject();
        }

        public JavaScriptFormatter get() {
            return obj;
        }

        @Override
        public void close() throws Exception {
            pool.returnObject(obj);
        }
    }
}
