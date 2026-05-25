<!-- META: spec_filename: wordcard.md -->

# WordCard — карточка слова

Экран детального просмотра и редактирования одного слова словаря: заголовок, лексемы (значения), их переводы и определения. Открывается из списка слов (DictionaryTab/VocabularyTab) по выбранному элементу.

## Бизнес-описание

WordCard — экран работы с одним словом словаря. Пользователь видит заголовок слова, дату добавления и плоский список лексем (значений), каждая лексема может иметь перевод и/или определение. На экране пользователь может:

- редактировать заголовок слова (inline);
- удалить слово целиком (с подтверждением);
- создать новую лексему через FAB и заполнить её перевод/определение inline-chip'ами;
- редактировать или удалять перевод/определение существующей лексемы;
- удалить лексему целиком через её контекстное меню.

Создание лексемы — двух-шаговое: тап FAB добавляет лексему **локально** в state без обращения к БД; запись в БД происходит только при первом подтверждённом вводе перевода или определения (handler атомарно делает insert лексемы + суб-сущности, возвращает реальный id, reducer заменяет локальный маркер `NOT_IN_DB` на реальный id). Если у лексемы удалена последняя суб-сущность — лексема каскадно удаляется (как локально для `NOT_IN_DB`, так и в БД через UseCase для реальных лексем).

Промежуточный UI «выбери чекбоксами что добавить» (старый bottom sheet) исключён в IS479 — заменён на inline-композицию: chip'ы «Перевод» / «Определение» внутри карточки конкретной лексемы.

## User Stories

- Как пользователь, я хочу видеть слово и все его значения на одном экране, чтобы удерживать полный контекст без переходов.
- Как пользователь, я хочу inline-редактировать заголовок слова с возможностью отмены, чтобы исправлять опечатки без модальных окон.
- Как пользователь, я хочу безопасно удалить слово через явное подтверждение, чтобы не потерять данные случайным жестом.
- Как пользователь, я хочу одним тапом FAB начать создавать новую лексему, чтобы не проходить через лишний промежуточный диалог.
- Как пользователь, я хочу chip'ами «Перевод» / «Определение» гибко выбирать что заполнить у лексемы, чтобы не быть вынужденным указывать обе сущности.
- Как пользователь, я хочу видеть когда сохранение в БД ещё не подтверждено, и не иметь возможности случайно отправить второй запрос — UI блокируется на время операции.
- Как пользователь, я хочу удалять перевод/определение точечно через меню лексемы; если удалена последняя суб-сущность — лексема исчезает автоматически (не остаётся «пустых» лексем).
- Как пользователь, я хочу получать понятное уведомление при ошибке сохранения, чтобы понимать что произошло, и иметь возможность повторить попытку.
- Как пользователь, я хочу что бы IME-фокус был один на экран: открытие меню одной лексемы или начало редактирования другого поля автоматически закрывают чужой активный edit.

## State

```kotlin
package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term
import java.util.Date

const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val snackbarState: SnackbarState = SnackbarState()
)

// computed: у этого слова есть незавершённое создание новой лексемы
val WordCardState.isCreatingLexeme: Boolean
    get() = lexemeList.any { it.id == NOT_IN_DB }

@Stable
data class TopBarState(
    val isMenuOpen: Boolean = false,
)

sealed interface WordState {
    data object NotLoaded : WordState

    @Stable
    data class Loaded(
        val id: Long,                 // != NOT_IN_DB по конструкции
        val added: Date,
        val value: String,            // non-empty по контракту Loaded
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
    val isMenuOpen: Boolean = false,
)

@Stable
data class TextValueState(
    val isEdit: Boolean = false,
    val origin: String = "",
    val edited: String = origin,
)

@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)
```

### Per-field

