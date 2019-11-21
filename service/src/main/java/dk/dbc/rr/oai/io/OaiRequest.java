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
package dk.dbc.rr.oai.io;

import dk.dbc.oai.pmh.OAIPMH;
import dk.dbc.oai.pmh.OAIPMHerrorType;
import dk.dbc.oai.pmh.OAIPMHerrorcodeType;
import dk.dbc.oai.pmh.VerbType;
import java.util.List;
import java.util.function.Function;
import javax.ws.rs.core.MultivaluedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.oai.pmh.OAIPMHerrorcodeType.*;
import static dk.dbc.rr.oai.io.OaiResponse.O;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiRequest {

    private static final Logger log = LoggerFactory.getLogger(OaiRequest.class);

    private final String identity;

    private final OaiTimestamp from;
    private final String identifier;
    private final String metadataPrefix;
    private final OaiResumptionToken resumptionToken;
    private final String set;
    private final OaiTimestamp until;
    private final VerbType verb;

    public OaiRequest(String identity, OaiTimestamp from, String identifier, String metadataPrefix, OaiResumptionToken resumptionToken, String set, OaiTimestamp until, VerbType verb) {
        this.identifier = identifier;
        this.identity = identity;
        this.from = from;
        this.metadataPrefix = metadataPrefix;
        this.resumptionToken = resumptionToken;
        this.set = set;
        this.until = until;
        this.verb = verb;
    }

    /**
     * Non standard request value - identity (login triple user:group:pass)
     *
     * @return Identification
     */
    public String getIdentity() {
        return identity;
    }

    /**
     * Request parameter
     * <p>
     * From query-string or resumption-token
     *
     * @return request parameter or null if unset
     */
    public OaiTimestamp getFrom() {
        return from;
    }

    /**
     * Request parameter
     *
     * @return request parameter or null if unset
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Request parameter (parsed)
     *
     * @return request parameter or null if unset
     */
    public OaiResumptionToken getResumptionToken() {
        return resumptionToken;
    }

    /**
     * Request parameter
     *
     * @return request parameter or null if unset
     */
    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    /**
     * Request parameter
     * <p>
     * From query-string or resumption-token
     *
     * @return request parameter or null if unset
     */
    public String getSet() {
        return set;
    }

    /**
     * Request parameter
     * <p>
     * From query-string or resumption-token
     *
     * @return request parameter or null if unset
     */
    public OaiTimestamp getUntil() {
        return until;
    }

    /**
     * Request parameter
     *
     * @return request parameter or null if unset
     */
    public VerbType getVerb() {
        return verb;
    }

    @Override
    public String toString() {
        return "OaiRequest{" + "identity=" + ( identity == null ? "UNSET" : "SET" ) + ", from=" + from + ", identifier=" + identifier + ", resumptionToken=" + resumptionToken + ", metadataPrefix=" + metadataPrefix + ", set=" + set + ", until=" + until + ", verb=" + verb + '}';
    }

    static class Parser {

        private static final String IDENTITY = "identity";

        private static final String FROM = "from";
        private static final String IDENTIFIER = "identifier";
        private static final String METADATA_PREFIX = "metadataPrefix";
        private static final String RESUMPTION_TOKEN = "resumptionToken";
        private static final String RESUMPTION_TOKEN_IS_SET = " when resumptionToken is set";
        private static final String SET = "set";
        private static final String UNTIL = "until";
        private static final String VERB = "verb";

        private final OAIPMH oaipmh;
        private boolean ok = true;
        private String identity = null;

        private OaiTimestamp from = null;
        private String identifier = null;
        private String metadataPrefix = null;
        private String resumptionToken = null;
        private OaiResumptionToken parsedResumptionToken = null;
        private String set = null;
        private OaiTimestamp until = null;
        private VerbType verb = null;

        Parser(OAIPMH oaipmh) {
            this.oaipmh = oaipmh;
        }

        Parser parse(MultivaluedMap<String, String> map) {
            map.forEach((name, values) -> {
                switch (name) {
                    case IDENTITY:
                        identity = getValue(name, values, s -> s, BAD_ARGUMENT);
                        break;
                    case FROM:
                        from = getValue(name, values, OaiTimestamp::of, BAD_ARGUMENT);
                        break;
                    case IDENTIFIER:
                        identifier = getValue(name, values, s -> s, BAD_ARGUMENT);
                        break;
                    case METADATA_PREFIX:
                        metadataPrefix = getValue(name, values, s -> s, BAD_ARGUMENT);
                        break;
                    case RESUMPTION_TOKEN:
                        resumptionToken = getValue(name, values, s -> s, BAD_ARGUMENT);
                        break;
                    case SET:
                        set = getValue(name, values, s -> s, BAD_ARGUMENT);
                        break;
                    case UNTIL:
                        until = getValue(name, values, OaiTimestamp::of, BAD_ARGUMENT);
                        break;
                    case VERB:
                        verb = getValue(name, values, VerbType::fromValue, BAD_VERB);
                        break;
                    default:
                        setError(BAD_ARGUMENT, "argument: " + name + " is unknown");
                        break;
                }
            });
            return this;
        }

        Parser parseResumptionToken(Function<String, OaiResumptionToken> mapper) {
            if (resumptionToken != null)
                parsedResumptionToken = mapper.apply(resumptionToken);
            return this;
        }

        Parser validateArguments() {
            if (verb != null) {
                switch (verb) {
                    case GET_RECORD:
                        valudateArgumentsGetRecord();
                        break;
                    case IDENTIFY:
                        validateArgumentsIdentify();
                        break;
                    case LIST_IDENTIFIERS:
                        validateArgumentsListIdentifiersOrRecords();
                        break;
                    case LIST_METADATA_FORMATS:
                        validateArgumentsListMetadataFormats();
                        break;
                    case LIST_RECORDS:
                        validateArgumentsListIdentifiersOrRecords();
                        break;
                    case LIST_SETS:
                        validateArgumentsListSets();
                        break;
                }
            }
            return this;
        }

        private void valudateArgumentsGetRecord() {
            ensureSet(IDENTIFIER, identifier);
            ensureSet(METADATA_PREFIX, metadataPrefix);

            ensureUnset(FROM, from);
            ensureUnset(RESUMPTION_TOKEN, resumptionToken);
            ensureUnset(SET, set);
            ensureUnset(UNTIL, until);
        }

        private void validateArgumentsIdentify() {
            ensureUnset(FROM, from);
            ensureUnset(IDENTIFIER, identifier);
            ensureUnset(METADATA_PREFIX, metadataPrefix);
            ensureUnset(RESUMPTION_TOKEN, resumptionToken);
            ensureUnset(SET, set);
            ensureUnset(UNTIL, until);
        }

        private void validateArgumentsListIdentifiersOrRecords() {
            ensureSet(METADATA_PREFIX, metadataPrefix);

            ensureUnset(IDENTIFIER, identifier);
            if (resumptionToken != null) {
                ensureUnset(FROM, from, RESUMPTION_TOKEN_IS_SET);
                ensureUnset(SET, set, RESUMPTION_TOKEN_IS_SET);
                ensureUnset(UNTIL, until, RESUMPTION_TOKEN_IS_SET);
                if (parsedResumptionToken == null) {
                    error(BAD_RESUMPTION_TOKEN, "Invalid or expired resumptionToken");
                    ok = false;
                    resumptionToken = null;
                }
            }
        }

        private void validateArgumentsListMetadataFormats() {
            ensureUnset(FROM, from);
            ensureUnset(METADATA_PREFIX, metadataPrefix);
            ensureUnset(RESUMPTION_TOKEN, resumptionToken);
            ensureUnset(SET, set);
            ensureUnset(UNTIL, until);
        }

        private void validateArgumentsListSets() {
            ensureUnset(FROM, from);
            ensureUnset(IDENTIFIER, identifier);
            ensureUnset(METADATA_PREFIX, metadataPrefix);
            ensureUnset(SET, set);
            ensureUnset(UNTIL, until);
            if (resumptionToken != null)
                error(BAD_RESUMPTION_TOKEN, "Invalid or expired resumptionToken");
        }

        OaiRequest asOaiRequest() {
            if (!ok)
                return null;
            return new OaiRequest(identity, from, identifier, metadataPrefix, parsedResumptionToken, set, until, verb);
        }

        private <T> T getValue(String name, List<String> values, Function<String, T> op, OAIPMHerrorcodeType errorCode) {
            if (values.size() > 1) {
                error(errorCode, "argument: " + name + " is declared multiple times");
                return null;
            }
            String value = values.get(0).trim();
            if (!value.isEmpty()) {
                try {
                    T t = op.apply(value);
                    if (t != null)
                        return t;
                } catch (RuntimeException ex) {
                    log.error("Error getting value for {}: {}", name, ex.getMessage());
                    log.debug("Error getting value for {}: ", name, ex);
                }
            }
            error(errorCode, "argument: " + name + " contains an invalid value");
            return null;
        }

        private void error(OAIPMHerrorcodeType errorCode, String message) {
            OAIPMHerrorType err = O.createOAIPMHerrorType();
            err.setCode(errorCode);
            err.setValue(message);
            oaipmh.getErrors().add(err);
            ok = false;
        }

        private <T> T setError(OAIPMHerrorcodeType errorCode, String msg) {
            error(errorCode, msg);
            return null;
        }

        private void ensureUnset(String name, Object value) {
            ensureUnset(name, value, "");
        }

        private void ensureUnset(String name, Object value, String extra) {
            if (value != null) {
                ok = false;
                setError(BAD_ARGUMENT, "argument: " + name + " is not allowed for verb: " + verb.value() + extra);
            }
        }

        private void ensureSet(String name, Object value) {
            ensureSet(name, value, "");
        }

        private void ensureSet(String name, Object value, String extra) {
            if (value == null) {
                ok = false;
                setError(BAD_ARGUMENT, "argument: " + name + " is required for verb: " + verb.value() + extra);
            }
        }
    }
}
