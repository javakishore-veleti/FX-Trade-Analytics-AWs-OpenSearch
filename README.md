# 🚀 FX Trade Analytics Platform (AWS + OpenSearch)

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-brightgreen)
![Kafka](https://img.shields.io/badge/Kafka-Event--Driven-black)
![OpenSearch](https://img.shields.io/badge/OpenSearch-Analytics-orange)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue)

---

## 📑 Table of Contents

- [🎥 Demo (System in Action)](#-demo-system-in-action)
- [📸 Screenshots](#-screenshots)
- [🧠 Architecture Diagram (Editable)](#-architecture-diagram-editable)
- [🌍 Core Idea](#-core-idea-what-makes-this-project-special)
- [🎬 System Overview](#-system-overview)
- [🔁 Daily Developer Workflow](#-daily-developer-workflow-recommended)
- [🎯 One Command Mode](#-one-command-mode)
- [🌐 Access URLs](#-access-urls)
- [🧭 OpenSearch Use Cases You Can Build on This Codebase](#-opensearch-use-cases-you-can-build-on-this-codebase)
  - [Region-Local Use Cases (single OpenSearch domain)](#region-local-use-cases-single-opensearch-domain)
    - [Trade lifecycle & operations](#trade-lifecycle--operations)
    - [Risk monitoring](#risk-monitoring)
    - [Volume & P&L analytics](#volume--pl-analytics)
    - [Compliance & audit (region-bound)](#compliance--audit-region-bound)
    - [Pipeline observability](#pipeline-observability)
  - [Unified / Cross-Region Use Cases (OpenSearch UI federation)](#unified--cross-region-use-cases-opensearch-ui-federation)
    - [Global situational awareness](#global-situational-awareness)
    - [Cross-region risk & compliance](#cross-region-risk--compliance)
    - [Cross-region aggregations](#cross-region-aggregations)
    - [Federated search](#federated-search)
    - [Multi-region operations & cost](#multi-region-operations--cost)
    - [Adjacent extensions (scaffolding ready)](#adjacent-extensions-scaffolding-ready)
- [🔥 Highlights](#-highlights)
- [🛠️ AWS OpenSearch Implementation Plan](#️-aws-opensearch-implementation-plan)
  - [Goal](#goal)
  - [Prerequisites — AWS Management Console setup](#prerequisites--aws-management-console-setup-before-running-anything)
  - [Deployment modes](#deployment-modes)
  - [Provider abstraction (factory pattern)](#provider-abstraction-factory-pattern)
  - [Pagination strategy](#pagination-strategy)
  - [GitHub Actions workflows for AWS provisioning](#github-actions-workflows-for-aws-provisioning)
  - [Open design decisions](#open-design-decisions)
  - [Companion document](#companion-document)

---

# 🎥 Demo (System in Action)

> _Demo recording pending. Drop the file at_ `docs/screenshots/demo.gif` _and the embed below will render._
> Record with QuickTime (Mac), OBS Studio, or Loom — then export as GIF.

<!-- ![Demo GIF](docs/screenshots/demo.gif) -->

---

# 📸 Screenshots

> _Screenshots pending. Drop PNGs into_ `docs/screenshots/` _at the paths below and uncomment the embeds._

## 🖥️ OpenSearch Dashboards
> Expected at `docs/screenshots/opensearch-dashboard.png`
<!-- ![OpenSearch Dashboard](docs/screenshots/opensearch-dashboard.png) -->

## 📊 Grafana Metrics
> Expected at `docs/screenshots/grafana-dashboard.png`
<!-- ![Grafana Dashboard](docs/screenshots/grafana-dashboard.png) -->

## 🧠 Kafka UI
> Expected at `docs/screenshots/kafka-ui.png`
<!-- ![Kafka UI](docs/screenshots/kafka-ui.png) -->

---

# 🧠 Architecture Diagram (Editable)

👉 File location:
docs/architecture/fx-architecture.drawio

👉 Open in:
https://app.diagrams.net

> Build a **real-world distributed FX analytics system** powered by Kafka, OpenSearch, and AWS cross-region capabilities.

---

# 🌍 Core Idea (What makes this project special)

This project demonstrates how to build a **global analytics platform** using:

🔥 **AWS OpenSearch Cross-Region UI Access**

- Query data across multiple AWS regions
- No data replication required
- No endpoint switching
- Supports cross-account + cross-region
- Works with IAM + Identity Center

👉 This enables **centralized analytics on globally distributed trading data** while keeping data local.

---

# 🎬 System Overview

```mermaid
flowchart LR
    subgraph Users
        CUST[Customer Portal]
        ADMIN[Admin Portal]
    end

    subgraph Backend
        TS[Trade Service]
        RS[Risk Service]
        IDX[Indexer]
    end

    subgraph Streaming
        K[(Kafka)]
    end

    subgraph Storage
        OS[(OpenSearch - Multi Region)]
    end

    subgraph Analytics
        UI[OpenSearch UI - Cross Region]
    end

    CUST -->|Create Trades| TS
    ADMIN -->|Monitor & Manage| TS

    TS --> K
    K --> RS
    RS --> K
    K --> IDX
    IDX --> OS

    OS --> UI
```

---

# 🔁 Daily Developer Workflow (Recommended)

## 🟢 Step 1 — Start Infra

```bash
npm run local:docker:up
```

## 🟡 Step 2 — Start Apps

```bash
npm run local:app:run-all
npm run local:ui:run-all
```

---

# 🎯 One Command Mode

```bash
npm run local:start
npm run local:status
npm run local:stop
```

---

# 🌐 Access URLs

| Service | URL |
|--------|-----|
| Trade API | http://localhost:8080 |
| Risk Service | http://localhost:8081 |
| Indexer | http://localhost:8082 |
| OpenSearch | http://localhost:9200 |
| Dashboards | http://localhost:5601 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Jaeger | http://localhost:16686 |

---

# 🧭 OpenSearch Use Cases You Can Build on This Codebase

This codebase is a **reference implementation** for FX trade analytics — but the OpenSearch patterns it ships with apply to any high-volume, region-partitioned event stream. Use cases split into two camps:

- **Region-Local** — built against a single OpenSearch domain (e.g. `fx-trades-us-east-1`). Each regional desk owns its own data, low latency, residency-bound.
- **Unified / Cross-Region** — built against the index pattern `fx-trades-*` via the new [OpenSearch UI cross-region data access](https://aws.amazon.com/about-aws/whats-new/2026/05/opensearch-ui-cross-region-data-access-domains/) feature (May 2026). Federates queries across all regional domains as a single pane of glass — without moving any data.

> **Index naming convention:** every trade is indexed into `fx-trades-{region}` — so a query against `fx-trades-eu-west-1` is region-local, and `fx-trades-*` is unified.

---

## Region-Local Use Cases (single OpenSearch domain)

These run inside one Region against one domain. Data residency preserved, latency local, no cross-region permissions needed.

### Trade lifecycle & operations
| Use case | OpenSearch query shape |
|---|---|
| Real-time trading-floor heads-up display | `term:region` + `range:timestamp` + `sort:timestamp desc` |
| Latest N trades per trader book | `term:traderBook` + `sort:timestamp desc` + `size:N` |
| Trade ticket lookup by ID | `term:tradeId` |
| Recent activity for a single currency pair | `term:fromCurrency` + `term:toCurrency` |

### Risk monitoring
| Use case | OpenSearch query shape |
|---|---|
| Risk-distribution dashboard for the desk | `terms` aggregation on `riskLevel` |
| HIGH-risk trade queue (for manual review) | `term:riskLevel=HIGH` + `range:timestamp` |
| Spike detection on HIGH-risk count | `date_histogram` on `timestamp` + filtered by `riskLevel=HIGH` |
| Per-trader-book risk profile | `terms:traderBook` + sub-agg `terms:riskLevel` |

### Volume & P&L analytics
| Use case | OpenSearch query shape |
|---|---|
| Per-region volume by currency pair | `terms:fromCurrency,toCurrency` + `sum:fromAmount` |
| Trade rate over time (live tick chart) | `date_histogram:timestamp` + `count` |
| Top trader books by notional | `terms:traderBook` + `sum:fromAmount` + `size:N` |
| Average / p95 trade size per pair | `terms:pair` + `avg/percentiles:fromAmount` |

### Compliance & audit (region-bound)
| Use case | Why region-local matters |
|---|---|
| MiFID II transaction reporting (EU region) | EU data must stay in EU — `fx-trades-eu-west-1` only |
| RBI India localization reporting | Same, for `fx-trades-ap-south-1` |
| MAS Singapore trade-data access | Domain-bound queries, never federated |
| GDPR Article 15 subject-data export | Per-region, no cross-border movement |
| Audit trail replay for a specific session | `range:timestamp` within one regional index |

### Pipeline observability
| Use case | OpenSearch query shape |
|---|---|
| Per-region indexer throughput | `date_histogram:timestamp` + `count` on the region's index |
| Per-region pipeline lag (ingest vs index) | Compare `timestamp` vs `_doc.@indexed_at` (if added) |
| DLQ inspection (per-service DLQ topic) | Combined with Kafka UI; OpenSearch logs the originally indexed events |

---

## Unified / Cross-Region Use Cases (OpenSearch UI federation)

These query `fx-trades-*` from a **single OpenSearch UI application**. The UI federates the query at runtime — data stays in its origin region, only result rows travel back. Compliance preserved, egress minimised.

### Global situational awareness
| Use case | What it answers |
|---|---|
| Global trading book consolidated view | "What are all my desks doing right now, anywhere in the world?" |
| Live worldwide trade volume + risk dashboard | One Kibana / OpenSearch UI dashboard across regions |
| Top currency pairs by global volume (24h) | `terms:fromCurrency,toCurrency` over `fx-trades-*` |
| Global head-of-desk ops view | Per-region columns in a single dashboard |

### Cross-region risk & compliance
| Use case | What it answers |
|---|---|
| Global HIGH-risk trade hunt | "Show me all HIGH-risk USD trades, last 24h, anywhere" |
| Cross-region wash-trade detection | Joins on `traderBook` × `pair` × `time-window` across regions |
| Cross-region anomaly / coordinated-activity hunt | Same trader, multiple regions, same minute |
| AML investigation across borders | Transactions for a counterparty across all regional domains |
| Group-level oversight | One compliance officer, all jurisdictions, one query |

### Cross-region aggregations
| Use case | OpenSearch query shape |
|---|---|
| Cross-region P&L roll-up by trader book | `terms:traderBook` + `sum:fromAmount` over `fx-trades-*` |
| Currency pair volume rankings (global) | `terms:fromCurrency,toCurrency` + `cardinality:tradeId` |
| Time-series global trade rate | `date_histogram:timestamp` + per-region sub-agg |
| Per-region performance comparison | `terms:region` + `avg:fromAmount`, etc. |
| Global risk distribution heatmap | `terms:region` × `terms:riskLevel` |

### Federated search
| Query | Pattern |
|---|---|
| "All USD/INR HIGH-risk trades, last 24h, anywhere" | `fx-trades-*` + `term:fromCurrency=USD` + `term:toCurrency=INR` + `term:riskLevel=HIGH` + `range:timestamp` |
| "All trades for trader-book FX-BOOK across regions" | `fx-trades-*` + `term:traderBook=FX-BOOK` |
| "Tradeid lookup federated" | `fx-trades-*` + `term:tradeId=...` |
| "Region-A trades that involve a Region-B currency" | `fx-trades-*` + filter on `region` and currency code |

### Multi-region operations & cost
| Use case | What it answers |
|---|---|
| Multi-region platform-health dashboard | One Kibana for indexer health across all clusters |
| Cross-region indexer DLQ visibility | Federated count + drill-down for `*-dlq` index |
| Cost / volume tracking across regions | `terms:region` + `sum:fromAmount` and document counts |
| Combined cross-account + cross-region tenant view | Multi-account orgs see all environments at once |

### Adjacent extensions (scaffolding ready)
The same Kafka → enrichment → OpenSearch indexing pattern naturally extends to:
| Extension | What changes |
|---|---|
| **Sanctions screening (OFAC, EU, UK SDN lists)** | Swap currency-pair allow-list for sanctions list; trade-service plumbing identical |
| **AML pattern detection** | Add a graph-aware enrichment service consuming the same Kafka topic |
| **Counterparty credit-limit checking** | New consumer next to `risk-service`; same DLQ patterns |
| **Real-time market-data overlay** | Additional indexer writing rate quotes to `fx-rates-{region}`; UI joins via lookup |
| **Multi-jurisdiction transaction reporting** | Existing trade index is the source-of-truth; reports run as scheduled aggregations per region |

---

# 🔥 Highlights

- Event-driven microservices
- Real-time analytics pipeline
- Cross-region OpenSearch analytics
- Clean developer workflow
- Production-style observability

---

# 🛠️ AWS OpenSearch Implementation Plan

> **Status:** Design phase — no code yet. Consolidates the design decisions for showcasing the new [AWS OpenSearch UI cross-region data access](https://aws.amazon.com/about-aws/whats-new/2026/05/opensearch-ui-cross-region-data-access-domains/) feature (May 1, 2026) using this codebase.

## Goal

Showcase **AWS OpenSearch UI cross-region data access** end-to-end on this codebase by:

1. Standing up **AWS OpenSearch domains in three regions** (`us-east-1`, `eu-west-1`, `ap-south-1`) via GitHub Actions workflows.
2. Letting the application — **whether running locally on a laptop or deployed to ECS** — write into the right regional OpenSearch domain based on each trade's `region` field.
3. Running the **AWS-managed OpenSearch UI in `us-east-1`** that federates queries across all three regional domains as a single pane of glass — without moving data.
4. Building a **companion search view in the admin portal** with region radio buttons (single-region only; cross-region demo lives in the AWS UI).

## Prerequisites — AWS Management Console setup before running anything

Do these **once per AWS account** before `npm run setup:aws:iam-all` (or any GitHub Actions workflow) will work. All clicks happen in the [AWS Management Console](https://console.aws.amazon.com).

### 1 · Have an AWS account

Sign up at [aws.amazon.com](https://aws.amazon.com) if you don't have one. Free Tier covers most experimentation; the project resources (OpenSearch domains, NAT gateways, etc.) are paid — budget roughly **~$350/mo** while the 7-region demo is running, **~$70/mo** when domains are paused.

### 2 · Create a dedicated bootstrap admin IAM user

**Don't use your root account access keys** — AWS strongly discourages it, and you can't delete root keys cleanly. Create a dedicated IAM user that exists only for the one-time bootstrap.

1. Sign in to the AWS Console.
2. Go to **IAM** (in the search bar) → **Users** in the left sidebar.
3. Click **Create user** (top-right).
4. **User name**: `fx-bootstrap-admin` (or any name you prefer — this is the one-time admin, *not* the deployer that workflows use).
5. **Provide user access to the AWS Management Console**: leave this **UNCHECKED**. This user only needs programmatic access (CLI / API).
6. Click **Next**.
7. **Permissions options**: select **Attach policies directly**.
8. In the policy search box, type `AdministratorAccess` and check the box next to it.
9. Click **Next** → **Create user**.

> ✋ The screen at this point is for **creating an IAM User**. If you accidentally end up at a "Create role" screen with a "Custom trust policy" editor, back out — you're in the wrong place. Roles are different from users, and we want a User here.

### 3 · Generate an access key for that user

1. Click into the user you just created (`fx-bootstrap-admin`).
2. Open the **Security credentials** tab.
3. Scroll to **Access keys** → click **Create access key**.
4. **Use case**: pick **Application running outside AWS** (or **Command Line Interface**).
5. Click **Next** (description is optional) → **Create access key**.
6. **Save the Secret access key NOW.** It is shown only once. The Access key ID can be retrieved later, but the secret cannot.

### 4 · Install the local CLI tools the bootstrap script depends on

```bash
# macOS (Homebrew)
brew install awscli jq

# Linux (Debian/Ubuntu)
sudo apt update && sudo apt install -y awscli jq
```

Verify both are on `PATH`:

```bash
aws --version    # → aws-cli/2.x.x
jq --version     # → jq-1.x
```

### 5 · Export the bootstrap admin credentials in your shell

```bash
export FX_TRADE_ANALYTICS_AWS_ACCESS_KEY=<Access key id from step 3>
export FX_TRADE_ANALYTICS_AWS_SECRET=<Secret access key from step 3>
# Optional (defaults to us-east-1; IAM is global so any region works):
export AWS_REGION=us-east-1
```

These are project-namespaced env vars so they don't collide with any shell-wide `AWS_*` you may already have. The script reads them and maps to standard `AWS_*` internally.

### 6 · Run the IAM bootstrap

```bash
npm run setup:aws:iam-all
```

The script (idempotent on the first 5 steps) creates:
- Customer Managed Policy `fx-trade-opensearch-policy`
- IAM Group `fx-trade-opensearch-deployers`
- IAM User `fx-trade-opensearch-github-deployer`
- Group membership
- A fresh access-key pair for the deployer user

The new keys are printed to your terminal. Copy them into **GitHub repo secrets** (Settings → Secrets and variables → Actions → New repository secret):

| Secret name | Value |
|---|---|
| `AWS_ACCESS_KEY_ID`     | from script output |
| `AWS_SECRET_ACCESS_KEY` | from script output |

### 7 · Clean up

```bash
unset FX_TRADE_ANALYTICS_AWS_ACCESS_KEY FX_TRADE_ANALYTICS_AWS_SECRET
```

(Or just close the shell.) Optionally, in the AWS Console, **delete the access keys on `fx-bootstrap-admin`** (Security credentials tab → Actions → Delete) — you don't need them again unless you re-run the bootstrap (e.g. to update the policy or add another deployer).

You're ready. Every workflow under `.github/workflows/` can now run.

> 📖 Detailed walkthrough + troubleshooting in [`.github/aws/configs/README.md`](.github/aws/configs/README.md). Common terminology pitfalls (Roles vs Users, trust policy errors) covered there.

## Deployment modes

Same code, different config:

| Mode | Compute lives | OpenSearch lives | When |
|---|---|---|---|
| **Local dev** | Developer's laptop | AWS OpenSearch domains (3 regions) | While building & recording the demo |
| **AWS** | ECS Fargate (per region) | AWS OpenSearch domains (same 3) | Production target |

The OpenSearch domains are the **same set of AWS-managed instances** in both modes. Only the compute origin changes.

## Provider abstraction (factory pattern)

Application talks to OpenSearch through a **factory that returns a stateless, region-keyed singleton client**. Each region's client is built once at startup with the right transport:

| Region's `provider` | Transport | Auth |
|---|---|---|
| `local` | Apache HTTP | None |
| `aws`   | `AwsSdk2Transport` from `opensearch-java` | SigV4 (uses local AWS profile in dev, ECS task role in prod) |
| _(future)_ `azure`, `gcp` | TBD | TBD |

**One SDK** (`opensearch-java`), two transports — not two SDKs. AWS provider only does data plane (`index`, `search`); control-plane operations (create/delete domains) live in the GitHub Actions CloudFormation workflows.

**Config schema (illustrative):**
```yaml
fx.opensearch.backends:
  - region: us-east-1
    provider: aws
    endpoint: https://fx-trades-use1.us-east-1.es.amazonaws.com
  - region: eu-west-1
    provider: aws
    endpoint: https://fx-trades-euw1.eu-west-1.es.amazonaws.com
  - region: ap-south-1
    provider: aws
    endpoint: https://fx-trades-aps1.ap-south-1.es.amazonaws.com
  - region: local-dev
    provider: local
    endpoint: http://localhost:9200
```

## Pagination strategy

Server cap: **200 hits per page** (for `_source` retrieval; aggregations exempt).
Client UX: **20 rows per visible page** → 10 client pages per server chunk.

| Behaviour | Implementation |
|---|---|
| Total count | Returned with every search response (cheap in OpenSearch) |
| Prefetch trigger | When the user reaches client page 9 of 10, prefetch the next 200 |
| Client cache | Sliding window of recent chunks (LRU); invalidated on filter/sort change |
| Pagination type | Start with `from/size` (≤ 2k results); upgrade to `search_after` cursor if scaling demands |

## GitHub Actions workflows for AWS provisioning

All workflows are **manual-trigger** (`workflow_dispatch`), prefixed by sequence number. New OpenSearch workflows extend the existing 001/002/003 family. Each workflow accepts a **multi-select region input** (one or more or all). Operations are **idempotent** — if the resource already exists, gracefully skip and log.

| Workflow | What it does |
|---|---|
| `001-AWS-Initial-Setup-VPC` | Per region: create new VPC **or** reuse an existing VPC if its ID is supplied as input |
| `001-AWS-Destroy-VPC` | Tear down VPCs created by the setup workflow (skips supplied/existing) |
| `004-AWS-Setup-Region-OpenSearch` | Per selected region: create AWS OpenSearch domain. Skip-if-exists with informational message. |
| `004-AWS-Destroy-Region-OpenSearch` | Per selected region: delete the OpenSearch domain |
| `995-AWS-All-Setup` | Orchestrator — runs all setup workflows in dependency order |
| `996-AWS-All-Destroy` | Orchestrator — runs all destroy workflows in reverse order |

Existing workflows `002-AWS-Initial-Setup-IAM-Roles` and `003-AWS-Initial-Setup-ECR` (and their destroy pairs) remain unchanged; the new OpenSearch workflows slot in at `004`.

## Open design decisions

These need answers before any code is written. The full discussion lives in `README_OpenSearch_Core_Impl_DevPlan.md`.

| # | Decision | My recommendation |
|---|---|---|
| **1** | `createOrder` vs `placeTrade` — same thing? | Same — keep existing `POST /api/trades/place` name |
| **2** | What is `publishIndex`? (REST endpoint, internal step, or rename of Kafka→indexer?) | Internal indexer write — not a new REST endpoint |
| **3** | Is `local` OpenSearch still in scope as a provider? | Yes — for offline iteration; AWS is what the demo shows |
| **4** | Why `cloudProvider` in every request DTO? | Make it optional override; default comes from server config keyed by `region` |
| **5** | Pagination cache scope | Sliding window of recent chunks (LRU), invalidated on filter/sort change |
| **6** | Prefetch direction | Forward only on initial impl; bidirectional later if needed |
| **7** | "AWS OpenSearch SDK" — which one? | `opensearch-java` + `AwsSdk2Transport` (data plane). NOT the v2 control-plane SDK |
| **8** | App-side cross-region search | Skip — single region only in app; federation belongs to AWS OpenSearch UI |
| **9** | Customer impersonation for demo recording | Add admin "view as customer X" button |
| **10** | OpenSearch domains — public or VPC | Public for demo; document VPC variant |
| **11** | Provision OpenSearch domains now or after workflow? | After workflow — build the workflow first, use it once |
| **12** | Numbering convention for new OpenSearch workflows | `004` (next after existing `001/002/003`) |
| **13** | Where does the search-client factory live? | New module `fx-search-client` (cleanest), shared by trade-service + indexer |
| **14** | VPC strategy per region | Mixed — workflow accepts optional VPC ID per region; creates new where not supplied |

## Companion document

See **[`README_OpenSearch_Core_Impl_DevPlan.md`](README_OpenSearch_Core_Impl_DevPlan.md)** for the full task breakdown — categorized by phase with status tracking, dependencies, and acceptance criteria. This README captures the **what**; the dev plan captures the **how, in what order, and what's done**.
