#!/usr/bin/env bash
#
# Tears down everything aws-iam-setup.sh created. Same env vars; same
# project-namespaced naming; idempotent (skips anything that doesn't
# exist).
#
# Usage:
#   export FX_TRADE_ANALYTICS_AWS_ACCESS_KEY=<admin access key id>
#   export FX_TRADE_ANALYTICS_AWS_SECRET=<admin secret access key>
#   CONFIRM_DESTROY=DESTROY npm run localhost:app:aws:destroy:iam-all
#   # OR run directly:
#   CONFIRM_DESTROY=DESTROY bash .github/aws/scripts/aws-iam-destroy.sh

set -euo pipefail

: "${FX_TRADE_ANALYTICS_AWS_ACCESS_KEY:?Set FX_TRADE_ANALYTICS_AWS_ACCESS_KEY in your shell — admin AWS access key id.}"
: "${FX_TRADE_ANALYTICS_AWS_SECRET:?Set FX_TRADE_ANALYTICS_AWS_SECRET in your shell — admin AWS secret access key.}"

if [ "${CONFIRM_DESTROY:-}" != "DESTROY" ]; then
  echo "ERROR: This will delete the IAM user, group, policy, and access keys." >&2
  echo "       To confirm, set CONFIRM_DESTROY=DESTROY in your shell, then rerun." >&2
  echo "       e.g.  CONFIRM_DESTROY=DESTROY npm run localhost:app:aws:destroy:iam-all" >&2
  exit 1
fi

export AWS_ACCESS_KEY_ID="$FX_TRADE_ANALYTICS_AWS_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$FX_TRADE_ANALYTICS_AWS_SECRET"
export AWS_REGION="${AWS_REGION:-us-east-1}"

POLICY_NAME="${POLICY_NAME:-fx-trade-opensearch-policy}"
GROUP_NAME="${GROUP_NAME:-fx-trade-opensearch-deployers}"
USER_NAME="${USER_NAME:-fx-trade-opensearch-github-deployer}"

command -v aws >/dev/null 2>&1 || { echo "ERROR: aws CLI not found on PATH." >&2; exit 1; }

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
POLICY_ARN="arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"

echo "═══════════════════════════════════════════════════════════════════"
echo " IAM teardown — account ${ACCOUNT_ID}"
echo "═══════════════════════════════════════════════════════════════════"

# 1. Delete user's access keys + group membership + user
if aws iam get-user --user-name "$USER_NAME" >/dev/null 2>&1; then
  for KEY in $(aws iam list-access-keys --user-name "$USER_NAME" --query 'AccessKeyMetadata[*].AccessKeyId' --output text); do
    aws iam delete-access-key --user-name "$USER_NAME" --access-key-id "$KEY"
    echo "→ Deleted access key ${KEY}."
  done
  aws iam remove-user-from-group --user-name "$USER_NAME" --group-name "$GROUP_NAME" 2>/dev/null || true
  aws iam delete-user --user-name "$USER_NAME"
  echo "→ Deleted user ${USER_NAME}."
else
  echo "→ User ${USER_NAME} does not exist; skipping."
fi

# 2. Detach policy from group + delete group
if aws iam get-group --group-name "$GROUP_NAME" >/dev/null 2>&1; then
  aws iam detach-group-policy --group-name "$GROUP_NAME" --policy-arn "$POLICY_ARN" 2>/dev/null || true
  aws iam delete-group --group-name "$GROUP_NAME"
  echo "→ Deleted group ${GROUP_NAME}."
else
  echo "→ Group ${GROUP_NAME} does not exist; skipping."
fi

# 3. Delete policy (after pruning non-default versions)
if aws iam get-policy --policy-arn "$POLICY_ARN" >/dev/null 2>&1; then
  for v in $(aws iam list-policy-versions --policy-arn "$POLICY_ARN" --query 'Versions[?!IsDefaultVersion].VersionId' --output text); do
    aws iam delete-policy-version --policy-arn "$POLICY_ARN" --version-id "$v"
  done
  aws iam delete-policy --policy-arn "$POLICY_ARN"
  echo "→ Deleted policy ${POLICY_NAME}."
else
  echo "→ Policy ${POLICY_NAME} does not exist; skipping."
fi

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo " Teardown complete. Stale GitHub repo secrets to delete:"
echo "   AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY"
echo "═══════════════════════════════════════════════════════════════════"
