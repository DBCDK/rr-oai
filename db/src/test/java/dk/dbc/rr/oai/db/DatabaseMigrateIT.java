/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-db
 *
 * rr-oai-db is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-db is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.db;

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class DatabaseMigrateIT {

    static final DBCPostgreSQLContainer dbcPostgreSQLContainer = makePostgresContainer();

    @Test
    @Timeout(value = 10, unit = java.util.concurrent.TimeUnit.SECONDS)
    void migrate() throws Exception {
        System.out.println("migrate");
        DataSource ds = makeDataSource();
        DatabaseMigrate.migrate(ds);

        int expected = countMigrations();

        try (Connection connection = ds.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery("SELECT COUNT(*) AS count FROM flyway_schema_history")) {
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                assertThat(count, is(expected));
                return; // Everything is OK!
            }
        }
        Assertions.fail("Could not apply migrations or count applied migrations");
    }

    private int countMigrations() {
        File directory = new File("src/main/resources/" + DatabaseMigrate.MIGRATION_LOCATION);
        System.out.println("directory = " + directory);
        if (directory.isDirectory()) {
            String[] sqlFiles = directory.list((f, s) -> s.contains("__") && s.endsWith(".sql")); // All migrations
            System.out.println("sqlFiles = " + Arrays.toString(sqlFiles));
            return sqlFiles.length;
        }
        throw new IllegalArgumentException("Cannot find migration directory");
    }

    private static DBCPostgreSQLContainer makePostgresContainer() {
        final DBCPostgreSQLContainer postgreSQLContainer = new DBCPostgreSQLContainer();
        postgreSQLContainer.start();
        postgreSQLContainer.exposeHostPort();
        return postgreSQLContainer;
    }

    private static DataSource makeDataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUser(dbcPostgreSQLContainer.getUsername());
        ds.setPassword(dbcPostgreSQLContainer.getPassword());
        ds.setServerNames(new String[] {dbcPostgreSQLContainer.getHost()});
        ds.setPortNumbers(new int[] {dbcPostgreSQLContainer.getHostPort()});
        ds.setDatabaseName(dbcPostgreSQLContainer.getDatabaseName());
        return ds;
    }
}
