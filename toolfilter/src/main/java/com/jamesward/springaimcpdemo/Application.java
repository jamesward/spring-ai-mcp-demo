package com.jamesward.springaimcpdemo;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationRunner toolFilterDemo(List<McpSyncClient> mcpClients) {
        return args -> {
            // Math agent — only sees math MCP tools
            var mathProvider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpClients)
                .toolFilter((conn, tool) -> Set.of("add", "multiply", "sub", "divide").contains(tool.name()))
                .build();

            // Creative agent — only sees creative MCP tools
            var creativeProvider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpClients)
                .toolFilter((conn, tool) -> Set.of("loudJoke", "random", "roll-the-dice").contains(tool.name()))
                .build();

            System.out.println("=== MCP Tool Filtering Demo ===\n");

            System.out.println("Math agent tools: " + toolNames(mathProvider.getToolCallbacks()));
            System.out.println("Creative agent tools: " + toolNames(creativeProvider.getToolCallbacks()));
        };
    }

    static List<String> toolNames(ToolCallback[] callbacks) {
        return Arrays.stream(callbacks).map(t -> t.getToolDefinition().name()).toList();
    }
}
