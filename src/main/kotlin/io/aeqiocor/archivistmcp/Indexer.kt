package io.aeqiocor.archivistmcp

import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import java.io.File

data class SearchResult(
    val module: String,
    val filename: String,
    val path: String,
    val score: Double,
)

class Indexer(private val config: AppConfig) {
    private val embeddingModel = AllMiniLmL6V2EmbeddingModel()
    private val embeddingStore: InMemoryEmbeddingStore<TextSegment> = loadOrCreate()

    private val ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(512, 64))
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .build()

    private fun loadOrCreate(): InMemoryEmbeddingStore<TextSegment> {
        val file = File(config.indexPath)
        return if (file.exists()) {
            println("Loading existing index from ${config.indexPath}")
            InMemoryEmbeddingStore.fromFile(file.toPath())
        } else {
            println("Creating new index at ${config.indexPath}")
            InMemoryEmbeddingStore()
        }
    }

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

        persist()
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

        persist()
        println("Document added and indexed: [${module.name}] $relative")
    }

    fun search(module: String?, query: String, maxResults: Int = 5): List<SearchResult> {
        val queryEmbedding = embeddingModel.embed(query).content()
        val candidates = if (module != null) maxResults * 4 else maxResults
        val request = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(candidates)
            .minScore(0.85)
            .build()

        return embeddingStore.search(request).matches()
            .filter { match ->
                module == null || match.embedded().metadata().getString("module") == module
            }
            .take(maxResults)
            .map { match ->
                SearchResult(
                    module = match.embedded().metadata().getString("module") ?: "unknown",
                    filename = match.embedded().metadata().getString("filename") ?: "unknown",
                    path = match.embedded().metadata().getString("path") ?: "",
                    score = match.score(),
                )
            }
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

    private fun persist() {
        val indexFile = File(config.indexPath)
        indexFile.parentFile?.mkdirs()
        embeddingStore.serializeToFile(indexFile.toPath())
    }
}
