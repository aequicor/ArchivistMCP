package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File

class ListWorkspaceFilesTool(private val config: AppConfig) : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "list_workspace_files",
            description = "List all files in the workspace directory",
        ) { _ ->
            val dir = File(config.workspaceDirectory)
            if (!dir.exists() || !dir.isDirectory) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "Directory not found: ${config.workspaceDirectory}"}""")),
                    isError = true,
                )
            } else {
                val files = dir.walkTopDown()
                    .filter { it.isFile }
                    .map { it.relativeTo(dir).path }
                    .toList()
                CallToolResult(
                    content = listOf(TextContent("""{"directory": "${config.workspaceDirectory}", "files": ${files.map { "\"$it\"" }}}""")),
                )
            }
        }
    }
}
