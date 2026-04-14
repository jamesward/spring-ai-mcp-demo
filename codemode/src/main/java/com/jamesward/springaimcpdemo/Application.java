package com.jamesward.springaimcpdemo;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
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
    public ApplicationRunner codeMode(List<McpSyncClient> clients) {
        return args -> clients.forEach(client -> {
            System.out.println("=== Code Mode: chaining MCP tool calls ===\n");

            // Discover available tools
            var tools = client.listTools().tools();
            System.out.println("Available tools: " + tools.stream().map(McpSchema.Tool::name).toList());

            // --- Code Mode script: "add 3+4, multiply by 5, subtract 10" ---
            // An LLM would generate this code block as a single tool call,
            // instead of 3 separate LLM ↔ tool round-trips.

            // Step 1: add(3, 4) → 7
            var addResult = callTool(client, "add", Map.of("x", 3, "y", 4));
            int sum = Integer.parseInt(text(addResult));
            System.out.println("add(3, 4) = " + sum);

            // Step 2: multiply(sum, 5) → 35
            var multResult = callTool(client, "multiply", Map.of("x", sum, "y", 5));
            int product = ((Number) ((Map<?, ?>) multResult.structuredContent()).get("result")).intValue();
            System.out.println("multiply(" + sum + ", 5) = " + product);

            // Step 3: sub(product, 10) → 25
            var subResult = callTool(client, "sub", Map.of("x", product, "y", 10));
            int diff = Integer.parseInt(text(subResult));
            System.out.println("sub(" + product + ", 10) = " + diff);

            System.out.println("\nFinal result: " + diff);
        });
    }

    static CallToolResult callTool(McpSyncClient client, String name, Map<String, Object> args) {
        return client.callTool(new CallToolRequest(name, args));
    }

    static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }
}
