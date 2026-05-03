#!/bin/bash

set -e

NETWORK_NAME="fx-trade-analytics-aws-opensearch-network"

echo "🛑 Stopping all services..."

echo "🧯 Stopping Kafka..."
docker compose -f devops/local/kafka/docker-compose.yml down -v

echo "🧯 Stopping Kafka UI..."
docker compose -f devops/local/kafka/kafka-ui/docker-compose.yml down -v

echo "🧯 Stopping OpenSearch..."
docker compose -f devops/local/opensearch/docker-compose.yml down -v

#echo "🧯 Stopping Observability..."
#docker compose -f devops/local/observability/docker-compose.yml down -v

#echo "🧯 Stopping Postgres..."
#docker compose -f devops/local/postgres/docker-compose.yml down -v

echo ""
echo "🧹 Removing network..."

if docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  docker network rm "$NETWORK_NAME"
  echo "✅ Network removed: $NETWORK_NAME"
else
  echo "ℹ️ Network already removed"
fi

echo ""
echo "🧼 Cleaning unused volumes..."
docker volume prune -f

echo ""
echo "✅ Full cleanup completed."