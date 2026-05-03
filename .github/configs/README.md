# `.github/configs/` — Configuration assets for repo automation

Files here are **inputs to setup steps that humans run once**, not consumed by CI directly.

## `01-AWS-ThisRepo-AWSUser-Policies.json`

Inline IAM policy intended for the **dedicated IAM user** that the GitHub Actions workflows authenticate as. Covers everything any current or planned workflow in this repo touches across **all AWS regions**.

### Recommended IAM user name

```
fx-trade-opensearch-github-deployer
```

### One-time setup (per AWS account)

```bash
# 1. Create the user
aws iam create-user --user-name fx-trade-opensearch-github-deployer

# 2. Attach this repo's policy as an inline policy
aws iam put-user-policy \
  --user-name fx-trade-opensearch-github-deployer \
  --policy-name fx-trade-opensearch-github-deployer-inline \
  --policy-document file://.github/configs/01-AWS-ThisRepo-AWSUser-Policies.json

# 3. Create access keys (these are the values you put into GitHub repo secrets)
aws iam create-access-key --user-name fx-trade-opensearch-github-deployer

# 4. In the GitHub repo, add two secrets:
#    AWS_ACCESS_KEY_ID       = <AccessKey.AccessKeyId from step 3>
#    AWS_SECRET_ACCESS_KEY   = <AccessKey.SecretAccessKey from step 3>
```

After step 4, every workflow under `.github/workflows/` can run.

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

1. Add the new `service:*` action to the relevant `Sid` block (or add a new `Sid`).
2. Run `aws iam put-user-policy` again with the same `--policy-name` to update in place.
3. Commit the change so the repo + AWS account stay in sync.
