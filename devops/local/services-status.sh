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
check_port 8080 "Trade Service"
check_port 8081 "Risk Service"
check_port 8082 "Indexer Service"
check_port 8083 "Master Data Service"
