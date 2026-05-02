#!/bin/bash

echo "Checking status of all services..."
docker ps
docker ps | grep opensearch
