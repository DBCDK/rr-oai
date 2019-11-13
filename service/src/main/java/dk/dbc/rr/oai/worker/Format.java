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

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class Format {

    private final String prefix;
    private final String schema;
    private final String namespace;

    public Format(String prefix, String schema, String namespace) {
        this.prefix = prefix;
        this.schema = schema;
        this.namespace = namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSchema() {
        return schema;
    }

    public String getNamespace() {
        return namespace;
    }
}
