# 🧠 ChatGPT Memory Management — fx-trade-analytics-aws-opensearch

This file ensures smooth and efficient collaboration with ChatGPT during development.

Since ChatGPT does not persist memory across sessions reliably, this document acts as a **single source of truth for project context**.

---

# 🎯 Project Context

## Repo Name
fx-trade-analytics-aws-opensearch

## Goal
Build a demo project to showcase AWS OpenSearch cross-region UI using FX trade analytics.

---

# 🧩 Key Components

- fx-trade-simulator (data generator)
- fx-trade-service
- fx-risk-service
- fx-opensearch-indexer
- Kafka messaging
- OpenSearch multi-region dashboards

---

# ⚠️ Constraints

- Keep it **demo-focused**, not production-heavy
- Avoid unnecessary UI (Angular portals optional)
- Prioritize **cross-region analytics demonstration**

---

# 🧱 Coding Standards

## DTO-Based Design
- All APIs must use DTOs
- Naming:
  - Request → `*Req`
  - Response → `*Resp`

## Package Structure
```
api/
service/
service/impl/
dao/
entity/
dtos/
utils/
constants/
common/
```

## Common Module
- Module name: `fx-common`
- Contains:
  - Shared DTOs
  - Constants
  - Utilities

---

# 🧠 Architecture Evolution (NEW)

## Workflow-Based Design

API → Facade → Workflow → Tasks Pipeline

### Workflow Tasks
- ValidateTradeTask
- EnrichTradeTask
- PublishTradeTask (Kafka)
- BuildResponseTask

### Benefits
- Modular logic
- Easy debugging
- Clean orchestration

---

## Event-Driven Architecture

- Trade service publishes events to Kafka
- Indexer consumes events
- OpenSearch used for analytics

---

## Resilience (DLQ + Retry)

- Kafka retry with backoff
- Dead Letter Queue: `trade-events-dlq`
- Failed events routed automatically

---

## Observability

- Prometheus metrics
- Grafana dashboards
- Task-level metrics:
  - workflow.task.executed
  - workflow.task.failed

---

## Multi-Region Simulation

- Region-based indexing:
  - fx-trades-us-east-1
  - fx-trades-us-west-2
- Single dashboard view using pattern:
  - fx-trades-*

---

# 📊 Development Tracking

- Primary tracking file:
  👉 `README_Development_Plan.md`

- Execution model:
  - Phase-based
  - Sequential development
  - Status tracking via checkboxes

---

# ⚡ How to Work Efficiently with ChatGPT

## ✅ Always Refer Instead of Re-Explaining

Use short commands like:

- “Start Phase 1”
- “Continue Phase 3”
- “Update Phase 5 status”
- “Use project context”

---

## ✅ Use This File as Anchor

If context is lost or unclear:

👉 Say:
> “Refer to README_ChatGPT_MemoryMgmt.md”

---

## 🔁 Recommended Workflow

1. Ask ChatGPT to start a phase
2. Implement changes
3. Mark tasks complete
4. Move to next phase

---

# 📌 Guiding Principle

> Demo-first system showcasing OpenSearch cross-region analytics — not a full trading platform.

---

# 🔥 End Goal

Produce:
- Working demo system
- Blog post
- YouTube video

All aligned with:
👉 OpenSearch cross-region analytics capability
