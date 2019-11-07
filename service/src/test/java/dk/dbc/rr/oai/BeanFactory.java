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
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class BeanFactory {

    public static Config newConfig(String... envs) {
        Map<String, String> env = Stream.of(envs)
                .map(s -> s.split("=", 2))
                .collect(toMap(a -> a[0], a -> a[1]));
        Config config = new Config(env);
        config.init();
        return config;
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

    private static OaiBean newOaiBean(IndexHtml indexHtml) {
        OaiBean oaiBean = new OaiBean();
        oaiBean.indexHtml = indexHtml;
        return oaiBean;
    }

}