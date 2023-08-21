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

import jakarta.annotation.PostConstruct;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@ApplicationScoped
@Startup
@LocalBean
public class IndexHtml {

    private static final Logger log = LoggerFactory.getLogger(IndexHtml.class);

    private byte[] indexHtml;

    @PostConstruct
    public void init() {
        ClassLoader loader = getClass().getClassLoader();
        try (InputStream is = loader.getResourceAsStream("oai-index.html")) {
            indexHtml = new byte[0];
            while (true) {
                int avail = is.available();
                if (avail == 0)
                    break;
                int pos = indexHtml.length;
                indexHtml = Arrays.copyOf(indexHtml, pos + avail);
                is.read(indexHtml, pos, avail);
            }
        } catch (IOException ex) {
            log.error("Error loading index.html: {}", ex.getMessage());
            log.debug("Error loading index.html: ", ex);
            throw new RuntimeException(ex);
        }
    }

    public ByteArrayInputStream getInputStream() {
        return new ByteArrayInputStream(indexHtml);
    }

}
