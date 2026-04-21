package io.aeqiocor.archivistmcp.tool

import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.Indexer
import io.aeqiocor.archivistmcp.ModuleConfig
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageResult
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetDocumentToolTest {

    private val indexer = mockk<Indexer>()
    private val connection = mockk<ClientConnection>()
    private val config = AppConfig(
        modules = listOf(ModuleConfig(name = "docs", dir = "/app/docs")),
        templatesDir = "/app/docs",
        workspaceDirectory = "/workspace",
    )
    private val tool = GetDocumentTool(indexer, config)

    @Test
    fun `returns error when name_or_path is missing`() = runBlocking {
        val result = tool.handle(connection, request(emptyMap()))

        assertTrue(result.isError == true)
        assertContains(result.textContent(), "name_or_path is required")
    }

    @Test
    fun `returns local file content when found`() = runBlocking {
        val file = File.createTempFile("readme", ".md").also { it.writeText("# Readme"); it.deleteOnExit() }
        every { indexer.getDocument("readme") } returns file

        val result = tool.handle(connection, request(mapOf("name_or_path" to "readme")))

        assertFalse(result.isError == true)
        val text = result.textContent()
        assertContains(text, """"source":"local"""")
        assertContains(text, "# Readme")
    }

    @Test
    fun `falls back to mcp sampling when file not found and saves the result`() = runBlocking {
        every { indexer.getDocument("missing-doc") } returns null
        every { indexer.addDocument(any(), any()) } just Runs
        coEvery { connection.createMessage(any(), any()) } returns CreateMessageResult(
            role = Role.Assistant,
            content = TextContent("# Generated Documentation\nThis was generated via MCP sampling."),
            model = "claude",
        )

        val result = tool.handle(connection, request(mapOf("name_or_path" to "missing-doc")))

        assertFalse(result.isError == true)
        val text = result.textContent()
        assertContains(text, """"source":"mcp_sampling"""")
        assertContains(text, "Generated Documentation")
        assertContains(text, """"path"""")
    }

    @Test
    fun `returns unavailable with SKILL hint when sampling is not supported`() = runBlocking {
        every { indexer.getDocument("missing-doc") } returns null
        coEvery { connection.createMessage(any(), any()) } throws RuntimeException("Client does not support sampling (required for sampling/createMessage)")

        val result = tool.handle(connection, request(mapOf("name_or_path" to "missing-doc")))

        assertFalse(result.isError == true)
        val text = result.textContent()
        assertContains(text, """"status":"unavailable"""")
        assertContains(text, "LOOKUP")
    }

    @Test
    fun `returns not_found when mcp sampling fails with generic error`() = runBlocking {
        every { indexer.getDocument("unavailable") } returns null
        coEvery { connection.createMessage(any(), any()) } throws RuntimeException("Network timeout")

        val result = tool.handle(connection, request(mapOf("name_or_path" to "unavailable")))

        assertFalse(result.isError == true)
        assertContains(result.textContent(), """"status":"not_found"""")
        assertContains(result.textContent(), "MCP sampling failed")
    }

    private fun request(args: Map<String, String>) = CallToolRequest(
        params = CallToolRequestParams(
            name = "get_document",
            arguments = buildJsonObject { args.forEach { (k, v) -> put(k, JsonPrimitive(v)) } },
        ),
    )

    private fun assertContains(text: String, substring: String) =
        assertTrue(text.contains(substring), "Expected '$substring' in: $text")
}

private fun io.modelcontextprotocol.kotlin.sdk.types.CallToolResult.textContent() =
    (content.firstOrNull() as? TextContent)?.text ?: ""
