# Demo Runbook — FX Trade Analytics on AWS OpenSearch (cross-region UI)

This is the click-by-click sequence for the live demo. Pre-flight first, then the actual on-stage flow. Time budget: ~5 min talking + ~3 min interactive.

---

## 🎯 What you're showing

> The new AWS OpenSearch cross-region UI feature, used as the federated read layer over a real-time FX trade analytics pipeline. Trades flow through Kafka → risk classifier → indexer → AWS OpenSearch domains in multiple regions, and the OpenSearch UI queries across all of them from one screen.

The application is the worked example. The OpenSearch UI feature is the headline.

---

## 🛫 Pre-flight (do these the day before, NOT during the demo)

Each step has a verification — don't proceed past a red ❌.

### 1 · GitHub repo secrets exist

- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `OPENSEARCH_MASTER_PASSWORD` all present
- ✅ Verify: Settings → Secrets and variables → Actions → 3 entries listed

### 2 · Local secrets file generated

```bash
export FX_DEPLOYER_AWS_ACCESS_KEY_ID=<from step 6 output of IAM bootstrap>
export FX_DEPLOYER_AWS_SECRET_ACCESS_KEY=<from step 6 output>
npm run localhost:app:secrets:generate
```
- ✅ Verify: `ls -la application-local-secrets.yml` → exists, mode `-rw-------`

### 3 · OpenSearch domains provisioned in 3 regions

GitHub Actions → `004-AWS-Setup-Region-OpenSearch` → Run workflow with:
- `regions = us-east-1,eu-west-2,ap-south-1`
- `select_all_regions` = unchecked
- Everything else default

Wait ~15 min for all 3 to reach `Active`.
- ✅ Verify in AWS Console (region selector → each region) → OpenSearch Service → Domains → all 3 are `Active` with FGAC enabled.

### 4 · Cross-region UI configured

