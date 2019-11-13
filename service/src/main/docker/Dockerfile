FROM docker.dbc.dk/payara5-micro:latest

LABEL \
    ADMIN_EMAIL="Official email used in Identify call (required)" \
    AUTHENTICATION_DISABLED="Disable authentication and have all sets allowed for everybody (boolean, default: false)" \
    CACHE_SUCCESS_MINUTES="Number of minutes a good result from forsRights is cached (required)"\
    CACHE_FAILURE_MINUTES="Number of minutes a bad result from forsRights is cached (required)"\
    EXPOSED_URL="Full external url of this service (required)" \
    FETCH_TIMEOUT_IN_SECONDS="Max number of seconds (walltime) to use to parallel fetch records (number, required)" \
    FORS_RIGHTS_RULES=";-seperated list of rule{name,right or */default} = set [, set]. ex *=nat;danbib,502=bkm,onl (required)" \
    FORS_RIGHTS_URL="Url of forsrights service (required)" \
    MAX_POOL_SIZE="Database pool size  (number, required)" \
    MAX_ROWS_PR_REQUEST="Max numbre of rows in ListIdentifers/ListRecords (number, required)" \
    PARALLEL_FETCH="How many records to format in parallel (number, required)" \
    POOL_MIN_IDLE="Minimum number of unused DOM Parsers (number, required)" \
    POOL_MAX_IDLE="Maximum number of unused DOM Parsers (number, required)" \
    RAWREPO_OAI_FORMATTER_SERVICE_URL="Url of rawrepo format service (required)" \
    RAWREPO_OAI_POSTGRES_URL="Database url (user:pass@host:port/base) for the set descriptions, can be readonly (required)" \
    REPOSITORY_NAME="Name of repository for Identify call (required)" \
    RESUMPTION_TOKEN_TIMEOUT="How long time-to-live for resumption token n{d|h|m|s} (required)" \
    SCOPE="Environment (test/stg/prod) for cache name (required)" \
    USER_AGENT="Name of client to set in http requests (optional)" \
    X_FORWARDED_FOR="Ipranges of the nets where our own proxies can be located (default all private ipv4 nets)" \
    XOR_TEXT_ASCII="Atleast 8 characters of noise to obfuscate resumptionToken with (required)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
