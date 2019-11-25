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
public class OaiSet {

    private final String setspec;
    private final String setname;
    private final String description;

    public OaiSet(String setspec, String setname, String description) {
        this.setspec = setspec;
        this.setname = setname;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getSetname() {
        return setname;
    }

    public String getSetspec() {
        return setspec;
    }
}
