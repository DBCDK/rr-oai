/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-service
 *
 * rr-oai-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.fetch;

import dk.dbc.rr.oai.Config;
import java.io.StringReader;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Startup
public class DocumentBuilderPool {

    private static final Logger log = LoggerFactory.getLogger(DocumentBuilderPool.class);

    private static final EntityResolver NULL_RESOLVER =
            (publicId, systemId) -> new InputSource(new StringReader(""));

    @Inject
    public Config config;

    private ObjectPool<DocumentBuilder> pool;
    private boolean inBadState;

    public DocumentBuilderPool() {
        this.inBadState = false;
    }

    @PostConstruct
    public void init() {
        try {
            this.pool = makePool();
        } catch (Exception ex) {
            log.error("Error priming pool: {}", ex.getMessage());
            log.debug("Error priming pool: ", ex);
            inBadState = true;
        }
    }

    /**
     * Used by health check
     *
     * @return if the pool is in a bad state
     */
    public boolean isInBadState() {
        return inBadState;
    }

    /**
     * Lease a DocumentBuilder from the pool
     *
     * @return auto-closable lease
     */
    public Lease lease() {
        return new Lease();
    }

    /**
     * DocmentBuilder constructor
     * <p>
     * Ensures that construction is thread safe
     *
     * @return new object
     */
    private DocumentBuilder makeDocumentBuilder() {
        synchronized (DocumentBuilderFactory.class) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setIgnoringComments(true);
                dbf.setIgnoringElementContentWhitespace(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                db.setEntityResolver(NULL_RESOLVER);
                return db;
            } catch (ParserConfigurationException ex) {
                inBadState = true;
                throw new RuntimeException(ex);
            }
        }
    }

    private ObjectPool<DocumentBuilder> makePool() throws Exception {
        GenericObjectPool<DocumentBuilder> newPool =
                new GenericObjectPool<>(new BasePooledObjectFactory<DocumentBuilder>() {
                    @Override
                    public DocumentBuilder create() throws Exception {
                        return makeDocumentBuilder();
                    }

                    @Override
                    public PooledObject<DocumentBuilder> wrap(DocumentBuilder obj) {
                        return new DefaultPooledObject<>(obj);
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
    public final class Lease implements AutoCloseable {

        private final DocumentBuilder obj;

        private Lease() {
            try {
                this.obj = pool.borrowObject();
            } catch (Exception ex) {
                log.error("Cannot borrow object from pool: {}", ex.getMessage());
                log.debug("Cannot borrow object from pool: ", ex);
                inBadState = true;
                throw new RuntimeException(ex);
            }
        }

        public DocumentBuilder get() {
            return obj;
        }

        @Override
        public void close() {
            try {
                pool.returnObject(obj);
            } catch (Exception ex) {
                log.error("Cannot return object to pool: {}", ex.getMessage());
                log.debug("Cannot return object to pool: ", ex);
                inBadState = true;
            }
        }
    }

}
