# OpenSearch Core Implementation — Development Plan

> Companion to the **🛠️ AWS OpenSearch Implementation Plan** section of [`README.md`](README.md).
> README captures the **what + design decisions**; this document captures **how, in what order, and what's done**.

---

## 📑 Table of Contents

- [Status Legend](#status-legend)
- [Open Decisions That Block Tasks](#open-decisions-that-block-tasks)
- [Phase Index & Dependencies](#phase-index--dependencies)
- [Phase 1 · GitHub Actions for AWS Infrastructure](#phase-1--github-actions-for-aws-infrastructure)
- [Phase 2 · Provider-Agnostic Search Client (`fx-search-client`)](#phase-2--provider-agnostic-search-client-fx-search-client)
- [Phase 3 · Indexer · Region-Aware Routing](#phase-3--indexer--region-aware-routing)
- [Phase 4 · Trade-Service · Search API Refactor](#phase-4--trade-service--search-api-refactor)
- [Phase 5 · Customer Entity + Region Binding](#phase-5--customer-entity--region-binding)
- [Phase 6 · Admin Portal · Trades Search View](#phase-6--admin-portal--trades-search-view)
- [Phase 7 · Schema Extensions for P&L](#phase-7--schema-extensions-for-pl)
- [Phase 8 · AWS OpenSearch UI Setup](#phase-8--aws-opensearch-ui-setup)
- [Phase 9 · Saved Dashboards (NDJSON)](#phase-9--saved-dashboards-ndjson)
- [Phase 10 · Cross-Cutting Infrastructure](#phase-10--cross-cutting-infrastructure)
- [Phase 11 · Demo Recording Prep](#phase-11--demo-recording-prep)
- [Risks & Watch List](#risks--watch-list)
- [Definition of Done (per task)](#definition-of-done-per-task)

---

## Status Legend

| Symbol | Meaning |
|---|---|
| ☐ | NOT STARTED |
| ⏸ | BLOCKED (on a decision or another task) |
| ▶ | IN PROGRESS |
| ✅ | DONE |
| ⚠️ | NEEDS REVIEW (built but unverified) |
| ⛔ | DESCOPED (decided not to build) |

---

## Open Decisions That Block Tasks

Until each decision below is closed, the marked tasks remain ⏸ BLOCKED. Default recommendations are in [`README.md`](README.md#open-design-decisions); they're shown here as `[default]`.

| # | Decision | Default | Blocks |
|---|---|---|---|
| 1  | `createOrder` ≡ `placeTrade`?                       | yes              | T4.1 |
| 2  | `publishIndex` — internal step?                     | yes              | T3.1, T4.6 |
| 3  | `local` provider in scope                           | yes              | T2.4 |
| 4  | `cloudProvider` per-request — optional override?    | optional         | T4.6, T6.2 |
| 5  | Pagination cache scope                              | LRU sliding      | T6.5 |
| 6  | Prefetch direction                                  | forward only     | T6.6 |
| 7  | OpenSearch SDK choice                               | `opensearch-java` + `AwsSdk2Transport` | T2.3, T2.4 |
| 8  | App-side cross-region search                        | skip             | T4.5, T6.3 |
| 9  | Customer impersonation toggle for demo              | yes              | T11.2 |
| 10 | OpenSearch domains public or VPC                    | public for demo  | T1.6, T10.3 |
| 11 | Provision domains now or after workflow             | after workflow   | T8.1 |
| 12 | Numbering: new workflows = `004`?                   | yes (`004`)      | T1.3, T1.4 |
| 13 | Factory module location                             | new `fx-search-client` | T2.1 |
| 14 | VPC: mixed (per-region create-or-reuse)             | yes              | T1.1, T1.2 |

---

## Phase Index & Dependencies

```
Phase 1 (AWS infra workflows) ─────────┐
Phase 2 (search client lib)  ──┬───────┤
Phase 3 (indexer routing)      │   needs Phase 2
Phase 4 (search API)           │   needs Phase 2
Phase 5 (customer entity)  ────┤
Phase 6 (admin search UI)      │   needs Phase 4 + Phase 5
Phase 7 (P&L schema)       ────┤
Phase 8 (OpenSearch UI)        │   needs Phase 1 (provisioned domains)
Phase 9 (dashboards)           │   needs Phase 8 + actual data
Phase 10 (cross-cutting)       │   parallel
Phase 11 (demo prep)       ────┘   needs all above
```

Phases 2, 3, 4, 5, 7, 10 can largely proceed in parallel once decisions are closed.

---

## Phase 1 · GitHub Actions for AWS Infrastructure

**Goal:** Provision and tear down AWS OpenSearch domains in any subset of regions through manual-trigger workflows that are idempotent and follow the existing 001/002/003 naming convention.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T1.1 | ✅ | Refactor `001-AWS-Initial-Setup-VPC.yml` to accept multi-region selection + per-region optional `existing_vpc_id` | Comma-separated `regions` input + `vpc_overrides_json` map; matrix strategy fans out per region |
| T1.2 | ✅ | Update `001-AWS-Destroy-VPC.yml` to skip VPCs that were provided as `existing_vpc_id` (don't delete what we didn't create) | Per-region matrix; both override-skip and stack-not-present-skip handled |
| T1.3 | ✅ | Build CloudFormation template `aws/cloudformation/region-opensearch.yml` for one regional OpenSearch domain (instance type, EBS, fine-grained access, tags) | Public endpoint, IAM auth, encryption at rest + node-to-node, t3.small.search default; `fxs-{env}-{region}` naming |
| T1.4 | ✅ | Build `004-AWS-Setup-Region-OpenSearch.yml` workflow with multi-region selection input + idempotent skip-if-exists check | Detects existing CFN stack (idempotent update) + existing AWS-native domain (skip with warning) |
| T1.5 | ✅ | Build `004-AWS-Destroy-Region-OpenSearch.yml` with confirm-token + per-region selection | DESTROY confirm gate + per-region skip-if-not-present |
| T1.6 | ✅ | Update `995-AWS-All-Setup.yml` to chain `001-VPC` → `002-IAM-Roles` → `003-ECR` → `004-Region-OpenSearch` | Single `regions` input + `home_region` for global resources (IAM, ECR) |
| T1.7 | ✅ | Update `996-AWS-All-Destroy.yml` to chain in reverse order including the new `004` destroy | 004 → 003 → 002 → 001 reverse chain, forwards `confirm` token |
| T1.8 | ✅ | Document the new workflows in `.github/workflows/README.md` (already present; just append `004` row) | Rewritten — multi-region inputs, VPC-overrides, OpenSearch idempotency sections; access-keys auth (replaced OIDC bootstrap) |
| T1.9 | ☐ | End-to-end smoke: run `995-AWS-All-Setup` against a sandbox account, verify all stacks land cleanly | Cost ~$2/hr while running. **Manual — you run this against your AWS account once the IAM user + secrets are set up.** |
| T1.10 | ✅ | Switch all 10 workflows from OIDC to access-keys auth (per D-3 update mid-Phase) | `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` repo secrets; `id-token: write` permission removed |
| T1.11 | ✅ | Add `.github/configs/01-AWS-ThisRepo-AWSUser-Policies.json` — comprehensive admin-shaped IAM policy | 8 statement blocks covering compute, data + streaming, events, observability, edge + DNS, identity + secrets, build, billing read |
| T1.12 | ✅ | Add `.github/configs/README.md` — IAM user setup instructions for `fx-trade-opensearch-github-deployer` | 4-CLI-command quick start |

### Acceptance criteria

- Running `995-AWS-All-Setup` provisions VPC + IAM + ECR + OpenSearch domain in any selected subset of `us-east-1, eu-west-1, ap-south-1` without error.
- Running it again is a no-op (skip-if-exists with informational logs, no failed step).
- Running `996-AWS-All-Destroy` tears everything down in correct order with the `confirm: DESTROY` token.

---

## Phase 2 · Provider-Agnostic Search Client (`fx-search-client`)

**Goal:** A small Maven module both `fx-trade-service` and `fx-opensearch-indexer` depend on, exposing a factory that returns an OpenSearch client per region built with the right transport.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T2.1 | ⏸ | Create `middleware/fx-search-client/` Maven module + parent POM wire-up | Blocked on D-13 |
| T2.2 | ☐ | Define `OpenSearchBackend` config record (region, provider, endpoint) + `@ConfigurationProperties("fx.opensearch")` binding | — |
| T2.3 | ⏸ | Define `OpenSearchClientFactory` interface + Spring impl (singleton beans keyed by region) | Blocked on D-7 |
| T2.4 | ⏸ | Implement transport selection: `local` → Apache HTTP, `aws` → `AwsSdk2Transport` with default credentials provider | Blocked on D-3, D-7 |
| T2.5 | ☐ | Define `IndexRequest`, `SearchRequest`, `SearchResponse` DTOs (region, optional provider override, payload, pagination) | — |
| T2.6 | ☐ | Add `OpenSearchClientFactory.clientFor(region)` lookup with a clear error when the region is not configured | — |
| T2.7 | ☐ | Wire `fx-search-client` as dependency of `fx-trade-service` and `fx-opensearch-indexer`; remove the per-service `OpenSearchConfig` classes | — |

### Acceptance criteria

- Both services compile after the per-service `OpenSearchConfig` is removed.
- Factory returns a working client for `local-dev` against `http://localhost:9200` and would return a SigV4-signed client when `provider: aws` is configured.
- Unit tests don't need to be exhaustive; one happy-path per provider is enough.

---

## Phase 3 · Indexer · Region-Aware Routing

**Goal:** When the indexer consumes a trade event, it must write to the OpenSearch domain that matches the trade's `region` field — no matter where the indexer process runs.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T3.1 | ⏸ | Wire `OpenSearchClientFactory` into `OpenSearchService`; remove the hardcoded single-client field | Blocked on T2.7, D-2 |
| T3.2 | ☐ | In `OpenSearchService.indexTrade(trade)` route writes to `factory.clientFor(trade.region)` | — |
| T3.3 | ☐ | Index name is `fx-trades-{trade.region}` (already convention; verify) | — |
| T3.4 | ☐ | Error handling: unknown region → log + send to existing `trade-index-dlq` (DLQ recoverer already in `KafkaErrorConfig`) | — |
| T3.5 | ☐ | Smoke test: trade with `region=eu-west-1` written via local-dev profile lands in the `eu-west-1` AWS OpenSearch domain (visible from AWS console) | Needs Phase 1 + AWS profile |

### Acceptance criteria

- Generator-produced trade with `region=eu-west-1` appears in `fx-trades-eu-west-1` on the EU AWS OpenSearch domain within 2 seconds.
- Trade with `region=invalid-region` lands in `trade-index-dlq` with an informative log entry.

---

## Phase 4 · Trade-Service · Search API Refactor

**Goal:** Replace the existing single-region `/trades/search` with a paginated, factory-backed search that accepts a single region (or default) per request.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T4.1 | ⏸ | Rename of intent only (no code rename): confirm `POST /api/trades/place` is the canonical "create" endpoint | Blocked on D-1 |
| T4.2 | ☐ | Refactor `TradeSearchController` and `TradeSearchService` to consume `fx-search-client` factory | — |
| T4.3 | ☐ | Add request params: `region` (required if multi-region; default to caller's customer region — see Phase 5), `risk`, `pair`, `from`, `to`, `page`, `size` | — |
| T4.4 | ☐ | Server-side cap: `size <= 200` (clamp). For aggregations endpoint, no row cap | — |
| T4.5 | ⏸ | App-side cross-region: explicitly **NOT** implemented; document on the search API that "All regions" is via AWS OpenSearch UI only | Blocked on D-8 |
| T4.6 | ⏸ | Standardize `SearchRequestDTO` and `SearchResponseDTO` (hits, totalHits, page, size); reuse `fx-search-client` DTOs | Blocked on D-2, D-4 |
| T4.7 | ☐ | Keep legacy `GET /trades/search/risk?risk=...` returning raw response for back-compat for one release; deprecate in OpenAPI | — |
| T4.8 | ☐ | OpenAPI/Swagger annotation pass on the new endpoint shape | — |

### Acceptance criteria

- `GET /api/trades/search?region=us-east-1&risk=HIGH&page=0&size=20` returns up to 20 hits with `totalHits` populated.
- Requesting `size=500` clamps to 200 with a hint in the response (or HTTP header).

---

## Phase 5 · Customer Entity + Region Binding

**Goal:** Add a `Customer` first-class entity with a home region, manageable from the admin portal. Customer portal infers its working region from the logged-in customer instead of a free dropdown.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T5.1 | ☐ | Liquibase changeset `006-create-customer.yaml` (id, code, name, email, region, traderBook, active, audit timestamps) | — |
| T5.2 | ☐ | Liquibase seed `007-seed-customers.yaml` — 3-5 demo customers, one per region | — |
| T5.3 | ☐ | JPA entity `Customer`, repository, service interface + impl, controller (CRUD + paginated list) | Mirrors existing `Currency` etc. |
| T5.4 | ☐ | Admin portal: `Customers` list + form components (Material), navigation entry | — |
| T5.5 | ☐ | Customer portal: replace region dropdown with read-only display sourced from `currentCustomer.region` | — |
| T5.6 | ☐ | Trade-service: `place(req)` derives region from logged-in customer (when present); explicit `region` in DTO is treated as override (admin/testing) | — |

### Acceptance criteria

- Admin can create/edit/delete customers; each customer has exactly one region.
- Customer portal — when "logged in as" a customer — shows that region as fixed; placing a trade implicitly tags it with that region.
- Trade-service rejects a trade whose `region` doesn't match the logged-in customer's region (unless explicit admin override).

---

## Phase 6 · Admin Portal · Trades Search View

**Goal:** A search screen in the admin portal with region radio buttons, filters, and the 200-server / 20-client paginated grid pattern.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T6.1 | ☐ | New feature module `features/trades-search/` in admin-portal | — |
| T6.2 | ⏸ | Search form: region radios (us-east-1 / eu-west-1 / ap-south-1; **no "All"** per D-8), risk select, pair text, time range | Blocked on D-4, D-8 |
| T6.3 | ⏸ | Hint card explaining that **"All regions" search lives in the AWS OpenSearch UI** with a deep-link button | Blocked on D-8 |
| T6.4 | ☐ | Result table: columns trade ID · pair · amount · risk pill · region · timestamp | — |
| T6.5 | ⏸ | Client-side chunk cache: keep last N=3 chunks (200 each) keyed by `{filters_hash}+{chunkIndex}`; LRU eviction | Blocked on D-5 |
| T6.6 | ⏸ | Prefetch on client page 9-of-10: load next chunk in background; surface as "loading next page" only if user actually clicks past current chunk | Blocked on D-6 |
| T6.7 | ☐ | Total count + active filter chips at top of result area | — |
| T6.8 | ☐ | Empty state + clear filters button | — |
| T6.9 | ☐ | Cache invalidation: any filter change → drop all chunks for the previous filter set | — |

### Acceptance criteria

- Searching shows totalHits and the first 20 of 200 cached rows; navigating to page 10 of 10 triggers a single background fetch for the next 200.
- Re-searching with the same filters within the session does not re-hit the server (cache hit).
- Changing the risk filter clears the cache.

---

## Phase 7 · Schema Extensions for P&L

**Goal:** Carry the fields needed for end-of-day P&L attribution without breaking existing flows.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T7.1 | ☐ | Add `side` (BUY / SELL), `costBasis` (BigDecimal), `mtmRate` (BigDecimal) to `TradeEventDTO` in `fx-common` | — |
| T7.2 | ☐ | Update `devops/local/opensearch/mappings/fx-trades-mapping.json` with the three new fields | — |
| T7.3 | ☐ | Update `TradeDataGenerator` to set sensible defaults (`side=BUY`, `costBasis=rate`, `mtmRate=rate`) so existing flows still pass | — |
| T7.4 | ☐ | Customer portal `place-trade` form: optionally surface the `side` selector; `costBasis` and `mtmRate` remain server-derived | — |

### Acceptance criteria

- Existing trade flows continue to work; new fields are populated by default.
- Newly indexed trades have all three new fields visible in OpenSearch.

---

## Phase 8 · AWS OpenSearch UI Setup

**Goal:** Stand up the AWS-managed OpenSearch UI in `us-east-1` and connect it to the three regional domains. This is the **headliner** of the demo.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T8.1 | ⏸ | Provision OpenSearch UI application in `us-east-1` (AWS console — no IaC support yet) | Blocked on T1.9 |
| T8.2 | ☐ | Add the three regional domains as data sources to the UI | — |
| T8.3 | ☐ | Configure auth — IAM for the demo (D-3 default); document IDC upgrade in `docs/design/AWS-OpenSearch-Cross-Region-Use-Cases.md` | — |
| T8.4 | ☐ | On each regional domain: grant trust to the UI's home-region principal | — |
| T8.5 | ☐ | Smoke test — query `fx-trades-*` from the UI; should return hits from all three regions | — |

### Acceptance criteria

- A single index-pattern query against `fx-trades-*` in the UI returns rows from all three regional domains, with per-region timing visible.

---

## Phase 9 · Saved Dashboards (NDJSON)

**Goal:** Four saved-objects NDJSON dashboards committed to the repo and importable in one click via OpenSearch Dashboards Stack Management.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T9.1 | ☐ | Build "Global Trading Overview" dashboard (covers UC-3) — global trade rate, top pairs, risk pie, region heatmap | — |
| T9.2 | ☐ | Build "Risk Hunt" dashboard (covers UC-6) — filter-driven HIGH-risk table + drill-down | — |
| T9.3 | ☐ | Build "Volume Analytics" dashboard (covers UC-6) — hour-of-day, pair × region matrix | — |
| T9.4 | ☐ | Build "EOD P&L" dashboard (covers UC-7) — query-time aggregation by trader-book × region | — |
| T9.5 | ☐ | Export each as `.ndjson`, commit to `devops/local/opensearch/mappings/` (existing folder) | — |
| T9.6 | ☐ | README import instructions update | — |

---

## Phase 10 · Cross-Cutting Infrastructure

**Goal:** IAM, networking, index template + ISM that every other phase depends on.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T10.1 | ☐ | IAM role `fx-indexer-multiregion` (cross-region `es:ESHttp*`) — added to existing `iam-roles.yml` template | — |
| T10.2 | ☐ | IAM role `fx-trade-service-search` for read-only `es:ESHttp{Get,Head}` across regions | — |
| T10.3 | ⏸ | Decision: public OpenSearch domains for the demo (D-10) — confirm in CFN template | Blocked on D-10 |
| T10.4 | ☐ | One-time bootstrap: apply `fx-trades-*` index template + ISM (hot 7d → warm 30d → snapshot at 90d) on each domain | — |
| T10.5 | ☐ | CloudWatch alarms: indexing rate floor + storage ceiling per domain | — |
| T10.6 | ☐ | Document: how a developer's `~/.aws/credentials` profile is used by the local indexer to sign requests | — |

---

## Phase 11 · Demo Recording Prep

**Goal:** Everything needed to record the YouTube video on this feature.

### Tasks

| ID | Status | Task | Notes |
|---|---|---|---|
| T11.1 | ☐ | Make `TradeDataGenerator` continuous (rate + duration + region-list configurable) | — |
| T11.2 | ⏸ | Admin portal "view as customer X" impersonation toggle | Blocked on D-9 |
| T11.3 | ☐ | Pre-warm a few minutes of trade data into all three regional domains | — |
| T11.4 | ☐ | Walkthrough script — what to click in what order | — |
| T11.5 | ☐ | Record screencast (15-min target) | — |

---

## Risks & Watch List

| Risk | Mitigation |
|---|---|
| AWS OpenSearch costs while idle | Use smallest instance type (`t3.small.search` ≈ $50/mo); spin domains down between work sessions |
| SigV4 misconfiguration → 403s during local dev | Document the AWS profile setup; add a startup self-check in `fx-search-client` |
| Cross-region domain creation rate limits | Stagger workflow runs; check `aws opensearch list-domain-names` before create |
| OpenSearch UI is console-only (no IaC) | Document the manual steps thoroughly; future IaC support can replace |
| Demo-time network flakiness across regions | Pre-warm data; record demo against a stable network |
| Workflow numbering conflict with future additions (e.g. RDS at `005`) | Reserve a numbering convention block now |

---

## Definition of Done (per task)

A task is ✅ DONE when:

1. Code merged to `main` (no long-lived feature branches).
2. Application still builds (`mvn -f middleware/pom.xml package` and `npm run build:admin`/`npm run build:customer` if portals touched).
3. Existing services still start without regressions.
4. Acceptance criteria for the **phase** verified against running services.
5. CLAUDE.md and / or README touched if behavior changed.
6. This dev plan updated — task status ✅ + a one-line note on what shipped.
