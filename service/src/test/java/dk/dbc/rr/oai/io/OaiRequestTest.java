/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part oaiResponseOf rr-oai-service
 *
 * rr-oai-service is free software: you can redistribute it and/or modify
 * it under the terms oaiResponseOf the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 oaiResponseOf the License, or
 * (at your option) any later version.
 *
 * rr-oai-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty oaiResponseOf
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy oaiResponseOf the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.io;

import dk.dbc.rr.oai.Config;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static dk.dbc.rr.oai.BeanFactory.*;
import static java.nio.charset.StandardCharsets.*;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_MAP;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@RunWith(Parameterized.class)
public class OaiRequestTest {

    private static final byte[] XOR = "ThisIsJustTestData".getBytes(ISO_8859_1);

    private static final OaiIOBean BEAN = newOaiIOBean(new Config(EMPTY_MAP) {
        @Override
        public byte[] getXorBytes() {
            return super.getXorBytes();
        }
    });

    @Parameters
    public static Collection<Object[]> tests() throws Exception {

        String rt = new OaiResumptionToken(OaiTimestamp.of("2019"), "xx", OaiTimestamp.of("2020"), null).toData(Instant.MAX, XOR);
        return asList(
                test("Invalid verb #1", "verb=Info",
                     "<error code=\"badVerb\">argument: verb contains an invalid value</error>"),
                test("Invalid verb #2", "verb=Info&foo=bar",
                     "<error code=\"badVerb\">argument: verb contains an invalid value</error>",
                     "<error code=\"badArgument\">argument: foo is unknown</error>"),
                test("GetRecord - from and until", "verb=GetRecord&from=2020&until=2020&set=x&resumptionToken=" + rt,
                     "<error code=\"badArgument\">argument: identifier is required for verb: GetRecord</error>",
                     "<error code=\"badArgument\">argument: metadataPrefix is required for verb: GetRecord</error>",
                     "<error code=\"badArgument\">argument: from is not allowed for verb: GetRecord</error>",
                     "<error code=\"badArgument\">argument: resumptionToken is not allowed for verb: GetRecord</error>",
                     "<error code=\"badArgument\">argument: set is not allowed for verb: GetRecord</error>",
                     "<error code=\"badArgument\">argument: until is not allowed for verb: GetRecord</error>"),
                test("GetRecord - success", "verb=GetRecord&identifier=ix&metadataPrefix=dc",
                     "<request verb=\"GetRecord\" identifier=\"ix\" metadataPrefix=\"dc\">http://foo/bar</request>"),
                test("ListRecords - bad resumption-token", "verb=ListRecords&metadataPrefix=dc&resumptionToken=xxx",
                     "<error code=\"badResumptionToken\">Invalid or expired resumptionToken</error>"),
                test("ListRecords - ok resumption-token", "verb=ListRecords&metadataPrefix=dc&resumptionToken=" + new OaiResumptionToken(OaiTimestamp.of("2019"), "xx", OaiTimestamp.of("2020"), null).toData(Instant.MAX, XOR),
                     "<request verb=\"ListRecords\" metadataPrefix=\"dc\" resumptionToken=\"",
                     "\">http://foo/bar</request>"),
                test("ListRecords - resumption-token and from", "verb=ListRecords&metadataPrefix=dc&from=2019&resumptionToken=" + new OaiResumptionToken(OaiTimestamp.of("2019"), "xx", OaiTimestamp.of("2020"), null).toData(Instant.MAX, XOR),
                     "<error code=\"badArgument\">argument: from is not allowed for verb: ListRecords when resumptionToken is set</error>"),
                test("Identify", "verb=Identify&identifier=x&metadataPrefix=dc&from=2020&until=2020&set=x&resumptionToken=" + rt,
                     "<error code=\"badArgument\">argument: from is not allowed for verb: Identify</error>",
                     "<error code=\"badArgument\">argument: identifier is not allowed for verb: Identify</error>",
                     "<error code=\"badArgument\">argument: metadataPrefix is not allowed for verb: Identify</error>",
                     "<error code=\"badArgument\">argument: resumptionToken is not allowed for verb: Identify</error>",
                     "<error code=\"badArgument\">argument: set is not allowed for verb: Identify</error>",
                     "<error code=\"badArgument\">argument: until is not allowed for verb: Identify</error>"),
                test("ListMetadataFormats - ok empty", "verb=ListMetadataFormats",
                     "<request verb=\"ListMetadataFormats\">"),
                test("ListMetadataFormats - ok empty", "verb=ListMetadataFormats&identifier=x",
                     "<request verb=\"ListMetadataFormats\" identifier=\"x\">"),
                test("ListMetadataFormats - extra arguments", "verb=ListMetadataFormats&identifier=x&metadataPrefix=dc&from=2020&until=2020&set=x&resumptionToken=" + rt,
                     "<error code=\"badArgument\">argument: from is not allowed for verb: ListMetadataFormats</error>",
                     "<error code=\"badArgument\">argument: metadataPrefix is not allowed for verb: ListMetadataFormats</error>",
                     "<error code=\"badArgument\">argument: resumptionToken is not allowed for verb: ListMetadataFormats</error>",
                     "<error code=\"badArgument\">argument: set is not allowed for verb: ListMetadataFormats</error>",
                     "<error code=\"badArgument\">argument: until is not allowed for verb: ListMetadataFormats</error>"),
                test("ListSets - extra arguments", "verb=ListSets&identifier=x&metadataPrefix=dc&from=2020&until=2020&set=x&resumptionToken=" + rt,
                     "<error code=\"badArgument\">argument: from is not allowed for verb: ListSets</error>",
                     "<error code=\"badArgument\">argument: metadataPrefix is not allowed for verb: ListSets</error>",
                     "<error code=\"badResumptionToken\">Invalid or expired resumptionToken</error>",
                     "<error code=\"badArgument\">argument: set is not allowed for verb: ListSets</error>",
                     "<error code=\"badArgument\">argument: until is not allowed for verb: ListSets</error>"),
                test("ListRecords - bad timestamp from", "verb=ListRecords&from=xxx&metadataPrefix=dc",
                     "<error code=\"badArgument\">argument: from contains an invalid value</error>"),
                test("ListRecords - bad timestamp until", "verb=ListRecords&until=xxx&metadataPrefix=dc",
                     "<error code=\"badArgument\">argument: until contains an invalid value</error>")
        );
    }

