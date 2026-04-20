# ArchivistMCP

MCP-сервер на Kotlin для семантической индексации и поиска markdown-документов.

## Технологии

| Компонент | Версия |
|-----------|--------|
| Kotlin | 2.2.21 |
| Ktor | 3.3.3 |
| MCP Kotlin SDK | 0.11.1 |
| LangChain4j + AllMiniLmL6V2 | — |
| ChromaDB | — |

## Инструменты

### `semantic_search`

Поиск документов в модуле по семантическому сходству.

| Параметр | Обязательный | Описание |
|----------|:---:|---------|
| `module` | ✓ | Имя модуля |
| `query` | ✓ | Поисковый запрос |

```json
{"module": "payments", "query": "how to authenticate"}
→ {"results": [{"path": "/payments/docs/auth.md", "score": 0.912}]}
```

### `index_document`

Создаёт markdown-документ по указанному пути и индексирует его. Модуль определяется автоматически по префиксу пути.

| Параметр | Обязательный | Описание |
|----------|:---:|---------|
| `path` | ✓ | Абсолютный путь (внутри директории модуля) |
| `content` | ✓ | Содержимое в формате Markdown |

```json
{"path": "/payments/docs/auth-guide.md", "content": "# Auth Guide\n..."}
→ {"status": "ok", "path": "/payments/docs/auth-guide.md"}
```

## Конфигурация

| Переменная | Обязательная | Описание |
|------------|:---:|---------|
| `modules_dirs` | ✓ | Директории модулей: `[/docs, /payments/docs]` |
| `tmps_dir` | ✓ | Директория с шаблонами |
| `CHROMA_URL` | — | URL ChromaDB (по умолчанию: `http://localhost:8000`) |
| `HOST_MODULES_DIRS` | — | Хостовые пути при запуске через Docker |

### Имя модуля

Выводится автоматически из пути: `/payments/docs` → `payments`, `/docs` → `docs`.

## Транспорт

```
--stdio          # для локальных агентов (по умолчанию)
--sse-server     # SSE по HTTP на порту 8080
```

## Docker

```bash
docker compose up -d   # запускает ChromaDB + ArchivistMCP
```
