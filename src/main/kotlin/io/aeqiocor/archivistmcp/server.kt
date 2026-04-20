package io.aeqiocor.archivistmcp

import io.aeqiocor.archivistmcp.tool.AddDocumentTool
import io.aeqiocor.archivistmcp.tool.FindOrCreateTool
import io.aeqiocor.archivistmcp.tool.GetDocumentTool
import io.aeqiocor.archivistmcp.tool.McpTool
import io.aeqiocor.archivistmcp.tool.SemanticSearchTool
import io.aeqiocor.archivistmcp.tool.SmartSearchTool
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
import java.io.InputStream
import kotlin.system.exitProcess

fun configureServer(config: AppConfig, indexer: Indexer): Server {
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

    val tools: List<McpTool> = listOf(
        SemanticSearchTool(indexer, config),
        AddDocumentTool(indexer, config),
        SmartSearchTool(indexer, config),
        FindOrCreateTool(indexer, config),
        GetDocumentTool(indexer, config),
    )
    tools.forEach { it.register(server) }

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

fun runSseMcpServerWithPlainConfiguration(
    port: Int,
    config: AppConfig,
    indexer: Indexer,
    wait: Boolean = true,
): EmbeddedServer<*, *> {
    printBanner(port = port, path = "/sse")
    val serverSessions = ConcurrentMap<String, ServerSession>()

    val server = configureServer(config, indexer)

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
fun runSseMcpServerUsingKtorPlugin(
    port: Int,
    config: AppConfig,
    indexer: Indexer,
    wait: Boolean = true,
): EmbeddedServer<*, *> {
    printBanner(port)

    val server = embeddedServer(CIO, host = "127.0.0.1", port = port) {
        installCors()
        mcp {
            return@mcp configureServer(config, indexer)
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

fun runMcpServerUsingStdio(config: AppConfig, indexer: Indexer) {
    val server = configureServer(config, indexer)

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
