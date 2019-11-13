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

import java.time.Instant;
import java.util.Map;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DBIT extends DB {

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println("testCase");

        insert("a:1").deleted().set("bkm=", "nat", "art=2020-01-01T00:00:00Z").commit();

        Map<String, Entry> dump = databaseDump();
        System.out.println("dump = " + dump);
        Entry a1 = dump.get("a:1");
        assertThat(a1.get("unknown"), approx(Instant.EPOCH, 0));
        assertThat(a1.get("nat"), nullValue());
        assertThat(a1.get("bkm"), approxNow(100));
        assertThat(a1.get("art"), approx("2020-01-01T00:00:00Z", 0));
    }
}
