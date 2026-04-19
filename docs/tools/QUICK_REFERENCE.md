# ArchivistMCP Tools - Quick Reference Guide

## Overview

Quick reference for all tools in the ArchivistMCP system. For detailed documentation, see individual tool files.

## Tools Summary

| Tool | Purpose | Best For |
|------|---------|----------|
| **add_document** | Create and index new documents | Adding documentation to knowledge base |
| **semantic_search** | Find documents by semantic similarity | Natural language queries |
| **smart_search** | Search with template fallback | Discovering & creating documentation |

---

## Tool Matrix

### 1. McpTool (Base Interface)

```
Location: src/main/kotlin/io/aeqiocor/archivistmcp/tool/McpTool.kt
Doc: docs/tools/McpTool.md
```

**Purpose:** Base interface for all tools

**Key Method:**
```kotlin
fun register(server: Server)
```

**Responsibility:** Define tool registration contract

---

### 2. AddDocumentTool

```
Location: src/main/kotlin/io/aeqiocor/archivistmcp/tool/AddDocumentTool.kt
Doc: docs/tools/AddDocumentTool.md
Tool Name: add_document
```

**Purpose:** Create and index markdown documents

**Parameters:**
- `filename` (string, required): File path with .md extension
- `content` (string, required): Markdown content

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

**Usage Examples:**
```kotlin
// Simple document
add_document("readme.md", "# My Docs")

// Nested directory
add_document("guides/setup.md", "# Setup Guide")

// Complex markdown
add_document("api/endpoints.md", "# Endpoints\n\n## GET /users")
```

**Key Features:**
- ✓ Creates files in docs directory
- ✓ Automatically indexes documents
- ✓ Supports nested directories
- ✓ Works with SmartSearchTool suggestions

---

### 3. SemanticSearchTool

```
Location: src/main/kotlin/io/aeqiocor/archivistmcp/tool/SemanticSearchTool.kt
Doc: docs/tools/SemanticSearchTool.md
Tool Name: semantic_search
```

**Purpose:** Search documents by semantic meaning

**Parameters:**
- `query` (string, required): Natural language search query

**Success Response:**
```json
{
  "query": "authentication",
  "results": [
    {"filename": "auth-guide.md", "score": 0.892},
    {"filename": "security.md", "score": 0.756}
  ]
}
```

**Empty Results:**
```json
{
  "query": "unknown topic",
  "results": []
}
```

**Usage Examples:**
```kotlin
// Technical query
semantic_search("How do I implement JWT?")

// Conceptual query
semantic_search("database optimization")

// Multi-concept
semantic_search("CI/CD pipeline with GitHub")
```

**Score Interpretation:**
- **0.9-1.0**: Highly relevant
- **0.8-0.9**: Very relevant
- **0.7-0.8**: Quite relevant
- **0.6-0.7**: Somewhat relevant
- **0.0-0.6**: Weakly relevant

**Key Features:**
- ✓ Understands meaning, not just keywords
- ✓ Handles synonyms automatically
- ✓ Context-aware matching
- ✓ Returns relevance scores
- ✗ Empty results return no suggestions

---

### 4. SmartSearchTool

```
Location: src/main/kotlin/io/aeqiocor/archivistmcp/tool/SmartSearchTool.kt
Doc: docs/tools/SmartSearchTool.md
Tool Name: smart_search
```

**Purpose:** Intelligent search with documentation template fallback

**Parameters:**
- `query` (string, required): Natural language search query

**Found Response:**
```json
{
  "status": "found",
  "query": "authentication",
  "results": [
    {"filename": "auth-guide.md", "score": 0.892}
  ]
}
```

**Not Found Response:**
```json
{
  "status": "not_found",
  "query": "new topic",
  "action": "add_document",
  "suggested_filename": "new-topic.md",
  "template": "# New Topic\n\n## Overview\n..."
}
```

**Usage Examples:**
```kotlin
// Search - documents exist
smart_search("authentication")
// Returns: existing documents

// Search - no documents
smart_search("quantum computing")
// Returns: template + "use add_document to create"
```

