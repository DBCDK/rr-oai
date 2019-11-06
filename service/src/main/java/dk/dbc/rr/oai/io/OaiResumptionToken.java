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
package dk.dbc.rr.oai.io;

import dk.dbc.oai.pmh.ResumptionTokenType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.*;
import static java.time.temporal.ChronoUnit.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiResumptionToken {

    private static final Logger log = LoggerFactory.getLogger(OaiResumptionToken.class);

    private static final byte[] XOR = makeXor();
    private static final Supplier<Instant> DEFAULT_EXPIRES = makeDefaultValueUntil();

    private final String from;
    private final String nextIdentifier;
    private final String until;
    private final String set;

    public static OaiResumptionToken of(String base64) {
        try {
            byte[] xored = Base64.getUrlDecoder().decode(base64);
            byte[] checksummed = xor(xored);
            try (ByteArrayInputStream bis = checkChecksum(checksummed) ;
                 DataInputStream dis = new DataInputStream(bis)) {
                Instant validUntil = readInstant(dis);
                String from = readString(dis);
                String nextIdentifier = readString(dis);
                String until = readString(dis);
                String set = readString(dis);
                if (validUntil.isAfter(Instant.now()))
                    return new OaiResumptionToken(from, nextIdentifier, until, set);
                return null;
            }
        } catch (IOException | RuntimeException ex) {
            log.error("Error parsing resumptionToken: {}", ex.getMessage());
            log.debug("Error parsing resumptionToken: ", ex);
            return null;
        }
    }

    OaiResumptionToken(String from, String nextIdentifier, String until, String set) {
        this.from = from;
        this.nextIdentifier = nextIdentifier;
        this.until = until;
        this.set = set;
    }

    public String getFrom() {
        return from;
    }

    public String getNextIdentifier() {
        return nextIdentifier;
    }

    public String getSet() {
        return set;
    }

    public String getUntil() {
        return until;
    }

    public ResumptionTokenType toXML() throws IOException {
        ResumptionTokenType resumptionToken = OaiResponse.O.createResumptionTokenType();
        Instant validUntil = DEFAULT_EXPIRES.get();
        resumptionToken.setExpirationDate(OaiResponse.xmlDate(validUntil));
        resumptionToken.setValue(toData(validUntil));
        return resumptionToken;
    }

    String toData(Instant expires) throws IOException {
        byte[] bytes = asBytes(expires);
        byte[] checksummed = calcChecksum(bytes);
        byte[] xored = xor(checksummed);
        byte[] base64 = Base64.getUrlEncoder().encode(xored);
        return new String(base64, ISO_8859_1);
    }

    /**
     * Turn data into bytes using writeInstant/String
     *
     * @param expires Then the token expires
     * @return byte array representing thie object and expires timestamp
     * @throws IOException in case of IO errors in java.io.*
     */
    private byte[] asBytes(Instant expires) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (DataOutputStream oos = new DataOutputStream(bos)) {
                writeInstant(oos, expires);
                writeString(oos, from);
                writeString(oos, nextIdentifier);
                writeString(oos, until);
                writeString(oos, set);
            }
            return bos.toByteArray();
        }
    }

    /**
     * Write an instant as nanos and seconds
     * <p>
     * if instant is null: nanos is written as Integer.MIN_VALUE and seconds
     * aren't written
     *
     * @param dos     output stream
     * @param instant timestamp
     * @throws IOException in case of IO errors in java.io.*
     */
    private static void writeInstant(DataOutputStream dos, Instant instant) throws IOException {
        if (instant == null) {
            dos.writeInt(Integer.MIN_VALUE); // Magic null value
        } else {
            dos.writeInt(instant.getNano());
            dos.writeLong(instant.getEpochSecond());
        }
    }

    /**
     * Write an String as length if utf-8 and bytes
     * <p>
     * if the string is null: length is Integer.MIN_VALUE and no bytes are
     * written
     *
     * @param dos output stream
     * @param s   timestamp
     * @throws IOException in case of IO errors in java.io.*
     */
    private static void writeString(DataOutputStream dos, String s) throws IOException {
        if (s == null) {
            dos.writeInt(Integer.MIN_VALUE); // Magic null value
        } else {
            byte[] bytes = s.getBytes(UTF_8);
            dos.writeInt(bytes.length);
            dos.write(bytes, 0, bytes.length);
        }
    }

    /**
     * Read an Instant from a datastream
     * <p>
     * read nanos and seconds. If nanos is Integer.MIN_VALUE then null is
     * returned and seconds aren't read
     *
     * @param dis input stream
     * @return instant or null
     * @throws IOException in case of IO errors in java.io.*
     */
    private static Instant readInstant(DataInputStream dis) throws IOException {
        int nano = dis.readInt();
        if (nano == Integer.MIN_VALUE)
            return null;
        long epochSeconds = dis.readLong();
        return Instant.ofEpochSecond(epochSeconds, nano);
    }

    /**
     * Read a String from a datastream
     * <p>
     * read length and UTF-8 bytes. If length is Integer.MIN_VALUE then null is
     * returned and content isn't read
     *
     * @param dis input stream
     * @return instant or null
     * @throws IOException in case of IO errors in java.io.*
     */
    private static String readString(DataInputStream dis) throws IOException {
        int l = dis.readInt();
        if (l == Integer.MIN_VALUE)
            return null;
        byte[] bytes = new byte[l];
        int read = dis.read(bytes);
        if (bytes.length != read)
            throw new IllegalStateException("Internal error on read from resumprtion token expected " + bytes.length + " but got " + read + " bytes");
        return new String(bytes, UTF_8);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.from);
        hash = 79 * hash + Objects.hashCode(this.nextIdentifier);
        hash = 79 * hash + Objects.hashCode(this.until);
        hash = 79 * hash + Objects.hashCode(this.set);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final OaiResumptionToken other = (OaiResumptionToken) obj;
        return Objects.equals(this.from, other.from) &&
               Objects.equals(this.nextIdentifier, other.nextIdentifier) &&
               Objects.equals(this.set, other.set) &&
               Objects.equals(this.until, other.until);
    }

    @Override
    public String toString() {
        return "OaiResumptionToken{" + "from=" + from + ", nextId=" + nextIdentifier + ", until=" + until + ", set=" + set + '}';
    }

    /**
     * Simple obfuscation using XOR from an environment variable
     *
     * @param in byte array
     * @return byte array xor'ed
     */
    private static byte[] xor(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0 ; i < in.length ; i++) {
            out[i] = (byte) ( in[i] ^ XOR[i % XOR.length] );
        }
        return out;
    }

    /**
     * Calculate checksum
     * <p>
     * 4 bytes are added to the front. Every 4th (starting at 0..3) xor'ed
     * together gives zero in the resulting byte array
     *
     * @param in byte array
     * @return byte array with 4 checksum bytes
     */
    private static byte[] calcChecksum(byte[] in) {
        byte[] out = new byte[in.length + 4];
        out[0] = out[1] = out[2] = out[3] = 0;
        for (int i = 0 ; i < in.length ; i++) {
            byte b = in[i];
            out[i % 4] ^= b;
            out[i + 4] = b;
        }
        return out;
    }

    /**
     * Check the checksum
     * <p>
     * Every 4th (starting at 0..3) xor'ed together gives zero in the input
     *
     * @param in raw byte array
     * @return input stream that skipped the 4 checksum bytes
     */
    private static ByteArrayInputStream checkChecksum(byte[] in) {
        if (in.length <= 4)
            throw new IllegalStateException("Too short");
        for (int i = 4 ; i < in.length ; i++) {
            in[i % 4] ^= in[i];
        }
        if (in[0] != 0 || in[1] != 0 || in[2] != 0 || in[3] != 0)
            throw new IllegalStateException("Checksum failed");
        return new ByteArrayInputStream(in, 4, in.length - 4);
    }

    /**
     * Extract a bunch of xor bytes from an environmwent variable
     *
     * @return xor bytes
     */
    private static byte[] makeXor() {
        try {
            String text = System.getenv("XOR_TEXT_ASCII");
            if (text == null || text.length() < 2) {
                log.error("XOR_TEXT_ASCII is unset or too short");
                text = "This really needs to be set";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(text.getBytes(ISO_8859_1));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a supplier of expiry timestamps eith a timeout taken from the
     * environment
     *
     * @return supplier of Instant
     */
    private static Supplier<Instant> makeDefaultValueUntil() {
        String text = System.getenv().getOrDefault("RESUMPTION_TOKEN_TIMEOUT", "3d");
        Matcher matcher = Pattern.compile("^([1-9][0-9]*)\\s*(y(?:ears?)?|m(?:onths?)?|d(?:ays?)?|h(?:ours?)?|minutes?|s(?:econds?)?)$")
                .matcher(text.toLowerCase(Locale.ROOT));
        if (!matcher.matches())
            throw new IllegalStateException("Cannot parse RESUMPTION_TOKEN_TIMEOUT (" + text + ")");
        long amount = Long.parseLong(matcher.group(1));
        TemporalUnit unit;
        switch (matcher.group(2)) {
            case "y":
            case "year":
            case "years":
                unit = YEARS;
                break;
            case "m":
            case "month":
            case "months":
                unit = MONTHS;
                break;
            case "d":
            case "day":
            case "days":
                unit = DAYS;
                break;
            case "h":
            case "hour":
            case "hours":
                unit = HOURS;
                break;
            case "minute":
            case "minutes":
                unit = MINUTES;
                break;
            case "s":
            case "second":
            case "seconds":
                unit = SECONDS;
                break;
            default:
                throw new AssertionError();
        }
        return () -> Instant.now().plus(amount, unit);
    }
}
