# contract_ui_msg — IS479 wordcard_lexeme_inline

> WARN **Этот артефакт — черновик.** Финал Msg-списка и связи Msg ↔ Effect живёт в `contract_io.output`.
> <br>Forward-references на effects ниже — гипотезы. Ревью на этом шаге: UI-триггеры, state changes, категоризация Msg.
> <br>Не фиксировать Msg ↔ Effect связь как окончательную.

## Режим работы

**Режим 1 — макет-driven** (унаследовано из `contract_state.md`). Источники: Figma `w8GmGCdOZJUi99Cuv4q4W9` (frames `9154-82519`, `9154-82532`, `9154-82521`, `9154-82625`) + текущий `Message.kt` / `WordCardReducer.kt`. Спека отсутствует — расхождения spec↔code не сверяются.

## Scope артефакта

`sealed interface Msg` ниже описывает **UI-стороны**: действия пользователя, навигация, UI-trigger lifecycle, UI-feedback **inbound от самого UI** (пользовательский dismiss). Datasource Msg (результаты от effect-handler'ов: `WordLoaded`, `WordNotFound`, `RefreshLexeme`, `RefreshTranslation`, `RefreshDefinition`, `ShowNotification` от effect-handler'а, failure-ветки `CreateLexeme`) — каноном идут в `contract_io`, здесь не фиксируются. Для каждого Datasource Msg, имеющего отношение к reducer-логике UI, ниже есть короткая forward-ref в раздел «Канон в contract_io».

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
> 📎 guide: docs/guides/messages.md — "Не плодить Msg для состояний экрана — Msg = действие/факт, состояние выводится в reducer"

### Категоризация (UI-сторона)

**Действия пользователя:**
- `RemoveWord`, `CommitWordChanges`, `UpdateWordInput`, `EnterWordEditMode`, `ExitWordEditMode`
- `CreateLexeme`, `RemoveLexeme`
- `CreateTranslation`, `UpdateTranslationInput`, `EnterTranslationEditMode`, `CommitTranslationEdit`, `CancelTranslationEdit`, `RemoveTranslation`
- `CreateDefinition`, `UpdateDefinitionInput`, `EnterDefinitionEditMode`, `CommitDefinitionEdit`, `CancelDefinitionEdit`, `RemoveDefinition`

**Навигация (intent от UI):**
- `NavigateBack`

