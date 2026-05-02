#!/bin/bash

echo "Stopping all services..."
docker compose -f docker-compose.yaml down
docker compose -f postgres/docker-compose.yaml down
docker compose -f observability/docker-compose.yaml down

echo "All services stopped."