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

import dk.dbc.rr.oai.DB;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@RunWith(Parameterized.class)
public class OaiTimestampIT extends DB {

    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                test("2019", "2019-01-01", 0),
                test("2019-01-01", "2019", 0),
                test("2019-01-01", "2019-02-01", -1),
                test("2019-01-01", "2019-01-01", 0),
                test("2019-02-01", "2019-01-01", 1)
        );
    }

    private static Object[] test(Object... objs) {
        return objs;
    }

    private final String from;
    private final String until;
    private final int expected;

    public OaiTimestampIT(String from, String until, int expected) {
        this.from = from;
        this.until = until;
        this.expected = expected;
    }

    @Test(timeout = 2_000L)
    public void test() throws Exception {
        System.out.println("test " + from + " - " + until);
        OaiTimestamp tsFrom = OaiTimestamp.of(from);
        OaiTimestamp tsUntil = OaiTimestamp.of(until);
        assertThat(tsFrom.compareTimeStamp(ds, tsUntil), is(expected));
    }

}
