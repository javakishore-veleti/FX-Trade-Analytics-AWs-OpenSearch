# 🧠 CLAUDE.md — FX Trade Analytics Platform

---

# 🎯 PURPOSE OF THIS CODEBASE

This repository implements a **distributed FX trade analytics platform**.

## Primary Objective
Build a **real-time data pipeline** that:

1. Ingests FX trades
2. Processes risk
3. Stores data in OpenSearch
4. Enables analytics + dashboards

Read the entire repo.

Do NOT add new services.

Focus only on:
1. Validating Kafka → Risk → Indexer → OpenSearch flow
2. Fixing OpenSearch indexing and mapping issues
3. Ensuring data is visible in dashboards

Do changes incrementally and show diffs per file.

## Why this exists
This is NOT a CRUD app.

👉 This is a **streaming + analytics system** demonstrating:
- Event-driven architecture
- Kafka pipelines
- OpenSearch analytics
- Multi-region system design

---

# 🏗️ SYSTEM OVERVIEW

## Core Flow

Trade Service → Kafka → Risk Service → Kafka (enriched)
                                         ↓
                                   Indexer Service
                                         ↓
                                   OpenSearch
                                         ↓
                                  Dashboards / APIs

---

# 🧩 MODULES

## fx-trade-service
- Entry point (REST API)
- Produces `trade-events`
- Contains search APIs (OpenSearch)

## fx-risk-service
- Consumes `trade-events`
- Calculates risk
- Publishes `trade-events-enriched`
- Handles failures (DLQ partial)

## fx-opensearch-indexer
- Consumes enriched events
- Indexes into OpenSearch

## fx-common
- Shared DTOs (TradeEventDTO)

---

# 🧠 DESIGN PRINCIPLES

- Event-driven (Kafka is backbone)
- Loose coupling between services
- Streaming-first pipeline
- Analytics-first design

---

# 🎯 TARGET STATE (VERY IMPORTANT)

Claude must aim for THIS state — not over-engineer.

## ✅ Functional Target

- Trades flow end-to-end successfully
- Risk is calculated and added
- Data is indexed in OpenSearch
- Data is queryable via API
- Dashboards show real data

---

## ✅ Technical Target

- Index pattern:
  fx-trades-{region}

- Correct mapping:
  tradeId → keyword
  riskLevel → keyword
  region → keyword
  timestamp → date
  fromAmount → double

- Kafka pipeline stable
- No data loss
- DLQ working

---

## ✅ Observability Target

- Basic metrics available
- System debuggable via logs + Kafka + OpenSearch

---

# ⚠️ CURRENT STATE (REALITY)

System is mostly implemented but not fully validated.

---

# 🔴 PENDING TASKS

## CRITICAL (DO FIRST)

- Validate OpenSearch indexing end-to-end
- Ensure documents exist in indices
- Fix mapping issues (avoid dynamic mapping problems)

---

## HIGH PRIORITY

- Improve Search API (filters, structured response)
- Implement proper Kafka retry (not just try/catch)
- Validate dashboards with real data

---

## MEDIUM

- Add aggregation queries:
  - risk distribution
  - region analytics
- Standardize index naming

---

## LOW

- UI improvements
- Better test data generation

---

# 🔍 OPENSEARCH EXPECTATIONS

## Index Pattern
fx-trades-*

## Must Support
- search by risk
- search by region
- aggregations

---

# 🐳 LOCAL DEVELOPMENT

## Start Infra
npm run local:docker:up

## Start Apps
npm run local:app:run-all

---

# 🧠 RULES FOR CLAUDE

When modifying this codebase:

## DO
- Respect event-driven flow
- Keep services decoupled
- Use Kafka for communication
- Validate OpenSearch results

## DO NOT
- Convert to monolith
- Add unnecessary frameworks
- Over-engineer abstractions
- Break Kafka pipeline

---

## DEBUGGING ORDER (IMPORTANT)

1. Kafka topic (data present?)
2. Risk service logs
3. Enriched topic
4. Indexer logs
5. OpenSearch index

---

# 🚀 FINAL SUMMARY

This project is a:

Real-time FX analytics platform using Kafka + OpenSearch

Focus on:
- Data flow correctness
- Indexing correctness
- Analytics visibility
