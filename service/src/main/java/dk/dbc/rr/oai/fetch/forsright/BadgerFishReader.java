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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class BadgerFishReader {

    public static final ObjectMapper O = makeObjectMapper();

    private static ObjectMapper makeObjectMapper() {
        ObjectMapper o = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new BadgerFishDeserializerString());
        module.addDeserializer(Boolean.class, new BadgerFishDeserializerBoolean());
        module.addDeserializer(boolean.class, new BadgerFishDeserializerBoolean());
        module.addDeserializer(Integer.class, new BadgerFishDeserializerInteger());
        module.addDeserializer(int.class, new BadgerFishDeserializerInteger());
        o.registerModule(module);
        o.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return o;
    }
}
