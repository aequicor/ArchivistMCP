# SemanticSearchTool Documentation

## Overview

The `SemanticSearchTool` performs semantic (vector-based) search on indexed documents. Unlike keyword-based search, it understands the meaning of queries and matches them against the semantic meaning of documents, providing more relevant results for natural language queries.

## Location

`src/main/kotlin/io/aeqiocor/archivistmcp/tool/SemanticSearchTool.kt`

## KDoc

```kotlin
/**
 * MCP tool for semantic document search.
 *
 * This tool leverages semantic similarity (vector search) to find relevant documents
 * based on meaning rather than keyword matching. It provides superior results for
 * natural language queries compared to traditional keyword-based search.
 *
 * The search uses embedding vectors to understand the semantic meaning of the query
 * and matches it against document embeddings stored in the index. This allows finding
 * relevant documents even when exact keywords don't match.
 *
 * ## How Semantic Search Works
 *
 * 1. Query is converted to an embedding vector using a language model
 * 2. Embedding vectors for indexed documents are retrieved from index
 * 3. Cosine similarity is calculated between query vector and document vectors
 * 4. Documents are ranked by similarity score (0.0 to 1.0)
 * 5. Results are returned sorted by relevance
 *
 * ## Comparison: Semantic vs Keyword Search
 *
 * | Aspect | Semantic | Keyword |
 * |--------|----------|---------|
 * | Matching | Meaning-based | Exact text match |
 * | Synonyms | Handled automatically | Requires manual queries |
 * | Phrase Order | Doesn't matter | Matters |
 * | Typos | Tolerant | Fails |
 * | Context | Understands context | Only matches words |
 *
 * ## Dependencies
 * - [Indexer]: Handles embedding generation and vector similarity search
 *
 * ## MCP Tool Details
 * - **Name:** semantic_search
 * - **MCP Type:** Tool
 * - **Availability:** Available to all MCP clients
 *
 * @property indexer The [Indexer] instance responsible for vector search
 *
 * @see Indexer.search
 * @see SmartSearchTool
 * @see McpTool
 */
class SemanticSearchTool(private val indexer: Indexer) : McpTool {
    /**
     * Registers the semantic_search tool with the MCP server.
     *
     * This tool enables searching the knowledge base using semantic similarity.
     * It's designed for natural language queries and provides excellent results
     * for conceptual searches.
     *
     * ## Tool Specification
     *
     * | Attribute | Value |
     * |-----------|-------|
     * | Name | `semantic_search` |
     * | Description | Search documents using semantic similarity (vector search). Better than keyword search for natural language queries. |
     * | Input Type | JSON Object |
     * | Output Type | JSON Object with Results Array |
     *
     * ## Input Parameters
     *
     * ### query (required, string)
     * - Natural language search query
     * - Can be a phrase, sentence, or paragraph
     * - Length: 1 character to several hundred words
     * - Supports any language supported by the embedding model
     * - Examples:
     *   - "How do I authenticate users?"
     *   - "database connection pooling"
     *   - "best practices for error handling"
     *
     * ## Input Schema
     *
     * ```json
     * {
     *   "type": "object",
     *   "properties": {
     *     "query": {
     *       "type": "string",
     *       "description": "Natural language search query"
     *     }
     *   },
     *   "required": ["query"]
     * }
     * ```
     *
     * ## Response Format
     *
     * ### Success Response (With Results)
     * ```json
     * {
     *   "query": "how to authenticate users",
     *   "results": [
     *     {"filename": "auth-guide.md", "score": 0.892},
     *     {"filename": "security.md", "score": 0.756},
     *     {"filename": "user-management.md", "score": 0.683}
     *   ]
     * }
     * ```
     *
     * ### Success Response (No Results)
     * ```json
     * {
     *   "query": "obscure topic",
     *   "results": []
     * }
     * ```
     *
     * **Fields:**
     * - `query`: The search query as provided
     * - `results`: Array of matching documents
     *   - `filename`: Path to the document file
     *   - `score`: Similarity score (0.0 to 1.0)
     *
     * ### Error Response
     * ```json
     * {
     *   "error": "query is required"
     * }
     * ```
     *
     * **Error Conditions:**
     * - `query` is null or missing
     *
     * ## Similarity Score Interpretation
     *
     * The score represents how semantically similar the document is to the query.
     * It's derived from cosine similarity between embedding vectors.
     *
     * | Score Range | Meaning | Relevance |
     * |-------------|---------|-----------|
     * | 0.9 - 1.0 | Highly similar | Highly relevant, exact match |
     * | 0.8 - 0.9 | Very similar | Very relevant |
     * | 0.7 - 0.8 | Similar | Quite relevant |
     * | 0.6 - 0.7 | Moderately similar | Somewhat relevant |
     * | 0.5 - 0.6 | Weakly similar | Weakly relevant |
     * | 0.0 - 0.5 | Dissimilar | Low relevance |
     *
     * **Note:** Exact score thresholds depend on the embedding model used.
     *
     * ## Usage Examples
     *
     * ### Example 1: Specific Technical Query
     * **Request:**
     * ```json
     * {
     *   "query": "How do I implement JWT authentication in Kotlin?"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "query": "How do I implement JWT authentication in Kotlin?",
     *   "results": [
     *     {"filename": "auth/jwt-guide.md", "score": 0.956},
     *     {"filename": "auth/security-best-practices.md", "score": 0.834},
     *     {"filename": "auth/user-session-management.md", "score": 0.712}
     *   ]
     * }
     * ```
     *
     * ### Example 2: Conceptual Query
     * **Request:**
     * ```json
     * {
     *   "query": "database performance optimization"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "query": "database performance optimization",
     *   "results": [
     *     {"filename": "database/indexing.md", "score": 0.871},
     *     {"filename": "database/query-optimization.md", "score": 0.823},
     *     {"filename": "database/caching-strategies.md", "score": 0.745},
     *     {"filename": "performance/monitoring.md", "score": 0.621}
     *   ]
     * }
     * ```
     *
     * ### Example 3: Synonym Handling (Strength of Semantic Search)
     * **Request:**
     * ```json
     * {
     *   "query": "deploy to cloud"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "query": "deploy to cloud",
     *   "results": [
     *     {"filename": "deployment/kubernetes.md", "score": 0.889},
     *     {"filename": "deployment/docker.md", "score": 0.856},
     *     {"filename": "infrastructure/cloud-setup.md", "score": 0.812}
     *   ]
     * }
     * ```
     * Note: Tool finds documents about "deploy", "cloud", "Kubernetes", "Docker"
     * even though exact terms may not be present.
     *
     * ### Example 4: No Results
     * **Request:**
     * ```json
     * {
     *   "query": "extremely obscure topic nobody documented"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "query": "extremely obscure topic nobody documented",
     *   "results": []
     * }
     * ```
     *
     * ### Example 5: Missing Required Parameter
     * **Request:**
     * ```json
     * {}
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "error": "query is required"
     * }
     * ```
     *
     * ## Implementation Details
     *
     * ### Input Validation
     * - Checks if `query` parameter is present and non-null
     * - Returns error response if validation fails
     * - No length validation (empty queries allowed)
     *
     * ### Search Process
     * 1. Validate input parameters
     * 2. Extract query string from request arguments
     * 3. Call `indexer.search(query)` for vector similarity search
     * 4. Receive list of (filename, score) tuples
     * 5. Format results as JSON
     * 6. Return CallToolResult
     *
     * ### Result Formatting
     * - Results are automatically formatted as JSON
     * - Each result includes filename and similarity score
     * - Scores are formatted to 3 decimal places using `"%.3f".format(score)`
     * - Results are ordered by relevance (highest scores first)
     * - Query is included in response for context
     *
     * ### Score Precision
     * ```kotlin
     * val formattedScore = "%.3f".format(score)  // Always 3 decimal places
     * // Examples: 0.892, 0.756, 0.001, 1.000
     * ```
     *
     * ## Performance Characteristics
     *
     * | Operation | Complexity | Notes |
     * |-----------|-----------|-------|
     * | Query Embedding | O(q) | q = query length |
     * | Vector Search | O(n log n) | n = number of documents |
     * | Score Formatting | O(m) | m = number of results |
     * | Total | O(q + n log n + m) | Usually dominates: n log n |
     *
     * ## Advantages Over Keyword Search
     *
     * 1. **Semantic Understanding:** Understands meaning, not just words
     * 2. **Synonym Tolerance:** Finds documents using different terminology
     * 3. **Context Awareness:** Understands document context
     * 4. **Flexible Matching:** Works with natural language phrasing
     * 5. **Better Ranking:** Ranks results by actual relevance
     *
     * ## Limitations
     *
     * - Requires indexed documents with embeddings
     * - Accuracy depends on embedding model quality
     * - May miss documents that don't exist yet
     * - Performance depends on index size
     * - Requires semantic similarity in knowledge base
     *
     * ## When to Use Semantic Search
     *
     * ✓ **Use for:**
     * - Conceptual queries ("how to scale applications")
     * - Natural language questions ("what's the best practice for...")
     * - Topic-based searches ("caching strategies")
     * - Synonym handling ("container management" → finds Docker, Kubernetes)
     * - Exploring the knowledge base
     *
     * ✗ **Don't use for:**
     * - Exact filename matching (use direct file lookup)
     * - Very specific technical terms that must match exactly
     * - Boolean logic queries (AND, OR, NOT)
     * - Structured data queries
     *
     * ## Integration with Other Tools
     *
     * ### With SmartSearchTool
     * SmartSearchTool internally uses semantic search and adds template fallback:
     * - If `semantic_search` returns results → returns them
     * - If no results → returns template for creating documentation
     *
     * ### With AddDocumentTool
     * Documents added with `add_document` are automatically indexed and
     * become searchable with `semantic_search`:
     * 1. User calls `add_document`
     * 2. Document is stored and indexed
     * 3. Document immediately available for `semantic_search`
     *
     * ## Error Handling
     *
     * The tool handles two types of errors:
     *
     * 1. **Input Validation Error**
     * ```json
     * {"error": "query is required"}
     * ```
     *
     * 2. **Indexer Errors** (propagated from indexer)
     * - Vector embedding generation fails
     * - Index lookup fails
     * - Reported as tool execution error
     *
     * ## Example: Building a Search UI
     *
     * ```kotlin
     * // Client code using semantic_search
     * fun searchDocumentation(userQuery: String) {
     *     val results = callTool("semantic_search", mapOf("query" to userQuery))
     *     
     *     results.results?.forEach { result ->
     *         println("${result.filename} (${result.score})")
     *     }
     * }
     * ```
     *
     * ## Response Field Reference
     *
     * | Field | Type | Always Present | Description |
     * |-------|------|---|---|
     * | query | string | Yes | The search query |
     * | results | array | Yes | Array of matching documents |
     * | results[].filename | string | Yes | Document filename |
     * | results[].score | number | Yes | Similarity score (0.0-1.0) |
     * | error | string | No | Error message (only on error) |
     *
     * @param server The MCP server instance to register this tool with
     *
     * @see Indexer.search
     * @see SmartSearchTool
     * @see AddDocumentTool
     * @see McpTool
     */
    override fun register(server: Server)
}
```

