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
package dk.dbc.rr.oai.fetch.forsrights;

import dk.dbc.rr.oai.Config;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.rr.oai.fetch.forsrights.BadgerFishReader.O;
import static java.util.Collections.EMPTY_SET;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@Lock(LockType.READ)
public class ForsRights {

    private static final Logger log = LoggerFactory.getLogger(ForsRights.class);

    @Inject
    public Config config;

    /**
     * Get a set of authorized setnames for a given login
     *
     * @param triple username:group:password
     * @param ip     remote-ip
     * @return Set of rights - if empty no rights where found (bad login)
     * @throws IOException          if there's a problem parsing the ForsRights
     *                              response
     * @throws ServerErrorException If the ForsRights service is down
     */
    @CacheResult(cacheName = "forsRights",
                 exceptionCacheName = "forsRightsFailure",
                 cachedExceptions = {ClientErrorException.class,
                                     ServerErrorException.class,
                                     IOException.class})
    public Set<String> authorized(@CacheKey String triple, @CacheKey String ip) throws IOException {
        try {
            URI uri = buildForsRightsURI(triple, ip);
            log.info("Fetching forsrights from: {}", uri.toString().replaceFirst("&passwordAut=[^&]*", "&passwordAut=[REDACTED]"));
            DTO dto = getDTO(uri);
            log.debug("dto = {}", dto);
            if (dto.hasAnyRight()) {
                return config.getForsRightsRules().entrySet().stream()
                        .filter(e -> dto.hasRight(e.getKey())) // Map entries that are allowed by DTO
                        .flatMap(e -> e.getValue().stream())
                        .collect(toSet());
            } else {
                return EMPTY_SET;
            }
        } catch (ServerErrorException | IOException ex) {
            log.error("Error getting rights: {}", ex.getMessage());
            log.debug("Error getting rights: ", ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Error getting rights: {}", ex.getMessage());
            log.debug("Error getting rights: ", ex);
            throw new ServerErrorException(INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Fetch (badgerfish json) and parse into a DTO
     *
     * @param uri ForsRights url
     * @return POJO representation on the JSON content
     * @throws IOException          If parsing of JSON fails
     * @throws ServerErrorException If the ForsRights service is down
     */
    DTO getDTO(URI uri) throws IOException {
        Client client = config.getHttpClient();
        try (InputStream is = client.target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(InputStream.class)) {
            return O.readValue(is, DTO.class);
        }
    }

    /**
     * Construct a ForsRights url
     *
     * @param tripple the user:group:password string (or null)
     * @param ip      the remote ip address (or null)
     * @return url with request parameters
     */
    URI buildForsRightsURI(String tripple, String ip) {
        UriBuilder builder = config.getForsRightsUrl()
                .queryParam("action", "forsRights");
        if (tripple != null) {
            String[] parts = tripple.split(":", 3);
            if (parts.length == 3) {
                builder.queryParam("userIdAut", parts[0]);
                builder.queryParam("groupIdAut", parts[1]);
                builder.queryParam("passwordAut", parts[2]);
            } else {
                tripple = null; // Invalid format for tripple
            }
        }
        if (ip != null)
            builder.queryParam("ipAddress", ip);
        if (tripple == null && ip == null)
            throw new IllegalArgumentException("No authorization supplied");
        builder.queryParam("outputType", "json");
        return builder.build();
    }
}
