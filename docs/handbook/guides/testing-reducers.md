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

## Full-state assert — проверять и то, что НЕ должно измениться

Для **новых** тестов веток редьюсера: ассертить стейт целиком, строя ожидаемый от `initial` через `copy` с заявленной в сценарии дельтой — а не проверять только целевые поля + эффекты.

```kotlin
val initial = loaded(lexemes = listOf(lexeme(8L, listOf(savedCv(60L)))))
    .copy(lexemeIdPendingDelete = 8L)

val result = reducer.testReduce(initial, Msg.RemoveLexeme(8L))

assertEquals(
    initial.copy(isPendingDbOp = true, lexemeIdPendingDelete = null),
    result.state(),
)
result.assertEffects(setOf(DatasourceEffect.RemoveLexeme(7L, 8L)))
```

Всё, что сценарий не заявил в `Стейт:`-дельте, обязано остаться нетронутым. Full-state assert ловит и незаявленное изменение, и незаявленное НЕизменение: IS481 BUG-2 — `RemoveLexeme` не сбрасывал `lexemeIdPendingDelete`, а тест проверял только эффект, поэтому висящая модалка была невидима.

Две оговорки:
- **Предусловие приходит из сценария.** Full-state assert проверяет только те поля, что взведены в `initial`: если открытый диалог туда не положили, его незакрытие не поймается. Источник предусловий — правило «судьба элемента в „Стало“» (R-US-001, [user-scenarios.md](user-scenarios.md)).
- **Существующие тесты не переписываются** — они неизменяемый контракт. Правило действует для новых тестов.

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

## Тестирование навигационных эффектов

После IS471 навигация выражается через `NavigationEffect` — reducer возвращает `NavigationEffect.Back` или per-screen `XxxNavigationEffect.ExitApp/OpenYyy`. Тест проверяет именно эффект, а не флаги в state.

```kotlin
@Test
fun `NavigateBack emits Back effect without state change`() {
    val initialState = WordCardState(/* ... */)

    val result = reducer.testReduce(initialState, Msg.NavigateBack)

    result.assertSingleEffect<NavigationEffect.Back>()
    assertEquals("state should remain unchanged", initialState, result.state())
}

@Test
fun `RequestBack emits ExitApp when list is empty`() {
    val initialState = DictionaryListScreenState(dictionaries = emptyList())

    val result = reducer.testReduce(initialState, DictionaryListMsg.RequestBack)

    result.assertSingleEffect<ListNavigationEffect.ExitApp>()
}

@Test
fun `RequestBack emits Back when list is not empty`() {
    val initialState = DictionaryListScreenState(dictionaries = listOf(/* ... */))

    val result = reducer.testReduce(initialState, DictionaryListMsg.RequestBack)

    result.assertSingleEffect<NavigationEffect.Back>()
}

@Test
fun `OpenWordCard emits OpenWordCard navigation effect`() {
    val result = reducer.testReduce(DictionaryTabState(), Msg.OpenWordCard(wordId = 42L))

    result.assertSingleEffect<VocabularyNavigationEffect.OpenWordCard>()
    val effect = result.effects().first() as VocabularyNavigationEffect.OpenWordCard
    assertEquals(42L, effect.wordId)
}
```

Conditional навигация (выбор `ExitApp` vs `Back` по state) — обязательно покрыть обе ветки.

## Что тестировать

- Каждое сообщение в sealed interface
- Изменения стейта для каждого сообщения
- Корректные эффекты для каждого сообщения, **включая `NavigationEffect.Back` и per-screen `XxxNavigationEffect.*`**
- Иммутабельность не затронутых полей стейта
- Граничные случаи: пустые списки, NOT_IN_DB id, null значения
- Ветвление в редьюсере (if/else в обработке сообщений, особенно conditional навигация)
- **Trim вводимых пользователем значений.** Любое строковое значение, которое юзер набирает в поле (компонент, слово, и т.п.), при сохранении должно триммиться — это надо ПОКРЫВАТЬ тестами на каждой точке ввода:
  - ведущие/замыкающие пробелы (`"  abc  "`) → сохраняется/сравнивается trimmed (`"abc"`);
  - whitespace-only (`"   "`) → трактуется как пусто (удаление / no-op / drop pristine), а не как значение из пробелов;
  - сравнение «изменилось ли» — по trimmed с обеих сторон (и `edited`, и хранимый `origin`/`value`), иначе пробелы дают ложный «изменён».

## Что НЕ тестировать в тестах редьюсера

- Выполнение эффект-хендлеров (тестируйте хендлеры отдельно)
- Рендеринг UI
- Жизненный цикл ViewModel
- Поведение корутин
- **State-флаги навигации (`closeScreen`, `exit`)** — таких полей в state больше нет, навигация только через `NavigationEffect`

## Регрессия: баг, проскользнувший мимо тестов

Когда баг найден ПОСЛЕ реализации (ревью, прод, ручная проверка) — фиксить только через тест, и строго в таком порядке:

