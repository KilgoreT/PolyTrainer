# WordCard — карточка слова

Экран детального просмотра и редактирования одного слова словаря: заголовок, лексемы (значения), их переводы и определения. Открывается из списка слов (DictionaryTab/VocabularyTab) по выбранному элементу.

## Бизнес-описание

WordCard — экран работы с одним словом словаря. Пользователь видит заголовок слова, дату добавления и список лексем (значений) с возможностью inline-редактирования. Каждая лексема может иметь перевод и/или определение. На экране пользователь может:

- редактировать заголовок слова (inline; commit на потере фокуса);
- удалить слово целиком (через ⋮-меню в TopBar + confirm-dialog);
- создать новую лексему через FAB (локально, без обращения к БД);
- редактировать перевод/определение лексемы (inline; commit на потере фокуса);
- удалять перевод/определение через ✕ или пустой ввод + потеря фокуса;
- удалить лексему целиком через `DeleteLexemeButton` в карточке (+ confirm-dialog для real-лексемы; для пустой NOT_IN_DB — без confirm);
- отменить недавнее удаление через snackbar+undo (для translation/definition/lexeme).

**Создание лексемы — двух-шаговое:** тап FAB добавляет лексему **локально** в state без обращения к БД (NOT_IN_DB-черновик). Запись в БД происходит только при первом подтверждённом вводе перевода или определения (handler атомарно делает insert лексемы + sub-сущности, возвращает реальный id, reducer заменяет `NOT_IN_DB` на реальный id).

**Cascade-delete на БД:** при удалении последней sub-сущности (translation или definition) лексема каскадно удаляется в БД через `lexemeApi.deleteLexeme(id)`. UI получает `LexemeCascadeRemovedWithUndo`-Msg; лексема **остаётся** в UI как пустой NOT_IN_DB-черновик (для возможности undo через snackbar).

**Порядок лексем:** newest-first. Новая NOT_IN_DB-лексема вставляется в **начало** списка; маппер `TermApiEntity.toDomainEntity()` сортирует БД-лексемы по `addDate DESC`.

## User Stories

- Как пользователь, я хочу видеть слово и все его значения на одном экране, чтобы удерживать полный контекст без переходов.
- Как пользователь, я хочу inline-редактировать заголовок слова с auto-commit на потере фокуса, чтобы исправлять опечатки без модальных окон.
- Как пользователь, я хочу безопасно удалить слово через явное подтверждение, чтобы не потерять данные случайным жестом.
- Как пользователь, я хочу одним тапом FAB начать создавать новую лексему, чтобы не проходить через лишний промежуточный диалог.
- Как пользователь, я хочу chip'ами «Перевод» / «Определение» гибко выбирать что заполнить у лексемы.
- Как пользователь, я хочу видеть когда сохранение в БД ещё не подтверждено, и не иметь возможности случайно отправить второй запрос — UI блокируется на время операции.
- Как пользователь, я хочу удалять перевод/определение точечно (✕ или пустой ввод + потеря фокуса); если удалена последняя sub-сущность — лексема каскадно удаляется в БД, но остаётся пустым черновиком в UI с возможностью undo.
- Как пользователь, я хочу отменить недавнее удаление через snackbar c кнопкой «Отменить» (4 секунды).
- Как пользователь, я хочу получать понятное уведомление при ошибке БД, чтобы понимать что произошло.
- Как пользователь, я хочу что бы IME-фокус был один на экран: открытие edit одного поля автоматически коммитит/закрывает другой активный edit.

## State

