# AddDocumentTool Documentation

## Overview

The `AddDocumentTool` enables dynamic document management by allowing new markdown files to be created in the documentation directory and automatically indexed for semantic search. This tool is essential for expanding and maintaining the knowledge base.

## Location

`src/main/kotlin/io/aeqiocor/archivistmcp/tool/AddDocumentTool.kt`

## KDoc

```kotlin
/**
 * MCP tool for adding and indexing new markdown documents.
 *
 * This tool enables dynamic document management by allowing new markdown files
 * to be created in the documentation directory and automatically indexed for
 * semantic search. Documents can be organized in subdirectories.
 *
 * The tool provides a simple interface for creating documentation that will be
 * immediately available for semantic search queries. This supports the
 * self-improving knowledge base pattern where missing documentation can be
 * identified and created on demand.
 *
 * ## Dependencies
 * - [Indexer]: Handles file I/O and semantic indexing
 *
 * ## MCP Tool Details
 * - **Name:** add_document
 * - **MCP Type:** Tool
 * - **Availability:** Available to all MCP clients
 *
 * @property indexer The [Indexer] instance responsible for file management and indexing
 *
 * @see Indexer.addDocument
 * @see McpTool
 * @see SmartSearchTool
 */
class AddDocumentTool(private val indexer: Indexer) : McpTool {
    /**
     * Registers the add_document tool with the MCP server.
     *
     * This tool allows creating new markdown documents in the docs directory
     * and automatically indexing them for semantic search. Documents can be
     * organized using subdirectory paths.
     *
     * ## Tool Specification
     *
     * | Attribute | Value |
     * |-----------|-------|
     * | Name | `add_document` |
     * | Description | Add a new markdown document to the docs directory and index it for semantic search |
     * | Input Type | JSON Object |
     * | Output Type | JSON Object |
     *
     * ## Input Parameters
     *
     * ### filename (required, string)
     * - Filename with .md extension
     * - Supports nested directory paths
     * - Examples:
     *   - `guide.md` - creates file in root docs directory
     *   - `guides/setup.md` - creates file in guides subdirectory
     *   - `api/v1/endpoints.md` - creates file in nested directories
     *
     * ### content (required, string)
     * - Full markdown content of the document
     * - Supports any valid markdown syntax
     * - Can include headers, code blocks, links, etc.
     * - Content is validated during indexing
     *
     * ## Input Schema
     *
     * ```json
     * {
     *   "type": "object",
     *   "properties": {
     *     "filename": {
     *       "type": "string",
     *       "description": "Filename with .md extension, e.g. 'guide.md' or 'subdir/notes.md'"
     *     },
     *     "content": {
     *       "type": "string",
     *       "description": "Markdown content of the document"
     *     }
     *   },
     *   "required": ["filename", "content"]
     * }
     * ```
     *
     * ## Response Format
     *
     * ### Success Response
     * ```json
     * {
     *   "status": "ok",
     *   "filename": "guides/setup.md"
     * }
     * ```
     *
     * **Status Codes:**
     * - `"ok"` - Document created and indexed successfully
     *
     * **Fields:**
     * - `status`: Always "ok" on success
     * - `filename`: The exact filename that was created
     *
     * ### Error Response
     * ```json
     * {
     *   "error": "filename and content are required"
     * }
     * ```
     *
     * **Error Conditions:**
     * - `filename` is null or missing
     * - `content` is null or missing
     * - Both parameters must be present
     *
     * ## Usage Examples
     *
     * ### Example 1: Simple Document
     * **Request:**
     * ```json
     * {
     *   "filename": "tutorial.md",
     *   "content": "# Getting Started\n\nThis is a tutorial.\n\n## Step 1\nFirst, do this."
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "status": "ok",
     *   "filename": "tutorial.md"
     * }
     * ```
     *
     * ### Example 2: Nested Directory
     * **Request:**
     * ```json
     * {
     *   "filename": "guides/api-reference.md",
     *   "content": "# API Reference\n\n## Endpoints\n\n### GET /users\nRetrieve all users."
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "status": "ok",
     *   "filename": "guides/api-reference.md"
     * }
     * ```
     *
     * ### Example 3: Complex Markdown
     * **Request:**
     * ```json
     * {
     *   "filename": "docs/advanced-features.md",
     *   "content": "# Advanced Features\n\n## Caching\n\n\`\`\`kotlin\nval cache = mutableMapOf<String, Any>()\n\`\`\`\n\n## See Also\n- [Basic Features](basic.md)\n- [Configuration](config.md)"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "status": "ok",
     *   "filename": "docs/advanced-features.md"
     * }
     * ```
     *
     * ### Example 4: Missing Parameter
     * **Request:**
     * ```json
     * {
     *   "filename": "guide.md"
     * }
     * ```
     *
     * **Response:**
     * ```json
     * {
     *   "error": "filename and content are required"
     * }
     * ```
     *
     * ## Implementation Details
     *
     * ### Input Validation
     * - Both `filename` and `content` must be present and non-null
     * - Returns error response if validation fails
     * - Validation is case-sensitive
     *
     * ### File Creation
     * - Actual file creation is delegated to the [Indexer]
     * - Directory structure is created if needed
     * - Files are written as UTF-8 encoded markdown
     * - Existing files with same name are overwritten
     *
     * ### Indexing
     * - Documents are automatically indexed after creation
     * - Semantic embeddings are generated for search
     * - Indexing is performed synchronously
     * - Indexing errors will propagate as tool errors
     *
     * ### Path Handling
     * - Forward slashes `/` are used for directory paths
     * - Relative paths start from the docs directory
     * - Absolute paths are not supported
     * - Path traversal (`../`) is not recommended
     * - Subdirectories are created automatically if needed
     *
     * ## Performance Characteristics
     *
     * | Operation | Time Complexity | Notes |
     * |-----------|-----------------|-------|
     * | File Writing | O(n) | n = content length |
     * | Semantic Indexing | O(n) | n = content length, varies by model |
     * | Validation | O(1) | Simple null checks |
     *
     * ## Error Handling
     *
     * The tool handles errors in two ways:
     *
     * 1. **Input Validation Errors** - Synchronous checks
     *    ```kotlin
     *    if (filename == null || content == null) {
     *        return error response
     *    }
     *    ```
     *
     * 2. **Indexer Errors** - From file I/O or embedding generation
     *    - If indexer throws exception, tool execution fails
     *    - Error is reported to client
     *    - No partial state is left behind
     *
     * ## Integration with SmartSearchTool
     *
     * The `add_document` tool is designed to work with [SmartSearchTool]:
     *
     * 1. User queries with `smart_search`
     * 2. If no documents found, tool suggests filename and provides template
     * 3. User creates document with `add_document`
     * 4. Future `smart_search` queries find the new document
     *
     * This creates a self-improving knowledge base pattern.
     *
     * ## Security Considerations
     *
     * - **Path Traversal:** Be cautious with user-provided filenames
     * - **File Permissions:** Documents are readable by all users
     * - **Content Validation:** No content filtering is performed
     * - **Storage Limits:** No quota enforcement at tool level
     *
     * ## Limitations
     *
     * - Only markdown files (.md extension) are supported
     * - File extension is not validated
     * - Concurrent writes to same filename are not handled
     * - No version control or history tracking
     * - No access control or permissions system
     *
     * @param server The MCP server instance to register this tool with
     *
     * @see Indexer.addDocument
     * @see SmartSearchTool
     * @see McpTool
     */
    override fun register(server: Server)
}
```

