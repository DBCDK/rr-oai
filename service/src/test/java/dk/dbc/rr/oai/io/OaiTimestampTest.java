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

import java.util.Arrays;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiTimestampTest {

    @Test(timeout = 2_000L)
    public void testCheckString() throws Exception {
        System.out.println("testCheckString");

        Arrays.asList(
                "2019 true",
                "2019-02 true",
                "2019-92 false",
                "2019-01-11 true",
                "2019-02-29 false", // Not leap
                "2019-01-01T12 false", // Requires :mm'Z' too
                "2019-01-01T12z false", // Requires :mm'Z' too
                "2019-01-01T12:34Z true",
                "2019-01-01T12:34:56Z true",
                "2019-01-01T21:34:56Z true",
                "2019-01-01T12:34:77Z false",
                "2019-01-01T12:34:56.Z false",
                "2019-01-01T12:34:56.1Z true",
                "2019-01-01T12:34:56.12Z true",
                "2019-01-01T12:34:56.123Z true",
                "2019-01-01T12:34:56.1234Z true",
                "2019-01-01T12:34:56.12345Z true",
                "2019-01-01T12:34:56.123456Z true",
                "2019-01-01T12:34:56.1234567Z false",
                "xxx false",
                " false" // Empty string
        ).forEach(l -> {
            String a[] = l.split("\\s+", 2);
            boolean actual = OaiTimestamp.checkString(a[0]) != null;
            System.out.println(a[0] + " = " + actual);
            assertThat(a[0], actual, is(Boolean.parseBoolean(a[1])));
        });
    }

    @Test(timeout = 2_000L)
    public void testOf() throws Exception {
        System.out.println("testOf - make timestamp with granuality");

        Arrays.asList(
                "2019 2019-01-01T00:00:00.000000Z year",
                "2019-02 2019-02-01T00:00:00.000000Z month",
                "2019-01-11 2019-01-11T00:00:00.000000Z day",
                "2019-01-01T12:34Z 2019-01-01T12:34:00.000000Z minute",
                "2019-01-01T12:34:56Z 2019-01-01T12:34:56.000000Z second",
                "2019-01-01T12:34:56.1Z 2019-01-01T12:34:56.100000Z milliseconds",
                "2019-01-01T12:34:56.12Z 2019-01-01T12:34:56.120000Z milliseconds",
                "2019-01-01T12:34:56.123Z 2019-01-01T12:34:56.123000Z milliseconds",
                "2019-01-01T12:34:56.1234Z 2019-01-01T12:34:56.123400Z microseconds",
                "2019-01-01T12:34:56.12345Z 2019-01-01T12:34:56.123450Z microseconds",
                "2019-01-01T12:34:56.123456Z 2019-01-01T12:34:56.123456Z microseconds"
        ).forEach(l -> {
            String a[] = l.split("\\s+", 3);
            OaiTimestamp actual = OaiTimestamp.of(a[0]);
            System.out.println(a[0] + " = " + actual);
            assertThat(a[0], actual.getTimestamp(), is(a[1]));
            assertThat(a[0], actual.getTruncate(), is(a[2]));
        });
    }
}
