# GitHub Actions — AWS infrastructure workflows

All workflows are **manually triggered** (`workflow_dispatch` — green "Run workflow" button in the Actions tab). Nothing here runs on push.

## Naming convention

| File pattern                          | Purpose                                    |
|---------------------------------------|--------------------------------------------|
| `00X-AWS-Initial-Setup-FOO.yml`       | Create / update one resource (CloudFormation stack) |
| `00X-AWS-Destroy-FOO.yml`             | Delete the matching stack                  |
| `995-AWS-All-Setup.yml`               | Orchestrator: runs every setup in order    |
| `996-AWS-All-Destroy.yml`             | Orchestrator: runs every destroy in reverse order |

Setup and destroy share the same stack name (`fx-${environment}-foo`), so each `Destroy` is just `aws cloudformation delete-stack` against the stack the matching `Initial-Setup` created.

## Current workflows

| #   | Setup                                       | Destroy                                  | What it provisions                |
|-----|---------------------------------------------|------------------------------------------|-----------------------------------|
| 001 | `001-AWS-Initial-Setup-VPC.yml`             | `001-AWS-Destroy-VPC.yml`                | VPC, 2 public + 2 private subnets, IGW, optional NAT |
| 002 | `002-AWS-Initial-Setup-IAM-Roles.yml`       | `002-AWS-Destroy-IAM-Roles.yml`          | ECS task-execution role + per-service task roles     |
| 003 | `003-AWS-Initial-Setup-ECR.yml`             | `003-AWS-Destroy-ECR.yml`                | One ECR repo per Spring Boot service                 |
| 995 | `995-AWS-All-Setup.yml`                     | —                                        | Calls 001 → 002 → 003 in order                       |
| 996 | —                                           | `996-AWS-All-Destroy.yml`                | Calls 003 → 002 → 001 (reverse order)                |

CloudFormation templates live under `.github/aws/cloudformation/`.

## Adding a new resource

1. Drop a CloudFormation template under `.github/aws/cloudformation/<name>.yml`.
2. Create `.github/workflows/00X-AWS-Initial-Setup-<NAME>.yml` (copy the closest existing setup as a template).
3. Create the matching `00X-AWS-Destroy-<NAME>.yml` (copy a destroy as a template).
4. Add a job to `995-AWS-All-Setup.yml` and `996-AWS-All-Destroy.yml` (in reverse order).

Each setup workflow MUST expose both `workflow_dispatch:` (so you can run it from the UI) and `workflow_call:` (so 995 can chain it).

## Safety rails

- Every **destroy** workflow requires the literal text `DESTROY` in the `confirm` input. Anything else aborts the run.
- `production` environment is in the dropdown but you are responsible for protecting it via [GitHub Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment) — add a required reviewer if you want a manual approval before any prod workflow runs.

## One-time AWS-side bootstrap (OIDC)

These workflows authenticate to AWS via **GitHub OIDC** — there are no static AWS access keys in the repo. You set this up **once per AWS account** before running any workflow.

### Step 1 — Create the GitHub OIDC provider in AWS

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

(The thumbprint is GitHub's intermediate CA fingerprint; verify against [GitHub's docs](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services) if it changes.)

### Step 2 — Create an IAM role the workflows can assume

Save this trust policy as `trust-policy.json` and replace `<ACCOUNT_ID>` and `<OWNER>/<REPO>`:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike":   { "token.actions.githubusercontent.com:sub": "repo:javakishore-veleti/FX-Trade-Analytics-AWs-OpenSearch:*" }
    }
  }]
}
```

```bash
aws iam create-role \
  --role-name fx-github-actions-deployer \
  --assume-role-policy-document file://trust-policy.json

# For dev convenience — for production, replace with a tighter policy.
aws iam attach-role-policy \
  --role-name fx-github-actions-deployer \
  --policy-arn arn:aws:iam::aws:policy/AdministratorAccess
```

### Step 3 — Add the role ARN as a repo secret

```bash
gh secret set AWS_ROLE_ARN --body "arn:aws:iam::<ACCOUNT_ID>:role/fx-github-actions-deployer"
```

(Or via Settings → Secrets and variables → Actions → New repository secret.)

That's it — every workflow in this folder picks up the secret automatically.

## How to run

### Single workflow

1. Go to **Actions** tab on GitHub.
2. Pick the workflow from the left sidebar (e.g. `001-AWS-Initial-Setup-VPC`).
3. Click **Run workflow** (top-right).
4. Fill in the inputs (region, environment, etc.). Defaults work for first-time dev.

### Everything at once

Pick `995-AWS-All-Setup` and click Run — it runs 001 → 002 → 003 in sequence.

To tear down: pick `996-AWS-All-Destroy`, type `DESTROY` in the confirm input, click Run.

## Stack naming

Every stack is `fx-${environment}-${resource}`:

| Workflow | Stack name (dev)      |
|----------|-----------------------|
| 001-VPC  | `fx-dev-vpc`          |
| 002-IAM  | `fx-dev-iam-roles`    |
| 003-ECR  | `fx-dev-ecr`          |

Outputs are exported as `fx-${environment}-${resource}-${output}` so later stacks can `Fn::ImportValue` them.

## Roadmap (next phases — not yet implemented)

| #   | Resource           | Notes                                    |
|-----|--------------------|------------------------------------------|
| 004 | RDS Postgres       | For master-data service                  |
| 005 | MSK                | Managed Kafka (replaces local Confluent) |
| 006 | OpenSearch Service | Managed search; multi-region future      |
| 007 | ECS cluster + ALB  | Fargate compute + load balancer          |
| 008 | Service deploys    | One per microservice                     |
| 009 | CloudFront + S3    | Hosting for admin & customer portals     |
| 010 | Route 53 + ACM     | Custom domain + TLS                      |

Each will follow the same setup/destroy pair pattern and be added to 995/996.
