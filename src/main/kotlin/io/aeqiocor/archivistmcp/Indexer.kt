package io.aeqiocor.archivistmcp

import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey
import dev.langchain4j.store.embedding.filter.Filter
import java.io.File

data class SearchResult(
    val module: String,
    val filename: String,
    val path: String,
    val score: Double,
)

class Indexer(private val config: AppConfig) {
    private val embeddingModel = AllMiniLmL6V2EmbeddingModel()
    private val embeddingStore = ChromaEmbeddingStore.builder()
        .baseUrl(config.chromaUrl)
        .collectionName("archivist")
        .build()

    private val ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(512, 64))
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .build()

    fun indexDocuments() {
        config.modules.forEach { module ->
            val dir = File(module.dir)
            if (!dir.exists() || !dir.isDirectory) {
                println("Skipping module '${module.name}': directory not found (${module.dir})")
                return@forEach
            }

            val files = dir.walkTopDown()
                .filter { it.isFile && (it.extension == "md" || it.extension == "markdown") }
                .toList()

            println("Indexing ${files.size} documents for module '${module.name}'...")
            files.forEach { file ->
                val doc = Document.from(
                    file.readText(),
                    Metadata.from(
                        mapOf(
                            "module" to module.name,
                            "filename" to file.relativeTo(dir).path,
                            "path" to file.absolutePath,
                        ),
                    ),
                )
                ingestor.ingest(doc)
                println("Indexed: [${module.name}] ${file.name}")
            }
        }
    }

    fun addDocument(path: String, content: String) {
        val file = File(path).absoluteFile
        val module = findModuleForPath(file)
            ?: throw IllegalArgumentException(
                "Path '$path' is not within any configured module directory: " +
                    config.modules.joinToString(", ") { "${it.name}=${it.dir}" },
            )

        file.parentFile?.mkdirs()
        file.writeText(content)

        val moduleDir = File(module.dir).absoluteFile
        val relative = file.relativeTo(moduleDir).path

        val doc = Document.from(
            content,
            Metadata.from(
                mapOf(
                    "module" to module.name,
                    "filename" to relative,
                    "path" to file.absolutePath,
                ),
            ),
        )
        ingestor.ingest(doc)
        println("Document added and indexed: [${module.name}] $relative")
    }

    fun search(module: String?, query: String, maxResults: Int = 5): List<SearchResult> {
        val queryEmbedding = embeddingModel.embed(query).content()
        val requestBuilder = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .minScore(0.85)

        if (module != null) {
            val sharedModules = config.modules.filter { it.shared }.map { it.name }
            val modulesToSearch = (listOf(module) + sharedModules).distinct()
            requestBuilder.filter(metadataKey("module").isIn(modulesToSearch))
        }

        return embeddingStore.search(requestBuilder.build()).matches()
            .map { match ->
                SearchResult(
                    module = match.embedded().metadata().getString("module") ?: "unknown",
                    filename = match.embedded().metadata().getString("filename") ?: "unknown",
                    path = match.embedded().metadata().getString("path") ?: "",
                    score = match.score(),
                )
            }
    }

    fun getDocument(nameOrPath: String): File? {
        if (nameOrPath.startsWith("/")) {
            val file = File(nameOrPath)
            if (file.exists() && config.modules.any { nameOrPath.startsWith(it.dir) }) return file
            return null
        }
        return config.modules
            .asSequence()
            .flatMap { File(it.dir).walkTopDown() }
            .firstOrNull { it.isFile && (it.name == nameOrPath || it.nameWithoutExtension == nameOrPath) }
    }

    fun modules(): List<String> = config.modules.map { it.name }

    private fun findModuleForPath(file: File): ModuleConfig? {
        val targetCanonical = try {
            file.canonicalPath
        } catch (_: Exception) {
            file.absolutePath
        }
        return config.modules.firstOrNull { module ->
            val moduleCanonical = try {
                File(module.dir).canonicalPath
            } catch (_: Exception) {
                File(module.dir).absolutePath
            }
            targetCanonical == moduleCanonical ||
                targetCanonical.startsWith(moduleCanonical + File.separator)
        }
    }
}
