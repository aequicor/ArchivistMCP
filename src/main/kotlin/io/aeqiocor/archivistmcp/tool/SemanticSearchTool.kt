package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.DocumentType
import java.util.Locale
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

class SemanticSearchTool(private val indexer: Indexer, private val config: AppConfig) : McpTool {
    override fun register(server: Server) {
        val availableModules = indexer.modules().joinToString(", ")
        server.addTool(
            name = "semantic_search",
            description = "Search documents within a specific module using semantic similarity (vector search). " +
                "Available modules: $availableModules. " +
                "Optionally filter by document type: ${DocumentType.allNames}. " +
                "If the result is empty — the documentation does not exist yet. " +
                "In that case, generate the documentation yourself and save it using the index_document tool.",
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
                    putJsonObject("type") {
                        put("type", "string")
                        put(
                            "description",
                            "Optional document type filter: ${DocumentType.allNames}. Omit to search all types.",
                        )
                    }
                },
                required = listOf("module", "query"),
            ),
        ) { request -> handle(request) }
    }

    internal fun handle(request: CallToolRequest): CallToolResult {
        val module = request.arguments?.get("module")?.jsonPrimitive?.contentOrNull
        val query = request.arguments?.get("query")?.jsonPrimitive?.contentOrNull
        val typeRaw = request.arguments?.get("type")?.jsonPrimitive?.contentOrNull
        if (module.isNullOrBlank() || query.isNullOrBlank()) {
            return CallToolResult(
                content = listOf(TextContent("""{"error": "module and query are required"}""")),
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
        val results = indexer.search(module, query, type)
        val json = results.joinToString(", ", "[", "]") { r ->
            val hostPath = config.toHostPath(r.path)
            val typeField = if (r.type != null) """, "type": "${r.type.jsonEscape()}"""" else ""
            """{"module": "${r.module.jsonEscape()}", "filename": "${r.filename.jsonEscape()}", "path": "${hostPath.jsonEscape()}", "score": ${String.format(Locale.US, "%.3f", r.score)}$typeField}"""
        }
        val typeFilter = if (type != null) """, "type_filter": "$type"""" else ""
        return CallToolResult(
            content = listOf(
                TextContent("""{"module": "${module.jsonEscape()}", "query": "${query.jsonEscape()}"$typeFilter, "results": $json}"""),
            ),
        )
    }
}

internal fun String.jsonEscape(): String =
    replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
