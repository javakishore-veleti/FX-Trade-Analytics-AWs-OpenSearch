# Helm charts for the FX Trade Analytics platform

```
deploy/helm/
├── README.md                 (this file)
├── fx-masterdata/            CRUD + AWS deploy admin + pre-upgrade Liquibase Job
├── fx-trade-service/         REST entry + Kafka producer + multi-region search
├── fx-risk-service/          Kafka consumer → risk classifier → enriched producer
├── fx-opensearch-indexer/    Kafka consumer → SigV4-signed OpenSearch writes
└── fx-platform/              Umbrella chart — depends on all four above
```

## Quick start (single command for the whole platform)

```bash
helm dependency update ./deploy/helm/fx-platform

helm upgrade --install fx ./deploy/helm/fx-platform \
  -f deploy/helm/fx-platform/values-prod.example.yaml \
  --namespace fx --create-namespace
```

`-f` is required — without it the image repositories are blank and pods will
fail to schedule. Copy `values-prod.example.yaml` to `values-prod.yaml`, edit
to point at your ECR account + Aurora endpoint + Kafka brokers, then `-f` it.

## Deploying individual services

If you don't want the umbrella, each chart works standalone:

```bash
helm upgrade --install fx-masterdata ./deploy/helm/fx-masterdata \
  -f your-overrides.yaml --namespace fx --create-namespace
```

The masterdata chart's README documents the pre-upgrade Liquibase Job pattern
in detail — the same shape applies to the umbrella when it deploys masterdata
as a dependency.

## Authentication to AWS — IRSA strongly recommended

For prod, set `serviceAccount.annotations.eks.amazonaws.com/role-arn` on each
chart's values. The pod then picks up an IAM role via the projected service
account token; no static keys to rotate.

For dev, you can fall back to static keys via `aws.credentialsSecretName` —
create a Secret with `access-key` and `secret-key` keys, set the values name,
and the chart injects `FX_AWS_ACCESS_KEY` / `FX_AWS_SECRET_KEY` env vars that
Spring binds to `fx.aws.access-key` / `fx.aws.secret-key`.

## Schema migrations (masterdata only)

Only `fx-masterdata-service` owns DB schema. Its chart includes a Helm
pre-upgrade hook Job that runs Liquibase before service pods roll. Other
services don't have a DB.

## What's NOT here

- Kafka itself — bring your own MSK or self-managed cluster
- OpenSearch domains — provisioned via the GitHub workflow `004-AWS-Setup-Region-OpenSearch`
- VPC, IAM, ECR — provisioned via workflows 001/002/003
- Ingress / load balancer config — chart leaves Service as ClusterIP; add an
  Ingress / Gateway object externally

These belong to the cluster bootstrap, not the application chart.
