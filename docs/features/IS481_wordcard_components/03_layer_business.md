# Слой Business — план изменений

> **Гайды:** `state-modeling.md` (sum-типы, impossible states — §3/§4), `state-and-extensions.md` (computed properties, extension'ы — §4), `reducer-patterns.md` (ветки, guard, R-RP-010 — §6), `messages.md` (имена Msg — §2), `effect-handlers.md` (handler/FlowHandler — §5/§7), `data-layer.md` (UseCase trim — §1), `testing-reducers.md` + `testing-extensions.md` (§8), `logging.md` (§5/§7), `naming.md` (везде).

Центральный слой. Полная унификация: `LexemeState.components: List<ComponentValueState>`. Все translation/definition-mirror ветки удаляются. Тесты переписываются под обобщённый CRUD.

Файлы под изменение:
- `:modules:screen:wordcard/.../deps/WordCardUseCase.kt` — **REWRITE интерфейс**.
- `app/.../di/module/wordCard/WordCardUseCaseImpl.kt` — **REWRITE реализация**.
- `:modules:screen:wordcard/.../mate/State.kt` — **REWRITE**.
- `:modules:screen:wordcard/.../mate/Message.kt` — **REWRITE**.
- `:modules:screen:wordcard/.../mate/DatasourceEffectHandler.kt` — **REWRITE**.
- `:modules:screen:wordcard/.../mate/WordCardReducer.kt` — **REWRITE**.
- `:modules:screen:wordcard/.../mate/UiEffect.kt` — **MODIFY** (+ retry snackbar).
- `:modules:screen:wordcard/.../mate/AvailableComponentTypesFlowHandler.kt` — **NEW**.
- `:modules:screen:wordcard/.../WordCardViewModel.kt` — **MODIFY** (+ inject FlowHandler).
- Тесты — `:modules:screen:wordcard/src/test/...` — REWRITE (см. § 8).

---

## §1. UseCase API (`WordCardUseCase`)

### 1.1 Финальный интерфейс (REWRITE)

```kotlin
package me.apomazkin.wordcard.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {

    // ===== Word =====
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean

    // ===== Lexeme CRUD =====
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): RemoveLexemeResult?

    /**
     * Создаёт новую лексему с первым компонентом. Atomic INSERT lexeme + write_quiz +
     * первый component_value через CoreDbApi.LexemeApi.addLexemeWithComponents
     * (compound, MIN-9). Используется когда `lexemeId == NOT_IN_DB` и юзер commit'ит
     * первый component.
     *
     * @return обновлённая Lexeme или null при ошибке (включая отсутствие type в словаре).
     */
    suspend fun addLexemeWithComponent(
        wordId: Long,
        dictionaryId: Long,
        ref: ComponentTypeRef,
        data: TemplateValues,
    ): Lexeme?

    /**
     * Atomic compound INSERT lexeme + N component_values (для undo cascade-delete).
     * C3: принимает полный `snapshot: Lexeme` (тот же, что несёт Effect — для A17 retry
     * round-trip). Маппинг `snapshot.components → List<Pair<ref, data>>` — внутри impl.
     */
    suspend fun restoreLexemeWithComponents(
        wordId: Long,
        dictionaryId: Long,
        snapshot: Lexeme,
    ): Lexeme?

    // ===== ComponentValue CRUD =====

    /**
     * INSERT нового component_value к существующей лексеме.
     * @return Pair(обновлённая Lexeme, новый componentValueId).
     *         null при ошибке (включая type soft-deleted / cardinality violation).
     */
    suspend fun addComponentValue(
        lexemeId: Long,
        componentTypeId: ComponentTypeId,
        data: TemplateValues,
    ): AddComponentValueResult?

    /**
     * UPDATE existing component_value. `lexemeId` передаётся caller'ом
     * (известен из Effect/Msg) → re-read Lexeme через `getLexemeById(lexemeId)`.
     * Симметрия с `deleteComponentValue`. A1: reverse-lookup удалён.
     */
    suspend fun updateComponentValue(
        componentValueId: ComponentValueId,
        lexemeId: Long,
        data: TemplateValues,
    ): Lexeme?

    /**
     * DELETE component_value. Cascade-decision: если у лексемы 0 оставшихся
     * components → caller (UseCase impl) делает `deleteLexeme` и возвращает Cascade.
     */
    suspend fun deleteComponentValue(
        componentValueId: ComponentValueId,
        lexemeId: Long,
    ): RemoveComponentResult?

    // ===== Component types stream =====

    fun flowAvailableComponentTypes(dictionaryId: Long): Flow<List<ComponentType>>
}

/**
 * Возврат `addComponentValue` — детерминированная квитанция на pristine.
 * Reducer match'ит `pristineKey ↔ newComponentValueId`.
 */
data class AddComponentValueResult(
    val lexeme: Lexeme,
    val newComponentValueId: ComponentValueId,
)

sealed interface RemoveLexemeResult {
    /** Снимок удалённой лексемы для undo. */
    data class Removed(val removedLexeme: Lexeme) : RemoveLexemeResult
}

sealed interface RemoveComponentResult {
    /** Удалён компонент, лексема осталась. */
    data class ComponentRemoved(val lexeme: Lexeme) : RemoveComponentResult
    /** Последний компонент — лексема удалена (snapshot для undo). */
    data class LexemeCascadeRemoved(val removedLexeme: Lexeme) : RemoveComponentResult
}
```

**УДАЛЯЕТСЯ из интерфейса** (текущие методы):
- `addLexemeTranslation`, `deleteLexemeTranslation`, `RemoveTranslationResult`.
- `addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`, `deleteDefinitionComponent`.
- `getComponentTypes` (suspend snapshot) — заменяется `flowAvailableComponentTypes`.
- `restoreLexeme(wordId, translation, definition)` — заменяется `restoreLexemeWithComponents`.

### 1.2 Реализация (REWRITE — `WordCardUseCaseImpl`)

Ключевые моменты (полные тела — на этапе imp по реальному коду):

- `addLexemeWithComponent(wordId, dictionaryId, ref, data)` → `lexemeApi.addLexemeWithComponents(wordId, dictionaryId, listOf(ref to data))?.let { id -> lexemeApi.getLexemeById(id)?.toDomain() }`. Возвращает null если тип не найден в словаре (внутри `addLexemeWithComponents` логирует и возвращает null) — UseCase прокидывает наружу как null.
  - **try/catch (симметрично `addComponentValue`).** `addLexemeWithComponents` бросает те же `IllegalStateException` (`check(type.removedAt==null)` + cardinality). Обернуть → `null` на исключении → `OperationFailed`. В WordCard-потоке недостижимо (anchor из активного типа), но защита симметрична — не падать.
- `addComponentValue(lexemeId, typeId, data)` → `val newId = lexemeApi.addComponentValue(lexemeId, typeId.id, data); val lexeme = lexemeApi.getLexemeById(lexemeId)?.toDomain() ?: return null; AddComponentValueResult(lexeme, ComponentValueId(newId))`.
  - **Обязателен `try/catch` (как у `updateComponentValue`).** Реальный `CoreDbApiImpl.addComponentValue` бросает `IllegalStateException` в ДВУХ местах: `check(type.removedAt == null)` (soft-deleted тип) и `insertSingleSafe` (cardinality violation для non-multi). Тело обернуть в `try { … } catch (e: Exception) { null }` → исключение → `null` → handler эмитит `OperationFailed`. Покрыто тестом T6 (`addComponentValue_exception_returns_null`). НЕ полагаться на «вернёт 0» — будет throw.
- `updateComponentValue(cvId, lexemeId, data)` → `val updated = lexemeApi.updateComponentValue(cvId.id, data); if (updated <= 0) return null; lexemeApi.getLexemeById(lexemeId)?.toDomain()`. `lexemeId` приходит параметром (из `Effect.UpsertComponentValue.lexemeId`) — reverse-lookup не нужен (A1).
  - **B1.5 — defensive DB-инвариант (в потоке WordCard НЕДОСТИЖИМ).** `lexemeApi.updateComponentValue` бросает `IllegalStateException`, если тип значения soft-deleted (`CoreDbApiImpl` line ~412: `check(type.removedAt == null)`). В WordCard этот случай **недостижим**: удаление типа каскадно soft-удаляет его значения (`componentValueDao.softDeleteByTypeId`), а маппер их отфильтровывает — значение с удалённым типом на карточку не грузится (см. 09 A9; экраны Android последовательны, одновременности нет). Тем не менее метод оборачиваем в `try/catch` → `null` → `OperationFailed` как дешёвую страховку data-слоя. **НЕ строить на этом пользовательский сценарий.**
- `deleteComponentValue(cvId, lexemeId)` → snapshot `val before = lexemeApi.getLexemeById(lexemeId)?.toDomain() ?: return null`; `val remaining = lexemeApi.deleteComponentValue(cvId.id)`; если `remaining > 0` → re-read и вернуть `ComponentRemoved(updated)`; иначе `lexemeApi.deleteLexeme(lexemeId)` + `LexemeCascadeRemoved(before)`.
- `deleteLexeme(wordId, lexemeId)` → snapshot before delete → `lexemeApi.deleteLexeme(lexemeId)` → `Removed(snapshot)`.
- `flowAvailableComponentTypes(dictId)` → `lexemeApi.flowTypesForDictionary(dictId).map { it.map { it.toDomain() } }`.
- `restoreLexemeWithComponents(wordId, dictId, snapshot)` → `val components = snapshot.components.map { it.type.toRef() to it.data }; lexemeApi.addLexemeWithComponents(wordId, dictId, components)?.let { id -> lexemeApi.getLexemeById(id)?.toDomain() }`. C3: маппинг snapshot→pairs внутри.
  - **try/catch ОБЯЗАТЕЛЕН (A17).** Если `addLexemeWithComponents` **бросит** (а не вернёт null), исключение уйдёт в generic catch-all хендлера → `OperationFailed(generic)`, минуя `RestoreLexemeFailed(snapshot)` → retry-снек с snapshot будет потерян. Обернуть → `null` → хендлер эмитит `RestoreLexemeFailed(effect.snapshot)`.
- **Trim (G3 / T18) — эскизы выше его опускают (полные тела на impl), но он ОБЯЗАТЕЛЕН.** Перед записью UseCase нормализует текст: `addComponentValue` / `updateComponentValue` триммят `TextValues` (`textValuesOf(data.asText()?.trim().orEmpty())`). Defense-in-depth: payload эффекта уже trimmed (G3), UseCase триммит повторно (idempotent). T18 (`addComponentValue_trims_before_write`) пинит запись trimmed-значения.

**Удаляется**: `resolveCurrentDictionaryId` через `prefsProvider` — `dictionaryId` приходит **параметром** из state (Reducer знает `loaded.dictionaryId`). `resolveDictionaryIdForLexeme` тоже удаляется. Это закрывает текущий TODO «replace with DAO query» — мы просто избегаем lookup'а.

`prefsProvider` остаётся в ctor только если других callsite'ов в UseCase нет — иначе удалить из ctor (проверить grep'ом на imp).

