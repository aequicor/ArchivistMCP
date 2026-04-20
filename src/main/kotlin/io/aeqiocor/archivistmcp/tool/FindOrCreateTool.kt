package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.Indexer
import io.modelcontextprotocol.kotlin.sdk.server.Server
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

class FindOrCreateTool(
    private val indexer: Indexer,
    private val config: AppConfig,
) : McpTool {

    private val template: String by lazy {
        val file = File(config.templatesDir, "doc-template.md")
        if (file.exists()) file.readText() else DEFAULT_TEMPLATE
    }

    override fun register(server: Server) {
        server.addTool(
            name = "find_or_create",
            description = "Search documents across all modules. If nothing is found, uses AI sampling " +
                "to research the topic (WebSearch + WebFetch) and automatically creates and indexes " +
                "a documentation file. Returns file paths.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "Natural language search query or topic name")
                    }
                    putJsonObject("module") {
                        put("type", "string")
                        put("description", "Optional module name to search within. If omitted, searches all modules.")
                    }
                },
                required = listOf("query"),
            ),
        ) { request ->
            // 'this' is ClientConnection — createMessage() is available directly
            val query = request.arguments?.get("query")?.jsonPrimitive?.contentOrNull
            val module = request.arguments?.get("module")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

            if (query.isNullOrBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("""{"error": "query is required"}""")),
                    isError = true,
                )
            }

            // Step 1: search local index
            val results = indexer.search(module = module, query = query)
            if (results.isNotEmpty()) {
                val paths = results.joinToString(", ", "[", "]") { r ->
                    """"${r.path.jsonEscape()}""""
                }
                return@addTool CallToolResult(
                    content = listOf(TextContent("""{"status": "found", "paths": $paths}""")),
                )
            }

            // Step 2: delegate research to the AI agent via MCP Sampling
            val moduleDir = module
                ?.let { mod -> config.modules.find { it.name == mod }?.dir }
                ?: config.modules.first().dir

            val slug = query.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            val docPath = "$moduleDir/$slug.md"

            val content = try {
                val samplingResult = createMessage(
                    CreateMessageRequest(
                        params = CreateMessageRequestParams(
                            maxTokens = 8000,
                            messages = listOf(
                                SamplingMessage(
                                    role = Role.User,
                                    content = TextContent(buildResearchPrompt(query, template, docPath)),
                                ),
                            ),
                            systemPrompt = "You are a documentation researcher. " +
                                "Follow the instructions and return ONLY the filled markdown document, no explanations.",
                        ),
                    ),
                )
                (samplingResult.content as? TextContent)?.text
            } catch (e: Exception) {
                println("Sampling failed for query '$query': ${e.message}")
                null
            }

            if (content == null) {
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(
                            """{"status": "not_found", "query": "${query.jsonEscape()}", """ +
                                """"reason": "sampling unavailable or returned no content"}""",
                        ),
                    ),
                )
            }

            // Step 3: index the document
            return@addTool try {
                indexer.addDocument(docPath, content)
                CallToolResult(
                    content = listOf(TextContent("""{"status": "created", "path": "${docPath.jsonEscape()}"}""")),
                )
            } catch (e: IllegalArgumentException) {
                CallToolResult(
                    content = listOf(
                        TextContent(
                            """{"status": "error", "message": "${(e.message ?: "failed to add document").jsonEscape()}"}""",
                        ),
                    ),
                    isError = true,
                )
            }
        }
    }

    private fun buildResearchPrompt(query: String, template: String, path: String): String = """
You are a documentation researcher for a software development knowledge base.

## Task
Research the following topic and return ONE filled markdown document.

## Topic
$query

## Document Template
$template

## Instructions
1. Use WebSearch to find relevant sources for the topic.
2. Use WebFetch to read official documentation, GitHub READMEs, or reputable articles.
   Priority: official docs > GitHub README > Baeldung > Stack Overflow (supplement only).
3. Fill in all template placeholders:
   - {name}          — name of the library / technology / concept
   - {keywords}      — comma-separated keywords and synonyms (improves future search recall)
   - {abstract}      — 2–4 sentences: purpose, scope, use cases
   - {documentation} — main body: key APIs, code samples, links to official sources
4. If the query lists multiple entities, combine them into one document.

## Output
Return ONLY the filled markdown document. No explanations, no preamble, no code fences.
    """.trimIndent()

    companion object {
        private const val DEFAULT_TEMPLATE = """# {name}

**Keywords:** {keywords}

## Abstract
{abstract}

## Documentation
{documentation}"""
    }
}