## Parameters

### filename
- **Type:** String
- **Required:** Yes
- **Format:** Filename with `.md` extension
- **Examples:**
  - `guide.md`
  - `tutorials/setup.md`
  - `api/endpoints.md`
- **Validation:**
  - Cannot be null or empty
  - Should end with `.md` (not enforced)
  - Supports nested directories

### content
- **Type:** String
- **Required:** Yes
- **Format:** Valid Markdown
- **Validation:**
  - Cannot be null or empty
  - Any markdown syntax is supported

## Return Values

### Success

```json
{
  "status": "ok",
  "filename": "guide.md"
}
```

### Error

```json
{
  "error": "filename and content are required"
}
```

## Usage Patterns

### Pattern 1: Create Simple Documentation

```kotlin
// Add a simple markdown file
// filename: "readme.md"
// content: "# My Documentation\n\nWelcome to my docs."
// Result: Document created and indexed
```

### Pattern 2: Organize with Subdirectories

```kotlin
// Create documentation structure
// filename: "guides/installation.md"
// content: "# Installation Guide\n\nHow to install..."
```

### Pattern 3: Add Code Documentation

```kotlin
// Create documentation with code examples
// filename: "examples/kotlin-snippets.md"
// content: "# Kotlin Snippets\n\n```kotlin\nfun main() { }\n```"
```

