#!/bin/bash

echo "🔧 Ensuring Docker network exists..."

docker network inspect fx-trade-analytics-aws-opensearch-network >/dev/null 2>&1 || \
docker network create fx-trade-analytics-aws-opensearch-network

echo "🚀 Starting Kafka..."
docker compose -f devops/local/kafka/docker-compose.yml up -d

echo "🚀 Starting Kafka UI..."
docker compose -f devops/local/kafka/kafka-ui/docker-compose.yml up -d

echo "🚀 Starting OpenSearch..."
docker compose -f devops/local/opensearch/docker-compose.yml up -d

#echo "🚀 Starting Observability..."
#docker compose -f devops/local/observability/docker-compose.yml up -d

#echo "🚀 Starting Postgres..."
#docker compose -f devops/local/postgres/docker-compose.yml up -d

echo "✅ All services started."