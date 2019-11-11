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

import dk.dbc.rr.oai.RemoteIp.IpRange;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

import static dk.dbc.rr.oai.RemoteIp.ipOf;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class RemoteIpTest {

    @Test(timeout = 2_000L)
    public void testIpOf() throws Exception {
        System.out.println("testIpOf");

        assertThat(ipOf(""), nullValue());
        assertThat(ipOf("1.2.3"), nullValue());
        assertThat(ipOf("1.2.3.4.5"), nullValue());
        assertThat(ipOf("1.2.3.a"), nullValue());
        assertThat(ipOf("1.2.3.-4"), nullValue());
        assertThat(ipOf("1.2.3.4"), is(0x01020304L));
        assertThat(ipOf("192.128.127.0"), is(0xc0807f00L));
        assertThat(ipOf("255.254.253.252"), is(0xfffefdfcL));
        assertThat(ipOf("200.199.192.99"), not(nullValue()));
        assertThat(ipOf("20.10.9.8"), not(nullValue()));
    }

    @Test(timeout = 2_000L)
    public void testIpRange() throws Exception {
        System.out.println("testIpRange");
        IpRange r;

        r = IpRange.of("123.123.123.123");
        assertThat(r.isInRange(ipOf("123.123.123.122")), is(false));
        assertThat(r.isInRange(ipOf("123.123.123.123")), is(true));
        assertThat(r.isInRange(ipOf("123.123.123.124")), is(false));

        r = IpRange.of("123.123.123.123-123.123.123.128");
        assertThat(r.isInRange(ipOf("123.123.123.122")), is(false));
        assertThat(r.isInRange(ipOf("123.123.123.123")), is(true));
        assertThat(r.isInRange(ipOf("123.123.123.124")), is(true));
        assertThat(r.isInRange(ipOf("123.123.123.128")), is(true));
        assertThat(r.isInRange(ipOf("123.123.123.129")), is(false));

        r = IpRange.of("123.123.123.128/26");
        assertThat(r, is(IpRange.of("123.123.123.128-123.123.123.191")));

        r = IpRange.of("10.16.23.45/16");
        assertThat(r, is(IpRange.of("10.16.0.0-10.16.255.255")));

        r = IpRange.of("10.16.23.45/0");
        assertThat(r, is(IpRange.of("0.0.0.0-255.255.255.255")));
        r = IpRange.of("10.16.23.45/32");
        assertThat(r, is(IpRange.of("10.16.23.45")));

        Stream.of("10.10.0.0-10.10.0.0",
                  "10.10.0.0-10.0.0.0",
                  "0.0.0.0-256.0.0.0",
                  "0.0.0.256-0.0.0.0",
                  "256.12.13.14",
                  "256.12.13.14/0",
                  "10.12.13.14/a",
                  "10.12.13.14/-1",
                  "10.12.13.14/33")
                .forEach(badRange -> {
                    try {
                        IpRange.of(badRange);
                        fail("Expected exception of range: " + badRange);
                    } catch (RuntimeException ex) {
                        System.out.println("Exception of: " + badRange + " expected");
                    }
                });
    }

    @Test(timeout = 2_000L)
    public void testRemoteIp() throws Exception {
        System.out.println("testRemoteIp");

        RemoteIp remoteIp = new RemoteIp();
        remoteIp.config = new Config() {
            @Override
            public List<String> getxForwardedFor() {
                return Arrays.asList("10.0.0.0/8", "192.168.0.0/16", "172.16.0.0/12", "127.0.0.0/8");
            }
        };
        remoteIp.init();

        assertThat(remoteIp.clientIp("127.0.0.1", null), is("127.0.0.1"));
        assertThat(remoteIp.clientIp("127.0.0.1", "12.34.56.78"), is("12.34.56.78"));
        assertThat(remoteIp.clientIp("127.0.0.1", "10.1.2.3"), is("10.1.2.3"));
        assertThat(remoteIp.clientIp("127.0.0.1", "10.1.2.3, 12.34.56.78"), is("12.34.56.78"));
        assertThat(remoteIp.clientIp("127.0.0.1", "12.34.56.78, 10.1.2.3"), is("12.34.56.78"));
        assertThat(remoteIp.clientIp("127.0.0.1", "my-host-name, 10.1.2.3"), is("my-host-name"));
        assertThat(remoteIp.clientIp("::1", "my-host-name, 10.1.2.3"), is("::1"));
    }
}
