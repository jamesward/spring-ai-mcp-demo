package com.jamesward.springaimcpdemo

import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.spec.McpSchema
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.springaicommunity.mcp.annotation.McpElicitation
import org.springaicommunity.mcp.annotation.McpLogging
import org.springaicommunity.mcp.annotation.McpProgress
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class Application {

    @Bean
    fun applicationRunner(mcpClients: List<McpSyncClient>) = ApplicationRunner {
        mcpClients.forEach { client ->
            println(client.serverInfo)
            println(client.serverCapabilities)

            // tools

//            println(client.listTools())
            val toolAddResult = client.callTool(McpSchema.CallToolRequest("add", mapOf("x" to 1, "y" to 2)))
            println(toolAddResult)

            // structured content
            val toolMultResult = client.callTool(McpSchema.CallToolRequest("multiply", mapOf("x" to 2, "y" to 3)))
            val toolMultContent = toolMultResult.structuredContent() as Map<*, *>
            println("result of multiple = ${toolMultContent["result"]}")

            // sends log message
            val toolSubResult = client.callTool(McpSchema.CallToolRequest("sub", mapOf("x" to 4, "y" to 1)))
            println(toolSubResult)

            // sends progress notifications
            val toolProgressResult = client.callTool(McpSchema.CallToolRequest("divide", mapOf("x" to 10, "y" to 2), mapOf("progressToken" to "0")))
            println(toolProgressResult)

            // elicitation - repeated because only sometimes is something elicited
            repeat(5) {
                val toolRandomResult = client.callTool(McpSchema.CallToolRequest("random", emptyMap()))
                println(toolRandomResult)
            }

            // resources

            println(client.listResourceTemplates())

            val resource = client.readResource(McpSchema.ReadResourceRequest("config://foobar"))
            println(resource)

            // completions

            val resourceCompletionRequest = McpSchema.CompleteRequest(McpSchema.ResourceReference("config://{key}"), McpSchema.CompleteRequest.CompleteArgument("key", "a"))
            val resourceCompletion = client.completeCompletion(resourceCompletionRequest)
            println(resourceCompletion)

            // prompts

            val promptResult = client.getPrompt(McpSchema.GetPromptRequest("greeting", mapOf("name" to "James")))
            println(promptResult)
        }
    }

    @McpLogging(clients = ["demo"]) // client id aligns to id in client connection config
    fun handleLoggingMessage(notification: McpSchema.LoggingMessageNotification) {
        LoggerFactory.getLogger(notification.logger())
            .atLevel(Level.valueOf(notification.level.name))
            .log(notification.data())
    }

    @McpProgress(clients = ["demo"])
    fun handleProgressNotification(notification: McpSchema.ProgressNotification) {
        val percentage = notification.progress() * 100
        println(
            String.format(
                "Progress: %.2f%% - %s",
                percentage, notification.message()
            )
        )
    }

    @McpElicitation(clients = ["demo"]) // client id aligns to id in client connection config
    fun handleElicitationRequest(request: McpSchema.ElicitRequest): McpSchema.ElicitResult {
        println("Server has elicited: ${request.message()}")
        val userData = mapOf("number" to "12345")
        return McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT, userData)
    }

}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
