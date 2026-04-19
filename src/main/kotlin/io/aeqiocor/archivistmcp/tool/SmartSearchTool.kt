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
import java.io.File

class SmartSearchTool(
    private val indexer: Indexer,
    private val templatesDir: String,
) : McpTool {
    private val template: String by lazy {
        val file = File(templatesDir, "doc-template.md")
        if (file.exists()) file.readText() else ""
    }

    override fun register(server: Server) {
        server.addTool(
            name = "smart_search",
            description = "Search documents by semantic similarity across all modules. " +
                "If nothing is found, returns a documentation template and instructs to call add_document.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "Natural language search query")
                    }
                },
                required = listOf("query"),
            ),
        ) { request ->
            val query = request.arguments?.get("query")?.jsonPrimitive?.contentOrNull
            if (query.isNullOrBlank()) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "query is required"}""")),
                    isError = true,
                )
            } else {
                val results = indexer.search(module = null, query = query)
                if (results.isNotEmpty()) {
                    val json = results.joinToString(", ", "[", "]") { r ->
                        """{"module": "${r.module.jsonEscape()}", "filename": "${r.filename.jsonEscape()}", "path": "${r.path.jsonEscape()}", "score": ${"%.3f".format(r.score)}}"""
                    }
                    CallToolResult(
                        content = listOf(
                            TextContent(
                                """{"status": "found", "query": "${query.jsonEscape()}", "results": $json}""",
                            ),
                        ),
                    )
                } else {
                    val slug = query.lowercase()
                        .replace(Regex("[^a-z0-9]+"), "-")
                        .trim('-')
                    CallToolResult(
                        content = listOf(
                            TextContent(
                                """{"status": "not_found", "query": "${query.jsonEscape()}", "action": "add_document", "suggested_filename": "$slug.md", "template": "${template.jsonEscape()}"}""",
                            ),
                        ),
                    )
                }
            }
        }
    }
}
