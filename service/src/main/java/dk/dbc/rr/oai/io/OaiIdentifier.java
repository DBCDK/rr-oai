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
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class OaiIdentifier {

    private final String identifier;
    private final boolean deleted;
    private final Timestamp changed;
    private final Timestamp vanished;
    private final HashSet<String> setspecs;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OaiIdentifier(String identifier, boolean deleted, Timestamp changed, Timestamp vanished, String... setspecs) {
        this.identifier = identifier;
        this.deleted = deleted;
        this.changed = changed;
        this.vanished = vanished;
        this.setspecs = new HashSet<>(Arrays.asList(setspecs));
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Timestamp getChanged() {
        return changed;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Timestamp getVanished() {
        return vanished;
    }

    public HashSet<String> getSetspecs() {
        return setspecs;
    }

    public void add(String setspec) {
        setspecs.add(setspec);
    }

    public Set<String> setspecsLimitedTo(Set<String> allowed) {
        Set<String> set = (Set<String>) setspecs.clone();
        set.retainAll(allowed);
        return set;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.identifier);
        hash = 79 * hash + ( this.deleted ? 1 : 0 );
        hash = 79 * hash + Objects.hashCode(this.changed);
        hash = 79 * hash + Objects.hashCode(this.vanished);
        hash = 79 * hash + Objects.hashCode(this.setspecs);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final OaiIdentifier other = (OaiIdentifier) obj;
        return this.deleted == other.deleted &&
               Objects.equals(this.identifier, other.identifier) &&
               Objects.equals(this.changed, other.changed) &&
               Objects.equals(this.vanished, other.vanished) &&
               Objects.equals(this.setspecs, other.setspecs);
    }

    @Override
    public String toString() {
        return "OaiIdentifier{" + "identifier=" + identifier + ", deleted=" + deleted + ", changed=" + changed + ", vanished=" + vanished + ", setspecs=" + setspecs + '}';
    }
}
