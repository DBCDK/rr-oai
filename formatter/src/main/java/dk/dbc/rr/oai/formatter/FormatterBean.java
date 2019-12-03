/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-formatter
 *
 * rr-oai-formatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-formatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.formatter;

import dk.dbc.formatter.js.MarcXChangeWrapper;
import dk.dbc.log.LogWith;
import java.util.UUID;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
@Path("format")
public class FormatterBean {

    private static final Logger log = LoggerFactory.getLogger(FormatterBean.class);

    @Inject
    RawRepo rr;

    @Inject
    JavaScriptPool jsPool;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response format(@QueryParam("id") String id,
                           @QueryParam("format") String format,
                           @QueryParam("sets") String sets,
                           @QueryParam("trackingId") String trackingId) {
        if (id == null || id.isEmpty())
            throw new ClientErrorException("Missing query param 'id'", Status.BAD_REQUEST);
        if (format == null || format.isEmpty())
            throw new ClientErrorException("Missing query param 'format'", Status.BAD_REQUEST);
        if (sets == null || sets.isEmpty())
            throw new ClientErrorException("Missing query param 'sets'", Status.BAD_REQUEST);

        if (trackingId == null)
            trackingId = UUID.randomUUID().toString();
        try (LogWith logWith = LogWith.track(trackingId)) {

            int agencyId;
            String recordId;
            try {
                String[] parts = id.split("[-:]", 2);
                if (parts.length != 2)
                    throw new IllegalArgumentException("Bad format");
                agencyId = Integer.parseUnsignedInt(parts[0]);
                recordId = parts[1];
                if (recordId.isEmpty())
                    throw new IllegalStateException("Empty record part");
            } catch (RuntimeException ex) {
                throw new ClientErrorException("Query param 'id' has wrong format (agency:recordid)", Status.BAD_REQUEST);
            }

            logWith.agencyId(agencyId)
                    .bibliographicRecordId(recordId);

            if (!jsPool.checkFormat(format))
                throw new ClientErrorException("Query param 'format' contains an unknown format", Status.BAD_REQUEST);
            log.debug("Fetching records");
            MarcXChangeWrapper[] records = rr.getRecordsFor(agencyId, recordId);
            log.debug("formatting");
            String response = jsPool.format(records, format, sets);

            return Response.ok()
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .entity(response)
                    .build();
        }
    }

}
