#!/bin/bash

echo "📊 Docker Services Status"
echo "----------------------------------"

echo ""
echo "🟢 Running Containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "🔴 Stopped Containers:"
docker ps -a --filter "status=exited" --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "🔍 Key Services Check:"
docker ps --format "{{.Names}}" | grep -E "kafka|opensearch|grafana|prometheus|postgres" || echo "No key services running"

echo ""
echo "🌐 Network Check:"
docker network inspect fx-trade-analytics-aws-opensearch-network >/dev/null 2>&1 \
  && echo "✅ Network exists" \
  || echo "❌ Network missing"

echo ""
echo "✅ Status check complete"