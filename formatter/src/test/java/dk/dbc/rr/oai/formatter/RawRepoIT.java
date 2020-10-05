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
package dk.dbc.rr.oai.formatter;

import dk.dbc.formatter.js.MarcXChangeWrapper;
import dk.dbc.rawrepo.RecordId;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;

import static dk.dbc.rr.oai.formatter.BeanFactory.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class RawRepoIT {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RawRepoIT.class);

    private RawRepo rawrepo;

    public RawRepoIT() {
    }

    @Before
    public void setUp() {
        Config config = newConfig("RAWREPO_RECORD_SERVICE_URL=" + System.getenv().getOrDefault("RAWREPO_RECORD_SERVICE_URL", "http://localhost/rawrepo-record-service"),
                                  "POOL_MIN_IDLE=1",
                                  "POOL_MAX_IDLE=1",
                                  "USER_AGENT=fool-me/1.0");
        this.rawrepo = newRawRepo(config);
    }

    @After
    public void tearDown() {
        rawrepo.close();
    }

    @Test(timeout = 2_000L)
    public void testRecords() throws Exception {
        System.out.println("testRecords");

        String id = "28407866";
        MarcXChangeWrapper[] wrappers = rawrepo.getRecordsFor(870970, id);

        assertThat(wrappers.length, is(2));
        assertThat(wrappers[0].content, containsString("<datafield ind1='0' ind2='0' tag='001'><subfield code='a'>28407866</subfield>"));
        assertThat(wrappers[0].children.length, is(0));
        assertThat(wrappers[1].content, containsString("<datafield ind1='0' ind2='0' tag='001'><subfield code='a'>28413882</subfield>"));
        assertThat(wrappers[1].children.length, not(is(0)));
        assertThat(Arrays.asList(wrappers[1].children), hasItem(new RecordId(id, 870970)));
    }

    @Test(timeout = 4_000L)
    public void testError() throws Exception {
        System.out.println("testError");

        String id = "43914804";
        try {
            MarcXChangeWrapper[] wrappers = rawrepo.getRecordsFor(870971, id);
        } catch (InternalServerErrorException ise) {
            assertThat(ise.getResponse().getStatus(), is(500));
        }
    }

    @Test(timeout = 2_000L)
    public void testPing() throws Exception {
        System.out.println("testPing");
        boolean ping = rawrepo.ping();
        assertThat(ping, is(true));
    }

}