```kotlin
package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Stable
import me.apomazkin.mate.Effect
import me.apomazkin.wordcard.entity.Lexeme
import java.util.Date

const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val lexemeIdPendingDelete: Long? = null,
) {
    val isLoaded: Boolean
        get() = when (wordState) {
            is WordState.Loaded -> true
            WordState.NotLoaded -> false
        }

    val isCreatingLexeme: Boolean
        get() = lexemeList.any { it.id == NOT_IN_DB }

    val canAddLexeme: Boolean
        get() = !isPendingDbOp && !isCreatingLexeme
}

@Stable
data class TopBarState(val isMenuOpen: Boolean = false)

@Stable
sealed interface WordState {
    data object NotLoaded : WordState
    data class Loaded(
        val id: Long,
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
    val translation: TextValueState? = null,
    val definition: TextValueState? = null,
) {
    val canAddTranslation: Boolean get() = translation == null
    val canAddDefinition: Boolean get() = definition == null
}

@Stable
data class TextValueState(
    val isEdit: Boolean = false,
    val origin: String = "",
    val edited: String = "",
)
```

### Per-field

- **`topBarState`** — суб-стейт верхней панели (⋮-меню удаления слова).
- **`isLoading`** — флаг первичной загрузки слова из БД.
- **`isPendingDbOp`** — глобальный pending-флаг БД-операции. Выставляется reducer-веткой UI Msg, отправляющей `DatasourceEffect`; сбрасывается reducer-веткой соответствующего Datasource Msg (Refresh* / WordLoaded / WordNotFound / NavigateBack / ShowError / TranslationDeleted / DefinitionDeleted / LexemeCascadeRemovedWithUndo / LexemeRemoved). UI читает поле для блокировки кнопок (`enabled = !isPendingDbOp`).
- **`wordState`** — sealed sum `NotLoaded | Loaded`.
- **`lexemeList`** — упорядоченный список лексем слова (newest-first).
- **`lexemeIdPendingDelete`** — id лексемы, для которой открыт confirm-dialog удаления. `null` ⇒ диалог не показан.

### Computed properties

- `isLoaded: Boolean` — derived из `wordState`.
- `isCreatingLexeme: Boolean` — derived из списка (`any { it.id == NOT_IN_DB }`).
- `canAddLexeme: Boolean` — `!isPendingDbOp && !isCreatingLexeme`. Используется как `enabled` для FAB.

### `WordState`

- `NotLoaded` — начальное состояние до прихода `WordLoaded`. Полей `id/added/value` физически нет.
- `Loaded(id, added, value, isEditMode, edited, showWarningDialog)` — слово получено из БД; `id != NOT_IN_DB`; `value` непустой по контракту.

### `LexemeState`

- `id: Long` — реальный id из БД либо `NOT_IN_DB = -1L` для свежесозданной локальной лексемы.
- `translation`/`definition: TextValueState?` — nullable: `null` = sub-сущность отсутствует.

### `TextValueState`

Паттерн toggle edit/view с буфером:
- `isEdit: Boolean` — режим редактирования.
- `origin: String` — last known good значение, синхронизировано с БД. Для свежего chip — пустая строка до первого Commit.
- `edited: String` — текущий ввод; при Commit → новый origin через Refresh; при no-op commit / cascade — сбрасывается в `""`.

### Инварианты (snapshot)

1. `wordState is Loaded ∧ wordState.isEditMode == false ⇒ wordState.edited == ""`.
2. `| { l ∈ lexemeList : l.id == NOT_IN_DB } | ≤ 1` — максимум одна локальная (несохранённая) лексема.
3. `∀ l1, l2 ∈ lexemeList : l1 ≠ l2 ⇒ l1.id != l2.id` — уникальность id.
4. Для каждой лексемы: `translation != null ∧ translation.isEdit == false ⇒ translation.edited == ""` (то же для definition).
5. `lexemeList.isNotEmpty() ⇒ wordState is Loaded` (структурный FK).
6. `isLoading == true ⇒ wordState is NotLoaded ∧ lexemeList.isEmpty()`.
7. Global single-edit-mode: одновременно активен максимум один TextField (word.isEditMode XOR одна chip с isEdit).
8. `wordState is Loaded ⇒ wordState.value != ""`.
9. `isPendingDbOp == true ⇒ существует pending Datasource Effect, для которого reducer ещё не получил confirm-Msg`.
10. Domain-инвариант БД (data-слой): real-лексема (id != NOT_IN_DB) **всегда** имеет хотя бы одну sub-сущность. Пустая лексема (translation = null ∧ definition = null) валидна только как NOT_IN_DB-черновик.