**UI feedback (от UI к reducer'у):**
- `DismissNotification` — тап/swipe пользователя по snackbar.

**Переключатели (toggles):**
- `OpenTopBarMenu` / `CloseTopBarMenu`
- `OpenDeleteWordDialog` / `CloseDeleteWordDialog`
- `OpenLexemeMenu` (с булевым `isShow` — payload-форма triggers (a))

**No-op:**
- `NoOperation`

### Канон в contract_io (Datasource → UI, forward-ref)

Эти Msg формируют reducer-поведение, но их декларация и точные сигнатуры фиксируются в `contract_io`:

| Datasource Msg | Семантика | Чьё reducer-side-effect упоминается ниже |
|---|---|---|
| `WordLoaded(word)` | результат `DatasourceEffect.LoadWord` (success) | завершает initial-load flow (effect улетел из `initEffects` ViewModel) |
| `WordNotFound` | результат `DatasourceEffect.LoadWord` (404) | завершает initial-load flow |
| `RefreshLexeme(lexeme)` | результат `DatasourceEffect.CreateLexeme` (success) | завершает `CreateLexeme` flow (append + reset `isCreatingLexeme`) |
| `CreateLexemeFailed` | результат `DatasourceEffect.CreateLexeme` (failure) | сбрасывает `isCreatingLexeme` + триггерит `ShowNotification` |
| `RefreshTranslation(lexemeId, translation?)` | возврат `UpdateLexemeTranslation` / `RemoveTranslation` | синхронизирует `origin` |
| `RefreshDefinition(lexemeId, definition?)` | возврат `UpdateLexemeDefinition` / `RemoveDefinition` | синхронизирует `origin` |
| `ShowNotification(text)` | effect-handler выставил snackbar после ошибки/успеха | пишет `snackbarState = SnackbarState(text, show=true)` |

## Reducer-логика per Msg

### Инициализация (initEffects, не UI Msg)

Загрузка слова идёт через `initEffects` ViewModel — не через UI Msg. См. `WordCardViewModel.kt:28`:

```kotlin
private val stateHolder = Mate(
    initState = WordCardState(),
    initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
    ...
)
```

`wordId` передаётся в ViewModel через `@AssistedInject`-навигационный аргумент. `DatasourceEffect.LoadWord(wordId)` улетает один раз при создании Mate, обработчик возвращает `WordLoaded(...)` либо `WordNotFound`. Никакой `Msg.LoadingWord` от UI **не нужен** и не существует — это анти-паттерн (см. `docs/guides/mate-framework.md` Конвенция 5).

Recompose-гонок нет: `initEffects` отрабатывают ровно один раз в `init { ... }` блоке Mate. ViewModel переживает recompose composable'ов; Mate переживает в ViewModel.

### Top bar menu (переключатели)

#### `OpenTopBarMenu`
- **Что:** пользователь раскрывает меню в шапке.
- **Trigger:** тап на иконке меню в TopBar.
- **State changes:** `topBarState.isMenuOpen = true`.
- **Effects:** нет.
- **Guard:** нет (toggle, безусловно идемпотентен).

#### `CloseTopBarMenu`
- **Что:** пользователь закрывает меню.
- **Trigger:** dismiss (тап вне меню / выбор пункта).
- **State changes:** `topBarState.isMenuOpen = false`.
- **Effects:** нет.

### Delete word

#### `OpenDeleteWordDialog`
- **Что:** пользователь инициирует удаление слова.
- **Trigger:** тап на пункте «Delete» в TopBar menu.
- **State changes:** `wordState.showWarningDialog = true`, `topBarState.isMenuOpen = false` (закрываем меню при переходе в диалог).
- **Effects:** нет.
- **Guard (F029):** `if (state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...` — нельзя открыть удаление того, чего ещё нет.

#### `CloseDeleteWordDialog`
- **Что:** пользователь отменяет удаление.
- **Trigger:** dismiss диалога / тап «Отмена».
- **State changes:** `wordState.showWarningDialog = false`.
- **Effects:** нет.

#### `RemoveWord(wordId)`
- **Что:** пользователь подтверждает удаление слова.
- **Trigger:** тап «Удалить» в confirmation-диалоге.
- **State changes:** `topBarState.isMenuOpen = false`, `wordState.showWarningDialog = false`. Закрываем меню и диалог сразу — между Msg и `NavigationEffect.Back` экран ещё виден; без сброса возможен визуальный артефакт.
- **Effects (forward-ref):** `DatasourceEffect.RemoveWord(wordId)`; затем `NavigationEffect.Back` (эмитится effect-handler'ом после успешного удаления — каноном в `contract_io`).
- **Guard (F014):** `if (state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...`.

### Word edit

#### `EnterWordEditMode`
- **Что:** пользователь входит в режим редактирования заголовка слова.
- **Trigger:** тап на заголовке / иконке pencil.
- **State changes:** `wordState.isEditMode = true`, `wordState.edited = wordState.value`.
- **Effects:** нет.
- **Guard (F027):** `if (state.wordState.isEditMode || state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...` — повторный Enter в уже открытом режиме иначе перезатёр бы `edited` пользователя.

#### `UpdateWordInput(value)`
- **Что:** пользователь вводит текст.
- **Trigger:** `onValueChange` в `TextField` заголовка.
- **State changes:** `wordState.edited = value`.
- **Effects:** нет.
- **Guard (F018/F028):** `if (!state.wordState.isEditMode) state to emptySet() else ...`. Без guard'а Update вне edit-mode записал бы `edited != ""` при `isEditMode == false` и нарушил инвариант 2 (`isEditMode == false ⇒ edited == ""`).
- **Почему payload одно поле:** простой editable (правило «Editable: Msg содержит только новое значение поля»).

#### `ExitWordEditMode`
- **Что:** пользователь отменяет редактирование.
- **Trigger:** тап «Отмена» / back-кнопка внутри edit-mode.
- **State changes:** `wordState.isEditMode = false`, `wordState.edited = ""` (инвариант 2).
- **Effects:** нет.
- **Guard:** `if (!state.wordState.isEditMode) state to emptySet() else ...` (нечего выходить, если не в режиме).
- **Naming:** оставляем `ExitWordEditMode` (cancel-семантика) — переименование в `CancelWordEdit` рассматривалось в F030, но симметрия с translation/definition уже достигнута переименованием `Exit*EditMode → Commit*Edit` там; для word существует пара `ExitWordEditMode` (cancel) / `CommitWordChanges` (commit) — конкретные глаголы (Exit/Commit) уникальны и не пересекаются с translation/definition (Commit/Cancel). Asymmetry допускается, потому что:
  - `word` — единственная сущность экрана, у Cancel нет адресной коллизии;
  - переименование `ExitWordEditMode → CancelWordEdit` затронуло бы много call-site без семантической пользы.

#### `CommitWordChanges`
- **Что:** пользователь подтверждает изменения слова.
- **Trigger:** тап «Сохранить» / IME action Done.
- **State changes (F001/F008):** `wordState.value = wordState.edited`, `wordState.isEditMode = false`, `wordState.edited = ""`. Сброс `edited` обязателен в том же reducer-шаге — инвариант 2 должен удерживаться атомарно.
- **Effects (forward-ref):** `DatasourceEffect.UpdateWord(wordId, value = wordState.edited перед сбросом)`. Сам эффект конструируется со старым значением `edited`, в state поле уже обнулено.
- **Guard (F026):** `if (!state.wordState.isEditMode || state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...`.
- **Сноска про no-op (F034 для word):** если `state.wordState.edited == state.wordState.value` — UI-слой может не отправлять `CommitWordChanges` (effect лишний). Если всё же отправил — reducer выполнит ту же мутацию (`value = edited`, `isEditMode = false`, `edited = ""`), а effect `UpdateWord` с прежним значением идемпотентен на стороне БД. В отличие от translation/definition (где no-op ветвь явно проверяется внутри reducer'а — см. ниже), здесь дополнительная ветвь не вводится: цена no-op `UpdateWord` мала, ветвление в reducer'е перевешивает.

### Lexeme (создание/удаление целиком)

#### `CreateLexeme`
- **Что:** пользователь создаёт новую пустую лексему через FAB.
- **Trigger:** тап на FAB `9154-82532` («Добавить значение»).
- **State changes:** `isCreatingLexeme = true`.
- **Effects (forward-ref):** `DatasourceEffect.CreateLexeme(wordId = state.wordState.id)`. Возврат — `RefreshLexeme(lexeme)` / `CreateLexemeFailed` (каноном в `contract_io`).
- **Guard:** `if (state.isCreatingLexeme || state.isLoading || state.wordState.id == NOT_IN_DB) state to emptySet() else ...` — защита от двойного нажатия (инвариант 7), от тапа до загрузки слова (инвариант 1).

#### `RemoveLexeme(lexemeId)`
- **Что:** пользователь удаляет существующую лексему.
- **Trigger:** тап «Удалить» в контекстном меню лексемы.
- **State changes (F025):** для лексемы с `id == lexemeId` — `isMenuOpen = false` (закрываем меню атомарно, не дожидаясь возврата из БД). Сам элемент остаётся в `lexemeList` до прихода Datasource Msg — фактическое удаление приходит каноном из `contract_io`.
- **Effects (forward-ref):** `DatasourceEffect.RemoveLexeme(lexemeId)`.
- **Guard:** «лексема не найдена по `lexemeId`» ⇒ `state to emptySet()` (см. общую сноску в конце).

#### `OpenLexemeMenu(lexemeId, isShow)`
- **Что:** пользователь открывает/закрывает контекстное меню конкретной лексемы.
- **Trigger:** тап на иконке «...» в карточке лексемы (open) или dismiss меню (close).
- **State changes (F022, эксклюзивность):**
  - Если `isShow == true` ⇒ для лексемы с `id == lexemeId` `isMenuOpen = true`; **для всех остальных лексем `isMenuOpen = false`** (DropdownMenu в Compose — модальный popup, эксклюзивный — одновременно открытое меню более чем на одной лексеме семантически невозможно).
  - Если `isShow == false` ⇒ для лексемы с `id == lexemeId` `isMenuOpen = false`; остальные не трогаем (они и так должны быть `false` по инварианту ниже).
- **Effects:** нет.
- **Почему payload не одно поле** (whitelist (a)): `(lexemeId, isShow)` — id адресации + дискриминатор операции.
- **Новый локальный инвариант (F022):** `forall l1, l2 ∈ lexemeList, l1 != l2: !(l1.isMenuOpen && l2.isMenuOpen)`. Удерживается через эксклюзивный reducer-маппинг выше. Этот инвариант — в добавок к списку из `contract_state` (формально его место там; явно фиксирую здесь, потому что он порождён reducer-логикой `OpenLexemeMenu`, а не шейпом state).

### Translation chip (внутри лексемы)

#### `CreateTranslation(lexemeId)`
- **Что:** пользователь нажимает chip «Перевод» внутри карточки лексемы, у которой ещё нет перевода.
- **Trigger:** тап на chip «Перевод» (frame `9154-82521` / `9154-82625`) в `LexemeState.translation == null`.
- **State changes:** для лексемы с `id == lexemeId` поле `translation = TextValueState(origin = "", edited = "", isEdit = true)`. Инвариант 8 удерживается.
- **Effects:** нет (создание перевода в БД произойдёт по `CommitTranslationEdit` с непустым `edited`).
- **Guard (F012):** `if (lexeme.translation != null) state to emptySet() else ...`.

#### `UpdateTranslationInput(lexemeId, value)`
- **Что:** пользователь вводит текст перевода.
- **Trigger:** `onValueChange` в inline-`TextField` перевода.
- **State changes:** для лексемы с `id == lexemeId` поле `translation.edited = value`.
- **Effects:** нет.
- **Guard (F013):** `if (lexeme.translation == null || !lexeme.translation.isEdit) state to emptySet() else ...`.

#### `EnterTranslationEditMode(lexemeId)`
- **Что:** пользователь повторно входит в режим редактирования существующего перевода.
- **Trigger:** тап по значению перевода в view-mode (chip + текст уже есть).
- **State changes:** для лексемы с `id == lexemeId` `translation.isEdit = true`, `translation.edited = translation.origin`.
- **Effects:** нет.
- **Guard (F011):** `if (lexeme.translation == null || lexeme.translation.isEdit) state to emptySet() else ...` — идемпотентность входа.

#### `CommitTranslationEdit(lexemeId)` (бывший `ExitTranslationEditMode`, переименован — F030)
- **Что:** пользователь коммитит изменения перевода.
- **Trigger:** тап вне поля / IME Done / тап на pencil-toggle.
- **State changes (F006):** для лексемы с `id == lexemeId` `translation.isEdit = false`. Сбрасываем сразу в reducer — UI получает мгновенный переход в view-mode.
- **Ветвление эффекта (F009 + F034):**
  1. **Если `lexeme.translation.edited.isEmpty()`** ⇒ `state.copy(... translation = null)` (политика: пустой коммит ≡ удаление) + `DatasourceEffect.RemoveTranslation(lexemeId)`.
  2. **Если `lexeme.translation.edited == lexeme.translation.origin`** ⇒ только `isEdit = false`, **никакого effect** (no-op коммит — UI просто покидает edit-режим, БД трогать не за чем).
  3. **Иначе** ⇒ `isEdit = false` (origin не трогаем — он синхронизируется через `RefreshTranslation`) + `DatasourceEffect.UpdateLexemeTranslation(wordId, lexemeId, translation = edited)`.
- **Guard:** `if (lexeme.translation == null || !lexeme.translation.isEdit) state to emptySet() else ...`.
- **Промежуток edited != origin до RefreshTranslation:** допускается политикой — на это время поле `edited` опережает `origin`, инвариант 8 удерживается так как `isEdit == false ∧ edited == ...` ⇒ требование `edited == origin` будет восстановлено приходом `RefreshTranslation`. Альтернатива «писать `origin = edited` сразу в reducer'е» отвергнута: reducer не знает, прошёл ли write в БД (failure-ветка вернула бы рассинхрон).

#### `CancelTranslationEdit(lexemeId)`
- **Что:** пользователь отменяет редактирование перевода без коммита.
- **Trigger (F033):** фиксирую конкретный список — финальный выбор живёт в UI sub-flow, концептуальный набор:
  1. Системный back-жест **внутри активного `TextField`** (Compose IME back / `onBackPressed` в edit-mode).
  2. Esc на физической клавиатуре (если поддерживается).
  3. Тап «Отмена» внутри chip в edit-mode (если такой control появится — на текущий момент не зафиксирован в макете).
  *Тап-вне-поля* и *IME Done* — **не** триггерят Cancel; они идут в `CommitTranslationEdit` (и при `edited == origin` или `edited.isEmpty()` ветвление reducer'а сделает no-op / Remove соответственно).
- **State changes (F021):**
  - Если `lexeme.translation.origin.isEmpty()` (свежесозданный перевод, пользователь нажал «Отмена» сразу) ⇒ `state.copy(... translation = null)`. Это симметрично политике «пустой Commit ⇒ Remove» и сохраняет 2 наблюдаемых режима nullable (либо `null`, либо `TextValueState(isEdit, origin != "", edited)`). Третий режим `TextValueState(isEdit=false, origin="", edited="")` исключён.
  - Иначе ⇒ `translation.isEdit = false`, `translation.edited = translation.origin` (восстанавливаем last known good).
- **Effects:** нет — отмена не идёт в БД.
- **Guard:** `if (lexeme.translation == null || !lexeme.translation.isEdit) state to emptySet() else ...`.

#### `RemoveTranslation(lexemeId)`
- **Что:** пользователь удаляет перевод у лексемы.
- **Trigger:** тап «Удалить перевод» в меню лексемы / отдельный жест на chip.
- **State changes (F025):** для лексемы с `id == lexemeId` `isMenuOpen = false` (закрываем меню атомарно — Msg триггерится из меню). `translation` пока не обнуляем — это придёт через `RefreshTranslation(translation=null)` (каноном в `contract_io`).
- **Effects (forward-ref):** `DatasourceEffect.RemoveTranslation(lexemeId)`.

### Definition chip (внутри лексемы)

Структурно симметрично Translation. Перечисляю кратко.

#### `CreateDefinition(lexemeId)`
- **Что / Trigger:** тап chip «Определение» в `LexemeState.definition == null`.
- **State changes:** для лексемы с `id == lexemeId` поле `definition = TextValueState(origin = "", edited = "", isEdit = true)`.
- **Effects:** нет.
- **Guard (F012):** `if (lexeme.definition != null) state to emptySet() else ...`.

#### `UpdateDefinitionInput(lexemeId, value)`
- **Что / Trigger:** ввод в `TextField` определения.
- **State changes:** `definition.edited = value` для соответствующей лексемы.
- **Effects:** нет.
- **Guard (F013):** `if (lexeme.definition == null || !lexeme.definition.isEdit) state to emptySet() else ...`.

#### `EnterDefinitionEditMode(lexemeId)`
- **Что / Trigger:** повторный вход в edit существующего определения.
- **State changes:** `definition.isEdit = true`, `definition.edited = definition.origin`.
- **Effects:** нет.
- **Guard (F011):** `if (lexeme.definition == null || lexeme.definition.isEdit) state to emptySet() else ...`.

#### `CommitDefinitionEdit(lexemeId)` (бывший `ExitDefinitionEditMode`, переименован — F030)
- **Что / Trigger:** коммит определения.
- **State changes (F006):** `definition.isEdit = false` сразу в reducer.
- **Ветвление эффекта (F009 + F034):**
  1. `lexeme.definition.edited.isEmpty()` ⇒ `definition = null` + `DatasourceEffect.RemoveDefinition(lexemeId)`.
  2. `lexeme.definition.edited == lexeme.definition.origin` ⇒ только `isEdit = false`, effect = `emptySet()`.
  3. Иначе ⇒ `isEdit = false` + `DatasourceEffect.UpdateLexemeDefinition(wordId, lexemeId, definition = edited)`.
- **Guard:** `if (lexeme.definition == null || !lexeme.definition.isEdit) state to emptySet() else ...`.

#### `CancelDefinitionEdit(lexemeId)`
- **Что / Trigger:** отмена редактирования определения без коммита; симметрично `CancelTranslationEdit`. Triggers — те же три (системный back в `TextField`, Esc, явная «Отмена» если появится).
- **State changes (F021):**
  - `lexeme.definition.origin.isEmpty()` ⇒ `definition = null`.
  - Иначе ⇒ `definition.isEdit = false`, `definition.edited = definition.origin`.
- **Effects:** нет.
- **Guard:** `if (lexeme.definition == null || !lexeme.definition.isEdit) state to emptySet() else ...`.

#### `RemoveDefinition(lexemeId)`
- **Что / Trigger:** удаление определения у лексемы (через меню лексемы).
- **State changes (F025):** для лексемы с `id == lexemeId` `isMenuOpen = false`.
- **Effects (forward-ref):** `DatasourceEffect.RemoveDefinition(lexemeId)`.

### Навигация

#### `NavigateBack`
- **Что:** пользователь уходит со страницы.
- **Trigger:** системный back / тап стрелки в TopBar.
- **State changes (F019/F031, Модель A):** **нет**.
  - Зафиксирован выбор: **ViewModel уничтожается с экраном** (типичный паттерн Compose Navigation: один экран — одна ViewModel scope, при pop весь `WordCardState` уничтожается вместе с ViewModel).
  - Из этого следует: любые сбросы (`isCreatingLexeme`, `isMenuOpen`, `isEditMode`, `showWarningDialog`) — избыточны, state перестаёт существовать.
  - Прежний рационал «`isCreatingLexeme = false` как страховка от async-write в умершее состояние» — снимается: effect-handler уже работает в scope ViewModel; при cancellation корутины никакого write в state не произойдёт.
- **Effects (forward-ref):** `NavigationEffect.Back`.
- **Guard:** нет.

### UI feedback

#### `DismissNotification` (F032 — вариант (c))
- **Что:** пользователь дёрнул snackbar (тап / swipe / по таймауту через `LaunchedEffect`).
- **Trigger:** UI-side — `SnackbarHostState` сообщил, что snackbar скрыт.
- **State changes:** `snackbarState = state.snackbarState.copy(show = false)`. `title` сохраняется как есть (инвариант 6 не требует обнуления).
- **Effects:** нет.
- **Почему отдельный Msg, а не `UiMsg.ShowNotification(show=false)`:**
  - `ShowNotification(text)` — Datasource Msg (effect-handler шлёт после успеха/ошибки в БД) — каноном в `contract_io`. Это не действие пользователя, scope этого артефакта он покидает.
  - `DismissNotification` — UI Msg (пользовательский dismiss / автоматический таймаут со стороны UI), здесь уместен.
  - Старый `UiMsg.ShowNotification(text, show)` склеивал два направления потока через payload-дискриминатор. Раздельная декомпозиция чище по scope.
  - Sub-interface `UiMsg : Msg` снят как избыточная вертикаль: единственный потребитель (`ShowNotification`) ушёл в `contract_io`, остался один `DismissNotification` — прямого члена `sealed interface Msg` достаточно.

### No-op

#### `NoOperation`
- **Что:** пустой Msg.
- **Trigger:** служебный (стартовый Msg в ViewModel, fallback в effect-handler'е).
- **State changes:** state не меняется.
- **Effects:** нет.

### Общая сноска про «лексема не найдена»

Все Msg с `lexemeId` (`RemoveLexeme`, `OpenLexemeMenu`, `CreateTranslation`, `UpdateTranslationInput`, `EnterTranslationEditMode`, `CommitTranslationEdit`, `CancelTranslationEdit`, `RemoveTranslation`, и парный набор для `Definition`) применяют общий неявный guard: если `lexemeList.none { it.id == lexemeId }` ⇒ `state to emptySet()`. Это базовая безопасность для async-расхождений (UI отправил Msg, лексема параллельно удалена через `RefreshLexeme`).

## Удаляемые / новые messages

### Удаляются (4 шт. — макет-driven)

- **`OpenAddLexemeDialog`** — bottom sheet больше нет. Триггер (e).
- **`CloseAddLexemeDialog`** — то же.
- **`EnableTranslationCreation(isAdded)`** — чекбокса нет; chip «Перевод» внутри лексемы использует `CreateTranslation(lexemeId)`. Триггер (e).
- **`EnableDefinitionCreation(isAdded)`** — то же.

### Удаляются (1 шт. — рефакторинг scope)

- **`UiMsg.ShowNotification(text, show)`** и sub-interface `UiMsg`:
  - `ShowNotification(text)` ⇒ переезжает в `contract_io` как Datasource Msg.
  - `ShowNotification(show=false)` ⇒ становится `DismissNotification` (UI Msg).
  - Sub-interface `UiMsg` не нужен — нет других членов.

### Удаляются (1 шт. — legacy/dead code)

- **`LoadingWord`** — устаревший паттерн «UI шлёт инициирующий Msg через `LaunchedEffect`». Антипаттерн (см. `docs/guides/mate-framework.md` Конвенция 5): нарушает single-direction (инициация side-effect живёт в UI-слое), порождает recompose-гонки и нуждается в guard-костыле в reducer'е.
  - В текущем проекте `LoadingWord` — мёртвый код: `WordCardScreen.kt` его не шлёт, инициирующая загрузка идёт через `initEffects = setOf(DatasourceEffect.LoadWord(wordId))` в `WordCardViewModel.kt:28`.
  - Ветвь reducer'а `is Msg.LoadingWord -> state.showLoading() to setOf(LoadWord(wordId = state.wordState.id))` потенциально багует: при вызове из теста / стороннего кода `state.wordState.id == NOT_IN_DB` ⇒ шлёт `LoadWord(-1L)`.
  - Удалить из `Message.kt`, удалить ветвь в `WordCardReducer.kt`. Триггер (e) — удаление существующего Msg.

### Переименовываются (F030 — выровнено по парам Commit/Cancel)

- **`ExitTranslationEditMode` → `CommitTranslationEdit`** (был commit, имя обманывало).
- **`ExitDefinitionEditMode` → `CommitDefinitionEdit`** (то же).
- `CancelTranslationEdit` / `CancelDefinitionEdit` — новые, см. ниже.
- `ExitWordEditMode` (cancel у word) — **не переименовывается**: пара `ExitWordEditMode` (cancel) / `CommitWordChanges` (commit) уникальна внутри word, цена переименования > пользы (обоснование в Reducer-секции `ExitWordEditMode` выше).

### Изменяется логика reducer'а у существующих Msg

(F023 — маркировка `[ИЗМЕНЕНО]` снята отовсюду для единообразия; список изменённых reducer'ов перечислен здесь, и в каждой Reducer-секции есть пометка финдинга.)

- `RemoveWord` — добавлены state-side-effects: сброс `topBarState.isMenuOpen` и `wordState.showWarningDialog` (F005); guard (F014).
- `CommitWordChanges` — добавлено обнуление `wordState.edited = ""` (F001/F008); guard (F026).
- `EnterWordEditMode` — добавлен guard (F027).
- `UpdateWordInput` — добавлен guard на `isEditMode` (F018/F028).
- `OpenDeleteWordDialog` — добавлен guard и сброс `topBarState.isMenuOpen` (F029).
- `CommitTranslationEdit` / `CommitDefinitionEdit` — (а) переименование (F030), (б) сброс `isEdit = false` в reducer'е (F006), (в) трёхветочное ветвление эффекта `Remove / no-op / Update` (F009 + F034).
- `CreateTranslation` / `CreateDefinition`, `EnterTranslationEditMode` / `EnterDefinitionEditMode`, `UpdateTranslationInput` / `UpdateDefinitionInput` — добавлены guards (F011, F012, F013).
- `CreateLexeme` — добавлен state-side-effect: `isCreatingLexeme = true`.
- `RemoveLexeme`, `RemoveTranslation`, `RemoveDefinition` — добавлен сброс `LexemeState.isMenuOpen = false` (F025).
- `OpenLexemeMenu` — добавлена эксклюзивность (закрытие меню остальных лексем — F022).
- `NavigateBack` — убран сброс `isCreatingLexeme` (F019/F031, Модель A — ViewModel умирает с экраном).

### Новые Msg (3 шт.)

- **`CancelTranslationEdit(lexemeId)`** (F010) — отмена редактирования перевода без коммита; при `origin == ""` ⇒ `translation = null` (F021).
- **`CancelDefinitionEdit(lexemeId)`** (F010) — то же для определения.
- **`DismissNotification`** (F032) — пользовательский dismiss snackbar (UI-side).

### Новые guards (сводно)

Точечные idempotency / pre-condition guards. Все — shortcut-ignore (`state to emptySet()`). В таблице — выделенные **в reducer'е** guards (общая сноска «лексема не найдена» применяется ко всем адресным Msg в дополнение к перечисленному):

| Msg | Условие игнора | Finding |
|---|---|---|
| `CreateLexeme` | `isCreatingLexeme \|\| isLoading \|\| wordState.id == NOT_IN_DB` | (было) |
| `RemoveWord` | `wordState.id == NOT_IN_DB \|\| isLoading` | F014 |
| `OpenDeleteWordDialog` | `wordState.id == NOT_IN_DB \|\| isLoading` | F029 |
| `EnterWordEditMode` | `wordState.isEditMode \|\| wordState.id == NOT_IN_DB \|\| isLoading` | F027 |
| `UpdateWordInput` | `!wordState.isEditMode` | F018/F028 |
| `ExitWordEditMode` | `!wordState.isEditMode` | (производный) |
| `CommitWordChanges` | `!wordState.isEditMode \|\| wordState.id == NOT_IN_DB \|\| isLoading` | F026 |
| `CreateTranslation` | `lexeme.translation != null` | F012 |
| `CreateDefinition` | `lexeme.definition != null` | F012 |
| `EnterTranslationEditMode` | `translation == null \|\| translation.isEdit` | F011 |
| `EnterDefinitionEditMode` | `definition == null \|\| definition.isEdit` | F011 |
| `UpdateTranslationInput` | `translation == null \|\| !translation.isEdit` | F013 |
| `UpdateDefinitionInput` | `definition == null \|\| !definition.isEdit` | F013 |
| `CommitTranslationEdit` / `CancelTranslationEdit` | `translation == null \|\| !translation.isEdit` | (производный) |
| `CommitDefinitionEdit` / `CancelDefinitionEdit` | `definition == null \|\| !definition.isEdit` | (производный) |

## Расхождения spec ↔ code

**Не применимо.** Режим работы — 1 (макет-driven), не 2 (spec-driven). Спека отсутствует (`spec_filename = null`). Сверка кода со спецификацией не производится; источник истины — Figma макет + текущий `Message.kt`/`WordCardReducer.kt`. Сверка с этими источниками — встроена в раздел «Удаляемые / новые messages».

---

## log_messages

- contract_ui_msg итерация 3: закрыты все 14 уникальных approved findings (3 critical + 11 minor); LoadingWord guard переформулирован на пост-условие WordLoaded (F017/F024), UpdateWordInput получил guard `!isEditMode` (F018/F028), Remove*-Msg закрывают `isMenuOpen` атомарно (F025).
- Переименованы Exit{Translation,Definition}EditMode → Commit{...}Edit (F030); UiMsg.ShowNotification раздроблен на DismissNotification (UI) + ShowNotification в contract_io (F032); Cancel*Edit при origin="" обнуляют translation/definition (F021); OpenLexemeMenu сделан эксклюзивным (F022); добавлены guards для EnterWordEditMode/CommitWordChanges/OpenDeleteWordDialog (F026/F027/F029); Commit*Edit получили no-op ветвь edited==origin (F034); NavigateBack зафиксирован на Модели A (F019/F031); RefreshLexeme унесён в раздел «Канон в contract_io» (F020); маркировки [ИЗМЕНЕНО]/[НОВОЕ] сняты (F023).
- Финальная архитектура Msg: 28 UI Msg (5 lifecycle/nav/no-op + 2 top bar + 3 delete word + 4 word edit + 3 lexeme + 6 translation + 6 definition + 1 UI feedback), Datasource Msg перечислены в forward-ref таблице.

---

_model: claude opus 4.7 (1M context)_
