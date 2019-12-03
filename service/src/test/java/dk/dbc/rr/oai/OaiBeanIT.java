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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
    public void testGetRecord() throws Exception {
        System.out.println("testGetRecord");
        insert("870970-00020389").set("nat=").commit();
        String content = requestAnonymous("verb=GetRecord&identifier=870970-00020389&metadataPrefix=marcx");
        assertThat(content, containsInOrder(
                   "<GetRecord>",
                   "<record>",
                   "<header>",
                   "<identifier>870970-00020389</identifier>",
                   "<metadata>",
                   "<marcx:"));
    }

    @Test(timeout = 2_000L)
    public void testGetRecordDeleted() throws Exception {
        System.out.println("testGetRecordDeleted");
        insert("870970-00020389").deleted().set("!nat=").commit();
        String content = requestAuthorized("verb=GetRecord&identifier=870970-00020389&metadataPrefix=marcx");
        assertThat(content, containsInOrder(
                   "<error code=\"idDoesNotExist\">"));
    }

    @Test(timeout = 2_000L)
    public void testGetRecordNoSet() throws Exception {
        System.out.println("testGetRecordNoSet");
        insert("870970-00020389").set("bkm=").commit();
        String content = requestAnonymous("verb=GetRecord&identifier=870970-00020389&metadataPrefix=marcx");
        assertThat(content, containsInOrder(
                   "<error code=\"idDoesNotExist\">"));
    }

    @Test(timeout = 2_000L)
    public void testIdentify() throws Exception {
        System.out.println("testIdentify");
        String content = requestAnonymous("verb=Identify");
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

    @Test(timeout = 2_000L)
    public void testListIdentifiersEmptySet() throws Exception {
        System.out.println("testListIdentifiersEmptySet");
        String content = requestAnonymous("verb=ListIdentifiers&from=2019&until=2019&set=nat&metadataPrefix=oai_dc");
        assertThat(content, containsInOrder(
                   "<error code=\"noRecordsMatch\">"
           ));
        assertThat(content, not(containsString("<ListIdentifiers>")));
    }

    @Test(timeout = 2_000L)
    public void listIdentifiers() throws Exception {
        System.out.println("listIdentifiers");
        loadResource("records-15-same-timestamp.json");
        String firstPid = "870970-00010480";
        String content = requestAnonymous("verb=ListIdentifiers&from=2019&until=2019&set=nat&metadataPrefix=oai_dc");
        assertThat(content, containsInOrder(
                   "<ListIdentifiers>",
                   "<header>",
                   "<identifier>",
                   "<datestamp>",
                   "<setSpec>nat</setSpec>",
                   "<resumptionToken",
                   "</ListIdentifiers>"
           ));
        content = requestAuthorized("verb=ListIdentifiers&from=2019&until=2019&set=nat&metadataPrefix=oai_dc");
        assertThat(content, containsInOrder(
                   "<ListIdentifiers>",
                   "<header>",
                   "<identifier>",
                   "<datestamp>",
                   "<setSpec>nat</setSpec>",
                   "<resumptionToken",
                   "</ListIdentifiers>"
           ));
        String resumptionToken = content.replaceFirst(".*<resumptionToken[^>]*>", "").replaceFirst("<.*", "");
        System.out.println("resumptionToken = " + resumptionToken);

        insert(firstPid).deleted()
                .set("!nat").set("!bkm").commit();
        content = requestAuthorized("verb=ListIdentifiers&metadataPrefix=oai_dc&resumptionToken=" + resumptionToken);
        assertThat(content, containsInOrder(
                   "<ListIdentifiers>",
                   "<header>",
                   "<identifier>",
                   "<datestamp>",
                   "<setSpec>nat</setSpec>",
                   "<header status=\"deleted\">", // FirstPid was deleted
                   "<identifier>",
                   firstPid,
                   "</ListIdentifiers>"
           ));
        assertThat(content, not(containsString("<resumptionToken")));
    }

    @Test(timeout = 2_000L)
    public void testListIdentifiersMultipleSets() throws Exception {
        System.out.println("testListIdentifiersMultipleSets");
        insert("123456-12000001").set("nat").commit();
        insert("123456-12000002").set("nat", "art").commit();
        insert("123456-12000003").set("nat").commit();
        String content = requestAuthorized("verb=ListIdentifiers&from=2019&until=2019&metadataPrefix=oai_dc");
        assertThat(content, containsString("123456-12000002"));
        assertThat(content, not(containsInOrder("123456-12000002", "123456-12000002"))); // Only once
    }

    @Test(timeout = 2_000L)
    public void testListIdentifiersManyErrors() throws Exception {
        System.out.println("testListIdentifiersManyErrors");
        String content = requestAuthorized("verb=ListIdentifiers&from=2019&until=2018&set=unknown&metadataPrefix=really_bad");
        assertThat(content, containsInOrder(
                   "code=\"cannotDisseminateFormat\""
           ));
        assertThat(content, containsInOrder(
                   "<error code=\"badArgument\"",
                   "Until is before from",
                   "<error code=\"badArgument\"",
                   "Unknown setspec"
           ));
    }

    @Test(timeout = 2_000L)
    public void testListMetadataFormats() throws Exception {
        System.out.println("testListMetadataFormats");
        String content = requestAnonymous("verb=ListMetadataFormats");
        assertThat(content, containsInOrder(
                   "<request verb=\"ListMetadataFormats\">http://foo/bar</request>",
                   "<ListMetadataFormats>",
                   "<metadataFormat>",
                   "<metadataPrefix>marcx</metadataPrefix>",
                   "<schema>https://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd</schema>",
                   "<metadataNamespace>info:lc/xmlns/marcxchange-v1</metadataNamespace>",
                   "</metadataFormat>",
                   "<metadataFormat>",
                   "<metadataPrefix>oai_dc</metadataPrefix>",
                   "<schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema>",
                   "<metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace>",
                   "</metadataFormat>",
                   "</ListMetadataFormats>"));
    }

    @Test(timeout = 15_000L)
    public void listRecords() throws Exception {
        System.out.println("listRecords");
        loadResource("records-15-same-timestamp.json");
        String content = requestAuthorized("verb=ListRecords&from=2019&until=2019&set=nat&metadataPrefix=marcx");
        assertThat(content, containsInOrder(
                   "<ListRecords>",
                   "<record>",
                   "<header>",
                   "<identifier>",
                   "<datestamp>",
                   "<setSpec>nat</setSpec>",
                   "</header>",
                   "<metadata>",
                   "<marcx:",
                   "</ListRecords>"
           ));
    }

    @Test(timeout = 15_000L)
    public void listRecordsDeleted() throws Exception {
        System.out.println("listRecordsDeleted");
        insert("870970-00010480").deleted()
                .set("!nat=now").commit();
        String content = requestAnonymous("verb=ListRecords&from=2019&set=nat&metadataPrefix=marcx");
        assertThat(content, containsInOrder(
                   "<ListRecords>",
                   "<record>",
                   "<header status=\"deleted\">",
                   "<identifier>",
                   "<datestamp>",
                   "</header>",
                   "</ListRecords>"
           ));
        assertThat(content, not(containsString("<metadata>"))); // record does not contain metadata
        assertThat(content, not(containsString("<setSpec>"))); // Not in any set
    }

    @Test(timeout = 15_000L)
    public void listRecordsVanished() throws Exception {
        System.out.println("listRecordsVanished");
        insert("870970-00010480")
                .set("!nat=now").commit();
        String content = requestAuthorized("verb=ListRecords&from=2019&until=2019&set=nat&metadataPrefix=marcx");
        assertThat(content, containsInOrder(
                   "<ListRecords>",
                   "<record>",
                   "<header",
                   "<identifier>",
                   "<datestamp>",
                   "</header>",
                   "</ListRecords>"
           ));
        assertThat(content, not(containsString("<metadata>"))); // record does not contain metadata
        assertThat(content, not(containsString("<setSpec>"))); // Not in any set
    }

    @Test(timeout = 2_000L)
    public void testListSets() throws Exception {
        System.out.println("testListSets");
        String content = requestAnonymous("verb=ListSets");
        assertThat(content, containsInOrder(
                   "<ListSets>",
                   "<set>",
                   "<setSpec>art</setSpec>",
                   "<setName>", "</setName>",
                   "<dc:description>",
                   "</set>",
                   "<set>",
                   "<setSpec>bkm</setSpec>",
                   "<setName>", "</setName>",
                   "<dc:description>",
                   "</set>",
                   "<set>",
                   "<setSpec>nat</setSpec>",
                   "<setName>", "</setName>",
                   "<dc:description>",
                   "</set>",
                   "<set>",
                   "<setSpec>onl</setSpec>",
                   "<setName>", "</setName>",
                   "<dc:description>",
                   "</set>",
                   "</ListSets>"
           ));
    }

    @Test(timeout = 2_000L)
    public void testListMetadataFormatsUnknownIdenitifier() throws Exception {
        System.out.println("testListMetadataFormatsUnknownIdenitifier");
        String content = requestAnonymous("verb=ListMetadataFormats&identifier=xxx");
        assertThat(content, containsString("<error code=\"idDoesNotExist\">No such record</error>"));
    }

    @Test(timeout = 2_000L)
    public void testListMetadataFormatsKnownIdenitifier() throws Exception {
        System.out.println("testListMetadataFormatsKnownIdenitifier");
        insert("xxx-yyy").deleted().set("nat").commit();
        String content = requestAnonymous("verb=ListMetadataFormats&identifier=xxx-yyy");
        assertThat(content, not(containsString("<error code=\"idDoesNotExist\">No such record</error>")));
    }

    @Test(timeout = 2_000L)
    public void testListMetadataFormatsKnownIdenitifierNotInOurSet() throws Exception {
        System.out.println("testListMetadataFormatsKnownIdenitifierNotInOurSet");
        insert("xxx-yyy").deleted().set("bkm").commit();
        String content = requestAnonymous("verb=ListMetadataFormats&identifier=xxx-yyy");
        assertThat(content, containsString("<error code=\"idDoesNotExist\">No such record</error>"));
    }

    private String requestAnonymous(String queryString) {
        byte[] bytes = oaiBean.processOaiRequest(new HashSet<>(Arrays.asList("nat")), queryString(queryString), "tracking");
        String content = new String(bytes, UTF_8);
        System.out.println("content = " + content);
        return content;
    }

    private String requestAuthorized(String queryString) {
        byte[] bytes = oaiBean.processOaiRequest(new HashSet<>(Arrays.asList("art", "bkm", "nat", "onl")), queryString(queryString), "tracking");
        String content = new String(bytes, UTF_8);
        System.out.println("content = " + content);
        return content;
    }

    private MultivaluedMap<String, String> queryString(String qs) {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        Stream.of(qs.split("&"))
                .filter(s -> !s.isEmpty())
                .map(s -> s.split("=", 2))
                .forEach(a -> map.computeIfAbsent(decode(a[0]),
                                                    x -> new ArrayList<>())
                        .add(decode(a[1])));
        return map;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
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
