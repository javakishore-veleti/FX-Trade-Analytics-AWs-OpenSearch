# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Scope guardrails (read first)

Read the entire repo.

Primary focus remains the streaming pipeline:
1. Validating Kafka → Risk → Indexer → OpenSearch flow
2. Fixing OpenSearch indexing and mapping issues
3. Ensuring data is visible in dashboards

Approved scope expansion (owner-confirmed): a `fx-masterdata-service` module exists for currency / currency-pair / trade-book CRUD that the admin UI consumes. New services beyond that still require explicit owner approval.

Do changes incrementally and show diffs per file.

---

## What this project is

A real-time **FX trade analytics pipeline** — not a CRUD app. The point is the streaming + analytics flow:

```
Trade Service (REST + producer) ──► Kafka: trade-events
                                          │
                                          ▼
                        Risk Service (consumer + enrichment)
                                          │
                                          ▼
                              Kafka: trade-events-enriched
                                          │
                                          ▼
                       OpenSearch Indexer (consumer)
                                          │
                                          ▼
                    OpenSearch index: fx-trades-{region}
                                          │
                                          ▼
                         Search API + Dashboards
```

Preserve this event-driven decoupling. Do **not** collapse services into direct calls or bypass Kafka. The pipeline is the product.

---

## Repository layout