- **`topBarState`** — суб-стейт верхней панели (меню Edit/Delete word).
- **`isLoading`** — флаг первичной загрузки слова из БД. Простой паттерн (не sealed).
- **`isPendingDbOp`** — глобальный pending-флаг БД-операции. Выставляется в `true` reducer-веткой UI Msg, отправляющей `DatasourceEffect`, сбрасывается в `false` reducer-веткой соответствующего Datasource Msg (Refresh* / WordLoaded / WordNotFound / NavigateBack / ShowNotification). UI читает поле для блокировки кнопок (`enabled = !isPendingDbOp`). Source of truth — эффект-канал, который лежит вне State; флаг — единственный способ сделать pending наблюдаемым из UI/reducer.
- **`wordState`** — sealed sum `NotLoaded | Loaded`. Корреляция `id ↔ added ↔ value` закрыта компилятором без отдельных инвариантов.
- **`lexemeList`** — упорядоченный список лексем слова. Порядок важен для UI; дубли по id невозможны; стоимость линейного поиска амортизируется.
- **`snackbarState`** — транзиентный UI-сигнал (snackbar один на экран).

### `WordState`

- `NotLoaded` — начальное состояние до прихода `WordLoaded`. Полей `id/added/value` физически нет — невалидные комбинации невозможны.
- `Loaded(id, added, value, isEditMode, edited, showWarningDialog)` — слово получено из БД; `id != NOT_IN_DB` по конструкции; `value` непустой по контракту.

### `LexemeState`

- `id: Long` — реальный id из БД либо `NOT_IN_DB = -1L` для свежесозданной локальной лексемы.
- `translation: TextValueState?` / `definition: TextValueState?` — nullable как ось координат: `null` = суб-сущность не существует, не-null = существует (возможно с пустым origin между tap chip и первым Commit).
- `isMenuOpen: Boolean` — контекстное меню одной лексемы.

### `TextValueState`

Паттерн toggle edit/view с буфером:
- `isEdit: Boolean = false` — режим редактирования (дефолт `false` — безопасный, маппер из БД получает view-mode без переопределения; reducer обязан явно ставить `true` при создании через chip).
- `origin: String` — last known good значение, синхронизировано с БД.
- `edited: String = origin` — текущий ввод; при Commit пишется в БД и попадает в `origin` через Refresh; при Cancel/Commit обнуляется в `""` (инв. 4).

### Computed properties

- `WordCardState.isCreatingLexeme: Boolean = lexemeList.any { it.id == NOT_IN_DB }` — derived из списка, не хранится.

### Инварианты (snapshot, проверяются по одному состоянию)

1. `wordState is WordState.Loaded ∧ wordState.isEditMode == false ⇒ wordState.edited == ""`.
2. `| { l ∈ lexemeList : l.id == NOT_IN_DB } | ≤ 1` — допустимо максимум одна локальная (несохранённая) лексема.
3. `∀ l1, l2 ∈ lexemeList : l1 != l2 ⇒ l1.id != l2.id` — уникальность id (включая `NOT_IN_DB`, см. инв. 2).
4. Для каждой лексемы: `translation != null ∧ translation.isEdit == false ⇒ translation.edited == ""` (то же для `definition`).
5. `lexemeList.isNotEmpty() ⇒ wordState is WordState.Loaded` (структурный FK).
6. `(∃ l ∈ lexemeList : l.id == NOT_IN_DB) ⇒ wordState is WordState.Loaded` (≡ `isCreatingLexeme ⇒ Loaded`).
7. `isLoading == true ⇒ lexemeList.isEmpty()` (производный из инв. 5 + 11).
8. `snackbarState.show == true ⇒ snackbarState.title != ""`.
9. Глобальный single-edit-mode: `(if Loaded ∧ isEditMode then 1 else 0) + Σ (translation?.isEdit, definition?.isEdit) ≤ 1` — в любой момент активен максимум один TextField.
10. `wordState is WordState.Loaded ⇒ wordState.value != ""`.
11. `isLoading == true ⇒ wordState is WordState.NotLoaded`.
12. (Runtime, не snapshot) `isPendingDbOp == true ⇒ существует pending Datasource Effect, для которого reducer ещё не получил confirm-Msg`.

## UI Messages

