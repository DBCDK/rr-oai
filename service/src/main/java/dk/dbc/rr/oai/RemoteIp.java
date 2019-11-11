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
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;

import static java.util.stream.Collectors.toList;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
public class RemoteIp {

    private static final String IP0TO255 = "(2(?:5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9][0-9]|[0-9])";
    private static final String DOT = "\\.";
    private static final Pattern IP_PATTERN = Pattern.compile("^" + IP0TO255 + DOT + IP0TO255 + DOT + IP0TO255 + DOT + IP0TO255 + "$");
    private static final Pattern NET_PATTERN = Pattern.compile("^(?:3[0-2]|[12]?[0-9])$");

    @Inject
    public Config config;

    private List<IpRange> proxyRanges;

    @PostConstruct
    public void init() {
        proxyRanges = config.getxForwardedFor().stream()
                .map(IpRange::of)
                .collect(toList());
    }

    /**
     * Look up remote IP address
     * <p>
     * This is only valid for IPv4 ranges.
     * <p>
     * It looks at current remove and proxies from the X-Forwarded-For header.
     * Then returns the first client, that is not represented as an IPv4 address
     * or not in the IP ranges that denotes there the proxies are located
     * <p>
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For
     *
     * @param ip            The peer ip from (@Context HttpServletRequest
     *                      httpRequest).getRemoteAddr();
     * @param xForwardedFor the X-Forwarded-For header (or null) from (@Context
     *                      HttpHeaders headers).getHeaderString("x-forwarded-for");
     * @return the actual ip of the client
     */
    public String clientIp(String ip, String xForwardedFor) {
        if (xForwardedFor == null)
            return ip;
        String[] xForwardedForHosts = Stream.of(xForwardedFor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        int offset = xForwardedForHosts.length - 1;
        for (;;) {
            if (offset < 0) // no more in X-Forwarded-For Header
                return ip;
            Long ipNumber = ipOf(ip);
            if (ipNumber == null) // Ip is not an ipv4 (a host or ipv6)
                return ip;
            if (!proxyRanges.stream()
                    .anyMatch(r -> r.isInRange(ipNumber))) // Ip is outside proxy range
                return ip;
            ip = xForwardedForHosts[offset--];
        }
    }

    /**
     * Convert an IPv4 into a long
     *
     * @param ip String of IPv4 address
     * @return long or null if not an IPv4 address
     */
    static Long ipOf(String ip) {
        Matcher m = IP_PATTERN.matcher(ip);
        if (m.matches()) {
            return Long.parseUnsignedLong(m.group(1)) << 24 |
                   Long.parseUnsignedLong(m.group(2)) << 16 |
                   Long.parseUnsignedLong(m.group(3)) << 8 |
                   Long.parseUnsignedLong(m.group(4));
        }
        return null;
    }

    static class IpRange {

        /**
         * Convert a string to an ip-range
         *
         * @param range host, host-host or host/net(0-32)
         * @return ip-range description
         */
        static IpRange of(String range) {
            if (range.contains("-")) {
                String[] a = range.split("-", 2);
                Long from = ipOf(a[0]);
                Long to = ipOf(a[1]);
                if (from == null || to == null)
                    throw new IllegalArgumentException("Cannot turn: " + range + " into an ip-range");
                if (Objects.equals(from, to))
                    throw new IllegalArgumentException("Cannot turn: " + range + " into an ip-range from and to are rqual");
                if (from > to)
                    throw new IllegalArgumentException("Cannot turn: " + range + " into an ip-range from and to are switched");
                return new IpRange(from, to);
            } else if (range.contains("/")) {
                String[] a = range.split("/", 2);
                Long from = ipOf(a[0]);
                if (from == null)
                    throw new IllegalArgumentException("Cannot turn: " + range + " into an ip-range");
                Matcher m = NET_PATTERN.matcher(a[1]);
                if (m.matches()) {
                    int width = Integer.parseUnsignedInt(a[1]);
                    long mask = 0xffffffffL << ( 32 - width );
                    from = from & mask;
                    long to = from | ( ~mask & 0xffffffffL );
                    return new IpRange(from, to);
                }
                throw new IllegalArgumentException("Cannot turn: " + range + " into an ip-range invalid net");
            } else {
                Long ip = ipOf(range);
                if (ip == null)
                    throw new IllegalArgumentException("Cannot turn: " + range + " into an ip-range");
                return new IpRange(ip, ip);
            }
        }

        long from;
        long to;

        private IpRange(long from, long to) {
            this.from = from;
            this.to = to;
        }

        /**
         * Check if an IP from {@link #ipOf(java.lang.String) } is in the range
         *
         * @param ip long representing the ip
         * @return is it is in the range
         */
        boolean isInRange(long ip) {
            return ip >= from && ip <= to;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + (int) ( this.from ^ ( this.from >>> 32 ) );
            hash = 89 * hash + (int) ( this.to ^ ( this.to >>> 32 ) );
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null ||
                getClass() != obj.getClass())
                return false;
            final IpRange other = (IpRange) obj;
            return this.from == other.from &&
                   this.to == other.to;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                                 "IpRange{%d.%d.%d.%d-%d.%d.%d.%d}",
                                 ( from >> 24 ) & 0xff, ( from >> 16 ) & 0xff, ( from >> 8 ) & 0xff, from & 0xff,
                                 ( to >> 24 ) & 0xff, ( to >> 16 ) & 0xff, ( to >> 8 ) & 0xff, to & 0xff);
        }
    }
}
