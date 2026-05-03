# GitHub Actions — AWS infrastructure workflows

All workflows are **manually triggered** (`workflow_dispatch` — green "Run workflow" button in the Actions tab). Nothing here runs on push.

## Naming convention

| File pattern                             | Purpose                                    |
|------------------------------------------|--------------------------------------------|
| `00X-AWS-Initial-Setup-FOO.yml`          | Create / update one resource (CloudFormation stack), single region |
| `00X-AWS-Setup-Region-FOO.yml`           | Same, but **multi-region matrix** — provisions one stack per selected region |
| `00X-AWS-Destroy-FOO.yml`                | Delete the matching stack(s)               |
| `995-AWS-All-Setup.yml`                  | Orchestrator: runs every setup in order    |
| `996-AWS-All-Destroy.yml`                | Orchestrator: runs every destroy in reverse order |

Setup and destroy share the same stack name (`fx-${environment}-foo` or `fx-${environment}-foo-${region}` for the multi-region ones), so each `Destroy` is just `aws cloudformation delete-stack` against the stack the matching setup created.

## Current workflows

| #   | Setup                                            | Destroy                                          | Scope        | What it provisions |
|-----|--------------------------------------------------|--------------------------------------------------|--------------|-------------------|
| 000 | `000-AWS-Bootstrap-IAM-Deployer.yml`             | `000-AWS-Destroy-IAM-Deployer.yml`               | Global (one-time) | IAM Customer Managed Policy + Group + User + access keys for the deployer identity. Uses **bootstrap admin secrets** (`BOOTSTRAP_AWS_*`) — see the IAM bootstrap section below |
| 001 | `001-AWS-Initial-Setup-VPC.yml`                  | `001-AWS-Destroy-VPC.yml`                        | Multi-region | One VPC per region (or reuses an existing VPC if its id is supplied via `vpc_overrides_json`) — 2 public + 2 private subnets, IGW, optional NAT |
| 002 | `002-AWS-Initial-Setup-IAM-Roles.yml`            | `002-AWS-Destroy-IAM-Roles.yml`                  | Global       | ECS task-execution role + per-service task roles |
| 003 | `003-AWS-Initial-Setup-ECR.yml`                  | `003-AWS-Destroy-ECR.yml`                        | Single-region | One ECR repo per Spring Boot service |
| 004 | `004-AWS-Setup-Region-OpenSearch.yml`            | `004-AWS-Destroy-Region-OpenSearch.yml`          | Multi-region | One AWS OpenSearch domain per region (`fxs-{env}-{region}`), idempotent skip-if-exists |
| 995 | `995-AWS-All-Setup.yml`                          | —                                                | All          | Calls 001 → 002 → 003 → 004 in order |
| 996 | —                                                | `996-AWS-All-Destroy.yml`                        | All          | Calls 004 → 003 → 002 → 001 (reverse order) |

CloudFormation templates live under `.github/aws/cloudformation/`.

### Multi-region inputs

The multi-region workflows (001-VPC, 004-OpenSearch, 995, 996) take a `regions` input that accepts:

- `all` → expands to the demo set: `us-east-1, us-west-2, sa-east-1, eu-west-2, eu-central-1, ap-south-1, ap-southeast-1, ap-northeast-1` (the 7-region "Balanced" set)
- A comma-separated list, e.g. `us-east-1,eu-west-2,ap-south-1` for a smaller demo

Each region runs its own job in a matrix — parallel, independent, with its own logs. One region failing does not abort the others (`fail-fast: false`).

### VPC overrides

