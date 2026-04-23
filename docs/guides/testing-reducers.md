# Тестирование редьюсеров

## Принципы

Тесты редьюсера проверяют **полный выход reduce()**: новый стейт И эффекты. В отличие от тестов расширений, тесты редьюсера фокусируются на **бизнес-логике** обработки сообщений.

## Тестовые утилиты

Все хелперы в `modules/core/mate` → `me.apomazkin.mate.test`.

### testReduce()

Тест одного сообщения на начальном стейте:

```kotlin
fun <STATE, MESSAGE, EFFECT> MateReducer<STATE, MESSAGE, EFFECT>.testReduce(
    initialState: STATE,
    message: MESSAGE
): ReducerResult<STATE, EFFECT>
```

### testScenario()

Тест последовательности сообщений, стейт прокидывается через каждый шаг:

```kotlin
fun <STATE, MESSAGE, EFFECT> MateReducer<STATE, MESSAGE, EFFECT>.testScenario(
    initialState: STATE,
    vararg messages: MESSAGE
): List<ReducerResult<STATE, EFFECT>>
```

### Хелперы ассертов

```kotlin
// Проверить точный стейт
result.assertState(expectedState)

// Проверить отсутствие эффектов
result.assertNoEffects()

// Проверить ровно один эффект типа T
result.assertSingleEffect<DatasourceEffect.LoadWord>()

// Проверить точное совпадение набора эффектов
result.assertEffects(setOf(DatasourceEffect.LoadQuiz))

// Проверить количество эффектов
result.assertEffectsCount(2)

// Проверить наличие эффекта типа T среди нескольких
result.assertHasEffect<DatasourceEffect.NextQuestion>()
```

### StateBuilder

Fluent API для создания сложных тестовых стейтов:

```kotlin
val state = ChatScreenState()
    .toBuilder()
    .modify { it.copy(loading = false) }
    .modify { it.copy(chat = it.chat.copy(readyToStart = true)) }
    .build()
```

## Структура тестовых файлов

Файлы тестов редьюсера организованы **по группам сообщений**, а не одним монолитным файлом:

```
src/test/java/.../mate/
    LoadingWordTest.kt
    OpenTopBarMenuTest.kt
    CloseTopBarMenuTest.kt
    DeleteWordDialogTest.kt
    WordEditTest.kt
    LexemeManagementTest.kt
    TranslationManagementTest.kt
    DefinitionManagementTest.kt
    NavigateBackTest.kt
    ShowNotificationTest.kt
    NoOperationTest.kt
```

Каждый файл тестирует **связную группу сообщений**.

## Паттерн тест-класса

```kotlin
/**
 * Test cases:
 * 1. Boundary case: ShowDropdownMenu opens menu on default state
 * 2. Standard case: ShowDropdownMenu opens menu when already closed
 * 3. Standard case: ShowDropdownMenu preserves loading state
 * 4. Standard case: ShowDropdownMenu preserves word state
 * 5. Standard case: ShowDropdownMenu preserves lexeme list
 * 6. Standard case: ShowDropdownMenu generates no effects
 */
class OpenTopBarMenuTest {

    private val reducer = WordCardReducer()

    @Test
    fun `should open menu when ShowDropdownMenu on default state`() {
        // Test case 1: Boundary case
        // Given
        val initialState = WordCardState()

        // When
        val result = reducer.testReduce(initialState, Msg.ShowDropdownMenu)

        // Then
        assertTrue(
            "topBarState.isMenuOpen should be true",
            result.state().topBarState.isMenuOpen
        )
        result.assertNoEffects("ShowDropdownMenu should produce no effects")
    }

    @Test
    fun `should preserve loading state when ShowDropdownMenu`() {
        // Test case 3: Standard case
        // Given
        val initialState = WordCardState()
            .toBuilder()
            .modify { it.copy(isLoading = true) }
            .build()

        // When
        val result = reducer.testReduce(initialState, Msg.ShowDropdownMenu)

        // Then
        assertEquals(
            "isLoading should not change",
            initialState.isLoading,
            result.state().isLoading
        )
    }
}
```

## Тестирование сообщений с эффектами

```kotlin
@Test
fun `should trigger LoadWord effect when TermLoading`() {
    // Given
    val wordId = 42L
    val initialState = WordCardState()
        .toBuilder()
        .modify {
            it.copy(wordState = it.wordState.copy(id = wordId))
        }
        .build()

    // When
    val result = reducer.testReduce(initialState, Msg.TermLoading)

    // Then
    assertTrue(
        "isLoading should be true",
        result.state().isLoading
    )
    result.assertSingleEffect<DatasourceEffect.LoadWord>(
        "Should have exactly one LoadWord effect"
    )
    val effect = result.effects().first() as DatasourceEffect.LoadWord
    assertEquals(
        "LoadWord should have correct wordId",
        wordId,
        effect.wordId
    )
}
```

## Тестирование сценариев (многошаговые flow)

```kotlin
@Test
fun `should handle complete data loading flow`() {
    // Given
    val initialState = WordCardState()

    // When
    val results = reducer.testScenario(
        initialState,
        Msg.TermLoading,
        Msg.TermLoaded(term = testTerm),
    )

    // Then
    // Шаг 1: TermLoading
    assertTrue("Step 1: should be loading", results[0].state().isLoading)
    results[0].assertSingleEffect<DatasourceEffect.LoadWord>()

    // Шаг 2: TermLoaded
    assertFalse("Step 2: should stop loading", results[1].state().isLoading)
    assertEquals("Step 2: word value", "hello", results[1].state().wordState.value)
    results[1].assertNoEffects()
}
```

## Конвенции

1. **Один тест-класс на группу сообщений.** Не один гигантский файл.
2. **Экземпляр редьюсера** создаётся один раз: `private val reducer = WordCardReducer()`
3. **Именование тестов:** `should [что происходит] when [сообщение/условие]`
4. **Всегда проверять и стейт И эффекты** в каждом тесте.
5. **Проверки иммутабельности** для полей стейта, не затронутых сообщением.
6. **testScenario()** — только для flow, которым реально нужна многошаговая проверка.
7. **Нумерация тест-кейсов** в doc-комментарии класса и в комментарии метода.
8. **Порядок:** Boundary → Standard → Edge.
9. **Сообщения ассертов < 80 символов.**

## Что тестировать

- Каждое сообщение в sealed interface
- Изменения стейта для каждого сообщения
- Корректные эффекты для каждого сообщения
- Иммутабельность не затронутых полей стейта
- Граничные случаи: пустые списки, NOT_IN_DB id, null значения
- Ветвление в редьюсере (if/else в обработке сообщений)

## Что НЕ тестировать в тестах редьюсера

- Выполнение эффект-хендлеров (тестируйте хендлеры отдельно)
- Рендеринг UI
- Жизненный цикл ViewModel
- Поведение корутин
