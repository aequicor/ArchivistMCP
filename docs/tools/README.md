# ArchivistMCP Tools Documentation

Complete Kotlin documentation for all MCP tools in the ArchivistMCP project.

## Documentation Files

### Overview & Reference
- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Quick lookup guide for all tools (start here!)
- **[../TOOLS.md](../TOOLS.md)** - Comprehensive tools documentation with detailed specifications

### Individual Tool Documentation

#### 1. McpTool (Base Interface)
**File:** [McpTool.md](McpTool.md)

Base interface that all tools implement. Defines the registration pattern and lifecycle.

```kotlin
interface McpTool {
    fun register(server: Server)
}
```

**Key Concepts:**
- Tool registration contract
- Server integration pattern
- Dependency injection
- Lifecycle management

#### 2. AddDocumentTool
**File:** [AddDocumentTool.md](AddDocumentTool.md) | **Tool Name:** `add_document`

Create and index markdown documents in the knowledge base.

```
Parameters:
  - filename (string): File path with .md extension
  - content (string): Markdown content

Response:
  {"status": "ok", "filename": "guide.md"}
```

**Best For:**
- Adding documentation to knowledge base
- Creating guided by SmartSearchTool suggestions
- Organizing docs in subdirectories

#### 3. SemanticSearchTool
**File:** [SemanticSearchTool.md](SemanticSearchTool.md) | **Tool Name:** `semantic_search`

Search documents using semantic similarity (vector search).

```
Parameters:
  - query (string): Natural language search query

Response:
  {
    "query": "...",
    "results": [
      {"filename": "...", "score": 0.892}
    ]
  }
```

**Best For:**
- Natural language queries
- Conceptual searches
- Finding related documents
- Understanding knowledge base content

#### 4. SmartSearchTool
**File:** [SmartSearchTool.md](SmartSearchTool.md) | **Tool Name:** `smart_search`

Intelligent search with documentation template fallback. Enables self-improving knowledge base.

```
Parameters:
  - query (string): Natural language search query

Response (Found):
  {
    "status": "found",
    "query": "...",
    "results": [...]
  }

Response (Not Found):
  {
    "status": "not_found",
    "action": "add_document",
    "suggested_filename": "...",
    "template": "..."
  }
```

**Best For:**
- Growing knowledge base with user input
- Identifying missing documentation
- Creating self-improving systems
- Helping users contribute docs

## Tool Relationships

```
McpTool (Interface)
    ↓
    ├─ AddDocumentTool (create docs)
    ├─ SemanticSearchTool (search docs)
    └─ SmartSearchTool (search + template)
         ↓
        Both use Indexer for storage & search
```

## Decision Matrix

When should I use which tool?

| Goal | Tool | Details |
|------|------|---------|
| Create documentation | `add_document` | Save markdown file and index it |
| Search documents | `semantic_search` | Vector-based semantic search |
| Smart search | `smart_search` | Semantic search + template fallback |
| Base tool | `McpTool` | Implement when creating custom tools |

## Documentation Structure

```
docs/
├── TOOLS.md                    # Main documentation
└── tools/
    ├── README.md              # This file
    ├── QUICK_REFERENCE.md     # Quick lookup guide
    ├── McpTool.md             # Interface documentation
    ├── AddDocumentTool.md     # Tool documentation
    ├── SemanticSearchTool.md  # Tool documentation
    └── SmartSearchTool.md     # Tool documentation
```

## Quick Links