## UI Messages

```kotlin
package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

sealed interface Msg {

    // --- Top bar menu ---
    data object OpenTopBarMenu : Msg
    data object CloseTopBarMenu : Msg

    // --- Delete word ---
    data object OpenDeleteWordDialog : Msg
    data object CloseDeleteWordDialog : Msg
    data class RemoveWord(val wordId: Long) : Msg

    // --- Word edit ---
    data object EnterWordEditMode : Msg
    data class UpdateWordInput(val value: String) : Msg
    data object CommitWordChanges : Msg

    // --- Lexeme ---
    data object CreateLexeme : Msg
    data class OpenDeleteLexemeDialog(val lexemeId: Long) : Msg
    data object CloseDeleteLexemeDialog : Msg
    data class RemoveLexeme(val lexemeId: Long) : Msg

    // --- Translation chip ---
    data class CreateTranslation(val lexemeId: Long) : Msg
    data class UpdateTranslationInput(val lexemeId: Long, val value: String) : Msg
    data class EnterTranslationEditMode(val lexemeId: Long) : Msg
    data class CommitTranslationEdit(val lexemeId: Long) : Msg
    data class RemoveTranslation(val lexemeId: Long) : Msg

    // --- Definition chip (симметрично Translation) ---
    data class CreateDefinition(val lexemeId: Long) : Msg
    data class UpdateDefinitionInput(val lexemeId: Long, val value: String) : Msg
    data class EnterDefinitionEditMode(val lexemeId: Long) : Msg
    data class CommitDefinitionEdit(val lexemeId: Long) : Msg
    data class RemoveDefinition(val lexemeId: Long) : Msg

    // --- Navigation + feedback ---
    data object NavigateBack : Msg
    data object NoOperation : Msg

    // --- Datasource Msg ---
    data class WordLoaded(val word: Term) : Msg
    data object WordNotFound : Msg
    data class RefreshWord(val word: Term) : Msg
    data class RefreshTranslation(val lexemeId: Long, val translation: String?) : Msg
    data class RefreshDefinition(val lexemeId: Long, val definition: String?) : Msg
    data class RefreshLexemeList(val lexemes: List<Lexeme>) : Msg
    data class ShowError(@StringRes val messageRes: Int) : Msg

    // --- Delete events с payload для undo ---
    data class TranslationDeleted(val lexemeId: Long, val removedValue: String) : Msg
    data class DefinitionDeleted(val lexemeId: Long, val removedValue: String) : Msg
    data class LexemeCascadeRemovedWithUndo(
        val lexemeId: Long,
        val removedTranslation: String?,
        val removedDefinition: String?,
    ) : Msg
    data class LexemeRemoved(
        val lexemeId: Long,
        val translation: String?,
        val definition: String?,
    ) : Msg

    // --- Undo Msg ---
    data class UndoRemoveTranslation(val lexemeId: Long, val value: String) : Msg
    data class UndoRemoveDefinition(val lexemeId: Long, val value: String) : Msg
    data class UndoRemoveLexeme(val translation: String?, val definition: String?) : Msg
}
```

### Категоризация

- **Действия пользователя:** `RemoveWord`, `CommitWordChanges`, `UpdateWordInput`, `EnterWordEditMode`, `CreateLexeme`, `RemoveLexeme`, парные `Create* / Update*Input / Enter*EditMode / Commit*Edit / Remove*` для Translation и Definition.
- **Toggles / диалоги:** `OpenTopBarMenu`/`CloseTopBarMenu`, `OpenDeleteWordDialog`/`CloseDeleteWordDialog`, `OpenDeleteLexemeDialog`/`CloseDeleteLexemeDialog`.
- **Навигация:** `NavigateBack`.
- **No-op:** `NoOperation`.
- **Datasource:** `WordLoaded`, `WordNotFound`, `RefreshWord`, `RefreshTranslation`, `RefreshDefinition`, `RefreshLexemeList`, `ShowError`.
- **Delete events (success → snackbar+undo):** `TranslationDeleted`, `DefinitionDeleted`, `LexemeCascadeRemovedWithUndo`, `LexemeRemoved`.
- **Undo:** `UndoRemoveTranslation`, `UndoRemoveDefinition`, `UndoRemoveLexeme`.