---

## §2. Msg (REWRITE — `Message.kt`)

```kotlin
sealed interface Msg {
    // ----- Top bar / dialogs -----
    data object OpenTopBarMenu : Msg
    data object CloseTopBarMenu : Msg
    data object OpenDeleteWordDialog : Msg
    data object CloseDeleteWordDialog : Msg
    data class RemoveWord(val wordId: Long) : Msg

    // ----- Word edit (UNCHANGED) -----
    data object EnterWordEditMode : Msg
    data class UpdateWordInput(val value: String) : Msg
    data object CommitWordChanges : Msg

    // ----- Lexeme lifecycle -----
    data object CreateLexeme : Msg
    data class OpenDeleteLexemeDialog(val lexemeId: Long) : Msg
    data object CloseDeleteLexemeDialog : Msg
    data class RemoveLexeme(val lexemeId: Long) : Msg

    // ----- ComponentValue (NEW — generic) -----

    /** Тап на chip из ChipsRow → создать pristine компонент. */
    data class CreateComponentValue(
        val lexemeId: Long,
        val componentTypeId: ComponentTypeId,
    ) : Msg

    /** Inline edit input change. Key — pristineKey или ComponentValueId. */
    data class UpdateComponentValueInput(
        val lexemeId: Long,
        val key: ComponentValueKey,
        val value: String,
    ) : Msg

    /** Открыть edit-mode для saved value (tap по value в view-mode). */
    data class EnterComponentValueEditMode(
        val lexemeId: Long,
        val key: ComponentValueKey,
    ) : Msg

    /** Commit blur — autosave. */
    data class CommitComponentValueEdit(
        val lexemeId: Long,
        val key: ComponentValueKey,
    ) : Msg

    /** Trash button. */
    data class RemoveComponentValueRequested(
        val lexemeId: Long,
        val key: ComponentValueKey,
    ) : Msg

    // ----- Datasource feedback (NEW) -----

    /** WordLoaded — payload без componentTypes (приходят через FlowHandler). */
    data class WordLoaded(val word: Term) : Msg
    data object WordNotFound : Msg
    data class RefreshWord(val word: Term) : Msg

    /**
     * Полный rebuild components одной лексемы после DAO write.
     * Используется для update + delete-non-last.
     */
    data class RefreshLexemeComponents(
        val lexemeId: Long,
        val components: List<ComponentValue>,
    ) : Msg

    /**
     * Квитанция: pristine был промочен в БД как реальный componentValueId.
     * Reducer заменяет identity на Saved(newId).
     */
    data class ComponentValueInserted(
        val lexemeId: Long,
        val pristineKey: Long,
        val newCvId: ComponentValueId,
    ) : Msg

    /**
     * NOT_IN_DB лексема стала реальной (после commit первого — «якорного» — component).
     * `anchorPristineKey` — pristineKey того pristine, что ушёл в создающий INSERT
     * (handler знает его из `effect.pristineKey`, НЕ из state). Reducer находит
     * единственную NOT_IN_DB лексему, исключает якорь ПО КЛЮЧУ из выживших pristine
     * (promoted уже содержит его как Saved), домерживает остальных и реэмитит для них
     * upsert. См. §6.2.1 / B2 (F3/F4-fix: исключение по ключу, без пересчёта selector'а).
     */
    data class LexemeDraftPromoted(
        val newLexeme: Lexeme,
        val anchorPristineKey: Long,
    ) : Msg

    /**
     * Cascade-delete: удалён последний component → удалена лексема.
     * Snapshot для undo. Reducer убирает из lexemeList + emit ShowSnackbarWithUndo.
     */
    data class LexemeCascadeRemoved(
        val removedLexeme: Lexeme,
    ) : Msg

    /**
     * Full-delete лексемы через DeleteLexemeButton.
     */
    data class LexemeRemoved(
        val removedLexeme: Lexeme,
    ) : Msg

    /** Available component types stream — FlowHandler emit. */
    data class ComponentTypesLoaded(val types: List<ComponentType>) : Msg
    data class ComponentTypesLoadFailed(val error: Throwable) : Msg
    data object RetryLoadComponentTypes : Msg

    // ----- Undo (NEW) -----

    /**
     * UndoRestore: восстановить лексему c сохранённым snapshot'ом components.
     * Применяется и к cascade-delete (LexemeCascadeRemoved), и к full-delete (LexemeRemoved).
     */
    data class UndoRestoreLexeme(
        val lexeme: Lexeme,
    ) : Msg

    /**
     * A17: restore (re-INSERT) упал → вернуть snapshot для retry-снека.
     * snapshot цел (round-trip через RestoreLexemeWithComponents.snapshot).
     */
    data class RestoreLexemeFailed(
        val snapshot: Lexeme,
    ) : Msg

    // ----- Errors -----
    /**
     * F7: единая точка обработки ошибки datasource-операции. Reducer-ветка
     * ОБЯЗАНА снять `isPendingDbOp=false` (иначе экран залипает после неудачного
     * INSERT/UPDATE/DELETE — особенно при промоушене NOT_IN_DB) + эмитнуть
     * `UiEffect.ShowErrorSnackbar`. Заменяет прежний `ShowError` (который не снимал pending).
     */
    data class OperationFailed(@StringRes val messageRes: Int) : Msg
    data object NavigateBack : Msg
    data object NoOperation : Msg
}
```

**УДАЛЯЮТСЯ** (вся mirror Translation/Definition группа):
- `CreateTranslation / UpdateTranslationInput / EnterTranslationEditMode / CommitTranslationEdit / RemoveTranslation / RefreshTranslation / TranslationDeleted / UndoRemoveTranslation`.
- `CreateDefinition / UpdateDefinitionInput / EnterDefinitionEditMode / CommitDefinitionEdit / RemoveDefinition / RefreshDefinition / DefinitionDeleted / UndoRemoveDefinition`.
- `LexemeCascadeRemovedWithUndo`, `UndoRemoveLexeme`, `RefreshLexemeList`.

**Сигнатура `WordLoaded` упрощается:** в реальном коде сейчас `WordLoaded(word, componentTypes)` — F1 fix передавал `componentTypes` синхронно через snapshot. После рефакторинга `componentTypes` приходит через **отдельный FlowHandler** (`Msg.ComponentTypesLoaded`), поэтому `WordLoaded(word)` без второго поля.

---

## §3. ComponentValueKey + CommitOutcome — NEW

### 3.1 `ComponentValueKey.kt` (NEW в `mate/`)

```kotlin
package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentValueId

/**
 * Identity для component_value в state. Pristine — pre-INSERT, Saved — после квитанции.
 * Identity flip Pristine → Saved выполняется reducer'ом на ComponentValueInserted.
 */
sealed interface ComponentValueKey {
    @JvmInline
    value class Pristine(val pristineKey: Long) : ComponentValueKey

    @JvmInline
    value class Saved(val componentValueId: ComponentValueId) : ComponentValueKey
}
```

### 3.2 `CommitOutcome.kt` (NEW в `mate/`)

```kotlin
package me.apomazkin.wordcard.mate

/**
 * Decision на commit одной component-value записи (parity с текущим
 * `commitTranslationEdit`/`commitDefinitionEdit` 4-веточным when).
 */
sealed interface CommitOutcome {
    /** Ничего не делать (close edit). */
    data object NoOp : CommitOutcome
    /** Локально удалить (pristine + empty edited). */
    data object LocalRemove : CommitOutcome
    /** Pessimistic remove из БД (saved + empty edited & non-empty origin). */
    data object PessimisticRemove : CommitOutcome
    /** Update в БД (text — финальный value). */
    data class Update(val text: String) : CommitOutcome
}

internal fun ComponentValueState.commitDecision(): CommitOutcome {
    if (!isEdit) return CommitOutcome.NoOp
    val trimmed = edited.trim()
    return when {
        trimmed.isEmpty() && origin.isEmpty() -> CommitOutcome.LocalRemove
        trimmed.isEmpty() && origin.isNotEmpty() -> CommitOutcome.PessimisticRemove
        trimmed == origin -> CommitOutcome.NoOp
        else -> CommitOutcome.Update(trimmed)
    }
}
```

> **Contract-gap (trim) — ЕДИНОЕ ПРАВИЛО.** Текст нормализуется через `.trim()` в ОДНОЙ наблюдаемой точке: payload эмитимого эффекта всегда `textValuesOf(trimmed)`. `CommitOutcome.Update(text)` несёт **уже trimmed** text. Реэмит в `LexemeDraftPromoted` (§6.2.1) тоже trim'ит. `UseCaseImpl` trim'ит повторно перед записью (defense-in-depth, data-layer.md) — это idempotent. Поэтому TDD-тесты ассертят `data = textValuesOf("<trimmed>")` детерминированно. Решение по ветке (Update/Remove/NoOp) — по `trimmed`; сравнение с `origin` — `trimmed == origin`.

---

## §4. State (REWRITE — `State.kt`)

### 4.1 Финальный shape