- **`middleware/`** — Maven multi-module reactor (parent: `middleware-parent`, packaging `pom`). Java 17, Spring Boot 3.4.6.
  - `fx-common/` — shared `TradeEventDTO` only
  - `fx-trade-service/` — REST entry point + Kafka producer + OpenSearch search API (port **8080**)
  - `fx-risk-service/` — Kafka consumer, risk classifier, enriched producer, DLQ (port **8081**)
  - `fx-opensearch-indexer/` — Kafka consumer → OpenSearch indexing (port **8082**). Routes writes by `trade.region` via the `OpenSearchClientFactory` (in `fx-search-client`).
  - `fx-search-client/` — provider-agnostic OpenSearch client factory used by `fx-trade-service` (search path) and `fx-opensearch-indexer` (write path). Returns a region-keyed singleton `OpenSearchClient` from `opensearch-java` 2.10; transport is `ApacheHttpClient5TransportBuilder` for `local`, `AwsSdk2Transport` (SigV4) for `aws`. **Backend resolution is pluggable** via `BackendsSource` (controlled by `fx.opensearch.source.type`):
    - `yaml` (default) — reads the static `fx.opensearch.backends` list from `application.yml` once at startup. Restart to pick up changes. Right for local dev.
    - `masterdata` — calls fx-masterdata-service `GET /api/admin/opensearch-deployments`, filters to `status=ACTIVE && provisionType=managed`, refreshes every `fx.opensearch.source.ttl-seconds` (default 60). Right for AWS, since the admin portal sync flow is now the source of truth. Failures fall back to the last cached snapshot — a transient masterdata outage doesn't kill search/index.
    
    Each `clientFor(region)` call re-resolves the current backend and rebuilds the cached client only if the endpoint changed (so a fresh sync becomes effective without restart). AWS credentials come from `AwsCredentialsProperties` (`fx.aws.access-key/secret-key` → `StaticCredentialsProvider`) when set, else `DefaultCredentialsProvider` chain. The `AwsCredentialsProvider` bean is `@ConditionalOnMissingBean`, so a service that defines its own (e.g. masterdata's `AwsClientsConfig`) wins and shares one provider across both code paths. **Same code path in local dev and ECS — only YAML changes.**
  - `fx-masterdata-service/` — JPA/REST CRUD for currencies, currency pairs, trade books (port **8083**) **plus** an Administration page that tracks AWS OpenSearch deployments (managed clusters + serverless collections). Layered: `api/` → `service/` (interface) + `service/impl/` → `repository/` (Spring Data JPA) → `entity/`. H2 in-memory by default; `--spring.profiles.active=postgres` switches to `localhost:5432/fxdb`. **Schema and seed are owned by Liquibase** (changelog at `src/main/resources/db/changelog/db.changelog-master.yaml`); Hibernate runs in `validate` mode and never alters DDL. To add a new migration: drop a new YAML file in `db/changelog/changes/` and add an `include` line in the master changelog. OpenAPI/Swagger at `/swagger-ui.html`.
    - **Migration topology profiles** (Postgres only — H2 always runs Liquibase since the in-memory DB lives inside the JVM):
      - `postgres` — datasource + Liquibase + web. Single-pod local dev.
      - `postgres,migrate` — runs Liquibase, then `MigrationRunner` exits the JVM (web disabled). One-shot runner: local `npm run localhost:app:postgres:migrate`, prod K8s pre-upgrade Job.
      - `postgres,no-migrate` — datasource + web, **Liquibase OFF**. Production service pods after the migration Job has run. Removes the 20-pod lock-contention class of bug entirely. Hibernate `ddl-auto: validate` still catches drift at boot.
      - Lock-wait shortened to 5 min (`change-log-lock-wait-time-minutes`) so a stale `DATABASECHANGELOGLOCK` row from an OOM-killed pod fails the boot fast instead of hanging forever — operator does `liquibase releaseLocks` and Kubernetes restarts.
    - **Dashboard provisioning** (`DashboardInstallService`): NDJSON saved-objects templates ship in `src/main/resources/dashboards/` and are POSTed to `{endpoint}/_dashboards/api/saved_objects/_import?overwrite=true` (multipart/form-data, `osd-xsrf: true` header). Endpoint: `POST /api/admin/opensearch-deployments/{id}/install-dashboards`. Works for local + AWS managed clusters provisioned with the current `region-opensearch.yml` template (FGAC + `InternalUserDatabase` + master user/password). For older domains created before FGAC was enabled, recreate them via the workflow or set `OPENSEARCH_MASTER_PASSWORD` repo secret and re-run `004-AWS-Setup-Region-OpenSearch` to upgrade. To add a new template: drop a new `.ndjson` file in the `dashboards/` directory — `PostConstruct` enumeration picks it up at startup, no code changes.
    - **OpenSearch deployments tracking** (table `opensearch_deployments`, migrations `006-create-opensearch-deployments.yaml` + `007-add-endpoint-to-opensearch-deployments.yaml`): `OpenSearchDeploymentSyncService` calls AWS `OpenSearchClient.listDomainNames` + `OpenSearchServerlessClient.listCollections` per region in `fx.aws.regions`, upserts each row by `(cloud_provider, provision_type, deployment_name, region)`, marks stale rows `INACTIVE`. Captures the canonical endpoint as a first-class column (`DomainStatus.endpoint()` for managed — promoted to https; `CollectionDetail.collectionEndpoint()` for serverless). Status mapping: managed `processing/created/deleted` → `ACTIVE/PROCESSING/INACTIVE`; serverless `ACTIVE/CREATING/DELETING/FAILED` → `ACTIVE/PROCESSING/PROCESSING/ERROR`. The full `describe-*` response is also stored as JSON in `config_json` for debug. Endpoints: `GET /api/admin/opensearch-deployments` (DB read; consumed by both the admin portal **and** by fx-search-client when `fx.opensearch.source.type=masterdata`), `POST .../sync?region=us-east-1`, `POST .../sync-all`. The DTO synthesizes click-through `dashboardsUrl` (`{endpoint}/_dashboards`) and `awsConsoleUrl` (region-specific deep link to the AWS Console). The dashboards link opens the FGAC login page (master user `fxadmin` + the password from `OPENSEARCH_MASTER_PASSWORD` repo secret) on any domain provisioned via the current `region-opensearch.yml` template. Domains created before FGAC was enabled will still hit `User: anonymous is not authorized` — recreate them via the workflow.
- **`devops/local/`** — Docker Compose stacks for Kafka, OpenSearch, Postgres, observability, plus orchestration scripts
- **`fx-admin-ui/`** — legacy minimal static React-via-CDN admin page (kept for reference; superseded by `portals/`)
- **`portals/`** — Angular 21 workspace with two apps:
  - `projects/admin-portal/` (port **4200**) — Master Data CRUD (currencies, currency-pairs, trade-books) + **Administration → OpenSearch (AWS)** page (lists tracked deployments, sync button per region + sync-all, click-through to OpenSearch Dashboards and the AWS Console). Proxies `/api/master/*` and `/api/admin/*` → `localhost:8083`.
  - `projects/customer-portal/` (port **4201**) — Place Trade form (driven by master-data pairs) + Recent Trades view. Proxies `/api/master/*` → `:8083`, `/api/trades` and `/trades` → `:8080`.
  - Standalone components, Angular Material, lazy-loaded routes. First-time setup: `npm run localhost:app:portals:install` from repo root.
- **`docs/`** — architecture notes, drawio, screenshots
- Root `pom.xml` declares only `middleware`. Base Java package is `com.jk.fx.trade_mgmt`.

---

## Build & run

### Maven build

```bash
mvn -f middleware/pom.xml clean install            # build all middleware modules
mvn -pl middleware/fx-trade-service -am package    # build one service + deps
```

There are currently **no unit tests** in any module. Don't claim test coverage you didn't write. If you add tests, run a single one with:

```bash
mvn -pl middleware/fx-trade-service -Dtest=ClassName#method test
```

### Infra + apps (npm orchestration)

**All scripts follow `localhost:app:<category>:<action>`.** Categories: `infra` (Docker), `services` (Spring services), `ui` (Angular portals), `services-ui` (both), `all` (everything), `postgres` (opt-in DB), `secrets`, `aws`, `pm2`.

```bash
# Infra (Kafka, OpenSearch, Kafka UI; Postgres is opt-in via the postgres tree)
npm run localhost:app:infra:all-up
npm run localhost:app:infra:all-down
npm run localhost:app:infra:all-status

# Microservices — individually or together
npm run localhost:app:services:trade            # one service (mvn spring-boot:run)
npm run localhost:app:services:risk
npm run localhost:app:services:indexer
npm run localhost:app:services:masterdata       # H2 default
npm run localhost:app:services:masterdata:postgres   # connects to localhost:5432, expects postgres:up first
npm run localhost:app:services:all-up           # all four via concurrently
npm run localhost:app:services:all-down         # kill 8080, 8081, 8082, 8083
npm run localhost:app:services:all-status

# UI portals — auto-opens both browser tabs once ng serve is ready
npm run localhost:app:ui:admin                  # http://localhost:4200
npm run localhost:app:ui:customer               # http://localhost:4201
npm run localhost:app:ui:all-up                 # both portals + auto-open browsers
npm run localhost:app:ui:all-down               # kill 4200, 4201
npm run localhost:app:ui:all-status

# Services + UI together (no infra — assumes infra:all-up already ran)
npm run localhost:app:services-ui:all-up
npm run localhost:app:services-ui:all-down
npm run localhost:app:services-ui:all-status

# Everything (infra + services + UI)
npm run localhost:app:all:all-up
npm run localhost:app:all:all-down              # devops/local/shutdown-all.sh
npm run localhost:app:all:all-status            # devops/local/status-all.sh
```

**Postgres mode** (separate, opt-in — H2 stays the default):

```bash
npm run localhost:app:postgres:up               # docker compose up -d
npm run localhost:app:postgres:down             # tear down + drop volume
npm run localhost:app:postgres:wait             # block until pg_isready
npm run localhost:app:postgres:migrate          # one-shot Liquibase runner (postgres,migrate profile; exits when done)
npm run localhost:app:postgres:run-all          # postgres:up → wait → migrate → start services with postgres,no-migrate
```

The `postgres:run-all` flow is the production-shape pattern: run migrations once via a separate process, then start service pods with Liquibase disabled. In K8s this becomes a Helm pre-upgrade Job + Deployment with `--spring.profiles.active=postgres,no-migrate`.

**Other:**

```bash
npm run localhost:app:secrets:generate          # writes ./application-local-secrets.yml from env vars
npm run localhost:app:secrets:help              # full usage doc
npm run localhost:app:aws:setup:iam-all         # one-time IAM bootstrap (deployer user + policy + group)
npm run localhost:app:aws:destroy:iam-all
npm run localhost:app:pm2:start / :status / :stop   # PM2 alternative to concurrently
```

Each UI script auto-runs `portals:ensure-install` first (`npm install --prefer-offline --no-audit --no-fund` inside `portals/`). It's a no-op when deps are in sync (~1s overhead) and pulls only the diff when `package.json` changed. For a clean install: `npm run localhost:app:portals:install`.

### Known broken script paths (don't trust blindly)

- `devops/local/docker-all-up.sh` references a root `docker-compose.yaml` and `postgres/docker-compose.yaml` (relative to repo root). Verify those exist before running, or expect early failures from `set -e`.

Both Angular apps use `proxy.conf.json` to forward `/api/master/*` to master-data (8083) and the customer portal additionally proxies `/api/trades` and `/trades` to trade-service (8080). No CORS config needed in dev.

---

## Kafka topic wiring (source of truth: code, not config)

| Producer | Topic | Consumer |
|---|---|---|
| `TradeProducer` (trade-service) | `trade-events` | `TradeRiskConsumer` (risk-service, group `risk-group`) |
| `TradeRiskConsumer` (after enrichment) | `trade-events-enriched` | `TradeIndexerConsumer` (indexer, group `indexer-group`) |
| `TradeRiskConsumer` (catch block) | `trade-events-dlq` | `DLQConsumer` (risk-service, group `dlq-group`) |
| Indexer `KafkaErrorConfig` recoverer | `trade-index-dlq` | `DLQConsumer` (indexer, group `indexer-dlq`) |

**Gotcha — config drift**: `fx-risk-service/application.yml` declares `app.kafka.output-topic: risk-events`, but `TradeRiskConsumer.java` hardcodes `trade-events-enriched`. The hardcoded topic wins. Either wire the config in or delete the unused property — don't assume `risk-events` exists anywhere.

**Gotcha — Kafka wire format is plain UTF-8 string, NOT `JsonSerializer`**: The producers manually JSON-encode payloads with Jackson (`mapper.writeValueAsString(dto)`) and ship them as Strings. All three services therefore configure `StringSerializer`/`StringDeserializer` for both keys and values. **Do not switch to Spring's `JsonSerializer` here** — it would JSON-encode the already-stringified JSON, producing `"\"{...}\""` on the wire and breaking every consumer (the symptom is 100% of messages going to DLQ with double/triple-escaped backslashes). If you ever migrate to typed serdes, do it across producer + consumer + KafkaTemplate generic type in lockstep.

**DLQ flow** (current): `TradeRiskConsumer` and `TradeIndexerConsumer` let exceptions propagate. Spring Kafka's `DefaultErrorHandler` (configured in each service's `KafkaErrorConfig`) retries **3 times with 2s backoff**, then the `DeadLetterPublishingRecoverer` routes the original record to `trade-events-dlq` (risk-service) or `trade-index-dlq` (indexer). `DLQConsumer` in each service just logs the message body for visibility. Note: deserialization failures will burn all 3 retries before reaching DLQ since they're never going to succeed — add a `setClassifications(Map.of(SerializationException.class, false), false)` on the `DefaultErrorHandler` later if that latency matters.

