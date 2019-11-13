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
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.sql.Types.TIMESTAMP;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DB {

    protected static final ObjectMapper O = new ObjectMapper();

    protected PGSimpleDataSource ds;

    @Before
    public void initDatabase() throws SQLException {
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
        ds = new PGSimpleDataSource();
        ds.setUser(user);
        ds.setPassword(pass);
        ds.setServerName(host);
        ds.setPortNumber(Integer.parseUnsignedInt(port));
        ds.setDatabaseName(base);

        DatabaseMigrate.migrate(ds);
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

    private void loadData(DatabaseInput input) throws SQLException {
        StringBuilder recordsSql = new StringBuilder();
        recordsSql.append("INSERT INTO oairecords(pid");
        if (input.getChanged() != null)
            recordsSql.append(", changed");
        if (input.getDeleted() != null)
            recordsSql.append(", deleted");
        recordsSql.append(") VALUES(?");
        if (input.getChanged() != null)
            recordsSql.append(", ?");
        if (input.getDeleted() != null)
            recordsSql.append(", ?");
        recordsSql.append(")");

        try (Connection connection = ds.getConnection() ;
             PreparedStatement deleteSets = connection.prepareStatement("DELETE FROM oairecordsets WHERE pid=?") ;
             PreparedStatement deleteRecords = connection.prepareStatement("DELETE FROM oairecords WHERE pid=?") ;
             PreparedStatement records = connection.prepareStatement(recordsSql.toString()) ;
             PreparedStatement sets = connection.prepareStatement("INSERT INTO oairecordsets(pid, setspec, vanished) VALUES(?, ?, ?)")) {
            deleteSets.setString(1, input.getPid());
            deleteSets.executeUpdate();
            deleteRecords.setString(1, input.getPid());
            deleteRecords.executeUpdate();

            int p = 0;
            records.setString(++p, input.getPid());
            if (input.getChanged() != null)
                records.setTimestamp(++p, Timestamp.from(Instant.parse(input.getChanged())));
            if (input.getDeleted() != null)
                records.setBoolean(++p, input.isDeleted());
            records.executeUpdate();

            sets.setString(1, input.getPid());
            for (String set : input.getSets()) {
                String[] parts = set.split("=", 2);
                if (parts.length == 2) {
                    sets.setString(2, parts[0]);
                    if (parts[1].isEmpty() || parts[1].equalsIgnoreCase("now")) {
                        sets.setTimestamp(3, Timestamp.from(Instant.now()));
                    } else {
                        sets.setTimestamp(3, Timestamp.from(Instant.parse(parts[1])));
                    }
                } else {
                    sets.setString(2, set);
                    sets.setNull(3, TIMESTAMP);
                }
                sets.executeUpdate();
            }
        }
    }

    protected Map<String, Entry> databaseDump() throws SQLException {
        HashMap<String, Entry> map = new HashMap<>();
        try (Connection connection = ds.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT pid, deleted, changed, setspec, vanished FROM oairecords JOIN oairecordsets USING (pid)")) {
            while (resultSet.next()) {
                String id = resultSet.getString(1);
                boolean deleted = resultSet.getBoolean(2);
                Timestamp changed = resultSet.getTimestamp(3);
                String setspec = resultSet.getString(4);
                Timestamp vanished = resultSet.getTimestamp(5);
                map.computeIfAbsent(id, i -> new Entry(id, deleted, changed))
                        .getSetspecs()
                        .put(setspec, vanished);
            }
        }
        return map;
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

    protected DatabaseInputBuilder insert(String pid) {
        return new DatabaseInputBuilder(pid);
    }

    protected class DatabaseInputBuilder {

        private final String pid;
        private String changed;
        private Boolean deleted;
        private final List<String> sets;

        public DatabaseInputBuilder(String pid) {
            this.pid = pid;
            this.sets = new ArrayList<>();
        }

        public DatabaseInputBuilder changed(String changed) {
            this.changed = changed;
            return this;
        }

        public DatabaseInputBuilder deleted() {
            this.deleted = true;
            return this;
        }

        public DatabaseInputBuilder set(String... sets) {
            this.sets.addAll(Arrays.asList(sets));
            return this;
        }

        public void commit() throws SQLException {
            DatabaseInput input = new DatabaseInput(pid, changed, deleted);
            input.getSets().addAll(sets);
            loadData(input);
        }
    }

    private static class DatabaseInput {

        private final String pid;
        private final String changed;
        private final Boolean deleted;
        private final List<String> sets;

        @JsonCreator
        private DatabaseInput(@JsonProperty(value = "pid", required = true) String pid,
                              @JsonProperty(value = "changed", required = false) String changed,
                              @JsonProperty(value = "deleted", required = false) Boolean deleted) {
            this.pid = pid;
            this.changed = changed;
            this.deleted = deleted;
            this.sets = new ArrayList<>();
        }

        public String getPid() {
            return pid;
        }

        public String getChanged() {
            return changed;
        }

        public Boolean getDeleted() {
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
            return "DatabaseInput{" + "pid=" + pid + ", changed=" + changed + ", deleted=" + deleted + ", sets=" + sets + '}';
        }
    }

    protected static class Entry {

        private final String id;
        private final boolean deleted;
        private final Timestamp changed;
        private final Map<String, Timestamp> setspecs = new HashMap<>();

        public Entry(String id, boolean deleted, Timestamp changed) {
            this.id = id;
            this.deleted = deleted;
            this.changed = changed;
        }

        public Timestamp getChanged() {
            return changed;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public String getId() {
            return id;
        }

        public Map<String, Timestamp> getSetspecs() {
            return setspecs;
        }

        /**
         * returns timestamp of setspec
         *
         * @param setspec name of set
         * @return null if exists but null, Instant.EPOCH if it does not exist
         */
        public Timestamp get(String setspec) {
            return setspecs.containsKey(setspec) ? setspecs.get(setspec) : Timestamp.from(Instant.EPOCH);
        }

        @Override
        public String toString() {
            return "Entry{" + "id=" + id + ", deleted=" + deleted + ", changed=" + changed + ", setspecs=" + setspecs + '}';
        }
    }
}
