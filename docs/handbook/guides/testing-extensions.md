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

## Формат описания тест-кейсов (до реализации)

Перед написанием тестов — документ с описанием кейсов. Для каждого extension:

### Таблица "до → после"

Показывает что меняется и что не должно:

```
#### showDeleteDialog(id=3, name="English")

| Поле | До | После |
|------|----|-------|
| `deleteDialogState.show` | false | **true** |
| `deleteDialogState.dictionaryId` | 0 | **3** |
| `deleteDialogState.dictionaryName` | "" | **"English"** |
| `listState` | * | не меняется |
| `formState` | * | не меняется |
```

Жирным — что изменилось. "не меняется" — что проверить на иммутабельность.

### Варианты входных данных

Для extension'ов с параметрами — таблица вариантов:

```
#### updateName(value)

| value | name → | saveButtonEnabled → |
|-------|--------|---------------------|
| `"English"` | "English" | **true** |
| `""` | "" | **false** |
| `"   "` | "   " | **false** |

Не меняются: `editingDictionaryId`, `isLanguageBound`, `selectedLanguage`
```

### Сигнатуры тестов

После таблиц — список сигнатур без реализации:

```kotlin
fun `should show dialog with data when showDeleteDialog`()
fun `should preserve other fields when showDeleteDialog`()
fun `should reset dialog when hideDeleteDialog`()
```

## Формат описания тест-кейсов для reducer'а

Для каждого Msg:

### Структура сообщения + варианты данных

```
#### Msg.SelectLanguage

data class SelectLanguage(val item: LanguageItem) : Msg

item: LanguageItem(code="es", displayName="Испанский")
```

### Таблица кейсов

```
| # | Кейс | Начальный стейт | Ожидание (state) | Effects |
|---|------|----------------|------------------|---------|
| 1 | Standard | picker open, query="исп" | selectedLanguage=item, picker closed, query="" | LoadFlagsForLanguage("es") |
| 2 | Standard | любой | name не изменился | — |
```

Видно: входные данные, что проверить в стейте, какие эффекты.

---

## Чеклист для новых тестов расширений

- [ ] Тест-класс имеет нумерованную документацию кейсов
- [ ] Каждый тест содержит номер кейса и тип в комментарии
- [ ] Структура Given/When/Then
- [ ] Основная функциональность проверена
- [ ] ВСЕ остальные поля стейта проверены на иммутабельность
- [ ] Описательные сообщения ассертов (до 80 символов)
- [ ] Имена тестов: `should [ожидание] when [условие]`
- [ ] Файл назван `*ExtTest.kt` и расположен в папке `ext/`

## Rules — машинно-проверяемые правила

### R-TE-001 — каждый F-NNN invariant из contract'а имеет парный test

- **Severity:** critical
- **Applies to:** любой UseCase / UseCaseImpl где в контракте (`business_contract.md`, KDoc интерфейса, `02_scope.md`) объявлены инварианты с явным `F-NNN` идентификатором — обычно «X не должен вызываться при условии Y», «X сравнивается ДО Y», «X происходит без обращения к Z».
- **Check:** для каждого `F-NNN` из контракта найти в соответствующем `*UseCaseImplTest.kt` test-метод с именем содержащим `F-NNN` либо описательное имя инварианта (`whenSubmitEditWithChangedTemplate_thenTemplateImmutable_andDataApiNotCalled` для F017). Если test не найден — **нарушение**. Если invariant подразумевает «X не вызывается» — test обязан использовать **verify-no-interactions** на mock'е (например `verify(exactly = 0) { lexemeApi.editComponentType(any(), any(), any(), any()) }`).
- **Почему:** без парного теста invariant из контракта легко проёбывается на implement-шаге. Sub-agent видит «зелёные тесты» = «контракт выполнен», но если test на invariant не написан — implement может реализовать упрощённо (только БД-уровень вместо UseCase + БД) и тесты всё равно зелёные. Это лже-зелёный signal — IS481 phase 2 F017 поймал только global_code_review.
- **Fix:** на `business_test` шаге (TDD до реализации) пройти по `business_contract.md` § Инварианты / `02_scope.md` § Аспекты — для каждого `F-NNN` написать конкретный test с именем содержащим F-NNN. Особенно для invariants формата «X не вызывается / без обращения к Y» — обязательно verify-no-interactions, не просто result-assertion.
