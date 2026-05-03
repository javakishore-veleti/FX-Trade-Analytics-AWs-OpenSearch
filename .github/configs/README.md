# `.github/configs/` — Configuration assets for repo automation

Files here are **inputs to setup steps that humans run once**, not consumed by CI directly.

## `01-AWS-ThisRepo-AWSUser-Policies.json`

Permission policy intended to be attached as a **Customer Managed Policy** in AWS IAM. The policy covers everything any current or planned workflow in this repo touches across **all AWS regions**.

### Recommended IAM topology

Three reusable objects, each with one job:

```
Customer Managed Policy:  fx-trade-opensearch-policy
                            ↓ attached to
IAM Group:                fx-trade-opensearch-deployers
                            ↓ contains
IAM User:                 fx-trade-opensearch-github-deployer
                          (+ any future users with the same perms)
```

**Why a Customer Managed Policy + Group instead of an inline policy on the user?**

- The policy lives as a first-class IAM object — versionable, viewable in the console, attachable to multiple users / groups / roles.
- Adding a future user (teammate, second CI pipeline, an emergency break-glass account) becomes a one-liner: `aws iam add-user-to-group --user-name <new-user> --group-name fx-trade-opensearch-deployers`. They instantly get the full set of permissions.
- The Group is the natural unit of "this is the team that can deploy fx-trade-opensearch infra".

### One-time setup (per AWS account)

```bash
# 1. Create the Customer Managed Policy from the JSON file
aws iam create-policy \
  --policy-name fx-trade-opensearch-policy \
  --policy-document file://.github/configs/01-AWS-ThisRepo-AWSUser-Policies.json
# → save the PolicyArn it returns (looks like arn:aws:iam::<account>:policy/fx-trade-opensearch-policy)

# 2. Create the IAM Group
aws iam create-group --group-name fx-trade-opensearch-deployers

# 3. Attach the policy to the Group
aws iam attach-group-policy \
  --group-name fx-trade-opensearch-deployers \
  --policy-arn <PolicyArn from step 1>

# 4. Create the User and add to the Group
aws iam create-user --user-name fx-trade-opensearch-github-deployer
aws iam add-user-to-group \
  --user-name fx-trade-opensearch-github-deployer \
  --group-name fx-trade-opensearch-deployers

# 5. Generate access keys (these are the values you put into GitHub repo secrets)
aws iam create-access-key --user-name fx-trade-opensearch-github-deployer

# 6. In the GitHub repo, add two secrets:
#    AWS_ACCESS_KEY_ID       = <AccessKey.AccessKeyId from step 5>
#    AWS_SECRET_ACCESS_KEY   = <AccessKey.SecretAccessKey from step 5>
```

After step 6, every workflow under `.github/workflows/` can run.

### Adding a future user with the same permissions

```bash
aws iam create-user --user-name <new-user-name>
aws iam add-user-to-group \
  --user-name <new-user-name> \
  --group-name fx-trade-opensearch-deployers
aws iam create-access-key --user-name <new-user-name>
```

The new user gets the full policy by group membership — no policy duplication.

### Scope of the policy

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

### Updating the policy

If a new AWS service gets added to a workflow:

1. Edit the JSON locally — add the new `service:*` action to the relevant `Sid` block (or add a new `Sid`).
2. Publish a new version of the Customer Managed Policy:
   ```bash
   aws iam create-policy-version \
     --policy-arn <PolicyArn> \
     --policy-document file://.github/configs/01-AWS-ThisRepo-AWSUser-Policies.json \
     --set-as-default
   ```
3. Commit the change so the repo + AWS account stay in sync.

### Common terminology trap

In AWS IAM, you don't *assign roles to users*. Roles are a separate identity type that's **assumed** (via `sts:AssumeRole`) — typically by services (EC2, Lambda, ECS task) or for cross-account access. For "share permissions across many users" the right primitives are **Customer Managed Policy + Group**, which is what the setup above uses.
