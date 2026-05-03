# fx-masterdata Helm chart

Deploys `fx-masterdata-service` to Kubernetes (EKS) with the production-shape
Liquibase migration topology built in: a **pre-upgrade `Job` runs the
changelog once**, then **service Pods boot with Liquibase OFF** (the
`postgres,no-migrate` Spring profile). This eliminates the
`DATABASECHANGELOGLOCK` contention class of bug across N pods, and makes
deploy failures deterministic — if the migration fails, the Job stays
`Failed`, `helm upgrade` reports failure, and **service Pods are never
updated**, so the old version keeps serving traffic on its compatible
schema.

## Layout

```
deploy/helm/fx-masterdata/
├── Chart.yaml
├── README.md                       (this file)
├── values.yaml                     defaults
├── values-prod.example.yaml        copy → values-prod.yaml, tune, then -f
└── templates/
    ├── _helpers.tpl                naming + labels
    ├── deployment.yaml             N service pods (no-migrate profile)
    ├── migration-job.yaml          pre-install/pre-upgrade hook (migrate profile)
    ├── service.yaml                ClusterIP on 8083
    └── serviceaccount.yaml         IRSA-ready
```

## First install

```bash
# Build + push the container image to ECR (or wherever)
mvn -f middleware/pom.xml -DskipTests -pl middleware/fx-masterdata-service \
    -am package
# … docker build … docker push …

# Create the DB password Secret out of band (don't put it in values.yaml)
kubectl create namespace fx
kubectl create secret generic fx-masterdata-db \
  --from-literal=password='****' \
  --namespace fx

# Render once to inspect:
helm template fx-masterdata ./deploy/helm/fx-masterdata \
  -f deploy/helm/fx-masterdata/values-prod.example.yaml \
  --namespace fx | less

# Install
helm upgrade --install fx-masterdata ./deploy/helm/fx-masterdata \
  -f values-prod.yaml \
  --set image.tag=$(git rev-parse --short HEAD) \
  --namespace fx --create-namespace
```

## What happens during `helm upgrade`

1. Helm renders templates, sees the `pre-upgrade` hook, **runs the
   migration `Job` first**.
2. Job container starts with `--spring.profiles.active=postgres,migrate`.
   Spring Boot autoconfig runs Liquibase against
   `jdbc:postgresql://{{ .Values.datasource.host }}:.../{{ database }}`.
3. `MigrationRunner` (in masterdata's source) calls
   `SpringApplication.exit(ctx, () -> 0)` then `System.exit(0)` —
   container terminates cleanly, Job marks Succeeded.
4. Helm sees the Job succeeded → proceeds with the rolling Deployment update.
5. New service Pods boot with `--spring.profiles.active=postgres,no-migrate`.
   Liquibase **never runs** in any Pod. Hibernate's `ddl-auto: validate`
   catches schema drift at boot.

## What happens if migrations fail

- Job retries up to `migrationJob.backoffLimit` (default `1`).
- If still failing → Job stays `Failed` → `helm upgrade` exits non-zero
  → **the Deployment never rolls** → old Pods continue serving traffic
  on the old (presumably working) schema.
- Operator inspects Job logs, fixes the migration, re-runs `helm upgrade`.

## Authentication to AWS (for the OpenSearch deployments sync feature)

Two options:

**IRSA (recommended)** — leave `aws.credentialsSecretName` empty in values.
Set `serviceAccount.annotations.eks\.amazonaws\.com/role-arn` to the IAM
role ARN with the `es:*` / `aoss:*` permissions. The masterdata pod's
`DefaultCredentialsProvider` picks up the IRSA-projected token automatically.

**Static keys (fallback)** — create a Secret with `access-key` / `secret-key`,
set `aws.credentialsSecretName: my-secret`. The chart will inject
`FX_AWS_ACCESS_KEY` / `FX_AWS_SECRET_KEY` env vars; Spring binds them to
`fx.aws.access-key` / `fx.aws.secret-key`.

## Schema changes (the safe-for-blue/green rule)

Each Liquibase changeset must be **backwards-compatible with the previous
deployed app version**, because between the migration Job finishing and
the new Pods reaching 100% rollout, the old Pods are still serving
traffic against the new schema.

Concretely: never combine an additive change with a destructive one in
the same release. Use **expand → backfill → contract** across releases:

| Release | Migration | Old code still works? |
|---|---|---|
| N+1 | ADD COLUMN new_col (nullable)        | ✓ ignores it |
| N+2 | backfill new_col, dual-write          | ✓ reads either |
| N+3 | switch reads to new_col               | ✓ both columns valid |
| N+4 | DROP COLUMN old_col                   | ✓ no readers left |

Background documented in CLAUDE.md (Postgres profiles section).
