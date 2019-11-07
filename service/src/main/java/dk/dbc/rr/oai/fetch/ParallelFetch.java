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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static java.util.stream.Collectors.toList;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class ParallelFetch {

    private static final Logger log = LoggerFactory.getLogger(ParallelFetch.class);

    @Inject
    public Config config;

    @Inject
    public DocumentBuilderPool documentBuilders;

    // List of currently active threads, for forced stop upon timeout
    private final ConcurrentHashMap<Long, Thread> threads;

    public ParallelFetch() {
        this.threads = new ConcurrentHashMap<>();
    }

    public URI buildUri(String id, String format, String sets) {
        return config.getFormatServiceUri()
                .queryParam("id", id)
                .queryParam("format", format)
                .queryParam("sets", sets)
                .build();
    }

    /**
     * Produces a list of DOM Elements for the given uris
     * <p>
     * If the service doesn't supply a valid xml response the element will be
     * null.
     *
     * @param uris uris as generated by
     *             {@link #buildUri(java.lang.String, java.lang.String, java.lang.String)}
     * @return list of XML-Elements and/or nulls
     * @throws ServerErrorException in case of a timeout
     */
    public List<Element> parallelFetch(List<URI> uris) {
        log.info("Requesting {} uris", uris.size());
        threads.clear();
        ExecutorService executor;
        executor = Executors.newFixedThreadPool(config.getParallelFetch());
        List<Future<Document>> requests = uris.stream()
                .map(u -> executor.submit(() -> fetcher(u)))
                .collect(toList());
        executor.shutdown();
        try {
            boolean completed = executor.awaitTermination(config.getFetchTimeoutInSeconds(), TimeUnit.SECONDS);
            log.debug("completed = {}", completed);
            if (!completed)
                throw new ServerErrorException("Fetching of records timed out", Response.Status.INTERNAL_SERVER_ERROR);
        } catch (InterruptedException ex) {
            log.error("Interrupted during awaitTermination: {}", ex.getMessage());
            log.debug("Interrupted during awaitTermination: ", ex);
            throw new ServerErrorException("Interrupted during parallel fetch", Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            if (!executor.isTerminated()) {
                log.info("Shutting down pending format calls");
                executor.shutdownNow();
                threads.forEachValue(1, Thread::interrupt);
            }
        }
        return requests.stream()
                .map(f -> {
                    try {
                        if (!f.isDone())
                            return null;
                        return f.get().getDocumentElement();
                    } catch (InterruptedException | ExecutionException ex) {
                        log.error("Error fetching document: {}", ex.getMessage());
                        log.debug("Error fetching document: ", ex);
                        return null;
                    }
                })
                .collect(toList());
    }

    /**
     * Fetch an uri as an XML Element
     * <p>
     * This registers itself in {@link #threads} before a fetch and unregisters
     * after.
     * It uses the {@link #documentBuilders} pool ,for xml parsing
     *
     * @param req uri to fetch
     * @return xml element or runtime exception
     */
    private Document fetcher(URI req) {
        log.debug("Fetching {}", req);
        Thread thread = Thread.currentThread();
        long me = thread.getId();
        try {
            threads.put(me, thread);
            try (DocumentBuilderPool.Lease lease = documentBuilders.lease()) {
                if (thread.isInterrupted())
                    throw new InterruptedException("during lease()");
                Client client = config.getHttpClient();
                try (InputStream is = client.target(req)
                        .request(MediaType.APPLICATION_XML_TYPE)
                        .get(InputStream.class)) {
                    if (thread.isInterrupted())
                        throw new InterruptedException("During get(url)");
                    return lease.get().parse(is);
                }
            } catch (SAXException | IOException ex) {
                log.error("Cannot parse XML from formatter url: {}: {}", req, ex.getMessage());
                log.debug("Cannot parse XML from formatter url: {}: ", req, ex);
                throw new ServerErrorException("Cannot format record (parse xml)", Response.Status.INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
                log.error("Error leasing document builder: {}", ex.getMessage());
                log.debug("Error leasing document builder: ", ex);
                throw new ServerErrorException("Cannot format record (get parser)", Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            threads.remove(me);
        }
    }
}
