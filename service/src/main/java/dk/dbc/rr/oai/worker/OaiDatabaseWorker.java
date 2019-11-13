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

import dk.dbc.rr.oai.Config;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.sql.DataSource;

import static java.util.Collections.EMPTY_SET;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class OaiDatabaseWorker {

    @Inject
    public Config config;

    @Resource(lookup = "jdbc/rawrepo-oai")
    public DataSource dataSource;

    public Set<String> getSetsForId(String identifier) throws SQLException {
        String[] parts = identifier.split("[-:]", 2);
        if (parts.length != 2)
            return EMPTY_SET;
        try (Connection connection = dataSource.getConnection() ;
             PreparedStatement stmt = connection.prepareStatement("SELECT setspec FROM oairecordsets WHERE pid=?")) {
            stmt.setString(1, parts[0] + ":" + parts[1]);
            HashSet<String> set = new HashSet<>();
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    set.add(resultSet.getString(1));
                }
            }
            return set;
        }
    }

}
