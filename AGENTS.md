# ArchivistMCP - AI Agent Configuration

## Project Overview

**ArchivistMCP** — Kotlin MCP server for document indexing using ChromaDB for semantic search.

- **Version**: 0.1.0
- **Type**: Model Context Protocol Server
- **Transport**: STDIO + SSE
- **Purpose**: Semantic indexing and search of documents (PDF, TXT, MD)

---

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.2.21 |
| Ktor | 3.3.3 |
| MCP Kotlin SDK | 0.11.1 |
| ChromaDB | (embedded) |
| Gradle | 9.4.1 |

---

## Architecture

```
src/main/kotlin/io/modelcontextprotocol/sample/server/
├── server.kt           # Main MCP server
├── main.kt           # Entry point
├── chroma/           # ChromaDB client
├── documents/        # Document parsing
├── indexing/         # Indexing logic
└── tools/           # MCP tools
```

---

## General Rules

1. **Always use Gradle** for building and dependency management (`./gradlew`)
2. **Kotlin idioms**: use `sealed class` for result types, `object` for singletons
3. **Coroutines**: use `kotlinx.coroutines`, avoid blocking calls in coroutines
4. **MCP tools**: use `snake_case` (e.g., `index_document`), return typed results
5. **Transport**: STDIO — for local agents, SSE — for remote connections
6. **ChromaDB**: embedded mode, storage in `./data/chromadb`

---

## Kotlin Rules

### Result Typing

```kotlin
// Correct - sealed class for Result
sealed class IndexResult {
    data class Success(val documentId: String) : IndexResult()
    data class Error(val message: String) : IndexResult()
}

// Avoid
fun indexDocument(path: String): Map<String, Any>
```

### Coroutines

```kotlin
// Correct - suspend functions with proper dispatcher
suspend fun indexDocument(path: String): IndexResult =
    withContext(Dispatchers.IO) { ... }

// Avoid - blocking calls in coroutines
fun indexDocument(path: String): IndexResult { ... }
```

### Null Safety

```kotlin
// Use safe calls and elvis operators
val content = document.content ?: return IndexResult.Error("Empty document")

// Avoid
val content = document.content!! // NPE risk
```

### Naming Conventions

- Functions: camelCase (`indexDocument`, `queryDocuments`)
- Classes: PascalCase (`IndexResult`, `ChromaClient`)
- MCP Tools: snake_case (`index_document`, `query_documents`)
- Constants: UPPER_SNAKE_CASE

---

## MCP Tooling Rules

### Tool Definition

- Use `snake_case` for tool names
- Always add description for each tool
- Type arguments through data classes

```kotlin
server.addTool(
    name = "index_document",
    description = "Indexes a document for semantic search"
) { request ->
    // request is a typed data class
}
```

### Result Types

- Use `CallToolResult` with `TextContent` for return values
- Structure JSON responses for readability

```kotlin
CallToolResult(
    content = listOf(TextContent("""{"documentId": "abc123", "chunks": 5}""))
)
```

### Transport Types

**STDIO** - For local AI agents:
```kotlin
fun runMcpServerUsingStdio() { ... }
```

**SSE** - For remote HTTP connections:
```kotlin
fun runSseMcpServerUsingKtorPlugin(port: Int) { ... }
```

### Server Capabilities

```kotlin
ServerOptions(
    capabilities = ServerCapabilities(
        prompts = ServerCapabilities.Prompts(listChanged = true),
        resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
        tools = ServerCapabilities.Tools(listChanged = true),
    ),
)
```

---

## ChromaDB Integration

### Connected Clients

```kotlin
// Embedded ChromaDB client
val client = ChromaClient.builder()
    .persistDirectory("./data/chromadb")
    .build()
```

### Collections

- Create collection with name and metadata
- Add documents with embeddings
- Query with semantic search

```kotlin
val collection = client.getOrCreateCollection("documents")
collection.add(documents)
val results = collection.query(queryEmbeddings, nResults = 5)
```

### Storage

- Default path: `./data/chromadb`
- Can be configured for custom paths

---

## Document Processing

| Format | Library | Status |
|--------|---------|--------|
| PDF | Apache PDFBox | Planned |
| Markdown | kotlinx-markdown | Planned |
| Plain Text | Kotlin stdlib | Planned |

### Parser Interface

```kotlin
interface DocumentParser {
    suspend fun parse(path: String): ParsedDocument
    fun supports(path: String): Boolean
}
```

---

## Testing

- Use JUnit 5 via `kotlin("test")`
- Tests in `src/test/kotlin/`
- Integration tests: `SseServerIntegrationTest.kt`

### Test Structure

```kotlin
class SseServerIntegrationTest {
    @Test
    fun `server starts on specified port`() { ... }
}
```

---

## Commands

```bash
./gradlew build        # Build project
./gradlew run          # Run server (STDIO mode)
./gradlew run --args="--sse-server-ktor 8080"  # SSE on port 8080
./gradlew test         # Run tests
./gradlew shadowJar    # Create uber-jar
```

---

## File Organization

```
src/main/kotlin/io/modelcontextprotocol/sample/server/
├── server.kt                    # Server configuration
├── main.kt                      # Entry point
├── chroma/
│   ├── ChromaClient.kt         # ChromaDB client wrapper
│   └── ChromaCollection.kt      # Collection operations
├── documents/
│   ├── DocumentParser.kt       # Base interface
│   ├── PdfParser.kt           # PDF parsing
│   ├── TextParser.kt          # Plain text parsing
│   └── MarkdownParser.kt     # Markdown parsing
├── indexing/
│   ├── DocumentIndexer.kt   # Indexing logic
│   └── EmbeddingGenerator.kt  # Embedding generation
└── tools/
    ├── IndexDocumentTool.kt  # index_document tool
    ├── QueryDocumentsTool.kt # query_documents tool
    └── ListDocumentsTool.kt # list_documents tool
```

---

## Dependencies Management

All dependencies in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.2.21"
ktor = "3.3.3"
mcp-kotlin = "0.11.1"

[libraries]
mcp-kotlin-server = { group = "io.modelcontextprotocol", name = "kotlin-sdk-server", version.ref = "mcp-kotlin" }
ktor-server-cio = { group = "io.ktor", name = "ktor-server-cio" }
```

For updates, modify version references. Avoid hardcoding versions in `build.gradle.kts`.

---

## Error Handling

### Result Pattern

```kotlin
sealed class IndexResult {
    data class Success(val documentId: String, val chunks: Int) : IndexResult()
    data class Error(val message: String, val cause: Throwable? = null) : IndexResult()
}
```

### Logging

Use SLF4J for logging:

```kotlin
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger(MyClass::class.java)
logger.info("Document indexed: {}", documentId)
logger.error("Failed to index: {}", path, exception)
```

---

## Best Practices Summary

1. **Gradle first** — always use Gradle for build
2. **Sealed classes** — for typed results
3. **Suspend with IO** — use Dispatchers.IO for blocking ops
4. **Safe calls** — avoid null with `?.` and `?:`
5. **snake_case tools** — MCP naming convention
6. **Description everywhere** —文档 tools have descriptions
7. **JSON structure** — structured tool responses
8. **Tests required** — JUnit 5 for testing