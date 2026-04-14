#!/usr/bin/env python3
"""
Verify deployed MCP server on AgentCore Runtime.
1. Gets Cognito bearer token via client_credentials grant (proper OAuth2 with aud claim)
2. Runs MCP Inspector CLI to verify tools, resources, prompts
"""
import base64, json, subprocess, sys, urllib.request, urllib.parse

def tf_output(key):
    script_dir = __import__('os').path.dirname(__import__('os').path.abspath(__file__))
    r = subprocess.run(
        ["terraform", f"-chdir={script_dir}/infra", "output", "-raw", key],
        capture_output=True, text=True, check=True
    )
    return r.stdout.strip()

def run(cmd, label):
    print(f"\n{'='*60}\n{label}\n{'='*60}")
    print(f"$ {' '.join(cmd[:8])}...\n")
    r = subprocess.run(cmd, capture_output=True, text=True)
    print(r.stdout)
    if r.stderr:
        print(r.stderr, file=sys.stderr)
    if r.returncode != 0:
        print(f"FAILED (exit {r.returncode})")
        sys.exit(1)

def main():
    runtime_arn = tf_output("agentcore_runtime_arn")
    pool_id = tf_output("cognito_pool_id")
    client_id = tf_output("cognito_m2m_client_id")
    cognito_domain = tf_output("cognito_domain")
    region = "us-east-1"

    # Get client secret
    desc = subprocess.run([
        "aws", "cognito-idp", "describe-user-pool-client",
        "--user-pool-id", pool_id, "--client-id", client_id, "--region", region
    ], capture_output=True, text=True, check=True)
    client_secret = json.loads(desc.stdout)["UserPoolClient"]["ClientSecret"]

    # Get token via client_credentials grant (carries proper aud + custom scopes)
    print("Getting token via client_credentials grant...")
    token_url = f"{cognito_domain}/oauth2/token"
    creds = base64.b64encode(f"{client_id}:{client_secret}".encode()).decode()
    data = urllib.parse.urlencode({
        "grant_type": "client_credentials",
        "scope": "spring-ai-mcp-demo-api/invoke"
    }).encode()
    req = urllib.request.Request(token_url, data=data, headers={
        "Authorization": f"Basic {creds}",
        "Content-Type": "application/x-www-form-urlencoded"
    })
    resp = urllib.request.urlopen(req)
    token = json.loads(resp.read())["access_token"]
    print(f"Token obtained ({len(token)} chars)")

    # Build AgentCore MCP URL
    encoded_arn = runtime_arn.replace(":", "%3A").replace("/", "%2F")
    mcp_url = f"https://bedrock-agentcore.{region}.amazonaws.com/runtimes/{encoded_arn}/invocations?qualifier=DEFAULT"
    header = f"Authorization: Bearer {token}"

    inspector = ["npx", "@modelcontextprotocol/inspector", "--cli", mcp_url,
                 "--transport", "http", "--header", header]

    run(inspector + ["--method", "tools/list"], "tools/list")
    run(inspector + ["--method", "tools/call", "--tool-name", "add",
                     "--tool-arg", "x=1", "--tool-arg", "y=2"], "tools/call add(1,2)")
    run(inspector + ["--method", "resources/list"], "resources/list")
    run(inspector + ["--method", "prompts/list"], "prompts/list")

    print(f"\n{'='*60}\nAll checks passed!\n{'='*60}")

if __name__ == "__main__":
    main()
