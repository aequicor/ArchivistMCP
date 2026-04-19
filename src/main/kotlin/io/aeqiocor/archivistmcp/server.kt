package io.aeqiocor.archivistmcp

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.util.collections.*
import io.modelcontextprotocol.kotlin.sdk.server.*
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.InputStream
import kotlin.system.exitProcess

var docsDirectory: String = System.getenv("DOCS_DIR") ?: "/docs"
var workspaceDirectory: String = System.getenv("WORKSPACE_DIR") ?: System.getProperty("user.dir") ?: "."

fun configureServer(): Server {
    try {
        Indexer.indexDocuments(docsDirectory)
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    val server = Server(
        Implementation(
            name = "ArchivistMCP",
            version = "0.1.0",
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        ),
    )

    server.addPrompt(
        name = "Kotlin Developer",
        description = "Develop small kotlin applications",
        arguments = listOf(
            PromptArgument(
                name = "Project Name",
                description = "Project name for the new project",
                required = true,
            ),
        ),
    ) { request ->
        GetPromptResult(
            messages = listOf(
                PromptMessage(
                    role = Role.User,
                    content = TextContent(
                        "Develop a kotlin project named <name>${request.arguments?.get("Project Name")}</name>",
                    ),
                ),
            ),
            description = "Description for ${request.name}",
        )
    }

    server.addTool(
        name = "list_workspace_files",
        description = "List all files in the workspace directory",
    ) { _ ->
        val dir = File(workspaceDirectory)
        if (!dir.exists() || !dir.isDirectory) {
            CallToolResult(
                content = listOf(TextContent("""{"error": "Directory not found: $workspaceDirectory"}""")),
                isError = true,
            )
        } else {
            val files = dir.walkTopDown()
                .filter { it.isFile }
                .map { it.relativeTo(dir).path }
                .toList()
            CallToolResult(
                content = listOf(TextContent("""{"directory": "$workspaceDirectory", "files": ${files.map { "\"$it\"" }}}""")),
            )
        }
    }

    server.addTool(
        name = "list_documents",
        description = "List all markdown documents in the documents directory",
    ) { _ ->
        val dir = File(docsDirectory)
        if (!dir.exists() || !dir.isDirectory) {
            CallToolResult(
                content = listOf(TextContent("""{"error": "Directory not found: $docsDirectory"}""")),
                isError = true,
            )
        } else {
            val files = dir.walkTopDown()
                .filter { it.isFile && (it.extension == "md" || it.extension == "markdown") }
                .map { it.relativeTo(dir).path }
                .toList()
            CallToolResult(
                content = listOf(TextContent("""{"directory": "$docsDirectory", "files": ${files.map { "\"$it\"" }}}""")),
            )
        }
    }

    server.addTool(
        name = "read_document",
        description = "Read a specific document by filename",
    ) { request ->
        val filename = request.arguments?.get("filename")?.jsonPrimitive?.contentOrNull
        if (filename == null) {
            CallToolResult(
                content = listOf(TextContent("""{"error": "filename is required"}""")),
                isError = true,
            )
        } else {
            val file = File(docsDirectory, filename)
            if (!file.exists()) {
                CallToolResult(
                    content = listOf(TextContent("""{"error": "File not found: $filename"}""")),
                    isError = true,
                )
            } else {
                val content = file.readText()
                CallToolResult(
                    content = listOf(TextContent("""{"filename": "$filename", "content": ${content.jsonEscape()}}""")),
                )
            }
        }
    }

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
            required = listOf("query")
        )
    ) { request ->
        val query = request.arguments?.get("query")?.jsonPrimitive?.contentOrNull
        if (query == null) {
            CallToolResult(
                content = listOf(TextContent("""{"error": "query is required"}""")),
                isError = true,
            )
        } else {
            val results = Indexer.search(query)
            val json = results.joinToString(", ", "[", "]") { (filename, score) ->
                """{"filename": "$filename", "score": ${"%.3f".format(score)}}"""
            }
            CallToolResult(
                content = listOf(TextContent("""{"query": "$query", "results": $json}""")),
            )
        }
    }

    server.addTool(
        name = "kotlin-sdk-tool",
        description = "A test tool",
    ) { _ ->
        CallToolResult(
            content = listOf(TextContent("Hello, world!")),
        )
    }

    server.addResource(
        uri = "https://search.com/",
        name = "Web Search",
        description = "Web search engine",
        mimeType = "text/html",
    ) { request ->
        ReadResourceResult(
            contents = listOf(
                TextResourceContents("Placeholder content for ${request.uri}", request.uri, "text/html"),
            ),
        )
    }

    return server
}

