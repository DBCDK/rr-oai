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
import dk.dbc.rr.oai.fetch.IdpRights;
import dk.dbc.rr.oai.fetch.ParallelFetch;
import dk.dbc.rr.oai.io.OaiIOBean;
import dk.dbc.rr.oai.worker.OaiDatabaseMetadata;
import dk.dbc.rr.oai.worker.OaiDatabaseWorker;
import dk.dbc.rr.oai.worker.OaiWorker;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.sql.DataSource;
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
                "AUTHENTICATION_DISABLED=false",
                "ADMIN_EMAIL=user@example.com",
                "EXPOSED_URL=http://foo/bar",
                "IDP_RULES=*=art,nat;danbib,502=bkm,onl",
                "IDP_URL=" + System.getenv().getOrDefault("IDP_URL", "http://localhost:8008/idp/"),
                "RAWREPO_OAI_FORMATTER_SERVICE_URL=" + System.getenv().getOrDefault("RAWREPO_OAI_FORMATTER_SERVICE_URL", "http://localhost:8008/rawrepo-oai-formatter-service"),
                "PARALLEL_FETCH=8",
                "FETCH_TIMEOUT_IN_SECONDS=30",
                "MAX_ROWS_PR_REQUEST=10",
                "POOL_MIN_IDLE=5",
                "POOL_MAX_IDLE=10",
                "REPOSITORY_NAME=some text",
                "RESUMPTION_TOKEN_TIMEOUT=3h",
                "USER_AGENT=SpecialAgent/1.0",
                "XOR_TEXT_ASCII=ThisNeedsToBeSetInProduction");
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

    public static IdpRights newIdpRights(Config config) {
        IdpRights idpRights = new IdpRights();
        idpRights.config = config;
        idpRights.init();
        return idpRights;
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

    public static RemoteIp newRemoteIp(Config config) {
        RemoteIp remoteIp = new RemoteIp();
        remoteIp.config = config;
        remoteIp.init();
        return remoteIp;
    }

    public static OaiBean newOaiBean(Config config, DataSource dataSource) {
        OaiIOBean ioBean = newOaiIOBean(config);
        return newOaiBean(config, newIdpRights(config), newIndexHtml(), newRemoteIp(config), ioBean, newOaiWorker(config, dataSource, ioBean));
    }

    public static OaiBean newOaiBean(Config config, IdpRights idpRights, IndexHtml indexHtml, RemoteIp remoteIp, OaiIOBean oiIOBean, OaiWorker oaiWorker) {
        OaiBean oaiBean = new OaiBean();
        oaiBean.config = config;
        oaiBean.idpRights = idpRights;
        oaiBean.indexHtml = indexHtml;
        oaiBean.remoteIp = remoteIp;
        oaiBean.oaiIO = oiIOBean;
        oaiBean.oaiWorker = oaiWorker;
        return oaiBean;
    }

    public static OaiIOBean newOaiIOBean(Config config) {
        OaiIOBean oaiIOBean = new OaiIOBean();
        oaiIOBean.config = config;
        return oaiIOBean;
    }

    public static OaiWorker newOaiWorker(Config config, DataSource dataSource, OaiIOBean ioBean) {
        OaiWorker oaiWorker = new OaiWorker();
        oaiWorker.config = config;
        oaiWorker.databaseWorker = newOaiDatabaseWorker(config, dataSource);
        oaiWorker.databaseMetadata = newDatabaseMetadata(dataSource);
        oaiWorker.documentBuilders = newDocumentBuilderPool(config);
        oaiWorker.ioBean = ioBean;
        oaiWorker.parallelFetch = newParallelFetch(config, oaiWorker.documentBuilders);
        return oaiWorker;
    }

    public static OaiDatabaseWorker newOaiDatabaseWorker(Config config, DataSource dataSource) {
        OaiDatabaseWorker oaiDatabaseWorker = new OaiDatabaseWorker();
        oaiDatabaseWorker.config = config;
        oaiDatabaseWorker.dataSource = dataSource;
        return oaiDatabaseWorker;
    }

    public static OaiDatabaseMetadata newDatabaseMetadata(DataSource dataSource) {
        OaiDatabaseMetadata oaiDatabaseMetadata = new OaiDatabaseMetadata();
        oaiDatabaseMetadata.dataSource = dataSource;
        oaiDatabaseMetadata.init();
        return oaiDatabaseMetadata;
    }

    public static IdpRightsConfigValidator newIdpRightsConfigValidator(Config config, DataSource rawRepoOaiDs) {
        IdpRightsConfigValidator idpRightsConfigValidator = new IdpRightsConfigValidator();
        idpRightsConfigValidator.config = config;
        idpRightsConfigValidator.rawRepoOaiDs = rawRepoOaiDs;
        idpRightsConfigValidator.init();
        return idpRightsConfigValidator;
    }
}
