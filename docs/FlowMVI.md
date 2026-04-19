# FlowMVI

**Keywords:** Kotlin Multiplatform, MVI, Architecture, State Management, Coroutines

## Abstract

FlowMVI — это Kotlin Multiplatform архитектурный фреймворк, основанный на паттерне MVI (Model-View-Intent). Добавляет систему плагинов для предотвращения крашей, обработки ошибок, повторного использования кода, аналитики, отладки и мониторинга производительности. Является KMP-совместимым и не требует внешних зависимостей, кроме корутин.

## Documentation

## Что такое FlowMVI?

FlowMVI — это архитектурный фреймворк для Kotlin, который предоставляет инфраструктуру для построения приложений с использованием паттерна MVI. В отличие от традиционных архитектурных фреймворков, FlowMVI не диктует структуру кода, а предоставляет инструменты для:

- **Система плагинов** — переиспользование любой бизнес-логики
- **Обработка ошибок** — автоматическое управление ошибками
- **Аналитика и мониторинг** — 3 строки кода для сбора метрик
- **Thread-safety** — достигается автоматически
- **Сохранение состояния** — встроенная поддержка
- **Тестирование** — DSL для декларативного тестирования

## Быстрый старт

### 1. Определение контракта

```kotlin
sealed interface State : MVIState {
    data object Loading : State
    data class Content(val items: List<String>) : State
    data class Error(val message: String) : State
}

sealed interface Intent : MVIIntent {
    data object Load : Intent
    data class UpdateItem(val index: Int, val value: String) : Intent
}

sealed interface Action : MVIAction {
    data class ShowSnackbar(val message: String) : Action
}
```

### 2. Объявление бизнес-логики

```kotlin
class MyStore : MVIStore<State, Intent, Action> by MVIStore.of(
    initial = State.Loading,
    reducer = { state, intent ->
        when (intent) {
            is Intent.Load -> State.Content(listOf("Item 1", "Item 2"))
            is Intent.UpdateItem -> state.reduce { copy(items = items.updated(intent.index, intent.value)) }
        }
    },
    actor = { action ->
        when (action) {
            is Action.ShowSnackbar -> showSnackbar(action.message)
        }
    }
)
```

### 3. Использование в UI

```kotlin
@Composable
fun MyScreen(store: MyStore) {
    MVIView(store) { state ->
        when (state) {
            is State.Loading -> LoadingIndicator()
            is State.Content -> ContentList(state.items)
            is State.Error -> ErrorMessage(state.message)
        }
    }
}
```

## Зависимости

```kotlin
// gradle/libs.versions.toml
[versions]
flowmvi = "3.2.1"

[libraries]
flowmvi-core = { module = "pro.respawn.flowmvi:core", version.ref = "flowmvi" }
flowmvi-test = { module = "pro.respawn.flowmvi:test", version.ref = "flowmvi" }
flowmvi-compose = { module = "pro.respawn.flowmvi:compose", version.ref = "flowmvi" }
```

## Тестирование

FlowMVI предоставляет DSL для тестирования:

```kotlin
store.test {
    // Отправка интентов
    Intent.Load
    
    // Проверка состояния
    awaitState() shouldEqual State.Content(listOf("Item 1"))
    
    // Проверка действий
    awaitAction() shouldEqual Action.ShowSnackbar("Loaded!")
}
```

## Ссылки

- **Репозиторий:** https://github.com/respawn-app/FlowMVI
- **Документация:** https://opensource.respawn.pro/FlowMVI/
- **Quickstart:** https://opensource.respawn.pro/FlowMVI/getting-started
- **Версия:** 3.2.1 (последний стабильный релиз)