### Reducer — ключевые правила

- **Инициализация:** loading идёт через `initEffects = setOf(DatasourceEffect.LoadWord(wordId))` в ViewModel. `WordCardState()` стартует с `wordState = NotLoaded, isLoading = true, isPendingDbOp = false`.
- **Глобальный guard `isPendingDbOp`:** см. `Msg.isGuardedByPending()`. При `state.isPendingDbOp == true` блокируются: `RemoveWord, CommitWordChanges, RemoveLexeme, CommitTranslationEdit, RemoveTranslation, CommitDefinitionEdit, RemoveDefinition, OpenTopBarMenu, OpenDeleteWordDialog, OpenDeleteLexemeDialog, EnterWordEditMode, CreateLexeme, CreateTranslation, EnterTranslationEditMode, CreateDefinition, EnterDefinitionEditMode`. Остальные (`Update*Input`, `Close*`, `Cancel*`, `Navigate*`, `NoOperation`, все Datasource Msg) проходят (Datasource — обязательно, иначе pending не снимется).
- **Хелпер `commitAndCloseAllEdits()`:** перед открытием любого нового edit-mode (`EnterWordEditMode`, `CreateLexeme`, `CreateTranslation/Definition`, `EnterTranslationEditMode/EnterDefinitionEditMode`) reducer вызывает атомарный helper, который для каждого активного edit с грязным `edited` (≠origin && не пустой) эмитит соответствующий `Update*`-эффект и **оптимистично** локально коммитит: `loaded.value = edited` для word, `TextValueState.origin = edited` для translation/definition (UI не показывает старый текст до прихода Refresh). Пустые/неизменённые edit'ы — просто закрываются (single-edit invariant 7).
- **`CreateLexeme` — полностью локальный:** `lexemeList = listOf(LexemeState(id = NOT_IN_DB, ...)) + closed.lexemeList` (вставка в **начало**). Guard: уже есть `NOT_IN_DB` → no-op.
- **`OpenDeleteLexemeDialog`:** если лексема `NOT_IN_DB` и **пуста** (`translation == null && definition == null`) — удаляется сразу через `removeLexeme(id)`, без открытия диалога. Иначе — `state.copy(lexemeIdPendingDelete = id)`.
- **`RemoveLexeme`:** независимо от ветви, `lexemeIdPendingDelete` сбрасывается в `null` (закрывает confirm-dialog). Далее:
  - `lexemeId == NOT_IN_DB` — локально `removeLexeme(NOT_IN_DB)`, без эффекта.
  - real id — `DatasourceEffect.RemoveLexeme(wordId, lexemeId)`, `isPendingDbOp = true`. Handler возвращает `Msg.LexemeRemoved(lexemeId, translation?, definition?)` с snapshot для undo.
- **`Commit*Edit` ветвление (4 ветви):**
  - **1a** `edited.isBlank() ∧ origin.isEmpty()` — локальный nullify `translation/definition = null`, без эффекта; для `NOT_IN_DB` с обоими null после nullify — `removeLexeme(id)`.
  - **1** `edited.isBlank() ∧ origin.isNotEmpty()` — pessimistic Remove: сбрасываем `isEdit=false, edited=""`, шлём `DatasourceEffect.RemoveTranslation/Definition` с `currentValue = origin`.
  - **2** `edited == origin` — no-op коммит: `isEdit=false, edited=""`, без эффекта.
  - **3** иначе — Update: `isEdit=false, edited=""`, шлём `UpdateLexeme*` с `lexemeId = if (NOT_IN_DB) null else id`. Реальный id придёт через `Refresh*`.
