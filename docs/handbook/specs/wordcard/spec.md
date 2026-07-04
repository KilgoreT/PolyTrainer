# WordCard — карточка слова

Экран детального просмотра и редактирования одного слова словаря: заголовок, лексемы (значения) и их **компоненты** (значения произвольных типов — перевод, определение, транскрипция и т.д.). Открывается из списка слов (DictionaryTab/VocabularyTab) по выбранному элементу.

> Модель IS481 (generic component constructor). Перевод (Translation) больше **не** спец-сущность — это обычный built-in компонент (`ComponentTypeRef.BuiltIn(TRANSLATION)`). Определение, транскрипция и любой user-defined тип словаря работают по тому же обобщённому CRUD. Translation/definition-специфичных полей, Msg, эффектов и виджетов в коде нет.

## Бизнес-описание

WordCard — экран работы с одним словом словаря. Пользователь видит заголовок слова, дату добавления и список лексем (значений), у каждой — набор компонентов с inline-редактированием. Доступные для добавления типы компонентов приходят реактивным потоком из словаря (built-in + user-defined). На экране пользователь может:

- редактировать заголовок слова (inline; commit на потере фокуса);
- удалить слово целиком (через ⋮-меню в TopBar + confirm-dialog);
- создать новую лексему через FAB (локально, без обращения к БД);
- добавить компонент лексеме (tap chip из `ComponentChipsRow` → pristine-поле в edit-режиме);
- редактировать значение компонента (inline; autosave-commit на потере фокуса);
- удалить компонент через ✕ или пустой ввод + потеря фокуса;
- удалить лексему целиком (через `DeleteLexemeButton` + confirm для real-лексемы; для пустого NOT_IN_DB — без confirm);
- отменить недавнее удаление лексемы через snackbar+undo, восстановив её со всем snapshot'ом компонентов.

**Создание лексемы — двух-шаговое.** Тап FAB добавляет лексему **локально** (NOT_IN_DB-черновик) без обращения к БД. Запись в БД происходит при коммите первого («якорного») компонента: handler атомарно делает INSERT lexeme + write_quiz + первый component_value через `addLexemeWithComponent`, возвращает реальную `Lexeme`, reducer промоутит черновик (`LexemeDraftPromoted`).

**Cascade-delete на БД.** При удалении последнего компонента лексемы UseCase каскадно удаляет лексему в БД (`deleteComponentValue` → `RemoveComponentResult.LexemeCascadeRemoved(snapshot)`). UI получает `Msg.LexemeCascadeRemoved` со snapshot'ом, убирает лексему и показывает snackbar+undo.

**Порядок лексем:** newest-first. Новый NOT_IN_DB-черновик вставляется в **начало** списка; БД-лексемы сортируются маппером по `addDate DESC`.

> Источник правды по поведению: `docs/features/IS481_wordcard_components/03_layer_business.md`.

## User Stories

- Как пользователь, я хочу видеть слово и все его значения на одном экране, чтобы удерживать полный контекст без переходов.
- Как пользователь, я хочу inline-редактировать заголовок слова с auto-commit на потере фокуса, чтобы исправлять опечатки без модальных окон.
- Как пользователь, я хочу безопасно удалить слово через явное подтверждение, чтобы не потерять данные случайным жестом.
- Как пользователь, я хочу одним тапом FAB начать создавать новую лексему, чтобы не проходить через лишний промежуточный диалог.
- Как пользователь, я хочу chip'ами выбирать какой компонент добавить лексеме; предлагаются только типы из словаря, ещё не добавленные (для non-multiple) или добавляемые многократно (для `isMultiple`).
- Как пользователь, я хочу видеть когда сохранение в БД ещё не подтверждено, и не иметь возможности случайно отправить второй запрос — UI блокируется на время операции.
- Как пользователь, я хочу удалять компонент точечно (✕ или пустой ввод + потеря фокуса); если удалён последний — лексема каскадно удаляется в БД, но я могу отменить удаление.
- Как пользователь, я хочу отменить недавнее удаление лексемы через snackbar с кнопкой «Отменить», восстановив все её компоненты.
- Как пользователь, я хочу получать понятное уведомление при ошибке БД (`OperationFailed`) с возможностью retry для load типов / restore.
- Как пользователь, я хочу что бы IME-фокус был один на экран: открытие edit одного поля автоматически коммитит/закрывает другой активный edit.
- Как пользователь, я хочу что бы при нажатии «назад» моя открытая правка успела сохраниться (flush-on-back), а не потерялась.

