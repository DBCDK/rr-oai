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
package dk.dbc.rr.oai;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toMap;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@ApplicationScoped
@Startup
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private final Map<String, String> env;

    private String exposedUrl;
    private UriBuilder formatServiceUri;
    private Client httpClient;
    private Integer parallelFetch;
    private Integer poolMinIdle;
    private Integer poolMaxIdle;
    private int fetchTimeoutInSeconds;

    public Config() {
        this(System.getenv());
    }

    public Config(Map<String, String> env) {
        this.env = env;
    }

    public static Config of(String... envs) {
        Map<String, String> env = Stream.of(envs)
                .map(s -> s.split("=", 2))
                .collect(toMap(a -> a[0], a -> a[1]));
        return new Config(env);
    }

    @PostConstruct
    public void init() {
        log.info("Setting up config");

        this.exposedUrl = getenv("EXPOSED_URL").get();
        this.httpClient = getenv("USER_AGENT", "RawRepoOaiService/1.0")
                .convert(userAgent -> ClientBuilder.newBuilder()
                        .register((ClientRequestFilter) (ClientRequestContext context) ->
                                context.getHeaders().putSingle("User-Agent", userAgent)
                        )
                        .register(new JacksonFeature())
                        .build());
        this.fetchTimeoutInSeconds = getenv("FETCH_TIMEOUT_IN_SECONDS").asInt()
                .min(1)
                .get();
        this.parallelFetch = getenv("PARALLEL_FETCH").asInt()
                .min(1)
                .get();
        this.poolMinIdle = getenv("POOL_MIN_IDLE").asInt()
                .min(0)
                .get();
        this.poolMaxIdle = getenv("POOL_MAX_IDLE").asInt()
                .min(1, "less that 1 whould create/destroy DOM Parser for every call")
                .min(poolMinIdle + 1, "is should be more that POOL_MIN_IDLE")
                .get();
        this.formatServiceUri = getenv("RAWREPO_OAI_FORMATTER_SERVICE_URL")
                .isNot("not empty", String::isEmpty)
                .convert(UriBuilder::fromUri)
                .path("api/format");
    }

    public String getExposedUrl() {
        return exposedUrl;
    }

    public int getFetchTimeoutInSeconds() {
        return fetchTimeoutInSeconds;
    }

    public UriBuilder getFormatServiceUri() {
        return formatServiceUri.clone();
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public Integer getParallelFetch() {
        return parallelFetch;
    }

    public Integer getPoolMaxIdle() {
        return poolMaxIdle;
    }

    public Integer getPoolMinIdle() {
        return poolMinIdle;
    }

    private static class FromEnv {

        private final String name;
        private final String value;

        private FromEnv(String name, String value) {
            this.name = name;
            this.value = value;
        }

        private String get() {
            return value;
        }

//        private FromEnv is(String test, Predicate<String> p) {
//            if (!p.test(value)) {
//                log.error("Variable: {} is {}", name, test);
//                throw new IllegalArgumentException("Variable: " + name + " is invalid");
//            }
//            return this;
//        }

        private FromEnv isNot(String test, Predicate<String> p) {
            if (p.test(value)) {
                log.error("Variable: {} is {}", name, test);
                throw new IllegalArgumentException("Variable: " + name + " is invalid");
            }
            return this;
        }

        private <T> T convert(Function<String, T> mapper) {
            try {
                return mapper.apply(value);
            } catch (RuntimeException ex) {
                log.error("Cannot convert {}: {}", name, ex.getMessage());
                throw new IllegalArgumentException("Variable: " + name + " is invalid");
            }
        }

        private IntFromEnv asInt() {
            return new IntFromEnv(name, convert(Integer::parseInt));
        }
    }

    private static class IntFromEnv {

        private final String name;
        private final int value;

        private IntFromEnv(String name, int value) {
            this.name = name;
            this.value = value;
        }

        private int get() {
            return value;
        }

        private IntFromEnv min(int minValue, String reason) {
            if (value < minValue) {
                if (reason != null) {
                    log.error("Variable: {} is invalid - should be atleast {} because {}", name, minValue, reason);
                } else {
                    log.error("Variable: {} is invalid - should be atleast {}", name, minValue);
                }
                throw new IllegalArgumentException("Values should be atleast: " + minValue);
            }
            return this;
        }

        private IntFromEnv min(int minValue) {
            return min(minValue, null);
        }
    }

    FromEnv getenv(String name) {
        String value = env.get(name);
        if (value == null)
            throw new IllegalArgumentException("Required variable: " + name + " is unset");
        return new FromEnv(name, value);
    }

    FromEnv getenv(String name, String defaultValue) {
        String value = env.getOrDefault(name, defaultValue);
        return new FromEnv(name, value);
    }

}