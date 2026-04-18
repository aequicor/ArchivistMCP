# Kotlin MCP Skill

> **Версия**: 1.0
> **Статус**: Готов к использованию

Навык для разработки MCP-сервера на Kotlin с ChromaDB для индексации документов.

## Quick Start

```bash
./gradlew build    # Сборка проекта
./gradlew run      # Запуск сервера
./gradlew test     # Запуск тестов
```

## Навыки

### kotlin-mcp-core

Типичные операции с MCP SDK.

- Создание инструментов через `server.addTool()`
- Добавление ресурсов через `server.addResource()`
- Генерация промптов через `server.addPrompt()`

### kotlin-chroma

Операции с ChromaDB.

- Подключение к embedded ChromaDB
- Создание коллекций
- Добавление документов с эмбеддингами
- Семантический поиск

### kotlin-ktor

Ktor HTTP сервер.

- STDIO транспорт
- SSE транспорт
- CORS настройка

## Архитектура

```
src/main/kotlin/io/modelcontextprotocol/sample/server/
├── server.kt         # Конфигурация MCP сервера
├── main.kt           # Точка входа
├── chroma/          # ChromaDB клиент
│   └── ChromaClient.kt
├── documents/        # Парсинг документов
│   ├── DocumentParser.kt
│   ├── PdfParser.kt
│   └── TextParser.kt
├── indexing/        # Логика индексации
│   └── DocumentIndexer.kt
└── tools/            # MCP инструменты
    ├── IndexDocumentTool.kt
    ├── QueryDocumentsTool.kt
    └── ListDocumentsTool.kt
```

## Agent Commands

```
@gen-tool [name]    Генерирует MCP инструмент
@gen-parser [type]  Генерирует парсер (pdf, txt, md)
@index              Индексирует документ
@query              Выполняет семантический поиск
```

## Best Practices

1. **Результаты** — используй sealed class для типизации
2. **Coroutines** — suspend функции с Dispatchers.IO
3. **Null Safety** — safe calls и elvis операторы
4. **Tool Naming** — snake_case (index_document, query_documents)
5. **Description** — всегда добавляй описание к инструментам

## Links

- [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk)
- [Ktor Documentation](https://ktor.io)
- [ChromaDB](https://docs.trychroma.com)