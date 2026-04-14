package com.jamesward.springaimcpdemo;

import gg.jte.generated.precompiled.StaticTemplates;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.*;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.ai.mcp.annotation.context.MetaProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.webjars.WebJarVersionLocator;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    public int sub(int x, int y, McpSyncRequestContext ctx) {
        ctx.info("subtract " + x + " - " + y);
        return x - y;
    }

    @McpTool(description = "divide two numbers")
    public int divide(int x, int y, McpSyncRequestContext ctx) throws InterruptedException {
        ctx.progress(0);
        int result = x / y;
        Thread.sleep(4000);
        ctx.progress(100);
        return result;
    }

    @McpTool(description = "say hello, but slowly")
    public Mono<String> hello(String name) {
        return Mono.delay(java.time.Duration.ofSeconds(1))
                .map(ignored -> "hello, " + name);
    }

    record UserRandomNumber(Number number) { }

    @McpTool(description = "generate a random number")
    public Number random(McpSyncRequestContext context) {
        Random rand = new Random();
        Number maybeNumber = null;

        if (rand.nextBoolean() && context.elicitEnabled()) {
            var elicitResult = context.elicit(UserRandomNumber.class);
            if (elicitResult.action() == McpSchema.ElicitResult.Action.ACCEPT) {
                maybeNumber = elicitResult.structuredContent().number();
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

    @McpResource(uri = "info://server", name = "Server Info", description = "Static server metadata")
    public String serverInfo() {
        return "Spring AI MCP Demo Server v1.0 — Java " + Runtime.version();
    }

    @McpResource(uri = "config://{key}", name = "Configuration", description = "Provides configuration data")
    public String getConfig(String key) {
        return key != null ? new StringBuilder(key).reverse().toString() : "default";
    }

    static final List<String> CONFIG_KEYS = List.of(
            "database.url", "database.port", "database.name",
            "app.name", "app.version", "app.debug");

    @McpComplete(uri = "config://{key}")
    public List<String> completeConfig(String prefix) {
        return CONFIG_KEYS.stream()
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

@Component
class DiceApp {

    StaticTemplates staticTemplates = new StaticTemplates();

    String appExtJs;

    public DiceApp() throws IOException {
        WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator();
        Resource appExt = new ClassPathResource(Objects.requireNonNull(webJarVersionLocator.fullPath("modelcontextprotocol__ext-apps", "dist/src/app-with-deps.js")));
        appExtJs = appExt.getContentAsString(Charset.defaultCharset());
    }

    @McpResource(name = "Dice App Resource",
            uri = "ui://dice/dice-app.html",
            mimeType = "text/html;profile=mcp-app"
    )
    public String getDiceAppResource() {
        return staticTemplates.diceApp(appExtJs).render();
    }

    @McpTool(
            title = "Roll the Dice",
            name = "roll-the-dice",
            description = "Rolls the dice",
            metaProvider = DiceMetaProvider.class)
    public String rollTheDice() {
        return "Opening dice roller app.";
    }

    public static final class DiceMetaProvider implements MetaProvider {
        @Override
        public Map<String, Object> getMeta() {
            return Map.of("ui",
                    Map.of(
                            "resourceUri", "ui://dice/dice-app.html"));
        }
    }

}

@Component
class ShoppingListApp {

    StaticTemplates staticTemplates = new StaticTemplates();
    String appExtJs;

    public ShoppingListApp() throws IOException {
        WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator();
        Resource appExt = new ClassPathResource(Objects.requireNonNull(webJarVersionLocator.fullPath("modelcontextprotocol__ext-apps", "dist/src/app-with-deps.js")));
        appExtJs = appExt.getContentAsString(Charset.defaultCharset());
    }

    @McpResource(name = "Shopping List App",
            uri = "ui://shopping/shopping-list.html",
            mimeType = "text/html;profile=mcp-app")
    public String getShoppingListResource() {
        return staticTemplates.shoppingList(appExtJs).render();
    }

    @McpTool(title = "Shopping List", name = "shopping-list",
            description = "Opens the shopping list app",
            metaProvider = ShoppingListMetaProvider.class)
    public String openShoppingList() {
        return "Opening shopping list app.";
    }

    public static final class ShoppingListMetaProvider implements MetaProvider {
        @Override
        public Map<String, Object> getMeta() {
            return Map.of("ui", Map.of("resourceUri", "ui://shopping/shopping-list.html"));
        }
    }
}
