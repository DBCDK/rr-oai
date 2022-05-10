package dk.dbc.rr.oai.fetch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.idp.marshallers.request.AuthenticationRequest;
import dk.dbc.idp.marshallers.response.AuthenticationResponse;
import dk.dbc.idp.marshallers.response.Rights;
import dk.dbc.rr.oai.Config;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class IdpRights {

    private static final Logger log = LoggerFactory.getLogger(IdpRights.class);

    private static final ObjectMapper O = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Inject
    public Config config;

    private Client client;
    private URI idpUri;
    private Map<String, List<String>> rules;

    @PostConstruct
    public void init() {
        client = config.getHttpClient();
        idpUri = config.getIdpUrl().path("authorize").build();
        rules = config.getIdpRightsRules();
    }

    /**
     * Get a set of authorized setnames for a given login
     *
     * @param triple username:group:password
     * @param ip     remote-ip
     * @return Set of rights - if empty no rights where found (bad login)
     * @throws ServerErrorException If the IDP service is down
     */
    @CacheResult(cacheName = "idpRights",
                 exceptionCacheName = "idpRightsFailure",
                 cachedExceptions = {ClientErrorException.class,
                                     ServerErrorException.class,
                                     IOException.class})
    @Timed
    public Set<String> authorized(@CacheKey String triple, @CacheKey String ip) throws IOException {
        List<Rights> rights = Collections.EMPTY_LIST;

        if (triple != null) {

            String[] parts = triple.split(":", 3);
            if (parts.length != 3) {
                log.warn("Could not authorize - malformed triple");
                return Collections.EMPTY_SET;
            }

            AuthenticationRequest req = new AuthenticationRequest();
            req.setUserIdAut(parts[0]);
            req.setAgencyId(parts[1]);
            req.setPasswordAut(parts[1]);
            AuthenticationResponse res = authorize(req);

            if (!res.isAuthenticated()) {
                log.warn("Could not authorize {}/{}/[REDACTED]", parts[0], parts[1]);
                return Collections.EMPTY_SET;
            } else {
                rights = res.getRights();
                if (rights == null || rights.isEmpty()) {
                    log.warn("Could not authorize {}/{}/[REDACTED] - no rights in response", parts[0], parts[1]);
                    rights = Collections.EMPTY_LIST;
                }
                log.info("Authorized {}/{}/[REDACTED]", parts[0], parts[1]);
            }

        } else if (ip != null) {

            AuthenticationRequest req = new AuthenticationRequest();
            req.setIp(ip);
            AuthenticationResponse res = authorize(req);

            if (res.isAuthenticated()) {
                rights = res.getRights();
                if (rights == null || rights.isEmpty()) {
                    log.warn("Could not authorize {} - no rights in response", ip);
                    rights = Collections.EMPTY_LIST;
                } else {
                    log.info("Authorized {}/[REDACTED]", ip);
                }
            }

        } else {

            log.info("No authentication supplied");
            rights = Collections.EMPTY_LIST;

        }
        // Product,Name + special '*' that everybody has
        Set<String> allRights = rights.stream()
                .map(r -> ( r.getProductName() + "," + r.getName() ).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return rules.entrySet().stream()
                .filter(e -> e.getKey().equals("*") || allRights.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toSet());
    }

    private AuthenticationResponse authorize(AuthenticationRequest req) throws IOException {
        try {
            log.debug("req = {}", req);
            String payload = O.writeValueAsString(req);
            try ( InputStream is = client.target(idpUri)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .buildPost(Entity.json(payload))
                    .invoke(InputStream.class)) {
                AuthenticationResponse resp = O.readValue(is, AuthenticationResponse.class);
                log.debug("resp = {}", resp);
                return resp;
            }
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Cannot jsonify authorize request", ex);
        }
    }

}
