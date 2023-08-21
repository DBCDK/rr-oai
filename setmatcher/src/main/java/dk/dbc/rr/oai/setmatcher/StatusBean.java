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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
@Path("status")
public class StatusBean {

    private static final Logger log = LoggerFactory.getLogger(StatusBean.class);

    @Inject
    JavaScriptPool pool;

    @Inject
    Worker worker;

    @Inject
    RawRepo rr;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getStatus() {
        log.info("getStatus called ");

        if (pool.isInBadState()) {
            log.warn("JavaScript pool bad state");
            return Response.serverError().entity(new Resp("JavaScript pool error")).build();
        }
        if (worker.isInBadState()) {
            log.warn("Worker is sick");
            return Response.serverError().entity(new Resp("Worker is unhealthy")).build();
        }
        if(worker.queueStalled()) {
            log.warn("Queue stalled");
            return Response.serverError().entity(new Resp("Queue stalled")).build();
        }
        if (!rr.ping()) {
            log.warn("RawRepo cannot be ping'ed");
            return Response.serverError().entity(new Resp("Cannot ping rawrepo-record-service")).build();
        }

        log.debug("Status - ok");
        return Response.ok().entity(new Resp()).build();
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class Resp {

        public boolean ok;
        public String text;

        public Resp() {
            this.ok = true;
            this.text = "Success";
        }

        public Resp(String diag) {
            this.ok = false;
            this.text = diag;
            log.error("Answering with diag: {}", diag);
        }
    }
}