## State

```kotlin
const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    /** Flush-on-back: «назад» при незавершённой записи — лоадер + отложенная навигация. */
    val isExiting: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val lexemeIdPendingDelete: Long? = null,
    /** Available component types словаря (driver для ChipsRow). */
    val availableComponentTypes: List<ComponentType> = emptyList(),
    /** Reducer-counter для уникальных pristine identity. */
    val nextPristineKey: Long = 1L,
) {
    val isLoaded: Boolean get() = wordState is WordState.Loaded
    val isCreatingLexeme: Boolean get() = lexemeList.any { it.id == NOT_IN_DB }
    val canAddLexeme: Boolean get() = !isPendingDbOp && !isCreatingLexeme
    /** Есть хоть одна in-flight запись компонента — driver flush-on-back. */
    val hasInFlightCommits: Boolean
        get() = lexemeList.any { l -> l.components.any { it.isCommitting } }
}

@Stable
sealed interface WordState {
    data object NotLoaded : WordState
    data class Loaded(
        val id: Long,
        val dictionaryId: Long,        // нужен reducer'у для лукапа типа / эффектов
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
    /** typeId всех НЕ-multiple компонентов — для скрытия их chip'ов в ChipsRow. */
    val addedNonMultipleTypeIds: Set<ComponentTypeId>
        get() = components.filterNot { it.isMultiple }.map { it.componentTypeId }.toSet()
}

@Stable
data class ComponentValueState(
    val key: ComponentValueKey,                  // identity: Pristine XOR Saved
    val componentTypeId: ComponentTypeId,
    val componentTypeRef: ComponentTypeRef,      // снимок типа (для fallback-лейбла)
    val isMultiple: Boolean,                     // cardinality «Несколько значений»
    val isEdit: Boolean = false,
    val isCommitting: Boolean = false,           // DB-операция в полёте (flush-on-back driver)
    val origin: String = "",
    val edited: String = "",
) {
    val isPristine: Boolean get() = key is ComponentValueKey.Pristine
    val componentValueId: ComponentValueId? get() = (key as? ComponentValueKey.Saved)?.componentValueId
    val pristineKey: Long? get() = (key as? ComponentValueKey.Pristine)?.pristineKey
}

/** Identity component_value: Pristine (pre-INSERT) XOR Saved (после квитанции). */
sealed interface ComponentValueKey {
    @JvmInline value class Pristine(val pristineKey: Long) : ComponentValueKey
    @JvmInline value class Saved(val componentValueId: ComponentValueId) : ComponentValueKey
}
```

### Per-field

- **`isLoading`** — флаг первичной загрузки слова.
- **`isPendingDbOp`** — глобальный pending-флаг БД-операции. Выставляется при отправке `DatasourceEffect`, сбрасывается каждой reducer-веткой Datasource Msg. UI блокирует контролы (`enabled = !isPendingDbOp`).
- **`isExiting`** — flush-on-back: пользователь нажал «назад», reducer закоммитил открытые правки и **держит** навигацию пока `hasInFlightCommits == true`. Edge-trigger на переходе в `(isExiting && !hasInFlightCommits)` эмитит `NavigationEffect.Back`. `OperationFailed` сбрасывает `isExiting=false` (остаёмся на экране).
- **`availableComponentTypes`** — реактивный список типов словаря (built-in + user-defined). Driver `ComponentChipsRow` и резолва лейбла (live-by-id с fallback на снимок `componentTypeRef`).
- **`nextPristineKey`** — счётчик для генерации уникальных `Pristine(pristineKey)`.
- **`lexemeIdPendingDelete`** — id лексемы с открытым confirm-dialog; `null` ⇒ диалог не показан.

