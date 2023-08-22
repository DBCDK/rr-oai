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

import dk.dbc.formatter.js.MarcXChangeWrapper;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class FormatterBeanTest {

    private static final Logger log = LoggerFactory.getLogger(FormatterBeanTest.class);

    private static final String MOCKED_FORMATTED_CONTENT = "<FORMATTED/>";

    @Test(timeout = 2_000L)
    public void testArguments() throws Exception {
        System.out.println("testArguments");
        FormatterBean bean = new FormatterBean();
        bean.rr = new RawRepo() {
            @Override
            public MarcXChangeWrapper[] getRecordsFor(int agencyId, String bibliographicRecordId) {
                return null;
            }
        };
        bean.jsPool = new JavaScriptPool() {
            @Override
            public String format(MarcXChangeWrapper[] records, String format, String sets) {
                return MOCKED_FORMATTED_CONTENT;
            }

            @Override
            public boolean checkFormat(String format) {
                switch (format) {
                    case "marcx":
                    case "oai_dc":
                        return true;
                    default:
                        return false;
                }
            }
        };

        expectException("Missing recordId", () -> bean.format(null, "marcx", "bkm", null), ClientErrorException.class);
        expectException("Empty recordId", () -> bean.format("", "marcx", "bkm", null), ClientErrorException.class);
        expectException("No colon in recordid", () -> bean.format("abc", "marcx", "bkm", null), ClientErrorException.class);
        expectException("Not a number for agency in recordid", () -> bean.format("abc:abc", "marcx", "bkm", null), ClientErrorException.class);
        expectException("Not a number for agency in recordid", () -> bean.format(":abc", "marcx", "bkm", null), ClientErrorException.class);
        expectException("Empty bibliographicRecordId", () -> bean.format("123:", "marcx", "bkm", null), ClientErrorException.class);
        expectException("Missing format", () -> bean.format("123:abc", null, "bkm", null), ClientErrorException.class);
        expectException("Empty format", () -> bean.format("123:abc", "", "bkm", null), ClientErrorException.class);
        expectException("Invalid format", () -> bean.format("123:abc", "abc", "bkm", null), ClientErrorException.class);
        expectException("Missing sets", () -> bean.format("123:abc", "marcx", null, null), ClientErrorException.class);
        expectException("Empty sets", () -> bean.format("123:abc", "marcx", "", null), ClientErrorException.class);

        Response resp = bean.format("123:abc", "marcx", "bkm", null);
        String xml = String.valueOf(resp.getEntity());
        assertThat(xml, is(MOCKED_FORMATTED_CONTENT));
    }

    private static <T extends RuntimeException> void expectException(String msg, Supplier<Response> sup, Class<T> e) {
        try {
            sup.get();
            assertThat(msg + " - Expected exception of type: " + e.getSimpleName(), false);
        } catch (RuntimeException ex) {
            if (e.isAssignableFrom(ex.getClass()))
                return;
            log.error("Got Exception: {}", ex.getMessage());
            log.debug("Got Exception: ", ex);
            assertThat(msg + " - Expected exception of type: " + e.getSimpleName() + " got " + ex.getClass().getSimpleName(), false);
        }
    }
}
