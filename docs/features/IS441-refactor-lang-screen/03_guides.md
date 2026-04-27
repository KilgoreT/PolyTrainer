# IS441. Задача 3 — Релевантные гайды

## Для тестов

### docs/guides/testing-extensions.md
Релевантен: тесты extension-функций стейта.
Ключевое:
- Файлы `*ExtTest.kt` в папке `ext/`
- Given/When/Then структура
- Проверка основной функциональности + иммутабельность ВСЕХ остальных полей
- Нумерация кейсов в doc-комментарии класса
- Имена: `should [ожидание] when [условие]`
- Порядок: Boundary → Standard → Edge

### docs/guides/testing-reducers.md
Релевантен: тесты редьюсера (обработка сообщений).
Ключевое:
- `testReduce(initialState, message)` → проверить state + effects
- `testScenario(initialState, *messages)` → многошаговые flow
- `assertNoEffects()`, `assertSingleEffect<T>()`, `assertHasEffect<T>()`
- Один тест-класс на группу сообщений, не один гигантский файл
- Всегда проверять и стейт И эффекты

### docs/guides/state-and-extensions.md
Релевантен: конвенция стейта, extension-функции.
Ключевое:
- Явные поля для каждого UI-элемента (не вычислять в composable)
- Extension-функции в State.kt
- Каждый extension — одна задача, чистая функция

## Для реализации (позже)

### docs/guides/navigation.md
Релевантен: два route, переключение.

### docs/guides/ui-patterns.md
Релевантен: AppBar виджет, AnimatedContent, три уровня виджетов.

### docs/guides/effect-handlers.md
Релевантен: DatasourceEffect, UseCase вызовы.

### docs/guides/data-layer.md
Релевантен: DAO, API контракты, маппинг.

### docs/guides/mate-framework.md
Релевантен: Mate инициализация, ReducerResult.
