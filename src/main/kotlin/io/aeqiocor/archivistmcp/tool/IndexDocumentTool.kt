package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.Indexer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class IndexDocumentTool(private val indexer: Indexer, private val config: AppConfig) : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "index_document",
            description = "Create a markdown document at the given path and index it. " +
                "The path must be inside one of the configured module directories; " +
                "the module is auto-detected from the path prefix.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("path") {
                        put("type", "string")
                        put(
                            "description",
                            "Path to the document file. Must be within one of the configured module directories. " +
                                "Absolute paths are recommended.",
                        )
                    }
                    putJsonObject("content") {
                        put("type", "string")
                        put("description", "Markdown content of the document")
                    }
                },
                required = listOf("path", "content"),
            ),
        ) { request ->
            val path = request.arguments?.get("path")?.jsonPrimitive?.contentOrNull
            val content = request.arguments?.get("content")?.jsonPrimitive?.contentOrNull
            if (path.isNullOrBlank() || content == null) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "path and content are required"}""")),
                    isError = true,
                )
            } else {
                try {
                    indexer.addDocument(path, content)
                    val hostPath = config.toHostPath(path)
                    CallToolResult(
                        content = listOf(
                            TextContent("""{"status": "ok", "path": "${hostPath.jsonEscape()}"}"""),
                        ),
                    )
                } catch (e: IllegalArgumentException) {
                    CallToolResult(
                        content = listOf(
                            TextContent("""{"error": "${(e.message ?: "invalid path").jsonEscape()}"}"""),
                        ),
                        isError = true,
                    )
                }
            }
        }
    }
}
