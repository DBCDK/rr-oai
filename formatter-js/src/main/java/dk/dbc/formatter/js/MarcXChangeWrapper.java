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
package dk.dbc.formatter.js;

import dk.dbc.rawrepo.dto.RecordIdDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class MarcXChangeWrapper {

    public final String content;
    public final RecordIdDTO[] children;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public MarcXChangeWrapper(String content, RecordIdDTO[] children) {
        this.content = content;
        this.children = children;
    }
}
