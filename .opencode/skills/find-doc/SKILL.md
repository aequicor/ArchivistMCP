---
name: find-doc
description: Find documentation for a given topic or library. First searches the local index via semantic_search; if nothing is found, launches a subagent that searches the internet, generates documents from the template, and indexes them via add_document. Returns only the list of file paths.
---

# FIND_DOC

Skill for searching and automatically populating the ArchivistMCP documentation base.

## Purpose

The skill accepts a description of what information to retrieve: free-form text, library names, technologies, frameworks, concepts, etc. Returns a list of file paths — either from the local index or created by a subagent from internet sources and persisted to the index for future queries.

## Input

The skill accepts:

| Field | Required | Description |
|-------|----------|-------------|
| `query` | Yes | What to search for — free-form text, library names, concepts |
| `module` | No | Which module to search in (e.g. `payments`, `docs`). If omitted, `smart_search` searches all modules |

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
┌──────────────────────────────────────────────────┐
│   Input: query [+ optional module]               │
└─────────────────────┬────────────────────────────┘
                      │
                      ▼
         ┌─────────────────────────┐
         │   module known?         │
         └────────┬────────────────┘
                  │
         ┌────────┴──────────────┐
      Yes (use                No (use
   semantic_search)         smart_search)
         │                       │
         ▼                       ▼
 semantic_search(          smart_search(
   module, query)            query)
         │                       │
         └──────────┬────────────┘
                    │
             ┌──────┴──────┐
             │             │
           found        not found
             │             │
             ▼             ▼
   Extract paths      Launch subagent
   from results       (Steps 2–4)
         │                 │
         │                 ▼
         │         Subagent returns
         │         list of created paths
         │                 │
         └────────┬────────┘
                  ▼
         Return path list to user
```

### Step 1. Search the local index

**If the target module is known**, call `semantic_search`:

```json
{
  "module": "<module name>",
  "query": "<input query>"
}
```

**If the module is unknown**, call `smart_search` (searches across all modules):

```json
{
  "query": "<input query>"
}
```

**If** the response contains at least one result — extract `path` from every result item and return the list. Stop here.

**If** the result is empty (or `"status": "not_found"`) — proceed to Step 2.

### Step 2. Launch a subagent

When the local index has no match, delegate the internet search and document creation to a **subagent** using the `Agent` tool. The subagent is self-contained and receives everything it needs in its prompt.

Build the subagent prompt from the template below, substituting the placeholders:

```
You are a documentation fetcher for the ArchivistMCP project.

## Task

Search the internet for documentation on the following topic(s) and create one markdown
document per entity. Index each document with the add_document MCP tool.

## Query

{QUERY}

## Target module directory

{MODULE_DIR}

Place every generated file inside this directory. Construct the full path as:
  {MODULE_DIR}/{filename}.md
where the filename uses kebab-case (e.g. kotlinx-serialization.md).

## Document template

Use the following template for every document:

{TEMPLATE}

Filling rules:
- {name}          — name of the library / technology / concept
- {keywords}      — comma-separated keywords and synonyms (improves future search recall)
- {abstract}      — 2–4 sentence description: purpose, scope, use cases
- {documentation} — main body: key APIs, code samples, links to official sources

## Internet search instructions

1. Use WebSearch to find relevant sources for each entity in the query.
2. Use WebFetch to read official documentation pages, GitHub READMEs, or reputable articles.
3. Source priority:
   a. Official documentation (kotlinlang.org, ktor.io, docs.github.com, etc.)
   b. Official GitHub repositories (README, wiki)
   c. Reputable technical resources (Baeldung, recognized Medium authors)
   d. Stack Overflow — only as a supplement
4. If the query lists multiple entities, search and create a document for each one separately.

## Indexing

For each generated document call the add_document MCP tool:
  path    — the full absolute path constructed above
  content — the filled-in markdown

The module is auto-detected from the path prefix; do not specify it separately.

## Output

