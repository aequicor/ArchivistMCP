package io.aeqiocor.archivistmcp

import java.io.File
import kotlinx.coroutines.runBlocking

fun main(vararg args: String): Unit = runBlocking {
    val command = args.firstOrNull() ?: "--stdio"
    val port = args.getOrNull(1)?.toIntOrNull() ?: throw IllegalArgumentException("Port required")

    val modulesDirsRaw = System.getenv("modules_dirs")
        ?: throw IllegalArgumentException(
            "Env var 'modules_dirs' is required, format: [/docs, /payments/docs]",
        )
    val templatesDir = System.getenv("tmps_dir")
        ?: throw IllegalArgumentException("Env var 'tmps_dir' is required")

    val baseModules = parseModulesDirs(modulesDirsRaw)
    require(baseModules.isNotEmpty()) { "modules_dirs must contain at least one directory" }

    val sharedDirs = System.getenv("shared_modules_dirs")
        ?.let { parseModulesDirs(it).map { m -> m.copy(shared = true) } }
        .orEmpty()

    val hostDirs = System.getenv("HOST_MODULES_DIRS")
        ?.let { parseHostDirs(it) }
        .orEmpty()
    val modules = (baseModules + sharedDirs).mapIndexed { i, m -> m.copy(hostDir = hostDirs.getOrNull(i)) }

    val config = AppConfig(
        modules = modules,
        templatesDir = templatesDir,
        workspaceDirectory = System.getenv("WORKSPACE_DIR") ?: System.getProperty("user.dir") ?: ".",
        chromaUrl = System.getenv("CHROMA_URL") ?: "http://localhost:8000",
    )

    println("Configured modules: ${modules.joinToString(", ") { "${it.name}=${it.dir}" }}")
    println("Templates dir: $templatesDir")

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

private fun parseHostDirs(raw: String): List<String> =
    raw.trim().removePrefix("[").removeSuffix("]")
        .split(",")
        .map { it.trim().trim('"', '\'') }
        .filter { it.isNotEmpty() }

private fun parseModulesDirs(raw: String): List<ModuleConfig> {
    return raw.trim()
        .removePrefix("[").removeSuffix("]")
        .split(",")
        .map { it.trim().trim('"', '\'') }
        .filter { it.isNotEmpty() }
        .map { dir -> ModuleConfig(name = deriveModuleName(dir), dir = dir) }
}

private fun deriveModuleName(dir: String): String {
    val file = File(dir).absoluteFile
    val name = file.name
    if (name.equals("docs", ignoreCase = true) || name.equals("documentation", ignoreCase = true)) {
        val parent = file.parentFile?.name
        if (!parent.isNullOrBlank()) return parent
    }
    return name.takeIf { it.isNotBlank() } ?: "root"
}
