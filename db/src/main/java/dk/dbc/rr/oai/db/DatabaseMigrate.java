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

import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FlyWay migrator for rawrepo-oai database
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DatabaseMigrate {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrate.class);

    static final String MIGRATION_LOCATION = "migrate/db/rr-oai";

    public static void migrate(DataSource ds) {
        Flyway flyway = Flyway.configure()
                .locations(MIGRATION_LOCATION)
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("1"))
                .dataSource(ds)
                .load();
        MigrationInfoService info = flyway.info();
        Stream.of(info.applied())
                .forEach(m -> log.info("Skipping migration {}, already applied", m.getScript()));
        Stream.of(info.pending())
                .forEach(m -> log.info("Applying migration {}", m.getScript()));
        flyway.migrate();
    }

}
