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
        val dir = File(config.docsDirectory)
        if (!dir.exists() || !dir.isDirectory) return

        val files = dir.walkTopDown()
            .filter { it.isFile && (it.extension == "md" || it.extension == "markdown") }
            .toList()

        println("Indexing ${files.size} documents...")
        files.forEach { file ->
            val doc = Document.from(
                file.readText(),
                Metadata.from("filename", file.relativeTo(dir).path),
            )
            ingestor.ingest(doc)
            println("Indexed: ${file.name}")
        }

        val indexFile = File(config.indexPath)
        indexFile.parentFile?.mkdirs()
        embeddingStore.serializeToFile(indexFile.toPath())
        println("Index saved to ${config.indexPath}")
    }

    fun search(query: String, maxResults: Int = 5): List<Pair<String, Double>> {
        val queryEmbedding = embeddingModel.embed(query).content()
        val request = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .minScore(0.5)
            .build()

        return embeddingStore.search(request).matches().map { match ->
            val filename = match.embedded().metadata().getString("filename") ?: "unknown"
            filename to match.score()
        }
    }
}
