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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    private final String timestamp;
    private final String truncate;

    public static OaiTimestamp of(String text) {
        try {
            if (!OaiTimestamp.ISO8601.matcher(text).matches())
                throw new DateTimeException("Not a iso-8601 date");
            String ts;
            String truncated; // https://www.postgresql.org/docs/12/functions-datetime.html#FUNCTIONS-DATETIME-TRUNC
            switch (text.length()) {
                case 4:
                    ts = text + "-01-01T00:00:00.000000Z";
                    truncated = "year";
                    break;
                case 7:
                    ts = text + "-01T00:00:00.000000Z";
                    truncated = "month";
                    break;
                case 10:
                    ts = text + "T00:00:00.000000Z";
                    truncated = "day";
                    break;
                case 17:
                    ts = text.replace("Z", ":00.000000Z");
                    truncated = "minute";
                    break;
                case 20:
                    ts = text.replace("Z", ".000000Z");
                    truncated = "second";
                    break;
                case 22:
                    ts = text.replace("Z", "00000Z");
                    truncated = "milliseconds";
                    break;
                case 23:
                    ts = text.replace("Z", "0000Z");
                    truncated = "milliseconds";
                    break;
                case 24:
                    ts = text.replace("Z", "000Z");
                    truncated = "milliseconds";
                    break;
                case 25:
                    ts = text.replace("Z", "00Z");
                    truncated = "microseconds";
                    break;
                case 26:
                    ts = text.replace("Z", "0Z");
                    truncated = "microseconds";
                    break;
                case 27:
                    ts = text;
                    truncated = "microseconds";
                    break;
                default:
                    throw new AssertionError();
            }
            Instant parsed = Instant.from(OaiTimestamp.EXACT.parse(ts));
            if (parsed.isBefore(Instant.EPOCH))
                throw new DateTimeException("Timestamp is before EPOCH");
            String formatted = OaiTimestamp.EXACT.format(parsed.atZone(OaiTimestamp.ZONE_Z));
            if (formatted.equalsIgnoreCase(ts)) // Sanity check - toString matches input
                return new OaiTimestamp(ts, truncated);
            return null;
        } catch (DateTimeException ex) {
            log.error("Error parsing timestamp: {}: {}", text, ex.getMessage());
            log.debug("Error parsing timestamp: {}: ", text, ex);
            return null;
        }
    }

    private OaiTimestamp(String timestamp, String truncate) {
        this.timestamp = timestamp;
        this.truncate = truncate;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getTruncate() {
        return truncate;
    }

    public static String checkString(String text) {
        try {
            if (!OaiTimestamp.ISO8601.matcher(text).matches())
                throw new DateTimeException("Not a iso-8601 date");
            String ts;
            switch (text.length()) {
                case 4:
                    ts = text + "-01-01T00:00:00.000000Z";
                    break;
                case 7:
                    ts = text + "-01T00:00:00.000000Z";
                    break;
                case 10:
                    ts = text + "T00:00:00.000000Z";
                    break;
                case 17:
                    ts = text.replace("Z", ":00.000000Z");
                    break;
                case 20:
                    ts = text.replace("Z", ".000000Z");
                    break;
                case 22:
                    ts = text.replace("Z", "00000Z");
                    break;
                case 23:
                    ts = text.replace("Z", "0000Z");
                    break;
                case 24:
                    ts = text.replace("Z", "000Z");
                    break;
                case 25:
                    ts = text.replace("Z", "00Z");
                    break;
                case 26:
                    ts = text.replace("Z", "0Z");
                    break;
                case 27:
                    ts = text;
                    break;
                default:
                    throw new AssertionError();
            }
            Instant parsed = Instant.from(OaiTimestamp.EXACT.parse(ts));
            if (parsed.isBefore(Instant.EPOCH))
                throw new DateTimeException("Timestamp is before EPOCH");
            String formatted = OaiTimestamp.EXACT.format(parsed.atZone(OaiTimestamp.ZONE_Z));
            if (formatted.equalsIgnoreCase(ts)) // Sanity check - toString matches input
                return text;
            return null;
        } catch (DateTimeException ex) {
            log.error("Error parsing timestamp: {}: {}", text, ex.getMessage());
            log.debug("Error parsing timestamp: {}: ", text, ex);
            return null;
        }
    }

}
