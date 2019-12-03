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
package dk.dbc.rr.oai.io;

import dk.dbc.oai.pmh.OAIPMHerrorcodeType;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiResponseTest {

    @Test(timeout = 2_000L)
    public void envelope() throws Exception {
        System.out.println("envelope");

        OaiResponse oaiResponse = OaiResponse.withoutRequestObject("http://foo/bar", qs("verb=ListRecords&from=2018-01-01&until=2019-01-01&metadataPrefix=dc"));
        String str = new String(oaiResponse.content("MY_MESSAGE"), UTF_8);

        assertThat(str, containsString("<?xml version=\"1.0\""));
        assertThat(str, containsString("<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"" +
                                       " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                                       " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">"));
        assertThat(str, containsString("<request verb=\"ListRecords\" metadataPrefix=\"dc\" from=\"2018-01-01\" until=\"2019-01-01\">http://foo/bar</request>"));
        assertThat(str, containsString("<!--MY_MESSAGE-->"));
    }

    @Test(timeout = 2_000L)
    public void errorsWithRequestParams() throws Exception {
        System.out.println("errorsWithRequestParams");

        OaiResponse oaiResponse = OaiResponse.withoutRequestObject("http://foo/bar", qs("verb=GetRecord&identifier=x&metadataPrefix=dc"));
        oaiResponse.error(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "no such id");
        String str = new String(oaiResponse.content(null), UTF_8);

        assertThat(str, containsString("<request verb=\"GetRecord\" identifier=\"x\" metadataPrefix=\"dc\">http://foo/bar</request>"));
        assertThat(str, containsString("<error code=\"idDoesNotExist\">no such id</error>"));
    }

    @Test(timeout = 2_000L)
    public void errorsWithoutRequestParams() throws Exception {
        System.out.println("errorsWithoutRequestParams");

        OaiResponse oaiResponse = OaiResponse.withoutRequestObject("http://foo/bar", qs(""));
        oaiResponse.error(OAIPMHerrorcodeType.BAD_VERB, "Value of the verb argument is not a legal OAI-PMH verb");
        String str = new String(oaiResponse.content(null), UTF_8);

        assertThat(str, containsString("<request>http://foo/bar</request>"));
        assertThat(str, containsString("<error code=\"badVerb\">Value of the verb argument is not a legal OAI-PMH verb</error>"));
    }

    @Test(timeout = 2_000L)
    public void errorsWithAndWithoutRequestParams() throws Exception {
        System.out.println("errorsWithAndWithoutRequestParams");

        OaiResponse oaiResponse = OaiResponse.withoutRequestObject("http://foo/bar", qs(""));
        oaiResponse.error(OAIPMHerrorcodeType.BAD_VERB, "Value of the verb argument is not a legal OAI-PMH verb");
        oaiResponse.error(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "no such id");
        String str = new String(oaiResponse.content(null), UTF_8);

        assertThat(str, containsString("<request>http://foo/bar</request>"));
        assertThat(str, containsString("<error code=\"idDoesNotExist\">no such id</error>"));
        assertThat(str, containsString("<error code=\"badVerb\">Value of the verb argument is not a legal OAI-PMH verb</error>"));
    }

    private static MultivaluedHashMap<String, String> qs(String qs) {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        if (qs != null && !qs.isEmpty()) {
            Arrays.stream(qs.split("&"))
                    .map(s -> s.split("=", 2))
                    .forEach(a -> {
                        try {
                            map.computeIfAbsent(URLDecoder.decode(a[0], "utf-8"),
                                                x -> new ArrayList<>())
                                    .add(URLDecoder.decode(a[1], "utf-8"));
                        } catch (UnsupportedEncodingException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
        return map;
    }

}
