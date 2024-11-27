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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.rr.oai.db.DatabaseMigrate;
import org.junit.Before;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DB {

    private static final Logger log = LoggerFactory.getLogger(DB.class);

    protected static final ObjectMapper O = new ObjectMapper();

    public static final String QUEUE_NAME = "my-queue";

    protected static final DBCPostgreSQLContainer dbcPostgreSQLContainer = makePostgresContainer();
    protected static PGSimpleDataSource dsrr = makeDataSource("rr");
    static {
        executeScript(Path.of("src", "test", "resources", "rawrepo-oai.sql"));
        executeScript(Path.of("target", "sql", "rawrepo.sql"));
        executeScript(Path.of("target", "sql", "queuerules.sql"));
    }
    protected static PGSimpleDataSource dsrroai = makeDataSource("rroai");

    private static DBCPostgreSQLContainer makePostgresContainer() {
        final DBCPostgreSQLContainer postgreSQLContainer = new DBCPostgreSQLContainer();
        postgreSQLContainer.withDatabaseName("rr");
        postgreSQLContainer.start();
        postgreSQLContainer.exposeHostPort();
        return postgreSQLContainer;
    }

    private static PGSimpleDataSource makeDataSource(String dbName) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUser(dbcPostgreSQLContainer.getUsername());
        ds.setPassword(dbcPostgreSQLContainer.getPassword());
        ds.setServerNames(new String[] {dbcPostgreSQLContainer.getHost()});
        ds.setPortNumbers(new int[] {dbcPostgreSQLContainer.getHostPort()});
        ds.setDatabaseName(dbName);
        return ds;
    }

    private static void executeScript(Path scriptPath) {
        try (Connection connection = dsrr.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(Files.readString(scriptPath));
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void initDatabase() throws SQLException {
        DatabaseMigrate.migrate(dsrroai);
        try (Connection connection = rawRepoOai() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("TRUNCATE oairecords CASCADE");
            stmt.executeUpdate("TRUNCATE oairecordsets CASCADE");
        }
        try (Connection connection = rawRepo() ;
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("TRUNCATE queue CASCADE");
            stmt.executeUpdate("TRUNCATE queueworkers CASCADE");
            stmt.executeUpdate("INSERT INTO queueworkers VALUES('" + QUEUE_NAME + "')");
        }
    }

    protected Connection rawRepo() throws SQLException {
        return dsrr.getConnection();
    }

    protected Connection rawRepoOai() throws SQLException {
        return dsrroai.getConnection();
    }

    public void queue(String resource) throws SQLException, IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (is == null)
                throw new IllegalArgumentException("Cannot find resource: " + resource);
            queue(is);
        }
    }

    public void queue(InputStream is) throws SQLException, IOException {
        Queue[] jobs = O.readValue(is, Queue[].class);
        queue(jobs);
    }

    public void queue(Queue... jobs) throws SQLException {
        try (Connection connection = rawRepo() ;
             PreparedStatement stmt = connection.prepareStatement("INSERT INTO queue(agencyId, bibliographicRecordId, worker) VALUES(?, ?, ?)")) {
            for (Queue job : jobs) {
                stmt.setInt(1, job.agencyId);
                stmt.setString(2, job.bibliographicRecordId);
                stmt.setString(3, QUEUE_NAME);
                stmt.addBatch();
            }
            int[] total = stmt.executeBatch();
            int sum = Arrays.stream(total)
                    .sum();
            log.debug("queued: {}", sum);
        }
    }

    public static class Queue {

        private final int agencyId;
        private final String bibliographicRecordId;

        @JsonCreator
        public Queue(@JsonProperty(value = "agencyId", required = false) Integer agencyId,
                     @JsonProperty(value = "bibliographicRecordId", required = true) String bibliographicRecordId) {
            this.agencyId = agencyId == null ? 870970 : agencyId;
            this.bibliographicRecordId = bibliographicRecordId;
        }

    }

}
