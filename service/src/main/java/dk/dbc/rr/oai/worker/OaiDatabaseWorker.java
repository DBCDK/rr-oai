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
import dk.dbc.rr.oai.io.OaiIdentifier;
import dk.dbc.rr.oai.io.OaiResumptionToken;
import dk.dbc.rr.oai.io.OaiTimestamp;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class OaiDatabaseWorker {

    private static final Logger log = LoggerFactory.getLogger(OaiDatabaseWorker.class);

    private static final String SELECT_OAI_RECORDS =
            "SELECT pid, deleted, changed, setspec, gone" +
            " FROM oairecords" +
            " JOIN oairecordsets USING (pid)";

    @Inject
    public Config config;

    @Resource(lookup = "jdbc/rawrepo-oai")
    public DataSource dataSource;

    /**
     * Returns an identifier or null if no record is available
     *
     * @param id id of record
     * @return identifier or null
     * @throws SQLException If the record cannot be fetched
     */
    public OaiIdentifier getIdentifier(String id) throws SQLException {
        String sql = SELECT_OAI_RECORDS + " WHERE pid = ?";
        log.debug("sql = {}", sql);
        try (Connection connection = dataSource.getConnection() ;
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            LinkedList<OaiIdentifier> list = listOfIdentifiersFromStatement(stmt, 1);
            if (list.isEmpty()) {
                return null;
            }
            return list.get(0);
        }
    }

    /**
     * List identifiers starting with the one in the token
     * <p>
     * Resulting list is up to {@link Config#getMaxRowsPrRequest()} + 1 long
     * If it is longer than configured, then the last should be removed and
     * converted into a new resumption token
     *
     * @param token       resumptionToken as supplied from user
     * @param allowedSets which sets to use in none has been declared originally
     * @return List of identifiers
     * @throws SQLException if identifiers couldn't be fetched from the database
     */
    @Timed
    public LinkedList<OaiIdentifier> listIdentifiers(OaiResumptionToken token, Set<String> allowedSets) throws SQLException {
        return listIdentifiers(token.getFrom(), token.getSegmentStart(), token.getSegmentId(), token.getUntil(), makeSetsSet(token, allowedSets));
    }

    private static Set<String> makeSetsSet(OaiResumptionToken token, Set<String> allowedSets) {
        String set = token.getSet();
        if (set == null)
            return allowedSets;
        return singleton(set);
    }

    /**
     * List identifiers starting with the specification supplied by the user
     * <p>
     * Resulting list is up to {@link Config#getMaxRowsPrRequest()} + 1 long
     * If it is longer than configured, then the last should be removed and
     * converted into a new resumption token
     *
     * @param from  Timestamp to start from (inclusive)
     * @param until Timestamp to end at (inclusive)
     * @param set   dataset to take identifiers from
     * @return List of identifiers
     * @throws SQLException if identifiers couldn't be fetched from the database
     */
    @Timed
    public LinkedList<OaiIdentifier> listIdentifiers(OaiTimestamp from, OaiTimestamp until, Set<String> set) throws SQLException {
        return listIdentifiers(from, null, null, until, set);
    }

    private LinkedList<OaiIdentifier> listIdentifiers(OaiTimestamp from, Timestamp segmentStart, String segmentId, OaiTimestamp until, Set<String> set) throws SQLException {
        Object[] values = new Object[5 + set.size()];
        values[0] = set;
        int pos = 0;
        for (String s : set) {
            values[pos++] = s;
        }
        String sql = listRecordsSql(values, pos, from, segmentStart, segmentId, until, set.size());
        log.debug("sql = {}, values = {}", sql, Arrays.toString(values));
        try (Connection connection = dataSource.getConnection() ;
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0 ; i < values.length ; i++) {
                Object value = values[i];
                if (value == null)
                    break;
                if (value instanceof String) {
                    stmt.setString(i + 1, (String) value);
                } else if (value instanceof Timestamp) {
                    stmt.setTimestamp(i + 1, (Timestamp) value);
                } else {
                    throw new AssertionError();
                }
            }
            int maxLength = config.getMaxRowsPrRequest() + 1;
            return listOfIdentifiersFromStatement(stmt, maxLength);
        }
    }

    /**
     * Construct a list of identifiers from a prepared statement
     *
     * @param stmt      Statement that has id,deleted,changed,setspec,gone
     * @param maxLength Max length of returned list
     * @return list of idenitfiers
     * @throws SQLException if a row cannot be fetched
     */
    private LinkedList<OaiIdentifier> listOfIdentifiersFromStatement(final PreparedStatement stmt, int maxLength) throws SQLException {
        OaiIdentifier oaiIdentifier = new OaiIdentifier();
        try (ResultSet resultSet = stmt.executeQuery()) {
            LinkedList<OaiIdentifier> identifiers = new LinkedList<>();
            while (resultSet.next()) {
                String identifier = resultSet.getString(1);
                if (!identifier.equals(oaiIdentifier.getIdentifier())) {
                    if (identifiers.size() == maxLength)
                        break; // All setspecs for the wanted number has been fetched
                    boolean deleted = resultSet.getBoolean(2);
                    Timestamp changed = resultSet.getTimestamp(3);
                    oaiIdentifier = new OaiIdentifier(identifier, deleted, changed);
                    identifiers.add(oaiIdentifier);
                }
                String setSpec = resultSet.getString(4);
                boolean gone = resultSet.getBoolean(5);
                if (!gone)
                    oaiIdentifier.add(setSpec);
            }
            return identifiers;
        }
    }

    /**
     * Build an sql statement for fetching identifiers from a time slot
     *
     * @param values       Array of values to insert into the prepared statement
     * @param valueOffset  Start position in values
     * @param from         Starting timestamp
     * @param segmentStart Continue from timestamp
     * @param segmentId    Continue from identifier
     * @param until        Ending timestamp
     * @return SQL statement
     */
    private String listRecordsSql(Object[] values, int valueOffset, OaiTimestamp from, Timestamp segmentStart, String segmentId, OaiTimestamp until, int setSize) {
        StringBuilder sql = new StringBuilder();
        sql.append(SELECT_OAI_RECORDS + " WHERE setspec");
        if (valueOffset == 1) {
            sql.append(" = ?");
        } else {
            sql.append(" IN (");
            for (int i = 0 ; i < valueOffset ; i++) {
                if (i == 0) {
                    sql.append("?");
                } else {
                    sql.append(", ?");
                }
            }
            sql.append(")");
        }
        if (segmentStart != null && segmentId != null) {
            sql.append(" AND (changed > ? OR changed = ? AND pid >= ?)");
            values[valueOffset++] = segmentStart;
            values[valueOffset++] = segmentStart;
            values[valueOffset++] = segmentId;
        } else if (from != null) {
            sql.append(" AND ");
            from.sqlFrom(sql, "changed");
            values[valueOffset++] = from.getTimestamp();
        }
        if (until != null) {
            sql.append(" AND ");
            until.sqlTo(sql, "changed");
            values[valueOffset++] = until.getTimestamp();
        }
        sql.append(" ORDER BY changed, pid LIMIT ")
                .append(config.getMaxRowsPrRequest() * setSize + 1);
        return sql.toString();
    }

}
