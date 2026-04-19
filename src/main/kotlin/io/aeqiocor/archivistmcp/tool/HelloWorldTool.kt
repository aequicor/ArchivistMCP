package io.aeqiocor.archivistmcp.tool

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

class HelloWorldTool : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "kotlin-sdk-tool",
            description = "A test tool",
        ) { _ ->
            CallToolResult(
                content = listOf(TextContent("Hello, world!")),
            )
        }
    }
}
