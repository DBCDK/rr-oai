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
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        String user = "";
        String pass = "";
        String host = "localhost";
        String port;
        String base = "rroai";
        if (portProperty != null) {
            port = portProperty;
        } else {
            String username = System.getProperty("user.name");
            System.out.println("username = " + username);
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
             PreparedStatement sets = connection.prepareStatement("INSERT INTO oairecordsets(pid, setspec, gone) VALUES(?, ?, ?)")) {
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
                boolean gone = set.startsWith("!");
                if (gone)
                    set = set.substring(1);
                sets.setString(2, set);
                sets.setBoolean(3, gone);
                sets.executeUpdate();
            }
        }
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
}
