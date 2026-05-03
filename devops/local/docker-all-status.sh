#!/bin/bash

NETWORK_NAME="fx-trade-analytics-aws-opensearch-network"

echo "📊 FX PLATFORM STATUS"
echo "=================================="

echo ""
echo "🟢 Running Containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "🔴 Stopped Containers:"
docker ps -a --filter "status=exited" --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "🔍 Core Services Check:"

check_service() {
  NAME=$1
  if docker ps --format "{{.Names}}" | grep -q "$NAME"; then
    echo "✅ $NAME is running"
  else
    echo "❌ $NAME NOT running"
  fi
}

check_service "kafka"
check_service "opensearch"
check_service "grafana"
check_service "prometheus"
check_service "postgres"

echo ""
echo "🌐 Network Check:"
if docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  echo "✅ Network exists: $NETWORK_NAME"
else
  echo "❌ Network missing"
fi

echo ""
echo "📡 OpenSearch Health Check:"
curl -s http://localhost:9200 >/dev/null && echo "✅ OpenSearch responding" || echo "❌ OpenSearch not responding"

echo ""
echo "📡 Kafka Port Check:"
nc -z localhost 9092 >/dev/null 2>&1 && echo "✅ Kafka port open" || echo "❌ Kafka not reachable"

echo ""
echo "=================================="
echo "✅ Status check complete"