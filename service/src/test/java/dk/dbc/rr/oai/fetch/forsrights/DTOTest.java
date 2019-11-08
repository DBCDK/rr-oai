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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class DTOTest {

    @Test(timeout = 2_000L)
    public void testHasRight() throws Exception {
        System.out.println("testHasRight");

        String x = "{\"forsRightsResponse\":" +
                   "  {" +
                   "    \"ressource\":" +
                   "      [" +
                   "        {" +
                   "          \"name\":{\"$\":\"foo\",\"@\":\"fr\"}," +
                   "          \"right\":" +
                   "            [" +
                   "              {\"$\":\"500\",\"@\":\"fr\"}," +
                   "              {\"$\":\"505\",\"@\":\"fr\"}," +
                   "              {\"$\":\"550\",\"@\":\"fr\"}" +
                   "           ]" +
                   "        }," +
                   "        {" +
                   "          \"name\":{\"$\":\"bar\",\"@\":\"fr\"}," +
                   "          \"right\":" +
                   "            [" +
                   "              {\"$\":\"500\",\"@\":\"fr\"}," +
                   "              {\"$\":\"505\",\"@\":\"fr\"}," +
                   "              {\"$\":\"550\",\"@\":\"fr\"}" +
                   "           ]" +
                   "        }" +
                   "      ],\"@\":\"fr\"}," +
                   "    \"@namespaces\":" +
                   "      {" +
                   "        \"fr\":\"http:\\/\\/oss.dbc.dk\\/ns\\/forsrights\"" +
                   "      }" +
                   "}";

        DTO dto = BadgerFishReader.O.readValue(x, DTO.class);

        assertThat(dto.hasRight("*"), is(true));
        assertThat(dto.hasRight("default"), is(true));
        assertThat(dto.hasRight("foo,300"), is(false));
        assertThat(dto.hasRight("foo,500"), is(true));
        assertThat(dto.hasRight("foo,550"), is(true));
        assertThat(dto.hasRight("bar,500"), is(true));
    }
}
