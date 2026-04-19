package io.aeqiocor.archivistmcp

import kotlinx.coroutines.runBlocking

fun main(vararg args: String): Unit = runBlocking {
    val command = args.firstOrNull() ?: "--stdio"
    val port = args.getOrNull(1)?.toIntOrNull() ?: throw IllegalArgumentException("Port required")
    val docsDir = args.firstOrNull { it.startsWith("--docs-dir=") }
        ?.substringAfter("=")
        ?: throw IllegalArgumentException("Docs dir required")

    val config = AppConfig(
        docsDirectory = docsDir,
        workspaceDirectory = System.getenv("WORKSPACE_DIR") ?: System.getProperty("user.dir") ?: ".",
        indexPath = System.getenv("INDEX_PATH") ?: "$docsDir/index/embeddings.json",
    )

    val indexer = Indexer(config)
    try {
        indexer.indexDocuments()
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    when (command) {
        "--stdio" -> runMcpServerUsingStdio(config, indexer)
        "--sse-server-ktor" -> runSseMcpServerUsingKtorPlugin(port, config, indexer)
        "--sse-server" -> runSseMcpServerWithPlainConfiguration(port, config, indexer)
        else -> error("Unknown command: $command")
    }
}