**Filename Generation:**
```
Query: "How to deploy to AWS?"
→ "how-to-deploy-to-aws.md"

Query: "Database Setup & Config!!!"
→ "database-setup-config.md"
```

**Key Features:**
- ✓ Searches like SemanticSearchTool
- ✓ Provides template if no results
- ✓ Suggests filename automatically
- ✓ Enables self-improving knowledge base
- ✓ Encourages documentation creation

---

## Usage Decision Tree

```
                        Search for documents?
                               |
                        -------+-------
                       /               \
            "I want results"      "I want to grow KB"
                   |                      |
                   |                      |
          semantic_search            smart_search
                   |                      |
         Returns results            Returns results
         OR empty array             OR template+hint
                                        |
                                   User can now:
                                   add_document
                                        |
                                   Document indexed
                                   & searchable
```

---

## Common Workflows

### Workflow 1: Add Documentation

```
1. Call: add_document
   - filename: "guides/setup.md"
   - content: "# Setup Guide\n..."

2. Result: {"status": "ok", "filename": "guides/setup.md"}

3. Effect: Document is now indexed and searchable
```

### Workflow 2: Search Existing Docs

```
1. Call: semantic_search
   - query: "how to setup"

2. Result: [
     {"filename": "guides/setup.md", "score": 0.95},
     ...
   ]

3. Effect: User finds relevant documentation
```

### Workflow 3: Self-Improving Knowledge Base

```
1. User: smart_search("deployment guide")

2. Result (not found):
   {
     "status": "not_found",
     "suggested_filename": "deployment-guide.md",
     "template": "# Deployment Guide\n..."
   }

3. User: add_document
   - filename: "deployment-guide.md"
   - content: <filled template>

4. Result: Document created and indexed

5. Next user: smart_search("deployment")
   → Finds newly created document!
```

---

## Tool Relationships

```
┌─────────────┐
│   McpTool   │ (Base Interface)
│ (register)  │
└──────┬──────┘
       │
       ├─────────────────────────────────────┐
       │                                     │
       v                                     v
┌─────────────────┐              ┌──────────────────┐
│ AddDocumentTool │              │ SemanticSearchTool
│  (create docs)  │              │  (search docs)
└────────┬────────┘              └────────┬─────────┘
         │                                │
         │      ┌───────────────────────┘
         │      │
         │      v
         └─→ Indexer ←─┐
               │        │
               v        │
          (storage)     │
          (indexing)    │
               │        │
               └────────┘
                    |
         ┌──────────v───────────┐
         │  SmartSearchTool     │
         │ (search + template)  │
         └──────────┬───────────┘
                    │
         Uses both AddDocumentTool
         and SemanticSearchTool
```

---

## Parameter Quick Reference

### AddDocumentTool Parameters

| Parameter | Type | Required | Example |
|-----------|------|----------|---------|
| filename | String | Yes | "guides/setup.md" |
| content | String | Yes | "# Setup\n\nInstructions..." |

### SemanticSearchTool Parameters

| Parameter | Type | Required | Example |
|-----------|------|----------|---------|
| query | String | Yes | "How to authenticate?" |

### SmartSearchTool Parameters

| Parameter | Type | Required | Example |
|-----------|------|----------|---------|
| query | String | Yes | "Best practices" |

---

## Response Status Codes

### AddDocumentTool

| Status | Meaning |
|--------|---------|
| ✓ Success | Document created and indexed |
| ✗ Error | Missing filename or content |

### SemanticSearchTool

| Status | Meaning |
|--------|---------|
| ✓ Success | Returned results (may be empty) |
| ✗ Error | Missing query parameter |

### SmartSearchTool

| Status | Meaning |
|--------|---------|
| "found" | Documents matched query |
| "not_found" | No documents, returned template |
| "error" | Missing query parameter |

---

## Performance Tips

1. **Batch Operations**
   - Add multiple documents before searching
   - Documents become searchable immediately

