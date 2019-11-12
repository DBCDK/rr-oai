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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test that all FORS_RIGHTS_RULES exposed sets are present in the database, and
 * that all the sets in the database are exposed via FORS_RIGHTS_RULES.
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Startup
public class ForsRightsConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(ForsRightsConfigValidator.class);

    @Resource(lookup = "jdbc/rawrepo-oai")
    public DataSource rawRepoOaiDs;

    @Inject
    public Config config;

    @PostConstruct
    public void init() {
        log.info("Checking forsrights configuration");

        Set<String> allSets = new HashSet<>(config.getAllForsRightsSets());
        log.trace("allSets = {}", allSets);

        try (Connection connection = rawRepoOaiDs.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT setspec FROM oaisets")) {

            while (resultSet.next()) {
                String setSpec = resultSet.getString(1);
                log.trace("setSpec = {}", setSpec);
                if (!allSets.remove(setSpec))
                    throw new EJBException("Set `" + setSpec + "` is declared in database but not allowed for anybody");
            }

            log.trace("allSets = {}", allSets);
            if (!allSets.isEmpty())
                throw new EJBException("Set(s) `" + allSets + "` is/are declared in FORS_RIGHTS_RULES but not in the database");

        } catch (SQLException ex) {
            log.error("Cannot read setspecs: {}", ex.getMessage());
            log.debug("Cannot read setspecs: ", ex);
            throw new EJBException("Cannot check FORS_RIGHTS_RULES");
        }
    }
}
