terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.70"
    }
  }
}

provider "aws" {
  region = var.region
}

data "aws_caller_identity" "current" {}

locals {
  account_id          = data.aws_caller_identity.current.account_id
  ecr_repo_url        = "${local.account_id}.dkr.ecr.${var.region}.amazonaws.com/${aws_ecr_repository.this.name}"
  cognito_discovery   = "https://cognito-idp.${var.region}.amazonaws.com/${aws_cognito_user_pool.this.id}/.well-known/openid-configuration"
  resource_server_id  = "${var.project_name}-api"
}

# --- ECR ---

resource "aws_ecr_repository" "this" {
  name         = var.project_name
  force_delete = true
}

# --- IAM ---

resource "aws_iam_role" "agentcore" {
  name = "${var.project_name}-agentcore"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "bedrock-agentcore.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "ecr_pull" {
  role = aws_iam_role.agentcore.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"]
        Resource = aws_ecr_repository.this.arn
      }
    ]
  })
}

# --- Cognito ---

resource "aws_cognito_user_pool" "this" {
  name = var.project_name
  password_policy {
    minimum_length    = 8
    require_lowercase = false
    require_numbers   = false
    require_symbols   = false
    require_uppercase = false
  }
}

resource "aws_cognito_user_pool_domain" "this" {
  domain       = "${var.project_name}-${local.account_id}"
  user_pool_id = aws_cognito_user_pool.this.id
}

resource "aws_cognito_resource_server" "this" {
  identifier   = local.resource_server_id
  name         = "${var.project_name} API"
  user_pool_id = aws_cognito_user_pool.this.id

  scope {
    scope_name        = "invoke"
    scope_description = "Invoke MCP server"
  }
}

resource "aws_cognito_user_pool_client" "this" {
  name         = "${var.project_name}-client"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret                      = true
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["${local.resource_server_id}/invoke"]
  callback_urls                        = var.callback_urls
  supported_identity_providers         = ["COGNITO"]
  explicit_auth_flows                  = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]

  depends_on = [aws_cognito_resource_server.this]
}

resource "aws_cognito_user_pool_client" "claude" {
  name         = "${var.project_name}-claude"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret                      = true
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["openid", "email", "phone", "profile", "${local.resource_server_id}/invoke"]
  callback_urls                        = ["https://claude.ai/api/mcp/auth_callback", "https://claude.com/api/mcp/auth_callback"]
  supported_identity_providers         = ["COGNITO"]

  depends_on = [aws_cognito_resource_server.this]
}

resource "aws_cognito_user_pool_client" "m2m" {
  name         = "${var.project_name}-m2m"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret                      = true
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["client_credentials"]
  allowed_oauth_scopes                 = ["${local.resource_server_id}/invoke"]
  supported_identity_providers         = ["COGNITO"]

  depends_on = [aws_cognito_resource_server.this]
}

# --- AgentCore Runtime ---

resource "aws_bedrockagentcore_agent_runtime" "this" {
  agent_runtime_name = replace(var.project_name, "-", "_")
  role_arn           = aws_iam_role.agentcore.arn

  agent_runtime_artifact {
    container_configuration {
      container_uri = "${local.ecr_repo_url}:latest"
    }
  }

  protocol_configuration {
    server_protocol = "MCP"
  }

  authorizer_configuration {
    custom_jwt_authorizer {
      discovery_url   = local.cognito_discovery
      allowed_clients = [aws_cognito_user_pool_client.this.id, aws_cognito_user_pool_client.claude.id, aws_cognito_user_pool_client.m2m.id]
    }
  }

  environment_variables = {
    PORT = "8000"
  }

  network_configuration {
    network_mode = "PUBLIC"
  }
}
