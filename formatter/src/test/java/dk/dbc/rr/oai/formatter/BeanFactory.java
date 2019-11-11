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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.JerseyClientBuilder;

import static java.util.stream.Collectors.toMap;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class BeanFactory {

    public static Config newConfig(String... envs) {
        Config config = new Config(configMapWithDefaults(envs)) {
            @Override
            protected ClientBuilder clientBuilder() {
                return JerseyClientBuilder.newBuilder();
            }
        };
        config.init();
        return config;
    }

    public static Map<String, String> configMapWithDefaults(String... envs) {
        Map<String, String> defaults = configMap(
                "RAWREPO_RECORD_SERVICE_URL=" + System.getenv().getOrDefault("RAWREPO_RECORD_SERVICE_URL", "http://localhost/rawrepo-record-service"),
                "POOL_MIN_IDLE=1",
                "POOL_MAX_IDLE=1",
                "USER_AGENT=SpecialAgent/1.0");
        Map<String, String> declared = configMap(envs);
        HashMap<String, String> env = new HashMap<>();
        env.putAll(defaults);
        env.putAll(declared);
        return env;
    }

    private static Map<String, String> configMap(String... envs) {
        return Stream.of(envs)
                .map(s -> s.split("=", 2))
                .collect(toMap(a -> a[0], a -> a[1]));
    }

    public static RawRepo newRawRepo(Config config) {
        RawRepo rawRepo = new RawRepo();
        rawRepo.config = config;
        rawRepo.init();
        return rawRepo;
    }
}