private fun String.jsonEscape(): String {
    return this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

fun runSseMcpServerWithPlainConfiguration(port: Int, wait: Boolean = true): EmbeddedServer<*, *> {
    printBanner(port = port, path = "/sse")
    val serverSessions = ConcurrentMap<String, ServerSession>()

    val server = configureServer()

    val ktorServer = embeddedServer(CIO, host = "127.0.0.1", port = port) {
        installCors()
        install(SSE)
        routing {
            sse("/sse") {
                val transport = SseServerTransport("/message", this)
                val serverSession = server.createSession(transport)
                serverSessions[transport.sessionId] = serverSession

                serverSession.onClose {
                    println("Server session closed for: ${transport.sessionId}")
                    serverSessions.remove(transport.sessionId)
                }
                awaitCancellation()
            }
            post("/message") {
                val sessionId: String? = call.request.queryParameters["sessionId"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing sessionId parameter")
                    return@post
                }

                val transport = serverSessions[sessionId]?.transport as? SseServerTransport
                if (transport == null) {
                    call.respond(HttpStatusCode.NotFound, "Session not found")
                    return@post
                }

                transport.handlePostMessage(call)
            }
        }
    }.start(wait = wait)

    return ktorServer
}

/**
 * Starts an SSE (Server-Sent Events) MCP server using the Ktor plugin.
 *
 * This is the recommended approach for SSE servers as it simplifies configuration.
 * The URL can be accessed in the MCP inspector at http://localhost:[port]/sse
 *
 * @param port The port number on which the SSE MCP server will listen for client connections.
 */
fun runSseMcpServerUsingKtorPlugin(port: Int, wait: Boolean = true): EmbeddedServer<*, *> {
    printBanner(port)

    val server = embeddedServer(CIO, host = "127.0.0.1", port = port) {
        installCors()
        mcp {
            return@mcp configureServer()
        }
    }.start(wait = wait)
    return server
}

private fun printBanner(port: Int, path: String = "") {
    if (port == 0) {
        println("🎬 Starting SSE server on random port")
    } else {
        println("🎬 Starting SSE server on ${if (port > 0) "port $port" else "random port"}")
        println("🔍 Use MCP inspector to connect to http://localhost:$port$path")
    }
}

private fun Application.installCors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowNonSimpleContentTypes = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
}

/**
 * Starts an MCP server using Standard I/O transport.
 *
 * This mode is useful for process-based communication where the server
 * communicates via stdin/stdout with a parent process or client.
 */
class EofDetectingInputStream(
    private val delegate: InputStream,
    private val onEof: () -> Unit,
) : InputStream() {
    override fun read(): Int {
        val b = delegate.read()
        if (b == -1) onEof()
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = delegate.read(b, off, len)
        if (n == -1) onEof()
        return n
    }
}

fun runMcpServerUsingStdio() {
    val server = configureServer()

    val wrappedStdin = EofDetectingInputStream(System.`in`) {
        exitProcess(0)
    }

    val transport = StdioServerTransport(
        inputStream = wrappedStdin.asSource().buffered(),
        outputStream = System.out.asSink().buffered(),
    )

    runBlocking {
        server.createSession(transport)
        awaitCancellation()
    }
}