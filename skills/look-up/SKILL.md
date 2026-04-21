---
name: lookup
description: Proactively look up documentation when working with an unfamiliar library, new instruction set, or any topic where knowledge is missing or uncertain. Searches the local index first; if nothing is found, researches the internet and indexes the result for future use.
---

# LOOKUP

Skill for proactive knowledge gap resolution. Use this skill before writing code or giving advice whenever you encounter something unfamiliar or uncertain.

## When to trigger

Activate this skill when **any** of the following is true:

- You are about to use a library, framework, or API you have not used in this project before
- You encounter an instruction, pattern, or convention you are unfamiliar with
- You are unsure about the correct API, version-specific behavior, or best practice
- You lack enough context to confidently complete the task
- You recognize that your training knowledge may be outdated for the topic at hand

**Do not skip this skill** and proceed on assumptions — an incorrect guess wastes more time than a lookup.

## Input

| Field | Required | Description |
|-------|----------|-------------|
| `query` | Yes | The library name, concept, or question — free-form text |
| `module` | No | Target module if known (e.g. `payments`, `auth`). Omit to search all modules |
| `type` | No | Document type: `documentation`, `guideline`, `specification`, `tutorial`, `reference`, `recipe`. Determines the subfolder when creating a new document. Omit if unsure — default is `documentation` for fetched docs. |

## Algorithm

```
┌─────────────────────────────────────────┐
│  Trigger: knowledge gap detected        │
└──────────────────┬──────────────────────┘
                   │
                   ▼
        Call find_or_create(query)
                   │
          ┌────────┴────────┐
          │                 │
       found            not found /
          │            sampling unavailable
          │                 │
          ▼                 ▼
     Read file(s)    Follow agent instructions
     from paths      returned by find_or_create
          │                 │
          └────────┬────────┘
                   ▼
         Proceed with the task
         using retrieved context
```

### Step 1. Determine document type

Before calling any tool, decide which `type` fits the document you're looking for or about to create:

| Type | When to use |
|------|-------------|
| `documentation` | General library/framework docs, overviews |
| `guideline` | Team conventions, best practices, code style rules |
| `specification` | Technical specs, requirements, contracts |
| `tutorial` | Step-by-step learning guides |
| `reference` | API reference, configuration keys, CLI flags |
| `recipe` | Code snippets, reusable patterns, how-tos |

If the type is unclear, use `documentation`.

### Step 2. Call find_or_create

Always start with `find_or_create`:

```json
{
  "query": "<topic or library name>",
  "module": "<module name if known>",
  "type": "<document type>"
}
```

### Step 3a. Document found

Response:
```json
{"status": "found", "paths": ["/docs/documentation/some-library.md"]}
```

Read each file from `paths`. Use the content as context for the current task. Proceed.

### Step 3b. Document created via sampling

Response:
```json
{"status": "created", "path": "/docs/documentation/some-library.md"}
```

Read the created file. Use the content as context. Proceed.

### Step 3c. Sampling unavailable — follow agent instructions

Response contains a plain-text instruction block like:
```
Document not found for query: "..."

Sampling is unavailable. To create this document, follow these steps:

1. Use WebSearch to find relevant sources for "..."
2. Use WebFetch to read official documentation, GitHub READMEs, or reputable articles
3. Fill in the template below with the research results
4. Call add_document with:
   - path: "/docs/slug.md"
   - content: the filled template

Template to fill:
...
```

Execute these steps yourself:
1. `WebSearch` — find 2–3 authoritative sources
2. `WebFetch` — read each source
3. Fill the template with gathered information
4. Call `add_document` with the path, **type**, and content from the instructions
5. Read the created file and proceed with the original task

## Principles

- **Look up first, code second.** Never assume API signatures, configuration keys, or behavior.
- **One lookup per unknown.** If multiple unknowns appear in one task, resolve them all before starting implementation.
- **Trust the index.** If a document was found, use it as the primary source — it reflects this project's actual usage, not generic internet advice.
- **Freshness matters.** If the found document seems outdated (version mismatch, deprecated API), note it and proceed with WebSearch to verify.
- **Index what you learn.** When you research something manually (WebSearch + WebFetch), always persist it via `add_document` so future lookups are instant.

## Examples

### Example 1. Unfamiliar library

Task: "Add retry logic using Resilience4j"

You have not used Resilience4j in this project before → trigger LOOKUP. Type = `reference` (API/config reference).

1. `find_or_create({"query": "Resilience4j retry", "type": "reference"})` → `{"status": "created", "path": "/docs/reference/resilience4j-retry.md"}`
2. Read `/docs/reference/resilience4j-retry.md`
3. Implement retry logic using the API from the document

### Example 2. New convention

Task: "Follow the new error handling convention from the team spec"

You are unfamiliar with the spec → trigger LOOKUP. Type = `guideline` (team convention).

1. `find_or_create({"query": "error handling convention", "type": "guideline"})` → `{"status": "found", "paths": ["/docs/guideline/error-handling.md"]}`
2. Read `/docs/guideline/error-handling.md`
3. Apply the convention

### Example 3. Sampling unavailable

Type = `documentation` (general library docs).

1. `find_or_create({"query": "Arrow Either monad", "type": "documentation"})` → returns agent instructions
2. `WebSearch("Arrow Kt Either monad kotlin")`
3. `WebFetch("https://arrow-kt.io/learn/typed-errors/either/")`
4. Fill the template, call `add_document(path="/docs/documentation/arrow-either.md", type="documentation", content=...)`
5. Read `/docs/documentation/arrow-either.md`, proceed with the task

## Tools used

| Tool | Purpose |
|------|---------|
| `find_or_create` | Primary search + auto-create via sampling |
| `WebSearch` | Fallback research when sampling is unavailable |
| `WebFetch` | Read source pages during fallback research |
| `add_document` | Persist manually researched documents |
