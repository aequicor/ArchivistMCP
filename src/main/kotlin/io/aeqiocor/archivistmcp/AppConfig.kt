package io.aeqiocor.archivistmcp

data class ModuleConfig(
    val name: String,
    val dir: String,
)

data class AppConfig(
    val modules: List<ModuleConfig>,
    val templatesDir: String,
    val workspaceDirectory: String,
    val indexPath: String,
)
