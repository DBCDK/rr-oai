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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
@Path("api/status")
public class StatusBean {

    private static final ObjectMapper O = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(StatusBean.class);

    @Resource(lookup = "jdbc/rawrepo-oai")
    DataSource ds;

    @Inject
    Config config;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        Optional<String> err = this.testDb()
                .or(this::testFormatService);
        return err.map(Resp::new)
                .orElse(Resp.OK)
                .asResponse();
    }

    private Optional<String> testDb() {
        try ( Connection connection = ds.getConnection();   Statement stmt = connection.createStatement();   ResultSet resultSet = stmt.executeQuery("SELECT NOW()")) {
            if (resultSet.next())
                return Optional.empty();
            log.error("No result from SELECT NOW()");
        } catch (SQLException | RuntimeException ex) {
            log.error("Error pinging database: {}", ex.getMessage());
            log.debug("Error pinging database: ", ex);
        }
        return Optional.of("Error pinging database");
    }

    private Optional<String> testFormatService() {
        try {
            URI uri = config.getFormatServiceUri().path("api/status").build();
            log.debug("uri = {}", uri);
            String json = config.getHttpClient().target(uri)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(String.class);
            JsonNode tree = O.readTree(json);
            if (tree.get("ok").asBoolean(false))
                return Optional.empty();
            log.error("Error parsing formatter response: {}", json);
        } catch (IOException | RuntimeException ex) {
            log.error("Error pinging formatter: {}", ex.getMessage());
            log.debug("Error pinging formatter: ", ex);
        }
        return Optional.of("Error pinging formatter");
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class Resp {

        public static final Resp OK = new Resp();

        public boolean ok;
        public String text;

        private Resp() {
            this.ok = true;
            this.text = "Success";
        }

        public Resp(String diag) {
            this.ok = false;
            this.text = diag;
            log.error("Answering with diag: {}", diag);
        }

        public Response asResponse() {
            return Response.status(ok ? Status.OK : Status.INTERNAL_SERVER_ERROR)
                    .entity(this)
                    .build();
        }

    }

}
