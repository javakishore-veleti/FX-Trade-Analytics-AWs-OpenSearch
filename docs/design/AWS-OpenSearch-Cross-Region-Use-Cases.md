# Design — AWS OpenSearch Cross-Region UI Use Cases

> **Status:** Design draft. No code yet. Decisions called out inline as 🟡 _OPEN_.
> **Reference:** [aws.amazon.com/.../opensearch-ui-cross-region-data-access-domains](https://aws.amazon.com/about-aws/whats-new/2026/05/opensearch-ui-cross-region-data-access-domains/) (May 1, 2026)

---

## 1 · Goal

Showcase the new AWS OpenSearch UI cross-region data-access feature on this codebase, by:

1. Deploying (or simulating) trade ingestion in **three AWS Regions** — `us-east-1`, `eu-west-1`, `ap-south-1`
2. Writing each region's trades into a **region-local AWS OpenSearch domain**, index `fx-trades-{region}`
3. Standing up a **single OpenSearch UI application in `us-east-1`** that federates queries across all three regional domains
4. Building **four named use cases** end-to-end on top of that setup:

   | # | Use case |
   |---|---|
   | UC-3 | Multi-region desk operations (regional ingest + global head visibility) |
   | UC-1 | Trade entry & ingest (manual sales-trader + high-volume algo / generator) |
   | UC-6 | Cross-region search & analytics (HIGH-risk hunt, anomaly, top-pair aggregations) |
   | UC-7 | End-of-day P&L attribution (region × trader-book roll-up) |

**Constraint:** Build and operate from `us-east-1`. The OpenSearch UI app, the AWS account that hosts it, and the developer's IDE / browser all sit in `us-east-1`.

**Non-goals (this design):** real market-data feed, settlement, regulatory wire formats, FX rate sourcing for live MTM. We'll stub these where needed.

---

## 2 · Two Deployment Modes

We deliberately want **both** modes to work, because they trade off effort vs. realism.

### Mode A — Regional Deployment (production-shaped)

Full stack runs in each Region:

```
                  ┌────────────────────────────────────┐
                  │ AWS OpenSearch UI in us-east-1     │
                  │  (federates fx-trades-* across)    │
                  └─────┬──────────┬──────────┬────────┘
                        │          │          │
       ┌────────────────┴──┐  ┌────┴───────┐  ┌──┴──────────────┐
       │   us-east-1       │  │ eu-west-1  │  │ ap-south-1      │
       │ ┌──────────────┐  │  │ (same)     │  │ (same)          │
       │ │ portals      │  │  │            │  │                 │
       │ │ trade / risk │  │  │            │  │                 │
       │ │ /indexer/MD  │  │  │            │  │                 │
       │ │ Kafka, PG    │  │  │            │  │                 │
       │ │ OpenSearch   │  │  │            │  │                 │
       │ └──────────────┘  │  │            │  │                 │
       └───────────────────┘  └────────────┘  └─────────────────┘
```

- Each region operates fully autonomously.
- Customer portal's region-aware routing (`/api/config/regions` already built) sends each customer to their nearest region.
- Each indexer writes to its **local** OpenSearch domain only — clean residency story.
- Best mirrors what a real bank would deploy.

🟡 _OPEN: do we need 3 fully managed regional stacks for this video, or is Mode B enough for the demo?_ Mode A is the honest "this is how you'd really run it" but is 3× the AWS cost.

### Mode B — Localhost Dev with Globally Distributed Writes

Single laptop runs the entire compute stack; only OpenSearch is multi-region:

```
┌──────────────────────────────────────────────────────────────┐
│ Localhost                                                    │
│ ┌──────────┐   ┌────────────┐   ┌──────────┐                 │
│ │ portals  │──▶│ trade-svc  │──▶│ Kafka    │                 │
│ └──────────┘   └────────────┘   └────┬─────┘                 │
│                                       │                      │
│                                ┌──────▼──────┐               │
│                                │ risk-svc    │               │
│                                └──────┬──────┘               │
│                                       │                      │
│                                ┌──────▼──────┐               │
│                                │ indexer     │── routes by   │
│                                │ (multi-     │   trade.region│
│                                │  region)    │               │
│                                └──┬───┬───┬──┘               │
└───────────────────────────────────┼───┼───┼──────────────────┘
                                    │   │   │  (SigV4-signed HTTPS)
              ┌─────────────────────┘   │   └─────────────────┐
              ▼                         ▼                     ▼
   ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
   │ AWS OpenSearch   │    │ AWS OpenSearch   │    │ AWS OpenSearch   │
   │   us-east-1      │    │   eu-west-1      │    │   ap-south-1     │
   │ fx-trades-us-…   │    │ fx-trades-eu-…   │    │ fx-trades-ap-…   │
   └──────────────────┘    └──────────────────┘    └──────────────────┘
```

- One trade-service receives or generates trades; each trade carries a `region` field.
- `TradeDataGenerator` simulates global high-volume traffic by randomly tagging regions.
- The **indexer is the only service that goes "multi-region"** — it routes each write to the corresponding regional OpenSearch domain.
- OpenSearch UI in `us-east-1` federates queries across the same three domains — identical UX as Mode A.
- Massive dev-velocity win: you can iterate on dashboards / queries without spinning up regional Kubernetes clusters.

> **Verdict:** start with Mode B for development and the YouTube demo recording. Mode A is the migration target once the use cases are proven.

---

## 3 · Setting Up OpenSearch UI in us-east-1 (Home Region)

Done **once** before any use case can be built. Same setup powers all four use cases.

| Step | What | Decision |
|---|---|---|
| 1 | Create one AWS OpenSearch domain per region (us-east-1, eu-west-1, ap-south-1) | 🟡 _OPEN: instance type? `t3.small.search` for dev, `r6g.large.search` for prod-shape demo_ |
| 2 | Apply identical index template `fx-trades-*` to each domain (same mapping for cross-region query consistency) | mapping already lives at `devops/local/opensearch/mappings/fx-trades-mapping.json` — reuse |
| 3 | Provision OpenSearch UI application in `us-east-1` | Console-driven; no IaC support yet for the UI itself |
| 4 | Add the eu-west-1 and ap-south-1 domains as data sources to the UI | This is the new feature — uses the cross-region access path |
| 5 | Choose auth mode: IAM **or** IAM Identity Center | 🟡 _OPEN: IDC for SSO is the right end state, but IAM is faster for the demo_ |
| 6 | On each remote domain, grant trust to the UI's home-region principal | Per the AWS doc on cross-region access |

🟡 _OPEN: public-internet OpenSearch domains vs. VPC-attached?_ Public is simplest for the demo (CORS + IAM); VPC is what a real bank would use (PrivateLink to the UI region). Recommendation: **public for the demo, document the VPC variant**.

---

## 4 · UC-3 — Multi-Region Desk Operations

### Personas
- **Regional desk trader** (NYC, London, Mumbai) — watches their own trades only
- **Global desk head** (NYC) — sees all regions in one view

### Data flow

| Step | Where it happens | Notes |
|---|---|---|
| Trader places a trade | Customer portal → trade-service | Region-aware routing already chooses the right backend |
| Trade validated against allow-list | trade-service in trade's region | Master-data CRUD already exists |
| Published to Kafka | Local broker | Confluent KRaft already wired |
| Risk classified | risk-service | Existing `RiskCalculator` |
| Indexed to OpenSearch | Mode A: local domain · Mode B: routed by `region` field | **Indexer change required for Mode B** |

### Index design

- Mapping (already specified in `fx-trades-mapping.json`): tradeId/region/riskLevel as `keyword`, timestamp as `date`, fromAmount as `double`.
- **Each region's domain hosts only its own `fx-trades-{region}` index.** No replication.

### What the regional desk trader sees

- Local OpenSearch UI (or the existing customer portal) querying `fx-trades-us-east-1` only.
- Sub-100ms response time (data is local).

### What the global desk head sees

- Single dashboard in the OpenSearch UI app (us-east-1) querying `fx-trades-*`.
- All three regional domains queried in parallel; results merged.

### Code/config required

| Component | Mode A | Mode B |
|---|---|---|
| trade-service | No change | No change |
| risk-service | No change | No change |
| indexer | No change (writes to local OpenSearch) | **NEW: region-routing logic + multi-domain client** |
| OpenSearch UI | Add 3 domains as data sources | Same |
| Dashboards | Per-region "Desk View" dashboard + a "Global Desk Head" dashboard | Same |

### Acceptance criteria

- A trade placed in `eu-west-1` appears in `fx-trades-eu-west-1` within 2 seconds.
- A `terms:region` aggregation in the global UI shows all three regions populated.
- Each regional desk dashboard shows zero documents from other regions.

---

## 5 · UC-1 — Trade Entry & Ingest

### Two ingestion paths

#### Path 1.A — Manual sales-trader entry

- Sales trader uses the customer portal (already built).
- POST `/api/trades/place` carries `region`, `traderBook`, `fromCurrency`, `toCurrency`, `fromAmount`, `rate`.
- Master-data allow-list validation prevents bad pairs from reaching Kafka (already built).
- One trade per click — low volume, sub-second latency.

#### Path 1.B — High-volume algo / generator

- Existing `TradeDataGenerator` fires 50 trades on startup. **Need to extend to a continuous generator** for the demo.
- Production stand-in: an algo client posting at sustained rate (e.g. 100 trades/sec).
- Same downstream pipeline; the only difference is volume.

### Region-tagging strategy

🟡 _OPEN_: who decides the trade's `region` field?

| Option | When it works |
|---|---|
| **Customer portal sets it** based on user's region selection (current behavior) | Always; it's already implemented |
| **Trade-service overrides** based on the regional deployment it lives in | Mode A: enforces "trades placed at this endpoint must be tagged with this region" |
| **Generator randomizes** for dev simulation | Mode B: gives you cross-region data without distributed compute |

**Recommendation:** keep customer-portal as the source of truth for region; let the generator override to randomize for Mode B.

### Code/config required

| Component | Change |
|---|---|
| `TradeDataGenerator` | Make rate + duration configurable; add `--regions` flag to randomize across a list |
| `trade-service` | (Mode A only) Reject trades whose `region` doesn't match the deployment's region |
| `indexer` (Mode B only) | Region-router: pick AWS OpenSearch endpoint by trade's `region` field |
| `application.yml` | New block `aws.opensearch.region-endpoints` (list of region → URL) |

### Failure modes to design for

- Trade arrives with unknown region → DLQ, surfaced in monitoring
- Regional OpenSearch domain unreachable → indexer retries with exponential backoff (Spring Kafka `DefaultErrorHandler` already handles this; just needs to know which domain failed)
- Master-data allow-list service down → `fail-open: true` already configured for dev

### Acceptance criteria

- Manual portal trade and generator trade produce identical doc shape in OpenSearch.
- Generator can sustain ≥ 50 trades/sec in dev with no DLQ growth.
- A trade with `region=eu-west-1` is **never** written to `fx-trades-us-east-1` (validated by query in OpenSearch UI).

---

## 6 · UC-6 — Cross-Region Search & Analytics

The killer use case — this is what the OpenSearch UI feature was built for.

### Query catalogue

| # | Question | Index pattern | Query shape |
|---|---|---|---|
| Q1 | "All HIGH-risk USD trades, last 24h, anywhere" | `fx-trades-*` | `bool` { must: `term:fromCurrency=USD`, `term:riskLevel=HIGH`, `range:timestamp:gte=now-24h` } |
| Q2 | "Top 10 currency pairs by global volume today" | `fx-trades-*` | `terms:fromCurrency,toCurrency size:10` + `sum:fromAmount` |
| Q3 | "Risk distribution per trader book, all regions" | `fx-trades-*` | `terms:traderBook` → sub-agg `terms:riskLevel` |
| Q4 | "Trade volume per hour, last 24h, broken down by region" | `fx-trades-*` | `date_histogram:timestamp interval:1h` → sub-agg `terms:region` |
| Q5 | "Wash-trade hunt — same trader-book, opposite-direction trades within 60s, any region" | `fx-trades-*` | `terms:traderBook` → `date_histogram` 60s buckets → script filter on direction; **complex — see below** |

### Q5 — Wash-trade detection (the hard one)

Two design options:

| Option | Tradeoff |
|---|---|
| **Saved Dashboard query** with painless script | Lives in OpenSearch UI; no service code; query is heavy (full scan within window) |
| **Scheduled detector job** (Lambda or Spring Batch) | Faster query at read time; persists detections to a separate `fx-anomalies-*` index; needs new infra |

🟡 _OPEN: pick one for the demo._ Recommendation: scheduled job, because (a) the demo wants to **show detections**, not raw queries, and (b) it generalizes to any future anomaly type.

### Dashboard inventory (in OpenSearch UI)

| Dashboard | Persona | Visualizations |
|---|---|---|
| **Global Trading Overview** | Desk head | Trade rate chart · Top pairs · Risk pie · Region heatmap |
| **Risk Hunt** | Compliance | Filter-driven table of HIGH-risk trades · Region/pair/trader-book filters · Drill-down to single trade |
| **Volume Analytics** | Trading ops | Hour-of-day volume · Pair × region matrix · Trader-book leaderboard |
| **Anomaly Detector** | Compliance + risk | Wash-trade candidates · Cross-region coordinated activity flags |

Each dashboard ships as a **saved-objects NDJSON file** in `devops/local/opensearch/mappings/` (folder already exists for this).

### Code/config required

| Component | Change |
|---|---|
| OpenSearch UI app | Configured with all 3 data sources (see §3) |
| Dashboards | 4 NDJSON files committed to repo, importable in one click |
| (Optional) Anomaly job | New Lambda or Spring Batch service writing to `fx-anomalies-{region}` |

### Acceptance criteria

- Q1 returns hits from all three regions in a single result set
- Each dashboard loads in < 5s (cold) and < 1s (warm)
- A wash-trade detection visibly fires when the generator is configured to produce one

---

## 7 · UC-7 — End-of-Day P&L Attribution

### What we currently have vs. what we need

| Field | Current `TradeEventDTO` | Needed for P&L |
|---|---|---|
| tradeId, region, traderBook, timestamp | ✅ | ✅ |
| fromCurrency, toCurrency, fromAmount, toAmount, rate | ✅ | ✅ |
| **side (BUY/SELL)** | ❌ | ✅ — direction matters for P&L |
| **costBasis** (mid-rate at trade time) | ❌ | ✅ — needed for spread P&L |
| **mtmRate** (end-of-day mark-to-market rate) | ❌ | ✅ — for unrealized P&L |
| **counterparty** | ❌ | Optional, useful for drill-down |

### Schema extension

🟡 _OPEN: do we extend `TradeEventDTO` now or after the demo?_ The current generator + risk pipeline doesn't care about side, but P&L does. Recommendation: **extend now, default `side=BUY` so existing flows still work.**

### MTM rate sourcing

🟡 _OPEN: where do EOD MTM rates come from?_ Three options:

| Option | Pros | Cons |
|---|---|---|
| Stub: hardcoded rates per pair | Demo-friendly | Not realistic |
| Public API (e.g. ECB rates) | Realistic for majors | External dep, rate limits |
| New `fx-rates-{region}` index, populated nightly | Fits the architecture | Need to build the rate-loader job |

Recommendation: **stub rates for the demo, document the rate-loader as a follow-up**.

### Aggregation strategy

Two paths:

| Path | How it works | When to use |
|---|---|---|
| **Query-time aggregation** | OpenSearch UI dashboard runs `terms:traderBook` × `sum(toAmount * (mtmRate - rate))` at view time | Few trades, ad-hoc analysis |
| **Materialized index** (`fx-pnl-{region}-{date}`) | Scheduled job runs nightly, writes per-book × per-region P&L rollups | Many trades, repeatable EOD reports |

Recommendation: **query-time for the demo** (instant, no extra job). Materialized later if performance demands it.

### Cross-region story for P&L

- Each regional desk's P&L computed locally (data residency preserved)
- Global P&L = sum of regional P&Ls, computed by OpenSearch UI federation against `fx-trades-*`
- A single P&L dashboard in `us-east-1` shows: per-region totals, per-book totals, global net — all from the federated query

### Code/config required

| Component | Change |
|---|---|
| `TradeEventDTO` | Add `side`, `costBasis`, `mtmRate` fields |
| `TradeDataGenerator` | Set sensible defaults for new fields |
| OpenSearch mapping | Add `side` (keyword), `costBasis` (double), `mtmRate` (double) |
| Liquibase changeset (optional) | If we add MTM rates as a separate index, we need a separate loader job — **out of scope for first iteration** |
| Dashboard | New "EOD P&L" dashboard NDJSON |

### Acceptance criteria

- A trader-book that traded in all three regions shows a single rolled-up P&L row in the global dashboard.
- Drill-down from the rolled-up P&L row reveals trades from each contributing region.
- Total P&L = sum of per-region P&Ls (verified by manual spot check).

---

## 8 · Cross-Cutting Infrastructure

These show up in every use case; design them once.

### 8.1 IAM — three role categories

| Role | Used by | Permissions |
|---|---|---|
| `fx-indexer-task` | indexer service (Mode A) | `es:ESHttp*` on the local regional domain only |
| `fx-indexer-multiregion` (NEW for Mode B) | indexer service (Mode B) | `es:ESHttp*` on **all three** regional domains, cross-region |
| `fx-opensearch-ui-app` | The OpenSearch UI app in us-east-1 | `es:ESHttpGet/Post` on the local domain + cross-region trust to remote domains |

🟡 _OPEN: where does the indexer service get credentials in Mode B?_ Options: (a) static IAM user keys in env vars (dev only); (b) AWS profile from `~/.aws/credentials` (developer's machine); (c) IRSA / IAM Roles Anywhere (closer to prod). Recommendation: **(b) for the YouTube demo, (c) for the eventual production write-up.**

### 8.2 Networking

- **Mode A:** each region's compute talks to its local OpenSearch over private VPC endpoints. No cross-region data plane.
- **Mode B:** indexer on developer laptop talks to AWS OpenSearch domains over public internet (or VPN to a corp network if domains are VPC-attached). 🟡 _OPEN: are demo domains public or VPC?_ Public is dramatically easier; VPC is what real customers will run.
- **OpenSearch UI app:** must be able to reach all three regional OpenSearch domains. Cross-region access feature handles the routing; what we control is the domain endpoint type (public vs. VPC-attached).

### 8.3 SigV4 signing in the indexer (Mode B)

- AWS OpenSearch HTTPS endpoints require SigV4-signed requests when not using fine-grained access control with username/password.
- The current indexer uses `opensearch-rest-high-level-client` against unsigned `localhost:9200`.
- For Mode B we need to swap to **`AwsSdk2Transport`** (or the AWS SigV4 interceptor) so requests against `*.us-east-1.es.amazonaws.com` are signed with the indexer's IAM identity.

🟡 _OPEN_: do we abstract this so the same indexer code works for both local OpenSearch and AWS OpenSearch? Recommendation: yes — config flag `aws.opensearch.signing-mode: none|sigv4`.

### 8.4 Index templates + ISM

- One **index template** for `fx-trades-*` covering all regions; ensures identical mappings → consistent cross-region query behavior.
- **ISM (Index State Management)** policy: hot for 7 days → warm for 30 → delete or snapshot to S3 at 90.
- Apply via a one-time bootstrap script per regional domain (or the GitHub workflow once we add the `006-AWS-OpenSearch` workflow).

### 8.5 Observability

- Per-region: CloudWatch metrics for OpenSearch domain (cluster status, indexing rate, free storage, JVM heap)
- Cross-region: a single CloudWatch dashboard in us-east-1 with cross-region metric streams (or just visit each)
- Indexer: existing Spring Boot Actuator + Prometheus already exposes per-region indexing throughput
- DLQ: per-region Kafka DLQ visibility (Mode A) or single local DLQ visibility (Mode B)

---

## 9 · Decisions You Need to Make

Aggregating the 🟡 _OPEN_ items in one place so you can decide before any code is written.

| # | Decision | Options | Recommendation |
|---|---|---|---|
| D1 | Mode A or Mode B for the demo? | Mode A · Mode B · both | **Mode B for demo + dev iteration · Mode A for the production write-up** |
| D2 | OpenSearch domain instance type | `t3.small.search` · `r6g.large.search` · `m6g.large.search` | `t3.small.search` for the demo (~$50/mo per region; 3 × that) |
| D3 | OpenSearch UI auth | IAM · IAM Identity Center | **IAM for fast demo · IDC documented as the upgrade path** |
| D4 | OpenSearch domains: public or VPC | public · VPC-attached | **Public for demo · note VPC variant in docs** |
| D5 | Wash-trade detection (Q5) | Saved query · scheduled job | **Scheduled job → writes to `fx-anomalies-*`** |
| D6 | P&L computation | Query-time agg · materialized index | **Query-time for demo · materialize later if needed** |
| D7 | MTM rate sourcing | Hardcoded stub · public API · separate `fx-rates-*` index | **Stub for demo** |
| D8 | Indexer credentials in Mode B | IAM keys · AWS profile · IRSA | **AWS profile (`~/.aws/credentials`) for demo** |
| D9 | Schema additions for P&L | Now · later | **Now — add `side`, `costBasis`, `mtmRate` with safe defaults** |
| D10 | Continuous generator | Tweak existing one-shot · build new | **Tweak existing — make rate + duration + region-list configurable** |
| D11 | Add the indexer's region-routing config to a future GitHub workflow? | Yes (006-AWS-OpenSearch) · No, manual | **Yes — adds it to the existing 001/002/003 pattern** |

---

## 10 · Suggested Implementation Phases

Ordered for shortest time to a recordable demo. Each phase is independently mergeable.

### Phase 1 — AWS scaffolding (1-2 days)
- Add `006-AWS-Initial-Setup-OpenSearch.yml` GitHub workflow + paired destroy
- CloudFormation template provisioning 3 regional OpenSearch domains (one stack per region)
- Apply the `fx-trades-*` index template + ISM policy on each
- IAM role for indexer (`fx-indexer-multiregion`) with cross-region permissions

### Phase 2 — Indexer changes for Mode B (1 day)
- Add `aws.opensearch.region-endpoints` config
- Add SigV4 signing path (config-flagged)
- Add region-router logic — pick endpoint by `trade.region`
- Backwards-compatible: still works against local OpenSearch when flag is off

### Phase 3 — Generator + portal updates (½ day)
- Make `TradeDataGenerator` continuous + rate-configurable
- Add `--regions` flag to randomize region tagging
- Ensure customer portal already covers manual-entry path (it does)

### Phase 4 — Schema extension for P&L (½ day)
- Add `side`, `costBasis`, `mtmRate` to `TradeEventDTO`
- Update mapping JSON
- Default values in generator

### Phase 5 — OpenSearch UI setup in us-east-1 (½ day; manual via console)
- Create UI app
- Add 3 data sources
- Configure auth (IAM for now)
- Smoke-test cross-region query

### Phase 6 — Build the four dashboards (1-2 days)
- "Global Trading Overview" → covers UC-3 (head's view)
- "Risk Hunt" + "Volume Analytics" → covers UC-6
- "EOD P&L" → covers UC-7
- Export each as NDJSON, commit to `devops/local/opensearch/mappings/`

### Phase 7 — Wash-trade detection job (optional, 1 day)
- Spring Batch or Lambda — your pick
- Writes to `fx-anomalies-{region}`
- Surfaces in the "Risk Hunt" dashboard

### Phase 8 — Record the demo (½ day)
- Walk the 4 use cases on camera
- Show the cross-region UI doing a single query that fans out

**Total:** ~6-9 dev days end to end, demo-ready.

---

## 11 · Out of Scope (this design)

To keep scope honest:

- Real market-data feed (we stub MTM)
- Settlement, confirmations, FpML/FIX wire formats
- Counterparty credit limit checking
- Automated migration from Mode B to Mode A (we'll write that as a separate doc when we get there)
- Sanctions screening, AML graph detection, KYC — adjacent use cases that the README catalogues but are not part of the four demo use cases

---

## Appendix · Quick Reference

**Index pattern:** `fx-trades-{region}` per domain · `fx-trades-*` for federated query
**Home Region:** `us-east-1`
**Target Regions:** `us-east-1`, `eu-west-1`, `ap-south-1`
**OpenSearch UI auth (demo):** IAM
**OpenSearch UI auth (target):** IAM Identity Center
**Existing reusable assets in repo:**
- Mapping: `devops/local/opensearch/mappings/fx-trades-mapping.json`
- Region-config endpoint: `GET /api/config/regions` on trade-service
- Master-data allow-list: `fx-masterdata-service` on port 8083
- Region-aware customer portal routing
- GitHub Actions scaffold: `001/002/003/995/996` workflows already exist; add `006-AWS-OpenSearch` next
