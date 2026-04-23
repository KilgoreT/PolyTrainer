# Тестирование extension-функций

## Принципы

Extension-функции — **чистые трансформации стейта**. Тесты проверяют:

1. **Основную функциональность** — расширение делает то, что должно.
2. **Иммутабельность остальных полей** — все другие поля не изменились.
3. **Атомарность** — затронуты только целевые свойства.

## Файловая структура

Файлы тестов расширений:
- Имя: `*ExtTest.kt` или `*ExtTests.kt`
- Расположение: в папке `ext/` внутри тестовой директории

```
src/test/java/.../mate/ext/
    TopBarExtTest.kt
    LoadingExtTest.kt
    SnackbarExtTest.kt
    WordExtTest.kt
    LexemeExtTest.kt
    AddLexemeExtTest.kt
```

## Структура теста

### Документация класса

Каждый тест-класс начинается с нумерованного списка всех тест-кейсов:

```kotlin
/**
 * Test cases:
 * 1. Boundary case: showActionMenu sets isActionMenuOpen to true
 * 2. Boundary case: hideActionMenu sets isActionMenuOpen to false
 * 3. Standard case: showActionMenu preserves all other state fields
 * 4. Standard case: hideActionMenu preserves all other state fields
 */
class TopBarExtTest {
```

### Паттерн тестового метода

```kotlin
@Test
fun `should set isActionMenuOpen to true when showActionMenu is called`() {
    // Test case 1: Boundary case - showActionMenu sets isActionMenuOpen to true
    // Given
    val initialState = ChatScreenState(
        loading = false,
        exit = false,
        appBarState = AppBarState(isActionMenuOpen = false),
        chat = ChatState(),
        snackbarState = SnackbarState(),
    )

    // When
    val resultState = initialState.showActionMenu()

    // Then
    // Проверка основной функциональности
    assertTrue(
        "isActionMenuOpen should be true after showActionMenu()",
        resultState.appBarState.isActionMenuOpen
    )

    // Проверки иммутабельности — ВСЕ остальные свойства не должны измениться
    assertEquals(
        "loading should not change after showActionMenu()",
        initialState.loading,
        resultState.loading
    )
    assertEquals(
        "exit should not change after showActionMenu()",
        initialState.exit,
        resultState.exit
    )
    assertEquals(
        "chat should not change after showActionMenu()",
        initialState.chat,
        resultState.chat
    )
    assertEquals(
        "snackbarState should not change after showActionMenu()",
        initialState.snackbarState,
        resultState.snackbarState
    )
}
```

### Использование StateBuilder для сложных стейтов

```kotlin
@Test
fun `should clear userInput when clearUserInput is called`() {
    // Test case 5: Standard case - clearUserInput empties input
    // Given
    val initialState = ChatScreenState()
        .toBuilder()
        .modify { it.copy(loading = false) }
        .modify {
            it.copy(chat = it.chat.copy(inputState = "some text"))
        }
        .build()

    // When
    val resultState = initialState.clearUserInput()

    // Then
    assertEquals(
        "inputState should be empty after clearUserInput()",
        EMPTY_STRING,
        resultState.chat.inputState
    )

    // Проверки иммутабельности
    assertEquals(
        "loading should not change",
        initialState.loading,
        resultState.loading
    )
    assertEquals(
        "appBarState should not change",
        initialState.appBarState,
        resultState.appBarState
    )
    // ... проверить каждое другое поле
}
```

## Правила ассертов

1. **Каждое поле получает assert.** Если стейт имеет 6 полей и расширение меняет 1 — пишем 1 позитивный assert + 5 проверок иммутабельности.
2. **Сообщения до 80 символов.** Кратко, но информативно.
3. **Включать expected vs actual** в сообщения ассертов.
4. **Порядок:** сначала основная функциональность, потом проверки иммутабельности в порядке объявления полей.

## Порядок тест-кейсов

1. **Boundary cases** — минимальные входы, крайние значения, пустые стейты
2. **Standard cases** — типичное использование, happy path
3. **Edge cases** — необычные комбинации, стресс-сценарии

## Что НЕ тестировать в тестах расширений

- Бизнес-логику (это задача редьюсера)
- Сайд-эффекты (расширения чистые)
- Генерацию эффектов (расширения не производят эффекты)
- Сложные сценарии (используйте тесты редьюсера для многошаговых flow)

## Чеклист для новых тестов расширений

- [ ] Тест-класс имеет нумерованную документацию кейсов
- [ ] Каждый тест содержит номер кейса и тип в комментарии
- [ ] Структура Given/When/Then
- [ ] Основная функциональность проверена
- [ ] ВСЕ остальные поля стейта проверены на иммутабельность
- [ ] Описательные сообщения ассертов (до 80 символов)
- [ ] Имена тестов: `should [ожидание] when [условие]`
- [ ] Файл назван `*ExtTest.kt` и расположен в папке `ext/`