- **`Remove*` (тап ✕ chip)** для real-лексемы:
  - если `origin.isEmpty()` (chip только что создан, не сохранён в БД) — локальный nullify без эффекта;
  - иначе — `DatasourceEffect.Remove*`, `isPendingDbOp = true`. Handler возвращает `*Deleted` (с snapshot для undo) или `LexemeCascadeRemovedWithUndo`.
- **`Refresh*` ветвления:**
  - Лексема с реальным `lexemeId` из payload **существует в `lexemeList`** — reducer обновляет только `origin`, сохраняет активный `isEdit/edited` пользователя.
  - Лексема с реальным `lexemeId` **отсутствует**, есть лексема с `NOT_IN_DB` — её id заменяется на реальный, конструируется `TextValueState(origin = value, isEdit = false, edited = "")`.
- **`LexemeCascadeRemovedWithUndo`:** лексема в state превращается в пустой `NOT_IN_DB`-черновик (`id = NOT_IN_DB, translation = null, definition = null`); эмитится `UiEffect.ShowSnackbarWithUndo` с соответствующим текстом + undo Msg (`UndoRemoveTranslation(NOT_IN_DB, value)` либо `UndoRemoveDefinition(NOT_IN_DB, value)`).
- **`TranslationDeleted`/`DefinitionDeleted`:** локально nullify соответствующего поля + snackbar `UiEffect.ShowSnackbarWithUndo` с `UndoRemoveTranslation/Definition(lexemeId, removedValue)`.
- **`LexemeRemoved`:** локально `removeLexeme(id)`; если есть translation/definition snapshot — snackbar с `UndoRemoveLexeme(translation, definition)`.
- **`Undo*` Msg:** эмитят соответствующие `DatasourceEffect.UpdateLexeme*(...)` либо `DatasourceEffect.RestoreLexeme(...)` для re-INSERT.
- **`ShowError`:** `state.copy(isPendingDbOp = false) to setOf(UiEffect.ShowErrorSnackbar(messageRes))` — снэкбар-ошибка через UiHost.
- **Сброс `isPendingDbOp = false`** обязателен в reducer-ветке каждого Datasource Msg.

## UI Layout

См. подробную UI-разметку: [wordcard-ui.md](wordcard-ui.md).

## IO

### Effects

```kotlin
package me.apomazkin.wordcard.mate

import me.apomazkin.mate.Effect

sealed interface DatasourceEffect : Effect {
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    data class UpdateLexemeTranslation(
        val wordId: Long,
        /** null ⇒ insert новой лексемы с переводом. */
        val lexemeId: Long?,
        val translation: String,
    ) : DatasourceEffect
    data class RemoveTranslation(val lexemeId: Long, val currentValue: String) : DatasourceEffect

    data class UpdateLexemeDefinition(
        val wordId: Long,
        /** null ⇒ insert новой лексемы с определением. */
        val lexemeId: Long?,
        val definition: String,
    ) : DatasourceEffect
    data class RemoveDefinition(val lexemeId: Long, val currentValue: String) : DatasourceEffect

    /** Re-INSERT лексемы после full-delete (undo). Хотя бы один из не-null. */
    data class RestoreLexeme(
        val wordId: Long,
        val translation: String?,
        val definition: String?,
    ) : DatasourceEffect
}

sealed interface UiEffect : Effect {
    /** Snackbar c action-кнопкой; при нажатии action отправляется [undoMsg]. */
    data class ShowSnackbarWithUndo(
        @StringRes val messageRes: Int,
        @StringRes val actionLabelRes: Int,
        val undoMsg: Msg,
    ) : UiEffect

    /** Snackbar без action для ошибок. */
    data class ShowErrorSnackbar(@StringRes val messageRes: Int) : UiEffect
}
```

`NavigationEffect.Back` — через `Msg.NavigateBack` reducer-ветку.

### Effect → Action → Msg (success)

