/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-setmatcher
 *
 * rr-oai-setmatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-setmatcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.setmatcher;

import dk.dbc.jscommon.JsCommonPaths;
import dk.dbc.jslib.ClasspathSchemeHandler;
import dk.dbc.jslib.Environment;
import dk.dbc.jslib.FileSchemeHandler;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.script.Bindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.EMPTY_SET;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class JavaScriptSetMatcher {

    private static final Logger log = LoggerFactory.getLogger(JavaScriptSetMatcher.class);

    private static final String OAI_SET_MATCHER_SCRIPT = "oaiSetMatcher.js";
    private static final String OAI_AGENCY_ELIGIBLE_METHOD = "oaiIsEligible";
    private static final String OAI_SET_MATCHER_METHOD = "oaiSetMatcher";

    private static final String[] SEARCH_PATHS = new String[] {
        "classpath:javascript/"
    };

    private final Environment environment;

    public JavaScriptSetMatcher() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        ModuleHandler moduleHandler = makeModuleHandler(classLoader,
                                                        SEARCH_PATHS);
        this.environment = new Environment();
        try (InputStream is = classLoader.getResourceAsStream("javascript/" + OAI_SET_MATCHER_SCRIPT)) {
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                environment.registerUseFunction(moduleHandler);
                try {
                    environment.eval(reader, OAI_SET_MATCHER_SCRIPT);
                } catch (IOException e) {
                    String errorMsg = String.format("Error accessing file: %s", OAI_SET_MATCHER_SCRIPT);
                    log.error(errorMsg, e);
                    throw new IOException(errorMsg, e);
                }
            }
        }
    }

    private static ModuleHandler makeModuleHandler(ClassLoader classLoader, String[] searchPaths) {
        ModuleHandler moduleHandler = new ModuleHandler();
        moduleHandler.registerHandler("file", new FileSchemeHandler("/"));
        moduleHandler.registerHandler("classpath", new ClasspathSchemeHandler(classLoader));
        for (String searchPath : searchPaths) {
            if (!searchPath.endsWith(":") && !searchPath.endsWith("/")) {
                searchPath += "/";
            }
            moduleHandler.addSearchPath(new SchemeURI(searchPath));
            JsCommonPaths.registerPaths(searchPath, moduleHandler);
        }
        return moduleHandler;
    }

    /**
     * Call JavaScript function that determines which sets to include record in
     *
     * @param agencyId Library number
     * @param content  MarcXChange record as string
     * @return Set of sets that include this record
     * @throws Exception In case of JavaScript Errors
     */
    public Set<String> getOaiSets(int agencyId, String content) throws Exception {
        return environment.getJavascriptObjectAsStringSet(
                environment.callMethod(OAI_SET_MATCHER_METHOD, new Object[] {agencyId, content})
        );
    }

    /**
     * Call JavaScript function that determines is the agency can be part of any set
     *
     * @param agencyId Library number
     * @return if it is eligible for matching through {@link #getOaiSets(int, java.lang.String)}
     * @throws Exception In case of JavaScript Errors
     */
    public boolean isElibible(int agencyId) throws Exception {
        return (boolean) environment.callMethod(OAI_AGENCY_ELIGIBLE_METHOD, new Object[] {agencyId});
    }

}
