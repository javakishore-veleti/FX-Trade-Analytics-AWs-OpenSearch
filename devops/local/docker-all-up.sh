#!/bin/bash

echo "Starting all services..."
docker compose -f docker-compose.yaml up -d
docker compose -f postgres/docker-compose.yaml up -d
docker compose -f observability/docker-compose.yaml up -d

echo "All services started."