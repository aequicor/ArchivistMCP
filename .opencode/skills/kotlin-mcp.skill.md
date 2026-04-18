# Kotlin MCP Skill

> **Version**: 1.0
> **Status**: Ready to use

Skill for developing MCP server in Kotlin with ChromaDB for document indexing.

## Quick Start

```bash
./gradlew build    # Build project
./gradlew run      # Run server
./gradlew test     # Run tests
```

## Skills

### kotlin-mcp-core

Typical MCP SDK operations.

- Creating tools via `server.addTool()`
- Adding resources via `server.addResource()`
- Generating prompts via `server.addPrompt()`

### kotlin-chroma

ChromaDB operations.

- Connecting to embedded ChromaDB
- Creating collections
- Adding documents with embeddings
- Semantic search

### kotlin-ktor

Ktor HTTP server.

- STDIO transport
- SSE transport
- CORS configuration

## Architecture

```
src/main/kotlin/io/modelcontextprotocol/sample/server/
├── server.kt         # MCP server configuration
├── main.kt           # Entry point
├── chroma/          # ChromaDB client
│   └── ChromaClient.kt
├── documents/        # Document parsing
│   ├── DocumentParser.kt
│   ├── PdfParser.kt
│   └── TextParser.kt
├── indexing/        # Indexing logic
│   └── DocumentIndexer.kt
└── tools/            # MCP tools
    ├── IndexDocumentTool.kt
    ├── QueryDocumentsTool.kt
    └── ListDocumentsTool.kt
```

## Agent Commands

```
@gen-tool [name]    Generates MCP tool
@gen-parser [type]  Generates parser (pdf, txt, md)
@index              Indexes document
@query              Performs semantic search
```

## Best Practices

1. **Results** — use sealed class for typing
2. **Coroutines** — suspend functions with Dispatchers.IO
3. **Null Safety** — safe calls and elvis operators
4. **Tool Naming** — snake_case (index_document, query_documents)
5. **Description** — always add description to tools

## Links

- [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk)
- [Ktor Documentation](https://ktor.io)
- [ChromaDB](https://docs.trychroma.com)