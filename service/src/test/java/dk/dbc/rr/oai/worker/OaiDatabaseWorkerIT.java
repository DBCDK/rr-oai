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

import dk.dbc.oai.pmh.ResumptionTokenType;
import dk.dbc.rr.oai.Config;
import dk.dbc.rr.oai.DB;
import dk.dbc.rr.oai.io.OaiIOBean;
import dk.dbc.rr.oai.io.OaiIdentifier;
import dk.dbc.rr.oai.io.OaiResumptionToken;
import dk.dbc.rr.oai.io.OaiTimestamp;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;

import static dk.dbc.rr.oai.BeanFactory.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiDatabaseWorkerIT extends DB {

    private Config config;
    private OaiDatabaseWorker bean;
    private OaiIOBean ioBean;

    @Before
    public void setUp() {
        this.config = newConfig();
        this.ioBean = newOaiIOBean(config);
        this.bean = newOaiDatabaseWorker(config, ds);
    }

    @Test(timeout = 2_000L)
    public void testContinuedFetch() throws Exception {
        System.out.println("testContinuedFetch");
        assertThat(config.getMaxRowsPrRequest(), is(10));
        loadResource("records-25-same-timestamp.json");
        OaiTimestamp from = OaiTimestamp.of("2019");
        OaiTimestamp to = OaiTimestamp.of("2019");
        String set = "bkm";
        LinkedList<OaiIdentifier> identifiers = bean.listIdentifiers(from, to, set);
        OaiResumptionToken token = takeLastAsResumptionToken(from, identifiers, to, set);
        assertThat(identifiers.size(), is(10));
        identifiers = bean.listIdentifiers(token);
        token = takeLastAsResumptionToken(from, identifiers, to, set);
        assertThat(identifiers.size(), is(10));
        identifiers = bean.listIdentifiers(token);
        assertThat(identifiers.size(), is(5));
    }

    @Test(timeout = 2_000L)
    public void testUpdatedDuringHarvest() throws Exception {
        System.out.println("testUpdatedDuringHarvest");
        loadResource("records-15-same-timestamp.json");
        OaiTimestamp from = OaiTimestamp.of("2019");
        OaiTimestamp to = OaiTimestamp.of("2019");
        String set = "bkm";
        LinkedList<OaiIdentifier> identifiers = bean.listIdentifiers(from, to, set);
        OaiResumptionToken token = takeLastAsResumptionToken(from, identifiers, to, set);
        assertThat(identifiers.size(), is(10));
        OaiIdentifier identifier = identifiers.pop();
        String id = identifier.getIdentifier().replaceFirst("-", ":");
        System.out.println("id = " + id);
        insert(id)
                .changed("2019-02-02T20:59:59Z")
                .set("bkm")
                .commit();
        identifiers = bean.listIdentifiers(token);
        assertThat(identifiers.size(), is(6));
    }

    @Test(timeout = 2_000L)
    public void testRemovedDuringHarvest() throws Exception {
        System.out.println("testRemovedDuringHarvest");
        
        assertThat("", is("The test case is a prototype."));
    }


    private OaiResumptionToken takeLastAsResumptionToken(OaiTimestamp from, LinkedList<OaiIdentifier> identifiers, OaiTimestamp to, String set) {
        return ioBean.resumptionTokenOf(ioBean.resumptionTokenFor(from, identifiers.removeLast(), to, set).getValue());
    }

}