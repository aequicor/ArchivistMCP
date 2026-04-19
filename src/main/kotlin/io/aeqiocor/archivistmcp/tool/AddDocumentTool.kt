package io.aeqiocor.archivistmcp.tool

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

class AddDocumentTool(private val indexer: Indexer) : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "add_document",
            description = "Add a new markdown document to the docs directory and index it for semantic search.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("filename") {
                        put("type", "string")
                        put("description", "Filename with .md extension, e.g. 'guide.md' or 'subdir/notes.md'")
                    }
                    putJsonObject("content") {
                        put("type", "string")
                        put("description", "Markdown content of the document")
                    }
                },
                required = listOf("filename", "content"),
            ),
        ) { request ->
            val filename = request.arguments?.get("filename")?.jsonPrimitive?.contentOrNull
            val content = request.arguments?.get("content")?.jsonPrimitive?.contentOrNull
            if (filename == null || content == null) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "filename and content are required"}""")),
                    isError = true,
                )
            } else {
                indexer.addDocument(filename, content)
                CallToolResult(
                    content = listOf(TextContent("""{"status": "ok", "filename": "$filename"}""")),
                )
            }
        }
    }
}
