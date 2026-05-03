#!/usr/bin/env bash
#
# One-time IAM bootstrap for the FX Trade Analytics deployer identity.
# Creates (idempotently): Customer Managed Policy + IAM Group + IAM User +
# Group membership, then mints a fresh access-key pair and prints it for
# you to copy into the GitHub repo secrets.
#
# Why a script (not a workflow): admin creds stay on your laptop. The new
# deployer access keys are emitted to your local terminal — never to a
# GitHub Actions log.
#
# Usage:
#   export FX_TRADE_ANALYTICS_AWS_ACCESS_KEY=<admin access key id>
#   export FX_TRADE_ANALYTICS_AWS_SECRET=<admin secret access key>
#   npm run setup:aws:iam-all                 # via package.json (recommended)
#   # OR run directly:
#   bash .github/aws/scripts/aws-iam-setup.sh
#
# Optional overrides:
#   POLICY_NAME, GROUP_NAME, USER_NAME, AWS_REGION

set -euo pipefail

# ── Required env vars (project-namespaced; never reuses your shell-wide AWS_*) ──
: "${FX_TRADE_ANALYTICS_AWS_ACCESS_KEY:?Set FX_TRADE_ANALYTICS_AWS_ACCESS_KEY in your shell — admin AWS access key id used ONLY for this one-time bootstrap.}"
: "${FX_TRADE_ANALYTICS_AWS_SECRET:?Set FX_TRADE_ANALYTICS_AWS_SECRET in your shell — admin AWS secret access key matching the access key id.}"

# Map to the standard names the AWS CLI expects, but only inside this script.
export AWS_ACCESS_KEY_ID="$FX_TRADE_ANALYTICS_AWS_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$FX_TRADE_ANALYTICS_AWS_SECRET"
export AWS_REGION="${AWS_REGION:-us-east-1}"

POLICY_NAME="${POLICY_NAME:-fx-trade-opensearch-policy}"
GROUP_NAME="${GROUP_NAME:-fx-trade-opensearch-deployers}"
USER_NAME="${USER_NAME:-fx-trade-opensearch-github-deployer}"

# Resolve repo root from this script's location so the policy file path is robust.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POLICY_FILE="$SCRIPT_DIR/../configs/01-AWS-ThisRepo-AWSUser-Policies.json"

# ── Sanity checks ──
command -v aws >/dev/null 2>&1 || { echo "ERROR: aws CLI not found on PATH." >&2; exit 1; }
command -v jq  >/dev/null 2>&1 || { echo "ERROR: jq not found on PATH (brew install jq)." >&2; exit 1; }
[ -f "$POLICY_FILE" ] || { echo "ERROR: policy file not found at $POLICY_FILE" >&2; exit 1; }

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
POLICY_ARN="arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"

echo "═══════════════════════════════════════════════════════════════════"
echo " IAM bootstrap — account ${ACCOUNT_ID}"
echo "   policy : ${POLICY_NAME}"
echo "   group  : ${GROUP_NAME}"
echo "   user   : ${USER_NAME}"
echo "═══════════════════════════════════════════════════════════════════"
echo ""

# 1. Customer Managed Policy
if aws iam get-policy --policy-arn "$POLICY_ARN" >/dev/null 2>&1; then
  echo "→ Policy ${POLICY_NAME} already exists; publishing a new default version (and pruning old)."
  for v in $(aws iam list-policy-versions --policy-arn "$POLICY_ARN" --query 'Versions[?!IsDefaultVersion].VersionId' --output text); do
    aws iam delete-policy-version --policy-arn "$POLICY_ARN" --version-id "$v"
  done
  aws iam create-policy-version \
    --policy-arn "$POLICY_ARN" \
    --policy-document "file://$POLICY_FILE" \
    --set-as-default >/dev/null
else
  aws iam create-policy \
    --policy-name "$POLICY_NAME" \
    --policy-document "file://$POLICY_FILE" \
    --description "FX Trade Analytics — permissions for the GitHub Actions deployer (managed in this repo)" >/dev/null
  echo "→ Policy ${POLICY_NAME} created."
fi

# 2. IAM Group
if aws iam get-group --group-name "$GROUP_NAME" >/dev/null 2>&1; then
  echo "→ Group ${GROUP_NAME} already exists."
else
  aws iam create-group --group-name "$GROUP_NAME" >/dev/null
  echo "→ Group ${GROUP_NAME} created."
fi

# 3. Attach policy to group (idempotent)
aws iam attach-group-policy --group-name "$GROUP_NAME" --policy-arn "$POLICY_ARN"
echo "→ Policy attached to group."

# 4. IAM User
if aws iam get-user --user-name "$USER_NAME" >/dev/null 2>&1; then
  echo "→ User ${USER_NAME} already exists."
else
  aws iam create-user --user-name "$USER_NAME" >/dev/null
  echo "→ User ${USER_NAME} created."
fi

# 5. Group membership (idempotent)
aws iam add-user-to-group --user-name "$USER_NAME" --group-name "$GROUP_NAME"
echo "→ User added to group."

# 6. Access keys
EXISTING_KEYS="$(aws iam list-access-keys --user-name "$USER_NAME" --query 'AccessKeyMetadata | length(@)' --output text)"
if [ "$EXISTING_KEYS" -ge "2" ]; then
  echo ""
  echo "ERROR: User ${USER_NAME} already has 2 access keys (AWS limit per user)." >&2
  echo "       Delete one first:  aws iam delete-access-key --user-name ${USER_NAME} --access-key-id <id>" >&2
  echo "       Then re-run this script." >&2
  exit 1
fi
[ "$EXISTING_KEYS" -ge "1" ] && echo "→ Note: user already has 1 access key. Creating an additional one — rotate the old when ready."

KEYS_JSON="$(aws iam create-access-key --user-name "$USER_NAME")"
AKID="$(echo "$KEYS_JSON" | jq -r '.AccessKey.AccessKeyId')"
SAK="$(echo "$KEYS_JSON" | jq -r '.AccessKey.SecretAccessKey')"

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo " IAM bootstrap complete. Add these to your GitHub repo secrets NOW:"
echo " (Settings → Secrets and variables → Actions → New repository secret)"
echo ""
printf "   AWS_ACCESS_KEY_ID       = %s\n" "$AKID"
printf "   AWS_SECRET_ACCESS_KEY   = %s\n" "$SAK"
echo ""
echo " The secret access key is shown ONCE — capture it now. The"
echo " bootstrap admin env vars (FX_TRADE_ANALYTICS_AWS_*) can be"
echo " unset from your shell once the GitHub secrets are populated."
echo "═══════════════════════════════════════════════════════════════════"