```kotlin
package me.apomazkin.wordcard.mate

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
    data object ExitWordEditMode : Msg
    data object CommitWordChanges : Msg

    // --- Lexeme (создание/удаление лексемы целиком) ---
    data object CreateLexeme : Msg
    data class RemoveLexeme(val lexemeId: Long) : Msg
    data class OpenLexemeMenu(val lexemeId: Long, val isShow: Boolean) : Msg

    // --- Translation chip внутри лексемы ---
    data class CreateTranslation(val lexemeId: Long) : Msg
    data class UpdateTranslationInput(val lexemeId: Long, val value: String) : Msg
    data class EnterTranslationEditMode(val lexemeId: Long) : Msg
    data class CommitTranslationEdit(val lexemeId: Long) : Msg
    data class CancelTranslationEdit(val lexemeId: Long) : Msg
    data class RemoveTranslation(val lexemeId: Long) : Msg

    // --- Definition chip внутри лексемы ---
    data class CreateDefinition(val lexemeId: Long) : Msg
    data class UpdateDefinitionInput(val lexemeId: Long, val value: String) : Msg
    data class EnterDefinitionEditMode(val lexemeId: Long) : Msg
    data class CommitDefinitionEdit(val lexemeId: Long) : Msg
    data class CancelDefinitionEdit(val lexemeId: Long) : Msg
    data class RemoveDefinition(val lexemeId: Long) : Msg

    // --- Навигация (shared с handler-confirm после RemoveWord) ---
    data object NavigateBack : Msg

    // --- UI feedback ---
    data object DismissNotification : Msg

    // --- No-op ---
    data object NoOperation : Msg

    // --- Datasource Msg (от handler'ов, см. § IO) ---
    data class WordLoaded(val word: Term) : Msg
    data object WordNotFound : Msg
    data class RefreshWord(val word: Term) : Msg
    data class RefreshTranslation(val lexemeId: Long, val translation: String?) : Msg
    data class RefreshDefinition(val lexemeId: Long, val definition: String?) : Msg
    data class RefreshLexemeList(val lexemes: List<Lexeme>) : Msg
    data class LexemeCascadeRemoved(val lexemeId: Long) : Msg
    data class ShowNotification(val text: String) : Msg
}
```

### Категоризация

