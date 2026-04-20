package io.aeqiocor.archivistmcp

data class ModuleConfig(
    val name: String,
    val dir: String,
    val hostDir: String? = null,
)

data class AppConfig(
    val modules: List<ModuleConfig>,
    val templatesDir: String,
    val workspaceDirectory: String,
    val indexPath: String,
) {
    fun toHostPath(path: String): String {
        for (module in modules) {
            val hostDir = module.hostDir ?: continue
            val prefix = module.dir.trimEnd('/') + "/"
            when {
                path.startsWith(prefix) ->
                    return hostDir.trimEnd('/', '\\') + "/" + path.removePrefix(prefix)
                path.trimEnd('/') == module.dir.trimEnd('/') ->
                    return hostDir.trimEnd('/', '\\')
            }
        }
        return path
    }
}