```kotlin
@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    /**
     * Flush-on-back (маркеры). Пользователь нажал «назад» — мы коммитим открытую
     * правку и НЕ навигируем, пока есть незавершённые записи (`hasInFlightCommits`).
     * true → UI показывает лоадер и блокирует ввод; reducer эмитит `NavigationEffect.Back`
     * как только летящих записей не осталось. Ошибка записи (`OperationFailed`) сбрасывает
     * `isExiting=false` → остаёмся на экране, показываем снек.
     */
    val isExiting: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val lexemeIdPendingDelete: Long? = null,
    /** Available component types для текущего словаря. Driver для ChipsRow. */
    val availableComponentTypes: List<ComponentType> = emptyList(),
    /** Reducer-counter для генерации уникальных pristine identity. */
    val nextPristineKey: Long = 1L,
) {
    // H-14: оба УЖЕ существуют в текущем State.kt (не новое) — сохраняем как есть.
    /** computed: в lexemeList есть несохранённый черновик (single-draft инвариант). */
    val isCreatingLexeme: Boolean get() = lexemeList.any { it.id == NOT_IN_DB }

    /** computed: можно создать новую лексему (нет черновика и нет идущей DB-операции). */
    val canAddLexeme: Boolean get() = !isCreatingLexeme && !isPendingDbOp

    /**
     * computed (flush-on-back, маркеры): есть хоть одна незавершённая (in-flight) запись
     * компонента — по флагу `isCommitting`. Покрывает Update/Add/CreateLexeme/реэмит survivors.
     * Пока true и `isExiting` — навигацию назад держим; когда false — выходим.
     */
    val hasInFlightCommits: Boolean
        get() = lexemeList.any { l -> l.components.any { it.isCommitting } }
}

@Stable
sealed interface WordState {
    data object NotLoaded : WordState
    data class Loaded(
        val id: Long,
        val dictionaryId: Long,         // NEW — для лукапа typeId в reducer
        val added: Date,
        val value: String,
        val isEditMode: Boolean = false,
        val edited: String = "",
        val showWarningDialog: Boolean = false,
    ) : WordState
}

@Stable
data class LexemeState(
    val id: Long = NOT_IN_DB,
    val components: List<ComponentValueState> = emptyList(),
) {
    /**
     * Гайд-правка (state-and-extensions.md принцип 6, R-RP-010): derived-логика —
     * computed property на State, НЕ вычисление в composable. UI читает готовое.
     * typeId уже добавленных non-multi компонентов — для скрытия их chip'ов в ChipsRow.
     */
    val addedNonMultiTypeIds: Set<ComponentTypeId>
        get() = components.filterNot { it.isMulti }.map { it.componentTypeId }.toSet()
}

@Stable
data class ComponentValueState(
    // F4-guide (state-modeling.md «make impossible states impossible»):
    // identity — единый sealed key (Pristine XOR Saved), НЕ два nullable-поля.
    // Невозможное состояние («оба null» / «оба non-null») невыразимо на уровне типа.
    val key: ComponentValueKey,
    val componentTypeId: ComponentTypeId,
    val componentTypeRef: ComponentTypeRef,
    val isMulti: Boolean,
    val isEdit: Boolean = false,
    /**
     * A10-redesign: компонент закоммичен (Update/PessimisticRemove) и его DB-операция
     * В ПОЛЁТЕ. Маркер «уже сохраняется» — отличает «in-flight» от «просто редактируется».
     * Нужен, чтобы:
     *  (1) success-refresh закрывал edit ТОЛЬКО завершившегося компонента (по совпадению
     *      `componentValueId` И `isCommitting`), а не все saved разом;
     *  (2) `commitAndCloseAllEdits` / `CreateComponentValue` (не guarded) НЕ переэмитили
     *      уже летящий компонент (skip if isCommitting) → нет дубля INSERT/UPDATE.
     */
    val isCommitting: Boolean = false,
    val origin: String = "",
    val edited: String = "",
) {
    val isPristine: Boolean get() = key is ComponentValueKey.Pristine
    val componentValueId: ComponentValueId? get() = (key as? ComponentValueKey.Saved)?.componentValueId
    val pristineKey: Long? get() = (key as? ComponentValueKey.Pristine)?.pristineKey
}
```

> **F4-guide.** Прежний вариант (два nullable + `error()` в геттере `key`) — это product-тип, маскирующий XOR-инвариант; `state-modeling.md` это явно запрещает. Sealed `ComponentValueKey` — используем прямо в state.

> **A10-redesign (после ревью).** Первая версия A10 («держать isEdit до success-refresh» + «refresh закрывает все saved») имела баг: `commitAndCloseAllEdits` оставляет несколько saved в edit, а refresh от ОДНОЙ операции закрывал ВСЕ → терял правку другого незавершённого поля; плюс re-commit in-flight давал дубль INSERT. Фикс — поле `isCommitting` (см. ниже §6.2 A10).

### 4.2 Mapper

```kotlin
fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = lexemeId.id,
    components = components.map { it.toComponentValueState() },
)

fun ComponentValue.toComponentValueState(): ComponentValueState =
    ComponentValueState(
        key = ComponentValueKey.Saved(id),
        componentTypeId = type.id,
        componentTypeRef = type.toRef(),
        isMulti = type.isMulti,
        origin = data.asText().orEmpty(),
    )
```

Создание pristine (в reducer-ветке `CreateComponentValue`):
```kotlin
ComponentValueState(
    key = ComponentValueKey.Pristine(state.nextPristineKey),
    componentTypeId = type.id,
    componentTypeRef = type.toRef(),
    isMulti = type.isMulti,
    isEdit = true,
    origin = "",
    edited = "",
)
```

### 4.2.1 B1 + A12 — Рендер поля: всегда; лейбл = живой тип по id, fallback на снимок

`ComponentValueState` несёт снимок `componentTypeRef` + `isMulti` + `componentTypeId` (из `ComponentValue.type` при `toComponentValueState()`).

**Инвариант рендера (B1/F1):** поле уже добавленного значения рендерится ВСЕГДА (никогда не скрывается). Поле НЕ зависит от наличия типа в `availableComponentTypes` для самого факта рендера — структурно идёт из `ComponentValueState`.

**Резолв лейбла (A12-решение):** лейбл вычисляет UI-резолвер `componentValueLabel(componentTypeId, snapshotRef, availableTypes)` (04 §1) — берёт имя живого типа из `availableComponentTypes` по `componentTypeId`, с **fallback** на снимок `componentTypeRef`, если типа нет в справочнике. (Метод `ComponentType.displayLabel()` в domain НЕ вводится — см. 01 §Domain; маппинг ref→текст живёт в UI.) Связь в БД по id, поэтому:
- Тип **переименован** в Manager → он в справочнике под новым именем → lookup по id находит → лейбл значения подхватывает новое имя реактивно (`ComponentTypesLoaded` → recompose). ✅ A12.
- **Окно загрузки (реальная причина fallback).** Между `WordLoaded` (грузит значения) и `ComponentTypesLoaded` (поток типов приходит чуть позже, отдельным эффектом) `availableComponentTypes` ещё пуст → lookup по id не находит → fallback на снимок держит лейбл читаемым этот короткий момент. **Удалённый тип сюда НЕ относится:** каскадный soft-delete + фильтр маппера (`value.removedAt==null && type.removedAt==null`) гарантируют, что значение с удалённым типом вообще не грузится (см. 09 A9). Orphan-значения не существует — single-screen Android, гонки нет.

UI: `componentValueLabel(componentTypeId, snapshotRef, availableTypes)` (04 §1). Lookup используется ТОЛЬКО для текста лейбла и ТОЛЬКО с fallback — НЕ для решения «рендерить ли поле» (это и был баг F1: `?: return@key` скрывал поле в окне до прихода `ComponentTypesLoaded`).

