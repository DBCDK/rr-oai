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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class JavaScriptSetMatcherTest {

    private static final JavaScriptSetMatcher JSSM = makeJavaScriptSetMatcher();

    private static JavaScriptSetMatcher makeJavaScriptSetMatcher() {
        try {
            return new JavaScriptSetMatcher();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test(timeout = 2_000L)
    public void testEligible() throws Exception {
        System.out.println("testEligible");
        boolean elibgible;
        elibgible = JSSM.isElibible(870970);
        System.out.println("elibgible(870970) = " + elibgible);
        assertThat(elibgible, is(true));
        elibgible = JSSM.isElibible(870971);
        System.out.println("elibgible(870971) = " + elibgible);
        assertThat(elibgible, is(true));
        elibgible = JSSM.isElibible(710100);
        System.out.println("elibgible(710100) = " + elibgible);
        assertThat(elibgible, is(false));
    }

    @Test(timeout = 2_000L)
    public void testNat() throws Exception {
        System.out.println("testNat");
        String content = getResource("870971-47366054.xml");
        Set<String> oaiSets = JSSM.getOaiSets(870971, content);
        System.out.println("oaiSets = " + oaiSets);
        assertThat(oaiSets, hasItems("NAT"));
        assertThat(oaiSets, not(hasItems("BKM", "ONL", "ART")));
    }

    @Test(timeout = 2_000L)
    public void testArtNat() throws Exception {
        System.out.println("testNat");
        String content = getResource("870971-47366038.xml");
        Set<String> oaiSets = JSSM.getOaiSets(870971, content);
        System.out.println("oaiSets = " + oaiSets);
        assertThat(oaiSets, hasItems("ART", "NAT"));
        assertThat(oaiSets, not(hasItems("BKM", "ONL")));
    }

    @Test(timeout = 2_000L)
    public void testBkm() throws Exception {
        System.out.println("testBkm");
        String content = getResource("870970-47308143.xml");
        Set<String> oaiSets = JSSM.getOaiSets(870970, content);
        System.out.println("oaiSets = " + oaiSets);
        assertThat(oaiSets, hasItems("BKM"));
        assertThat(oaiSets, not(hasItems("NAT", "ONL", "ART")));
    }

    @Test(timeout = 2_000L)
    public void testBkmNat() throws Exception {
        System.out.println("testBkmNat");
        String content = getResource("870970-54252625.xml");
        Set<String> oaiSets = JSSM.getOaiSets(870970, content);
        System.out.println("oaiSets = " + oaiSets);
        assertThat(oaiSets, hasItems("BKM", "NAT"));
        assertThat(oaiSets, not(hasItems("ONL", "ART")));
    }

    @Test(timeout = 2_000L)
    public void testBkmNatOnl() throws Exception {
        System.out.println("testBkmNatOnl");
        String content = getResource("870970-47314933.xml");
        Set<String> oaiSets = JSSM.getOaiSets(870970, content);
        System.out.println("oaiSets = " + oaiSets);
        assertThat(oaiSets, hasItems("BKM", "NAT", "ONL"));
        assertThat(oaiSets, not(hasItems("ART")));
    }

    @Test(timeout = 2_000L)
    public void testBci() throws Exception {
        System.out.println("testBci");
        String content = getResource("870970-47666813.xml");
        Set<String> oaiSets = JSSM.getOaiSets(870970, content);
        System.out.println("oaiSets = " + oaiSets);
        assertThat(oaiSets, hasItems("FDEPOT"));
        assertThat(oaiSets, not(hasItems("NAT", "ONL", "ART", "BKM")));
    }


    public String getResource(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null)
            throw new IllegalArgumentException("Cannot find resource: " + path);
        byte[] bytes = new byte[250_000];
        int len = is.read(bytes);
        return new String(bytes, 0, len, UTF_8);
    }

}
