/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of rr-oai-setmatcher
 *
 * rr-oai-setmatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rr-oai-setmatcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.rr.oai.setmatcher;

import org.junit.Test;

import static dk.dbc.rr.oai.setmatcher.BeanFactory.newConfig;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ThrottleTest {

    @Test(timeout = 2_000L)
    public void testThrottle() throws Exception {
        System.out.println("testThrottle");
        Config config = newConfig("THROTTLE=10ms/3,500ms/5,10s");
        Throttle throttle = new Throttle();
        throttle.config = config;

        assertThat(throttle.getSleep(), is(0L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(10L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(10L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(10L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(500L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(500L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(500L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(500L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(500L));
        throttle.failure();
        assertThat(throttle.getSleep(), is(10_000L));
        throttle.success();
        assertThat(throttle.getSleep(), is(0L));
    }
}