Return ONLY a newline-separated list of the absolute file paths that were successfully
indexed (one path per line). No explanations, no document content.
```

**Placeholder substitution rules:**

| Placeholder | Value |
|-------------|-------|
| `{QUERY}` | The original query string |
| `{MODULE_DIR}` | If `module` was specified — its directory path; otherwise the first module directory returned by the server (use the path from `smart_search`'s `suggested_filename` as a hint, or ask the server for defaults) |
| `{TEMPLATE}` | The template string from `smart_search`'s `"template"` field (already retrieved in Step 1); or load `{tmps_dir}/doc-template.md` directly if using `semantic_search` |

### Step 3. Collect and return paths

Wait for the subagent to complete. Its response is a newline-separated list of absolute paths. Return that list verbatim to the user.

## Tools used

| Tool | Purpose | Who uses it |
|------|---------|-------------|
| `semantic_search` | Search a specific module by semantic similarity | Main skill (Step 1) |
| `smart_search` | Search all modules with template fallback | Main skill (Step 1) |
| `Agent` | Spawn the internet-fetch subagent | Main skill (Step 2) |
| `WebSearch` | Find web pages | Subagent |
| `WebFetch` | Fetch page contents | Subagent |
| `add_document` | Write and index a new document | Subagent |

## Tool signatures

### semantic_search

```
Arguments (required):
  module  — name of the module to search within
  query   — natural language search query

Response (found):
  {"module": "payments", "query": "...", "results": [{"module": "...", "filename": "...", "path": "...", "score": 0.892}, ...]}

Response (error):
  {"error": "module and query are required"}
```

### smart_search

```
Arguments (required):
  query   — natural language search query

Response (found):
  {"status": "found", "query": "...", "results": [...]}

Response (not found):
  {"status": "not_found", "query": "...", "action": "add_document",
   "suggested_filename": "slug.md", "template": "..."}
```

### add_document

```
Arguments (required):
  path    — absolute path to the document file (must be within a module directory)
  content — full markdown content

Response (success):
  {"status": "ok", "path": "/payments/docs/guide.md"}

Response (error):
  {"error": "path '/tmp/x.md' is not within any configured module directory: ..."}
```

## Examples

### Example 1. Document already in the index (module known)

**Query:** `module=payments`, `query="Ktor routing"`

1. `semantic_search({"module": "payments", "query": "Ktor routing"})` → results found
2. Extract paths from results.
3. Return:
   ```
   /payments/docs/ktor-routing.md
   ```

### Example 2. Document already in the index (module unknown)

**Query:** `"Ktor routing"`

1. `smart_search({"query": "Ktor routing"})` → `{"status": "found", "results": [{"path": "/docs/ktor-routing.md", ...}]}`
2. Return:
   ```
   /docs/ktor-routing.md
   ```

### Example 3. No document, single library

**Query:** `"kotlinx.serialization"`

1. `smart_search({"query": "kotlinx.serialization"})` → `{"status": "not_found", "suggested_filename": "kotlinx-serialization.md", "template": "..."}`
2. Launch subagent with `{QUERY}="kotlinx.serialization"`, `{MODULE_DIR}="/docs"`, `{TEMPLATE}=<template from response>`.
3. Subagent performs WebSearch + WebFetch, builds document, calls `add_document`.
4. Subagent returns:
   ```
   /docs/kotlinx-serialization.md
   ```
5. Return that path to the user.

### Example 4. Multiple libraries, target module known

**Query:** `module=payments`, `query="kotlinx.coroutines, ktor-client"`

1. `semantic_search({"module": "payments", "query": "kotlinx.coroutines"})` → not found
2. `semantic_search({"module": "payments", "query": "ktor-client"})` → not found
3. Launch one subagent with `{QUERY}="kotlinx.coroutines, ktor-client"`, `{MODULE_DIR}="/payments/docs"`.
4. Subagent creates both files and returns:
   ```
   /payments/docs/kotlinx-coroutines.md
   /payments/docs/ktor-client.md
   ```
5. Return those paths to the user.

## Principles

- **Fail-fast at step 1.** If the documentation already exists, do not spend resources on internet search.
- **Subagent isolation.** The subagent is fully self-contained — it does not share context with the main skill. Pass everything it needs in the prompt.
- **Single source of truth — official documentation.** Third-party material is acceptable only as a supplement.
- **One document per entity.** One file — one library / concept / framework.
- **Keywords for future queries.** The `Keywords` field should include synonyms and adjacent terms to improve recall.
- **Path = module.** Place the file inside the correct module directory so the module is auto-detected on indexing.
- **Paths only.** Never return document content — only the list of file paths.
