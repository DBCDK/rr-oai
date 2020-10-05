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

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiResumptionTokenTest {

    private static final byte[] XOR = "ThisIsJustTestData".getBytes(ISO_8859_1);

    @Test(timeout = 2_000L)
    public void resumptionTokenOk() throws Exception {
        System.out.println("resumptionTokenOk");
        OaiResumptionToken before = new OaiResumptionToken(OaiTimestamp.of("1970-01-01T00:00:00Z"), ts("1970-01-01T12:34:56Z"), null, OaiTimestamp.of("2134-12-31T23:59:59.999Z"), "foo");
        String data = before.toData(Instant.MAX, XOR);
        System.out.println("data = " + data);
        OaiResumptionToken after = OaiResumptionToken.of(data, XOR);
        assertThat(after, is(before));
    }

    @Test(timeout = 2_000L)
    public void resumptionTokenOkWithNull1() throws Exception {
        System.out.println("resumptionTokenOkWithNull1");
        OaiResumptionToken before = new OaiResumptionToken(OaiTimestamp.of("1970-01-01T00:00:00Z"), ts("1970-01-01T12:34:56Z"), "id:0", null, "");
        String data = before.toData(Instant.MAX, XOR);
        System.out.println("data = " + data);
        OaiResumptionToken after = OaiResumptionToken.of(data, XOR);
        assertThat(after, is(before));
    }

    @Test(timeout = 2_000L)
    public void resumptionTokenOkWithNull2() throws Exception {
        System.out.println("resumptionTokenOkWithNull2");
        OaiResumptionToken before = new OaiResumptionToken(OaiTimestamp.of("1970-01-01T00:00:00Z"), ts("1970-01-01T12:34:56Z"), "id:0", OaiTimestamp.of("2134-12-31T23:59:59.999Z"), null);
        String data = before.toData(Instant.MAX, XOR);
        System.out.println("data = " + data);
        OaiResumptionToken after = OaiResumptionToken.of(data, XOR);
        assertThat(after, is(before));
    }

    @Test(timeout = 2_000L)
    public void resumptionTokenExpired() throws Exception {
        System.out.println("resumptionTokenExpired");
        OaiResumptionToken before = new OaiResumptionToken(OaiTimestamp.of("1970-01-01T00:00:00Z"), ts("1970-01-01T12:34:56Z"), "id:0", OaiTimestamp.of("2134-12-31T23:59:59.999Z"), "foo");
        String data = before.toData(Instant.now().minusMillis(1), XOR);
        System.out.println("data = " + data);
        OaiResumptionToken after = OaiResumptionToken.of(data, XOR);
        assertThat(after, nullValue());
    }

    @Test(timeout = 2_000L)
    public void resumptionTokenBadBase64() throws Exception {
        System.out.println("resumptionTokenBadBase64");
        OaiResumptionToken token = OaiResumptionToken.of("01=", XOR); // .length % 4 != 0
        assertThat(token, nullValue());
    }

    @Test(timeout = 2_000L)
    public void resumptionTokenTooShort() throws Exception {
        System.out.println("resumptionTokenTooShort");
        OaiResumptionToken token = OaiResumptionToken.of("012=", XOR);
        assertThat(token, nullValue());
    }

    @Test(timeout = 2_000L)
    public void resumptionTokenChecksumError() throws Exception {
        System.out.println("resumptionTokenChecksumError");
        OaiResumptionToken token = OaiResumptionToken.of("0123456789==", XOR);
        assertThat(token, nullValue());
    }

    private static Timestamp ts(String text) {
        return OaiTimestamp.of(text).getTimestamp();
    }
}
