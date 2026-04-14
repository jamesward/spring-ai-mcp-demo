package com.jamesward.springaimcpdemo;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {

    @LocalServerPort
    int port;

    McpSyncClient client;
    List<McpSchema.LoggingMessageNotification> logMessages = new CopyOnWriteArrayList<>();
    List<McpSchema.ProgressNotification> progressNotifications = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        logMessages.clear();
        progressNotifications.clear();

        McpClientTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();

        client = McpClient.sync(transport)
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .sampling()
                        .elicitation()
                        .build())
                .loggingConsumer(logMessages::add)
                .progressConsumer(progressNotifications::add)
                .sampling(request -> McpSchema.CreateMessageResult.builder()
                        .role(McpSchema.Role.ASSISTANT)
                        .content(new McpSchema.TextContent("funny joke"))
                        .model("test")
                        .build())
                .elicitation(request -> new McpSchema.ElicitResult(
                        McpSchema.ElicitResult.Action.ACCEPT, Map.of("number", 42)))
                .build();

        client.initialize();
    }

    @AfterEach
    void tearDown() {
        client.closeGracefully();
    }

    // --- Tools ---

    @Test
    void toolRegistration_syncToolsAreRegistered() {
        List<String> toolNames = client.listTools().tools().stream()
                .map(McpSchema.Tool::name).toList();

        assertThat(toolNames).containsExactlyInAnyOrder(
                "add", "multiply", "sub", "divide", "random", "loudJoke",
                "roll-the-dice", "shopping-list");
    }

    @Test
    void addTool_returnsSumOfTwoNumbers() {
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("add", Map.of("x", 3, "y", 7)));

        assertThat(result.isError()).isFalse();
        assertThat(result.content().toString()).contains("10");
    }

    @Test
    void multiplyTool_returnsStructuredContent() {
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("multiply", Map.of("x", 4, "y", 5)));

        assertThat(result.isError()).isFalse();
        @SuppressWarnings("unchecked")
        Map<String, Object> structured = (Map<String, Object>) result.structuredContent();
        assertThat(structured).containsEntry("result", 20);
    }

    @Test
    void subTool_returnsResultAndSendsLogNotification() {
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("sub", Map.of("x", 10, "y", 3)));

        assertThat(result.content().toString()).contains("7");
        assertThat(logMessages).anyMatch(log ->
                log.level() == McpSchema.LoggingLevel.INFO && log.data().contains("subtract"));
    }

    @Test
    void divideTool_returnsResultAndSendsProgressNotifications() {
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("divide", Map.of("x", 10, "y", 2),
                        Map.of("progressToken", "p1")));

        assertThat(result.content().toString()).contains("5");
        assertThat(progressNotifications).hasSizeGreaterThanOrEqualTo(2);
        assertThat(progressNotifications.getLast().progress()).isEqualTo(1.0);
    }

    @Test
    void loudJokeTool_usesSamplingAndUppercasesResult() {
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("loudJoke", Map.of()));

        assertThat(result.isError()).isFalse();
        assertThat(result.content().toString()).contains("FUNNY JOKE");
    }

    // --- Resources ---

    @Test
    void resourceTemplates_configTemplateIsRegistered() {
        List<McpSchema.ResourceTemplate> templates = client.listResourceTemplates().resourceTemplates();

        assertThat(templates).anyMatch(t -> t.uriTemplate().contains("config://"));
    }

    @Test
    void readResource_returnsReversedKey() {
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest("config://hello"));

        String text = ((McpSchema.TextResourceContents) result.contents().getFirst()).text();
        assertThat(text).isEqualTo("olleh");
    }

    @Test
    void resourceCompletion_returnsMatchingKeys() {
        McpSchema.CompleteResult result = client.completeCompletion(new McpSchema.CompleteRequest(
                new McpSchema.ResourceReference("config://{key}"),
                new McpSchema.CompleteRequest.CompleteArgument("key", "database.")));

        assertThat(result.completion().values()).contains("database.url", "database.port", "database.name");
    }

    @Test
    void fixedResource_serverInfoReturnsMetadata() {
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest("info://server"));

        String text = ((McpSchema.TextResourceContents) result.contents().getFirst()).text();
        assertThat(text).contains("Spring AI MCP Demo Server");
    }

    @Test
    void fixedResource_serverInfoAppearsInResourceList() {
        List<McpSchema.Resource> resources = client.listResources().resources();
        assertThat(resources).anyMatch(r -> "info://server".equals(r.uri()));
    }

    // --- Prompts ---

    @Test
    void prompts_greetingIsRegistered() {
        List<McpSchema.Prompt> prompts = client.listPrompts().prompts();

        assertThat(prompts).anyMatch(p -> "greeting".equals(p.name()));
    }

    @Test
    void getPrompt_returnsGreetingWithName() {
        McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest("greeting", Map.of("name", "Alice")));

        assertThat(result.messages().toString()).contains("hello, Alice");
    }

    @Test
    void promptCompletion_returnsMatchingNames() {
        McpSchema.CompleteResult result = client.completeCompletion(new McpSchema.CompleteRequest(
                new McpSchema.PromptReference("greeting"),
                new McpSchema.CompleteRequest.CompleteArgument("name", "J")));

        assertThat(result.completion().values()).containsExactlyInAnyOrder("James", "Josh");
    }

    // --- MCP App ---

    @Test
    void toolRegistration_includesRollTheDice() {
        List<McpSchema.Tool> tools = client.listTools().tools();
        McpSchema.Tool diceTool = tools.stream()
                .filter(t -> "roll-the-dice".equals(t.name())).findFirst().orElseThrow();

        // Tool should have ui.resourceUri metadata linking to the HTML resource
        @SuppressWarnings("unchecked")
        Map<String, Object> ui = (Map<String, Object>) diceTool.meta().get("ui");
        assertThat(ui).containsEntry("resourceUri", "ui://dice/dice-app.html");
    }

    @Test
    void rollTheDiceTool_returnsOpeningMessage() {
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("roll-the-dice", Map.of()));

        assertThat(result.isError()).isFalse();
        assertThat(result.content().toString()).contains("Opening dice roller app");
    }

    @Test
    void diceAppResource_servesHtmlWithMcpAppProfile() {
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest("ui://dice/dice-app.html"));

        McpSchema.TextResourceContents contents =
                (McpSchema.TextResourceContents) result.contents().getFirst();
        assertThat(contents.mimeType()).isEqualTo("text/html;profile=mcp-app");
        assertThat(contents.text()).contains("Dice Roller");
        assertThat(contents.text()).contains("ext-apps");
    }

    // --- Shopping List MCP App ---

    @Test
    void shoppingListTool_hasUiMetadata() {
        McpSchema.Tool tool = client.listTools().tools().stream()
                .filter(t -> "shopping-list".equals(t.name())).findFirst().orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> ui = (Map<String, Object>) tool.meta().get("ui");
        assertThat(ui).containsEntry("resourceUri", "ui://shopping/shopping-list.html");
    }

    @Test
    void shoppingListTool_returnsOpeningMessage() {
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("shopping-list", Map.of()));

        assertThat(result.isError()).isFalse();
        assertThat(result.content().toString()).contains("Opening shopping list app");
    }

    @Test
    void shoppingListResource_servesHtmlWithMcpAppProfile() {
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest("ui://shopping/shopping-list.html"));

        McpSchema.TextResourceContents contents =
                (McpSchema.TextResourceContents) result.contents().getFirst();
        assertThat(contents.mimeType()).isEqualTo("text/html;profile=mcp-app");
        assertThat(contents.text()).contains("Shopping List");
        assertThat(contents.text()).contains("ext-apps");
    }
}
