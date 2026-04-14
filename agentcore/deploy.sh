#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INFRA_DIR="$SCRIPT_DIR/infra"

# 1. Terraform apply — only ECR + IAM + Cognito first (skip AgentCore runtime)
terraform -chdir="$INFRA_DIR" init -upgrade
terraform -chdir="$INFRA_DIR" apply -auto-approve -target=aws_ecr_repository.this \
  -target=aws_iam_role.agentcore -target=aws_iam_role_policy.ecr_pull \
  -target=aws_cognito_user_pool.this -target=aws_cognito_user_pool_domain.this \
  -target=aws_cognito_resource_server.this -target=aws_cognito_user_pool_client.this

# 2. Read outputs
ECR_REPO=$(terraform -chdir="$INFRA_DIR" output -raw ecr_repo_url)
REGION=$(echo "$ECR_REPO" | cut -d. -f4)
ACCOUNT=$(echo "$ECR_REPO" | cut -d. -f1)

# 3. ECR login
aws ecr get-login-password --region "$REGION" | \
  finch login --username AWS --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"

# 4. Build and push ARM64 image
finch build --platform linux/arm64 -t "$ECR_REPO:latest" -f "$SCRIPT_DIR/Dockerfile" "$SCRIPT_DIR/.."
finch push "$ECR_REPO:latest"

# 5. Full apply — now AgentCore runtime can find the image
terraform -chdir="$INFRA_DIR" apply -auto-approve

echo ""
echo "=== Deployed ==="
terraform -chdir="$INFRA_DIR" output
