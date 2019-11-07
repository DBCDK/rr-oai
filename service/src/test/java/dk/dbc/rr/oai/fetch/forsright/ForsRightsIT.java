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
package dk.dbc.rr.oai.fetch.forsright;

import dk.dbc.rr.oai.Config;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import static dk.dbc.rr.oai.BeanFactory.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ForsRightsIT {

    private Config config;
    private ForsRights forsRights;

    @Before
    public void setUp() {
        this.config = newConfig();
        this.forsRights = newForsRights(config);
    }

    @Test(timeout = 2_000L)
    public void testGetDataFromService() throws Exception {
        System.out.println("testGetDataFromService");
        forsRights.config = newConfig("FORS_RIGHTS_RULES=*=nat;foo,500=onl;foo,300=bkm");
        Set<String> authorized = forsRights.authorized("aaa:bbb:ccc", "127.0.0.1");
        assertThat(authorized, hasItems("nat", "onl"));
        assertThat(authorized, not(hasItems("bkm")));
    }
}
