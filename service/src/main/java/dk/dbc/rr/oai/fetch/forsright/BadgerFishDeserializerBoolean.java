/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-solr
 *
 * opensearch-solr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-solr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.fetch.forsright;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Locale;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class BadgerFishDeserializerBoolean extends StdDeserializer<Boolean> {

    private static final long serialVersionUID = -2564149563067859892L;

    public BadgerFishDeserializerBoolean() {
        super(String.class);
    }

    @SuppressFBWarnings(value = {"NP_BOOLEAN_RETURN_NULL"})
    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {

        if (parser.isExpectedStartObjectToken()) {
            Boolean value = null;
            parser.nextToken(); // Skip '{'
            while (!parser.hasToken(JsonToken.END_OBJECT)) {
                JsonToken token = parser.currentToken();
                switch (token) {
                    case FIELD_NAME:
                        String field = parser.getCurrentName();
                        token = parser.nextToken();
                        if (token != JsonToken.VALUE_STRING) {
                            context.reportInputMismatch(String.class, "Expected badgerfish type input string after %s", field);
                        }
                        if (field.equals("$")) {
                            String boolValue = parser.getValueAsString();
                            switch (boolValue.toLowerCase(Locale.ROOT)) {
                                case "1":
                                case "yes":
                                case "true":
                                case "t":
                                    value = true;
                                    break;
                                case "0":
                                case "no":
                                case "false":
                                case "f":
                                    value = false;
                                    break;
                                default:
                                    context.reportInputMismatch(String.class, "Expected badgerfish type cannot make: `%s' into a boolean", boolValue);
                            }
                        } else if (field.startsWith("@")) {
                            // Namespace
                            break;
                        } else {
                            context.reportInputMismatch(String.class, "Expected badgerfish type unexpected property: %s", field);
                        }
                        break;
                    case END_OBJECT:
                        break;
                    default:
                        context.reportInputMismatch(String.class, "Expected badgerfish type input");
                        break;
                }
                parser.nextToken();
            }
            return value;
        }
        return null;
    }
}
