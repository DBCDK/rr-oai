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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.junit.Test;

import static dk.dbc.rr.oai.setmatcher.BeanFactory.*;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class WorkerIT extends DB {

    @Test(timeout = 2_000L)
    public void testWritingToDatabase() throws Exception {
        System.out.println("testWritingToDatabase");
        try (Connection connection = rawRepoOai()) {
            Worker.setPidInDatabase("a", false, Arrays.asList("BKM", "NAT"), connection);
            assertThat(setsFor("a", connection), expects("BKM=false", "NAT=false"));
            Worker.setPidInDatabase("a", false, Arrays.asList("ONL", "NAT"), connection);
            assertThat(setsFor("a", connection), expects("BKM=true", "NAT=false", "ONL=false"));
            Worker.setPidInDatabase("a", true, Arrays.asList(), connection);
            assertThat(setsFor("a", connection), expects("BKM=true", "NAT=true", "ONL=true"));
        }
        try (Connection connection = rawRepoOai()) {
            Worker.setPidInDatabase("b", false, Arrays.asList(), connection);
            assertThat(setsFor("b", connection), expects());
        }
    }

    @Test(timeout = 30_000L)
    public void testWorkerInParallel() throws Exception {
        System.out.println("testWorkerInParallel");
        Config config = newConfig();
        Worker worker = newWorker(config, dsrr, dsrroai);
        queue("WorkerIT/queue-5.json");
        worker.init();
        try (Connection connection = rawRepo()) {
            while (countQueue(connection) > 0) {
                Thread.sleep(100);
            }
        }
        worker.destroy();
    }

    /**
     * setsFor matcher
     *
     * @param strings list of setname={gone?}
     * @return matcher
     */
    private Matcher<Map<String, Boolean>> expects(String... strings) {
        Map<String, Boolean> expected = Stream.of(strings)
                .map(s -> s.split("="))
                .collect(toMap(a -> a[0].toLowerCase(Locale.ROOT), a -> Boolean.parseBoolean(a[1])));
        return is(expected);
    }

    private int countQueue(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT COUNT(*) FROM queue")) {
            if (resultSet.next())
                return resultSet.getInt(1);
        }
        return Integer.MAX_VALUE;
    }

    private Map<String, Boolean> setsFor(String pid, Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT setspec, gone FROM oairecordsets WHERE pid = ?")) {
            stmt.setString(1, pid);
            try (ResultSet resultSet = stmt.executeQuery()) {
                HashMap<String, Boolean> ret = new HashMap<>();
                while (resultSet.next()) {
                    ret.put(resultSet.getString(1),
                            resultSet.getBoolean(2));
                }
                return ret;
            }
        }
    }
}