### Region routing (customer portal → regional trade-service)

The customer portal does **not** hardcode the trade-service URL. On startup it fetches `GET /api/config/regions` from trade-service, which returns a `region → URL` map sourced from `fx.regions.endpoints` in `application.yml`. The Region dropdown is built from that map's keys. When the user submits a trade, `TradeService.place(req, baseUrl)` posts to `${baseUrl}/api/trades/place` for the selected region.

```yaml
# fx-trade-service/application.yml
fx:
  regions:
    endpoints:
      us-east-1: http://localhost:8080    # local dev: every region → same instance
      us-west-2: http://localhost:8080
      eu-west-1: http://localhost:8080
      ap-south-1: http://localhost:8080
  cors:
    allowed-origins:
      - http://localhost:4200
      - http://localhost:4201
```

In AWS, override per regional deployment. Spring's relaxed binding maps env vars: `FX_REGIONS_ENDPOINTS_US_EAST_1=https://trades.us-east-1.example.com` becomes `fx.regions.endpoints.us-east-1`. Add a region by adding it to the map — no code or UI change needed.

CORS is needed because in dev the customer portal at `:4201` posts cross-origin to trade-service at `:8080`. Allowed origins are configurable via `fx.cors.allowed-origins`.

### Master-data validation in trade-service

