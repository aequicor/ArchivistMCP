package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.DocumentType
import io.aeqiocor.archivistmcp.Indexer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
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
                "the module is auto-detected from the path prefix. " +
                "When 'type' is provided, the document is automatically placed in a '{moduleDir}/{type}/' subfolder " +
                "unless the path already targets that subfolder. " +
                "Supported types: ${DocumentType.allNames}.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("path") {
                        put("type", "string")
                        put(
                            "description",
                            "Path to the document file. Must be within one of the configured module directories. " +
                                "Absolute paths are recommended. When 'type' is provided, only the filename matters — " +
                                "the subfolder is determined by the type.",
                        )
                    }
                    putJsonObject("content") {
                        put("type", "string")
                        put("description", "Markdown content of the document")
                    }
                    putJsonObject("type") {
                        put("type", "string")
                        put(
                            "description",
                            "Document type — determines the subfolder: ${DocumentType.allNames}. " +
                                "Omit for unclassified documents.",
                        )
                    }
                },
                required = listOf("path", "content"),
            ),
        ) { request -> handle(request) }
    }

    internal fun handle(request: CallToolRequest): CallToolResult {
        val path = request.arguments?.get("path")?.jsonPrimitive?.contentOrNull
        val content = request.arguments?.get("content")?.jsonPrimitive?.contentOrNull
        val typeRaw = request.arguments?.get("type")?.jsonPrimitive?.contentOrNull
        if (path.isNullOrBlank() || content == null) {
            return CallToolResult(
                content = listOf(TextContent("""{"error": "path and content are required"}""")),
                isError = true,
            )
        }
        val type = typeRaw?.let {
            DocumentType.fromString(it)?.folderName
                ?: return CallToolResult(
                    content = listOf(TextContent("""{"error": "unknown type '$it'. Supported: ${DocumentType.allNames}"}""")),
                    isError = true,
                )
        }
        return try {
            indexer.addDocument(path, content, type)
            val hostPath = config.toHostPath(path)
            val typeJson = if (type != null) """, "type": "$type"""" else ""
            CallToolResult(
                content = listOf(TextContent("""{"status": "ok", "path": "${hostPath.jsonEscape()}"$typeJson}""")),
            )
        } catch (e: IllegalArgumentException) {
            CallToolResult(
                content = listOf(TextContent("""{"error": "${(e.message ?: "invalid path").jsonEscape()}"}""")),
                isError = true,
            )
        }
    }
}
