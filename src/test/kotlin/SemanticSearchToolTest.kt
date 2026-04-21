package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.Indexer
import io.aeqiocor.archivistmcp.ModuleConfig
import io.aeqiocor.archivistmcp.SearchResult
import io.mockk.every
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemanticSearchToolTest {

    private val indexer = mockk<Indexer>()
    private val config = AppConfig(
        modules = listOf(ModuleConfig(name = "payments", dir = "/docs/payments")),
        templatesDir = "/docs",
        workspaceDirectory = "/workspace",
    )
    private val tool = SemanticSearchTool(indexer, config)

    @Test
    fun `returns error when module is missing`() {
        val request = request(mapOf("query" to "authentication"))

        val result = tool.handle(request)

        assertTrue(result.isError == true)
        assertContains(result.textContent(), "module and query are required")
    }

    @Test
    fun `returns error when query is missing`() {
        val request = request(mapOf("module" to "payments"))

        val result = tool.handle(request)

        assertTrue(result.isError == true)
        assertContains(result.textContent(), "module and query are required")
    }

    @Test
    fun `returns empty results when nothing found`() {
        every { indexer.search("payments", "authentication") } returns emptyList()

        val result = tool.handle(request(mapOf("module" to "payments", "query" to "authentication")))

        assertFalse(result.isError == true)
        assertContains(result.textContent(), """"results": []""")
    }

    @Test
    fun `returns results from indexer`() {
        every { indexer.search("payments", "retry logic") } returns listOf(
            SearchResult(module = "payments", filename = "retry.md", path = "/docs/payments/retry.md", score = 0.923),
        )

        val result = tool.handle(request(mapOf("module" to "payments", "query" to "retry logic")))

        assertFalse(result.isError == true)
        val text = result.textContent()
        assertContains(text, "retry.md")
        assertContains(text, "0.923")
    }

    @Test
    fun `escapes special characters in results`() {
        every { indexer.search("payments", "query") } returns listOf(
            SearchResult(module = "payments", filename = "file\"name.md", path = "/docs/payments/file\"name.md", score = 0.9),
        )

        val result = tool.handle(request(mapOf("module" to "payments", "query" to "query")))

        assertFalse(result.isError == true)
        assertContains(result.textContent(), "\\\"")
    }

    private fun request(args: Map<String, String>) = CallToolRequest(
        params = CallToolRequestParams(
            name = "semantic_search",
            arguments = buildJsonObject { args.forEach { (k, v) -> put(k, JsonPrimitive(v)) } },
        ),
    )

    private fun assertContains(text: String, substring: String) =
        assertTrue(text.contains(substring), "Expected '$substring' in: $text")
}

private fun io.modelcontextprotocol.kotlin.sdk.types.CallToolResult.textContent() =
    (content.firstOrNull() as? TextContent)?.text ?: ""