2. **Query Optimization**
   - More specific queries return better results
   - Natural language phrasing works best
   - Longer queries (2-10 words) are optimal

3. **Document Quality**
   - Clear titles improve semantic matching
   - Good markdown structure helps indexing
   - Comprehensive content aids search

---

## Troubleshooting

### Documents Not Found

| Problem | Solution |
|---------|----------|
| No results after adding | Check document indexing |
| Wrong results | Try rephrasing query |
| Documents missing | Verify add_document succeeded |

### Tool Errors

| Error | Cause | Fix |
|-------|-------|-----|
| "query is required" | Missing query param | Provide query string |
| "filename and content required" | Missing parameter | Provide both params |
| Unexpected error | Indexer issue | Check indexer logs |

---

## Integration Examples

### With MCP Clients

```kotlin
// Register tools with server
val addTool = AddDocumentTool(indexer)
val semanticTool = SemanticSearchTool(indexer)
val smartTool = SmartSearchTool(indexer)

server.apply {
    addTool.register(this)
    semanticTool.register(this)
    smartTool.register(this)
}
```

### Typical Flow

```
Client
  │
  ├─→ smart_search("topic")
  │       │
  │       ├─ Documents found → return results
  │       │
  │       └─ No documents → return template
  │
  ├─→ [Optional] add_document("topic.md", content)
  │       │
  │       └─ Document indexed
  │
  └─→ semantic_search("related topic")
          │
          └─ Find all related documents
```

---

## Best Practices

### Document Creation

✓ Use clear, descriptive filenames
✓ Start with a level-1 heading
✓ Include examples in technical docs
✓ Link to related documentation
✗ Don't use special characters in filenames
✗ Don't leave documents empty

### Searching

✓ Use natural language phrasing
✓ Be specific about what you're looking for
✓ Try multiple search approaches
✓ Review top results even if score is moderate
✗ Don't expect exact keyword matches
✗ Don't assume query phrasing doesn't matter

### Knowledge Base Growth

✓ Review "not_found" results to identify gaps
✓ Encourage documentation for common questions
✓ Keep documentation up-to-date
✓ Cross-link related documents
✗ Don't add duplicate documentation
✗ Don't leave suggested documents uncreated

---

## File Locations

```
src/main/kotlin/io/aeqiocor/archivistmcp/
├── tool/
│   ├── McpTool.kt                    # Base interface
│   ├── AddDocumentTool.kt            # Create docs
│   ├── SemanticSearchTool.kt         # Search docs
│   └── SmartSearchTool.kt            # Smart search
├── Indexer.kt                        # Index management
├── AppConfig.kt                      # Configuration
├── main.kt                           # Entry point
└── server.kt                         # Server setup

docs/
├── TOOLS.md                          # Full documentation
└── tools/
    ├── McpTool.md                    # Interface doc
    ├── AddDocumentTool.md            # Tool doc
    ├── SemanticSearchTool.md         # Tool doc
    ├── SmartSearchTool.md            # Tool doc
    └── QUICK_REFERENCE.md            # This file
```

---

## Links to Full Documentation

- [Complete Tools Documentation](TOOLS.md)
- [McpTool Interface](McpTool.md)
- [AddDocumentTool Reference](AddDocumentTool.md)
- [SemanticSearchTool Reference](SemanticSearchTool.md)
- [SmartSearchTool Reference](SmartSearchTool.md)

---

## Abbreviations

- **MCP**: Model Context Protocol
- **KB**: Knowledge Base
- **KDoc**: Kotlin Documentation
- **JSON**: JavaScript Object Notation
- **UTF-8**: Unicode Transformation Format

---

## Version Info

- **Project**: ArchivistMCP
- **Language**: Kotlin
- **Framework**: MCP Kotlin SDK
- **Documentation**: Markdown + KDoc

---

## Support

For more information:
- See full documentation in `docs/tools/`
- Review source code in `src/main/kotlin/`
- Check project tests in `src/test/kotlin/`
