import io.ktor.server.engine.EmbeddedServer
import io.aeqiocor.archivistmcp.AppConfig
import io.aeqiocor.archivistmcp.Indexer
import io.aeqiocor.archivistmcp.ModuleConfig
import io.aeqiocor.archivistmcp.runSseMcpServerUsingKtorPlugin
import io.aeqiocor.archivistmcp.runSseMcpServerWithPlainConfiguration
import java.io.File

private val tmpDir: String = System.getProperty("java.io.tmpdir")

private val testConfig = AppConfig(
    modules = listOf(ModuleConfig(name = "test", dir = tmpDir)),
    templatesDir = tmpDir,
    workspaceDirectory = System.getProperty("user.dir"),
    indexPath = File(tmpDir, "test-embeddings.json").path,
)
private val testIndexer = Indexer(testConfig)

enum class McpServerType(
    val sseEndpoint: String,
    val serverFactory: (port: Int) -> EmbeddedServer<*, *>
) {
    KTOR_PLUGIN(
        sseEndpoint = "",
        serverFactory = { port -> runSseMcpServerUsingKtorPlugin(port, testConfig, testIndexer, wait = false) }
    ),
    PLAIN_CONFIGURATION(
        sseEndpoint = "/sse",
        serverFactory = { port -> runSseMcpServerWithPlainConfiguration(port, testConfig, testIndexer, wait = false) }
    )
}
