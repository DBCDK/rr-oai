package dk.dbc.rr.oai.setmatcher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@ApplicationScoped
@Liveness
public class JavaScriptPoolHealth implements HealthCheck {

    private static final Logger log = LoggerFactory.getLogger(JavaScriptPoolHealth.class);

    @Inject
    JavaScriptPool js;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("javascript-pool")
                .status(js.allIsGood())
                .build();
    }
}
