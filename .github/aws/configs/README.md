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

Use the manual GitHub workflow — no local CLI required.

### Step 1 — Set bootstrap admin credentials as repo secrets

You need ADMIN-level AWS credentials (used **only** to create the IAM resources below; they're never used by any other workflow). In **GitHub → Settings → Secrets and variables → Actions**, add:

| Secret | Value |
|---|---|
| `BOOTSTRAP_AWS_ACCESS_KEY_ID`     | Access key id of an admin AWS user |
| `BOOTSTRAP_AWS_SECRET_ACCESS_KEY` | Matching secret access key         |

### Step 2 — Run the bootstrap workflow

In the **Actions** tab, pick **`000-AWS-Bootstrap-IAM-Deployer`** and click **Run workflow**.

- Type `BOOTSTRAP` in the `confirm` input.
- Defaults are fine for the `policy_name` / `group_name` / `user_name` inputs.
- Click **Run workflow**.

The workflow will (idempotent on the first 5 steps):
1. Create the Customer Managed Policy from `01-AWS-ThisRepo-AWSUser-Policies.json` (or update its default version if it already exists).
2. Create the IAM Group.
3. Attach the policy to the Group.
4. Create the IAM User.
5. Add the User to the Group.
6. Generate a fresh access-key pair for the User.

The new access keys are printed in the workflow's final step output.

### Step 3 — Copy the new keys into the deployer repo secrets

Add two more repo secrets (these are the ones every other workflow uses):

| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID`     | from the workflow output |
| `AWS_SECRET_ACCESS_KEY` | from the workflow output |

### Step 4 — Scrub

After copying the keys:

1. **Delete the workflow run** (Actions → run → ⋯ → Delete run) so the access key isn't sitting in logs.
2. Optionally **delete the `BOOTSTRAP_AWS_*` secrets** — they're only needed if you ever re-run the bootstrap (e.g. to update the policy or add another user).

That's it. Every workflow under `.github/workflows/` can now run.

## Adding a future deployer user

Re-run the **same `000-AWS-Bootstrap-IAM-Deployer` workflow** with a different `user_name` input (e.g. `teammate-deployer`, or `staging-ci-deployer`). The policy + group already exist (idempotent skip); the workflow only creates the new user, adds them to the group, and emits their access keys.

## Tearing it all down

Use the **`000-AWS-Destroy-IAM-Deployer`** workflow. Type `DESTROY` in the confirm input. Same `BOOTSTRAP_AWS_*` secrets required. Deletes (in order): user's access keys → user → group → policy versions → policy.

## Updating the policy

If a new AWS service gets added to a workflow:

1. Edit `01-AWS-ThisRepo-AWSUser-Policies.json` — add the new `service:*` action to the relevant `Sid` block (or add a new `Sid`).
2. Re-run **`000-AWS-Bootstrap-IAM-Deployer`** — the workflow detects the existing policy and publishes a new default version (pruning old versions to stay under AWS's 5-version limit).

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
