package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File

class ListDocumentsTool(private val config: AppConfig) : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "list_documents",
            description = "List all markdown documents in the documents directory",
        ) { _ ->
            val dir = File(config.docsDirectory)
            if (!dir.exists() || !dir.isDirectory) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "Directory not found: ${config.docsDirectory}"}""")),
                    isError = true,
                )
            } else {
                val files = dir.walkTopDown()
                    .filter { it.isFile && (it.extension == "md" || it.extension == "markdown") }
                    .map { it.relativeTo(dir).path }
                    .toList()
                CallToolResult(
                    content = listOf(TextContent("""{"directory": "${config.docsDirectory}", "files": ${files.map { "\"$it\"" }}}""")),
                )
            }
        }
    }
}
