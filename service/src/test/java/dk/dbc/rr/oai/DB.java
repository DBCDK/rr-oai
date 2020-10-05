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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.rr.oai.db.DatabaseMigrate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DB {

    protected static final ObjectMapper O = new ObjectMapper();

    protected static final PGSimpleDataSource ds = createDataSource();

    private static PGSimpleDataSource createDataSource() {
        String portProperty = System.getProperty("postgresql.port");
        String username = System.getProperty("user.name");
        String user = username;
        String pass = "";
        String host = "localhost";
        String port;
        String base = "rroai";
        if (portProperty != null) {
            port = portProperty;
        } else {
            Map<String, String> env = System.getenv();
            user = env.getOrDefault("PGUSER", username);
            pass = env.getOrDefault("PGPASSWORD", username);
            port = env.getOrDefault("PGPORT", "5432");
            host = env.getOrDefault("PGHOST", "localhost");
            base = env.getOrDefault("PGDATABASE", username);
        }
        PGSimpleDataSource ds = new PGSimpleDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                Connection connection = super.getConnection();
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET TIMEZONE='UTC'");
                }
                return connection;
            }

        };
        ds.setUser(user);
        ds.setPassword(pass);
        ds.setServerNames(new String[] {host});
        ds.setPortNumbers(new int[] {Integer.parseUnsignedInt(port)});
        ds.setDatabaseName(base);

        DatabaseMigrate.migrate(ds);
        return ds;
    }

    @Before
    public void wipeDatabase() throws SQLException {
        try (Connection connection = ds.getConnection() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("TRUNCATE oairecords CASCADE");
            stmt.executeUpdate("TRUNCATE oairecordsets CASCADE");
        }

    }

    protected void loadData(String data) throws IOException, SQLException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes(UTF_8))) {
            loadData(bis);
        }
    }

    protected void loadResource(String path) throws IOException, SQLException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null)
                throw new IllegalArgumentException("Cannot open resource: " + path);
            loadData(is);
        }
    }

    protected void loadData(InputStream is) throws IOException, SQLException {
        DatabaseInput[] inputs = O.readValue(is, DatabaseInput[].class);
        for (DatabaseInput input : inputs) {
            loadData(input);
        }
    }
    private static final Logger log = LoggerFactory.getLogger(DB.class);

    private void loadData(DatabaseInput input) throws SQLException {
        try (Connection connection = ds.getConnection() ;
             PreparedStatement deleteSets = connection.prepareStatement("DELETE FROM oairecordsets WHERE pid=?") ;
             PreparedStatement deleteRecords = connection.prepareStatement("DELETE FROM oairecords WHERE pid=?") ;
             PreparedStatement records = connection.prepareStatement("INSERT INTO oairecords(pid, deleted) VALUES(?, ?)") ;
             PreparedStatement sets = connection.prepareStatement("INSERT INTO oairecordsets(pid, setspec, gone, changed) VALUES(?, ?, ?, ?)")) {
            deleteSets.setString(1, input.getPid());
            deleteSets.executeUpdate();
            deleteRecords.setString(1, input.getPid());
            deleteRecords.executeUpdate();

            int p = 0;
            records.setString(++p, input.getPid());
            records.setBoolean(++p, input.isDeleted());
            records.executeUpdate();

            sets.setString(1, input.getPid());
            for (String set : input.getSets()) {
                boolean gone = set.startsWith("!");
                if (gone)
                    set = set.substring(1);
                String[] parts = set.split("=", 2);
                sets.setString(2, parts[0]);
                sets.setBoolean(3, gone);
                if (parts.length == 1 || parts[1].isEmpty() || parts[1].equalsIgnoreCase("now")) {
                    sets.setTimestamp(4, Timestamp.from(Instant.now()));
                } else {
                    sets.setTimestamp(4, Timestamp.from(Instant.parse(parts[1])));
                }
                sets.executeUpdate();
            }
        }
    }

    protected Map<String, Map<String, Entry>> databaseDump() throws SQLException {
        Map<String, Map<String, Entry>> map = new HashMap<>();
        try (Connection connection = ds.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT pid, deleted, changed, setspec, gone FROM oairecords JOIN oairecordsets USING (pid)")) {
            while (resultSet.next()) {
                String id = resultSet.getString(1);
                boolean deleted = resultSet.getBoolean(2);
                Timestamp changed = resultSet.getTimestamp(3);
                String setspec = resultSet.getString(4);
                boolean gone = resultSet.getBoolean(5);
                map.computeIfAbsent(id, i -> new HashMap<>())
                        .put(setspec, new Entry(deleted, gone, changed));
            }
        }
        return map;
    }

    public static class Entry {

        private final boolean deleted;
        private final boolean gone;
        private final Timestamp changed;

        public Entry(boolean deleted, boolean gone, Timestamp changed) {
            this.deleted = deleted;
            this.gone = gone;
            this.changed = changed;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public boolean isGone() {
            return gone;
        }

        public Timestamp getChanged() {
            return changed;
        }
    }

    private static final ZoneId Z = ZoneId.of("Z");

    public static Matcher<Timestamp> approxNow(long giveOrTakeMs) {
        return approx(Instant.now(), giveOrTakeMs);
    }

    public static Matcher<Timestamp> approx(Instant expected, long giveOrTakeMs) {
        return approx(expected.atZone(ZoneId.systemDefault()).withZoneSameInstant(Z), giveOrTakeMs);
    }

    public static Matcher<Timestamp> approx(ZonedDateTime expected, long giveOrTakeMs) {
        return new BaseMatcher<Timestamp>() {
            ZonedDateTime actual = null;

            @Override
            public boolean matches(Object item) {
                if (item instanceof Timestamp) {
                    actual = ( (Timestamp) item ).toLocalDateTime()
                            .atZone(ZoneId.systemDefault())
                            .withZoneSameInstant(Z);
                    return !actual.isBefore(expected.minus(giveOrTakeMs, MILLIS)) &&
                           !actual.isAfter(expected.plus(giveOrTakeMs, MILLIS));
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(expected)
                        .appendText(" give or take ")
                        .appendText(String.valueOf(giveOrTakeMs))
                        .appendText("ms (was: ")
                        .appendValue(expected)
                        .appendText(")");
            }
        };
    }

    public static Matcher<Timestamp> approx(String timespec, long giveOrTakeMs) {
        return approx(ZonedDateTime.parse(timespec), giveOrTakeMs);
    }

    @CheckReturnValue
    protected DatabaseInputBuilder insert(String pid) {
        return new DatabaseInputBuilder(pid);
    }

    protected class DatabaseInputBuilder {

        private final String pid;
        private boolean deleted;
        private final List<String> sets;

        public DatabaseInputBuilder(String pid) {
            this.pid = pid;
            this.deleted = false;
            this.sets = new ArrayList<>();
        }

        @CheckReturnValue
        public DatabaseInputBuilder deleted() {
            this.deleted = true;
            return this;
        }

        @CheckReturnValue
        public DatabaseInputBuilder set(String... sets) {
            this.sets.addAll(Arrays.asList(sets));
            return this;
        }

        public void commit() throws SQLException {
            DatabaseInput input = new DatabaseInput(pid, deleted);
            input.getSets().addAll(sets);
            loadData(input);
        }
    }

    private static class DatabaseInput {

        private final String pid;
        private final boolean deleted;
        private final List<String> sets;

        @JsonCreator
        private DatabaseInput(@JsonProperty(value = "pid", required = true) String pid,
                              @JsonProperty(value = "deleted", required = false) Boolean deleted) {
            this.pid = pid;
            if (deleted == null)
                deleted = false;
            this.deleted = deleted;
            this.sets = new ArrayList<>();
        }

        public String getPid() {
            return pid;
        }

        public boolean getDeleted() {
            return deleted;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public List<String> getSets() {
            return sets;
        }

        @Override
        public String toString() {
            return "DatabaseInput{" + "pid=" + pid + ", deleted=" + deleted + ", sets=" + sets + '}';
        }

    }
}
