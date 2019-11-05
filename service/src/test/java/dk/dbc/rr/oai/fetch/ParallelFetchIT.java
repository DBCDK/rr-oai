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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import dk.dbc.rr.oai.Config;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Client;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ParallelFetchIT {

    @Test(timeout = 60_000L)
    public void testSuccess() throws Exception {
        System.out.println("testSuccess");

        ConcurrentSkipListSet<Long> tids = new ConcurrentSkipListSet<>();

        Map<String, String> env = System.getenv();
        Config config = new Config(Stream.of(
                "EXPOSED_URL=http://foo/bar",
                "RAWREPO_OAI_FORMATTER_SERVICE_URL=" + env.getOrDefault("RAWREPO_OAI_FORMATTER_SERVICE_URL", "http://localhost/rawrepo-oai-formatter-service"),
                "PARALLEL_FETCH=30",
                "FETCH_TIMEOUT_IN_SECONDS=30",
                "POOL_MIN_IDLE=10",
                "POOL_MAX_IDLE=100",
                "USER_AGENT=fool-me/1.0"
        )
                .map(s -> s.split("=", 2))
                .collect(toMap(a -> a[0], a -> a[1]))) {

            @Override
            public Client getHttpClient() {
                // Record which threads get http clients
                tids.add(Thread.currentThread().getId());
                return super.getHttpClient();
            }

        };
        config.init();
        DocumentBuilderPool documentBuilderPool = new DocumentBuilderPool();
        documentBuilderPool.config = config;
        documentBuilderPool.init();

        ParallelFetch parallelFetch = new ParallelFetch();
        parallelFetch.config = config;
        parallelFetch.documentBuilders = documentBuilderPool;

        List<String> ids = Arrays.asList(
                "870970:00010480", "870970:00010626", "870970:00020001", "870970:00020087", "870970:00020117",
                "870970:00020125", "870970:00020141", "870970:00020184", "870970:00020206", "870970:00020214",
                "870970:00020257", "870970:00020281", "870970:00020370", "870970:00020389", "870970:00020478",
                "870970:00020486", "870970:00020397", "870970:00020508", "870970:00020788", "870970:00020796",
                "870970:00020818", "870970:00020826", "870970:00020834", "870970:00020877", "870970:00020893");

        List<URI> uris = ids.stream()
                .map(id -> parallelFetch.buildUri(id, "marcx", "bkm"))
                .collect(toList());

        List<Element> docs = parallelFetch.parallelFetch(uris);
        System.out.println("docs = " + docs);
        assertThat(docs.stream()
                .anyMatch(e -> e == null), is(false));

        System.out.println("tids = " + tids);
        assertThat("Multiple threads in play", tids.size() > 1, is(true));
    }

    @Test(timeout = 2_000L)
    public void testBadXml() throws Exception {
        System.out.println("testBadXml");

        Map<String, String> env = System.getenv();
        Config config = Config.of(
                "EXPOSED_URL=http://foo/bar",
                "RAWREPO_OAI_FORMATTER_SERVICE_URL=" + env.getOrDefault("RAWREPO_OAI_FORMATTER_SERVICE_URL", "http://localhost/rawrepo-oai-formatter-service"),
                "PARALLEL_FETCH=30",
                "FETCH_TIMEOUT_IN_SECONDS=30",
                "POOL_MIN_IDLE=10",
                "POOL_MAX_IDLE=100",
                "USER_AGENT=fool-me/1.0"
        );
        config.init();
        DocumentBuilderPool documentBuilderPool = new DocumentBuilderPool();
        documentBuilderPool.config = config;
        documentBuilderPool.init();

        ParallelFetch parallelFetch = new ParallelFetch();
        parallelFetch.config = config;
        parallelFetch.documentBuilders = documentBuilderPool;

        List<String> ids = Arrays.asList(
                "870970:00010480", "870970:error");

        List<URI> uris = ids.stream()
                .map(id -> parallelFetch.buildUri(id, "marcx", "bkm"))
                .collect(toList());

        List<Element> docs = parallelFetch.parallelFetch(uris);
        System.out.println("docs = " + docs);
        assertThat(docs.stream()
                .anyMatch(e -> e == null), is(true));
    }

    @Test(timeout = 2_000L, expected = ServerErrorException.class)
    public void testTimeout() throws Exception {
        System.out.println("testTimeout");

        Map<String, String> env = System.getenv();
        Config config = Config.of(
                "EXPOSED_URL=http://foo/bar",
                "RAWREPO_OAI_FORMATTER_SERVICE_URL=" + env.getOrDefault("RAWREPO_OAI_FORMATTER_SERVICE_URL", "http://localhost/rawrepo-oai-formatter-service"),
                "PARALLEL_FETCH=1",
                "FETCH_TIMEOUT_IN_SECONDS=1",
                "POOL_MIN_IDLE=1",
                "POOL_MAX_IDLE=100",
                "USER_AGENT=fool-me/1.0"
        );
        config.init();
        DocumentBuilderPool documentBuilderPool = new DocumentBuilderPool();
        documentBuilderPool.config = config;
        documentBuilderPool.init();

        ParallelFetch parallelFetch = new ParallelFetch();
        parallelFetch.config = config;
        parallelFetch.documentBuilders = documentBuilderPool;

        ArrayList<URI> uris = new ArrayList<>(10_000);
        URI uri = parallelFetch.buildUri("870970:00010480", "marcx", "bkm");
        for (int i = 0 ; i < 10_000 ; i++) {
            uris.add(uri);
        }

        // Set loglevel to INFO for rest of this test
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger(getClass().getCanonicalName().replaceAll("IT$", ""));
        Level level = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            parallelFetch.parallelFetch(uris); // throws ServerError Exception
        } finally {
            logger.setLevel(level);
        }
    }
}
