# contract_ui_msg — IS479 wordcard_lexeme_inline (v2)

> ⚠ **Этот артефакт — черновик.** Финал Msg-списка и связи Msg ↔ Effect живёт в `contract_io.output`.
>
> Forward-references на effects — гипотезы. Ревью на этом шаге: UI-триггеры, state changes, категоризация Msg.
>
> Не фиксировать Msg ↔ Effect связь как окончательную.

## Режим работы

**Режим 1 — макет-driven** (унаследовано из `contract_state.md` v2). Источники: Figma `w8GmGCdOZJUi99Cuv4q4W9` (frames `9154-82519`, `9154-82532`, `9154-82521`, `9154-82625`) + текущий `Message.kt` / `WordCardReducer.kt`. Спека отсутствует — расхождения spec↔code не сверяются.

## Scope артефакта

`sealed interface Msg` ниже описывает **UI-стороны**: действия пользователя, навигация, UI-feedback inbound от самого UI (пользовательский dismiss snackbar). Datasource Msg (результаты от effect-handler'ов — `WordLoaded`, `WordNotFound`, `RefreshLexeme`, `CreateLexemeFailed`, `RefreshTranslation`, `RefreshDefinition`, `ShowNotification`) — каноном идут в `contract_io`, здесь упомянуты только forward-ref таблицей для reducer-логики, на которую они влияют.

> 📎 guide: docs/guides/messages.md — "Категории сообщений: действия пользователя, навигация, результаты данных, UI обратная связь, переключатели, no-op"
>
> 📎 guide: docs/guides/effect-handlers.md — "Маппинг Effect → Message: deструктивные операции часто запускают перезагрузку"

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

    // --- Навигация ---
    data object NavigateBack : Msg

    // --- UI feedback (inbound от UI) ---
    data object DismissNotification : Msg

    // --- No-op ---
    data object NoOperation : Msg
}
```

> 📎 guide: docs/guides/messages.md — "Msg = sum type sealed interface; data object для no-payload, data class для payload"
>
> 📎 guide: docs/guides/messages.md — "Не плодить Msg для состояний экрана — Msg = действие/факт, состояние выводится в reducer"
>
> 📎 guide: docs/guides/messages.md — "Exhaustiveness: sealed interface гарантирует исчерпывающий when в редьюсере"
>
> 📎 guide: docs/guides/code-style.md — "Sealed interface Msg (всегда Msg); пакет logic для State/Message/Reducer"
>
> 📎 guide: docs/guides/state-modeling.md — "sealed class — лучшее type-safety, smart cast; зависимые/взаимоисключающие состояния → sum type"

### Категоризация (UI-сторона)

**Действия пользователя:**
- `RemoveWord`, `CommitWordChanges`, `UpdateWordInput`, `EnterWordEditMode`, `ExitWordEditMode`
- `CreateLexeme`, `RemoveLexeme`
- `CreateTranslation`, `UpdateTranslationInput`, `EnterTranslationEditMode`, `CommitTranslationEdit`, `CancelTranslationEdit`, `RemoveTranslation`
- `CreateDefinition`, `UpdateDefinitionInput`, `EnterDefinitionEditMode`, `CommitDefinitionEdit`, `CancelDefinitionEdit`, `RemoveDefinition`

> 📎 guide: docs/guides/messages.md — "Действия пользователя: императивный глагол, описывающий намерение (Add*/Delete*, Save*, *TextChange)"

**Навигация (intent от UI):**
- `NavigateBack`

> 📎 guide: docs/guides/messages.md — "Навигационные сообщения идут через Msg, не через прямые callback; reducer возвращает NavigationEffect"
>
> 📎 guide: docs/guides/navigation.md — "BackHandler в composable отправляет один Msg.RequestBack — без логики"

**UI feedback (от UI к reducer'у):**
- `DismissNotification` — тап/swipe пользователя по snackbar / автоматический таймаут.

**Переключатели (toggles):**
- `OpenTopBarMenu` / `CloseTopBarMenu`
- `OpenDeleteWordDialog` / `CloseDeleteWordDialog`
- `OpenLexemeMenu` (с булевым `isShow` — payload-форма triggers (a))

> 📎 guide: docs/guides/messages.md — "Toggle-пары: явные on/off сообщения предпочтительны, булев параметр также допустим"

**No-op:**
- `NoOperation`

> 📎 guide: docs/guides/messages.md — "В каждой фиче есть Msg.Empty как no-op; в редьюсере: `is Msg.Empty -> state to emptySet()`"
>
> 📎 guide: docs/guides/reducer-patterns.md — "Всегда обрабатывать Msg.Empty: `is Msg.Empty -> state to emptySet()`"

### Канон в contract_io (Datasource → UI, forward-ref)

Эти Msg формируют reducer-поведение, но их декларация и точные сигнатуры фиксируются в `contract_io`:

| Datasource Msg | Семантика | Где упоминается в reducer-логике ниже |
|---|---|---|
| `WordLoaded(word)` | результат `DatasourceEffect.LoadWord` (success) | завершает initial-load flow (effect улетел из `initEffects` ViewModel) |
| `WordNotFound` | результат `DatasourceEffect.LoadWord` (404) | завершает initial-load flow |
| `RefreshLexeme(lexeme)` | результат `DatasourceEffect.CreateLexeme` (success) | завершает `CreateLexeme` flow (append + сброс `isCreatingLexeme`) |
| `CreateLexemeFailed` | результат `DatasourceEffect.CreateLexeme` (failure) | сбрасывает `isCreatingLexeme` + триггерит `ShowNotification` |
| `RefreshTranslation(lexemeId, translation?)` | возврат `UpdateLexemeTranslation` / `RemoveTranslation` | синхронизирует `translation.origin` (или ставит `translation = null` если БД вернула отсутствие) |
| `RefreshDefinition(lexemeId, definition?)` | возврат `UpdateLexemeDefinition` / `RemoveDefinition` | синхронизирует `definition.origin` (или ставит `definition = null`) |
| `ShowNotification(text)` | effect-handler выставил snackbar после ошибки/успеха | пишет `snackbarState = SnackbarState(title = text, show = true)` |

> 📎 guide: docs/guides/messages.md — "Результаты эффектов: прошедшее время или существительное (*Loaded, *Update, *Skipped)"
>
> 📎 guide: docs/guides/effect-handlers.md — "Маппинг Effect → Message: handler конвертирует результат в Msg, consumer вызывается только при полезном msg"

Точные сигнатуры (`text` vs `textRes`, `lexemeId` vs `lexeme`) — закреплены в `contract_io`.

## Reducer-логика per Msg

### Инициализация (initEffects, не UI Msg)

Загрузка слова идёт через `initEffects` ViewModel — **не через UI Msg**. См. `WordCardViewModel.kt:28`:

```kotlin
private val stateHolder = Mate(
    initState = WordCardState(),
    initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
    ...
)
```

> 📎 guide: docs/guides/mate-framework.md — "initEffects запускают первую загрузку данных при создании экрана; антипаттерн LaunchedEffect → Msg.LoadingWord"
>
> 📎 guide: docs/guides/viewmodel-wiring.md — "ViewModel через @AssistedInject; runtime-аргументы (wordId, navigator) через @Assisted; initEffects = setOf(DatasourceEffect.LoadWord(wordId))"

`wordId` передаётся в ViewModel через `@AssistedInject` навигационный аргумент. `DatasourceEffect.LoadWord(wordId)` улетает ровно один раз при создании Mate; обработчик возвращает `WordLoaded(...)` либо `WordNotFound`. Никакой `Msg.LoadingWord` от UI **не существует** — это анти-паттерн (см. `docs/guides/mate-framework.md` Конвенция 5 — антипаттерн `LaunchedEffect` для init).

> 📎 guide: docs/guides/mate-framework.md — "Антипаттерн LaunchedEffect → Msg.LoadingWord: триггер side-effect в UI нарушает single-direction; recompose-гонки; мёртвый Msg-вариант"

Recompose-гонок нет: `initEffects` отрабатывают ровно один раз в `init { ... }` блоке Mate. ViewModel переживает recompose composable'ов; Mate переживает в ViewModel.

### Top bar menu

#### `OpenTopBarMenu`
- **Что:** пользователь раскрывает меню в шапке.
- **Trigger:** тап на иконке меню в TopBar.
- **State changes:** `topBarState.isMenuOpen = true`.
- **Effects:** нет.
- **Guard:** нет (toggle, безусловно идемпотентен).

> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 1: Только стейт (без эффектов) — цепочка расширений + пустой набор эффектов"

#### `CloseTopBarMenu`
- **Что:** пользователь закрывает меню.
- **Trigger:** dismiss (тап вне меню).
- **State changes:** `topBarState.isMenuOpen = false`.
- **Effects:** нет.

### Delete word

#### `OpenDeleteWordDialog`
- **Что:** пользователь инициирует удаление слова.
- **Trigger:** тап на пункте «Delete» в TopBar menu.
- **State changes:** `wordState.showWarningDialog = true`, `topBarState.isMenuOpen = false` (атомарно закрываем меню при переходе в диалог — между состояниями экран ещё виден, без сброса меню перекроет диалог).
- **Effects:** нет.
- **Guard:** `if (state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...` — нельзя открыть удаление того, чего ещё нет в БД (инвариант 1).

> 📎 guide: docs/guides/state-and-extensions.md — "NOT_IN_DB = -1L для неинициализированных ID"

#### `CloseDeleteWordDialog`
- **Что:** пользователь отменяет удаление.
- **Trigger:** dismiss диалога / тап «Отмена».
- **State changes:** `wordState.showWarningDialog = false`.
- **Effects:** нет.

#### `RemoveWord(wordId)`
- **Что:** пользователь подтверждает удаление слова.
- **Trigger:** тап «Удалить» в confirmation-диалоге.
- **State changes:** `topBarState.isMenuOpen = false`, `wordState.showWarningDialog = false`. Закрываем меню и диалог сразу — между этим Msg и `NavigationEffect.Back` экран ещё виден; без сброса возможен визуальный артефакт.
- **Effects (forward-ref):** `DatasourceEffect.RemoveWord(wordId)`; затем effect-handler эмитит `NavigationEffect.Back` после успешного удаления (каноном в `contract_io`).
- **Guard:** `if (state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...`.

> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 3: Стейт + эффекты — цепочка расширений вместе с эффектами"
>
> 📎 guide: docs/guides/navigation.md — "Conditional навигация в reducer, а не в composable"

### Word edit

#### `EnterWordEditMode`
- **Что:** пользователь входит в режим редактирования заголовка слова.
- **Trigger:** тап на заголовке / иконке pencil.
- **State changes:** `wordState.isEditMode = true`, `wordState.edited = wordState.value` (инициализируем буфер текущим значением).
- **Effects:** нет.
- **Guard:** `if (state.wordState.isEditMode || state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...` — повторный Enter в уже открытом режиме иначе перезатёр бы `edited` пользователя; вход в edit на ненайденном слове бессмыслен.

> 📎 guide: docs/guides/state-and-extensions.md — "TextValueState — паттерн для toggle edit/view: origin (сохранённое) + edited (в процессе) + isEdit (режим)"
>
> 📎 guide: docs/guides/state-modeling.md — "Редактируемые поля отдельно от domain model: оригинал + редактируемая копия"

#### `UpdateWordInput(value)`
- **Что:** пользователь вводит текст заголовка.
- **Trigger:** `onValueChange` в `TextField` заголовка.
- **State changes:** `wordState.edited = value`.
- **Effects:** нет.
- **Guard:** `if (!state.wordState.isEditMode) state to emptySet() else ...`. Без guard'а Update вне edit-mode записал бы `edited != ""` при `isEditMode == false`, что породило бы вне-edit «грязный» буфер.
- **Почему payload — одно поле:** простой editable (правило «Msg на изменение значения содержит только новое значение поля»).

> 📎 guide: docs/guides/messages.md — "data class (с данными) для действий с параметрами от UI или эффектов: ChangeWordValue(val value: String)"

#### `ExitWordEditMode`
- **Что:** пользователь отменяет редактирование заголовка без коммита.
- **Trigger:** тап «Отмена» / back-кнопка внутри edit-mode.
- **State changes:** `wordState.isEditMode = false`, `wordState.edited = ""` (вытираем буфер — отменённая работа не должна оставлять следов).
- **Effects:** нет.
- **Guard:** `if (!state.wordState.isEditMode) state to emptySet() else ...`.
- **Naming (асимметрия с translation/definition):** для word оставляем существующее `ExitWordEditMode` (cancel) + `CommitWordChanges` (commit). Пара глаголов уникальна внутри word (нет коллизии адресации), переименование `Exit→Cancel` дало бы только семантическую симметрию ценой массовой правки call-site. Для translation/definition пара `Commit*Edit` / `Cancel*Edit` симметрична намеренно — там сосуществуют две сущности (translation и definition), однообразие имён внутри них помогает.

#### `CommitWordChanges`
- **Что:** пользователь подтверждает изменения заголовка.
- **Trigger:** тап «Сохранить» / IME action Done.
- **State changes:** `wordState.value = wordState.edited`, `wordState.isEditMode = false`, `wordState.edited = ""`. Сброс `edited` обязателен в том же reducer-шаге, чтобы не оставлять «грязный» буфер вне edit-mode.
- **Effects (forward-ref):** `DatasourceEffect.UpdateWord(wordId, value = wordState.edited перед сбросом)`. Эффект конструируется со старым значением `edited` (snapshot до мутации), в state поле уже обнулено.
- **Guard:** `if (!state.wordState.isEditMode || state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...`.
- **No-op `edited == value` (необязательная UI-side оптимизация):** UI-слой может не отправлять `CommitWordChanges`, если `edited == value` (effect лишний). Если всё же отправил — reducer выполнит ту же мутацию (`value = edited`, `isEditMode = false`, `edited = ""`), а effect `UpdateWord` с прежним значением идемпотентен на стороне БД. Дополнительная ветвь в reducer'е не вводится — цена no-op write мала.

### Lexeme (создание/удаление целиком)

#### `CreateLexeme`
- **Что:** пользователь создаёт новую пустую лексему через FAB.
- **Trigger:** тап на FAB `9154-82532` («Добавить значение»).
- **State changes:** `isCreatingLexeme = true`.
- **Effects (forward-ref):** `DatasourceEffect.CreateLexeme(wordId = state.wordState.id)`. Возврат — `RefreshLexeme(lexeme)` / `CreateLexemeFailed` (каноном в `contract_io`).
- **Guard:** `if (state.isCreatingLexeme || state.isLoading || state.wordState.id == NOT_IN_DB) state to emptySet() else ...` — защита от двойного нажатия, от тапа до загрузки слова (инвариант 1).

> 📎 guide: docs/guides/effect-handlers.md — "DatasourceEffect: операции с БД, сетью, preferences; всегда на Dispatchers.IO"

#### `RemoveLexeme(lexemeId)`
- **Что:** пользователь удаляет существующую лексему.
- **Trigger:** тап «Удалить» в контекстном меню лексемы.
- **State changes:** для лексемы с `id == lexemeId` — `isMenuOpen = false` (закрываем меню атомарно, поскольку Msg триггерится из меню; иначе меню зависло бы до прихода `Refresh`-like сообщения от data-слоя). Сама лексема остаётся в `lexemeList` до фактического возврата из БД — удаление приходит каноном из `contract_io`.
- **Effects (forward-ref):** `DatasourceEffect.RemoveLexeme(lexemeId)`.
- **Guard (общий):** «лексема не найдена по `lexemeId`» ⇒ `state to emptySet()` (см. общую сноску в конце).

> 📎 guide: docs/guides/state-and-extensions.md — "Для модификации элементов в списках используется modifyFiltered из modules/core/tools"
>
> 📎 guide: docs/guides/state-modeling.md — "State как база данных: с ростом state — структурировать его как БД, индекс/foreign key между коллекциями"

#### `OpenLexemeMenu(lexemeId, isShow)`
- **Что:** пользователь открывает/закрывает контекстное меню конкретной лексемы.
- **Trigger:** тап на иконке «...» в карточке лексемы (open) или dismiss popup'а (close).
- **State changes (эксклюзивность):**
  - Если `isShow == true` ⇒ для лексемы с `id == lexemeId` `isMenuOpen = true`; **для всех остальных лексем `isMenuOpen = false`**. DropdownMenu в Compose — модальный popup; семантически одновременно открытое меню более чем на одной лексеме невозможно. Эксклюзивный маппинг страхует от рассогласованного UI при быстрых тапах.
  - Если `isShow == false` ⇒ для лексемы с `id == lexemeId` `isMenuOpen = false`; остальные не трогаем (они и так должны быть `false` по эксклюзивности выше).
- **Effects:** нет.
- **Почему payload не одно поле** (whitelist (a) — payload = id адресации + дискриминатор операции).
- **Локальный инвариант, удерживаемый reducer'ом:** `forall l1, l2 ∈ lexemeList, l1 != l2: !(l1.isMenuOpen && l2.isMenuOpen)`. Это не структурный инвариант (его место не в `contract_state`), а свойство reducer-маппинга — фиксирую здесь, потому что порождён он именно эксклюзивностью `OpenLexemeMenu`.

> 📎 guide: docs/guides/state-modeling.md — "Ловушка ложного 'или': sum или product — спросить, это новое состояние или новая ось координат"

### Translation chip (внутри лексемы)

#### `CreateTranslation(lexemeId)`
- **Что:** пользователь нажимает chip «Перевод» внутри карточки лексемы, у которой ещё нет перевода.
- **Trigger:** тап на chip «Перевод» (frame `9154-82625`) в `LexemeState.translation == null`.
- **State changes:** для лексемы с `id == lexemeId` — `translation = TextValueState(origin = "", edited = "", isEdit = true)`. Сразу в edit-mode — пользователь должен ввести значение.
- **Effects:** нет (создание перевода в БД произойдёт по `CommitTranslationEdit` с непустым `edited`).
- **Guard:** `if (lexeme.translation != null) state to emptySet() else ...` — у лексемы уже есть перевод, повторный «Create» бессмыслен.

> 📎 guide: docs/guides/state-modeling.md — "T? = count(T) + 1: nullable семантика — может быть null на момент создания state или в течение flow"

#### `UpdateTranslationInput(lexemeId, value)`
- **Что:** пользователь вводит текст перевода.
- **Trigger:** `onValueChange` в inline-`TextField` перевода.
- **State changes:** для лексемы с `id == lexemeId` — `translation.edited = value`.
- **Effects:** нет.
- **Guard:** `if (lexeme.translation == null || !lexeme.translation.isEdit) state to emptySet() else ...` — Update без открытого edit-mode не имеет источника UI.

#### `EnterTranslationEditMode(lexemeId)`
- **Что:** пользователь повторно входит в режим редактирования существующего перевода.
- **Trigger:** тап по значению перевода в view-mode (chip + текст уже есть).
- **State changes:** для лексемы с `id == lexemeId` — `translation.isEdit = true`, `translation.edited = translation.origin` (инициализируем буфер last known good значением).
- **Effects:** нет.
- **Guard:** `if (lexeme.translation == null || lexeme.translation.isEdit) state to emptySet() else ...` — идемпотентность входа (повторный Enter иначе перезатёр бы `edited` пользователя).

#### `CommitTranslationEdit(lexemeId)`
- **Что:** пользователь коммитит изменения перевода.
- **Trigger:** тап вне поля / IME Done / тап на chip-toggle подтверждения.
- **State changes:** для лексемы с `id == lexemeId` — `translation.isEdit = false` сразу в reducer (UI получает мгновенный переход в view-mode).
- **Ветвление эффекта (3 ветви — различаются по `edited`):**
  1. **`lexeme.translation.edited.isEmpty()`** ⇒ для лексемы с `id == lexemeId` `translation = null` + `DatasourceEffect.RemoveTranslation(lexemeId)`. Политика: пустой коммит ≡ удаление перевода.
  2. **`lexeme.translation.edited == lexeme.translation.origin`** ⇒ только `translation.isEdit = false`, **никакого effect** (no-op коммит — пользователь вышел из edit без изменений, БД трогать не за чем).
  3. **Иначе** ⇒ `translation.isEdit = false` (origin не трогаем — синхронизируется через `RefreshTranslation` от data-слоя) + `DatasourceEffect.UpdateLexemeTranslation(wordId, lexemeId, translation = edited)`.
- **Guard:** `if (lexeme.translation == null || !lexeme.translation.isEdit) state to emptySet() else ...`.
- **Замечание про transient `edited != origin`:** после ветви 3 поле `translation.edited` некоторое время опережает `origin` до прихода `RefreshTranslation`. Это допускается — после v2 жёсткого инварианта «`isEdit == false ⇒ edited == origin`» нет. Альтернатива «писать `origin = edited` сразу в reducer'е» отвергается: reducer не знает, прошёл ли write в БД (failure → рассинхрон).

> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 5: Динамические эффекты — когда эффекты зависят от содержимого сообщения"
>
> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 4: Условный стейт — когда логика ветвится по данным сообщения"

#### `CancelTranslationEdit(lexemeId)`
- **Что:** пользователь отменяет редактирование перевода без коммита.
- **Trigger** (концептуальный набор, финальный выбор — в UI sub-flow):
  1. Системный back-жест внутри активного `TextField` (Compose IME back).
  2. Esc на физической клавиатуре (если поддерживается).
  3. Тап «Отмена» внутри chip в edit-mode (если такой control появится в макете).

  *Тап-вне-поля* и *IME Done* — **не** триггерят Cancel; они идут в `CommitTranslationEdit` (и при `edited == origin` или `edited.isEmpty()` ветвление reducer'а сделает no-op / Remove соответственно).
- **State changes:**
  - Если `lexeme.translation.origin.isEmpty()` (свежесозданный перевод — пользователь нажал Cancel сразу после Create) ⇒ для лексемы с `id == lexemeId` `translation = null`. Симметрично политике «пустой Commit ⇒ Remove» и удерживает 2 наблюдаемых режима nullable: либо `null`, либо `TextValueState(origin != "", ...)`. Третий «висячий» режим `TextValueState(isEdit=false, origin="", edited="")` исключается.
  - Иначе ⇒ для лексемы с `id == lexemeId` `translation.isEdit = false`, `translation.edited = translation.origin` (восстанавливаем last known good).
- **Effects:** нет — отмена не идёт в БД.
- **Guard:** `if (lexeme.translation == null || !lexeme.translation.isEdit) state to emptySet() else ...`.

> 📎 guide: docs/guides/state-modeling.md — "Считаем варианты State: помогает найти лишние/недостающие комбинации; исключать невалидные состояния на уровне типов"

#### `RemoveTranslation(lexemeId)`
- **Что:** пользователь удаляет перевод у лексемы (явный пункт, не через пустой Commit).
- **Trigger:** тап «Удалить перевод» в контекстном меню лексемы (Msg рождается из меню — это важно для сброса `isMenuOpen`).
- **State changes:** для лексемы с `id == lexemeId` — `isMenuOpen = false` (закрываем меню атомарно — Msg инициирован из меню). `translation` пока **не обнуляем** — фактическое отсутствие придёт через `RefreshTranslation(lexemeId, translation=null)` от data-слоя (каноном в `contract_io`).
- **Effects (forward-ref):** `DatasourceEffect.RemoveTranslation(lexemeId)`.

### Definition chip (внутри лексемы)

Структурно симметрично Translation.

#### `CreateDefinition(lexemeId)`
- **Что / Trigger:** тап chip «Определение» (frame `9154-82625`) в `LexemeState.definition == null`.
- **State changes:** для лексемы с `id == lexemeId` — `definition = TextValueState(origin = "", edited = "", isEdit = true)`.
- **Effects:** нет.
- **Guard:** `if (lexeme.definition != null) state to emptySet() else ...`.

#### `UpdateDefinitionInput(lexemeId, value)`
- **Что / Trigger:** ввод в `TextField` определения.
- **State changes:** для лексемы с `id == lexemeId` — `definition.edited = value`.
- **Effects:** нет.
- **Guard:** `if (lexeme.definition == null || !lexeme.definition.isEdit) state to emptySet() else ...`.

#### `EnterDefinitionEditMode(lexemeId)`
- **Что / Trigger:** повторный вход в edit существующего определения.
- **State changes:** для лексемы с `id == lexemeId` — `definition.isEdit = true`, `definition.edited = definition.origin`.
- **Effects:** нет.
- **Guard:** `if (lexeme.definition == null || lexeme.definition.isEdit) state to emptySet() else ...`.

#### `CommitDefinitionEdit(lexemeId)`
- **Что / Trigger:** коммит определения.
- **State changes:** для лексемы с `id == lexemeId` — `definition.isEdit = false` сразу в reducer.
- **Ветвление эффекта (3 ветви):**
  1. `lexeme.definition.edited.isEmpty()` ⇒ `definition = null` + `DatasourceEffect.RemoveDefinition(lexemeId)`.
  2. `lexeme.definition.edited == lexeme.definition.origin` ⇒ только `isEdit = false`, effect = `emptySet()`.
  3. Иначе ⇒ `isEdit = false` + `DatasourceEffect.UpdateLexemeDefinition(wordId, lexemeId, definition = edited)`.
- **Guard:** `if (lexeme.definition == null || !lexeme.definition.isEdit) state to emptySet() else ...`.

#### `CancelDefinitionEdit(lexemeId)`
- **Что / Trigger:** отмена редактирования определения без коммита; trigger-набор симметричен `CancelTranslationEdit` (системный back в `TextField`, Esc, явная «Отмена» если появится).
- **State changes:**
  - `lexeme.definition.origin.isEmpty()` ⇒ для лексемы `definition = null`.
  - Иначе ⇒ `definition.isEdit = false`, `definition.edited = definition.origin`.
- **Effects:** нет.
- **Guard:** `if (lexeme.definition == null || !lexeme.definition.isEdit) state to emptySet() else ...`.

#### `RemoveDefinition(lexemeId)`
- **Что / Trigger:** удаление определения у лексемы (через контекстное меню лексемы).
- **State changes:** для лексемы с `id == lexemeId` — `isMenuOpen = false` (Msg инициирован из меню, закрываем атомарно).
- **Effects (forward-ref):** `DatasourceEffect.RemoveDefinition(lexemeId)`.

### Навигация

#### `NavigateBack`
- **Что:** пользователь уходит со страницы.
- **Trigger:** системный back / тап стрелки в TopBar.
- **State changes:** **нет**.
  - Модель A: ViewModel уничтожается с экраном (типичный паттерн Compose Navigation — один экран ⇒ одна ViewModel scope; при pop весь `WordCardState` и Mate уничтожаются вместе с ViewModel).
  - Из этого следует: любые сбросы (`isCreatingLexeme`, `isMenuOpen`, `isEditMode`, `showWarningDialog`) — избыточны, state перестаёт существовать.
  - Effect-handler работает в scope ViewModel; при cancellation корутины никакого write в state не произойдёт — отдельная страховка не нужна.
- **Effects (forward-ref):** `NavigationEffect.Back`.
- **Guard:** нет.

> 📎 guide: docs/guides/messages.md — "State навигационные Msg не модифицируют — только порождают эффект"
>
> 📎 guide: docs/guides/navigation.md — "Закрытие экрана — это эффект, не state; reducer возвращает NavigationEffect.Back → Navigator.back() → navController.popBackStack()"
>
> 📎 guide: docs/guides/viewmodel-wiring.md — "Не хранить closeScreen: Boolean в state — навигация это эффект, не флаг"
>
> 📎 guide: docs/guides/state-and-extensions.md — "State = только отображаемое; никаких навигационных флагов (needClose, closeScreen) — навигация через Effect"
>
> 📎 guide: docs/guides/ui-patterns.md — "Антипаттерн LaunchedEffect(closeScreen) — закрытие экрана это эффект, не state"

### UI feedback

#### `DismissNotification`
- **Что:** snackbar скрыт (пользователь дёрнул / swipe / автоматический таймаут).
- **Trigger:** UI-side — `SnackbarHostState` сообщил, что snackbar не показывается (через `LaunchedEffect` после `showSnackbar(...)`).
- **State changes:** `snackbarState = state.snackbarState.copy(show = false)`. `title` сохраняется как есть (нет смысла перетирать — `show = false` достаточно для UI-предиката).
- **Effects:** нет.
- **Почему отдельный Msg, а не `UiMsg.ShowNotification(show=false)`:**
  - `ShowNotification(text)` — Datasource Msg (effect-handler шлёт после успеха/ошибки в БД), каноном в `contract_io`. Это не действие пользователя — выходит из scope данного артефакта.
  - `DismissNotification` — UI Msg (пользовательский dismiss / автоматический таймаут со стороны UI), его место здесь.
  - Старый `UiMsg.ShowNotification(text, show)` склеивал два направления потока через payload-дискриминатор. Раздельная декомпозиция чище по scope.
  - Sub-interface `UiMsg : Msg` снимается как избыточная вертикаль: единственный его потребитель (`ShowNotification`) ушёл в `contract_io`, остаётся один `DismissNotification` — прямого члена `sealed interface Msg` достаточно.

> 📎 guide: docs/guides/messages.md — "Внутренние сообщения (UiMsg) — генерируемые UI-эффект-хендлерами, маркируются internal"
>
> 📎 guide: docs/guides/ui-patterns.md — "Показ snackbar — UI-эффект через флаг в state допустим для toast/snackbar"

### No-op

#### `NoOperation`
- **Что:** пустой Msg.
- **Trigger:** служебный (fallback в effect-handler'е, стартовый Msg в тестах).
- **State changes:** state не меняется.
- **Effects:** нет.

> 📎 guide: docs/guides/messages.md — "Empty Message — no-op; в редьюсере `is Msg.Empty -> state to emptySet()`"
>
> 📎 guide: docs/guides/mate-framework.md — "Empty message — для своих эффектов без полезного результата; для чужих consumer не вызывается — фильтрация в filter()"

### Общая сноска про «лексема не найдена»

Все Msg с `lexemeId` (`RemoveLexeme`, `OpenLexemeMenu`, `CreateTranslation`, `UpdateTranslationInput`, `EnterTranslationEditMode`, `CommitTranslationEdit`, `CancelTranslationEdit`, `RemoveTranslation`, и парный набор для `Definition`) применяют общий неявный guard: если `lexemeList.none { it.id == lexemeId }` ⇒ `state to emptySet()`. Базовая безопасность для async-расхождений (UI отправил Msg, лексема параллельно удалена через `Refresh*`-Msg).

> 📎 guide: docs/guides/state-modeling.md — "Q: когда отдельный Idle state vs шорткат-getter; бизнес-логика игнорирует message при асинхронном рассинхроне"

## Удаляемые / новые messages

### Удаляются (4 шт. — макет-driven)

- **`OpenAddLexemeDialog`** — bottom sheet больше нет.
- **`CloseAddLexemeDialog`** — то же.
- **`EnableTranslationCreation(isAdded)`** — чекбокса нет; chip «Перевод» внутри лексемы использует `CreateTranslation(lexemeId)`.
- **`EnableDefinitionCreation(isAdded)`** — то же; используется `CreateDefinition(lexemeId)`.

### Удаляются (1 шт. — рефакторинг scope)

- **`UiMsg.ShowNotification(text, show)`** и sub-interface `UiMsg`:
  - `ShowNotification(text)` ⇒ переезжает в `contract_io` как Datasource Msg.
  - `ShowNotification(show=false)` ⇒ становится `DismissNotification` (UI Msg).
  - Sub-interface `UiMsg` не нужен — нет других членов.

### Удаляются (1 шт. — legacy / dead code)

- **`LoadingWord`** — устаревший паттерн «UI шлёт инициирующий Msg через `LaunchedEffect`». Антипаттерн (см. `docs/guides/mate-framework.md` Конвенция 5): нарушает single-direction (инициация side-effect живёт в UI-слое), порождает recompose-гонки и нуждается в guard-костыле в reducer'е.
  - В текущем проекте `LoadingWord` — мёртвый код: `WordCardScreen.kt` его не шлёт, инициирующая загрузка идёт через `initEffects = setOf(DatasourceEffect.LoadWord(wordId))` в `WordCardViewModel.kt:28`.
  - Текущая reducer-ветвь `is Msg.LoadingWord -> state.showLoading() to setOf(LoadWord(wordId = state.wordState.id))` потенциально опасна: при вызове из теста / стороннего кода со `state.wordState.id == NOT_IN_DB` отправит `LoadWord(-1L)`.
  - Действие: удалить из `Message.kt`, удалить ветвь в `WordCardReducer.kt`, удалить расширение `WordCardState.showLoading()` (либо проверить остальных потребителей).

> 📎 guide: docs/guides/mate-framework.md — "Антипаттерн LaunchedEffect → Msg.LoadingWord; правильно — initEffects в Mate через ViewModel"

### Переименовываются

- **`ExitTranslationEditMode` → `CommitTranslationEdit`** — старое имя обманывало (был commit, не cancel).
- **`ExitDefinitionEditMode` → `CommitDefinitionEdit`** — то же.
- Парные `CancelTranslationEdit` / `CancelDefinitionEdit` — **новые**, см. ниже.
- `ExitWordEditMode` (cancel у word) — **не переименовывается** (обоснование в Reducer-секции `ExitWordEditMode` выше).

> 📎 guide: docs/guides/code-style.md — "Принцип именования: имена должны быть лаконичными и понятными; короткое имя лучше длинного при равной ясности"

### Изменяется логика reducer'а у существующих Msg

- `RemoveWord` — добавлен сброс `topBarState.isMenuOpen` и `wordState.showWarningDialog`; добавлен guard на `id == NOT_IN_DB || isLoading`.
- `CommitWordChanges` — добавлено обнуление `wordState.edited = ""`; добавлен guard.
- `EnterWordEditMode` — добавлен guard на `isEditMode || id == NOT_IN_DB || isLoading`.
- `UpdateWordInput` — добавлен guard `!isEditMode`.
- `ExitWordEditMode` — добавлен явный guard `!isEditMode` (производный).
- `OpenDeleteWordDialog` — добавлен guard и сброс `topBarState.isMenuOpen`.
- `CommitTranslationEdit` / `CommitDefinitionEdit` — (а) переименование, (б) сброс `isEdit = false` в reducer'е, (в) трёхветочное ветвление эффекта `Remove / no-op / Update`.
- `CreateTranslation` / `CreateDefinition`, `EnterTranslationEditMode` / `EnterDefinitionEditMode`, `UpdateTranslationInput` / `UpdateDefinitionInput` — добавлены guards (см. таблицу ниже).
- `CreateLexeme` — добавлен state-side-effect: `isCreatingLexeme = true`; добавлен guard.
- `RemoveLexeme`, `RemoveTranslation`, `RemoveDefinition` — добавлен сброс `LexemeState.isMenuOpen = false`.
- `OpenLexemeMenu` — добавлена эксклюзивность (закрытие меню остальных лексем при `isShow == true`).
- `RefreshLexeme` — отнесён в forward-ref таблицу `contract_io` (это Datasource Msg). Reducer-логика (append + сброс `isCreatingLexeme = false`) — каноном в `contract_io`.

### Новые Msg (3 шт.)

- **`CancelTranslationEdit(lexemeId)`** — отмена редактирования перевода без коммита; при `origin == ""` ⇒ `translation = null`.
- **`CancelDefinitionEdit(lexemeId)`** — то же для определения.
- **`DismissNotification`** — пользовательский dismiss snackbar (UI-side).

### Новые guards (сводно)

Все — shortcut-ignore (`state to emptySet()`). Общая сноска «лексема не найдена» применяется ко всем адресным Msg в дополнение к перечисленному:

| Msg | Условие игнора |
|---|---|
| `CreateLexeme` | `isCreatingLexeme \|\| isLoading \|\| wordState.id == NOT_IN_DB` |
| `RemoveWord` | `wordState.id == NOT_IN_DB \|\| isLoading` |
| `OpenDeleteWordDialog` | `wordState.id == NOT_IN_DB \|\| isLoading` |
| `EnterWordEditMode` | `wordState.isEditMode \|\| wordState.id == NOT_IN_DB \|\| isLoading` |
| `UpdateWordInput` | `!wordState.isEditMode` |
| `ExitWordEditMode` | `!wordState.isEditMode` |
| `CommitWordChanges` | `!wordState.isEditMode \|\| wordState.id == NOT_IN_DB \|\| isLoading` |
| `CreateTranslation` | `lexeme.translation != null` |
| `CreateDefinition` | `lexeme.definition != null` |
| `EnterTranslationEditMode` | `translation == null \|\| translation.isEdit` |
| `EnterDefinitionEditMode` | `definition == null \|\| definition.isEdit` |
| `UpdateTranslationInput` | `translation == null \|\| !translation.isEdit` |
| `UpdateDefinitionInput` | `definition == null \|\| !definition.isEdit` |
| `CommitTranslationEdit` / `CancelTranslationEdit` | `translation == null \|\| !translation.isEdit` |
| `CommitDefinitionEdit` / `CancelDefinitionEdit` | `definition == null \|\| !definition.isEdit` |

> 📎 guide: docs/guides/testing-reducers.md — "Что тестировать: ветвление в редьюсере (if/else в обработке сообщений); граничные случаи: пустые списки, NOT_IN_DB id, null значения"

## Расхождения spec ↔ code

**Не применимо.** Режим работы — 1 (макет-driven), не 2 (spec-driven). Спека отсутствует (`spec_filename = null`). Сверка кода со спецификацией не производится; источник истины — Figma макет + текущий `Message.kt`/`WordCardReducer.kt`. Сверка с этими источниками встроена в раздел «Удаляемые / новые messages».

---

## log_messages

- contract_ui_msg v2: перерисован под v2 contract_state (без жёсткого инварианта 8, без политики сброса isCreatingLexeme на NavigateBack); удалён legacy Msg.LoadingWord, добавлены guards на word-сторону и сброс LexemeState.isMenuOpen в Remove*-Msg.
- Введены Commit*Edit / Cancel*Edit пары для translation/definition (3 ветви эффекта: Remove на пустом edited / no-op на edited==origin / Update иначе); ExitWordEditMode сохранён ради уникальности пары внутри word.
- NavigateBack зафиксирован на Модели A (ViewModel умирает с экраном — сбросы не нужны); OpenLexemeMenu эксклюзивен; UiMsg.ShowNotification раздроблен на DismissNotification (UI) + ShowNotification в contract_io.

---

_model: claude opus 4.7 (1M context)_
