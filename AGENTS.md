# AGENTS.md

## What is this

Spring AI MCP demo — two Gradle modules (`server/` and `client/`) communicating over MCP Streamable HTTP. The server exposes tools, resources, and prompts via Spring AI MCP annotations. The client connects, calls them, and handles callbacks (logging, progress, elicitation, sampling).

## Stack

- Java 25, Spring Boot 4.0.5, Spring AI 2.0.0-M4, Gradle 9.4.1 (Kotlin DSL, always use `./gradlew`)
- Server: `spring-ai-starter-mcp-server-webflux` + JTE templates
- Client: `spring-ai-starter-mcp-client-webflux` (non-web, exits after running)

## Module layout

- `server/src/main/java/.../Application.java` — single file: app entry, tools (`MyTools`), resources (`MyResources`), prompt (`Prompt`), dice app (`DiceApp`), shopping list app (`ShoppingListApp`)
- `client/src/main/java/.../Application.java` — single file: app entry, `ApplicationRunner` that calls all server tools/resources/prompts, callback handlers (`@McpLogging`, `@McpProgress`, `@McpElicitation`, `@McpSampling`)
- `server/src/test/java/.../McpServerIntegrationTest.java` — integration tests using raw MCP client against `@SpringBootTest(RANDOM_PORT)`
- `server/src/main/jte/dice-app.jte` — HTML/JS dice roller MCP app resource
- `server/src/main/jte/shopping-list.jte` — HTML/JS shopping list MCP app resource
- `codemode/`, `toolfilter/` — separate experimental modules

## Running

Start server first, then client in a separate terminal:
```bash
./gradlew :server:bootRun        # localhost:8080
./gradlew :client:bootRun        # connects, runs, exits
```

## Testing

```bash
./gradlew :server:test
```

## Key config

- `server/.../application.properties` — protocol: `streamable`, sync mode, port `${PORT:8080}`
- `client/.../application.properties` — connects to `http://localhost:8080`, connection name: `demo`

## Deploy to AWS AgentCore Runtime

### Prerequisites

- AWS CLI configured with credentials
- Terraform >= 1.5
- Finch (or Docker with buildx)
- Node.js (for MCP Inspector CLI)

### Deploy

```bash
./agentcore/deploy.sh
```

This creates: ECR repo, IAM role, Cognito (user pool + domain + resource server + app client), and AgentCore runtime (MCP protocol, OAuth/JWT authorizer). Then builds the ARM64 container and pushes to ECR.

### Create a test user

```bash
POOL_ID=$(terraform -chdir=agentcore/infra output -raw cognito_pool_id)
aws cognito-idp admin-create-user --user-pool-id $POOL_ID --username testuser --message-action SUPPRESS --region us-east-1
aws cognito-idp admin-set-user-password --user-pool-id $POOL_ID --username testuser --password testpass1 --permanent --region us-east-1
```

### Verify

```bash
COGNITO_USERNAME=testuser COGNITO_PASSWORD=testpass1 python3 agentcore/verify.py
```

Runs Inspector CLI against the deployed server: `tools/list`, `tools/call add(1,2)`, `resources/list`, `prompts/list`.

### Known limitations

- **Inspector OAuth discovery**: AgentCore's `.well-known` endpoints don't return CORS headers, so the Inspector UI can't auto-discover OAuth metadata. Use manual OAuth fields or pre-obtained bearer tokens.
- **Claude.ai connector**: Cognito doesn't advertise custom scopes in OIDC discovery `scopes_supported`. Claude.ai may request scopes not in the client's allowed list. Debugging requires inspecting the exact authorize URL Claude sends.
- **Stateful features**: Elicitation, sampling, and progress work but require an MCP client that declares those capabilities. Inspector CLI and Claude.ai don't support them — tools fall back gracefully.

### Teardown

```bash
terraform -chdir=agentcore/infra destroy
```
