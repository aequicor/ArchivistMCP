package io.aeqiocor.archivistmcp

import kotlinx.coroutines.runBlocking

fun main(vararg args: String): Unit = runBlocking {
    val command = args.firstOrNull() ?: "--stdio"
    val port = args.getOrNull(1)?.toIntOrNull() ?: throw IllegalArgumentException()
    val docsDir = args.firstOrNull { it.startsWith("--docs-dir=") }
        ?.substringAfter("=")
        ?: throw IllegalArgumentException("Docs dir required")

    docsDirectory = docsDir



    when (command) {
        "--stdio" -> runMcpServerUsingStdio()
        "--sse-server-ktor" -> runSseMcpServerUsingKtorPlugin(port)
        "--sse-server" -> runSseMcpServerWithPlainConfiguration(port)
        else -> error("Unknown command: $command")
    }
}
