#!/bin/bash

set -e

NETWORK_NAME="fx-trade-analytics-aws-opensearch-network"

echo "🛑 Stopping and removing all services..."

docker compose -f docker-compose.yaml down -v
docker compose -f postgres/docker-compose.yaml down -v
docker compose -f observability/docker-compose.yml down -v
docker compose -f devops/local/opensearch/docker-compose.yml down -v
docker compose -f devops/local/kafka/docker-compose.yml down -v
docker compose -f devops/local/kafka/kafka-ui/docker-compose.yml down -v

echo ""
echo "🧹 Removing network..."

if docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  docker network rm "$NETWORK_NAME"
  echo "✅ Network removed: $NETWORK_NAME"
else
  echo "ℹ️ Network already removed"
fi

echo ""
echo "🧼 Optional: remove dangling volumes"
docker volume prune -f

echo ""
echo "✅ Full cleanup completed."