- **Действия пользователя:** `RemoveWord`, `CommitWordChanges`, `UpdateWordInput`, `EnterWordEditMode`, `ExitWordEditMode`, `CreateLexeme`, `RemoveLexeme`, парные `Create*` / `Update*Input` / `Enter*EditMode` / `Commit*Edit` / `Cancel*Edit` / `Remove*` для Translation и Definition.
- **Toggles:** `OpenTopBarMenu` / `CloseTopBarMenu`, `OpenDeleteWordDialog` / `CloseDeleteWordDialog`, `OpenLexemeMenu(lexemeId, isShow)`.
- **Навигация:** `NavigateBack` (shared — UI back-кнопка и handler-success для RemoveWord; одна reducer-ветка).
- **UI feedback:** `DismissNotification`.
- **No-op:** `NoOperation`.
- **Datasource (от handler'ов):** `WordLoaded`, `WordNotFound`, `RefreshWord`, `RefreshTranslation`, `RefreshDefinition`, `RefreshLexemeList`, `LexemeCascadeRemoved`, `ShowNotification`.

### Reducer — ключевые правила

- **Инициализация:** loading идёт через `initEffects = setOf(DatasourceEffect.LoadWord(wordId))` в ViewModel, **не** через UI Msg. `WordCardState()` стартует с `wordState = NotLoaded`, `isLoading = true`, `isPendingDbOp = false`.
- **Глобальный guard `isPendingDbOp`:** любой UI Msg, способный породить Effect, при `state.isPendingDbOp == true` → `state to emptySet()` (no-op). Исключения: `DismissNotification`, `Close*`, `Cancel*Edit`, `ExitWordEditMode`, `Update*Input`, `NavigateBack`, `NoOperation`.
- **Хелпер `closeAllEditModes()`:** перед открытием любого нового edit-mode (Enter*/Create*/CreateLexeme/OpenLexemeMenu(isShow=true)) reducer обязан атомарно закрыть все остальные edit-mode флаги (инв. 9). Хелпер сбрасывает `isEditMode/edited` в `Loaded` и `isEdit/edited` во всех `TextValueState` лексем. Все последующие мутации `lexemeList` берут список из результата хелпера (`closed.lexemeList`), не из исходного `state.lexemeList` (иначе сбросы затрутся).
- **`CreateLexeme` — полностью локальный:** append `LexemeState(id = NOT_IN_DB, translation = null, definition = null, isMenuOpen = false)` без отправки Effect. Guard: уже есть лексема с `NOT_IN_DB` → ignore.
- **`Commit*Edit` ветвление (4 ветви):**
  - **1a** `edited.isBlank() ∧ origin.isEmpty()` — локальный nullify `translation/definition = null` без Effect (свежесозданный chip без ввода).
  - **1** `edited.isBlank() ∧ origin.isNotEmpty()` — pessimistic Remove: сбрасываем `isEdit=false, edited=""` (не nullify), шлём `DatasourceEffect.RemoveTranslation/Definition`; финал — `Refresh*(null)` или `LexemeCascadeRemoved`.
  - **2** `edited == origin` — no-op коммит: сбрасываем `isEdit=false, edited=""`, без Effect.
  - **3** иначе — Update: сбрасываем `isEdit=false, edited=""`, шлём `UpdateLexemeTranslation/Definition(wordId, lexemeId = if (NOT_IN_DB) null else id, value)`. Реальный id придёт через `Refresh*`.
- **`Refresh*` ветвления (F073):**
  - Лексема с реальным `lexemeId` из payload **существует в `lexemeList`** — reducer сохраняет активный `isEdit/edited` пользователя, обновляет только `origin` (`l.translation?.copy(origin = ...)`). Refresh не закрывает активный edit.
  - Лексема с реальным `lexemeId` **отсутствует**, есть лексема с `NOT_IN_DB` — её `id` заменяется на реальный, конструируется `TextValueState(origin = value, isEdit = false, edited = "")`. Завершает «первый Commit».
  - `payload.translation == null` — стандартный nullify для существующей лексемы (Refresh от RemoveTranslation).
- **Локальная cascade для `NOT_IN_DB`:** `Remove*` для лексемы с `NOT_IN_DB` — локальный nullify; если после nullify `translation == null ∧ definition == null` — лексема удаляется из `lexemeList` целиком (симметрия с серверным `LexemeCascadeRemoved`).
- **Сброс `isPendingDbOp = false`** обязателен в reducer-ветке каждого Datasource Msg (`WordLoaded`, `WordNotFound`, `RefreshWord`, `RefreshTranslation`, `RefreshDefinition`, `RefreshLexemeList`, `LexemeCascadeRemoved`, `NavigateBack`, `ShowNotification`).

> Полные pseudocode по каждой ветке — см. `business/contract_ui_msg.md` v3.2 и `business/contract_io.md` v7.

### Сводная таблица guards (shortcut-ignore = `state to emptySet()`)

| Msg | Условие игнора |
|---|---|
| `CreateLexeme` | `wordState !is Loaded ∨ lexemeList.any { it.id == NOT_IN_DB }` |
| `RemoveWord` | `wordState !is Loaded ∨ wordState.id != msg.wordId` |
| `OpenDeleteWordDialog` | `wordState !is Loaded` |
| `CloseDeleteWordDialog` | `wordState !is Loaded ∨ !wordState.showWarningDialog` |
| `EnterWordEditMode` | `wordState !is Loaded` |
| `UpdateWordInput` | `wordState !is Loaded ∨ !wordState.isEditMode` |
| `ExitWordEditMode` | `wordState !is Loaded ∨ !wordState.isEditMode` |
| `CommitWordChanges` | `wordState !is Loaded ∨ !wordState.isEditMode ∨ wordState.edited.isBlank()` |
| `RemoveLexeme` | `wordState !is Loaded` + лексема не найдена |
| `OpenLexemeMenu` | лексема не найдена |
| `CreateTranslation` | `lexeme.translation != null` + лексема не найдена |
| `CreateDefinition` | `lexeme.definition != null` + лексема не найдена |
| `EnterTranslationEditMode` | `lexeme.translation == null` + лексема не найдена |
| `EnterDefinitionEditMode` | `lexeme.definition == null` + лексема не найдена |
| `UpdateTranslationInput` | `lexeme.translation == null ∨ !lexeme.translation.isEdit` + лексема не найдена |
| `UpdateDefinitionInput` | `lexeme.definition == null ∨ !lexeme.definition.isEdit` + лексема не найдена |
| `CommitTranslationEdit` | `wordState !is Loaded ∨ lexeme.translation == null ∨ !lexeme.translation.isEdit` + лексема не найдена |
| `CommitDefinitionEdit` | `wordState !is Loaded ∨ lexeme.definition == null ∨ !lexeme.definition.isEdit` + лексема не найдена |
| `CancelTranslationEdit` | `lexeme.translation == null ∨ !lexeme.translation.isEdit` + лексема не найдена |
| `CancelDefinitionEdit` | `lexeme.definition == null ∨ !lexeme.definition.isEdit` + лексема не найдена |
| `RemoveTranslation` | `lexeme.translation == null` + лексема не найдена |
| `RemoveDefinition` | `lexeme.definition == null` + лексема не найдена |
| `CloseTopBarMenu` | `!topBarState.isMenuOpen` |
| `DismissNotification` | `!snackbarState.show` |

Глобальный guard `state.isPendingDbOp == true ⇒ state to emptySet()` применяется ко всем UI Msg выше за исключением `Close*` / `Dismiss*` / `Cancel*Edit` / `ExitWordEditMode` / `Update*Input` / `NavigateBack` / `NoOperation`.

## IO

### Effects

```kotlin
package me.apomazkin.wordcard.logic

import me.apomazkin.mate.Effect

sealed interface DatasourceEffect : Effect {
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    data class UpdateLexemeTranslation(
        val wordId: Long,
        val lexemeId: Long?,   // null ⇒ handler делает insert лексемы + insert translation в одной Room-транзакции
        val translation: String,
    ) : DatasourceEffect
    data class RemoveTranslation(val lexemeId: Long) : DatasourceEffect

    data class UpdateLexemeDefinition(
        val wordId: Long,
        val lexemeId: Long?,   // null ⇒ insert лексемы + insert definition в одной Room-транзакции
        val definition: String,
    ) : DatasourceEffect
    data class RemoveDefinition(val lexemeId: Long) : DatasourceEffect
}
```

`NavigationEffect.Back` — используется существующий mate-овский тип (через `Msg.NavigateBack` reducer-ветку, общую для UI-intent и handler-confirm после `RemoveWord` success / `WordNotFound`).

**UI Effects отсутствуют.** Snackbar реализуется через `snackbarState` в State — это позволяет retain'ить snackbar через configuration change.

#### Effect → Action → Msg

| Effect | UseCase method | Success Msg | Failure Msg |
|---|---|---|---|
| `LoadWord(wordId)` | `getTermById(wordId): Term?` | `WordLoaded(term)` / `WordNotFound` (если `null`) | `WordNotFound` (silent exit на exception) |
| `RemoveWord(wordId)` | `deleteWord(wordId): Int` | `NavigateBack` | `ShowNotification("Не удалось удалить слово")` |
| `UpdateWord(wordId, value)` | `updateWord(wordId, value): Boolean` + `getTermById(wordId)` (resync) | `RefreshWord(term)` | `ShowNotification("Не удалось сохранить")` (или `"Не удалось получить обновлённое слово"` при `null` после `true`) |
| `RemoveLexeme(wordId, lexemeId)` | `deleteLexeme(wordId, lexemeId): List<Lexeme>?` | `RefreshLexemeList(lexemes)` | `ShowNotification("Не удалось удалить значение")` |
| `UpdateLexemeTranslation(wordId, lexemeId?, translation)` | `addLexemeTranslation(wordId, lexemeId?, translation): Lexeme?` | `RefreshTranslation(lexeme.lexemeId.id, lexeme.translation?.value)` | `ShowNotification("Не удалось сохранить перевод")` |
| `RemoveTranslation(lexemeId)` | `deleteLexemeTranslation(lexemeId): RemoveTranslationResult?` | `RefreshTranslation(lexemeId, lexeme.translation?.value)` / `LexemeCascadeRemoved(lexemeId)` | `ShowNotification("Не удалось удалить перевод")` |
| `UpdateLexemeDefinition(wordId, lexemeId?, definition)` | `addLexemeDefinition(wordId, lexemeId?, definition): Lexeme?` | `RefreshDefinition(lexeme.lexemeId.id, lexeme.definition?.value)` | `ShowNotification("Не удалось сохранить определение")` |
| `RemoveDefinition(lexemeId)` | `deleteLexemeDefinition(lexemeId): RemoveDefinitionResult?` | `RefreshDefinition(lexemeId, lexeme.definition?.value)` / `LexemeCascadeRemoved(lexemeId)` | `ShowNotification("Не удалось удалить определение")` |

**UX-инвариант:** каждый control-path каждого handler'а (success / failure / catch) обязан вернуть ровно один разблокирующий Msg — иначе UI зависнет с `isPendingDbOp = true`. Разблокирующий Msg = любой Datasource Msg из таблицы выше (плюс shared `NavigateBack`).

**Atomicity contract** (`addLexemeTranslation(wordId, lexemeId = null, ...)` и `addLexemeDefinition(wordId, lexemeId = null, ...)`): impl обязан выполнять `addLexeme(wordId)` + `updateLexeme*(newId, value)` в одной Room-транзакции (`@Transaction` DAO или `withTransaction { ... }`). Без транзакции failure после success `addLexeme` оставит пустую лексему в БД — нарушение инварианта «БД-лексема имеет ≥ 1 суб-сущность».

**Mapper:**

```kotlin
internal fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = lexemeId.id,
    translation = translation?.let { TextValueState(origin = it.value, isEdit = false, edited = "") },
    definition  = definition?.let { TextValueState(origin = it.value, isEdit = false, edited = "") },
    isMenuOpen  = false,
)
```

### Subscribers

**Отсутствуют.** WordCard не имеет cross-screen реактивности: `WordCardUseCase` экспонирует только `suspend`-методы, без `Flow`/`observe`. Все обновления state — реакция на handler-Msg от user-action. Если когда-нибудь появится UI, изменяющий слово вне WordCard, — subscriber вводится отдельной задачей (backlog-кандидат).

## UseCase

```kotlin
package me.apomazkin.wordcard.deps

import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {

    /** Cold load слова + лексем (Term.lexemeList уже содержит лексемы). */
    suspend fun getTermById(wordId: Long): Term?                       // Effect LoadWord

    /** Удаляет слово; БД каскадно (FK ON DELETE CASCADE) удаляет лексемы и суб-сущности. */
    suspend fun deleteWord(wordId: Long): Int                          // Effect RemoveWord

    /** Обновляет value слова. */
    suspend fun updateWord(wordId: Long, value: String): Boolean       // Effect UpdateWord

    /** Удаляет лексему; возвращает актуальный список лексем слова. Idempotent: для отсутствующей возвращает текущий список, не null. */
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?   // Effect RemoveLexeme

    /**
     * Если lexemeId == null — insert лексемы + insert translation в одной Room-транзакции (atomicity contract).
     * Если lexemeId != null — upsert translation существующей лексемы.
     * Возвращает лексему с реальным id.
     */
    suspend fun addLexemeTranslation(
        wordId: Long,
        lexemeId: Long?,
        translation: String,
    ): Lexeme?                                                          // Effect UpdateLexemeTranslation

    /** Удаляет translation. Sealed result различает non-cascade и cascade. */
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?   // Effect RemoveTranslation

    /** Симметрично addLexemeTranslation (atomicity contract для null-кейса). */
    suspend fun addLexemeDefinition(
        wordId: Long,
        lexemeId: Long?,
        definition: String,
    ): Lexeme?                                                          // Effect UpdateLexemeDefinition

    /** Симметрично deleteLexemeTranslation. */
    suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?   // Effect RemoveDefinition
}

sealed interface RemoveTranslationResult {
    /** Translation удалён в БД; лексема осталась (definition non-null). Содержит обновлённую лексему. */
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    /** Translation был последней суб-сущностью; лексема каскадно удалена data-слоем. */
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveDefinitionResult {
    /** Definition удалён в БД; лексема осталась (translation non-null). Содержит обновлённую лексему. */
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    /** Definition был последней суб-сущностью; лексема каскадно удалена data-слоем. */
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}
```

### Покрытие 1-к-1

8 методов interface ↔ 8 `DatasourceEffect`-вариантов (см. таблицу Effect → Action → Msg выше).

### Семантика возврата (общие правила)

- `null` — реальная БД-ошибка / failure. Handler конвертирует в `ShowNotification`.
- Non-null — success. Для idempotent-кейсов (повторный delete на уже отсутствующем) возвращается «как есть» (актуальный список / `TranslationRemoved(lexeme с null translation)`), **не** `null`.
- Sealed result (`RemoveTranslationResult` / `RemoveDefinitionResult`) — handler должен явно различить «суб-сущность удалена, лексема осталась» vs «лексема каскадно ушла». `Lexeme?` со значением `null` неоднозначен (failure vs cascade), sealed различает три исхода.

### БД-инвариант (data-слой)

«В БД лексема имеет ≥ 1 суб-сущность (translation либо definition non-null).» При удалении последней суб-сущности через `delete*` data-слой каскадно удаляет лексему (`lexemeApi.deleteLexeme(id)`) — формально не БД-FK, а внутренний механизм impl. UI-сторона видит этот path через `LexemeCascadeRemoved` result.

Для state локальная (`NOT_IN_DB`) пустая лексема (`translation = null ∧ definition = null`) валидна **временно** между UI Msg `CreateLexeme` (тап FAB) и первым `Commit Translation/Definition`. Это разрыв между жизненными моделями БД и state — БД-инвариант неприменим к локальным лексемам.

## Тестовые сценарии

### Reducer (snapshot)

- **WordLoaded атомарность:** из `(NotLoaded, isLoading=true)` после `WordLoaded(term)` → `wordState = Loaded(...)`, `isLoading = false`, `isPendingDbOp = false`, `lexemeList = term.lexemeList.map { toLexemeState() }` — одна reduce-операция.
- **WordNotFound silent exit:** `(NotLoaded, isLoading=true)` → `WordNotFound` → `isLoading = false`, `isPendingDbOp = false`, эффект `NavigationEffect.Back`.
- **CreateLexeme локально:** `(Loaded, lexemeList = [...real...])` → `CreateLexeme` → `lexemeList` дополнен `LexemeState(id = NOT_IN_DB, ...)`, **никаких Effect**, `isPendingDbOp` не меняется.
- **CreateLexeme guard:** `(Loaded, lexemeList = [..., NOT_IN_DB-lexeme])` → повторный `CreateLexeme` → `state to emptySet()`.
- **Глобальный guard isPendingDbOp:** `(any, isPendingDbOp=true)` → `RemoveWord(wordId)` → `state to emptySet()`.
- **CommitTranslationEdit ветвь 1a:** `LexemeState(NOT_IN_DB, translation = TextValueState(origin="", edited="  ", isEdit=true))` → `CommitTranslationEdit` → `translation = null`, без Effect.
- **CommitTranslationEdit ветвь 1 pessimistic Remove:** `LexemeState(realId, translation = TextValueState(origin="x", edited="", isEdit=true))` → reducer сбрасывает `isEdit=false, edited=""` (но **не** nullify), шлёт `DatasourceEffect.RemoveTranslation(realId)`, `isPendingDbOp = true`. Финальный `RefreshTranslation(realId, null)` обнулит `translation`.
- **CommitTranslationEdit ветвь 3 first-Commit:** `LexemeState(NOT_IN_DB, translation = TextValueState(origin="", edited="hello", isEdit=true))` + `wordState = Loaded(...)` → Effect `UpdateLexemeTranslation(wordId, lexemeId = null, "hello")`, `isPendingDbOp = true`. После `RefreshTranslation(newId, "hello")` reducer заменяет `NOT_IN_DB → newId`, конструирует `TextValueState(origin = "hello", isEdit = false, edited = "")`, `isPendingDbOp = false`.
- **closeAllEditModes single-edit:** `(Loaded, isEditMode = true)` + лексема с `definition.isEdit = false` → `EnterTranslationEditMode(lexemeId)` → `wordState.isEditMode = false`, `wordState.edited = ""`, `translation.isEdit = true, edited = origin`; инв. 9 удерживается.
- **Локальная cascade:** `LexemeState(NOT_IN_DB, translation = TextValueState(origin="", ...), definition = null)` → `RemoveTranslation(NOT_IN_DB)` → лексема исчезает из `lexemeList` (обе суб-сущности `null` после nullify).
- **Серверная cascade:** `RemoveTranslation(realId)` для лексемы с одним переводом → handler возвращает `RemoveTranslationResult.LexemeCascadeRemoved` → `Msg.LexemeCascadeRemoved(realId)` → лексема исчезает из `lexemeList`, `isPendingDbOp = false`.
- **DismissNotification idempotent:** `(snackbarState.show = false)` → `DismissNotification` → `state to emptySet()`.
- **CommitWordChanges blank guard (инв. 10):** `(Loaded, isEditMode = true, edited = "   ")` → `CommitWordChanges` → `state to emptySet()`.

### Handler (interaction)

- **LoadWord exception:** UseCase бросает → handler ловит → `consumer(Msg.WordNotFound)` (silent exit).
- **UpdateLexemeTranslation first-Commit failure:** UseCase бросает (нет dictionary в Prefs) → `consumer(Msg.ShowNotification("Не удалось сохранить перевод"))` → reducer: `NOT_IN_DB`-лексема остаётся в `lexemeList` с пустым `edited`, `isPendingDbOp = false`, snackbar показан.
- **RemoveTranslation cascade:** UseCase возвращает `LexemeCascadeRemoved` → `consumer(Msg.LexemeCascadeRemoved(lexemeId))`, не `RefreshTranslation`.

### UseCase (interface contract)

- **deleteLexeme idempotent:** повторный вызов для уже отсутствующего `lexemeId` возвращает текущий список без него, не `null`.
- **addLexemeTranslation(lexemeId = null) atomicity:** impl делает insert лексемы + insert translation в одной транзакции; при exception между шагами — rollback (БД остаётся без лексемы).
- **deleteLexemeTranslation для лексемы с одной суб-сущностью:** возвращает `LexemeCascadeRemoved`; в БД лексема удалена через `lexemeApi.deleteLexeme(id)`.

## Лог итераций

### ит.1 (2026-05-19T22:00:00-0600)

Финальная компиляция спеки IS479. Источники:
- `business/contract_state.md` v2.5 — State, computed `isCreatingLexeme`, инварианты 1-12.
- `business/contract_ui_msg.md` v3.2 — UI Msg + Datasource Msg + reducer-правила (`closeAllEditModes`, глобальный guard `isPendingDbOp`, ветвление `Commit*Edit`, replacement `NOT_IN_DB → реальный id`, локальная cascade).
- `business/contract_io.md` v7 — `DatasourceEffect`, handler pseudocode, UX-инвариант (гарантированный разблокирующий Msg на каждом control-path), atomicity contract, отсутствие subscribers.
- `business/contract_usecase.md` v1 — финальный `WordCardUseCase` (8 методов, sealed result для `delete*Translation/Definition`, удалённый `addLexeme`).

Спеки `wordcard.md` в `docs/features-spec/` ранее не существовало — создаётся новая (см. `docs/features-spec/README.md` § «Известные пробелы»: «Карточка слова (WordCard)» — без ссылки).

Включены тестовые сценарии: reducer snapshot (ключевые ветви: CreateLexeme локально, ветви `CommitTranslationEdit` 1a/1/3, replacement `NOT_IN_DB`, локальная/серверная cascade, single-edit-mode через `closeAllEditModes`, глобальный guard `isPendingDbOp`), handler interaction (LoadWord exception, first-Commit failure, cascade result), UseCase interface contract (idempotent deleteLexeme, atomicity, cascade).

Файл публикуется в `docs/features-spec/wordcard.md` на шаге `publish_spec` (после `implement`).

---

_model: claude opus 4.7 (1M context)_