`TradeProducer.send(...)` now consults a cached `CurrencyPairAllowList` (populated from `fx-masterdata-service` via `MasterDataClient`) before publishing to `trade-events`. Trades whose `(fromCurrency, toCurrency)` pair isn't in the active allow-list are dropped with a WARN log and `send` returns `false`. Tunables in `application.yml` under `masterdata.*`:

- `base-url` — defaults to `http://localhost:8083`
- `allow-list.fail-open` — defaults to `true` so dev isn't blocked when master-data is down. **Flip to `false` in prod.**
- `allow-list.refresh-interval-seconds` — lazy refresh cadence (default 300s)

---

## OpenSearch wiring

- **Backend resolution lives in `fx-search-client`** (the `OpenSearchClientFactory` bean). `fx-trade-service` (search path) and `fx-opensearch-indexer` (write path) inject this factory and call `clientFor(region)`; both share one cached singleton client per region. Older `OpenSearchConfig` (`RestHighLevelClient` 2.11.0, hardcoded `localhost:9200`) still exists in code paths that haven't been migrated — those are local-dev only and bypass the factory.
- Indexer writes to `fx-trades-{region}`, document id = `tradeId`, source built via `mapper.convertValue(trade, Map.class)` (so types rely on dynamic mapping unless an index template is applied first).
- Mapping JSON: `devops/local/opensearch/mappings/fx-trades-mapping.json`. Apply it as an index template **before** any documents land, otherwise dynamic mapping will misclassify `timestamp` (long, should be date) and `riskLevel/region/tradeId` (text+keyword multi-field, should be keyword).
- Dashboards NDJSON: `devops/local/opensearch/mappings/fx-{overview,risk,region,monitoring}.ndjson` — load via OpenSearch Dashboards → Stack Management → Saved Objects → Import. They depend on the index pattern `fx-trades-*` existing.
- Search API: `TradeSearchController` (in `api` package) exposes:
  - `GET /trades/search?risk=HIGH&region=us-east-1&size=50` — returns `List<Map<String,Object>>` JSON, single-region query against `fx-trades-{region}`. Cross-region "all regions" search is intentionally NOT supported here — that's the AWS OpenSearch UI's federation responsibility.
  - `GET /trades/search/risk?risk=HIGH` — legacy, returns a count summary string. Kept for backward compat; new callers should use the JSON endpoint.