| Effect | UseCase | Success Msg | Failure Msg |
|---|---|---|---|
| `LoadWord(wordId)` | `getTermById` | `WordLoaded(term)` / `WordNotFound` | `WordNotFound` (silent exit) |
| `RemoveWord(wordId)` | `deleteWord` | `NavigateBack` (если deleted > 0) | `ShowError(R.string.word_card_error_remove_word)` |
| `UpdateWord(wordId, value)` | `updateWord` + `getTermById` (resync) | `RefreshWord(term)` | `ShowError(error_save_word)` / `ShowError(error_refresh_word)` |
| `RemoveLexeme(wordId, lexemeId)` | `deleteLexeme` (с snapshot до delete) | `LexemeRemoved(lexemeId, translation?, definition?)` | `ShowError(error_remove_lexeme)` |
| `UpdateLexemeTranslation(wordId, lexemeId?, translation)` | `addLexemeTranslation` (insert либо upsert) | `RefreshTranslation(realId, value)` | `ShowError(error_save_translation)` |
| `RemoveTranslation(lexemeId, currentValue)` | `deleteLexemeTranslation` | `TranslationDeleted(realId, currentValue)` / `LexemeCascadeRemovedWithUndo(lexemeId, removedTranslation = currentValue, null)` | `ShowError(error_remove_translation)` |
| `UpdateLexemeDefinition` | `addLexemeDefinition` (insert либо upsert) | `RefreshDefinition(realId, value)` | `ShowError(error_save_definition)` |
| `RemoveDefinition(lexemeId, currentValue)` | `deleteLexemeDefinition` | `DefinitionDeleted(realId, currentValue)` / `LexemeCascadeRemovedWithUndo(lexemeId, null, removedDefinition = currentValue)` | `ShowError(error_remove_definition)` |
| `RestoreLexeme(wordId, translation?, definition?)` | `restoreLexeme` (atomic insert lex + translation; rollback при провале + definition) | `RefreshLexemeList(lexemes)` | `ShowError(error_restore_lexeme)` |

`UiEffect.ShowSnackbarWithUndo` → `UiHost.showSnackbarWithAction(messageRes, actionLabelRes): Boolean`. Если `true` (нажата кнопка) — handler эмитит `undoMsg`.

`UiEffect.ShowErrorSnackbar` → `UiHost.showSnackbar(messageRes)`.

**UX-инвариант:** каждый control-path каждого handler'а обязан вернуть ровно один разблокирующий Msg — иначе UI зависнет с `isPendingDbOp = true`. Catch-блок в `DatasourceEffectHandler` маппит exception на соответствующий `ShowError` per effect-type.

**Atomicity contracts:**
- `addLexemeTranslation(lexemeId = null, ...)` / `addLexemeDefinition(lexemeId = null, ...)` — impl выполняет insert лексемы + write-quiz + insert sub-сущности в одной Room-транзакции через `lexemeApi.addLexemeWithTranslation` / `addLexemeWithDefinition`.
- `restoreLexeme(translation, definition)`:
  - input-guard: если **оба** `null` — `null` (no-op, ошибка логирования).
  - оба non-null — `insertLexemeWithTranslation` + `updateLexemeDefinition` двумя шагами; при провале второго — rollback первой через `lexemeApi.deleteLexeme(id)`.
  - один non-null — единичный `insertLexemeWith*`; rollback не нужен (один шаг).

**Trim:** все String-входы в UseCase (`updateWord.value`, `addLexemeTranslation.translation`, `addLexemeDefinition.definition`, `restoreLexeme.translation/definition`) триммятся (`String.trim()`) перед обращением к `CoreDbApi`. Контракт data-слоя "write what you give" сохраняется; нормализация — в UseCase.

### Mappers

**`TermApiEntity.toDomainEntity()`** (в `app/.../WordCardUseCaseImpl.kt`):

```kotlin
fun TermApiEntity.toDomainEntity(): Term = Term(
    wordId = WordId(word.id),
    word = Word(word.value),
    addedDate = word.addDate,
    changedDate = word.changeDate,
    removedDate = word.removeDate,
    // newest-first для UI-инварианта "новая лексема сверху".
    lexemeList = lexemes
        .sortedByDescending { it.addDate }
        .map { it.toDomainEntity() }
)
```

