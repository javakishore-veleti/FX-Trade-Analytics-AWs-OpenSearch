#!/bin/bash

set -e

NETWORK_NAME="fx-trade-analytics-aws-opensearch-network"

echo "🔧 Ensuring Docker network exists..."

if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  docker network create "$NETWORK_NAME"
  echo "✅ Network created: $NETWORK_NAME"
else
  echo "ℹ️ Network already exists: $NETWORK_NAME"
fi

echo "🚀 Starting core services..."

docker compose -f docker-compose.yaml up -d
docker compose -f postgres/docker-compose.yaml up -d
docker compose -f observability/docker-compose.yml up -d

echo "🚀 Starting OpenSearch..."
docker compose -f devops/local/opensearch/docker-compose.yml up -d

echo "🚀 Starting Kafka..."
docker compose -f devops/local/kafka/docker-compose.yml up -d

echo "🚀 Starting Kafka UI..."
docker compose -f devops/local/kafka/kafka-ui/docker-compose.yml up -d

echo "✅ All services started successfully."