### Computed properties

- `isLoaded`, `isCreatingLexeme`, `canAddLexeme` — derived (см. код).
- `hasInFlightCommits` — `any { component.isCommitting }`; держит навигацию при flush-on-back.
- `LexemeState.addedNonMultipleTypeIds` — typeId уже добавленных non-multiple компонентов (скрытие их chip'ов в ChipsRow). Derived на State, **не** в composable.

### `ComponentValueState` — toggle edit/view с буфером

- `key` — единый sealed identity (Pristine XOR Saved), невозможные состояния невыразимы (state-modeling «make impossible states impossible»).
- `origin` — last-known-good (синхронизировано с БД). Pristine до первого commit — `""`.
- `edited` — текущий ввод; решение по commit принимает `commitDecision()` по `edited.trim()` vs `origin.trim()`.
- `isCommitting` — компонент закоммичен и его DB-операция в полёте. Нужен чтобы (1) success-refresh закрывал edit ТОЛЬКО завершившегося компонента (по `componentValueId` И `isCommitting`); (2) `commitAndCloseAllEdits` / `CreateComponentValue` не реэмитили уже летящий компонент (skip if `isCommitting`).

### Инварианты (snapshot)

1. `| { l ∈ lexemeList : l.id == NOT_IN_DB } | ≤ 1` — максимум один локальный (несохранённый) черновик.
2. `∀ l1, l2 : l1 ≠ l2 ⇒ l1.id != l2.id` — уникальность id лексем.
3. `lexemeList.isNotEmpty() ⇒ wordState is Loaded`; `isLoading ⇒ wordState is NotLoaded ∧ lexemeList.isEmpty()`.
4. Global single-edit-mode: одновременно активен максимум один TextField (word.isEditMode XOR один компонент с `isEdit`).
5. `wordState is Loaded ⇒ wordState.value != ""`.
6. `isPendingDbOp ⇒ существует pending Datasource Effect без полученного confirm-Msg`. Каждая Datasource-ветка обязана снять pending.
7. Identity-инвариант: `key` — ровно один из Pristine/Saved (невыразимо иначе).
8. Domain-инвариант БД: real-лексема (id != NOT_IN_DB) **всегда** имеет ≥ 1 component_value. Пустая лексема валидна только как NOT_IN_DB-черновик (временно между FAB и первым commit).
9. Пустой NOT_IN_DB-черновик не должен «зависать»: `commitAndCloseAllEdits`/`closeAllEditModes`/`dropComponentMaybeCascade` фильтруют `id == NOT_IN_DB && components.isEmpty()`.
10. Flush-on-back: `isExiting ∧ !hasInFlightCommits` ⇒ (на переходе) `NavigationEffect.Back`; `isExiting` не сбрасывается до выхода, кроме `OperationFailed`.

## UI Layout

Подробная UI-логика — [ui.md](ui.md).

Виджеты (generic): `LexemeComponentsBlock` (блок компонентов лексемы), `ComponentValueField` (поле значения, edit/view toggle), `ComponentChipsRow` (chip'ы доступных типов из `availableComponentTypes`, минус `addedNonMultipleTypeIds`), `ComponentLabel` (лейбл значения: live-by-id из `availableComponentTypes` по `componentTypeId`, fallback на снимок `componentTypeRef` на окно загрузки до `ComponentTypesLoaded`).

Поле уже добавленного значения рендерится **всегда** (структурно из `ComponentValueState`), независимо от наличия типа в `availableComponentTypes` — lookup только для текста лейбла (баг-фикс F1).

## UI Messages

```kotlin
sealed interface Msg {
    // --- Top bar / dialogs ---
    data object OpenTopBarMenu : Msg
    data object CloseTopBarMenu : Msg
    data object OpenDeleteWordDialog : Msg
    data object CloseDeleteWordDialog : Msg
    data class RemoveWord(val wordId: Long) : Msg

    // --- Word edit ---
    data object EnterWordEditMode : Msg
    data class UpdateWordInput(val value: String) : Msg
    data object CommitWordChanges : Msg

    // --- Lexeme lifecycle ---
    data object CreateLexeme : Msg
    data class OpenDeleteLexemeDialog(val lexemeId: Long) : Msg
    data object CloseDeleteLexemeDialog : Msg
    data class RemoveLexeme(val lexemeId: Long) : Msg

    // --- ComponentValue (generic CRUD) ---
    data class CreateComponentValue(val lexemeId: Long, val typeId: ComponentTypeId) : Msg
    data class UpdateComponentValueInput(val lexemeId: Long, val key: ComponentValueKey, val value: String) : Msg
    data class EnterComponentValueEditMode(val lexemeId: Long, val key: ComponentValueKey) : Msg
    data class CommitComponentValueEdit(val lexemeId: Long, val key: ComponentValueKey) : Msg
    data class RemoveComponentValueRequested(val lexemeId: Long, val key: ComponentValueKey) : Msg

    // --- Component types stream ---
    data class ComponentTypesLoaded(val types: List<ComponentType>) : Msg
    data class ComponentTypesLoadFailed(val error: Throwable) : Msg
    data object RetryLoadComponentTypes : Msg

    // --- Datasource events ---
    data class WordLoaded(val word: Term) : Msg            // без componentTypes (приходят через FlowHandler)
    data object WordNotFound : Msg
    data class RefreshWord(val word: Term) : Msg
    data class RefreshLexemeComponents(val lexemeId: Long, val components: List<ComponentValue>) : Msg
    data class ComponentValueInserted(val lexemeId: Long, val pristineKey: Long, val newCvId: ComponentValueId) : Msg
    data class LexemeDraftPromoted(val newLexeme: Lexeme, val anchorPristineKey: Long) : Msg

    // --- Delete / undo ---
    data class LexemeCascadeRemoved(val removedLexeme: Lexeme) : Msg
    data class LexemeRemoved(val removedLexeme: Lexeme) : Msg
    data class UndoRestoreLexeme(val lexeme: Lexeme) : Msg
    data class RestoreLexemeFailed(val snapshot: Lexeme) : Msg

    // --- Errors / nav ---
    data class OperationFailed(@StringRes val messageRes: Int) : Msg
    data object NavigateBack : Msg
    data object NoOperation : Msg
}
```

> **Удалено относительно старой модели:** вся mirror-группа `CreateTranslation/UpdateTranslationInput/EnterTranslationEditMode/CommitTranslationEdit/RemoveTranslation/RefreshTranslation/TranslationDeleted/UndoRemoveTranslation` и симметричная Definition-группа; `LexemeCascadeRemovedWithUndo`, `UndoRemoveLexeme`, `RefreshLexemeList`, `RefreshTranslation/RefreshDefinition`, `ShowError`. `WordLoaded` больше не несёт `componentTypes` (типы идут отдельным `ComponentTypesLoaded`). `hasDefinitionComponent` удалён — заменён фильтром `availableComponentTypes`.

### Категоризация

- **Действия пользователя (component):** `CreateComponentValue`, `UpdateComponentValueInput`, `EnterComponentValueEditMode`, `CommitComponentValueEdit`, `RemoveComponentValueRequested` — одни и те же для перевода, определения и любого типа.
- **Lexeme/word:** `CreateLexeme`, `RemoveLexeme`, `EnterWordEditMode`/`UpdateWordInput`/`CommitWordChanges`, `RemoveWord`.
- **Types stream:** `ComponentTypesLoaded`/`ComponentTypesLoadFailed`/`RetryLoadComponentTypes`.
- **Datasource feedback:** `WordLoaded`, `WordNotFound`, `RefreshWord`, `RefreshLexemeComponents`, `ComponentValueInserted`, `LexemeDraftPromoted`, `LexemeCascadeRemoved`, `LexemeRemoved`.
- **Undo / retry:** `UndoRestoreLexeme`, `RestoreLexemeFailed`.
- **Errors / nav:** `OperationFailed`, `NavigateBack`, `NoOperation`.

### Reducer — ключевые правила

Сценарии редьюсера подробно — [user-scenarios.md](user-scenarios.md).

| Msg | Поведение reducer'а |
|---|---|
| `WordLoaded(word)` | `isLoading=false`, `isPendingDbOp=false`, `wordState=Loaded(...)`, `lexemeList = word.lexemeList.map { toLexemeState() }`; эмитит `LoadAvailableComponentTypes(dictionaryId)` (стартует FlowHandler). |
| `WordNotFound` | `isLoading=false`, `isPendingDbOp=false`, `NavigationEffect.Back` (silent exit). |
| `RefreshWord` | сбрасывает `isPendingDbOp`, пишет новый `value`, `isEditMode=false`, `edited=""`. |
| `CreateLexeme` | guard `isCreatingLexeme` → no-op. Иначе `commitAndCloseAllEdits()` + вставка `LexemeState(NOT_IN_DB)` в **начало**. |
| `OpenDeleteLexemeDialog` | `lexemeIdPendingDelete = id`. |
| `RemoveLexeme` | `NOT_IN_DB` → локальный `removeLexeme`. real → `isPendingDbOp=true` + `DatasourceEffect.RemoveLexeme`. |
| `CreateComponentValue(lexemeId, typeId)` | lookup type в `availableComponentTypes` (нет → no-op). `commitAndCloseAllEdits()`, затем append `Pristine`-компонент (`isEdit=true`) к лексеме (или восстановление выкинутого NOT_IN_DB-черновика), `nextPristineKey++`. |
| `UpdateComponentValueInput` | если компонент в `isEdit` — пишет `edited`, иначе no-op. |
| `EnterComponentValueEditMode` | `commitAndCloseAllEdits()`, затем `isEdit=true`, `edited=origin`. |
| `CommitComponentValueEdit` | по `commitDecision()`: **NoOp** → закрыть edit; **LocalRemove** → `dropComponentMaybeCascade`; **PessimisticRemove** → `isCommitting=true` + `RemoveComponentValue(cvId, lexId)`; **Update(text)** → `isCommitting=true` + `upsertEffect` (CreateLexeme / UpdateValue / AddValue). |
| `RemoveComponentValueRequested` | pristine → `dropComponentMaybeCascade`; saved с пустым origin → локальный remove; иначе `isCommitting=true` + `RemoveComponentValue`. |
| `RefreshLexemeComponents` | сбрасывает pending; **union-by-id merge**: для каждого domain-компонента — match по `componentValueId`; `isCommitting` → закрыть edit (`isEdit=false, isCommitting=false, edited=""`); `isEdit` → обновить только `origin`; иначе обновить `origin`; новые → `toComponentValueState()`. Pristine-хвост сохраняется. |
| `ComponentValueInserted` | identity-flip: pristine с `pristineKey` → `Saved(newCvId)`, `isEdit=false`, `isCommitting=false` (или дроп pristine, если Saved уже существует). |
| `LexemeDraftPromoted` | находит единственную NOT_IN_DB-лексему; исключает якорь **по ключу** (`anchorPristineKey`) из выживших pristine (он уже Saved в promoted), домерживает остальных как `isCommitting=true` и реэмитит для них `AddValue` (two-Msg продолжение). |
| `LexemeCascadeRemoved` / `LexemeRemoved` | `removeLexemeWithUndo`: убрать лексему, снять pending; при flush-on-back undo-снек не показывается, иначе `ShowSnackbarWithUndo(undoMsg = UndoRestoreLexeme(removed))`. |
| `UndoRestoreLexeme` | `isPendingDbOp=true` + `RestoreLexemeWithComponents(wordId, dictId, snapshot)`. |
| `RestoreLexemeFailed` | сброс pending + `ShowSnackbarWithRetry(retryMsg = UndoRestoreLexeme(snapshot))` (round-trip snapshot). |
| `ComponentTypesLoaded` | `availableComponentTypes = types`. |
| `ComponentTypesLoadFailed` | `ShowSnackbarWithRetry(retryMsg = RetryLoadComponentTypes)`. |
| `RetryLoadComponentTypes` | повторный `LoadAvailableComponentTypes(dictionaryId)`. |
| `OperationFailed` | `isPendingDbOp=false`, `isExiting=false`, снять `isCommitting` со всех компонентов + `ShowErrorSnackbar`. |
| `NavigateBack` | если `isExiting` → no-op; иначе `isExiting=true` + `commitAndCloseAllEdits()` (flush). |

**Глобальный guard.** `(isPendingDbOp || isExiting) && message.isGuardedByPending()` → no-op. Guarded: `RemoveWord, CommitWordChanges, RemoveLexeme, CommitComponentValueEdit, RemoveComponentValueRequested, EnterComponentValueEditMode, OpenTopBarMenu, OpenDeleteWordDialog, OpenDeleteLexemeDialog, EnterWordEditMode, CreateLexeme`. Datasource Msg и `Update*Input/Close*` проходят (иначе pending не снимется).

**Flush-on-back (пост-шаг).** После `reduceImpl` reducer проверяет edge-trigger: `readyNow = next.isExiting && !next.hasInFlightCommits`; если `readyNow && !readyBefore` → добавить `NavigationEffect.Back`. `isExiting` не сбрасывается.

**`commitDecision()` (CommitOutcome).** `!isEdit → NoOp`. По `edited.trim()` / `origin.trim()`: оба пусты → `LocalRemove`; edited пуст, origin нет → `PessimisticRemove`; `trimmed == originTrimmed` → `NoOp`; иначе → `Update(trimmed)`. Payload эффекта — всегда `textValuesOf(trimmed)`.

**`commitAndCloseAllEdits()`.** Для каждой лексемы: real → `commitRealLexeme` (per-component decision, skip `isCommitting`, для Update/PessimisticRemove ставит `isCommitting=true`); NOT_IN_DB → `commitDraftLexeme` (только **один** якорный `CreateLexeme`, якорь помечается `isCommitting=true`, остальные ждут `LexemeDraftPromoted`; пустой draft дропается). Word-edit тоже коммитится (`UpdateWord`), но flush-on-back его **не** удерживает (durability слова — IS479, вне скоупа).

## IO

### Effects

```kotlin
sealed interface DatasourceEffect : Effect {
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect
    data class RemoveLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    /** A3: три РАЗНЫЕ операции upsert (impossible states impossible). */
    sealed interface UpsertComponentValue : DatasourceEffect {
        val wordId: Long; val dictionaryId: Long
        val componentTypeId: ComponentTypeId; val componentTypeRef: ComponentTypeRef
        val data: TemplateValues
        data class CreateLexeme(/* + pristineKey */ ...) : UpsertComponentValue   // создание NOT_IN_DB якорным значением
        data class AddValue(/* + lexemeId, pristineKey */ ...) : UpsertComponentValue  // новое значение к существующей лексеме
        data class UpdateValue(/* + lexemeId, componentValueId */ ...) : UpsertComponentValue  // update existing
    }

    data class RemoveComponentValue(val componentValueId: ComponentValueId, val lexemeId: Long) : DatasourceEffect
    data class LoadAvailableComponentTypes(val dictionaryId: Long) : DatasourceEffect   // trigger для FlowHandler
    data class RestoreLexemeWithComponents(val wordId: Long, val dictionaryId: Long, val snapshot: Lexeme) : DatasourceEffect
}

sealed interface UiEffect : Effect {
    data class ShowSnackbarWithUndo(@StringRes val messageRes, @StringRes val actionLabelRes, val undoMsg: Msg)
    data class ShowSnackbarWithRetry(@StringRes val messageRes, @StringRes val actionLabelRes, val retryMsg: Msg)
    data class ShowErrorSnackbar(@StringRes val messageRes: Int)
}
```

`RestoreLexemeWithComponents` несёт **полный** `snapshot: Lexeme` — round-trip для retry при ошибке restore (snapshot не теряется при hard-delete). `NavigationEffect.Back` — через flush-on-back пост-шаг / `WordNotFound` / `RemoveWord` success.

### Effect → UseCase → Msg (`DatasourceEffectHandler`)

| Effect | UseCase | Success Msg | Failure Msg |
|---|---|---|---|
| `LoadWord` | `getTermById` | `WordLoaded(term)` / `WordNotFound` | `WordNotFound` (НЕ generic — иначе вечный спиннер) |
| `UpdateWord` | `updateWord` + `getTermById` | `RefreshWord(term)` | `OperationFailed(error_save_word)` |
| `RemoveWord` | `deleteWord` | `NavigateBack` (deleted>0) | `OperationFailed(error_remove_word)` |
| `RemoveLexeme` | `deleteLexeme` → `RemoveLexemeResult.Removed(snapshot)` | `LexemeRemoved(snapshot)` | `OperationFailed(error_remove_lexeme)` |
| `UpsertComponentValue.CreateLexeme` | `addLexemeWithComponent(wordId, dictId, ref, data)` | `LexemeDraftPromoted(lex, anchorPristineKey = effect.pristineKey)` | `OperationFailed(generic)` |
| `UpsertComponentValue.AddValue` | `addComponentValue(lexemeId, typeId, data)` → `AddComponentValueResult(lexeme, newCvId)` | **two-Msg burst:** `RefreshLexemeComponents(lexemeId, lexeme.components)` + `ComponentValueInserted(lexemeId, pristineKey, newCvId)` | `OperationFailed(generic)` |
| `UpsertComponentValue.UpdateValue` | `updateComponentValue(cvId, lexemeId, data)` | `RefreshLexemeComponents(lexemeId, lexeme.components)` | `OperationFailed(generic)` |
| `RemoveComponentValue` | `deleteComponentValue(cvId, lexemeId)` → `RemoveComponentResult` | `ComponentRemoved` → `RefreshLexemeComponents`; `LexemeCascadeRemoved(snapshot)` → `LexemeCascadeRemoved` | `OperationFailed(error_remove_lexeme)` |
| `RestoreLexemeWithComponents` | `restoreLexemeWithComponents(...)` + `getTermById` (resync) | `WordLoaded(term)` | `RestoreLexemeFailed(effect.snapshot)` |
| `LoadAvailableComponentTypes` | — (no-op в этом handler'е) | обрабатывает `AvailableComponentTypesFlowHandler` | — |

**UX-инвариант:** каждый control-path обязан вернуть ровно один разблокирующий Msg, иначе UI залипает с `isPendingDbOp=true`. `guarded { }`-обёртка: `CancellationException` пробрасывается, прочее → `OperationFailed(errorRes)`.

### Component types stream (`AvailableComponentTypesFlowHandler`)

`MateFlowHandler`. `runEffect` на `LoadAvailableComponentTypes(dictId)`: отменяет прежний job, подписывается на `useCase.flowAvailableComponentTypes(dictId)` (launch `CoroutineStart.UNDISPATCHED` — подписка регистрируется синхронно, иначе эмиссия до первого dispatch теряется). emit → `ComponentTypesLoaded(types)`; catch → `ComponentTypesLoadFailed(t)`. Resubscribe отменяет предыдущий job. Это **единственная** реактивная подписка экрана.

### Mappers

```kotlin
fun ComponentValue.toComponentValueState(): ComponentValueState = ComponentValueState(
    key = ComponentValueKey.Saved(id),
    componentTypeId = type.id,
    componentTypeRef = type.toRef(),
    isMultiple = type.isMultiple,
    origin = data.asText().orEmpty(),
)

fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = lexemeId.id,
    components = components.map { it.toComponentValueState() },
)
```

`TermApiEntity.toDomainEntity()` (в `app/.../WordCardUseCaseImpl.kt`) сортирует `lexemeList` по `addDate DESC` (newest-first) и несёт `dictionaryId`.

## UseCase

```kotlin
interface WordCardUseCase {
    // Word / lexeme
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): RemoveLexemeResult?

    // Generic component API
    suspend fun addLexemeWithComponent(wordId: Long, dictionaryId: Long, ref: ComponentTypeRef, data: TemplateValues): Lexeme?
    suspend fun addComponentValue(lexemeId: Long, componentTypeId: ComponentTypeId, data: TemplateValues): AddComponentValueResult?
    suspend fun updateComponentValue(componentValueId: ComponentValueId, lexemeId: Long, data: TemplateValues): Lexeme?
    suspend fun deleteComponentValue(componentValueId: ComponentValueId, lexemeId: Long): RemoveComponentResult?
    suspend fun restoreLexemeWithComponents(wordId: Long, dictionaryId: Long, snapshot: Lexeme): Lexeme?

    fun flowAvailableComponentTypes(dictionaryId: Long): Flow<List<ComponentType>>
}

data class AddComponentValueResult(val lexeme: Lexeme, val newComponentValueId: ComponentValueId)
sealed interface RemoveComponentResult {
    data class ComponentRemoved(val lexeme: Lexeme) : RemoveComponentResult
    data class LexemeCascadeRemoved(val removedLexeme: Lexeme) : RemoveComponentResult
}
sealed interface RemoveLexemeResult { data class Removed(val snapshot: Lexeme) : RemoveLexemeResult }
```

### Семантика

- `null` от UseCase — реальная БД-ошибка / failure → `OperationFailed`. Non-null — success.
- `addComponentValue` возвращает **квитанцию** `AddComponentValueResult(lexeme, newComponentValueId)` — reducer match'ит `pristineKey ↔ newCvId` (детерминированный identity-flip).
- `deleteComponentValue`: 0 оставшихся компонентов → impl делает `deleteLexeme` и возвращает `LexemeCascadeRemoved(snapshot)`.
- `updateComponentValue` принимает `lexemeId` параметром (re-read через `getLexemeById`, reverse-lookup удалён — A1).
- `restoreLexemeWithComponents` принимает полный `snapshot` (маппинг `components → List<Pair<ref, data>>` внутри impl), atomic compound INSERT lexeme + write_quiz + N component_values; try/catch обязателен (иначе snapshot теряется минуя `RestoreLexemeFailed`).
- **Trim** всех String-входов перед записью (defense-in-depth, idempotent с trim в `commitDecision`).
- `dictionaryId` приходит **параметром** из State (`WordState.Loaded.dictionaryId`) — `resolveCurrentDictionaryId` через Prefs удалён.

### БД-инвариант (data-слой)

«В БД лексема имеет ≥ 1 component_value.» При удалении последнего через `deleteComponentValue` data-слой каскадно удаляет лексему → `LexemeCascadeRemoved`. Тип soft-deleted в Manager → его значения каскадно soft-удаляются и фильтруются маппером (`value.removedAt==null && type.removedAt==null`) — orphan на карточку не грузится (single-screen Android, гонки нет).

### Domain types (dependency)

`Term` живёт в `modules/screen/wordcard/.../entity`. `Lexeme`, `ComponentType`, `ComponentTypeId`, `ComponentTypeRef`, `ComponentValue`, `ComponentValueId`, `TemplateValues`, `toRef()` — в `modules/domain/lexeme` (pure-JVM). Wordcard импортирует напрямую. Translation — built-in компонент `ComponentTypeRef.BuiltIn(TRANSLATION)`, **не** отдельная сущность.

## Ссылки

- [user-scenarios.md](user-scenarios.md) — сценарии редьюсера (создание/коммит/удаление компонентов, promotion, flush-on-back, undo/retry).
- [ui.md](ui.md) — UI-логика (виджеты `LexemeComponentsBlock`/`ComponentValueField`/`ComponentChipsRow`/`ComponentLabel`, резолв лейбла, рендер-инвариант).
- `docs/features/IS481_wordcard_components/03_layer_business.md` — бизнес-контракт generic-модели (источник правды).
