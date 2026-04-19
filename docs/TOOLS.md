# ArchivistMCP Tools Documentation

This document provides comprehensive Kotlin documentation for all MCP tools in the ArchivistMCP project.

## Table of Contents

1. [McpTool Interface](#mcptool-interface)
2. [AddDocumentTool](#adddocumenttool)
3. [SemanticSearchTool](#semanticsearchtool)
4. [SmartSearchTool](#smartsearchtool)

---

## McpTool Interface

**Location:** `src/main/kotlin/io/aeqiocor/archivistmcp/tool/McpTool.kt`

### Overview

The `McpTool` interface is the base contract for all tools in the ArchivistMCP system. It defines the lifecycle for registering tools with the MCP (Model Context Protocol) server.

### KDoc

```kotlin
/**
 * Base interface for MCP tools.
 *
 * All tools must implement this interface to be registered with the MCP server.
 * This provides a standardized way for tools to define their capabilities and
 * behavior when invoked through the Model Context Protocol.
 *
 * @see io.modelcontextprotocol.kotlin.sdk.server.Server
 */
interface McpTool {
    /**
     * Registers this tool with the MCP server.
     *
     * This method is called during server initialization to register the tool
     * and make it available for invocation by clients. Implementations should
     * use [Server.addTool] to register tool functionality, including:
     * - Tool name and description
     * - Input schema (parameter definitions)
     * - Tool handler logic
     *
     * @param server The MCP server instance to register with
     */
    fun register(server: Server)
}
```

### Implementation Notes

- Implementations should be lightweight and focus on their specific functionality
- Tools are registered during server startup before accepting client requests
- All registered tools are available to MCP clients for invocation

---

## AddDocumentTool

**Location:** `src/main/kotlin/io/aeqiocor/archivistmcp/tool/AddDocumentTool.kt`

### Overview

The `AddDocumentTool` allows users to add new markdown documents to the docs directory and automatically index them for semantic search. This tool is essential for expanding the knowledge base dynamically.

### KDoc

```kotlin
/**
 * MCP tool for adding and indexing new markdown documents.
 *
 * This tool enables dynamic document management by allowing new markdown files
 * to be created in the documentation directory and automatically indexed for
 * semantic search. Documents can be organized in subdirectories.
 *
 * @property indexer The [Indexer] instance used to index documents
 *
 * @see Indexer.addDocument
 * @see McpTool
 */
class AddDocumentTool(private val indexer: Indexer) : McpTool {
    /**
     * Registers the add_document tool with the MCP server.
     *
     * ## Tool Details
     * - **Name:** add_document
     * - **Description:** Add a new markdown document to the docs directory and index it for semantic search
     * - **Input Parameters:**
     *   - `filename` (string, required): Filename with .md extension (e.g., 'guide.md' or 'subdir/notes.md')
     *   - `content` (string, required): Markdown content of the document
     *
     * ## Response Format
     * On success:
     * ```json
     * {"status": "ok", "filename": "guide.md"}
     * ```
     *
     * On error:
     * ```json
     * {"error": "filename and content are required"}
     * ```
     *
     * ## Usage Example
     * ```kotlin
     * // The tool is called with these parameters:
     * // filename: "api-guide.md"
     * // content: "# API Guide\n\nIntroduction to the API..."
     * // Returns: {"status": "ok", "filename": "api-guide.md"}
     * ```
     *
     * ## Implementation Notes
     * - Requires both `filename` and `content` parameters
     * - Returns an error if either parameter is missing or null
     * - Delegates indexing to the [Indexer] for semantic search capability
     * - File extension .md is required but not validated
     * - Subdirectory paths are supported (e.g., 'subdir/filename.md')
     *
     * @param server The MCP server instance to register with
     */
    override fun register(server: Server)
}
```

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `filename` | String | Yes | Markdown filename with .md extension. Supports subdirectories (e.g., 'guides/setup.md') |
| `content` | String | Yes | Complete markdown content of the document |

### Return Format

**Success Response:**
```json
{
  "status": "ok",
  "filename": "guide.md"
}
```

**Error Response:**
```json
{
  "error": "filename and content are required"
}
```

### Usage Examples

```kotlin
// Add a simple document
// Input:
// - filename: "tutorial.md"
// - content: "# Tutorial\n\nStep 1: ..."
// Output: {"status": "ok", "filename": "tutorial.md"}

// Add document in subdirectory
// Input:
// - filename: "guides/api-reference.md"
// - content: "# API Reference\n\n## Endpoints..."
// Output: {"status": "ok", "filename": "guides/api-reference.md"}
```

### Implementation Details

- **Input Validation:** Checks for null/empty filename and content
- **Error Handling:** Returns error JSON if validation fails
- **Indexing:** Delegates to `indexer.addDocument()` for semantic indexing
- **File Creation:** Physical file creation handled by the Indexer
- **Path Support:** Supports nested directory structures in filename

---

## SemanticSearchTool

**Location:** `src/main/kotlin/io/aeqiocor/archivistmcp/tool/SemanticSearchTool.kt`

### Overview

The `SemanticSearchTool` performs semantic (vector-based) search on indexed documents. It's superior to keyword search for natural language queries as it understands meaning rather than just matching words.

### KDoc

```kotlin
/**
 * MCP tool for semantic document search.
 *
 * This tool leverages semantic similarity (vector search) to find relevant documents
 * based on meaning rather than keyword matching. It provides better results for
 * natural language queries compared to traditional keyword-based search.
 *
 * The search uses embeddings to understand the semantic meaning of the query
 * and matches it against document embeddings in the index.
 *
 * @property indexer The [Indexer] instance used for semantic search
 *
 * @see Indexer.search
 * @see McpTool
 */
class SemanticSearchTool(private val indexer: Indexer) : McpTool {
    /**
     * Registers the semantic_search tool with the MCP server.
     *
     * ## Tool Details
     * - **Name:** semantic_search
     * - **Description:** Search documents using semantic similarity (vector search). 
     *   Better than keyword search for natural language queries.
     * - **Input Parameters:**
     *   - `query` (string, required): Natural language search query
     *
     * ## Response Format
     * ```json
     * {
     *   "query": "how to setup authentication",
     *   "results": [
     *     {"filename": "auth-guide.md", "score": 0.892},
     *     {"filename": "security.md", "score": 0.756},
     *     {"filename": "user-management.md", "score": 0.683}
     *   ]
     * }
     * ```
     *
     * ## Score Explanation
     * - Scores range from 0.0 to 1.0
     * - Higher scores indicate better semantic similarity
     * - Scores are formatted to 3 decimal places
     * - Results are ordered by relevance (highest scores first)
     *
     * ## Usage Example
     * ```kotlin
     * // Query: "authentication and authorization"
     * // Returns documents semantically similar to authentication/authorization
     * // Results sorted by relevance score
     * ```
     *
     * ## Implementation Notes
     * - Returns error if query parameter is missing or null
     * - Always returns a list (may be empty if no matches)
     * - Uses vector embeddings for similarity calculation
     * - Results are sorted by relevance score (highest first)
     * - Query text is preserved in response for context
     *
     * @param server The MCP server instance to register with
     */
    override fun register(server: Server)
}
```

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | String | Yes | Natural language search query |

### Return Format

```json
{
  "query": "how to use the API",
  "results": [
    {
      "filename": "api-guide.md",
      "score": 0.892
    },
    {
      "filename": "quick-start.md",
      "score": 0.756
    }
  ]
}
```

### Score Interpretation

| Score Range | Meaning |
|-------------|---------|
| 0.9 - 1.0  | Highly relevant, very similar to query |
| 0.7 - 0.9  | Relevant, semantically related |
| 0.5 - 0.7  | Somewhat relevant, related content |
| 0.0 - 0.5  | Low relevance, weakly related |

### Usage Examples

```kotlin
// Natural language query for authentication
// Input: "How do I authenticate users?"
// Output: Returns documents about authentication, user login, JWT, OAuth, etc.

// Conceptual query
// Input: "Database configuration and connection pooling"
// Output: Returns docs about databases, configuration, connection management

// Multi-concept query
// Input: "Setting up CI/CD pipeline with GitHub actions"
// Output: Returns docs about CI/CD, GitHub, automation, etc.
```

### Implementation Details

- **Empty Result Handling:** Returns empty array if no matches found
- **Score Formatting:** All scores formatted to 3 decimal places (e.g., 0.892)
- **Result Ordering:** Results automatically sorted by relevance score (highest first)
- **Null Handling:** Returns error JSON if query is null or empty
- **Vector-based Matching:** Uses embeddings for semantic similarity

---

## SmartSearchTool

**Location:** `src/main/kotlin/io/aeqiocor/archivistmcp/tool/SmartSearchTool.kt`

### Overview

The `SmartSearchTool` is an intelligent search tool that combines semantic search with a helpful fallback mechanism. When documents are not found, it returns a documentation template and instructs users to add the missing content.

### KDoc

```kotlin
/**
 * MCP tool for intelligent semantic search with documentation template fallback.
 *
 * This tool performs semantic search on documents and provides a helpful fallback
 * mechanism when no results are found. If documents matching the query don't exist,
 * it returns a markdown template and instructs the user to create and index new
 * documentation using the [AddDocumentTool].
 *
 * This creates a self-improving knowledge base where missing documentation can be
 * identified and added on-demand.
 *
 * @property indexer The [Indexer] instance used for semantic search
 * @property template Lazily-loaded markdown template for new documents
 *
 * @see Indexer.search
 * @see AddDocumentTool
 * @see McpTool
 */
class SmartSearchTool(private val indexer: Indexer) : McpTool {
    /**
     * Registers the smart_search tool with the MCP server.
     *
     * ## Tool Details
     * - **Name:** smart_search
     * - **Description:** Search documents by semantic similarity. If nothing is found,
     *   returns a documentation template and instructs to call add_document to create
     *   and index a new document.
     * - **Input Parameters:**
     *   - `query` (string, required): Natural language search query
     *
     * ## Response Format - Documents Found
     * ```json
     * {
     *   "status": "found",
     *   "query": "authentication guide",
     *   "results": [
     *     {"filename": "auth-guide.md", "score": 0.892},
     *     {"filename": "user-mgmt.md", "score": 0.756}
     *   ]
     * }
     * ```
     *
     * ## Response Format - Documents Not Found
     * ```json
     * {
     *   "status": "not_found",
     *   "query": "authentication guide",
     *   "action": "add_document",
     *   "suggested_filename": "authentication-guide.md",
     *   "template": "# Authentication Guide\n\n## Overview\n..."
     * }
     * ```
     *
     * ## Filename Slug Generation
     * When documents are not found, a filename slug is automatically generated:
     * - Lowercase the query
     * - Replace non-alphanumeric characters with hyphens
     * - Trim leading/trailing hyphens
     * - Append .md extension
     *
     * Examples:
     * - "How to authenticate?" → "how-to-authenticate.md"
     * - "Database Setup Guide" → "database-setup-guide.md"
     * - "CI/CD Best Practices" → "cicd-best-practices.md"
     *
     * ## Template Loading
     * The documentation template is loaded from the classpath resource:
     * `classpath:doc-template.md`
     * - Lazy-loaded on first use for performance
     * - Cached for subsequent uses
     * - Falls back to empty string if not found
     *
     * ## Usage Example - Found Case
     * ```kotlin
     * // Query: "REST API documentation"
     * // Returns: Existing documents about REST APIs with relevance scores
     * ```
     *
     * ## Usage Example - Not Found Case
     * ```kotlin
     * // Query: "GraphQL implementation guide"
     * // Returns: Template for creating "graphql-implementation-guide.md"
     *           with instruction to use add_document tool
     * ```
     *
     * ## Implementation Notes
     * - Returns error if query parameter is missing or null
     * - Status is "found" or "not_found" depending on results
     * - Template is lazily loaded and cached for performance
     * - JSON string escaping: backslashes, quotes, newlines
     * - Suggests filename automatically for user convenience
     * - Encourages self-improving documentation pattern
     *
     * @param server The MCP server instance to register with
     */
    override fun register(server: Server)
}
```

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | String | Yes | Natural language search query |

### Return Format - Documents Found

```json
{
  "status": "found",
  "query": "REST API authentication",
  "results": [
    {
      "filename": "api-auth.md",
      "score": 0.934
    },
    {
      "filename": "security-best-practices.md",
      "score": 0.812
    }
  ]
}
```

### Return Format - Documents Not Found

```json
{
  "status": "not_found",
  "query": "REST API authentication",
  "action": "add_document",
  "suggested_filename": "rest-api-authentication.md",
  "template": "# REST API Authentication\n\n## Overview\n..."
}
```

### Filename Slug Generation Algorithm

The tool automatically generates filenames by:
1. Converting query to lowercase
2. Replacing non-alphanumeric characters with hyphens
3. Trimming leading/trailing hyphens
4. Appending `.md` extension

| Input Query | Generated Filename |
|-------------|-------------------|
| "How to authenticate users?" | `how-to-authenticate-users.md` |
| "Database Setup & Configuration" | `database-setup-configuration.md` |
| "API/REST Best Practices!!!" | `apirest-best-practices.md` |

### Template Handling

- **Location:** `classpath:doc-template.md`
- **Loading:** Lazy-loaded on first use for performance
- **Caching:** Result is cached for subsequent requests
- **Fallback:** Empty string if template file not found
- **Escaping:** JSON-safe escaping (backslashes, quotes, newlines)

### Usage Examples

```kotlin
// Case 1: Found documents
// Input: "authentication mechanisms"
// Output: 
// {
//   "status": "found",
//   "query": "authentication mechanisms",
//   "results": [...]
// }

// Case 2: No documents found
// Input: "quantum computing explained"
// Output:
// {
//   "status": "not_found",
//   "query": "quantum computing explained",
//   "action": "add_document",
//   "suggested_filename": "quantum-computing-explained.md",
//   "template": "..."
// }
```

### Self-Improving Knowledge Base Pattern

SmartSearchTool implements a self-improving documentation pattern:

1. **User asks a question** (via smart_search)
2. **Tool checks if documentation exists**
   - If yes: Returns relevant documents
   - If no: Proceeds to step 3
3. **Tool provides template** with suggested filename
4. **User creates documentation** using AddDocumentTool
5. **Future queries** find the newly created documentation

This pattern encourages organic growth of the knowledge base based on actual user needs.

### Implementation Details

- **Error Handling:** Returns error JSON if query is null/empty
- **Case Sensitivity:** All operations are case-insensitive
- **Special Characters:** Replaced with hyphens in filenames
- **Template Caching:** Loaded once and reused
- **JSON Escaping:** Proper escaping for JSON string serialization
- **Result Ordering:** Results sorted by relevance score (highest first)

---

## Common Patterns and Best Practices

### Error Handling

All tools follow a consistent error handling pattern:

```kotlin
if (query == null) {
    CallToolResult(
        content = listOf(TextContent("""{"error": "query is required"}""")),
        isError = true,
    )
}
```

### JSON Response Format

Tools return JSON as strings within `TextContent`:

```kotlin
CallToolResult(
    content = listOf(TextContent(jsonString)),
    isError = false // or true for errors
)
```

### Tool Registration

All tools register with the server using `server.addTool()`:

```kotlin
server.addTool(
    name = "tool_name",
    description = "Tool description",
    inputSchema = ToolSchema(...),
) { request ->
    // Tool implementation
}
```

---

## Integration with Indexer

All tools depend on the `Indexer` for document management:

| Tool | Indexer Method | Purpose |
|------|----------------|---------|
| AddDocumentTool | `indexer.addDocument(filename, content)` | Store and index new documents |
| SemanticSearchTool | `indexer.search(query)` | Retrieve documents by semantic similarity |
| SmartSearchTool | `indexer.search(query)` | Retrieve documents with fallback template |

---

## Tool Lifecycle

```
Server Startup
    ↓
Tool Registration
    ├─ McpTool.register(server)
    ├─ AddDocumentTool.register()
    ├─ SemanticSearchTool.register()
    └─ SmartSearchTool.register()
    ↓
Client Requests
    ├─ Tool invocation with parameters
    ├─ Input validation
    ├─ Business logic execution
    └─ Response formatting
```

---

## Quick Reference

| Tool | Purpose | Best For |
|------|---------|----------|
| **AddDocumentTool** | Create new indexed documents | Adding documentation to knowledge base |
| **SemanticSearchTool** | Search by semantic similarity | Finding relevant documents by meaning |
| **SmartSearchTool** | Search with template fallback | Intelligent search with knowledge base growth |

