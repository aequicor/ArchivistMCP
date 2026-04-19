---
name: find-doc
description: Find documentation for a given topic or library. First searches the local index via semantic_search; if nothing is found, searches the internet, generates documents from the template, and indexes them via add_document.
---

# FIND_DOC

Skill for searching and automatically populating the ArchivistMCP documentation base.

## Purpose

The skill accepts a description of what information to retrieve: free-form text, library names, technologies, frameworks, concepts, etc. The user receives relevant documentation — either from the local index or generated from internet sources and persisted to the index for future queries.

## Input

The skill accepts a free-form query. Examples:

- `"Ktor routing"` — documentation for a specific technology
- `"how to set up CI/CD in GitHub Actions"` — a natural-language question
- `"kotlinx.serialization, kotlinx.coroutines"` — a list of libraries
- `"OAuth 2.0 flow, JWT tokens"` — a set of concepts

## Algorithm

```
┌─────────────────────────────────────────────┐
│   Input query (text / libraries)            │
└────────────────────┬────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────┐
│   Step 1: semantic_search(query)            │
└────────────────────┬────────────────────────┘
                     │
              ┌──────┴──────┐
              │             │
            found        not found
              │             │
              ▼             ▼
  ┌─────────────────┐   ┌──────────────────────┐
  │ Return matched  │   │ Step 2: WebSearch    │
  │ documents       │   │         + WebFetch   │
  └─────────────────┘   └──────────┬───────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │ Step 3: Build docs   │
                        │ from                 │
                        │ docs/doc-template.md │
                        └──────────┬───────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │ Step 4: add_document │
                        │ for each generated   │
                        │ document             │
                        └──────────────────────┘
```

### Step 1. Search the local index

Call the MCP tool `semantic_search`:

```json
{
  "query": "<input query>"
}
```

**If** the response contains at least one document — return it to the user and stop. The document is already indexed; no further work is needed.

**If** the `results` array is empty — proceed to step 2.

### Step 2. Search the internet

When the local index has no match, use internet-search tools:

- `WebSearch` — to obtain a list of relevant sources
- `WebFetch` — to fetch the contents of specific pages (official docs, GitHub README, articles)

Source priority:

1. Official documentation (kotlinlang.org, ktor.io, docs.github.com, etc.)
2. Official GitHub repositories (README, wiki)
3. Reputable technical resources (Baeldung, recognized Medium authors)
4. Stack Overflow — only as a supplement

If the query contains multiple entities (for example, a list of libraries), run the search for each one separately.

### Step 3. Generate documents from the template

The template lives at [docs/doc-template.md](docs/doc-template.md):

```markdown
# {name}

**Keywords:** {keywords}

## Abstract

{abstract}

## Documentation

{documentation}
```

**Filling rules:**

| Placeholder | Content |
|-------------|---------|
| `{name}` | Name of the library / technology / concept |
| `{keywords}` | Comma-separated keywords (improves semantic retrieval) |
| `{abstract}` | Short description (2–4 sentences) — purpose, scope, use cases |
| `{documentation}` | Main body: API, code samples, links to sources |

**File-naming rules:**

- Use `kebab-case`
- `.md` extension
- For libraries — full package name (`kotlinx-serialization.md`)
- For concepts — short descriptor (`oauth2-flow.md`)
- For multi-entity queries — one file per entity

### Step 4. Index the documents

For each generated document call the MCP tool `add_document`:

```json
{
  "filename": "<file-name>.md",
  "content": "<full document content>"
}
```

After a successful call (`{"status": "ok", ...}`) the document becomes available for future queries via `semantic_search` and `smart_search`.

At the end, return the generated documents (or a summary of them) to the user.

## Tools used

| Tool | Purpose | Step |
|------|---------|------|
| `semantic_search` | Search the local index | 1 |
| `WebSearch` | Find web pages | 2 |
| `WebFetch` | Fetch page contents | 2 |
| `add_document` | Index a new document | 4 |

## Examples

### Example 1. Document already in the index

**Query:** `"Ktor routing"`

1. `semantic_search({"query": "Ktor routing"})` → `[{"filename": "ktor-routing.md", "score": 0.91}]`
2. Return `ktor-routing.md` to the user.

### Example 2. No document, single source

**Query:** `"kotlinx.serialization"`

1. `semantic_search({"query": "kotlinx.serialization"})` → `[]`
2. `WebSearch("kotlinx.serialization official documentation")` → links to github.com/Kotlin/kotlinx.serialization and kotlinlang.org
3. `WebFetch(...)` → page contents
4. Build the document:
   ```markdown
   # kotlinx.serialization

   **Keywords:** kotlin, serialization, json, protobuf, cbor, multiplatform

   ## Abstract

   Official Kotlin serialization library. Supports JSON, ProtoBuf, CBOR and other formats and works with Kotlin Multiplatform.

   ## Documentation

   ...
   ```
5. `add_document({"filename": "kotlinx-serialization.md", "content": "..."})` → `{"status": "ok", ...}`

### Example 3. Multiple libraries

**Query:** `"kotlinx.coroutines, kotlinx.serialization, ktor-client"`

1. `semantic_search` — nothing found.
2. Run a separate `WebSearch` + `WebFetch` for each library.
3. Create three documents: `kotlinx-coroutines.md`, `kotlinx-serialization.md`, `ktor-client.md`.
4. Call `add_document` three times — once per file.

## Principles

- **Fail-fast at step 1.** If the documentation already exists, do not spend resources on internet search.
- **Single source of truth — official documentation.** Third-party material is acceptable only as a supplement.
- **One document per entity.** One file — one library / concept / framework.
- **Keywords for future queries.** The `Keywords` field should include synonyms and adjacent terms — this improves recall on semantic search.
