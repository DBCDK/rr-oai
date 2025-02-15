/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-formatter-js
 *
 * rr-oai-formatter-js is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-formatter-js is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.formatter.js;

import dk.dbc.jslib.ClasspathSchemeHandler;
import dk.dbc.jslib.Environment;
import dk.dbc.jslib.FileSchemeHandler;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class JavaScriptFormatter {

    private static final Logger log = LoggerFactory.getLogger(JavaScriptFormatter.class);

    private static final String JS_FILE_NAME = "formatter.js";
    private static final String FORMATTER_METHOD = "format";
    private static final String ALLOWED_FORMATS_METHOD = "allowedFormats";

    private static final String[] SEARCH_PATHS = new String[] {
        "classpath:javascript/",
        "classpath:javascript/javacore/",
        "classpath:javascript/jscommon/config/",
        "classpath:javascript/jscommon/convert/",
        "classpath:javascript/jscommon/devel/",
        "classpath:javascript/jscommon/external/",
        "classpath:javascript/jscommon/io/",
        "classpath:javascript/jscommon/marc/",
        "classpath:javascript/jscommon/net/",
        "classpath:javascript/jscommon/system/",
        "classpath:javascript/jscommon/util/",
        "classpath:javascript/jscommon/xml/"
    };

    private final Environment environment;
    private final Set<String> allowedFormats;

    public JavaScriptFormatter() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        ModuleHandler moduleHandler = makeModuleHandler(classLoader,
                                                        SEARCH_PATHS);
        this.environment = new Environment();
        try (InputStream is = classLoader.getResourceAsStream("javascript/" + JS_FILE_NAME)) {
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                environment.registerUseFunction(moduleHandler);
                try {
                    environment.eval(reader, JS_FILE_NAME);
                } catch (IOException e) {
                    String errorMsg = String.format("Error accessing file: %s", JS_FILE_NAME);
                    log.error(errorMsg, e);
                    throw new IOException(errorMsg, e);
                }
            }
        }
        this.allowedFormats = getAllowedFormats();
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
        }
        return moduleHandler;
    }

    public String format(MarcXChangeWrapper[] records, String format, String sets) {
        List<String> setList = Arrays.asList(sets.split(","));
        try {
            if (!checkFormat(format))
                throw new IllegalArgumentException("Format '" + format + "' not allowed. Formats allowed: " + allowedFormats);
            return (String) environment.callMethod(FORMATTER_METHOD, new Object[] {records, format, setList});
        } catch (Exception ex) {
            log.error("Error formatting record: {}", ex.getMessage());
            log.debug("Error formatting record: ", ex);
            throw new IllegalStateException("Cannot format record");
        }
    }

    public boolean checkFormat(String format) {
        return allowedFormats.contains(format);
    }

   private Set<String> getAllowedFormats() throws Exception {
        Bindings obj = (Bindings) environment.callMethod(ALLOWED_FORMATS_METHOD, new Object[] {});
        return obj.values().stream().map(String::valueOf).collect(Collectors.toSet());
    }

}
