# ArchivistMCP

> semantic search · document indexing · MCP server · vector embeddings · ChromaDB · Kotlin

> An MCP server that semantically indexes and retrieves Markdown documentation across multiple project modules. Agents can search, fetch, and create documents using natural language — with fallback to external MCP servers when a document is not found locally.

## How to configure opencode and docker

**1. Build the Docker image**

```bash
docker build -t archivist-mcp:latest .
```

**2. Create `_LocalProjectAbdsoulePath.txt`** with the absolute path to your project root (no trailing slash):

```
C:/Users/you/IdeaProjects/MyProject
```

**3. Configure `opencode.json`** in your project root:

```json
{
  "mcp": {
    "archivist-mcp": {
      "type": "local",
      "command": [
        "docker", "run", "--rm", "--interactive",
        "--volume", "{file:./_LocalProjectAbdsoulePath.txt}/docs:/app/docs",
        "--env", "modules_dirs=[/app/docs]",
        "--env", "HOST_MODULES_DIRS=[{file:./_LocalProjectAbdsoulePath.txt}/docs]",
        "--env", "tmps_dir=/app/docs",
        "--env", "CHROMA_URL=http://host.docker.internal:8000",
        "--add-host", "host.docker.internal:host-gateway",
        "archivist-mcp:latest"
      ]
    }
  }
}
```

**4. Start ChromaDB** (required for vector storage):

```bash
docker compose up -d chroma
```

> ChromaDB runs on `localhost:8000` by default. Version is pinned to `0.5.23` — LangChain4j 0.36.0 uses the v1 API which is not supported in ChromaDB 0.6+.

**Environment variables**

| Variable | Required | Description |
|---|:---:|---|
| `modules_dirs` | ✓ | Module doc directories: `[/app/docs, /app/payments/docs]` |
| `tmps_dir` | ✓ | Templates directory |
| `CHROMA_URL` | — | ChromaDB URL (default: `http://localhost:8000`) |
| `HOST_MODULES_DIRS` | — | Host-side paths for Docker path translation |

**Multi-module setup** — separate modules with commas:

```json
"modules_dirs=[/app/docs, /app/payments/docs, /app/auth/docs]"
"HOST_MODULES_DIRS=[/project/docs, /project/payments/docs, /project/auth/docs]"
```

Module names are derived automatically from the path: `/payments/docs` → `payments`.

## How to use it

**Find documentation by topic**

```
Find docs about authentication flow in the payments module
```

```
Search for how retry logic is implemented
```

**Get a specific document**

```
Get the document named "api-reference"
```

```
Fetch /app/docs/onboarding/setup.md
```

**Create and index a new document**

```
Create a document at /app/docs/auth/oauth-guide.md explaining the OAuth2 flow we use
```

```
Write documentation for the payment retry mechanism and save it to /app/docs/payments/retry.md
```

**Cross-module search with shared docs**

```
Search all modules for rate limiting documentation
```

```
Find examples of error handling across the codebase docs
```