## Parameters

### query
- **Type:** String
- **Required:** Yes
- **Format:** Natural language text
- **Length:** Flexible (1+ characters)
- **Examples:**
  - "How do I authenticate users?"
  - "database performance optimization"
  - "microservices architecture patterns"
- **Validation:**
  - Cannot be null or missing

## Return Values

### Success - With Results

```json
{
  "query": "authentication",
  "results": [
    {"filename": "auth-guide.md", "score": 0.912},
    {"filename": "security.md", "score": 0.834},
    {"filename": "user-management.md", "score": 0.756}
  ]
}
```

### Success - No Results

```json
{
  "query": "obscure topic",
  "results": []
}
```

### Error

```json
{
  "error": "query is required"
}
```

## Score Range

| Score | Interpretation |
|-------|---|
| 0.9-1.0 | Highly relevant |
| 0.8-0.9 | Very relevant |
| 0.7-0.8 | Quite relevant |
| 0.6-0.7 | Somewhat relevant |
| 0.5-0.6 | Weakly relevant |
| 0.0-0.5 | Low relevance |

## Usage Examples

### Example 1: Technical Query

**Request:**
```json
{"query": "how to implement JWT authentication"}
```

**Response:**
```json
{
  "query": "how to implement JWT authentication",
  "results": [
    {"filename": "auth/jwt-implementation.md", "score": 0.956},
    {"filename": "auth/security.md", "score": 0.823},
    {"filename": "user-management.md", "score": 0.712}
  ]
}
```

