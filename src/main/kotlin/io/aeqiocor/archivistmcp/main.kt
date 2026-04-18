package io.aeqiocor.archivistmcp

import kotlinx.coroutines.runBlocking

fun main(vararg args: String): Unit = runBlocking {
    val command = args.firstOrNull() ?: "--stdio"
    val port = args.getOrNull(1)?.toIntOrNull() ?: 3001
    val docsDir = args.filter { it.startsWith("--docs-dir=") }
        .firstOrNull()
        ?.substringAfter("=")
        ?: System.getenv("DOCS_DIR")
        ?: "./docs"

    System.setProperty("docs.dir", docsDir)
    docsDirectory = docsDir

    when (command) {
        "--stdio" -> runMcpServerUsingStdio()
        "--sse-server-ktor" -> runSseMcpServerUsingKtorPlugin(port)
        "--sse-server" -> runSseMcpServerWithPlainConfiguration(port)
        else -> error("Unknown command: $command")
    }
}