`availableComponentTypes` используется для: (1) `ComponentChipsRow` (какие chip'ы предложить); (2) резолв лейбла значений (live-by-id, fallback snapshot). Тип soft-deleted в Manager → его значения каскадно soft-удаляются и на карточку не грузятся (фильтр маппера, см. 09 A9) — orphan на экране не возникает. Fallback на снимок нужен только на окно до прихода `ComponentTypesLoaded`.

### 4.3 Helpers (rewrite сегменты)

- `LexemeState.findByKey(key: ComponentValueKey): ComponentValueState?`
- `LexemeState.updateComponent(key, transform: (ComponentValueState) -> ComponentValueState): LexemeState`
- `LexemeState.removeComponent(key): LexemeState`
- `LexemeState.appendPristine(component: ComponentValueState): LexemeState`
- `WordCardState.updateLexeme(lexemeId, fn)` — без изменений (id-based).
- `WordCardState.removeLexeme(lexemeId)` — без изменений.

**REWRITE `closeAllEditModes`:**
```kotlin
fun WordCardState.closeAllEditModes(): WordCardState {
    val newWordState = (wordState as? WordState.Loaded)
        ?.copy(isEditMode = false, edited = "") ?: wordState
    val newList = lexemeList.map { l ->
        // pristine с empty edited — выкидываем (висячий черновик не должен оставаться).
        val cleaned = l.components.mapNotNull { c ->
            if (c.isPristine && c.edited.trim().isEmpty()) null
            else c.copy(isEdit = false, edited = "")
        }
        l.copy(components = cleaned)
    }.filterNot { it.id == NOT_IN_DB && it.components.isEmpty() }
    return copy(wordState = newWordState, lexemeList = newList)
}
```

**REWRITE `commitAndCloseAllEdits`:** итерация по компонентам, для каждого `commitDecision` → effect.
- **A10-redesign (skip in-flight):** компонент с `isCommitting=true` ПРОПУСКАЕТСЯ (его операция уже летит) — НЕ переэмитить, иначе дубль INSERT/UPDATE. Это закрывает баг A10-3 (не-guarded `CreateComponentValue`/прочие вызовы `commitAndCloseAllEdits`, пока saved-commit в полёте).
- **A10:** для `Update` ставим `isEdit=true` + `isCommitting=true` (как одиночный commit) — закроется success-refresh'ем по id.
- **NOT_IN_DB лексема:** эмитим **только один** `UpsertComponentValue.CreateLexeme` (A3) и помечаем якорный pristine **`isCommitting=true`** (flush-on-back: иначе `hasInFlightCommits=false` → пост-шаг §6.1 уведёт назад ДО завершения INSERT = преждевременный выход с потерей черновика). Остальные edited components ждут `LexemeDraftPromoted` → reducer реэмитит их как `AddValue` (finding #17), тоже помечая `isCommitting=true` (§6.2.1).

**H-11 — word-edit тоже коммитится.** `commitAndCloseAllEdits` помимо компонентов ОБЯЗАН закоммитить активный edit самого слова (`WordState.Loaded.isEditMode` + `edited`≠`value` → emit `UpdateWord(wordId, edited)`, `isEditMode=false`). Тесты `1.3`/`2.4` это требуют. Без явной word-ветки имплементатор по §4.3 её пропустит.

**Граница flush-on-back для word-edit.** `hasInFlightCommits` (§6.2.3) отслеживает только записи КОМПОНЕНТОВ (`isCommitting`). Коммит правки самого слова (`UpdateWord`) flush-on-back **НЕ удерживает**: редактирование слова — IS479 (вне скоупа IS481, решение 09 #3), durability не гарантируется. При «назад» с грязной ТОЛЬКО word-правкой `UpdateWord` эмитится, но выход его не ждёт. Осознанно; трекинг word-commit маркером — backlog при необходимости.

**B3 — финальный cleanup пустых NOT_IN_DB лексем.** `commitAndCloseAllEdits` ОБЯЗАН в конце применить тот же фильтр, что и `closeAllEditModes`:
```kotlin
.filterNot { it.id == NOT_IN_DB && it.components.isEmpty() }
```
Сценарий без него: NOT_IN_DB лексема с одним pristine (пустой edited) → `EnterWordEditMode` → `commitAndCloseAllEdits` → этот pristine получает `LocalRemove` (empty+empty) → лексема становится пустой NOT_IN_DB → **висит навсегда** → `isCreatingLexeme=true` блокирует `CreateLexeme` (новые лексемы создать нельзя). Фильтр удаляет такую осиротевшую пустую NOT_IN_DB лексему. **Покрыть тестом** в `CommitAndCloseAllEditsTest` («NOT_IN_DB с единственным empty pristine → после commitAndCloseAllEdits лексема удалена, CreateLexeme снова доступен»).

### 4.4 Удаляется

- `TextValueState` (заменяется ComponentValueState).
- `createLexemeTranslation / updateLexemeTranslationText / enableLexemeTranslationEdit`.
- Зеркальные definition helpers.
- `hasDefinitionComponent` — заменяется фильтром `availableComponentTypes`.
- Поле `Lexeme.toLexemeState()` `translation`/`definition` ветки — заменяется map по components.

---

## §5. Effect (REWRITE — внутри `DatasourceEffectHandler.kt`)

```kotlin
sealed interface DatasourceEffect : Effect {
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    /**
     * Upsert значения компонента. A3: sealed — три РАЗНЫЕ операции, каждая со своими
     * полями. Невозможные комбинации (как раньше с nullable lexemeId/componentValueId)
     * больше невыразимы (impossible states impossible — как для ComponentValueKey).
     * pristineKey (Create/Add) → reducer знает какой pristine стал saved.
     */
    sealed interface UpsertComponentValue : DatasourceEffect {
        val wordId: Long
        val dictionaryId: Long
        val componentTypeId: ComponentTypeId
        val componentTypeRef: ComponentTypeRef
        val data: TemplateValues

        /** Создание NOT_IN_DB лексемы якорным значением (lexeme ещё нет). */
        data class CreateLexeme(
            override val wordId: Long,
            override val dictionaryId: Long,
            val pristineKey: Long,
            override val componentTypeId: ComponentTypeId,
            override val componentTypeRef: ComponentTypeRef,
            override val data: TemplateValues,
        ) : UpsertComponentValue

        /** Добавление нового значения к существующей лексеме. */
        data class AddValue(
            override val wordId: Long,
            override val dictionaryId: Long,
            val lexemeId: Long,
            val pristineKey: Long,
            override val componentTypeId: ComponentTypeId,
            override val componentTypeRef: ComponentTypeRef,
            override val data: TemplateValues,
        ) : UpsertComponentValue

        /** Обновление существующего значения. */
        data class UpdateValue(
            override val wordId: Long,
            override val dictionaryId: Long,
            val lexemeId: Long,
            val componentValueId: ComponentValueId,
            override val componentTypeId: ComponentTypeId,
            override val componentTypeRef: ComponentTypeRef,
            override val data: TemplateValues,
        ) : UpsertComponentValue
    }

    data class RemoveComponentValue(
        val componentValueId: ComponentValueId,
        val lexemeId: Long,
    ) : DatasourceEffect

    /**
     * Trigger для AvailableComponentTypesFlowHandler (re-)subscribe.
     */
    data class LoadAvailableComponentTypes(val dictionaryId: Long) : DatasourceEffect

    data class RestoreLexemeWithComponents(
        val wordId: Long,
        val dictionaryId: Long,
        val snapshot: Lexeme,   // A17: полный snapshot — для retry при ошибке restore (round-trip)
    ) : DatasourceEffect
}
```

> A17: effect несёт ПОЛНЫЙ `snapshot: Lexeme` (не только пары). UseCase внутри маппит `snapshot.components.map { it.type.toRef() to it.data }`. При ошибке handler возвращает snapshot в retry-Msg (см. ниже) — иначе snapshot потерян (hard-delete).

### 5.1 `DatasourceEffectHandler.onEffect` (REWRITE)

```text
when (effect) {
    LoadWord -> term = useCase.getTermById(wordId);
        if null      -> WordNotFound
        else         -> WordLoaded(term)
        on exception -> WordNotFound   // НЕ generic OperationFailed: тот не снимает isLoading → вечный спиннер.
                                       // WordNotFound снимает isLoading + NavigationEffect.Back.
                 (FlowHandler сам стартует через LoadAvailableComponentTypes)

    UpdateWord -> as today, refresh via getTermById.

    RemoveWord -> useCase.deleteWord; NavigateBack on success.

    RemoveLexeme -> useCase.deleteLexeme(wordId, lexemeId) ->
        Removed(snapshot) -> LexemeRemoved(snapshot)
        null -> OperationFailed(R.string.word_card_error_generic)   // F7: снимает pending

    is UpsertComponentValue -> when (effect) {                      // A3: sealed, исчерпывающий when
        is CreateLexeme ->                                          // промоушен NOT_IN_DB
            result = useCase.addLexemeWithComponent(effect.wordId, effect.dictionaryId, effect.componentTypeRef, effect.data)
            if null -> OperationFailed(...)                         // F7: снимает pending, лексема остаётся NOT_IN_DB редактируемой
            else    -> LexemeDraftPromoted(result, effect.pristineKey)
                       // F3/F4: anchorPristineKey = effect.pristineKey (handler ЗНАЕТ его — это
                       // из effect, не из state). Reducer исключает якорь из survivors ПО КЛЮЧУ.
                       // promoted-лексема уже содержит якорь как Saved-компонент (из БД).

        is AddValue ->
            result = useCase.addComponentValue(effect.lexemeId, effect.componentTypeId, effect.data)
            if null -> OperationFailed(...)
            else    -> RefreshLexemeComponents(effect.lexemeId, result.lexeme.components)
                       + ComponentValueInserted(effect.lexemeId, effect.pristineKey, result.newComponentValueId)
                       (two-Msg-burst: один update, один identity-flip)

        is UpdateValue ->
            lexeme = useCase.updateComponentValue(effect.componentValueId, effect.lexemeId, effect.data)
            if null -> OperationFailed(...)                         // вкл. B1.5 graceful-fail soft-deleted типа
            else    -> RefreshLexemeComponents(effect.lexemeId, lexeme.components)
    }

    RemoveComponentValue -> useCase.deleteComponentValue(cvId, lexemeId) ->
        ComponentRemoved(lexeme)         -> RefreshLexemeComponents(lexemeId, lexeme.components)
        LexemeCascadeRemoved(snapshot)   -> LexemeCascadeRemoved(snapshot)
        null                             -> OperationFailed(...)

    LoadAvailableComponentTypes ->
        no-op (handled by FlowHandler через runEffect; см. § 7).

    RestoreLexemeWithComponents -> useCase.restoreLexemeWithComponents(wordId, dictId, effect.snapshot) ->
        success (Lexeme != null) -> getTermById → WordLoaded(term)
            // H-12: НЕ эмитим LoadAvailableComponentTypes отдельно — WordLoaded reducer-ветка
            //       сама его эмитит (§6.2). Иначе двойная подписка/двойной ComponentTypesLoaded.
        null -> RestoreLexemeFailed(effect.snapshot)   // A17: вернуть snapshot для retry
}
```

**Detail для `LexemeDraftPromoted`:** handler не имеет доступа к state, но знает `anchorPristineKey` из `effect.pristineKey`. Msg несёт `newLexeme: Lexeme` + `anchorPristineKey: Long`. Reducer находит единственную NOT_IN_DB лексему, исключает якорь из выживших pristine **по ключу** (promoted уже содержит его как Saved), домерживает остальных и реэмитит для них upsert (B2 §6.2.1):

```kotlin
data class LexemeDraftPromoted(val newLexeme: Lexeme, val anchorPristineKey: Long) : Msg
```

**M4 — LogTags в `DatasourceEffectHandler`.** Существующий generic catch-all (try/catch вокруг диспетчинга эффекта) использует `me.apomazkin.mate.LogTags.MATE` — **оставить как есть** (не переписывать на WORDCARD). Новый код (`AvailableComponentTypesFlowHandler`, rewritten `WordCardUseCaseImpl`) использует `me.apomazkin.wordcard.LogTags.WORDCARD`. Не плодить путаницу: MATE — для mate-инфраструктурного catch-all, WORDCARD — для доменных ошибок модуля.

### 5.2 Two-Msg handoff race (finding #19)

Для INSERT (lexemeId != null, cvId == null) handler должен эмитить **в строгом порядке** сначала `RefreshLexemeComponents(updated)`, затем `ComponentValueInserted(pristineKey → newCvId)`. Reducer обрабатывает Msg sequentially — `Refresh` сначала overrwrite'ит components (новый cv с реальным id уже там), затем `Inserted` сделает identity-flip Pristine→Saved для match'енного pristineKey. Identity-flip — no-op safe если в state pristine уже не существует (idempotent).

Альтернативно (упрощение): обойтись только `RefreshLexemeComponents` без `Inserted`, и в reducer merge-policy: для каждого pristine match'ить по `(typeId, edited)` → если в свежих components есть запись с этим typeId и `data.asText() == origin/edited` → удалить pristine. **Менее надёжно** (multi: 2 pristine с одинаковым typeId и одинаковым text — collapse'ятся). Поэтому держим явный `ComponentValueInserted`.

### 5.3 `UiEffect.ShowSnackbarWithRetry` — ОБЯЗАТЕЛЬНО обновить `UiEffectHandler` (G4)

`UiEffect` — sealed; его `UiEffectHandler.onEffect` имеет **исчерпывающий `when` без `else`** (реальный `UiEffectHandler.kt`, две ветки: `ShowErrorSnackbar`, `ShowSnackbarWithUndo`). Новый `ShowSnackbarWithRetry(messageRes, actionLabelRes, retryMsg)` (G4) ломает компиляцию, пока не добавлена ветка. Проводка — зеркало `ShowSnackbarWithUndo`:
- показать снек через `uiHost.showSnackbarWithAction(messageRes, actionLabelRes)` (метод уже используется ветками undo);
- по нажатию action → `consumer(retryMsg)` (retryMsg = `RetryLoadComponentTypes` для load-failure, `UndoRestoreLexeme(snapshot)` для restore-failure).

Без этого пункта имплементатор добавит тип `ShowSnackbarWithRetry`, но забудет ветку handler'а → либо non-exhaustive `when` (не компилируется), либо снек молча не покажется. Эмитится из reducer-веток `ComponentTypesLoadFailed` (§6.2) и `RestoreLexemeFailed` (A17); покрыто §1.2 / S17 / S-trap-4.

---

## §6. Reducer (REWRITE — `WordCardReducer.kt`)

### 6.1 Sketch (общие принципы)

```kotlin
class WordCardReducer : MateReducer<WordCardState, Msg, Effect> {
    override fun reduce(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
        // Guard: при идущей записи ИЛИ при выходе (лоадер) — глотаем guarded-ввод.
        if ((state.isPendingDbOp || state.isExiting) && message.isGuardedByPending()) return state to emptySet()
        val (next, effects) = reduceImpl(state, message)
        // Flush-on-back (единая точка): эмитим Back на ПЕРЕХОДЕ в «готов к выходу»
        // (isExiting && !hasInFlightCommits), а НЕ на каждом reduce при этом условии.
        // isExiting НЕ сбрасываем. Это закрывает ОБА класса лишней навигации:
        //  - двойной NavigateBack (double-tap «назад»): второй — no-op (см. ветку NavigateBack),
        //    условие уже было истинным до него (readyBefore==true) → второго Back нет;
        //  - поздний не-NavigateBack Msg (RefreshWord после UpdateWord; запоздалый
        //    ComponentTypesLoaded): он не меняет isExiting/hasInFlightCommits → перехода нет → Back нет.
        val readyNow = next.isExiting && !next.hasInFlightCommits
        val readyBefore = state.isExiting && !state.hasInFlightCommits
        return if (readyNow && !readyBefore)
            next to (effects + NavigationEffect.Back)
        else next to effects
    }
    // ... when (message) { ... }
}
```

Конструктор без параметров. Логирование внутри reducer'а не вводится.

### 6.2 Handlers (overview)

| Msg | Действие | Effect |
|---|---|---|
| `WordLoaded(term)` | set `wordState = Loaded(...,dictionaryId=term.dictionaryId)`; map `lexemeList = term.lexemeList.map { it.toLexemeState() }`; `isLoading=false`, `isPendingDbOp=false` | `LoadAvailableComponentTypes(term.dictionaryId)` |
| `WordNotFound` | `isLoading=false,isPendingDbOp=false` | `NavigationEffect.Back` |
| `RefreshWord(term)` | `loaded.copy(value = term.value, isEditMode = false, edited = "")` + **`isPendingDbOp = false`** (как в текущем reducer — обновление слова закрывает word-edit и снимает блокировку; lexemeList НЕ трогаем) | — |
| `ComponentTypesLoaded(types)` | set `availableComponentTypes = types` | — |
| `ComponentTypesLoadFailed(e)` | unchanged state | `ShowSnackbarWithRetry(error_load_component_types, action_retry, RetryLoadComponentTypes)` |
| `RetryLoadComponentTypes` | unchanged | `LoadAvailableComponentTypes(loaded.dictionaryId)` |
| `CreateComponentValue(lexemeId, typeId)` | guard: NOT_IN_DB invariant (см. § 6.3) → `commitAndCloseAllEdits` → append pristine component (with `pristineKey = state.nextPristineKey`); `nextPristineKey++` | batch commit effects |
| `UpdateComponentValueInput(lexemeId, key, value)` | updateComponent (key) `{ copy(edited = value) }` (only if isEdit) | — |
| `EnterComponentValueEditMode(lexemeId, key)` | `commitAndCloseAllEdits` + enter edit; pre-existing saved: set `edited = origin`, `isEdit = true` | batch commit effects |
| `CommitComponentValueEdit(lexemeId, key)` | per `commitDecision()`: NoOp → reset edit `isEdit=false` (**нет** pending); LocalRemove → removeComponent (+ cascade NOT_IN_DB cleanup, **нет** pending); PessimisticRemove → emit RemoveComponentValue + **`isPendingDbOp=true`**, **`isCommitting=true`** (B6); Update(text) → emit UpsertComponentValue + **`isPendingDbOp=true`** + **A10: `isEdit=true` держим, `isCommitting=true`** (отметка in-flight; закроет ТОЛЬКО свой success-refresh; ошибка снимет isCommitting и оставит edit) | up to 1 effect |
| `RemoveComponentValueRequested(lexemeId, key)` | Pristine → removeComponent locally (+ cascade NOT_IN_DB cleanup). Saved + origin.empty → local nullify (mirrors current «origin.isEmpty»). Иначе → emit RemoveComponentValue, isPendingDbOp=true | up to 1 effect |
| `RefreshLexemeComponents(lexemeId, comps)` | **union-by-id merge (B4)**, НЕ full-replace. См. детальную политику ниже (§6.2 B4); `isPendingDbOp=false` | — |
| `ComponentValueInserted(lexemeId, pristineKey, newCvId)` | **S-DUP-fix (dedup-aware):** найти pristine с этим pristineKey. Если в components УЖЕ есть `Saved(newCvId)` (Refresh прилетел раньше) → **удалить pristine** (дедуп). Иначе → identity-flip pristine → `Saved(newCvId)`, **A10: `isEdit=false`, `isCommitting=false`** (успех добавления закрывает edit). Если pristine нет → no-op. | — |
| `LexemeDraftPromoted(newLexeme, anchorPristineKey)` | **B2 pristine-merge** (см. §6.2.1): найти единственную NOT_IN_DB лексему; survivors = pristine КРОМЕ якоря (по `anchorPristineKey`); заменить на `newLexeme.toLexemeState()` + домержить survivors; реэмитить `UpsertComponentValue(lexemeId=newId, cvId=null, pristineKey)` для каждого survivor; pending=false | N−1 upsert effects (survivors) |
| `LexemeCascadeRemoved(snapshot)` | removeLexeme(snapshot.lexemeId); pending=false; emit snackbar with undo | `ShowSnackbarWithUndo(...,UndoRestoreLexeme(snapshot))` |
| `LexemeRemoved(snapshot)` | removeLexeme; pending=false; emit snackbar | `ShowSnackbarWithUndo(...,UndoRestoreLexeme(snapshot))` |
| `UndoRestoreLexeme(lex)` | pending=true | `RestoreLexemeWithComponents(wordId, dictionaryId, snapshot = lex)` |
| `RestoreLexemeFailed(snapshot)` | **A17:** `isPendingDbOp=false` | `ShowSnackbarWithRetry(word_card_error_restore_lexeme, word_card_action_retry, UndoRestoreLexeme(snapshot))` — повтор восстановления (snapshot цел). H-8: `word_card_error_restore_lexeme` УЖЕ существует |
| `CreateLexeme` | **(добавлено — был пропущен).** guard: `isPendingDbOp` (guarded) и `isCreatingLexeme` (no-op, single-draft). Иначе → `commitAndCloseAllEdits` (коммит открытых правок) → prepend пустой `LexemeState(id=NOT_IN_DB)` | batch commit effects (если были правки) |
| `RemoveLexeme(lexemeId)` | snapshot + если `lexemeId == NOT_IN_DB` → **local `removeLexeme`, БЕЗ эффекта/undo** (в БД ничего нет); иначе → emit `RemoveLexeme(wordId, lexemeId)`, pending=true | 0 либо 1 effect |
| `OperationFailed(messageRes)` | **F7: `isPendingDbOp=false`** (снять блокировку — иначе экран залипает). **A10/flush: снять `isCommitting=false` со ВСЕХ компонентов, ВКЛЮЧАЯ pristine-выживших** (реэмит `AddValue` при промоушене), но `isEdit`/`edited`/key НЕ трогать → поле остаётся с текстом (можно повторить). КРИТИЧНО: если у pristine-выжившего НЕ снять `isCommitting` — при провале его `AddValue` маркер залипнет навсегда (terminal-Msg уже не придёт) → `hasInFlightCommits` вечно true → лоадер следующего выхода зависнет. **Flush-on-back: `isExiting=false`** (запись упала при выходе → отменяем выход, остаёмся показать ошибку; без сброса isExiting пост-шаг §6.1 увёл бы назад при неуспехе, т.к. isCommitting снят) | `UiEffect.ShowErrorSnackbar(messageRes)` |
| `NavigateBack` | **Flush-on-back (маркеры).** Если уже `isExiting` → no-op (выход уже идёт). Иначе: `commitAndCloseAllEdits` (коммит открытой правки; пустой pristine / пустая NOT_IN_DB — дропаются) → **`isExiting=true`**. Навигацию здесь НЕ эмитим — её добавит пост-шаг `reduce` (§6.1): летящих записей нет → сразу `Back`; есть → ждём их завершения под лоадером. | commit-эффекты (`Back` добавит пост-шаг §6.1) |
| `NoOperation` | **(C6)** no-op заглушка (handler обязан вернуть Msg, а делать нечего): `state` без изменений | — (явная ветка, чтобы `when` был исчерпывающим без `else`) |
| `RemoveWord` / `OpenDeleteWordDialog` / `CloseDeleteWordDialog` / etc. | as today (guarded: RemoveWord) | `DeleteWord` → `NavigationEffect.Back` on success |

### 6.2.1 B2 — `LexemeDraftPromoted`: сохранить выжившие pristine при промоушене

**Проблема.** Новая NOT_IN_DB лексема, юзер заполнил 2+ компонента (перевод + Example). `commitAndCloseAllEdits` эмитит **ровно один** `UpsertComponentValue(lexemeId=null)` (создание лексемы якорным компонентом — §6.3 инвариант). Остальные pristine остаются локальными в state. Приходит `LexemeDraftPromoted(newLexeme)`, где в БД только якорный компонент. Наивная замена `lexemeList[i] = newLexeme.toLexemeState()` **теряет** выжившие pristine (Example исчезает).

**F3/F4 — почему НЕ пересчитывать selector.** Прежний вариант пересчитывал «какой pristine ушёл в INSERT» через `dropFirstInsertCandidate` в reducer. Но между emit'ом INSERT и приходом `LexemeDraftPromoted` состав pristine мог измениться (B5: `CreateComponentValue` не guarded → юзер добавил ещё один pristine). Пересчёт по `componentTypeRef` мог исключить НЕ тот pristine → дубликат / cardinality-fail. **Фикс:** якорь идентифицируется по `anchorPristineKey` (пришёл в Msg из `effect.pristineKey`), а не пересчётом.

**Фикс (reducer, ветка `LexemeDraftPromoted`):**
```kotlin
is Msg.LexemeDraftPromoted -> {
    val loaded = state.wordState as? WordState.Loaded ?: return state to emptySet()
    val draft = state.lexemeList.firstOrNull { it.id == NOT_IN_DB }
    val promoted = message.newLexeme.toLexemeState()   // содержит якорный компонент как Saved

    // 1. Выжившие pristine = все pristine черновика, КРОМЕ якоря (по anchorPristineKey).
    //    Якорь уже в promoted.components как Saved — повторно его НЕ добавляем и НЕ реэмитим.
    val survivors: List<ComponentValueState> = draft
        ?.components
        ?.filter {
            it.isPristine && it.pristineKey != message.anchorPristineKey &&
                it.edited.ifEmpty { it.origin }.trim().isNotEmpty()   // #2: пустых выживших дропаем — ни в merged, ни в реэмит (пустой INSERT не шлём)
        }
        .orEmpty()

    // 2. Домержить выживших в promoted (сохраняя их pristineKey).
    //    Flush-on-back: survivors реэмитятся как AddValue ниже → помечаем isCommitting=true,
    //    чтобы hasInFlightCommits держал выход, пока они не досохранятся (§6.2.3).
    val survivorsInFlight = survivors.map { it.copy(isCommitting = true) }
    val merged = promoted.copy(components = promoted.components + survivorsInFlight)
    // Заменяем черновик на готовую лексему НА ЕГО МЕСТЕ — порядок списка не меняется,
    // свежая лексема остаётся сверху (где была создана через prepend).
    val newList = state.lexemeList.map { if (it.id == NOT_IN_DB) merged else it }

    // 3. Реэмитить upsert для каждого выжившего pristine (теперь с реальным lexemeId).
    //    A3: это AddValue (лексема уже создана, добавляем значение).
    val effects = survivors.map { p ->
        DatasourceEffect.UpsertComponentValue.AddValue(
            wordId = loaded.id,
            dictionaryId = loaded.dictionaryId,
            lexemeId = promoted.id,            // реальный id
            pristineKey = p.pristineKey,
            componentTypeId = p.componentTypeId,
            componentTypeRef = p.componentTypeRef,
            data = textValuesOf(p.edited.ifEmpty { p.origin }.trim()),   // contract-gap trim: единое правило
        )
    }.toSet()

    state.copy(lexemeList = newList, isPendingDbOp = false) to effects
}
```
Якорь исключается **по `pristineKey`** — детерминированно, без зависимости от порядка/типа. **Покрыть тестами:** (а) «promote NOT_IN_DB с 2+ pristine → 1 лексема + N значений (1 в promoted + (N-1) реэмит upsert)»; (б) «promote, где якорь — НЕ первый по порядку, а юзер добавил ещё pristine до промоушена → якорь исключён по ключу, дубликата нет».

Замечание по гонке: реэмиченные upsert'ы вернут `ComponentValueInserted(pristineKey → newCvId)` + `RefreshLexemeComponents` — identity-flip отработает как обычно (§5.2). Pending снимается финальным refresh'ем.

### 6.2.2 B4 — `RefreshLexemeComponents`: union-by-id merge, pristine не трогать

**Инвариант payload (S-TRAP3-fix).** `RefreshLexemeComponents.components` ВСЕГДА = полное post-mutation состояние компонентов лексемы из БД (handler делает re-read `getLexemeById` ПОСЛЕ мутации). Значит payload включает любой только что вставленный cv. Поэтому правило «(3) saved из state, отсутствующий в payload → удалить» безопасно: оно срабатывает только для реально удалённых извне записей, а не для in-flight insert (тот уже в payload). Тест обратного порядка `Inserted→Refresh` ОБЯЗАН использовать payload, включающий newCvId (реалистичный) — иначе тест моделирует невозможный stale-payload.

**Проблема.** Описание «full replace + выкинуть все pristine» ломает multi: пока юзер печатает второй pristine, refresh от сохранения первого выкинет второй (текст потерян). Конкурентные refresh для multi также затирают друг друга.

**Политика merge (union-by-id + A10 id-targeted close):**
```
result = buildList {
  // (1) Pristine из state — НИКОГДА не трогаем (флипает только ComponentValueInserted).
  // (2) Для каждого saved из payload:
  //     найти соответствие в state по componentValueId.
  //       — если в state он isEdit=true И isCommitting=true → ЭТО завершившийся коммит
  //         → закрыть edit: взять из payload (origin свежий, isEdit=false, isCommitting=false).
  //       — если в state он isEdit=true И НЕ isCommitting → пользователь редактирует ДРУГОЕ
  //         поле (его commit ещё не эмитнут) → СОХРАНИТЬ edit (origin из payload, isEdit/edited целы).
  //       — иначе → взять из payload (свежий origin, isEdit=false).
  // (3) Saved из state, отсутствующий в payload по componentValueId → УДАЛИТЬ.
}
```
Псевдокод:
```kotlin
val keptPristine = lexeme.components.filter { it.isPristine }
val mergedSaved = comps.map { cv ->
    val inState = lexeme.components.firstOrNull { it.componentValueId == cv.id }
    when {
        // A10: закрыть edit ТОЛЬКО у завершившегося коммита (id + isCommitting).
        inState != null && inState.isEdit && inState.isCommitting ->
            cv.toComponentValueState()                       // isEdit=false, isCommitting=false
        // A10: чужой активный edit (не in-flight) — сохранить, обновить только origin.
        inState != null && inState.isEdit && !inState.isCommitting ->
            inState.copy(origin = cv.data.asText().orEmpty())
        else -> cv.toComponentValueState()
    }
}
lexeme.copy(components = mergedSaved + keptPristine)
```
- **A10 id-targeted close (BLOCKER-fix):** refresh от операции X закрывает edit ТОЛЬКО компонента X (`isCommitting`), а не все saved. Другой компонент, который юзер редактирует параллельно (его commit ещё не пошёл, `isCommitting=false`), сохраняет свой ввод. Это правильно восстанавливает таргетированный F073.
- **Pristine** → всегда сохраняется (in-flight ввод цел).
- **Saved нет в payload** → удаляется.

**A10-важно:** edit saved-компонента закрывает ТОЛЬКО успешный refresh. При `OperationFailed` refresh НЕ приходит → компонент остаётся `isEdit=true` с `edited` → юзер видит свой текст, повторит.

**C5 — НЕ удалять keep-edit ветку (794-795).** Кейс «второй saved в edit, пока первый коммитится (pending)» **ДОСТИЖИМ**: `CreateComponentValue` НЕ guarded by pending (B5) → повторно дёргает `commitAndCloseAllEdits` во время полёта → в state одновременно бывает >1 компонента с разными `isEdit/isCommitting`. Ветка `isEdit && !isCommitting → сохранить edit` (794-795) именно это и спасает — она **нагружена, не мёртвый код**. НЕ помечать как unreachable и НЕ удалять (иначе вернётся потеря ввода B3). Полный correlation-id фикс гонки — в backlog (см. `ВекторныйПиздеж`), но сама ветка-защита нужна уже сейчас.

**Тесты:** «refresh при наличии in-flight pristine → pristine сохранён»; «refresh saved → `isEdit=false`, origin из payload (edit закрыт)»; «commit(Update) → НЕ закрывает edit (isEdit=true, pending=true)»; «commit(Update) → OperationFailed → поле остаётся isEdit=true, edited цел»; «saved которого нет в payload → удалён».

### 6.2.3 Flush-on-back (`isExiting` — маркеры)

Цель: при «назад» с незавершённой записью НЕ терять данные. Раньше `NavigateBack` сразу эмитил `Back`, а scope экрана умирал раньше, чем доезжала async-запись (ложный «автосейв»). Теперь — дождаться и только потом выйти, по МАРКЕРАМ, а не по одному bool:

1. `NavigateBack` → `commitAndCloseAllEdits` (коммит открытой правки) + `isExiting=true`. Навигацию сам НЕ эмитит.
2. «Ещё пишем?» = `hasInFlightCommits` = `∃ компонент с isCommitting=true`. Покрывает ВСЕ async-записи: `Update` / `AddValue` / `CreateLexeme` и реэмит survivors при промоушене черновика (им тоже ставится `isCommitting=true`, §6.2.1).
3. Пост-шаг `reduce` (§6.1): эмитим `+NavigationEffect.Back` на **ПЕРЕХОДЕ** в `(isExiting && !hasInFlightCommits)` — т.е. когда этот reduce изменил `isExiting`/`hasInFlightCommits` и привёл к условию (`readyNow && !readyBefore`). `isExiting` **НЕ сбрасываем**. Так Back эмитится ровно один раз: повторный `NavigateBack` (double-tap «назад») — no-op (условие уже было истинным до него → перехода нет); поздний `RefreshWord`/`ComponentTypesLoaded` не меняет `isExiting`/`hasInFlightCommits` → перехода нет → второго Back нет. Выход происходит ровно когда снят **последний** `isCommitting`, а не первый — это и решает «узел» нескольких одновременных записей.
4. Каждый успех (`RefreshLexemeComponents` / `ComponentValueInserted` / промоушен) снимает `isCommitting` своего компонента → когда у всех снят, пост-шаг навигирует.
5. Ошибка (`OperationFailed`) при выходе → `isExiting=false` (остаёмся на экране, снек). Без этого пост-шаг увёл бы назад (при неуспехе `isCommitting` тоже снимается).
6. UI: `isExiting=true` → блокирующий лоадер (локальная запись обычно мелькает доли секунды). Повторный `NavigateBack` при `isExiting` — no-op.

**Пустой/чистый случай:** `commitAndCloseAllEdits` ничего не коммитит (пустой pristine дропнут, пустая NOT_IN_DB удалена), `hasInFlightCommits=false` → пост-шаг навигирует **сразу** (лоадер не появляется).

### 6.3 NOT_IN_DB invariant (findings #16, #17)

Brief требует chip'ы для всех типов. Закрываем риск «лексема создаётся первым ввеянным значением Example, потом всё ломается»:

- **Reducer-инвариант:** `CreateComponentValue` для NOT_IN_DB лексемы **разрешён для любого type**.
- В `commitAndCloseAllEdits` для NOT_IN_DB лексемы выбирается **ровно один** pristine — «якорь» — для emit'а `UpsertComponentValue.CreateLexeme` (A3) (порядок выбора: **первый pristine, у которого `commitDecision()==Update`** — т.е. первый НЕпустой по порядку добавления; пустые pristine отсеиваются до выбора якоря, иначе якорь окажется пустым → CreateLexeme с пустым значением. Без приоритета TRANSLATION — A2: generic-first; итог идентичен при любом якоре, т.к. остальные досохраняются как `AddValue`). `pristineKey` якоря уходит в effect и возвращается в `LexemeDraftPromoted.anchorPristineKey`. Остальные pristine остаются «локальными» — после `LexemeDraftPromoted` reducer исключает якорь **по ключу** и **повторно эмитит** для каждого выжившего pristine отдельный `UpsertComponentValue.AddValue(lexemeId=newId, pristineKey=…)`.
- Это закрывает finding #17 (не плодим N compound INSERT'ов) и F3/F4 (якорь определяется по ключу, не пересчётом selector'а в reducer — состав pristine мог измениться из-за B5).

Альтернатива: запретить tap не-translation chip'а пока в pristine лексеме нет translation. **Не выбираем** (UX уродский, нарушает обобщённый подход).

### 6.4 `isGuardedByPending` (REWRITE)

```kotlin
private fun Msg.isGuardedByPending(): Boolean = when (this) {
    is Msg.RemoveWord,
    Msg.CommitWordChanges,
    is Msg.RemoveLexeme,
    is Msg.CommitComponentValueEdit,
    is Msg.RemoveComponentValueRequested,
    is Msg.EnterComponentValueEditMode,   // parity с EnterTranslationEditMode (guarded в реальном reducer)
    Msg.OpenTopBarMenu,
    Msg.OpenDeleteWordDialog,
    is Msg.OpenDeleteLexemeDialog,
    Msg.EnterWordEditMode,
    Msg.CreateLexeme -> true
    else -> false
}
```

**B5 — `CreateComponentValue` НЕ guarded.** Append pristine — чисто локальная операция: не трогает saved, не конфликтует с refresh (B4: refresh не трогает pristine). Включение в guard ломало бы 04 §9 (быстрые тапы по multi chip → несколько pristine): первый тап через `commitAndCloseAllEdits` мог бы поставить pending, и последующие тапы проглатывались бы. Поэтому `CreateComponentValue` остаётся в `else -> false`.

**S-CREATE-fix — семантика стекинга pristine (важно для тестов multi).** `CreateComponentValue` ПЕРЕД добавлением нового pristine вызывает `commitAndCloseAllEdits`, который по `commitDecision`:
- ПУСТОЙ pristine (edited+origin пусто) → `LocalRemove` (дропается).
- ЗАПОЛНЕННЫЙ pristine → `Update` (commit, остаётся как saved/в processing).

Следствие (зафиксировано как контракт, НЕ баг): **пустые pristine НЕ стекаются.** Сценарий «3 раза тапнул multi-chip БЕЗ ввода» → остаётся 1 pristine (предыдущие пустые дропнуты). Сценарий «тап → ввёл текст → тап → ввёл → тап» → 3 pristine (заполненные не дропаются). Это корректный UX: бессмысленных пустых полей быть не может, а заполненные накапливаются.

**Тесты multi (S4, S-trap-2, [1.3]) ОБЯЗАНЫ** чередовать `UpdateComponentValueInput` между `CreateComponentValue`, чтобы получить несколько pristine. «3 пустых Create → 3 pristine» — НЕВЕРНОЕ ожидание (противоречит commit-on-create). Правильные кейсы:
- `Create → Create` (без ввода) → 1 pristine (первый пустой дропнут).
- `Create → Input("a") → Create` → 2 pristine (key 1 с edited="a", key 2 пустой).

> Реальный `WordCardReducer.isGuardedByPending` (line 623) сейчас guard'ит `CreateTranslation` — это НЕ переносим на `CreateComponentValue` (осознанное отступление от parity ради корректного multi-tap UX).

`CreateComponentValue` / `RefreshLexemeComponents` / `ComponentValueInserted` / `LexemeDraftPromoted` / `LexemeCascadeRemoved` / `LexemeRemoved` / `ComponentTypesLoaded` / `ComponentTypesLoadFailed` / `RetryLoadComponentTypes` / `UpdateComponentValueInput` — НЕ guarded.

### 6.5 `RefreshLexemes` vs `RefreshLexemeComponents` (finding #22)

В новом плане:
- **`RefreshLexemeComponents(lexemeId, components)`** — single-lexeme update. Используется после `addComponentValue` / `updateComponentValue` / `deleteComponentValue` (non-cascade).
- **`WordLoaded(term)`** уже выполняет full-rebuild lexemeList. Для restore-after-undo / multi-lexeme refresh используем повторный `WordLoaded` (handler `RestoreLexemeWithComponents` после восстановления делает `getTermById` → emit `WordLoaded`).
- Отдельный `RefreshLexemes` не нужен → удаляется. Это closes finding #22 (no overlap).

---

## §7. AvailableComponentTypesFlowHandler — NEW

```kotlin
package me.apomazkin.wordcard.mate

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.wordcard.LogTags
import me.apomazkin.wordcard.deps.WordCardUseCase

class AvailableComponentTypesFlowHandler @Inject constructor(
    private val useCase: WordCardUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null
    private var scope: CoroutineScope? = null
    private var send: ((Msg) -> Unit)? = null

    /** Init-time subscribe() — no-op: ждём LoadAvailableComponentTypes с dictId. */
    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        this.scope = scope
        this.send = send
    }

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        if (effect !is DatasourceEffect.LoadAvailableComponentTypes) return
        val s = scope ?: return
        val sender = send ?: consumer
        unsubscribe()
        job = s.launch {
            useCase.flowAvailableComponentTypes(effect.dictionaryId)
                .catch { e ->
                    logger.e(
                        tag = LogTags.WORDCARD,
                        message = "AvailableComponentTypes flow failed: ${e.message}",
                    )
                    sender(Msg.ComponentTypesLoadFailed(e))
                }
                .collectLatest { types ->
                    sender(Msg.ComponentTypesLoaded(types))
                }
        }
    }
}
```

**Обоснование @Inject (не Assisted, finding #25):** dictionaryId неизвестен на construction time Mate (приходит после `WordLoaded`). Pattern `runEffect(LoadAvailableComponentTypes(dictId)) → (re-)subscribe` идентичен `AllUserDefinedTypesFlowHandler` (на разный trigger). Assisted pattern `ComponentsForDictionaryFlowHandler` НЕ применим — там dictId известен на open экрана (передаётся как navigation arg).

`LogTags.WORDCARD` (local) — parity с UseCase impl, не вводим `LogTags.MATE` зависимость.

---

## §8. Тесты

> **АВТОРИТЕТНЫЙ контракт тестов — `07_test_design.md`** (TDD: пинящие таблицы «вход → точный state + точный Set<Effect>», truth-tables, scenario-цепочки, anti-regression на translation, трассировка инвариантов). Список ниже — оглавление; при расхождении приоритет у `07`.

### 8.1 DELETE

- `mate/TranslationManagementTest.kt`
- `mate/DefinitionManagementTest.kt`
- `mate/ext/SpecializedLexemeExtTest.kt` — **DELETE (B7).** Тестирует удаляемые helpers `createLexemeTranslation` / `createLexemeDefinition` / `updateLexemeTranslationText` / `updateLexemeDefinitionText` / `enableLexemeTranslationEdit` / `enableLexemeDefinitionEdit` + старый `closeAllEditModes` через `TextValueState`/`LexemeState.translation`. Все эти символы исчезают. Покрытие `closeAllEditModes` (новая семантика, drop empty pristine) переезжает в `CommitAndCloseAllEditsTest`.

### 8.2 REWRITE

- `mate/UndoDeleteTest.kt` → под `LexemeCascadeRemoved` / `LexemeRemoved` / `UndoRestoreLexeme`.
- `mate/LexemeManagementTest.kt` → проверки NOT_IN_DB → real (`LexemeDraftPromoted`, B2 pristine-merge):
  - promote с 2+ pristine → 1 promoted + (N−1) реэмит `UpsertComponentValue`.
  - **F3/F4 (anchor-by-key):** promote, где якорь — НЕ первый по порядку, И до промоушена добавлен ещё pristine (B5) → якорь исключён по `anchorPristineKey`, дубликата/cardinality-fail нет.
  - `LexemeDraftPromoted` при >1 NOT_IN_DB (теоретически) → fallback append + не падает.
- `mate/DatasourceEffectHandlerTest.kt` → переписать под новые effects.
- `mate/WordLoadedTest.kt` → `WordLoaded(term)` без componentTypes; reducer эмитит `LoadAvailableComponentTypes`.
- `mate/ext/LexemeExtTest.kt` — **REWRITE (B7).** Использует `LexemeState(translation = TextValueState(...))` — после переписи `LexemeState` это `LexemeState(components = listOf(ComponentValueState(...)))`. Сами тестируемые helpers `updateLexeme` / `removeLexeme` ВЫЖИВАЮТ (id-based, не трогают компоненты) — переписать только fixture'ы на новый shape `ComponentValueState`.

> `mate/ext/TopBarExtTest.kt` и `mate/ext/WordExtTest.kt` — **NO-OP (выживают):** не зависят от компонентного/translation state.

### 8.3 NEW

- `ComponentValueLifecycleTest.kt` (минимум 8 тестов, DSL — `testReduce` / `assertEffects` / `assertNoEffects` из `me.apomazkin.mate.test`, parity с TranslationManagementTest):
  - `CreateComponentValue` создаёт pristine с уникальным pristineKey + incr `nextPristineKey`.
  - `CreateComponentValue` на multi-type → можно дважды (два pristine).
  - `CreateComponentValue` под isPendingDbOp — **НЕ guarded (B5)**: append pristine проходит даже при pending (тест: pending=true + 2 быстрых CreateComponentValue по multi → 2 pristine).
  - `UpdateComponentValueInput` на pristine — only edited меняется.
  - `EnterComponentValueEditMode` на saved → `commitAndCloseAllEdits` сначала.
  - `CommitComponentValueEdit` 4 ветки (NoOp / LocalRemove / PessimisticRemove / Update).
  - `RemoveComponentValueRequested` для pristine — local remove.
  - `RemoveComponentValueRequested` для saved — `RemoveComponentValue` effect.
- `ComponentValueRefreshTest.kt`:
  - **A10:** `RefreshLexemeComponents` для saved → `isEdit=false`, origin из payload (успешный refresh ЗАКРЫВАЕТ edit коммитившегося).
  - **B4 union-by-id:** `RefreshLexemeComponents` сохраняет in-flight pristine (pristineKey != null) — НЕ выкидывает их.
  - **B4 union-by-id:** saved, отсутствующий в payload, удаляется; saved из payload overwrite'ит origin по componentValueId.
  - **B4 (multi-гонка):** state = [saved#5 + pristine P10 с текстом]; приходит Refresh с [#5,#7] → pristine P10 НЕ потерян, #7 добавлен.
  - `ComponentValueInserted` делает identity-flip pristine→saved.
  - `ComponentValueInserted` для несуществующего pristineKey — idempotent no-op.
  - **B1 (рендер до загрузки типов):** компонент, чей `componentTypeId` ещё отсутствует в `availableComponentTypes` (окно между `WordLoaded` и `ComponentTypesLoaded`), остаётся в `lexemeState.components` после `RefreshLexemeComponents` (мы не фильтруем по справочнику) → поле не теряется из state. Удалённый тип сюда не относится (его значение не грузится, 09 A9).
- `OperationFailedTest.kt` (**F7 — блокер, обязателен**):
  - `OperationFailed(msg)` при `isPendingDbOp=true` → `isPendingDbOp=false` + `UiEffect.ShowErrorSnackbar`.
  - `OperationFailed` НЕ трогает pristine/edit-состояния (юзер может повторить ввод).
  - Сценарий залипания: pending=true (после Update) → OperationFailed → следующий guarded Msg (напр. CommitComponentValueEdit) снова проходит (не проглочен).
- `ComponentTypesFlowTest.kt`:
  - `WordLoaded` эмитит `LoadAvailableComponentTypes(term.dictionaryId)` (finding #23).
  - `ComponentTypesLoaded` сохраняет в `availableComponentTypes`.
  - `ComponentTypesLoadFailed` → `ShowSnackbarWithRetry(messageRes, actionLabelRes, retryMsg = RetryLoadComponentTypes)` (отдельный UiEffect-тип, НЕ переиспользуем ShowSnackbarWithUndo — retry семантически не undo).
  - `RetryLoadComponentTypes` → `LoadAvailableComponentTypes(loaded.dictionaryId)`.
- `ComponentCascadeRemoveTest.kt`:
  - `LexemeCascadeRemoved(snapshot)` → removeLexeme + snackbar.
  - `UndoRestoreLexeme(snapshot)` → `RestoreLexemeWithComponents`.
- `CommitAndCloseAllEditsTest.kt`:
  - mix NoOp/LocalRemove/PessimisticRemove/Update в одной лексеме.
  - **B6:** Update/PessimisticRemove ставят `isPendingDbOp=true`; NoOp/LocalRemove — НЕ ставят.
  - NOT_IN_DB лексема с 3 pristine: emit ровно 1 UpsertComponentValue (lexemeId=null) (finding #17).
  - **B3:** NOT_IN_DB с единственным empty pristine → после `commitAndCloseAllEdits` лексема удалена (filterNot пустых NOT_IN_DB) → `canAddLexeme`/`isCreatingLexeme` снова доступен (нет вечной блокировки CreateLexeme).
  - `closeAllEditModes` убирает pristine с empty edited (finding #18).

### 8.4 UseCase tests — `WordCardUseCaseImplTest.kt`

REWRITE: добавить тесты для:
- `addLexemeWithComponent` happy / null (тип не найден).
- `addComponentValue` happy → `AddComponentValueResult(lexeme, newId)`.
- `updateComponentValue` happy (re-read через `getLexemeById(lexemeId)`, lexemeId — параметр) / `updated == 0` → null.
- **B1.5 (явный тест):** `updateComponentValue` для soft-deleted типа — `lexemeApi.updateComponentValue` бросает `IllegalStateException` → метод ловит → возвращает `null` (graceful-fail). Mock бросает исключение, проверяем return null (не пробрасывание).
- `deleteComponentValue` ComponentRemoved / LexemeCascadeRemoved.
- `deleteComponentValue` не валидирует `removedAt` типа (нет `check(removedAt)`) — delete проходит (контраст с update; защита data-слоя, в WordCard недостижимо).
- `flowAvailableComponentTypes` — emit'ит mapped domain `ComponentType`.

Удалить тесты `addLexemeTranslation` / `deleteLexemeTranslation` / `addLexemeWithUserDefinedComponent` / `deleteDefinitionComponent` / `restoreLexeme(t, d)` / `getComponentTypes`.

---

## §9. Acceptance Tier 2

1. `./scripts/cc-build.sh :modules:screen:wordcard:assembleDebug` — зелёный.
2. `./scripts/cc-build.sh :modules:screen:wordcard:testDebugUnitTest` — зелёный, новый suite passes.
3. `./scripts/cc-build.sh :app:testDebugUnitTest` — зелёный, `WordCardUseCaseImplTest` rewrite passes.
4. Reducer/Handler не содержат строк `"Definition"` / `"Translation"` (grep clean).
5. `WordCardUseCase` интерфейс не содержит `translation`/`definition`-specific методов.

---

## §10. Риски

- **`commitAndCloseAllEdits` для NOT_IN_DB:** инвариант «один INSERT» требует чёткого порядка emit'ов в reducer (см. § 4.3). Тест `CommitAndCloseAllEditsTest` обязателен.
- **`LexemeDraftPromoted`:** в state ровно одна `NOT_IN_DB` лексема (инвариант `isCreatingLexeme`). Reducer заменяет черновик на готовую лексему **на его месте** (`lexemeList.map { if (it.id == NOT_IN_DB) merged else it }`) — порядок списка НЕ меняется, свежая лексема остаётся сверху (где создана через prepend). Случай «черновика нет» нереален (promote приходит только на свой черновик); спец-fallback не вводим.
- **Race `RefreshLexemeComponents` + `ComponentValueInserted` (B4-aware):** sequential dispatch Mate'а гарантирует порядок. **Pristine НЕ выкидываются Refresh'ем (B4)** — они флипаются только `ComponentValueInserted`. Если Inserted пришёл раньше Refresh — identity-flip pristine→saved отработает; следующий Refresh union-merge'ит этот saved по componentValueId (origin обновится из payload). Если Refresh раньше Inserted — pristine остаётся (Refresh его не трогает), затем Inserted его флипнет. Оба порядка корректны, потери нет.
- **`RefreshLexemeComponents` merge-policy (B4 union-by-id):** см. §6.2.2. Pristine не трогать; saved берётся из payload (`isEdit=false` — A10: успешный refresh закрывает edit коммитившегося); saved, отсутствующий в payload, удаляется. Тесты обязательны.
- **A10 isEdit lifecycle:** `CommitComponentValueEdit(Update)` НЕ закрывает edit (держит `isEdit=true`+`edited`+`pending`). Закрывает только success-`RefreshLexemeComponents`. При `OperationFailed` поле остаётся открытым → ввод не теряется на экране. **Выход с экрана теперь дожимает несохранённые компоненты** (flush-on-back, §6.2.3: `NavigateBack` коммитит и ждёт записи под лоадером); теряется только грязная правка самого слова (IS479, вне скоупа).
- **F9 (multi-commit при pending) — parity с переводом, НЕ блокер.** `CommitComponentValueEdit` guarded by pending. Если юзер закоммитил одно значение (pending=true) и быстро blur'ит второе до прихода refresh — второй commit проглатывается guard'ом (значение остаётся pristine/edited, не теряется из state, но не уходит в БД до следующего явного действия). Это **идентично** текущему поведению translation/definition (они тоже guarded). Не новый баг — сериализация коммитов. Принимаем как parity; отдельная оптимизация (очередь/flush-on-refresh) — backlog.
- **Trim нормализация (data-layer.md):** финальный `.trim()` текста перед записью — в `WordCardUseCaseImpl.addComponentValue/updateComponentValue` (единая точка по гайду), НЕ полагаться только на `.trim()` в `commitDecision` (reducer триммит для UX-решения Update-vs-Remove, но запись в БД нормализует UseCase).