### Example 2: Conceptual Query

**Request:**
```json
{"query": "cloud deployment strategies"}
```

**Response:**
```json
{
  "query": "cloud deployment strategies",
  "results": [
    {"filename": "infrastructure/cloud-setup.md", "score": 0.889},
    {"filename": "deployment/kubernetes.md", "score": 0.856},
    {"filename": "devops/ci-cd.md", "score": 0.745}
  ]
}
```

### Example 3: Synonym Tolerance

**Request:**
```json
{"query": "containerized applications"}
```

**Response:** (Semantic search finds Docker, Kubernetes, etc.)
```json
{
  "query": "containerized applications",
  "results": [
    {"filename": "docker/getting-started.md", "score": 0.901},
    {"filename": "kubernetes/deployment.md", "score": 0.878},
    {"filename": "devops/container-orchestration.md", "score": 0.823}
  ]
}
```

## When to Use

**Use SemanticSearchTool when:**
- ✓ Searching with natural language questions
- ✓ Looking for documents by topic/concept
- ✓ Exploring the knowledge base
- ✓ Synonym handling is important
- ✓ Finding related documents

**Use something else when:**
- ✗ Need exact filename matches (direct lookup)
- ✗ Need boolean logic (AND, OR, NOT)
- ✗ Searching structured data
- ✗ Need full-text keyword search

