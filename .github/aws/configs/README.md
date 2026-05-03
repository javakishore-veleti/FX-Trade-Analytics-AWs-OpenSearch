# `.github/aws/configs/` — Configuration assets for AWS automation

Files here are **inputs to setup steps run from GitHub Actions** (or, optionally, from the AWS CLI directly).

## `01-AWS-ThisRepo-AWSUser-Policies.json`

Permission policy attached as a **Customer Managed Policy** in AWS IAM. Covers everything any current or planned workflow in this repo touches across **all AWS regions**.

### IAM topology this policy is attached to

```
Customer Managed Policy:  fx-trade-opensearch-policy
                            ↓ attached to
IAM Group:                fx-trade-opensearch-deployers
                            ↓ contains
IAM User:                 fx-trade-opensearch-github-deployer
                          (+ any future users you add to the group)
```

The **Group** is the natural unit of "this is the team / set of identities that can deploy fx-trade-opensearch infra". The **Customer Managed Policy** lives as a first-class IAM object — versionable, attachable to multiple users / groups / roles. Adding a future deployer is one IAM API call (add-user-to-group); they instantly get the full set of permissions.

## How to bootstrap the IAM resources

Run a **single npm command from your laptop**. Admin credentials never leave your machine; the new deployer access keys are printed only to your local terminal (never to a GitHub Actions log).

### Step 1 — Export your admin AWS credentials in your shell

These are project-namespaced env vars so they don't collide with your shell-wide `AWS_*` variables. Use the access keys of an AWS user that has `iam:*` permission (typically your admin / root user). They're only used for this one-time bootstrap.

```bash
export FX_TRADE_ANALYTICS_AWS_ACCESS_KEY=<your admin access key id>
export FX_TRADE_ANALYTICS_AWS_SECRET=<your admin secret access key>
# optional override (defaults to us-east-1; IAM is global so any region works)
export AWS_REGION=us-east-1
```

If either is missing the script aborts with a clear error — it never silently falls back to your default AWS profile.

### Step 2 — Run the npm script

From the repo root:

```bash
npm run localhost:app:aws:setup:iam-all
```

The script (idempotent on steps 1-5) will:
1. Create the Customer Managed Policy from `01-AWS-ThisRepo-AWSUser-Policies.json` — or publish a new default version if it already exists.
2. Create the IAM Group.
3. Attach the policy to the Group.
4. Create the IAM User.
5. Add the User to the Group.
6. Generate a fresh access-key pair for the User and print it to your terminal.

### Step 3 — Copy the printed keys into GitHub repo secrets

In **GitHub → Settings → Secrets and variables → Actions**, add:

| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID`     | from the script output |
| `AWS_SECRET_ACCESS_KEY` | from the script output |

These are the credentials every other workflow under `.github/workflows/` uses.

### Step 4 — Unset the admin env vars in your shell

```bash
unset FX_TRADE_ANALYTICS_AWS_ACCESS_KEY FX_TRADE_ANALYTICS_AWS_SECRET
```

(Or close the shell.) The admin creds are no longer needed unless you re-run the bootstrap to update the policy or add another deployer.

## Adding a future deployer user

Set the env vars again, then run with a different `USER_NAME`:

```bash
USER_NAME=teammate-deployer npm run localhost:app:aws:setup:iam-all
```

Policy + group already exist (idempotent skip); the script only creates the new user, adds them to the group, and emits their access keys.

## Tearing it all down

```bash
export FX_TRADE_ANALYTICS_AWS_ACCESS_KEY=<your admin access key id>
export FX_TRADE_ANALYTICS_AWS_SECRET=<your admin secret access key>
CONFIRM_DESTROY=DESTROY npm run localhost:app:aws:destroy:iam-all
```

Refuses to run without `CONFIRM_DESTROY=DESTROY`. Deletes (in order): user's access keys → user → group → policy versions → policy. Idempotent — skips anything that already doesn't exist.

## Updating the policy

If a new AWS service gets added to a workflow:

1. Edit `01-AWS-ThisRepo-AWSUser-Policies.json` — add the new `service:*` action to the relevant `Sid` block (or add a new `Sid`).
2. Re-run `npm run localhost:app:aws:setup:iam-all` — the script detects the existing policy and publishes a new default version (pruning old versions to stay under AWS's 5-version limit).

## Scope of the policy

Categorized by Statement Sid:

| Sid | What's covered |
|---|---|
| `InfraAndCompute`             | CloudFormation, EC2 (incl. VPC), ECS, EKS, ECR, ELB, autoscaling, Lambda |
| `DataAndStreaming`            | OpenSearch (es + serverless), RDS, Kinesis (incl. Firehose), MSK + Kafka Connect, S3, DynamoDB |
| `EventsAndMessaging`          | EventBridge (events + schemas + scheduler), SNS, SQS |
| `ObservabilityAndDashboards`  | CloudWatch (metrics + dashboards), Logs, X-Ray, Synthetics |
| `EdgeNetworkingAndDns`        | CloudFront, Route 53 (incl. Resolver), ACM (incl. PCA), API Gateway, Global Accelerator |
| `IdentitySecretsAndConfig`    | IAM, STS, KMS, Secrets Manager, SSM (Parameter Store + Session Manager), IAM Identity Center |
| `BuildOrchestrationAndArtifacts` | CodePipeline, CodeBuild, CodeDeploy, CodeArtifact, Step Functions, Batch |
| `BillingAndAccountVisibility` | Cost Explorer (read-only), Cost Reports, Budgets (read-only), Resource Tagging API |

This is **admin-shaped for a demo / single-team project**. For a real production environment, split per-service policies onto per-service IAM roles instead.

## Common terminology trap

In AWS IAM, you don't *assign roles to users*. Roles are a separate identity type that's **assumed** (via `sts:AssumeRole`) — typically by services (EC2, Lambda, ECS task) or for cross-account access. For "share permissions across many users" the right primitives are **Customer Managed Policy + Group**, which is what the bootstrap workflow uses.