### For Tool Users
- **Getting Started:** [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
- **API Reference:** [../TOOLS.md](../TOOLS.md)
- **Detailed Tool Info:** See individual .md files

### For Tool Developers
- **Interface Guide:** [McpTool.md](McpTool.md)
- **Implementation Examples:** Individual tool documentation
- **Integration Guide:** [../TOOLS.md](../TOOLS.md#integration-with-indexer)

### For Knowledge Base Architects
- **Design Patterns:** [SmartSearchTool.md](SmartSearchTool.md#self-improving-knowledge-base-pattern)
- **Best Practices:** Each tool documentation
- **Workflow Examples:** [QUICK_REFERENCE.md](QUICK_REFERENCE.md#common-workflows)

## Common Use Cases

### Use Case 1: Adding Documentation
```
1. User has markdown documentation
2. Call: add_document(filename, content)
3. Result: Document is indexed and searchable
```

See: [AddDocumentTool.md](AddDocumentTool.md)

### Use Case 2: Searching Knowledge Base
```
1. User searches for information
2. Call: semantic_search(query)
3. Result: List of relevant documents with scores
```

See: [SemanticSearchTool.md](SemanticSearchTool.md)

### Use Case 3: Building Self-Improving KB
```
1. User searches with smart_search
2. If found: Returns documents
3. If not found: Returns template + suggestion
4. User creates documentation with add_document
5. Knowledge base grows and improves
```

See: [SmartSearchTool.md](SmartSearchTool.md#self-improving-knowledge-base-flow)

## Key Concepts

### Semantic Search
Vector-based similarity matching that understands meaning, not just keywords.
- **Advantages:** Handles synonyms, context-aware, flexible
- **Disadvantages:** Depends on embedding model quality

### Knowledge Base
Collection of indexed markdown documents available for search.
- Created with `add_document`
- Searched with `semantic_search` or `smart_search`

### Self-Improving Pattern
Knowledge base that grows based on user queries and needs.
- SmartSearchTool identifies missing documentation
- Users create documentation using suggested template
- New documents become searchable

### Tool Registration
Process where tools register themselves with MCP server during startup.
- Called once during server initialization
- Makes tool available to clients
- Defined via `McpTool.register(server)`

## File Organization

### Source Code Location
```
src/main/kotlin/io/aeqiocor/archivistmcp/tool/
├── McpTool.kt               # Interface
├── AddDocumentTool.kt       # Implementation
├── SemanticSearchTool.kt    # Implementation
└── SmartSearchTool.kt       # Implementation
```

### Documentation Location
```
docs/
├── TOOLS.md                 # Complete reference
└── tools/
    ├── README.md           # This file
    ├── QUICK_REFERENCE.md  # Quick lookup
    ├── McpTool.md          # Interface doc
    ├── AddDocumentTool.md  # Tool doc
    ├── SemanticSearchTool.md
    └── SmartSearchTool.md
```

## Document Formats

### KDoc Format
```kotlin
/**
 * Documentation in Kotlin doc comment format
 * 
 * @param parameter Description
 * @return Description of return value
 * @see RelatedClass
 * @throws ExceptionType When something goes wrong
 */
fun myFunction(parameter: Type): ReturnType
```

### Markdown Format
This documentation uses standard Markdown with:
- Headers for structure
- Code blocks for examples
- Tables for comparison
- Links for navigation

## Abbreviations Used

| Abbr | Full Form |
|------|-----------|
| MCP | Model Context Protocol |
| KB | Knowledge Base |
| KDoc | Kotlin Documentation |
| JSON | JavaScript Object Notation |
| REST | Representational State Transfer |

## Navigation Guide

**Start Here:** [QUICK_REFERENCE.md](QUICK_REFERENCE.md)

**Detailed Info:**
- AddDocumentTool → [AddDocumentTool.md](AddDocumentTool.md)
- SemanticSearchTool → [SemanticSearchTool.md](SemanticSearchTool.md)
- SmartSearchTool → [SmartSearchTool.md](SmartSearchTool.md)
- McpTool → [McpTool.md](McpTool.md)

**Complete Reference:** [../TOOLS.md](../TOOLS.md)

## Features Overview

### AddDocumentTool Features
- ✓ Create markdown documents
- ✓ Organize in subdirectories
- ✓ Automatic semantic indexing
- ✓ Immediate search availability
- ✗ No version control
- ✗ No access control

### SemanticSearchTool Features
- ✓ Semantic similarity search
- ✓ Natural language queries
- ✓ Relevance scoring
- ✓ Synonym tolerance
- ✗ No template on empty results
- ✗ Requires existing documents

### SmartSearchTool Features
- ✓ All SemanticSearchTool features
- ✓ Template generation
- ✓ Filename suggestions
- ✓ Self-improving pattern
- ✓ Encourages documentation
- ✗ Slightly slower than semantic_search

## Performance Characteristics

| Tool | Operation | Complexity | Notes |
|------|-----------|-----------|-------|
| AddDocumentTool | Create | O(n) | n = content size |
| AddDocumentTool | Index | O(n) | n = content size |
| SemanticSearchTool | Search | O(n log n) | n = document count |
| SmartSearchTool | Search (found) | O(n log n) | Same as semantic |
| SmartSearchTool | Search (not found) | O(n log n + t) | t = template size |

## Best Practices

### When Creating Documents
1. Use clear, descriptive filenames
2. Start with a level-1 heading
3. Include examples for technical content
4. Link to related documentation
5. Keep content current

### When Searching
1. Use natural language phrasing
2. Be specific about intent
3. Try different query approaches
4. Review top results
5. Refine if needed

### For Knowledge Base Growth
1. Monitor "not_found" results
2. Create suggested documentation
3. Cross-link related documents
4. Remove duplicate content
5. Keep content organized

## Troubleshooting

### Documents Not Found
- Check that add_document succeeded
- Try rephrasing your query
- Verify document was created

### Poor Search Results
- Use more specific queries
- Try different terminology
- Check document content quality

### Tool Errors
- Ensure all required parameters provided
- Check parameter types and formats
- Review indexer logs for details

## Additional Resources

### In This Documentation
- Tool API specifications
- Usage examples
- Integration guides
- Best practices
- Troubleshooting

### In Source Code
- Implementation details
- Error handling
- Input validation
- Integration points

### In Tests
- Usage examples
- Edge cases
- Integration scenarios
- Error conditions

## Versioning

| Component | Version | Notes |
|-----------|---------|-------|
| Project | Latest | See git tags |
| Kotlin | Latest | See build.gradle |
| MCP SDK | Latest | See build.gradle |

## Support & Feedback

### Documentation Issues
- Check if already addressed in other files
- Review Quick Reference for overview
- Check individual tool documentation

### Implementation Questions
- Review source code comments
- Check test files for examples
- Review McpTool interface

### Feature Requests
- See project repository issues
- Check project goals and roadmap
- Review architecture decisions

## Summary Table

| Tool | Purpose | Input | Output | Use When |
|------|---------|-------|--------|----------|
| **add_document** | Create docs | filename, content | {status, filename} | Adding documentation |
| **semantic_search** | Search docs | query | {query, results[]} | Finding documents |
| **smart_search** | Smart search | query | {status, results/template} | Growing KB |
| **McpTool** | Base interface | - | - | Creating tools |

## Next Steps

1. **Learn Basics:** Read [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
2. **Deep Dive:** Read [../TOOLS.md](../TOOLS.md)
3. **Tool Details:** See individual tool .md files
4. **Implement:** Use McpTool interface as base
5. **Integrate:** Register with MCP server

---

**Last Updated:** April 2026  
**Status:** Complete Documentation  
**Language:** Kotlin  
**Format:** Markdown + KDoc
