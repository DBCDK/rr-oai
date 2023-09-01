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
package dk.dbc.rr.oai.setmatcher;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@ApplicationScoped
@Startup
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private final Map<String, String> env;

    private Client httpClient;
    private int poolMinIdle;
    private int poolMaxIdle;
    private String queueName;
    private String rawrepoRecordService;
    private int maxBatchSize;
    private long maxProcessingTime;
    private int maxConsecutiveServerErrors;
    private long pollRate;
    private int threads;

    public Config() {
        this(System.getenv());
    }

    public Config(Map<String, String> env) {
        this.env = env;
    }

    @PostConstruct
    public void init() {
        log.info("Setting up config");

        this.httpClient = getenv("USER_AGENT", "RawRepoOaiSerMatcher/1.0")
                .convert(userAgent -> clientBuilder()
                        .register((ClientRequestFilter) (ClientRequestContext context) ->
                                context.getHeaders().putSingle("User-Agent", userAgent)
                        )
                        .register(new JacksonFeature())
                        .build());
        this.queueName = getenv("QUEUE_NAME")
                .isNot("not empty", String::isEmpty)
                .get();
        this.maxBatchSize = getenv("MAX_BATCH_SIZE").asInt().min(1).get();
        this.maxConsecutiveServerErrors = getenv("MAX_CONSECUTIVE_ERRORS").asInt().min(1).get();
        this.maxProcessingTime = getenv("MAX_PROCESSING_TIME").convert(Config::seconds);
        this.pollRate = getenv("POLL_RATE").convert(Config::seconds);
        this.poolMinIdle = getenv("POOL_MIN_IDLE").asInt().min(1).get();
        this.poolMaxIdle = getenv("POOL_MAX_IDLE").asInt().min(1).get();
        this.rawrepoRecordService = getenv("RAWREPO_RECORD_SERVICE_URL")
                .isNot("not empty", String::isEmpty)
                .get();
        this.threads = getenv("THREADS").asInt().min(1).get();
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public int getMaxConsecutiveServerErrors() {
        return maxConsecutiveServerErrors;
    }

    public long getMaxProcessingTime() {
        return maxProcessingTime;
    }

    public long getPollRate() {
        return pollRate;
    }

    public int getPoolMaxIdle() {
        return poolMaxIdle;
    }

    public int getPoolMinIdle() {
        return poolMinIdle;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getRawrepoRecordService() {
        return rawrepoRecordService;
    }

    public int getThreads() {
        return threads;
    }

    protected ClientBuilder clientBuilder() {
        return ClientBuilder.newBuilder();
    }

    /**
     * Convert a string representation of a duration (number{h|m|s|ms}) to
     * milliseconds
     *
     * @param spec string representation
     * @return number of milliseconds
     */
    static long seconds(String spec) {
        String[] split = spec.split("(?<=\\d)(?=\\D)");
        if (split.length == 2) {
            long units = Long.parseUnsignedLong(split[0]);
            switch (split[1].toLowerCase(Locale.ROOT)) {
                case "s":
                    return TimeUnit.SECONDS.toSeconds(units);
                case "m":
                    return TimeUnit.MINUTES.toSeconds(units);
                case "h":
                    return TimeUnit.HOURS.toSeconds(units);
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("Invalid time spec: " + spec);
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
