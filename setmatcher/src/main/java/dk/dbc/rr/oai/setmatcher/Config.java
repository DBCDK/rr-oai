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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.*;

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
    private int threads;
    private int queueStalledAfter;
    private List<ThrottleRule> throttle;

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
        this.poolMinIdle = getenv("POOL_MIN_IDLE").asInt().min(1).get();
        this.poolMaxIdle = getenv("POOL_MAX_IDLE").asInt().min(1).get();
        this.rawrepoRecordService = getenv("RAWREPO_RECORD_SERVICE_URL")
                .isNot("not empty", String::isEmpty)
                .get();
        this.threads = getenv("THREADS").asInt().min(1).get();
        this.throttle = getenv("THROTTLE")
                .convert(Config::parseThrottleRules);
        this.poolMaxIdle = getenv("QUEUE_STALLED_AFTER").asInt().min(1).get();
    }

    public Client getHttpClient() {
        return httpClient;
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

    public int getQueueStalledAfter() {
        return queueStalledAfter;
    }

    public String getRawrepoRecordService() {
        return rawrepoRecordService;
    }

    public int getThreads() {
        return threads;
    }

    public List<ThrottleRule> getThrottle() {
        return throttle;
    }

    protected ClientBuilder clientBuilder() {
        return ClientBuilder.newBuilder();
    }

    public static class ThrottleRule {

        private final long millis;
        private final long count;

        public ThrottleRule(long millis, long count) {
            this.millis = millis;
            this.count = count;
        }

        public static ThrottleRule of(String spec) {
            String[] parts = spec.split("/", 2);
            long millis = milliseconds(parts[0].trim());
            if (millis <= 0L)
                throw new IllegalArgumentException("Invalid timeout in throttle spec: " + spec);
            int count = Integer.parseUnsignedInt(parts[1].trim());
            if (count <= 0)
                throw new IllegalArgumentException("Invalid count in throttle spec: " + spec);
            return new ThrottleRule(millis, count);
        }

        public long getCount() {
            return count;
        }

        public long getMillis() {
            return millis;
        }
    }

    public static List<ThrottleRule> parseThrottleRules(String ruleSpec) {
        String[] specs = Stream.of(ruleSpec.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (specs.length == 0)
            throw new IllegalArgumentException("Cannot parse throttle-spec");
        String lastSpec = specs[specs.length - 1];
        ThrottleRule lastRule = new ThrottleRule(milliseconds(lastSpec), Long.MAX_VALUE);
        return Stream.concat(Arrays.stream(specs, 0, specs.length - 1).map(ThrottleRule::of),
                             Stream.of(lastRule))
                .collect(toList());
    }

    /**
     * Convert a string representation of a duration (number{h|m|s|ms}) to
     * milliseconds
     *
     * @param spec string representation
     * @return number of milliseconds
     */
    static long milliseconds(String spec) {
        String[] split = spec.split("(?<=\\d)(?=\\D)");
        if (split.length == 2) {
            long units = Long.parseUnsignedLong(split[0]);
            switch (split[1].toLowerCase(Locale.ROOT)) {
                case "ms":
                    return TimeUnit.MILLISECONDS.toMillis(units);
                case "s":
                    return TimeUnit.SECONDS.toMillis(units);
                case "m":
                    return TimeUnit.MINUTES.toMillis(units);
                case "h":
                    return TimeUnit.HOURS.toMillis(units);
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
