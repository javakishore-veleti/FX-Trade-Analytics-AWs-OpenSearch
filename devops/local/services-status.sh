#!/bin/bash
# Reports which microservices are currently listening on their ports.

check_port() {
  PORT=$1
  NAME=$2
  if lsof -i :$PORT > /dev/null 2>&1; then
    echo "✅ $NAME running on port $PORT"
  else
    echo "❌ $NAME NOT running (port $PORT)"
  fi
}

echo "🧠 Microservices:"
check_port 9080 "Trade Service"
check_port 9081 "Risk Service"
check_port 9082 "Indexer Service"
check_port 9083 "Master Data Service"
