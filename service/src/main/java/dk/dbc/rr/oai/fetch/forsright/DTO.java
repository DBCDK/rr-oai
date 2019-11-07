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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Transfer Object for Fors Rights
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressFBWarnings({"NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"})
class DTO {

    private static final Logger log = LoggerFactory.getLogger(DTO.class);

    public ForsRightsResponse forsRightsResponse;

    /**
     * Look up of it's a default value, or of name,right is in the response
     *
     * @param nameAndRight either "*", "default" or "{name},{right}"
     * @return if it is a right that is in the response or default
     */
    boolean hasRight(String nameAndRight) {
        if ("*".equals(nameAndRight) ||
            "default".equals(nameAndRight))
            return true; // Everybody has "default"
        String[] parts = nameAndRight.split(",", 2);
        if (parts.length != 2) {
            log.error("Invalid formatted forsright rule: {}", nameAndRight);
            return false; // Badly formatted part
        }
        return forsRightsResponse.ressource.stream()
                .filter(r -> parts[0].equals(r.name))
                .flatMap(r -> r.right.stream())
                .anyMatch(s -> parts[1].equals(s));
    }

    @Override
    public String toString() {
        return "DTO{" + "forsRightsResponse=" + forsRightsResponse + '}';
    }

    public static class ForsRightsResponse {

        public List<Resource> ressource;

        @Override
        public String toString() {
            return "ForsRightsResponse{" + "resource=" + ressource + '}';
        }
    }

    public static class Resource {

        public String name;
        public List<String> right;

        @Override
        public String toString() {
            return "Resource{" + "name=" + name + ", right=" + right + '}';
        }
    }
}