**`seed-data` profile**: `TradeDataGenerator` fires 50 random trades on startup ONLY when `--spring.profiles.active=seed-data` is set. Used for pipeline smoke tests without the UI. The customer portal's **Generate demo trades** button is the everyday path for populating data.

---

## Local AWS credentials (shared file)

Three services need AWS credentials when their `fx.opensearch.backends` includes an `aws` provider entry, or when masterdata's deployment-sync is exercised: `fx-masterdata-service`, `fx-trade-service`, `fx-opensearch-indexer`.

Rather than copy keys into three places, all three services import **one shared file at the repo root** via:

```yaml
spring:
  config:
    import: 'optional:file:./application-local-secrets.yml'
```

Generate it with:

```bash
# Set env vars (project-namespaced wins; AWS_* is the fallback):
export FX_DEPLOYER_AWS_ACCESS_KEY_ID=<deployer key id>
export FX_DEPLOYER_AWS_SECRET_ACCESS_KEY=<deployer secret>

npm run localhost:app:secrets:generate           # writes ./application-local-secrets.yml at mode 600
npm run localhost:app:secrets:help               # full usage doc
```

The file is **gitignored** (see `.gitignore`); the committed sibling `application-local-secrets-template.yml` documents the schema. Spring services must be launched from the repo root so the relative path resolves — the `npm run localhost:app:services:*` scripts already do this.

