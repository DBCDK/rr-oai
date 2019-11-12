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
package dk.dbc.rr.oai;

import java.util.List;
import java.util.Set;
import javax.ejb.embeddable.EJBContainer;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static dk.dbc.rr.oai.BeanFactory.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiBeanIT {

    private static final String ABNO_TRIPLE = "abno:xxx:yyy";
    private static final String AN_TRIPLE = "an:xxx:yyy";
    private static final String UNAUTHORIZED_TRIPLE = "unknown:xxx:yyy";

    private OaiBean oaiBean;

    @Before
    public void setUp() {
        this.oaiBean = newOaiBean(newConfig());
    }

    @Test(timeout = 2_000L)
    public void testGetAllowedSets() throws Exception {
        System.out.println("testGetAllowedSets");
        Set<String> unknown = oaiBean.getAllowedSets(UNAUTHORIZED_TRIPLE, "127.0.0.1");
        assertThat(unknown.stream().sorted().collect(toList()), is(asList()));
        Set<String> an = oaiBean.getAllowedSets(AN_TRIPLE, "127.0.0.1");
        assertThat(an.stream().sorted().collect(toList()), is(asList("art", "nat")));
        Set<String> abno = oaiBean.getAllowedSets(ABNO_TRIPLE, "127.0.0.1");
        assertThat(abno.stream().sorted().collect(toList()), is(asList("art", "bkm", "nat", "onl")));
    }
}
