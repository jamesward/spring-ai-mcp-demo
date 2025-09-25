package com.jamesward.springaimcpdemo

import io.modelcontextprotocol.server.McpSyncServerExchange
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.mono
import org.springaicommunity.mcp.annotation.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.random.Random


@SpringBootApplication
class Application

@Component
class MyTools {

    @McpTool(description = "add two numbers")
    fun add(x: Int, y: Int): Int = x + y


    // schema & MCP annotations

    data class MultiplyResult(val result: Int)

    @McpTool(
        name = "multiply",
        description = "multiply two numbers",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true
        )
    )
    fun mult(
        @McpToolParam(description = "a number", required = true) x: Int,
        @McpToolParam(description = "a number", required = true) y: Int): MultiplyResult = MultiplyResult(x + y)


    // notifications - requires streamable or sse

    @McpTool(description = "subtract two numbers")
    fun sub(x: Int, y: Int, exchange: McpSyncServerExchange): Int {
        exchange.loggingNotification(McpSchema.LoggingMessageNotification(McpSchema.LoggingLevel.INFO, "my-log", "subtract $x - $y"))

        return x - y
    }


    // progress notifications - requires streamable or sse

    @McpTool(description = "divide two numbers")
    fun divide(x: Int, y: Int, @McpProgressToken progressToken: String, exchange: McpSyncServerExchange): Int {
        exchange.progressNotification(McpSchema.ProgressNotification(progressToken, 0.0, 1.0, "dividing"))

        val result = x / y

        exchange.progressNotification(McpSchema.ProgressNotification(progressToken, 1.0, 1.0, "divided"))

        return result
    }


    // async - only works if spring.ai.mcp.server.type=async

    @McpTool(description = "say hello, but slowly")
    fun hello(name: String): Mono<String> = mono {
        delay(1000)
        "hello, $name"
    }

    // elicitation

    @McpTool(description = "generate a random number")
    fun random(exchange: McpSyncServerExchange): Int {
        // sometimes additional data is needed
        val maybeNumber = if (Random.nextBoolean()) {
            val schema = mapOf("type" to "object", "properties" to mapOf("number" to mapOf("type" to "integer")), "required" to listOf("number"))
            val userInput = exchange.createElicitation(McpSchema.ElicitRequest("what is your favorite number?", schema))
            (userInput.content["number"] as? String)?.toIntOrNull().takeIf { userInput.action == McpSchema.ElicitResult.Action.ACCEPT }
        } else null

        return maybeNumber ?: Random.nextInt(100)
    }

}

@Component
class MyResources {

    @McpResource(uri = "config://{key}", name = "Configuration", description = "Provides configuration data")
    fun getConfig(key: String?): String {
        return key?.reversed() ?: "default"
    }

    @McpComplete(uri = "config://{key}")
    fun completeConfig(prefix: String): List<String> {
        return listOf("asdf", "zxcv").filter { it.startsWith(prefix) }
    }

    // todo: handle resources/list ?
}

@Component
class Prompt {

    @McpPrompt(name = "greeting", description = "Greet the user")
    fun greeting(@McpArg(name = "name", required = false) name: String?): String =
        "hello, ${name ?: "world"}"

    @McpComplete(prompt = "greeting")
    fun completeConfig(prefix: String): List<String> {
        return listOf("James", "Josh").filter { it.startsWith(prefix, ignoreCase = true) }
    }

}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
