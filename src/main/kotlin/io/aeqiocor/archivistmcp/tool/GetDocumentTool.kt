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

class GetDocumentTool(private val indexer: Indexer, private val config: AppConfig) : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "get_document",
            description = "Retrieve the content of a document by its filename or absolute path. " +
                "Use this when you know the exact document name or path from a previous search.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("name") {
                        put("type", "string")
                        put(
                            "description",
                            "Filename (e.g. 'flowmvi-testing.md' or 'flowmvi-testing') or absolute path of the document.",
                        )
                    }
                },
                required = listOf("name"),
            ),
        ) { request ->
            val name = request.arguments?.get("name")?.jsonPrimitive?.contentOrNull
            if (name.isNullOrBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("""{"error": "name is required"}""")),
                    isError = true,
                )
            }

            val file = indexer.getDocument(name)
            if (file == null) {
                CallToolResult(
                    content = listOf(
                        TextContent("""{"status": "not_found", "name": "${name.jsonEscape()}"}"""),
                    ),
                )
            } else {
                val hostPath = config.toHostPath(file.absolutePath)
                CallToolResult(
                    content = listOf(
                        TextContent(
                            """{"status": "ok", "path": "${hostPath.jsonEscape()}", "content": "${file.readText().jsonEscape()}"}""",
                        ),
                    ),
                )
            }
        }
    }
}
