#!/bin/bash
# Stops all four Spring microservices by killing whatever is listening on
# their ports. Idempotent — silent if nothing is running.

kill_port() {
  PORT=$1
  NAME=$2
  PID=$(lsof -ti:$PORT || true)
  if [ -n "$PID" ]; then
    echo "🔥 Killing $NAME (port $PORT, pid $PID)"
    kill -9 $PID
  else
    echo "✅ $NAME already stopped (port $PORT)"
  fi
}

echo "🛑 Stopping microservices..."
kill_port 8080 "Trade Service"
kill_port 8081 "Risk Service"
kill_port 8082 "Indexer Service"
kill_port 8083 "Master Data Service"
echo "✅ Done."
