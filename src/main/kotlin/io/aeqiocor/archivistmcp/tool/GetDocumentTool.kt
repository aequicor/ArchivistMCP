package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.DocumentType
import io.aeqiocor.archivistmcp.Indexer
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequest
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.SamplingMessage
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File

class GetDocumentTool(private val indexer: Indexer, private val config: AppConfig) : McpTool {

    companion object {
        private const val SAMPLING_UNAVAILABLE_MARKER = "does not support sampling"

        @Volatile
        private var bannerPrinted = false

        fun printSamplingUnavailableBanner() {
            if (bannerPrinted) return
            bannerPrinted = true
            System.err.println(
                """
                |╔══════════════════════════════════════════════════════════════╗
                |║  WARNING: MCP Sampling is not available on this client       ║
                |║                                                              ║
                |║  get_document requires Sampling to fetch docs from internet. ║
                |║  The tool has been removed from the tool list.               ║
                |║                                                              ║
                |║  Alternative — use the LOOKUP skill:                         ║
                |║    Ask Claude: "use LOOKUP skill to find <document name>"    ║
                |╚══════════════════════════════════════════════════════════════╝
                """.trimMargin(),
            )
        }
    }

    override fun register(server: Server) {
        server.addTool(
            name = "get_document",
            description = "Get a document by name or path. " +
                "First searches in local modules. " +
                "If not found, uses MCP Sampling to ask the client to fetch information from the internet and generate the documentation. " +
                "When 'type' is specified, newly fetched documents are saved in the '{moduleDir}/{type}/' subfolder. " +
                "Supported types: ${DocumentType.allNames}.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("name_or_path") {
                        put("type", "string")
                        put("description", "Document name (e.g., 'readme') or absolute path (e.g., '/docs/api/readme.md')")
                    }
                    putJsonObject("type") {
                        put("type", "string")
                        put(
                            "description",
                            "Optional document type — determines subfolder for newly created docs: ${DocumentType.allNames}.",
                        )
                    }
                },
                required = listOf("name_or_path"),
            ),
        ) { request -> handle(this, request) }
    }

    internal suspend fun handle(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        val nameOrPath = request.arguments?.get("name_or_path")?.jsonPrimitive?.contentOrNull
        val typeRaw = request.arguments?.get("type")?.jsonPrimitive?.contentOrNull
        if (nameOrPath.isNullOrBlank()) {
            return CallToolResult(
                content = listOf(TextContent("""{"error": "name_or_path is required"}""")),
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
            val result = getDocumentWithFallback(connection, nameOrPath, type)
            CallToolResult(content = listOf(TextContent(result)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent("""{"error": "${e.message?.jsonEscape() ?: "Unknown error"}"}""")),
                isError = true,
            )
        }
    }

    private suspend fun getDocumentWithFallback(connection: ClientConnection, nameOrPath: String, type: String? = null): String {
        val localFile = indexer.getDocument(nameOrPath)
        if (localFile != null && localFile.exists()) {
            val hostPath = config.toHostPath(localFile.absolutePath)
            return buildJsonObject {
                put("status", "found")
                put("source", "local")
                put("path", hostPath)
                put("content", localFile.readText())
            }.toString()
        }

        return tryMcpSampling(connection, nameOrPath, type)
    }

    private suspend fun tryMcpSampling(connection: ClientConnection, nameOrPath: String, type: String? = null): String {
        return try {
            val result = connection.createMessage(
                CreateMessageRequest(
                    params = CreateMessageRequestParams(
                        maxTokens = 4096,
                        messages = listOf(
                            SamplingMessage(
                                role = Role.User,
                                content = TextContent(
                                    "Search the internet for information about \"$nameOrPath\" and produce comprehensive Markdown documentation for it. " +
                                        "Include: overview, key concepts, configuration, usage examples, and links to official sources.",
                                ),
                            ),
                        ),
                        systemPrompt = "You are a documentation specialist. " +
                            "When asked about a topic, search the internet and produce comprehensive Markdown documentation.",
                    ),
                ),
            )
            val content = (result.content as? TextContent)?.text
                ?: return buildJsonObject {
                    put("status", "not_found")
                    put("error", "MCP sampling returned no text content for '$nameOrPath'")
                }.toString()

            val savedPath = saveAndIndex(nameOrPath, content, type)
            val hostPath = config.toHostPath(savedPath)

            buildJsonObject {
                put("status", "found")
                put("source", "mcp_sampling")
                put("path", hostPath)
                put("content", content)
            }.toString()
        } catch (e: Exception) {
            if (e.message?.contains(SAMPLING_UNAVAILABLE_MARKER) == true) {
                printSamplingUnavailableBanner()
                return buildJsonObject {
                    put("status", "unavailable")
                    put("error", "MCP Sampling is not available on this client")
                    put("hint", "Use the LOOKUP skill: ask Claude to use LOOKUP skill to find '$nameOrPath'")
                }.toString()
            }
            buildJsonObject {
                put("status", "not_found")
                put("error", "MCP sampling failed: ${e.message}")
            }.toString()
        }
    }

    private fun saveAndIndex(nameOrPath: String, content: String, type: String? = null): String {
        val moduleDir = config.modules.first().dir
        val filename = nameOrPath
            .substringAfterLast("/")
            .replace(Regex("[^a-zA-Z0-9._-]"), "-")
            .lowercase()
            .let { if (it.endsWith(".md")) it else "$it.md" }
        val subfolder = type ?: "sampled"
        val path = "$moduleDir/$subfolder/$filename"
        File(path).parentFile?.mkdirs()
        indexer.addDocument(path, content, type)
        return path
    }
}
