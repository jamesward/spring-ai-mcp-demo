#!/usr/bin/env bash
set -euo pipefail

CLIENT_ID=$(terraform -chdir="$(dirname "$0")/infra" output -raw cognito_m2m_client_id)
CLIENT_SECRET=$(aws cognito-idp describe-user-pool-client \
  --user-pool-id "$(terraform -chdir="$(dirname "$0")/infra" output -raw cognito_pool_id)" \
  --client-id "$CLIENT_ID" --region us-east-1 \
  --query 'UserPoolClient.ClientSecret' --output text)
DOMAIN=$(terraform -chdir="$(dirname "$0")/infra" output -raw cognito_domain)

TOKEN=$(curl -s -X POST "$DOMAIN/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "$CLIENT_ID:$CLIENT_SECRET" \
  -d "grant_type=client_credentials&scope=spring-ai-mcp-demo-api/invoke" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

echo "$TOKEN"
