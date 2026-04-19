package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class ReadDocumentTool(private val config: AppConfig) : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "read_document",
            description = "Read a specific document by filename",
        ) { request ->
            val filename = request.arguments?.get("filename")?.jsonPrimitive?.contentOrNull
            if (filename == null) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "filename is required"}""")),
                    isError = true,
                )
            } else {
                val file = File(config.docsDirectory, filename)
                if (!file.exists()) {
                    CallToolResult(
                        content = listOf(TextContent("""{"error": "File not found: $filename"}""")),
                        isError = true,
                    )
                } else {
                    val content = file.readText()
                    CallToolResult(
                        content = listOf(TextContent("""{"filename": "$filename", "content": ${content.jsonEscape()}}""")),
                    )
                }
            }
        }
    }

    private fun String.jsonEscape(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
