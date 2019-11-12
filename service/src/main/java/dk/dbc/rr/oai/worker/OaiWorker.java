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

import dk.dbc.oai.pmh.DeletedRecordType;
import dk.dbc.oai.pmh.GranularityType;
import dk.dbc.oai.pmh.IdentifyType;
import dk.dbc.rr.oai.Config;
import dk.dbc.rr.oai.io.OaiResponse;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
public class OaiWorker {

    @Inject
    public Config config;

    @Inject
    public OaiDatabaseWorker oaiDatabaseWorker;

    public void identify(OaiResponse response) {

        IdentifyType identify = response.identify();
        identify.setRepositoryName(config.getRepoName());
        identify.setBaseURL(config.getExposedUrl());
        identify.setProtocolVersion("2.0");
        identify.getAdminEmails().add(config.getAdminEmail());
        identify.setEarliestDatestamp("1970-01-01T00:00:00Z"); // Epoch
        identify.setDeletedRecord(DeletedRecordType.TRANSIENT); // Cannot guarantee against database wipes
        identify.setGranularity(GranularityType.YYYY_MM_DD_THH_MM_SS_Z);

    }

}
