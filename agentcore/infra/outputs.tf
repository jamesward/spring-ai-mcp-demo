output "ecr_repo_url" {
  value = local.ecr_repo_url
}

output "agentcore_runtime_arn" {
  value = aws_bedrockagentcore_agent_runtime.this.agent_runtime_arn
}

output "cognito_pool_id" {
  value = aws_cognito_user_pool.this.id
}

output "cognito_client_id" {
  value = aws_cognito_user_pool_client.this.id
}

output "cognito_m2m_client_id" {
  value = aws_cognito_user_pool_client.m2m.id
}

output "cognito_claude_client_id" {
  value = aws_cognito_user_pool_client.claude.id
}

output "cognito_domain" {
  value = "https://${aws_cognito_user_pool_domain.this.domain}.auth.${var.region}.amazoncognito.com"
}

output "authorization_endpoint" {
  value = "https://${aws_cognito_user_pool_domain.this.domain}.auth.${var.region}.amazoncognito.com/oauth2/authorize"
}