**`Lexeme.toLexemeState()`** (top-level extension в `mate/State.kt`):

```kotlin
fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = lexemeId.id,
    translation = translation?.let { TextValueState(origin = it.value, isEdit = false, edited = "") },
    definition  = definition?.let { TextValueState(origin = it.value, isEdit = false, edited = "") },
)
```

### Subscribers

**Отсутствуют.** WordCard не имеет cross-screen реактивности: `WordCardUseCase` экспонирует только `suspend`-методы.

### UiHost

```kotlin
package me.apomazkin.wordcard.deps

interface UiHost {
    suspend fun showSnackbar(messageRes: Int)
    suspend fun showSnackbarWithAction(messageRes: Int, actionLabelRes: Int): Boolean
}
```

Реализация `UiHostImpl` оборачивает `SnackbarHostState` + `Context` для резолва строк. Инжектится в `WordCardViewModel` через `@AssistedInject`; реализация создаётся на стороне Composable.

## UseCase

```kotlin
package me.apomazkin.wordcard.deps

import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean

    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?

    suspend fun addLexemeTranslation(
        wordId: Long,
        lexemeId: Long?,
        translation: String,
    ): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?

    suspend fun addLexemeDefinition(
        wordId: Long,
        lexemeId: Long?,
        definition: String,
    ): Lexeme?
    suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?

    suspend fun restoreLexeme(
        wordId: Long,
        translation: String?,
        definition: String?,
    ): List<Lexeme>?
}

sealed interface RemoveTranslationResult {
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveDefinitionResult {
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}
```

### Семантика возврата

- `null` — реальная БД-ошибка / failure. Handler конвертирует в `ShowError`.
- Non-null — success.
- Sealed result (`RemoveTranslationResult` / `RemoveDefinitionResult`) — handler различает «sub-сущность удалена, лексема осталась» vs «лексема каскадно ушла».

### БД-инвариант (data-слой)

«В БД лексема имеет ≥ 1 sub-сущность.» При удалении последней через `delete*` data-слой каскадно удаляет лексему. UI видит этот путь через `LexemeCascadeRemoved` result → `Msg.LexemeCascadeRemovedWithUndo`. Для state локальная (`NOT_IN_DB`) пустая лексема валидна **временно** между `CreateLexeme` (тап FAB) и первым `Commit Translation/Definition`.

## Тестовые сценарии

### Reducer (snapshot)

