/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-database-migrate-tool
 *
 * rr-oai-database-migrate-tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-database-migrate-tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import dk.dbc.rr.oai.db.DatabaseMigrate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final Pattern POSTGRES_URL_REGEX = Pattern.compile("(?:postgres(?:ql)?://)?(?:([^:@]+)(?::([^@]*))@)?([^:/]+)(?::([1-9][0-9]*))?/(.+)");

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar rr-oai-database-migrate-tool-jar-with-dependencies.jar {DATABASE_URL}");
        } else {
            DataSource ds = makeDataSource(args[0]);
            if (ds != null) {
                DatabaseMigrate.migrate(ds);
                log.info("Database migrated");
            }
        }
    }

    private static DataSource makeDataSource(String url) {
        Matcher matcher = POSTGRES_URL_REGEX.matcher(url);
        if (matcher.matches()) {
            String user = matcher.group(1);
            String pass = matcher.group(2);
            String host = matcher.group(3);
            String port = matcher.group(4);
            String base = matcher.group(5);
            PGSimpleDataSource ds = new PGSimpleDataSource();
            if (user != null)
                ds.setUser(user);
            if (pass != null)
                ds.setPassword(pass);
            ds.setServerName(host);
            if (port != null)
                ds.setPortNumber(Integer.parseUnsignedInt(port));
            ds.setDatabaseName(base);
            return ds;
        } else {
            log.error("Not a database url: {}", url);
            return null;
        }
    }
}
