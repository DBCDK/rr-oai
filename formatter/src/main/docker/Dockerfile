FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:old-202447

LABEL \
    USER_AGENT="Name of client to set in http requests (optional)" \
    RAWREPO_RECORD_SERVICE_URL="Url of record-service (required)" \
    POOL_MIN_IDLE="Minimum number of unused JavaScript environments (number, required)" \
    POOL_MAX_IDLE="Maximum number of unused JavaScript environments (number, required)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