    /**
     *
     * @param name     Name oaiResponseOf test
     * @param qs       query string
     * @param expected list oaiResponseOf expected strings in response
     * @return test
     */
    private static Object[] test(String name, String qs, String... expected) {
        return new Object[] {name, qs, expected};
    }

    private final String name;
    private final MultivaluedHashMap<String, String> request;
    private final String[] expected;

    public OaiRequestTest(String name, String queryString, String... expected) {
        this.name = name;
        this.request = new MultivaluedHashMap<>();
        Arrays.stream(queryString.split("&"))
                .map(s -> s.split("=", 2))
                .forEach(a -> {
                    try {
                        String key = URLDecoder.decode(a[0], "UTF-8");
                        String val = URLDecoder.decode(a[1], "UTF-8");
                        this.request.computeIfAbsent(key,
                                                     x -> new ArrayList<>())
                                .add(val);
                    } catch (UnsupportedEncodingException ex) {
                        throw new RuntimeException(ex);
                    }
                });
        this.expected = expected;
    }

    @Test(timeout = 2_000L)
    public void test() throws Exception {
        System.out.println("test - " + name);
        OaiResponse oaiResponse = BEAN.oaiResponseOf("http://foo/bar", request);
        String str = new String(oaiResponse.content(), UTF_8);
        System.out.println(str);
        for (String exp : expected) {
            assertThat(str, containsString(exp));
        }
    }
}
