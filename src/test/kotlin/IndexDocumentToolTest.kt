package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.Indexer
import io.aeqiocor.archivistmcp.ModuleConfig
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexDocumentToolTest {

    private val indexer = mockk<Indexer>()
    private val config = AppConfig(
        modules = listOf(ModuleConfig(name = "docs", dir = "/app/docs")),
        templatesDir = "/app/docs",
        workspaceDirectory = "/workspace",
    )
    private val tool = IndexDocumentTool(indexer, config)

    @Test
    fun `returns error when path is missing`() {
        val result = tool.handle(request(mapOf("content" to "# Hello")))

        assertTrue(result.isError == true)
        assertContains(result.textContent(), "path and content are required")
    }

    @Test
    fun `returns error when content is missing`() {
        val result = tool.handle(request(mapOf("path" to "/app/docs/guide.md")))

        assertTrue(result.isError == true)
        assertContains(result.textContent(), "path and content are required")
    }

    @Test
    fun `returns success when document is indexed`() {
        every { indexer.addDocument("/app/docs/guide.md", "# Guide") } just Runs

        val result = tool.handle(request(mapOf("path" to "/app/docs/guide.md", "content" to "# Guide")))

        assertFalse(result.isError == true)
        assertContains(result.textContent(), """"status": "ok"""")
        assertContains(result.textContent(), "guide.md")
        verify { indexer.addDocument("/app/docs/guide.md", "# Guide") }
    }

    @Test
    fun `returns error when path is outside module directory`() {
        every { indexer.addDocument("/outside/guide.md", any()) } throws
            IllegalArgumentException("Path '/outside/guide.md' is not within any configured module directory")

        val result = tool.handle(request(mapOf("path" to "/outside/guide.md", "content" to "# Guide")))

        assertTrue(result.isError == true)
        assertContains(result.textContent(), "not within any configured module directory")
    }

    private fun request(args: Map<String, String>) = CallToolRequest(
        params = CallToolRequestParams(
            name = "index_document",
            arguments = buildJsonObject { args.forEach { (k, v) -> put(k, JsonPrimitive(v)) } },
        ),
    )

    private fun assertContains(text: String, substring: String) =
        assertTrue(text.contains(substring), "Expected '$substring' in: $text")
}

private fun io.modelcontextprotocol.kotlin.sdk.types.CallToolResult.textContent() =
    (content.firstOrNull() as? TextContent)?.text ?: ""
