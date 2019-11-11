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

import dk.dbc.rr.oai.fetch.DocumentBuilderPool;
import dk.dbc.rr.oai.fetch.ParallelFetch;
import dk.dbc.rr.oai.fetch.forsrights.ForsRights;
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
                "ADMIN_EMAIL=user@example.com",
                "DESCRIPTION=some text",
                "EXPOSED_URL=http://foo/bar",
                "FORS_RIGHTS_RULES=*=art,nat;danbib,502=bkm,onl",
                "FORS_RIGHTS_URL=" + System.getenv().getOrDefault("FORS_RIGHTS_URL", "http://localhost/forsrights"),
                "RAWREPO_OAI_FORMATTER_SERVICE_URL=" + System.getenv().getOrDefault("RAWREPO_OAI_FORMATTER_SERVICE_URL", "http://localhost/rawrepo-oai-formatter-service"),
                "PARALLEL_FETCH=5",
                "FETCH_TIMEOUT_IN_SECONDS=30",
                "POOL_MIN_IDLE=5",
                "POOL_MAX_IDLE=10",
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

    public static IndexHtml newIndexHtml() {
        IndexHtml indexHtml = new IndexHtml();
        indexHtml.init();
        return indexHtml;
    }

    public static DocumentBuilderPool newDocumentBuilderPool(Config config) {
        DocumentBuilderPool documentBuilderPool = new DocumentBuilderPool();
        documentBuilderPool.config = config;
        documentBuilderPool.init();
        return documentBuilderPool;
    }

    public static ForsRights newForsRights(Config config) {
        ForsRights forsRights = new ForsRights();
        forsRights.config = config;
        return forsRights;
    }

    public static ParallelFetch newParallelFetch(Config config) {
        return newParallelFetch(config, newDocumentBuilderPool(config));
    }

    public static ParallelFetch newParallelFetch(Config config, DocumentBuilderPool documentBuilderPool) {
        ParallelFetch parallelFetch = new ParallelFetch();
        parallelFetch.config = config;
        parallelFetch.documentBuilders = documentBuilderPool;
        return parallelFetch;
    }

    public static OaiBean newOaiBean() {
        return newOaiBean(newIndexHtml());
    }

    public static OaiBean newOaiBean(IndexHtml indexHtml) {
        OaiBean oaiBean = new OaiBean();
        oaiBean.indexHtml = indexHtml;
        return oaiBean;
    }

}
