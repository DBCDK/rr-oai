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
    public void testOf() throws Exception {
        System.out.println("testOf - make timestamp with granuality");

        Arrays.asList(
                "2019 2019-01-01T00:00:00Z year",
                "2019-02 2019-02-01T00:00:00Z month",
                "2019-01-11 2019-01-11T00:00:00Z day",
                "2019-01-01T12:34Z 2019-01-01T12:34:00Z minute",
                "2019-01-01T12:34:56Z 2019-01-01T12:34:56Z second",
                "2019-01-01T12:34:56.1Z 2019-01-01T12:34:56.100Z milliseconds",
                "2019-01-01T12:34:56.12Z 2019-01-01T12:34:56.120Z milliseconds",
                "2019-01-01T12:34:56.123Z 2019-01-01T12:34:56.123Z milliseconds",
                "2019-01-01T12:34:56.1234Z 2019-01-01T12:34:56.123400Z microseconds",
                "2019-01-01T12:34:56.12345Z 2019-01-01T12:34:56.123450Z microseconds",
                "2019-01-01T12:34:56.123456Z 2019-01-01T12:34:56.123456Z microseconds"
        ).forEach(l -> {
            String a[] = l.split("\\s+", 3);
            OaiTimestamp actual = OaiTimestamp.of(a[0]);
            String instant = actual.getTimestamp().toInstant().toString();
            System.out.println(a[0] + " = " + instant + " " + actual.getTruncate());
            assertThat(a[0], instant, is(a[1]));
            assertThat(a[0], actual.getTruncate(), is(a[2]));
        });
    }

    @Test(timeout = 2_000L)
    public void testSql() throws Exception {
        System.out.println("testSql");
        Arrays.asList(
                "2019       DATE_TRUNC('year', XXX) <= DATE_TRUNC('year', ?::TIMESTAMP)",
                "2019-01    DATE_TRUNC('month', XXX) <= DATE_TRUNC('month', ?::TIMESTAMP)",
                "2019-01-11 DATE_TRUNC('day', XXX) <= DATE_TRUNC('day', ?::TIMESTAMP)"
        ).forEach(l -> {
            String[] a = l.split("\\s+", 2);
            String actual = OaiTimestamp.of(a[0]).sql("XXX", "<=");
            assertThat(actual, is(a[1]));
        });
    }
}
