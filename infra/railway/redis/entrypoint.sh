#!/bin/sh
# Starts redis-server with auth from env (no file mounts on Railway, so no redis.conf file).
set -eu

: "${REDIS_PASSWORD:?REDIS_PASSWORD is required — an unauthenticated Redis on a network is not acceptable even on private networking}"

exec redis-server \
  --requirepass "$REDIS_PASSWORD" \
  --maxmemory 48mb \
  --maxmemory-policy noeviction \
  --appendonly yes \
  --dir /data
