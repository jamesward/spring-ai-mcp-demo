package com.jamesward.springaimcpdemo;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springaicommunity.mcp.annotation.McpElicitation;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationRunner applicationRunner(List<McpSyncClient> mcpClients) {
        return args ->
            mcpClients.forEach(client -> {
                System.out.println(client.getServerInfo());
                System.out.println(client.getServerCapabilities());

                // tools
                McpSchema.CallToolResult toolAddResult = client.callTool(new McpSchema.CallToolRequest("add", Map.of("x", 1, "y", 2)));
                System.out.println(toolAddResult);

                // structured content
                McpSchema.CallToolResult toolMultResult = client.callTool(new McpSchema.CallToolRequest("multiply", Map.of("x", 2, "y", 3)));
                Map<?, ?> toolMultContent = (Map<?, ?>) toolMultResult.structuredContent();
                System.out.println("result of multiple = " + toolMultContent.get("result"));

                // sends log message
                McpSchema.CallToolResult toolSubResult = client.callTool(new McpSchema.CallToolRequest("sub", Map.of("x", 4, "y", 1)));
                System.out.println(toolSubResult);

                // sends progress notifications
                McpSchema.CallToolResult toolProgressResult = client.callTool(new McpSchema.CallToolRequest("divide", Map.of("x", 10, "y", 2), Map.of("progressToken", "0")));
                System.out.println(toolProgressResult);

                // elicitation - repeated because only sometimes is something elicited
                for (int i = 0; i < 5; i++) {
                    McpSchema.CallToolResult toolRandomResult = client.callTool(new McpSchema.CallToolRequest("random", Map.of()));
                    System.out.println(toolRandomResult);
                }

                // resources
                System.out.println(client.listResourceTemplates());

                McpSchema.ReadResourceResult resource = client.readResource(new McpSchema.ReadResourceRequest("config://foobar"));
                System.out.println(resource);

                // completions
                McpSchema.CompleteRequest resourceCompletionRequest = new McpSchema.CompleteRequest(
                    new McpSchema.ResourceReference("config://{key}"),
                    new McpSchema.CompleteRequest.CompleteArgument("key", "a")
                );
                McpSchema.CompleteResult resourceCompletion = client.completeCompletion(resourceCompletionRequest);
                System.out.println(resourceCompletion);

                // prompts
                McpSchema.GetPromptResult promptResult = client.getPrompt(new McpSchema.GetPromptRequest("greeting", Map.of("name", "James")));
                System.out.println(promptResult);

                // sampling
                Object loudJoke = client.callTool(new McpSchema.CallToolRequest("loudJoke", Map.of())).content();
                System.out.println(loudJoke);
            });
    }

    @McpLogging(clients = {"demo"})
    public void handleLoggingMessage(McpSchema.LoggingMessageNotification notification) {
        LoggerFactory.getLogger(notification.logger())
            .atLevel(Level.valueOf(notification.level().name()))
            .log(notification.data());
    }

    @McpProgress(clients = {"demo"})
    public void handleProgressNotification(McpSchema.ProgressNotification notification) {
        double percentage = notification.progress() * 100;
        System.out.printf("Progress: %.2f%% - %s%n", percentage, notification.message());
    }

    @McpElicitation(clients = {"demo"})
    public McpSchema.ElicitResult handleElicitationRequest(McpSchema.ElicitRequest request) {
        System.out.println("Server has elicited: " + request.message());
        Map<String, Object> userData = Map.of("number", "12345");
        return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT, userData);
    }

    @McpSampling(clients = {"demo"})
    public McpSchema.CreateMessageResult handleSampling(McpSchema.CreateMessageRequest request) {
        System.out.println("Server has requested sampling: " + request);
        String response = "You're absolutely right!";
        return McpSchema.CreateMessageResult.builder()
            .role(McpSchema.Role.ASSISTANT)
            .content(new McpSchema.TextContent(response))
            .build();
    }
}
