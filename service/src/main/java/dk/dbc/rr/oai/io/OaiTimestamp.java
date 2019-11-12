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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiTimestamp {

    private static final Logger log = LoggerFactory.getLogger(OaiTimestamp.class);

    public static final Pattern ISO8601 = Pattern.compile("^\\d{4}(?:-\\d{2}(?:-\\d{2}(?:T\\d{2}:\\d{2}(:\\d{2}(?:\\.\\d{1,6})?)?z)?)?)?$", Pattern.CASE_INSENSITIVE);
    public static final DateTimeFormatter EXACT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSz");
    public static final ZoneId ZONE_Z = ZoneId.of("Z");

    private final Timestamp timestamp;
    private final Granuality granuality;

    public static OaiTimestamp of(String text) {
        try {
            if (!OaiTimestamp.ISO8601.matcher(text).matches())
                throw new DateTimeException("Not a iso-8601 date");
            String ts;
            Granuality truncated; // https://www.postgresql.org/docs/12/functions-datetime.html#FUNCTIONS-DATETIME-TRUNC
            switch (text.length()) {
                case 4:
                    ts = text + "-01-01T00:00:00.000000Z";
                    truncated = Granuality.YEAR;
                    break;
                case 7:
                    ts = text + "-01T00:00:00.000000Z";
                    truncated = Granuality.MONTH;
                    break;
                case 10:
                    ts = text + "T00:00:00.000000Z";
                    truncated = Granuality.DAY;
                    break;
                case 17:
                    ts = text.replace("Z", ":00.000000Z");
                    truncated = Granuality.MINUTE;
                    break;
                case 20:
                    ts = text.replace("Z", ".000000Z");
                    truncated = Granuality.SECOND;
                    break;
                case 22:
                    ts = text.replace("Z", "00000Z");
                    truncated = Granuality.MILLISECONDS;
                    break;
                case 23:
                    ts = text.replace("Z", "0000Z");
                    truncated = Granuality.MILLISECONDS;
                    break;
                case 24:
                    ts = text.replace("Z", "000Z");
                    truncated = Granuality.MILLISECONDS;
                    break;
                case 25:
                    ts = text.replace("Z", "00Z");
                    truncated = Granuality.MICROSECONDS;
                    break;
                case 26:
                    ts = text.replace("Z", "0Z");
                    truncated = Granuality.MICROSECONDS;
                    break;
                case 27:
                    ts = text;
                    truncated = Granuality.MICROSECONDS;
                    break;
                default:
                    throw new AssertionError();
            }
            Instant parsed = Instant.from(OaiTimestamp.EXACT.parse(ts));
            if (parsed.isBefore(Instant.EPOCH))
                throw new DateTimeException("Timestamp is before EPOCH");
            String formatted = OaiTimestamp.EXACT.format(parsed.atZone(OaiTimestamp.ZONE_Z));
            if (formatted.equalsIgnoreCase(ts)) // Sanity check - toString matches input
                return new OaiTimestamp(Timestamp.from(parsed), truncated);
            return null;
        } catch (DateTimeException ex) {
            log.error("Error parsing timestamp: {}: {}", text, ex.getMessage());
            log.debug("Error parsing timestamp: {}: ", text, ex);
            return null;
        }
    }

    public static OaiTimestamp from(Timestamp timestamp) {
        return new OaiTimestamp(timestamp, Granuality.MICROSECONDS);
    }

    private OaiTimestamp(Timestamp timestamp, Granuality granuality) {
        this.timestamp = timestamp;
        this.granuality = granuality;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getTruncate() {
        return granuality.getText();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.timestamp);
        hash = 29 * hash + Objects.hashCode(this.granuality);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final OaiTimestamp other = (OaiTimestamp) obj;
        return this.granuality == other.granuality &&
               Objects.equals(this.timestamp, other.timestamp);
    }

    @Override
    public String toString() {
        return "OaiTimestamp{" + timestamp + "/" + granuality.text + '}';
    }

    static void to(DataOutputStream dos, OaiTimestamp ts) throws IOException {
        if (ts == null) {
            dos.writeByte(Byte.MIN_VALUE);

        } else {
            dos.writeByte(ts.granuality.getNo());
            dos.writeInt(ts.timestamp.getNanos());
            dos.writeLong(ts.timestamp.getTime());
        }
    }

    static OaiTimestamp from(DataInputStream dis) throws IOException {
        byte b = dis.readByte();
        if (b == Byte.MIN_VALUE)
            return null;
        Granuality g = Granuality.of(b);
        int nanos = dis.readInt();
        long time = dis.readLong();
        Timestamp ts = new Timestamp(time);
        ts.setNanos(nanos);
        return new OaiTimestamp(ts, g);
    }

    private enum Granuality {
        YEAR("year", 0),
        MONTH("month", 1),
        DAY("day", 2),
        HOUR("hour", 3),
        MINUTE("minute", 4),
        SECOND("second", 5),
        MILLISECONDS("milliseconds", 6),
        MICROSECONDS("microseconds", 7);

        private final String text;
        private final byte no;

        Granuality(String text, int no) {
            this.text = text;
            this.no = (byte) no;
        }

        private static Granuality of(byte no) {
            for (Granuality value : values()) {
                if (value.no == no)
                    return value;
            }
            throw new IllegalArgumentException("Cannot convert '" + no + "' to Granuality");
        }

        public String getText() {
            return text;
        }

        private byte getNo() {
            return no;
        }
    }
}
