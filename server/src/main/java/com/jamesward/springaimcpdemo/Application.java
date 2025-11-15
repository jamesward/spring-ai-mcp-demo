package com.jamesward.springaimcpdemo;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.*;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Component
class MyTools {

    @McpTool(description = "add two numbers")
    public int add(int x, int y) {
        return x + y;
    }

    record MultiplyResult(int result) { }

    @McpTool(
        name = "multiply",
        description = "multiply two numbers",
        generateOutputSchema = true,
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true
        )
    )
    public MultiplyResult mult(
        @McpToolParam(description = "a number", required = true) int x,
        @McpToolParam(description = "a number", required = true) int y) {
        return new MultiplyResult(x * y);
    }

    @McpTool(description = "subtract two numbers")
    public int sub(int x, int y, McpSyncServerExchange exchange) {
        exchange.loggingNotification(new McpSchema.LoggingMessageNotification(McpSchema.LoggingLevel.INFO, "my-log", "subtract " + x + " - " + y));
        return x - y;
    }

    @McpTool(description = "divide two numbers")
    public int divide(int x, int y, McpSyncServerExchange exchange, McpMeta mcpMeta) { //, @McpProgressToken String progressToken
        var progressToken = mcpMeta.get("progressToken");
        exchange.progressNotification(new McpSchema.ProgressNotification(progressToken, 0.0, 1.0, "dividing"));
        int result = x / y;
        exchange.progressNotification(new McpSchema.ProgressNotification(progressToken, 1.0, 1.0, "divided"));
        return result;
    }

    @McpTool(description = "say hello, but slowly")
    public Mono<String> hello(String name) {
        return Mono.delay(java.time.Duration.ofSeconds(1))
                .map(ignored -> "hello, " + name);
    }

    @McpTool(description = "generate a random number")
    public int random(McpSyncServerExchange exchange) {
        Random rand = new Random();
        Integer maybeNumber = null;

        if (rand.nextBoolean()) {
            Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("number", Map.of("type", "integer")),
                "required", List.of("number")
            );
            McpSchema.ElicitResult userInput = exchange.createElicitation(new McpSchema.ElicitRequest("what is your favorite number?", schema));
            if (userInput.action() == McpSchema.ElicitResult.Action.ACCEPT) {
                maybeNumber = (Integer) userInput.content().get("number");
            }
        }

        return maybeNumber != null ? maybeNumber : rand.nextInt(100);
    }

    @McpTool(description = "tell a joke")
    public String loudJoke(McpSyncRequestContext context) {
        if (context.sampleEnabled()) {
            var result = context.sample("Tell me a joke!");
            var content = (McpSchema.TextContent) result.content();
            return content.text().toUpperCase();
        }
        else {
            return "NO JOKE";
        }
    }
}

@Component
class MyResources {

    @McpResource(uri = "config://{key}", name = "Configuration", description = "Provides configuration data")
    public String getConfig(String key) {
        return key != null ? new StringBuilder(key).reverse().toString() : "default";
    }

    @McpComplete(uri = "config://{key}")
    public List<String> completeConfig(String prefix) {
        return Stream.of("asdf", "zxcv")
                .filter(s -> s.startsWith(prefix))
                .toList();
    }
}

@Component
class Prompt {

    @McpPrompt(name = "greeting", description = "Greet the user")
    public String greeting(@McpArg(name = "name", required = false) String name) {
        return "hello, " + (name != null ? name : "world");
    }

    @McpComplete(prompt = "greeting")
    public List<String> completeConfig(String prefix) {
        return Stream.of("James", "Josh")
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }
}
