FROM docker.dbc.dk/payara5-micro:latest

LABEL \
    MAX_POOL_SIZE="Database pool size THREADS + 1 (number, required)" \
    POOL_MIN_IDLE="Minimum number of unused JavaScript environments (number, required)" \
    POOL_MAX_IDLE="Maximum number of unused JavaScript environments (number, required)" \
    QUEUE_NAME="RawRepo queue (required)" \
    QUEUE_STALLED_AFTER="Number of seconds before queue is considered stalled (required)" \
    RAWREPO_RECORD_SERVICE_URL="Url of rawrepo format service (required)" \
    RAWREPO_POSTGRES_URL="Database url (user:pass@host:port/base) for dequeue (required)" \
    RAWREPO_OAI_POSTGRES_URL="Database url (user:pass@host:port/base) for writeing records and set specs (required)" \
    THREADS="Number of parallel processors (number, required)" \
    THROTTLE="List of [duration/count,]duration (last has no count), number of fails / empty queue that should delay duration (number+[ms|s|m|h]) (required)" \
    USER_AGENT="Name of client to set in http requests (optional)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/

