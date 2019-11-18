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
package dk.dbc.rr.oai.worker;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class OaiDatabaseMetadata {

    private static final Logger log = LoggerFactory.getLogger(OaiDatabaseMetadata.class);

    @Resource(lookup = "jdbc/rawrepo-oai")
    public DataSource dataSource;

    private List<Format> formats;
    private Set<String> prefixes;
    private List<OaiSet> sets;

    @PostConstruct
    public void init() {
        try {
            this.formats = unmodifiableList(listFormats());
            this.prefixes = this.formats.stream()
                    .map(Format::getPrefix)
                    .collect(toSet());
            this.sets = unmodifiableList(listSet());
        } catch (SQLException ex) {
            log.error("Error building formats lists: {}", ex.getMessage());
            log.debug("Error building formats lists: ", ex);
            throw new EJBException("Cannot get formats list during startup");
        }
    }

    public List<Format> getFormats() {
        return formats;
    }

    public boolean knownPrefix(String prefix) {
        return prefixes.contains(prefix);
    }

    public List<OaiSet> getSets() {
        return sets;
    }

    private List<Format> listFormats() throws SQLException {
        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT prefix, schema, namespace FROM oaiformats ORDER BY prefix")) {
            ArrayList<Format> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(new Format(resultSet.getString(1),
                                    resultSet.getString(2),
                                    resultSet.getString(3)));
            }
            return list;
        }
    }

    private List<OaiSet> listSet() throws SQLException {
        try (Connection connection = dataSource.getConnection() ;
             Statement stmt = connection.createStatement() ;
             ResultSet resultSet = stmt.executeQuery("SELECT setspec, setname, description FROM oaisets ORDER BY setspec")) {
            ArrayList<OaiSet> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(new OaiSet(resultSet.getString(1),
                                    resultSet.getString(2),
                                    resultSet.getString(3)));
            }
            return list;
        }

    }

}
