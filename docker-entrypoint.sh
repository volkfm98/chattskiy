#!/bin/sh
set -e

export NODE_ID="${NODE_ID:-$(cat /proc/sys/kernel/random/uuid)}"

echo "Starting Chattskiy with NODE_ID=${NODE_ID}"

exec java ${JAVA_OPTS} -jar app.jar