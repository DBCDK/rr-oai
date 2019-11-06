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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DatabaseMigratorIT extends DB {

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println("testCase");

        loadData("[{\"pid\":\"a\",\"sets\":[\"bkm\",\"!nat\"]}," +
                 "{\"pid\":\"b\",\"deleted\":true}]");

        insert("c").changed("2020-01-01T12:34:56.543210Z").deleted().set("bkm").commit();
        insert("c").changed("2020-01-01T12:34:56.543210Z").deleted().set("!bkm").commit(); // overwrite

        try (Connection connection = ds.getConnection() ;
             Statement stmtRecords = connection.createStatement() ;
             Statement stmtRecordSets = connection.createStatement() ;
             ResultSet resRecords = stmtRecords.executeQuery("SELECT deleted, COUNT(*) FROM oairecords GROUP BY deleted") ;
             ResultSet resRecordSets = stmtRecordSets.executeQuery("SELECT gone, COUNT(*) FROM oairecordsets GROUP BY gone")) {

            HashMap<Boolean, Integer> res = new HashMap<>();
            while (resRecords.next()) {
                res.put(resRecords.getBoolean(1), resRecords.getInt(2));
            }
            assertThat("not deleted", res.get(false), is(1)); // a
            assertThat("deleted", res.get(true), is(2));  // b,c

            res.clear();
            while (resRecordSets.next()) {
                res.put(resRecordSets.getBoolean(1), resRecordSets.getInt(2));
            }
            assertThat("not gone", res.get(false), is(1));  // a/bkm
            assertThat("gone", res.get(true), is(2)); // a/nat, c/bkm
        }
    }
}