`001-VPC` setup + destroy take a `vpc_overrides_json` input — a JSON map of `region → existing VPC id`. Regions present in the map have **no VPC stack created** (and on destroy are skipped — we never delete what we didn't create).

```json
{"us-east-1": "vpc-0abc...", "eu-west-2": "vpc-0def..."}
```

Regions not in the map get a freshly-provisioned VPC.

### OpenSearch idempotency

`004-AWS-Setup-Region-OpenSearch` checks each region before provisioning:

| Pre-state | Action |
|---|---|
| CFN stack `fx-{env}-opensearch-{region}` already exists | Run an idempotent CloudFormation update (`--no-fail-on-empty-changeset`); no-op if nothing to change |
| Domain `fxs-{env}-{region}` exists in AWS but **not** managed by our CFN stack | Skip with a warning (don't try to create a colliding domain) |
| Neither | Create the stack and the domain |

## Adding a new resource

1. Drop a CloudFormation template under `.github/aws/cloudformation/<name>.yml`.
2. Create `.github/workflows/00X-AWS-Initial-Setup-<NAME>.yml` (or `00X-AWS-Setup-Region-<NAME>.yml` if multi-region) — copy the closest existing setup as a template.
3. Create the matching `00X-AWS-Destroy-<NAME>.yml`.
4. Add a job to `995-AWS-All-Setup.yml` and a reverse-order job to `996-AWS-All-Destroy.yml`.

Each setup workflow MUST expose both `workflow_dispatch:` (so you can run it from the UI) and `workflow_call:` (so 995 can chain it).

## Safety rails

- Every **destroy** workflow requires the literal text `DESTROY` in the `confirm` input. Anything else aborts the run.
- `production` environment is in the dropdown but you are responsible for protecting it via [GitHub Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment) — add a required reviewer if you want a manual approval before any prod workflow runs.

## One-time AWS-side setup (IAM bootstrap)

The deployer identity is created **by the `000-AWS-Bootstrap-IAM-Deployer` workflow itself** — no local CLI required.

The IAM policy that defines what the deployer is allowed to do lives at [`.github/aws/configs/01-AWS-ThisRepo-AWSUser-Policies.json`](../aws/configs/01-AWS-ThisRepo-AWSUser-Policies.json). Full setup walkthrough in [`.github/aws/configs/README.md`](../aws/configs/README.md).

**4-step bootstrap (one-time per AWS account):**

1. **Add bootstrap admin secrets to the repo** (Settings → Secrets and variables → Actions):
   - `BOOTSTRAP_AWS_ACCESS_KEY_ID`
   - `BOOTSTRAP_AWS_SECRET_ACCESS_KEY`

   These are the access keys of an admin AWS user. They're used **only** for the bootstrap workflow; you can delete them afterward.

2. **Run `000-AWS-Bootstrap-IAM-Deployer`** from the Actions tab. Type `BOOTSTRAP` in the confirm input. Click Run.

   The workflow creates the Customer Managed Policy + Group + User + access keys (idempotent on the first 5 steps; access keys are always freshly minted). Output prints the new keys.

3. **Copy the new keys into the deployer secrets**:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`

4. **Scrub the workflow run** (Actions → run → ⋯ → Delete run) so the access key isn't sitting in logs. Optionally delete the `BOOTSTRAP_AWS_*` secrets too.

**Adding another deployer user:** re-run `000-AWS-Bootstrap-IAM-Deployer` with a different `user_name` input. Policy + group are reused; only a new user + keys are created.

**Tearing it all down:** `000-AWS-Destroy-IAM-Deployer` workflow. Type `DESTROY`. Same `BOOTSTRAP_AWS_*` secrets required.

After the secrets are set, every workflow in this folder picks them up automatically.

## How to run

### Single workflow

1. Go to **Actions** tab on GitHub.
2. Pick the workflow from the left sidebar (e.g. `004-AWS-Setup-Region-OpenSearch`).
3. Click **Run workflow** (top-right).
4. Fill in the inputs. For multi-region workflows, leave `regions` as `all` to use the 7-region demo set, or supply a comma-separated subset.

### Everything at once

Pick `995-AWS-All-Setup` and click Run — it runs 001 → 002 → 003 → 004 in sequence, chaining through `workflow_call`.

To tear down: pick `996-AWS-All-Destroy`, type `DESTROY` in the confirm input, click Run.

## Stack naming

| Workflow            | Stack name (dev environment, us-east-1)         |
|---------------------|-------------------------------------------------|
| 001-VPC             | `fx-dev-vpc` (per region)                        |
| 002-IAM             | `fx-dev-iam-roles` (global)                      |
| 003-ECR             | `fx-dev-ecr` (single-region)                     |
| 004-OpenSearch      | `fx-dev-opensearch-us-east-1` (per region)       |

OpenSearch domain name (inside the 004 stack): `fxs-dev-us-east-1` — the `fxs-` prefix keeps the name within OpenSearch's 28-character limit even for the longest region codes (`fxs-prod-ap-southeast-1` = 23 chars).

## Roadmap (next phases — not yet implemented)

| #   | Resource           | Notes                                    |
|-----|--------------------|------------------------------------------|
| 005 | RDS Postgres       | For master-data service                  |
| 006 | MSK                | Managed Kafka (replaces local Confluent) |
| 007 | ECS cluster + ALB  | Fargate compute + load balancer          |
| 008 | Service deploys    | One per microservice                     |
| 009 | CloudFront + S3    | Hosting for admin & customer portals     |
| 010 | Route 53 + ACM     | Custom domain + TLS                      |

Each will follow the same setup/destroy pair pattern and be added to 995/996.
