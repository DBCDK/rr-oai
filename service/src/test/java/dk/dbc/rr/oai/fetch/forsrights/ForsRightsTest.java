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
package dk.dbc.rr.oai.fetch.forsrights;

import dk.dbc.rr.oai.Config;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.UriBuilder;
import org.junit.Before;
import org.junit.Test;

import static dk.dbc.rr.oai.fetch.forsrights.BadgerFishReader.O;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ForsRightsTest {

    private Config config;
    private ForsRights forsRights;
    private String rule;
    private String json;

    @Before
    public void setUp() {
        this.config = new Config() {
            @Override
            public UriBuilder getForsRightsUrl() {
                return UriBuilder.fromUri("http://foo/bar");
            }

            @Override
            public Map<String, List<String>> getForsRightsRules() {
                return forsRights(rule);
            }
        };
        this.forsRights = new ForsRights() {
            @Override
            DTO getDTO(URI uri) throws IOException {
                return O.readValue(json, DTO.class);
            }
        };
        this.forsRights.config = config;
        this.json = "{\"forsRightsResponse\":{\"ressource\":[{\"name\":{\"$\":\"foo\",\"@\":\"fr\"},\"right\":[{\"$\":\"500\",\"@\":\"fr\"}]}],\"@\":\"fr\"},\"@namespaces\":{\"fr\":\"http:\\/\\/oss.dbc.dk\\/ns\\/forsrights\"}}";
    }

    @Test(timeout = 2_000L)
    public void testNoAuthorization() throws Exception {
        System.out.println("testNoAuthorization");
        String uri = forsRights.buildForsRightsURI(null, null).toString();
        assertThat(uri, containsString("userIdAut=&"));
        assertThat(uri, not(containsString("ipAddress")));
    }

    @Test(timeout = 2_000L)
    public void testInvalidTrippleNoIp() throws Exception {
        System.out.println("testInvalidTrippleNoIp");
        String uri = forsRights.buildForsRightsURI("a:b", null).toString();
        assertThat(uri, containsString("userIdAut=&"));
        assertThat(uri, not(containsString("ipAddress")));
    }

    @Test(timeout = 2_000L)
    public void testIpAddress() throws Exception {
        System.out.println("testIpAddress");
        String uri = forsRights.buildForsRightsURI(null, "127.0.0.1").toString();
        assertThat(uri, containsString("userIdAut=&"));
        assertThat(uri, containsString("ipAddress"));
    }

    @Test(timeout = 2_000L)
    public void testInvalidTrippleIpAddress() throws Exception {
        System.out.println("testInvalidTrippleIpAddress");
        String uri = forsRights.buildForsRightsURI("", "127.0.0.1").toString();
        assertThat(uri, containsString("userIdAut=&"));
        assertThat(uri, containsString("ipAddress"));
    }

    @Test(timeout = 2_000L)
    public void testTrippleNoIp() throws Exception {
        System.out.println("testTrippleNoIp");
        String uri = forsRights.buildForsRightsURI("aaa:bbb:ccc", null).toString();
        assertThat(uri, containsString("userIdAut"));
        assertThat(uri, not(containsString("ipAddress")));
    }

    @Test(timeout = 2_000L)
    public void testTrippleAndIp() throws Exception {
        System.out.println("testTrippleAndIp");
        String uri = forsRights.buildForsRightsURI("aaa:bbb:ccc", "127.0.0.1").toString();
        assertThat(uri, containsString("userIdAut"));
        assertThat(uri, containsString("ipAddress"));
    }

    @Test(timeout = 2_000L)
    public void testRule() throws Exception {
        System.out.println("testRule");
        // has only rule foo,500
        rule = "*=abc ; foo,300=def,ghi; foo,500=jkl,mno";
        Set<String> authorized1 = forsRights.authorized("aaa:bbb:ccc", "127.0.0.1");
        assertThat(authorized1, hasItems("abc", "jkl", "mno"));
        assertThat(authorized1, not(hasItems("def", "ghi")));
        assertThat(authorized1.size(), is(3));
    }

}
