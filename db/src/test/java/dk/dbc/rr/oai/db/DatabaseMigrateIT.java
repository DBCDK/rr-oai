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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class DatabaseMigrateIT {

    public DatabaseMigrateIT() {
    }

    @Test(timeout = 10_000L)
    public void migrate() throws Exception {
        System.out.println("migrate");
        DataSource ds = makeDataSource();
        DatabaseMigrate.migrate(ds);

        try (Connection connection = ds.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT COUNT(*) AS count FROM flyway_schema_history")) {
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                assertThat(count, is(2));
                return; // Everything is OK!
            }
        }
        fail("Cound not apply migrations or count applied migrations");
    }

    private static DataSource makeDataSource() {
        String port = System.getProperty("postgresql.port");
        String user = null;
        String password = null;
        String database = "rroai";
        String host = "localhost";
        if (port == null) {
            String username = System.getProperty("user.name");
            Map<String, String> env = System.getenv();
            user = env.getOrDefault("PGUSER", username);
            password = env.getOrDefault("PGPASSWORD", username);
            database = env.getOrDefault("PGDATABASE", username);
            host = env.getOrDefault("PGHOST", "localhost");
            port = env.getOrDefault("PGPORT", "5432");
        }
        PGSimpleDataSource ds = new PGSimpleDataSource();
        if (user != null)
            ds.setUser(user);
        if (password != null)
            ds.setPassword(password);
        ds.setServerName(host);
        ds.setPortNumber(Integer.parseInt(port));
        ds.setDatabaseName(database);
        return ds;
    }
}