In AWS OpenSearch Service Console (in your home region, e.g. `us-east-1`):
- Navigate to the new **OpenSearch UI** section (left sidebar — name may vary by AWS Console version)
- Create a new application; add the 3 domains as data sources
- Configure cross-region IAM access (Console wizard handles it)
- ✅ Verify: from the UI app, you can switch between regions and run a `_search` against `fx-trades-*` in each — should return 0 hits today (we haven't indexed anything yet)

### 5 · Local stack starts cleanly

```bash
mvn -f middleware/pom.xml -DskipTests clean install         # ~5s
cd portals && npm install && cd ..                           # one-time
npm run localhost:app:infra:all-up                           # Kafka + local OpenSearch
npm run localhost:app:services-ui:all-up                     # 4 services + 2 portals; browsers auto-open
```

- ✅ Verify: `npm run localhost:app:all:all-status` shows everything green

### 6 · Wire indexer + trade-service to AWS endpoints (via masterdata sync)

In the admin portal that just opened ([http://localhost:4200](http://localhost:4200)):
- Sidebar → **Administration → OpenSearch (AWS)**
- Click **Sync all regions** at the top
- ✅ Verify: 3 rows appear, all `ACTIVE`, with synthesized dashboards + AWS Console links

Edit `middleware/fx-trade-service/src/main/resources/application.yml` and `middleware/fx-opensearch-indexer/src/main/resources/application.yml`:
```yaml
fx:
  opensearch:
    source:
      type: masterdata    # was: yaml
```

Restart services so they pick up the new source mode:
```bash
npm run localhost:app:services:all-down
npm run localhost:app:services:all-up
```

### 7 · Install dashboard templates into each AWS region

In the admin portal **OpenSearch (AWS)** page → click the ✨ icon on each row.
- ✅ Verify: snackbar shows `Installed N/N templates` per region. Templates land: index pattern, recent trades search, volume-by-region viz, risk-distribution donut.

### 8 · Pre-populate at least 50 trades

In the customer portal ([http://localhost:4201](http://localhost:4201)) → click **Generate demo trades** (top of page) → enter `100` → wait for the progress button to finish.
- ✅ Verify: in any region's `/_dashboards`, log in as `fxadmin` + your master password → Discover → index pattern `fx-trades-*` → see ~33 trades per region.

### 9 · Tabs ready for the demo

Open these in dedicated browser tabs in this order:
1. Customer portal — http://localhost:4201
2. Admin portal — http://localhost:4200
3. AWS OpenSearch UI (cross-region) — your URL from step 4
4. (Optional) AWS Console → OpenSearch Service → Domains list — `us-east-1`

Done. Now you're ready to go live.

---

## 🎬 On-stage flow

### Beat 1 (45s) — The architecture

> "We have a real-time FX trade analytics pipeline. Trades enter via this customer portal, flow through Kafka, get classified for risk, and land in OpenSearch — but not one OpenSearch. We deploy a domain in every region we trade in. So the data lives close to where it's created."

Show: customer portal → admin portal **Administration → OpenSearch (AWS)** — point at the 3 ACTIVE rows in different regions.

### Beat 2 (60s) — Generate live data

> "Let me show you the pipeline by generating 100 trades distributed across all three regions."

Customer portal → **Generate demo trades** → enter `100` → watch the progress counter on the button.

While it's running:
> "These are flowing through Kafka right now, getting risk-classified, and the indexer is routing each one to the OpenSearch domain that matches the trade's `region` field."

### Beat 3 (45s) — Per-region view (the old way)

> "Traditionally to query across regions, you'd open each region's OpenSearch Dashboards separately. Let me show you what that looks like."

Admin portal → click the dashboards icon on `us-east-1` row → log in as `fxadmin` → Discover → see ~33 trades.

> "Now I'd have to switch tabs, log in again, see eu-west-2 separately. And again for ap-south-1. Three tabs, three logins, three result sets to mentally merge."

### Beat 4 (90s) — The new way: AWS OpenSearch cross-region UI

> "Here's the new feature."

Switch to the **AWS OpenSearch UI** tab (your cross-region app from pre-flight step 4).

> "One UI, one login. Behind the scenes it's federating queries to all three domains."

Run a search across `fx-trades-*` → show all ~100 results pooled.

> "Filter by `riskLevel: HIGH` — that filter applies across all regions simultaneously."

Open the visualization → show "FX Trades — volume by region" → bars for all 3 regions visible from one chart.

> "Same for the risk distribution donut. We're aggregating data physically located in three AWS regions, into one visualization, with no application-layer code doing federation. The UI just talks to AWS."

### Beat 5 (30s) — Why this matters

> "Before this feature, cross-region search meant either:
> - Replicate everything to one global cluster (expensive, latent, single failure domain), or
> - Build app-layer federation (complex, brittle, hard to operate).
>
> AWS gives you the third option: keep data regional, query globally. That's the demo."

---

## 🧹 Tear down (after the demo)

```bash
npm run localhost:app:all:all-down                # local stack
```

Then in GitHub Actions → `004-AWS-Destroy-Region-OpenSearch`:
- `regions = us-east-1,eu-west-2,ap-south-1`
- `confirm = DESTROY`
- Run

Cost reminder: each `t3.small.search` domain = ~$50/mo. 3 idle domains = ~$25/mo. Don't forget to destroy.

---

## 🔧 If something goes wrong on stage

| Symptom | Likely cause | 30-second fix |
|---|---|---|
| "Generate demo trades" button greyed out | Pairs not loaded yet, or `/api/config/regions` failed | Refresh the page |
| Trades succeed locally but no docs in AWS | `fx.opensearch.source.type` still `yaml` | Check `application.yml`, restart services |
| `/_dashboards` shows anonymous-not-authorized | Domain was created BEFORE FGAC was enabled | Have a backup region pre-recorded; skip live |
| Cross-region UI shows `0 hits` | Index pattern `fx-trades-*` not refreshed | In OpenSearch UI: Stack Management → Index Patterns → refresh |
| AWS Console looks different from your prep | Console UI revisions ship constantly | Have a screen recording from prep as fallback |

---

## ✅ One-line sanity check before going live

```bash
npm run localhost:app:all:all-status && curl -sf http://localhost:8083/api/admin/opensearch-deployments | jq 'length'
```

Should print all green plus `3` (or however many AWS regions you provisioned).
