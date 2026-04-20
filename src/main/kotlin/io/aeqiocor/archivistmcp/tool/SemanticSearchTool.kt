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

class SemanticSearchTool(private val indexer: Indexer, private val config: AppConfig) : McpTool {
    override fun register(server: Server) {
        val availableModules = indexer.modules().joinToString(", ")
        server.addTool(
            name = "semantic_search",
            description = "Search documents within a specific module using semantic similarity (vector search). " +
                "First argument is the module name, second is the query. " +
                "Available modules: $availableModules.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("module") {
                        put("type", "string")
                        put("description", "Module name to search within. Available: $availableModules")
                    }
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "Natural language search query")
                    }
                },
                required = listOf("module", "query"),
            ),
        ) { request ->
            val module = request.arguments?.get("module")?.jsonPrimitive?.contentOrNull
            val query = request.arguments?.get("query")?.jsonPrimitive?.contentOrNull
            if (module.isNullOrBlank() || query.isNullOrBlank()) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "module and query are required"}""")),
                    isError = true,
                )
            } else {
                val results = indexer.search(module, query)
                val json = results.joinToString(", ", "[", "]") { r ->
                    val hostPath = config.toHostPath(r.path)
                    """{"module": "${r.module.jsonEscape()}", "filename": "${r.filename.jsonEscape()}", "path": "${hostPath.jsonEscape()}", "score": ${"%.3f".format(r.score)}}"""
                }
                CallToolResult(
                    content = listOf(
                        TextContent(
                            """{"module": "${module.jsonEscape()}", "query": "${query.jsonEscape()}", "results": $json}""",
                        ),
                    ),
                )
            }
        }
    }
}

internal fun String.jsonEscape(): String =
    replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
