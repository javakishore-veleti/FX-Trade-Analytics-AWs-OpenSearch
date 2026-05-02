# 🚀 FX Trade Analytics Platform (AWS + OpenSearch)

A distributed FX trade analytics platform using:
- Spring Boot microservices
- Kafka (event-driven)
- OpenSearch (analytics)
- Docker-based local orchestration

---

## 📚 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [First-Time Setup](#first-time-setup)
- [Daily Usage](#daily-usage)
- [Stop Everything](#stop-everything)
- [Check Status](#check-status)
- [Key URLs](#key-urls)

---

## Overview

This project demonstrates a real-world FX analytics system with:
- Trade ingestion API
- Kafka event streaming
- Risk enrichment service
- OpenSearch indexing
- Dashboards

---

## Architecture

Trade Service → Kafka → Risk Service → Kafka → OpenSearch Indexer → OpenSearch → Dashboards

---

## Project Structure

```
devops/local/
  ├── kafka/
  ├── opensearch/
  ├── observability/
  ├── postgres/
  ├── docker-all-up.sh
  ├── docker-all-down.sh
  ├── status-all.sh
  ├── shutdown-all.sh

middleware/
  ├── fx-trade-service
  ├── fx-risk-service
  ├── fx-opensearch-indexer
```

---

## Prerequisites

- Java 17+
- Maven
- Node.js
- Docker Desktop

---

## First-Time Setup

### Install dependencies

```
npm install
```

### Make scripts executable

```
chmod +x devops/local/*.sh
```

### Create Docker network (one-time)

```
docker network create fx-trade-analytics-aws-opensearch-network
```

---

## Daily Usage

### Start everything

```
npm run local:start
```

### Background (daemon mode)

```
npm run fx:start
```

---

## Stop Everything

```
npm run local:stop
```

or

```
npm run fx:stop
```

---

## Check Status

```
npm run local:status
```

---

## Key URLs

| Service | URL |
|--------|-----|
| Trade API | http://localhost:8080 |
| Risk Service | http://localhost:8081 |
| Indexer | http://localhost:8082 |
| OpenSearch | http://localhost:9200 |
| Dashboards | http://localhost:5601 |
| Kafka UI | http://localhost:8080 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Jaeger | http://localhost:16686 |

---

## 🎯 Quick Commands

```
npm run local:start
npm run local:status
npm run local:stop
```
