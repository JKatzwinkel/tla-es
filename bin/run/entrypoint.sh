#!/bin/sh

ES_PORT="${ES_PORT:-9200}"
ES_HOST="${ES_HOST:-localhost}"
ES_URL="${ES_HOST}:${ES_PORT}"
CMD=runserver

if [ $# -ge 1 ]; then
    if [ "$1" = "ingest" ]; then
        echo "download corpus data from ${SAMPLE_URL}..."
        if ! wget "${SAMPLE_URL}" -O sample.tar.gz; then
            echo "could not retrieve corpus data!"
            exit 1
        fi
        CMD=populate
    fi
fi

until wget -q --spider "${ES_URL}" 2>/dev/null; do
    echo "waiting for connection to ES instance at ${ES_URL}..."
    sleep 4
done

wget -q --spider "${ES_URL}" 1>/dev/null && echo "...connected."
echo "wait for cluster health to reach at least yellow..."
wget -q "${ES_URL}/_cluster/health?wait_for_status=yellow" -O /dev/stdout
echo

if [ "${CMD}" = "populate" ]; then
    echo "start TLA backend && populate database..."
    java -jar tla-backend.jar --data-file=sample.tar.gz --shutdown
else
    echo "run backend server..."
    java -jar tla-backend.jar
fi
