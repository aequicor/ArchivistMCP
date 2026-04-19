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

class SemanticSearchTool(private val indexer: Indexer) : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "semantic_search",
            description = "Search documents using semantic similarity (vector search). Better than keyword search for natural language queries.",
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
                val json = results.joinToString(", ", "[", "]") { (filename, score) ->
                    """{"filename": "$filename", "score": ${"%.3f".format(score)}}"""
                }
                CallToolResult(
                    content = listOf(TextContent("""{"query": "$query", "results": $json}""")),
                )
            }
        }
    }
}
