#!/bin/bash

echo "=============================="
echo "🚀 FX PLATFORM STATUS"
echo "=============================="

# -----------------------------
# 🐳 Docker Status
# -----------------------------
echo ""
echo "🐳 Docker Containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# -----------------------------
# 🌐 Network Status
# -----------------------------
NETWORK_NAME="fx-trade-analytics-aws-opensearch-network"

echo ""
echo "🌐 Network:"
if docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  echo "✅ Network exists: $NETWORK_NAME"
else
  echo "❌ Network NOT found: $NETWORK_NAME"
fi

# -----------------------------
# 🔍 Function to check port
# -----------------------------
check_port() {
  PORT=$1
  NAME=$2

  if lsof -i :$PORT > /dev/null 2>&1; then
    echo "✅ $NAME running on port $PORT"
  else
    echo "❌ $NAME NOT running (port $PORT)"
  fi
}

echo ""
echo "🧠 Microservices:"
check_port 8080 "Trade Service"
check_port 8081 "Risk Service"
check_port 8082 "Indexer Service"

echo ""
echo "🖥️ UI Apps:"
check_port 4200 "Admin Portal"
check_port 4201 "Customer Portal"

echo ""
echo "📊 Key Infra Ports:"
check_port 9092 "Kafka"
check_port 9200 "OpenSearch"
check_port 5601 "OpenSearch Dashboards"
check_port 3000 "Grafana"
check_port 9090 "Prometheus"

echo ""
echo "=============================="
echo "✅ Status Check Complete"
echo "=============================="