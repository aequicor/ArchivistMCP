# ArchivistMCP

MCP-server на Kotlin для индексации документов.

## Технологии

| Компонент | Версия |
|-----------|--------|
| Kotlin | 2.2.21 |
| Ktor | 3.3.3 |
| MCP Kotlin SDK | 0.11.1 |

## Транспорт

- **STDIO** — для локальных агентов
- **SSE** — для удалённого подключения (порт 8080)

## Конфигурация (переменные среды)

| Переменная | Обязательная | Описание | Пример |
|------------|:---:|---------|--------|
| `modules_dirs` | ✓ | Список директорий модулей для индексации | `[/docs, /payments/docs]` |
| `tmps_dir` | ✓ | Директория с шаблонами документов | `/templates` |
| `INDEX_PATH` | — | Путь к файлу индекса (по умолчанию: `{первый модуль}/index/embeddings.json`) | `/var/index/embeddings.json` |
| `WORKSPACE_DIR` | — | Рабочая директория | `/workspace` |

### Имя модуля

Имя модуля выводится автоматически из пути:
- `/docs` → `docs`
- `/payments/docs` → `payments` (берётся родительская директория, если имя папки `docs`)

## Инструменты

### `semantic_search`

Поиск документов в конкретном модуле по семантическому сходству.

| Параметр | Обязательный | Описание |
|----------|:---:|---------|
| `module` | ✓ | Имя модуля для поиска |
| `query` | ✓ | Поисковый запрос на естественном языке |

```json
// Запрос
{"module": "payments", "query": "how to authenticate"}

// Ответ
{"module": "payments", "query": "how to authenticate",
 "results": [{"module": "payments", "filename": "auth.md", "path": "/payments/docs/auth.md", "score": 0.912}]}
```

### `smart_search`

Поиск по всем модулям. При отсутствии результатов возвращает шаблон и рекомендует создать документ через `add_document`.

| Параметр | Обязательный | Описание |
|----------|:---:|---------|
| `query` | ✓ | Поисковый запрос на естественном языке |

```json
// Запрос
{"query": "kotlin coroutines"}

// Ответ (найдено)
{"status": "found", "query": "kotlin coroutines", "results": [...]}

// Ответ (не найдено)
{"status": "not_found", "query": "kotlin coroutines",
 "action": "add_document", "suggested_filename": "kotlin-coroutines.md", "template": "..."}
```

### `add_document`

Создаёт markdown-документ по указанному пути и индексирует его. Модуль определяется автоматически по префиксу пути.

| Параметр | Обязательный | Описание |
|----------|:---:|---------|
| `path` | ✓ | Абсолютный путь к файлу (должен находиться в одной из директорий модулей) |
| `content` | ✓ | Содержимое документа в формате Markdown |

```json
// Запрос
{"path": "/payments/docs/auth-guide.md", "content": "# Auth Guide\n..."}

// Ответ
{"status": "ok", "path": "/payments/docs/auth-guide.md"}
```

## Скиллы

- [FIND_DOC](FIND_DOC.md) — поиск документации: локально → интернет → индексация в базу