### Pattern 4: Self-Improving Knowledge Base (with SmartSearchTool)

```kotlin
// 1. User searches: "How to deploy to production?"
// 2. SmartSearchTool returns: "Document not found. Use this template..."
// 3. User creates: 
//    filename: "deployment-guide.md"
//    content: <filled out template>
// 4. Document is indexed and available for future searches
```

## Integration Points

### With Indexer

The tool relies on `Indexer.addDocument()`:

```kotlin
// Tool calls:
indexer.addDocument(filename, content)

// Indexer responsibilities:
// - Create directory structure if needed
// - Write file to disk
// - Generate semantic embeddings
// - Index document for search
```

### With SmartSearchTool

SmartSearchTool suggests using AddDocumentTool:

```json
{
  "status": "not_found",
  "action": "add_document",
  "suggested_filename": "deployment-guide.md",
  "template": "# Deployment Guide\n..."
}
```

## Best Practices

1. **Clear Filenames**
   - Use descriptive, lowercase names
   - Separate words with hyphens
   - Include `.md` extension

2. **Structured Content**
   - Start with a level-1 heading (#)
   - Use proper markdown structure
   - Include code examples for technical docs

3. **Consistent Organization**
   - Group related documents in subdirectories
   - Use consistent naming conventions
   - Link between related documents

4. **Content Quality**
   - Provide complete, useful information
   - Include code examples where appropriate
   - Keep content up-to-date

## Examples

### Example: API Documentation

**Request:**
```json
{
  "filename": "api/rest-endpoints.md",
  "content": "# REST API Endpoints\n\n## Authentication\n\nAll endpoints require an API key.\n\n## Users\n\n### GET /api/users\nRetrieve all users.\n\n**Response:**\n```json\n[{\"id\": 1, \"name\": \"John\"}]\n```"
}
```

**Response:**
```json
{
  "status": "ok",
  "filename": "api/rest-endpoints.md"
}
```

### Example: Configuration Guide

**Request:**
```json
{
  "filename": "configuration/database-setup.md",
  "content": "# Database Setup\n\n## Prerequisites\n- PostgreSQL 12+\n- Node.js 14+\n\n## Installation\n\n1. Install PostgreSQL\n2. Create database: `createdb myapp`\n3. Run migrations"
}
```

**Response:**
```json
{
  "status": "ok",
  "filename": "configuration/database-setup.md"
}
```

## Performance Considerations

- **File Writing:** Speed depends on content size and disk I/O
- **Indexing:** Speed depends on embedding model and content length
- **Validation:** Negligible overhead (simple null checks)

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| "filename and content are required" | Missing parameter | Ensure both filename and content are provided |
| File not created | Indexer error | Check indexer logs for file I/O or embedding errors |
| Document not searchable | Indexing failed | Verify document content is valid markdown |

## See Also

- [SmartSearchTool Documentation](SmartSearchTool.md)
- [SemanticSearchTool Documentation](SemanticSearchTool.md)
- [Indexer Documentation](../Indexer.md)
- [McpTool Interface Documentation](McpTool.md)