## Advantages Over Keyword Search

| Feature | Semantic | Keyword |
|---------|----------|---------|
| Understands meaning | ✓ | ✗ |
| Handles synonyms | ✓ | ✗ |
| Context aware | ✓ | ✗ |
| Natural language | ✓ | ✗ |
| Exact phrase required | ✗ | ✓ |
| Fast | ✗ | ✓ |

## Integration

### With SmartSearchTool

SmartSearchTool wraps SemanticSearchTool with:
- Fallback documentation template if no results
- Suggestion to create missing documentation
- Self-improving knowledge base pattern

### With AddDocumentTool

New documents added with AddDocumentTool are immediately:
- Indexed for semantic search
- Available for SemanticSearchTool queries
- Ranked by relevance in results

## Best Practices

1. **Clear Queries**
   - Use natural language phrasing
   - Include context and intent
   - Specific is better than vague

2. **Interpret Scores**
   - Scores >0.8 are generally relevant
   - Review top results first
   - Adjust query if scores are low

3. **Handle No Results**
   - Try rephrasing the query
   - Use different terminology
   - Consider creating documentation

## See Also

- [SmartSearchTool Documentation](SmartSearchTool.md)
- [AddDocumentTool Documentation](AddDocumentTool.md)
- [Indexer Documentation](../Indexer.md)
- [McpTool Interface Documentation](McpTool.md)
