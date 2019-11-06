FROM docker.dbc.dk/payara5-micro:latest

LABEL \
    EXPOSED_URL="Full external url of this service (required)" \
    FETCH_TIMEOUT_IN_SECONDS="Max number of seconds (walltime) to use to parallel fetch records (number, required)" \
    PARALLEL_FETCH="How many records to format in parallel (number, required)" \
    POOL_MIN_IDLE="Minimum number of unused DOM Parsers (number, required)" \
    POOL_MAX_IDLE="Maximum number of unused DOM Parsers (number, required)" \
    RAWREPO_OAI_FORMATTER_SERVICE_URL="Url of rawrepo format service (required)" \
    USER_AGENT="Name of client to set in http requests (optional)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
