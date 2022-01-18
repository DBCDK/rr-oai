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

import dk.dbc.rr.oai.db.DatabaseMigrate;
import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Startup
public class DatabaseMigrator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrator.class);

    @Resource(lookup = "jdbc/rawrepo-oai")
    DataSource ds;

    @PostConstruct
    public void init() {
        if (isReadOnly()) {
            log.info("Not migrating database - database is readonly");
            return;
        }

        log.info("Migrating database");
        DatabaseMigrate.migrate(ds);
    }

    private boolean isReadOnly() {
        try (Connection con = ds.getConnection()) {
            return con.isReadOnly();
        } catch (SQLException ex) {
            log.error("Cannot get database connection: {}", ex.getMessage());
            log.debug("Cannot get database connection: ", ex);
            throw new EJBException(ex);
        }
    }
}
