variable "project_name" {
  default = "spring-ai-mcp-demo"
}

variable "region" {
  default = "us-east-1"
}

variable "callback_urls" {
  type    = list(string)
  default = ["http://localhost:3000/callback", "http://localhost:6274/oauth/callback"]
}
