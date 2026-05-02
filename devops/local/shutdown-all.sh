#!/bin/bash

echo "=============================="
echo "🛑 SHUTTING DOWN FX PLATFORM"
echo "=============================="

# -----------------------------
# Kill Spring Boot + UI ports
# -----------------------------
kill_port() {
  PORT=$1
  NAME=$2

  PID=$(lsof -ti:$PORT)

  if [ ! -z "$PID" ]; then
    echo "🔥 Killing $NAME (port $PORT, pid $PID)"
    kill -9 $PID
  else
    echo "✅ $NAME already stopped"
  fi
}

echo ""
echo "🧠 Stopping Microservices..."

kill_port 8080 "Trade Service"
kill_port 8081 "Risk Service"
kill_port 8082 "Indexer Service"

echo ""
echo "🖥️ Stopping UI..."

kill_port 4200 "Admin Portal"
kill_port 4201 "Customer Portal"

# -----------------------------
# Stop Docker
# -----------------------------
echo ""
echo "🐳 Stopping Docker containers..."
bash devops/local/docker-all-down.sh

echo ""
echo "=============================="
echo "✅ ALL SYSTEMS STOPPED"
echo "=============================="