The `optional:` prefix means a missing file is fine; the AWS SDK then falls back to `DefaultCredentialsProvider` (env vars → `~/.aws/credentials` profile → ECS task role → EC2 IMDS), which is the prod path.

---

## Service URLs

| Service | URL |
|---|---|
| Trade API | http://localhost:8080 (`POST /api/trades` legacy demo, `POST /api/trades/place` real, `GET /trades/search?risk&region&size`, `GET /trades/search/risk` legacy) |
| Admin Portal | http://localhost:4200 |
| Customer Portal | http://localhost:4201 |
| Risk Service | http://localhost:8081 |
| Indexer | http://localhost:8082 |
| Master Data | http://localhost:8083 (Swagger: `/swagger-ui.html`, H2 console: `/h2-console`, OpenSearch deployments admin: `GET /api/admin/opensearch-deployments`, `POST .../sync?region=...`, `POST .../sync-all`) |
| OpenSearch | http://localhost:9200 |
| OpenSearch Dashboards | http://localhost:5601 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Kafka (Confluent cp-kafka 7.7, KRaft mode) | host: `localhost:9092` / in-Docker: `kafka:29092` |
| Kafka UI | http://localhost:8085 (moved off 8080 to free it for trade-service) |

Each Spring service exposes Prometheus metrics via Actuator at `/actuator/prometheus`.

---

## Debugging order (when end-to-end flow breaks)

Earlier links break the later ones — check in this order:

1. Source Kafka topic populated? (`trade-events`, then `trade-events-enriched`)
2. Risk-service logs for `⚡ Risk calculated` / `❌ Sending to DLQ`
3. `trade-events-enriched` topic populated?
4. Indexer logs for `✅ Indexed trade: ...`
5. OpenSearch: `GET _cat/indices?v` and `GET fx-trades-*/_search`
6. Dashboards: index pattern `fx-trades-*` exists in OpenSearch Dashboards?

---

## Target state (the bar to hit)

**Functional**: trades flow end-to-end, risk added, documents in OpenSearch, queryable via API, dashboards show real data.

**Technical**: index pattern `fx-trades-{region}`; mapping enforces `tradeId/riskLevel/region` as `keyword`, `timestamp` as `date`, `fromAmount` as `double`; pipeline stable; no data loss; DLQ working.

**Observability**: basic metrics; debuggable via logs + Kafka + OpenSearch.

---

## Conventions

- Communicate between services via Kafka, not REST-to-REST.
- Don't introduce a database, ORM, or service-mesh layer for problems Kafka + OpenSearch already solve here.
- When you change a topic name, update producer, consumer, AND `application.yml` together (see drift gotcha).
- Don't add features outside the immediate request — this codebase is intentionally minimal scaffolding around the pipeline.
