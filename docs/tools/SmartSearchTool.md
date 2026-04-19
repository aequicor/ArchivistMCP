# SmartSearchTool Documentation

## Overview

The `SmartSearchTool` is an intelligent search tool that combines semantic search with a helpful fallback mechanism. When documents are found, it returns them; when no documents exist, it provides a markdown template and instructs users to create the missing documentation using AddDocumentTool. This enables a self-improving knowledge base.

## Location

`src/main/kotlin/io/aeqiocor/archivistmcp/tool/SmartSearchTool.kt`

## KDoc

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
 * identified and added on-demand. Rather than returning empty results, the tool
 * helps users bootstrap missing documentation.
 *
 * ## Smart Search Philosophy
 *
 * Smart search recognizes that an empty result set is an opportunity:
 * - Empty result → Missing documentation identified
 * - Provide template → Lower barrier to documentation creation
 * - Suggest filename → Standardize naming and organization
 * - Enable add_document → Seamless knowledge base expansion
 *
 * This closes the documentation gap loop: when users ask questions that aren't
 * answered, they can immediately contribute answers.
 *
 * ## Self-Improving Knowledge Base Pattern
 *
 * 1. **User Searches:** smart_search doesn't find documents
 * 2. **Tool Suggests:** Template and filename for new documentation
 * 3. **User Creates:** Fills template and calls add_document
 * 4. **Knowledge Base:** Document is indexed and available
 * 5. **Future Searches:** Other users find the documentation
 *
 * ## Dependencies
 * - [Indexer]: Handles semantic search and document management
 *
 * ## Resources
 * - `classpath:doc-template.md`: Default markdown template for new documents
 *
 * ## MCP Tool Details
 * - **Name:** smart_search
 * - **MCP Type:** Tool
 * - **Availability:** Available to all MCP clients
 *
 * @property indexer The [Indexer] instance for semantic search
 * @property template Lazily-loaded markdown template for new documents
 *
 * @see Indexer.search
 * @see AddDocumentTool
 * @see SemanticSearchTool
 * @see McpTool
 */
