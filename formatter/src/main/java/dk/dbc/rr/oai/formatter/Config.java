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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
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

    private Client httpClient;
    private String rawrepoRecordService;
    private Integer poolMinIdle;
    private Integer poolMaxIdle;

    public Config() {
        this(System.getenv());
    }

    private Config(Map<String, String> env) {
        this.env = env;
    }

    /**
     * Used for unittesting
     *
     * @param envs list of strings (KEY=VALUE)
     * @return configuration for testing
     */
    public static Config of(String... envs) {
        Map<String, String> env = Arrays.stream(envs)
                .map(s -> s.split("=", 2))
                .collect(toMap(a -> a[0], a -> a[1]));
        return new Config(env);
    }

    @PostConstruct
    public void init() {
        log.info("Setting up configuration");
        this.httpClient = getenv("USER_AGENT", "RawRepoOaiFormatter/1.0")
                .convert(userAgent -> ClientBuilder.newBuilder()
                        .register((ClientRequestFilter) (ClientRequestContext context) ->
                                context.getHeaders().putSingle("User-Agent", userAgent)
                        )
                        .register(new JacksonFeature()) // Needed by RawRepo RecordService
                        .build());
        this.rawrepoRecordService = getenv("RAWREPO_RECORD_SERVICE_URL").get();
        this.poolMinIdle = getenv("POOL_MIN_IDLE").convert(Integer::parseUnsignedInt);
        this.poolMaxIdle = getenv("POOL_MAX_IDLE").convert(Integer::parseUnsignedInt);
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public String getRawrepoRecordService() {
        return rawrepoRecordService;
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

        private <T> T convert(Function<String, T> mapper) {
            try {
                return mapper.apply(value);
            } catch (RuntimeException ex) {
                log.error("Cannot convert {}: {}", name, ex.getMessage());
                throw new IllegalArgumentException("Variable: " + name + " is invalid");
            }
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
