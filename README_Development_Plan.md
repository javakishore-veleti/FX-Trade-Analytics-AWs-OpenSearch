# 🚀 FX Trade Analytics Platform – Development Plan (UPDATED)

## 🎯 Objective

Build a distributed FX trade analytics platform using:
- Spring Boot microservices
- Kafka (event-driven architecture)
- OpenSearch (analytics + dashboards)
- Multi-region capability
- Developer-friendly orchestration

---

# 🧱 Architecture Overview

Trade Service → Kafka → Risk Service → Kafka (enriched)
                                         ↓
                                  OpenSearch Indexer

---

# ✅ COMPLETED

## Core Platform
- Monorepo + multi-module build
- fx-common shared DTO
- Trade producer → Kafka
- Risk consumer + enrichment
- Basic DLQ handling (manual)

## Event Flow
- trade-events topic
- trade-events-enriched topic
- End-to-end Kafka pipeline working

## DevOps
- Docker orchestration scripts
- Kafka + OpenSearch local setup
- One-command startup/shutdown

---

# 🟡 PARTIALLY IMPLEMENTED

## Risk Layer
- RiskCalculator implemented
- Enrichment working
- ❗ No proper retry/backoff strategy

## DLQ
- Basic DLQ push exists
- ❗ No retry policy
- ❗ No replay mechanism

---

# ❗ NOT FULLY IMPLEMENTED (CRITICAL GAPS)

## 🔴 OpenSearch Indexing
- ❗ Indexer may exist but not verified end-to-end
- ❗ No confirmed mapping enforcement
- ❗ No validation of stored documents

## 🔴 Query Layer
- ❌ No backend search API
- ❌ UI cannot fetch data programmatically

## 🔴 Dashboards
- ⚠️ NDJSON present
- ❗ Auto-loading not guaranteed
- ❗ Data binding not verified

## 🔴 Observability
- ❌ No Prometheus metrics
- ❌ No Grafana dashboards wired
- ❌ No tracing (Jaeger not integrated)

## 🔴 UI Integration
- ❌ Portals not connected to backend
- ❌ No live data visualization

---

# ⏳ PENDING (TRUE STATE)

## 1️⃣ Complete OpenSearch Layer (CRITICAL)
- Implement/verify indexer consumer
- Add index mapping (riskLevel, timestamp, region)
- Validate documents via API

## 2️⃣ Search API
- Add REST endpoint to query OpenSearch
- Enable UI/backend integration

## 3️⃣ DLQ + Retry (Production Grade)
- Retry with backoff (3 attempts)
- Structured DLQ topics
- Replay mechanism

## 4️⃣ Observability
- Spring Boot actuator
- Prometheus integration
- Grafana dashboards

## 5️⃣ Data Generator
- Script/service to generate trades
- Continuous stream for demo

## 6️⃣ UI Integration
- Connect portals to APIs
- Display analytics data

---

# 🧠 CURRENT STATUS

| Layer | Status |
|------|-------|
| Build | ✅ |
| Kafka Pipeline | ✅ |
| Risk Engine | ✅ |
| OpenSearch Indexing | ⚠️ |
| Query API | ❌ |
| Dashboards | ⚠️ |
| DLQ + Retry | ⚠️ |
| DevOps | ✅ |
| UI | ❌ |
| Observability | ❌ |
| AWS | ❌ |

---

# 🚀 NEXT CODING TASK (IMMEDIATE)

## 🔥 IMPLEMENT: OpenSearch Indexer (END-TO-END)

### Goal:
Consume enriched events and store in OpenSearch with proper mapping.

### Steps:
1. Consume `trade-events-enriched`
2. Deserialize DTO
3. Create index: `fx-trades-{region}`
4. Index document
5. Verify via REST API

---

# 🎯 AFTER THAT

1. Add search API
2. Verify dashboards with real data
3. Add retry + DLQ improvements

---

# 💡 TRUTH

You have a strong event-driven backbone.

👉 What’s missing is making data:
- searchable
- visible
- reliable

That’s what we complete next.
