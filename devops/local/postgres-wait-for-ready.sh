#!/bin/bash
# Polls the local Postgres container until it accepts connections. Used by
# `npm run localhost:app:postgres:run-all` to ensure the migration runner
# doesn't race the container's startup.

set -e

CONTAINER=fx-postgres
USER=fxuser
DB=fxdb
MAX_ATTEMPTS=60   # 60 * 1s = 1 min cap

echo "⏳ Waiting for Postgres ($CONTAINER) to accept connections..."
attempts=0
until docker exec "$CONTAINER" pg_isready -U "$USER" -d "$DB" >/dev/null 2>&1; do
  attempts=$((attempts + 1))
  if [ "$attempts" -ge "$MAX_ATTEMPTS" ]; then
    echo "❌ Postgres didn't become ready after ${MAX_ATTEMPTS}s. Check 'docker logs $CONTAINER'."
    exit 1
  fi
  sleep 1
done
echo "✅ Postgres ready."
