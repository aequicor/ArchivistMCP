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

class SmartSearchTool(private val indexer: Indexer) : McpTool {
    private val template: String by lazy {
        SmartSearchTool::class.java.classLoader
            .getResourceAsStream("doc-template.md")
            ?.bufferedReader()?.readText() ?: ""
    }

    override fun register(server: Server) {
        server.addTool(
            name = "smart_search",
            description = "Search documents by semantic similarity. If nothing is found, returns a documentation template and instructs to call add_document to create and index a new document.",
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
            if (query == null) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "query is required"}""")),
                    isError = true,
                )
            } else {
                val results = indexer.search(query)
                if (results.isNotEmpty()) {
                    val json = results.joinToString(", ", "[", "]") { (filename, score) ->
                        """{"filename": "$filename", "score": ${"%.3f".format(score)}}"""
                    }
                    CallToolResult(content = listOf(TextContent(
                        """{"status": "found", "query": "$query", "results": $json}"""
                    )))
                } else {
                    val slug = query.lowercase()
                        .replace(Regex("[^a-z0-9]+"), "-")
                        .trim('-')
                    val escapedTemplate = template
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                    CallToolResult(content = listOf(TextContent(
                        """{"status": "not_found", "query": "$query", "action": "add_document", "suggested_filename": "$slug.md", "template": "$escapedTemplate"}"""
                    )))
                }
            }
        }
    }
}
