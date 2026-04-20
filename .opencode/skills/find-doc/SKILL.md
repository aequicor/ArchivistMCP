---
name: find-doc
description: Find or create documentation for a given topic. Calls the find_or_create MCP tool which autonomously searches the local index and, if not found, researches the internet via AI sampling (WebSearch + WebFetch) and creates the document. Returns file paths.
---

# FIND_DOC

Skill for searching and automatically populating the ArchivistMCP documentation base.

## Purpose

The skill accepts a description of what information to retrieve and returns a list of file paths. The `find_or_create` MCP tool handles the full workflow autonomously: local index search → internet research via AI sampling → document creation and indexing.

## Input

| Field | Required | Description |
|-------|----------|-------------|
| `query` | Yes | What to search for — free-form text, library names, concepts |
| `module` | No | Which module to search in. If omitted, searches all modules |

Examples:

- `"Ktor routing"` — documentation for a specific technology
- `"how to set up CI/CD in GitHub Actions"` — a natural-language question
- `"kotlinx.serialization, kotlinx.coroutines"` — a list of libraries
- `"OAuth 2.0 flow, JWT tokens"` — a set of concepts

## Output

Always a plain list of absolute file paths, one per line:

```
/payments/docs/ktor-routing.md
/payments/docs/kotlinx-coroutines.md
```

No document content, no scores, no JSON — only paths.

## Algorithm

```
1. Call find_or_create(query=<input>, module=<module if known>)
2. Extract paths from the response and return them
```

The tool handles everything internally — no subagents, no manual orchestration needed.

## Tool used

| Tool | Purpose |
|------|---------|
| `find_or_create` | Local search + autonomous internet research + document creation |

## Tool signature

```
find_or_create(query: string, module?: string)

Response (found in index):
  {"status": "found", "paths": ["/docs/ktor-routing.md", ...]}

Response (created from internet research):
  {"status": "created", "path": "/docs/kotlinx-serialization.md"}

Response (not found — sampling unavailable):
  {"status": "not_found", "query": "...", "reason": "..."}
```

## Examples

### Example 1. Document already in the index

**Query:** `"Ktor routing"`

1. `find_or_create({"query": "Ktor routing"})` → `{"status": "found", "paths": ["/docs/ktor-routing.md"]}`
2. Return:
   ```
   /docs/ktor-routing.md
   ```

### Example 2. Module known, document exists

**Query:** `module=payments`, `query="Ktor routing"`

1. `find_or_create({"query": "Ktor routing", "module": "payments"})` → `{"status": "found", "paths": ["/payments/docs/ktor-routing.md"]}`
2. Return:
   ```
   /payments/docs/ktor-routing.md
   ```

### Example 3. New topic — tool researches automatically

**Query:** `"kotlinx.serialization"`

1. `find_or_create({"query": "kotlinx.serialization"})` — tool searches local index (not found), then uses WebSearch + WebFetch autonomously
2. Response: `{"status": "created", "path": "/docs/kotlinx-serialization.md"}`
3. Return:
   ```
   /docs/kotlinx-serialization.md
   ```

## Principles

- **One call.** The skill is a thin wrapper — all intelligence is inside the `find_or_create` tool.
- **Paths only.** Never return document content — only the list of file paths.
