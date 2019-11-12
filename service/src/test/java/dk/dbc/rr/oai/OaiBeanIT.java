/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is match of rr-oai-service
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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import static dk.dbc.rr.oai.BeanFactory.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiBeanIT extends DB {

    private static final String ABNO_TRIPLE = "abno:xxx:yyy";
    private static final String AN_TRIPLE = "an:xxx:yyy";
    private static final String UNAUTHORIZED_TRIPLE = "unknown:xxx:yyy";

    private OaiBean oaiBean;

    @Before
    public void setUp() throws Exception {
        this.oaiBean = newOaiBean(newConfig(), ds);
    }

    @Test(timeout = 2_000L)
    public void testGetAllowedSets() throws Exception {
        System.out.println("testGetAllowedSets");
        Set<String> unknown = oaiBean.getAllowedSets(UNAUTHORIZED_TRIPLE, "127.0.0.1");
        assertThat(unknown.stream().sorted().collect(toList()), is(asList()));
        Set<String> an = oaiBean.getAllowedSets(AN_TRIPLE, "127.0.0.1");
        assertThat(an.stream().sorted().collect(toList()), is(asList("art", "nat")));
        Set<String> abno = oaiBean.getAllowedSets(ABNO_TRIPLE, "127.0.0.1");
        assertThat(abno.stream().sorted().collect(toList()), is(asList("art", "bkm", "nat", "onl")));
        Set<String> anonymous = oaiBean.getAllowedSets(null, "8.8.8.8");
        assertThat(anonymous.stream().sorted().collect(toList()), is(asList("art", "nat")));
    }

    @Test(timeout = 2_000L)
    public void testIdentify() throws Exception {
        System.out.println("testIdentify");

        byte[] bytes = oaiBean.processOaiRequest(null, null, queryString("verb=Identify"));
        String content = new String(bytes, UTF_8);
        System.out.println("content = " + content);
        assertThat(content, containsInOrder(
                   "<request verb=\"Identify\">http://foo/bar</request>",
                   "<Identify>",
                   "<repositoryName>some text</repositoryName>",
                   "<baseURL>http://foo/bar</baseURL>",
                   "<protocolVersion>2.0</protocolVersion>",
                   "<adminEmail>user@example.com</adminEmail>",
                   "<earliestDatestamp>1970-01-01T00:00:00Z</earliestDatestamp>",
                   "<deletedRecord>transient</deletedRecord>",
                   "<granularity>YYYY-MM-DDThh:mm:ssZ</granularity>",
                   "</Identify>"));
    }

    private MultivaluedMap<String, String> queryString(String... strings) {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        Stream.of(strings)
                .map(s -> s.split("=", 2))
                .forEach(a -> map.computeIfAbsent(URLDecoder.decode(a[0], UTF_8),
                                                    x -> new ArrayList<>())
                        .add(URLDecoder.decode(a[1], UTF_8)));
        return map;
    }

    public static Matcher<String> containsInOrder(String... parts) {
        return new BaseMatcher<String>() {
            String text;
            String match;
            int offset = 0;

            @Override
            public boolean matches(Object item) {
                text = String.valueOf(item);
                for (String part : parts) {
                    this.match = part;
                    int pos = text.indexOf(part, offset);
                    if (pos == -1)
                        return false;
                    offset = pos + part.length();
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                String unmatched = text.substring(offset);
                if (unmatched.length() > 45)
                    unmatched = unmatched.substring(0, 40) + "...";
                description.appendText("expected ")
                        .appendValue(match)
                        .appendText(" looking at ")
                        .appendValue(unmatched);
            }
        };
    }
}