class SmartSearchTool(private val indexer: Indexer) : McpTool {
    /**
     * Registers the smart_search tool with the MCP server.
     *
     * This tool provides intelligent search with fallback documentation support.
     * It combines semantic search results with automatic template generation for
     * missing documentation.
     *
     * ## Tool Specification
     *
     * | Attribute | Value |
     * |-----------|-------|
     * | Name | `smart_search` |
     * | Description | Search documents by semantic similarity. If nothing is found, returns a documentation template and instructs to call add_document to create and index a new document. |
     * | Input Type | JSON Object |
     * | Output Type | JSON Object |
     * | Response Type | Variable (found or not_found) |
     *
     * ## Input Parameters
     *
     * ### query (required, string)
     * - Natural language search query
     * - Any phrasing or length
     * - Examples:
     *   - "How do I deploy to production?"
     *   - "Database configuration"
     *   - "Best practices for error handling"
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
     * ## Response Format - Documents Found
     *
     * When matching documents are found, SmartSearchTool returns semantic search results:
     *
     * ```json
     * {
     *   "status": "found",
     *   "query": "authentication guide",
     *   "results": [
     *     {"filename": "auth-guide.md", "score": 0.892},
     *     {"filename": "security.md", "score": 0.756},
     *     {"filename": "user-management.md", "score": 0.683}
     *   ]
     * }
     * ```
     *
     * **Fields:**
     * - `status`: Always "found" when documents are located
     * - `query`: The search query as provided
     * - `results`: Array of matching documents sorted by relevance
     *   - `filename`: Path to the document
     *   - `score`: Similarity score (0.0 to 1.0)
     *
     * ## Response Format - Documents Not Found
     *
     * When no matching documents are found, SmartSearchTool returns a template:
     *
     * ```json
     * {
     *   "status": "not_found",
     *   "query": "authentication guide",
     *   "action": "add_document",
     *   "suggested_filename": "authentication-guide.md",
     *   "template": "# Authentication Guide\n\n## Overview\n\nProvide an overview...\n\n## Key Concepts\n\nExplain key concepts..."
     * }
     * ```
     *
     * **Fields:**
     * - `status`: Always "not_found" when no documents match
     * - `query`: The original search query
     * - `action`: The recommended action ("add_document")
     * - `suggested_filename`: Auto-generated filename slug
     * - `template`: Markdown template for the document
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
     * ## Filename Slug Generation
     *
     * When documents are not found, a filename slug is automatically generated:
     *
     * ### Algorithm
     * 1. Convert query to lowercase
     * 2. Replace non-alphanumeric characters with hyphens
     * 3. Trim leading/trailing hyphens
     * 4. Append `.md` extension
     *
     * ### Code
     * ```kotlin
     * val slug = query.lowercase()
     *     .replace(Regex("[^a-z0-9]+"), "-")
     *     .trim('-')
     * val filename = "$slug.md"
     * ```
     *
     * ### Examples
     *
     * | Query | Generated Filename |
     * |-------|-------------------|
     * | "How to authenticate users?" | `how-to-authenticate-users.md` |
     * | "Database Setup & Configuration" | `database-setup-configuration.md` |
     * | "API/REST Best Practices!!!" | `apirest-best-practices.md` |
     * | "CI/CD Pipeline for GitHub" | `cicd-pipeline-for-github.md` |
     * | "Multi-word MIXED case query" | `multi-word-mixed-case-query.md` |
     *
     * ## Template Resource
     *
     * ### Location
     * - **Resource Path:** `classpath:doc-template.md`
     * - **File Type:** Markdown (.md)
     * - **Encoding:** UTF-8
     *
     * ### Loading Process
     * - Template is loaded lazily (on first use)
     * - Loaded via: `SmartSearchTool::class.java.classLoader.getResourceAsStream()`
     * - Result is cached for performance
     * - Falls back to empty string if file not found
     *
     * ### Content Escaping
     * Template content is JSON-escaped for safe embedding in response:
     * - Backslashes `\` → `\\\\`
     * - Quotes `"` → `\\\"`
     * - Newlines `\n` → `\\n`
     *
     * ## Usage Examples
     *
     * ### Example 1: Found - Search Returns Documents
     * **Request:**
     * ```json
     * {
     *   "query": "REST API documentation"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "status": "found",
     *   "query": "REST API documentation",
     *   "results": [
     *     {"filename": "api/rest-endpoints.md", "score": 0.923},
     *     {"filename": "api/authentication.md", "score": 0.834},
     *     {"filename": "guides/getting-started.md", "score": 0.712}
     *   ]
     * }
     * ```
     *
     * ### Example 2: Not Found - Returns Template
     * **Request:**
     * ```json
     * {
     *   "query": "How to deploy to AWS?"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "status": "not_found",
     *   "query": "How to deploy to AWS?",
     *   "action": "add_document",
     *   "suggested_filename": "how-to-deploy-to-aws.md",
     *   "template": "# How to Deploy to AWS\n\n## Prerequisites\n\nList prerequisites here...\n\n## Step-by-Step Guide\n\n1. First step...\n2. Second step..."
     * }
     * ```
     *
     * ### Example 3: Not Found - Simple Query
     * **Request:**
     * ```json
     * {
     *   "query": "Quantum computing"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "status": "not_found",
     *   "query": "Quantum computing",
     *   "action": "add_document",
     *   "suggested_filename": "quantum-computing.md",
     *   "template": "# Quantum Computing\n\n## Overview\n..."
     * }
     * ```
     *
     * ### Example 4: Not Found - Special Characters
     * **Request:**
     * ```json
     * {
     *   "query": "C++/C# Performance Tips & Tricks!"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "status": "not_found",
     *   "query": "C++/C# Performance Tips & Tricks!",
     *   "action": "add_document",
     *   "suggested_filename": "cc-performance-tips-tricks.md",
     *   "template": "..."
     * }
     * ```
     *
     * ### Example 5: Missing Query Parameter
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
     * - Validates that `query` parameter is present and non-null
     * - Returns error if validation fails
     * - Allows any query content and length
     *
     * ### Search Execution
     * - Calls `indexer.search(query)` for semantic search
     * - Returns list of (filename, score) tuples
     *
     * ### Response Decision Logic
     * ```kotlin
     * if (results.isNotEmpty()) {
     *     // Return found response
     * } else {
     *     // Generate filename slug
     *     // Load template
     *     // Return not_found response
     * }
     * ```
     *
     * ### Template Lazy Loading
     * ```kotlin
     * private val template: String by lazy {
     *     SmartSearchTool::class.java.classLoader
     *         .getResourceAsStream("doc-template.md")
     *         ?.bufferedReader()?.readText() ?: ""
     * }
     * ```
     *
     * Benefits:
     * - Template is loaded once on first use
     * - Improves server startup time
     * - Fallback to empty string if not found
     * - Cached for subsequent requests
     *
     * ### JSON Escaping Process
     * Template content must be escaped for JSON embedding:
     * ```kotlin
     * val escapedTemplate = template
     *     .replace("\\", "\\\\")      // Backslash
     *     .replace("\"", "\\\"")      // Quote
     *     .replace("\n", "\\n")       // Newline
     * ```
     *
     * ## Self-Improving Knowledge Base Flow
     *
     * ### Step 1: User Searches
     * ```
     * Client: smart_search(query: "How to deploy?")
     * ```
     *
     * ### Step 2: No Documents Found
     * ```
     * SmartSearchTool checks index
     * Result: No matching documents
     * ```
     *
     * ### Step 3: Tool Suggests Action
     * ```json
     * {
     *   "status": "not_found",
     *   "action": "add_document",
     *   "suggested_filename": "how-to-deploy.md",
     *   "template": "..."
     * }
     * ```
     *
     * ### Step 4: User Creates Documentation
     * ```
     * Client: add_document(
     *   filename: "how-to-deploy.md",
     *   content: <filled template>
     * )
     * ```
     *
     * ### Step 5: Document is Indexed
     * ```
     * Indexer stores file
     * Semantic embeddings generated
     * Document immediately searchable
     * ```
     *
     * ### Step 6: Future Searches Find Document
     * ```
     * Next user: smart_search(query: "deployment guide")
     * Result: Returns newly created document
     * ```
     *
     * ## Advantages Over Basic Semantic Search
     *
     * | Feature | SmartSearchTool | SemanticSearchTool |
     * |---------|-----------------|-------------------|
     * | Finds existing docs | ✓ | ✓ |
     * | Suggests docs to create | ✓ | ✗ |
     * | Provides template | ✓ | ✗ |
     * | Suggests filename | ✓ | ✗ |
     * | Self-improving KBase | ✓ | ✗ |
     * | Speed | Slightly slower | Slightly faster |
     *
     * ## Performance Characteristics
     *
     * | Operation | Complexity | Notes |
     * |-----------|-----------|-------|
     * | Semantic search | O(n log n) | n = number of documents |
     * | Slug generation | O(q) | q = query length |
     * | Template loading | O(t) | t = template size (lazy, once) |
     * | JSON escaping | O(t) | t = template size |
     * | Total (found) | O(n log n) | Just semantic search |
     * | Total (not found) | O(n log n + q + t) | Semantic search + slug + escaping |
     *
     * ## Limitations
     *
     * - Exact score threshold for "found" vs "not found" is binary (at least 1 result)
     * - Template is fixed (not customizable per query)
     * - Slug generation may collide for different queries
     * - No validation of document quality
     * - No spam/abuse protection
     *
     * ## When to Use SmartSearchTool
     *
     * ✓ **Use when:**
     * - Want automatic template fallback
     * - Building self-improving knowledge base
     * - Users need documentation creation hints
     * - Want consistent document naming
     * - Prefer helpful empty results over zero results
     *
     * ✗ **Don't use when:**
     * - Only want existing documents
     * - Template fallback is distracting
     * - Performance is critical (use SemanticSearchTool)
     * - Don't want to encourage documentation creation
     *
     * ## Integration Points
     *
     * ### With AddDocumentTool
     * SmartSearchTool suggests calling AddDocumentTool:
     * - Provides suggested filename
     * - Includes template content
     * - Users can immediately create documents
     * - New documents become searchable
     *
     * ### With SemanticSearchTool
     * SmartSearchTool is a wrapper around semantic search:
     * - If results found: returns them (like SemanticSearchTool)
     * - If no results: adds template and suggestion
     * - Can be used interchangeably for most purposes
     *
     * ## Error Handling
     *
     * Two error types:
     *
     * 1. **Input Validation Error**
     * ```json
     * {"error": "query is required"}
     * ```
     *
     * 2. **Indexer Errors** (propagated)
     * - Embedding generation fails
     * - Index lookup fails
     * - Reported as tool execution error
     *
     * ## Customization
     *
     * To customize the template:
     * 1. Modify `src/main/resources/doc-template.md`
     * 2. Template is lazily loaded on first use
     * 3. Restart server to load new template
     * 4. Template appears in all `not_found` responses
     *
     * ## Analytics and Insights
     *
     * By tracking SmartSearchTool usage, you can identify:
     * - Popular topics with missing documentation
     * - Frequently created documents
     * - Knowledge gaps in documentation
     * - Emerging topics and needs
     *
     * ## Testing
     *
     * ```kotlin
     * @Test
     * fun testSmartSearchFound() {
     *     // Should return found response with results
     * }
     *
     * @Test
     * fun testSmartSearchNotFound() {
     *     // Should return not_found response with template
     * }
     *
     * @Test
     * fun testSlugGeneration() {
     *     // Should correctly generate filenames
     * }
     * ```
     *
     * @param server The MCP server instance to register this tool with
     *
     * @see SemanticSearchTool
     * @see AddDocumentTool
     * @see Indexer.search
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
- **Examples:**
  - "How do I authenticate users?"
  - "Database performance optimization"
  - "Best practices for error handling"
- **Validation:**
  - Cannot be null or missing

## Return Values

### Success - Documents Found

```json
{
  "status": "found",
  "query": "authentication",
  "results": [
    {"filename": "auth-guide.md", "score": 0.912},
    {"filename": "security.md", "score": 0.834}
  ]
}
```

### Success - No Documents Found (Helpful Fallback)

```json
{
  "status": "not_found",
  "query": "quantum computing",
  "action": "add_document",
  "suggested_filename": "quantum-computing.md",
  "template": "# Quantum Computing\n\n## Overview\n..."
}
```

### Error

```json
{
  "error": "query is required"
}
```

## Filename Slug Generation

When documents aren't found, SmartSearchTool generates a filename by:

| Query | Generated Filename |
|-------|-------------------|
| "How to authenticate users?" | `how-to-authenticate-users.md` |
| "Database Setup & Configuration" | `database-setup-configuration.md` |
| "API/REST Best Practices!!!" | `apirest-best-practices.md` |
| "CI/CD Pipeline for GitHub" | `cicd-pipeline-for-github.md` |

### Algorithm

1. Convert to lowercase
2. Replace non-alphanumeric with hyphens
3. Trim leading/trailing hyphens
4. Append `.md`

```kotlin
query.lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
    .plus(".md")
```

## Usage Examples

### Example 1: Found Documents

**Request:**
```json
{"query": "REST API documentation"}
```

**Response:**
```json
{
  "status": "found",
  "query": "REST API documentation",
  "results": [
    {"filename": "api/rest-endpoints.md", "score": 0.923},
    {"filename": "api/authentication.md", "score": 0.834}
  ]
}
```

### Example 2: No Documents - Returns Template

**Request:**
```json
{"query": "How to deploy to AWS?"}
```

**Response:**
```json
{
  "status": "not_found",
  "query": "How to deploy to AWS?",
  "action": "add_document",
  "suggested_filename": "how-to-deploy-to-aws.md",
  "template": "# How to Deploy to AWS\n\n## Prerequisites\n...\n## Step-by-Step\n..."
}
```

## Self-Improving Knowledge Base Pattern

SmartSearchTool enables a virtuous cycle:

1. **User asks question** via smart_search
2. **No documents found** → Tool suggests creation
3. **User creates documentation** via add_document
4. **Document is indexed** → Available for future searches
5. **Knowledge base grows** → More queries find answers

This pattern transforms the knowledge base into a living document that grows with user needs.

## Comparison with SemanticSearchTool

| Feature | SmartSearch | SemanticSearch |
|---------|-------------|---|
| Finds documents | ✓ | ✓ |
| Provides template | ✓ | ✗ |
| Suggests filename | ✓ | ✗ |
| Empty result handling | Helpful | Empty array |
| Encourages documentation | ✓ | ✗ |
| Performance | Slightly slower | Slightly faster |

**Use SmartSearchTool when:** You want to grow your knowledge base with user input.
**Use SemanticSearchTool when:** You only want to search existing documents.

## Template Loading

- **Location:** `classpath:doc-template.md`
- **Timing:** Lazy-loaded on first use
- **Caching:** Result is cached for performance
- **Fallback:** Empty string if file not found
- **Escaping:** JSON-safe escaping for embedding in response

## Best Practices

1. **Create High-Quality Templates**
   - Clear structure and sections
   - Helpful hints for documentation authors
   - Good starting point for new docs

2. **Customize Filenames**
   - Review suggested filenames
   - Adjust if they don't match your naming conventions
   - Consider prepending directory structure

3. **Use Both Tools**
   - SmartSearchTool for discovery and suggestions
   - AddDocumentTool to create suggested documents
   - Together they create a powerful documentation workflow

4. **Monitor Gaps**
   - Track which topics are most frequently not found
   - Identify priority areas for documentation
   - Proactively fill critical gaps

## Integration Examples

### Workflow: Create Missing Documentation

```
1. User: smart_search("How to set up OAuth?")
2. Tool: "Document not found. Suggested: oauth-setup.md"
3. User: add_document("oauth-setup.md", <filled template>)
4. System: Document is now indexed and searchable
5. Next user: smart_search("OAuth configuration") → finds it!
```

### Multi-Tool Flow

```
smart_search
  ↓
[Found] → Return results
[Not found] → Suggest add_document
  ↓
add_document
  ↓
[Create document] → Immediately indexed
  ↓
[Document searchable] → Future smart_search finds it
```

## See Also

- [SemanticSearchTool Documentation](SemanticSearchTool.md)
- [AddDocumentTool Documentation](AddDocumentTool.md)
- [Indexer Documentation](../Indexer.md)
- [McpTool Interface Documentation](McpTool.md)