1. **Сначала красный тест.** До правки кода написать тест, воспроизводящий баг, и **прогнать его — убедиться, что он красный** на сломанном коде. Это единственное доказательство, что тест ловит именно этот дефект, а не тавтологически зелён. Если код уже починен — откатить фикс, показать красный, вернуть фикс.
2. **Потом фикс кода → зелёный.**

Конвенции:

- **Тест — рядом с родственными, не отдельным файлом.** Регрессионный тест кладётся в существующий тест-класс той же группы сообщений (баг в `CreateComponentValue` → `ComponentValueLifecycleTest`; баг в restore/undo → `UndoDeleteTest`). Никаких `XxxRegressionTest.kt` — иначе тесты-сироты, не привязанные к группе.
- **Баг документируется в ТЕСТЕ, не в коде.** KDoc теста описывает: что было сломано, почему проскользнуло мимо сюиты. Продакшн-код чинится **без** BUGFIX-комментариев — он просто становится корректным; журнал бага живёт в тесте (тест = документация дефекта).
- **Если корень — дыра в контракте/сценарии** (пропущен шаг или `Msg` в `NN_user_scenarios.md` / тест-дизайне), закрыть её в доке: просто дополнить корректное описание (например, добавить опущенный `Msg`), **без** пометок «⚠️ BUGFIX». Сценарий — спецификация поведения, а не changelog.

Пример (IS481): `CreateComponentValue(lexemeId=NOT_IN_DB)` уничтожал пустой черновик — баг прошёл 218 зелёных, т.к. ни один тест не подавал этот путь, а в сценарии `Msg` был опущен. Красный тест в `ComponentValueLifecycleTest` → фикс → дополнен сценарий L2.1. Аналогично `RestoreLexemeFailed` → `UndoDeleteTest`.

## Rules — машинно-проверяемые правила

### R-TR-002 — баг, найденный после реализации, фиксится red-тестом до правки кода

- **Severity:** critical
- **Applies to:** любой дефект reducer'а, обнаруженный после `business_test`/реализации (ревью, прод, ручная проверка).
- **Check:** в коммите с фиксом присутствует тест в существующем тест-классе соответствующей группы сообщений (НЕ в отдельном `*RegressionTest.kt`), KDoc которого описывает баг; продакшн-фикс **не содержит** BUGFIX-комментариев. Если тест отсутствует или вынесен в отдельный файл-сироту — **нарушение**.
- **Почему:** баг, проскользнувший мимо сюиты, означает непокрытый путь; без red→green регрессия вернётся незаметно. Отдельный файл рвёт связь «группа сообщений → её тесты».
- **Fix:** написать красный тест рядом с родственными (увидеть красный до фикса), затем чинить код; при корне в контракте — дополнить `NN_user_scenarios.md` без BUGFIX-пометок.

### R-TR-003 — новый тест ветки редьюсера ассертит стейт целиком

- **Severity:** major
- **Applies to:** новые тесты веток редьюсера (существующие — неизменяемый контракт, не переписываются).
- **Check:** тест содержит сравнение полного стейта — `assertEquals(initial.copy(<заявленная дельта>), result.state())` или `assertState(expected)` с полностью построенным ожидаемым стейтом. Ассерты только по отдельным целевым полям + эффектам — **нарушение**.
- **Почему:** точечные ассерты слепы к незаявленным изменениям и незаявленным НЕизменениям (IS481 BUG-2: `RemoveLexeme` не сбрасывал `lexemeIdPendingDelete` — тест смотрел только на эффект). Full-state assert превращает `Стейт:`-дельту сценария в железный контракт: всё незаявленное обязано остаться нетронутым.
- **Fix:** строить ожидаемый стейт от `initial` через `copy` с дельтой из сценария; предусловия (открытые диалоги и т.п.) взводить в `initial` по R-US-001.

### R-TR-001 — каждый F-NNN invariant Reducer'а из contract'а имеет парный test

- **Severity:** critical
- **Applies to:** Reducer-инварианты с явным `F-NNN` идентификатором в `business_contract.md` / `02_scope.md` — типа F138 (mutual exclusion dialog state), F124/F136 (epoch correlation / stale epoch guard), F101 (close on Removed), F030 (DictionariesLoaded не мутирует EditDialogState).
- **Check:** для каждого `F-NNN` Reducer-invariant найти в соответствующем `*ReducerTest.kt` test-метод с описательным именем включающим F-NNN либо суть инварианта (`whenCreateDialogOpen_thenOpenEditDialog_dropped` для F138; `whenStaleEpochEditResult_thenIgnored` для F136). Если test не найден — **нарушение**. Для invariants формата «X не мутирует Y» — обязательно явная assertion `assertThat(newState.editDialog).isEqualTo(prevState.editDialog)`, не «просто эмитим Msg.X без падения».
- **Почему:** Reducer-инварианты — самые хрупкие в TEA: легко добавить ветку которая случайно дёргает чужой dialog state. Без покрытия каждого F-NNN отдельным тестом регрессия пройдёт незаметно (см. R-TE-001 — частный случай этого паттерна для UseCase).
- **Fix:** на `business_test` шаге (TDD до реализации) для каждого F-NNN Reducer-инварианта написать конкретный test. Связано с R-TE-001 (testing-extensions.md) — то же правило для UseCase invariants.