- **WordLoaded атомарность:** `(NotLoaded, isLoading=true)` → `WordLoaded(term)` → `wordState = Loaded(...)`, `isLoading = false`, `isPendingDbOp = false`, `lexemeList = term.lexemeList.map { toLexemeState() }` (уже newest-first из маппера).
- **WordNotFound silent exit:** `(NotLoaded, isLoading=true)` → `WordNotFound` → `isLoading = false`, `isPendingDbOp = false`, эффект `NavigationEffect.Back`.
- **CreateLexeme prepend:** `(Loaded, lexemeList = [real])` → `CreateLexeme` → `lexemeList[0] == NOT_IN_DB`, без эффектов.
- **CreateLexeme guard:** `(Loaded, lexemeList = [NOT_IN_DB, ...])` → повторный `CreateLexeme` → `state to emptySet()`.
- **OpenDeleteLexemeDialog для пустой NOT_IN_DB:** `LexemeState(NOT_IN_DB, translation=null, definition=null)` → `OpenDeleteLexemeDialog(NOT_IN_DB)` → лексема удалена из списка, диалог не открыт, `lexemeIdPendingDelete = null`.
- **OpenDeleteLexemeDialog для real-лексемы:** `lexemeIdPendingDelete = lexemeId`, диалог откроется на UI.
- **Глобальный guard isPendingDbOp:** `(isPendingDbOp=true)` → `RemoveWord` → `state to emptySet()`.
- **CommitTranslationEdit ветвь 1a:** `LexemeState(NOT_IN_DB, translation = TextValueState(origin="", edited="  ", isEdit=true))` → `translation = null`, без эффекта.
- **CommitTranslationEdit ветвь 1 pessimistic Remove:** `LexemeState(realId, translation = TextValueState(origin="x", edited="", isEdit=true))` → reducer сбрасывает `isEdit=false, edited=""`, шлёт `DatasourceEffect.RemoveTranslation(realId, currentValue="x")`, `isPendingDbOp = true`. Финал — `Msg.TranslationDeleted` + snackbar.
- **CommitTranslationEdit ветвь 3 first-Commit:** `LexemeState(NOT_IN_DB, ... edited="hello", isEdit=true)` → `UpdateLexemeTranslation(wordId, lexemeId = null, "hello")`, `isPendingDbOp = true`. После `RefreshTranslation(newId, "hello")` reducer заменяет `NOT_IN_DB → newId`.
- **RemoveTranslation для real-лексемы с empty origin (баг-фикс):** `LexemeState(realId, translation = TextValueState(origin="", ...))` → `RemoveTranslation(realId)` → локальный `translation = null` без эффекта (БД ничего не знает об этом chip'е).
- **commitAndCloseAllEdits single-edit:** при открытии нового edit (`EnterTranslationEditMode`) все остальные активные edit-ы коммитятся / закрываются атомарно.
- **Локальная cascade:** `LexemeState(NOT_IN_DB, translation = TextValueState(origin="", ...), definition = null)` → `RemoveTranslation(NOT_IN_DB)` → лексема исчезает (обе sub-сущности `null`).
- **Серверная cascade с undo:** `RemoveTranslation(realId)` для лексемы с одним переводом → handler возвращает `LexemeCascadeRemoved` → `Msg.LexemeCascadeRemovedWithUndo(lexemeId, removedTranslation="x", null)` → лексема становится пустым NOT_IN_DB-черновиком + snackbar с undo.
- **Undo translation (cascade-case):** `UndoRemoveTranslation(lexemeId = NOT_IN_DB, value = "x")` → `DatasourceEffect.UpdateLexemeTranslation(wordId, lexemeId = null, "x")` → re-INSERT новой лексемы.
- **CommitWordChanges blank guard:** `(Loaded, isEditMode = true, edited = "   ")` → `state to emptySet()`.

### Handler (interaction)

- **LoadWord exception:** UseCase бросает → handler ловит → `consumer(Msg.WordNotFound)` (silent exit).
- **UpdateLexemeTranslation first-Commit failure:** UseCase возвращает null (нет dictionary в Prefs) → `consumer(Msg.ShowError(error_save_translation))`.
- **RemoveTranslation cascade:** UseCase возвращает `LexemeCascadeRemoved` → `consumer(Msg.LexemeCascadeRemovedWithUndo(lexemeId, removedTranslation, null))`.
- **RemoveLexeme success:** handler берёт snapshot (translation, definition) ДО delete → возвращает `Msg.LexemeRemoved(lexemeId, snap.t, snap.d)` для undo.

### UseCase (interface contract)

- **deleteLexeme idempotent (контракт data-слоя):** UseCase пробрасывает вызов в `lexemeApi.deleteLexeme(lexemeId)` под try/catch; идемпотентность для отсутствующего id обеспечивается реализацией data-слоя.
- **addLexemeTranslation(lexemeId = null) atomicity:** insert лексемы + insert translation + insert write-quiz в одной Room-транзакции через `lexemeApi.addLexemeWithTranslation`.
- **deleteLexemeTranslation для лексемы с одной sub-сущностью:** возвращает `LexemeCascadeRemoved`; в БД лексема удалена через `lexemeApi.deleteLexeme(id)`.
- **restoreLexeme(translation+definition) atomicity:** insertLexemeWithTranslation + updateLexemeDefinition; при провале второго шага — rollback (`deleteLexeme(id)`).
- **Trim:** все String-входы триммятся перед БД.
- **Маппер sort:** `TermApiEntity.toDomainEntity()` возвращает `lexemeList` в порядке DESC по `addDate`.
