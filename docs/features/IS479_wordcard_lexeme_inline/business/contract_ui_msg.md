# contract_ui_msg — IS479 wordcard_lexeme_inline (v3.2)

> ⚠ **Этот артефакт — черновик.** Финал Msg-списка и связи Msg ↔ Effect живёт в `contract_io.output`.
>
> Forward-references на effects — гипотезы. Ревью на этом шаге: UI-триггеры, state changes, категоризация Msg.
>
> Не фиксировать Msg ↔ Effect связь как окончательную.

## Режим работы

**Режим 1 — макет-driven** (унаследовано из `contract_state.md` v2.3). Источники: Figma `w8GmGCdOZJUi99Cuv4q4W9` (frames `9154-82519`, `9154-82532`, `9154-82521`, `9154-82625`) + текущий `Message.kt` / `WordCardReducer.kt`. Спека отсутствует — расхождения spec↔code не сверяются.

**Главное отличие от v2:** `WordState` теперь sealed (`NotLoaded | Loaded(...)`). Reducer-логика везде идёт через `when (state.wordState)`. Любая ветка обязана обработать оба случая — для большинства Msg при `NotLoaded` это явный `state to emptySet()` (Msg не применим до прихода `WordLoaded`).

## Scope артефакта

`sealed interface Msg` ниже описывает **UI-стороны**: действия пользователя, навигация, UI-feedback inbound от самого UI (пользовательский dismiss snackbar). Datasource Msg (результаты от effect-handler'ов — `WordLoaded`, `WordNotFound`, `RefreshWord`, `RefreshTranslation`, `RefreshDefinition`, `RefreshLexemeList`, `LexemeCascadeRemoved`, `ShowNotification`, `NavigateBack`) — каноном идут в `contract_io`, здесь упомянуты только forward-ref таблицей для reducer-логики, на которую они влияют.

> 📎 guide: docs/guides/messages.md — "Категории сообщений: действия пользователя, навигация, результаты данных, UI обратная связь, переключатели, no-op"
>
> 📎 guide: docs/guides/effect-handlers.md — "Маппинг Effect → Message: деструктивные операции часто запускают перезагрузку"

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
| `WordLoaded(term)` | результат `DatasourceEffect.LoadWord` (success) | завершает initial-load flow: `wordState = Loaded(...)`, `isLoading = false` (инв. 11) |
| `WordNotFound` | результат `DatasourceEffect.LoadWord` (404) | завершает initial-load flow с error-веткой |
| `RefreshWord(term)` | возврат `DatasourceEffect.UpdateWord` (success) | синхронизирует `wordState.value` после commit заголовка |
| `RefreshTranslation(lexemeId, translation?)` | возврат `UpdateLexemeTranslation` / `RemoveTranslation` (handler знает реальный `lexemeId` после Update в БД) | синхронизирует `translation.origin` (или ставит `translation = null` если БД вернула отсутствие). **Кроме того:** если в state нет лексемы с `lexemeId`, но есть лексема с `id == NOT_IN_DB` — reducer заменяет `NOT_IN_DB` на `lexemeId` (завершение «первого Commit» через нового lexeme insert). См. отдельный раздел ниже |
| `RefreshDefinition(lexemeId, definition?)` | возврат `UpdateLexemeDefinition` / `RemoveDefinition` (handler знает реальный `lexemeId`) | симметрично `RefreshTranslation` — включая замену `NOT_IN_DB → lexemeId` |
| `RefreshLexemeList(lexemes)` | возврат `DatasourceEffect.RemoveLexeme` (success) | resync порядка/состава `lexemeList` после удаления лексемы целиком |
| `LexemeCascadeRemoved(lexemeId)` | cascade в БД (последняя суб-сущность лексемы удалена ⇒ data-слой удалил саму лексему) | reducer удаляет лексему из `lexemeList` |
| `ShowNotification(text)` | effect-handler выставил snackbar после ошибки/успеха | пишет `snackbarState = SnackbarState(title = text, show = true)` (инв. 8) |
| `NavigateBack` | effect-handler сигналит «уход с экрана» после успешного `RemoveWord` или `WordNotFound` | триггерит `NavigationEffect.Back` в data-слое (каноном в `contract_io`) |

**Удалены (vs ит.5):**
- `RefreshLexeme(lexeme)` — больше не существует. Канала «свежесозданная лексема из БД» нет: лексема создаётся в БД при первом Commit Translation/Definition через `UpdateLexemeTranslation` / `UpdateLexemeDefinition` (effect handler делает insert лексемы + suб-сущности и возвращает `RefreshTranslation` / `RefreshDefinition` с реальным `lexemeId`).
- `CreateLexemeFailed` — больше не существует. UI Msg `CreateLexeme` не порождает effect (лексема живёт локально с `NOT_IN_DB`), значит и failure-канала нет. Ошибки первого Commit идут через failure-обработку соответствующих `Update*` effects.

> 📎 guide: docs/guides/messages.md — "Результаты эффектов: прошедшее время или существительное (*Loaded, *Update, *Skipped)"
>
> 📎 guide: docs/guides/effect-handlers.md — "Маппинг Effect → Message: handler конвертирует результат в Msg, consumer вызывается только при полезном msg"

Точные сигнатуры (`text` vs `textRes`, `lexemeId` vs `lexeme`) — закреплены в `contract_io`.

## Reducer-логика per Msg

### Инициализация (initEffects, не UI Msg)

Загрузка слова идёт через `initEffects` ViewModel — **не через UI Msg**. См. `WordCardViewModel.kt`:

```kotlin
private val stateHolder = Mate(
    initState = WordCardState(),
    initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
    ...
)
```

`WordCardState()` стартует с `wordState = WordState.NotLoaded`, `isLoading = true` — соответствует инварианту 11 (`isLoading ⇒ NotLoaded`).

**Мини-патч ит.7 — начальный `isPendingDbOp = false` (согласовано с state v2.5):** в начальном `WordCardState()` `isPendingDbOp = false` (дефолт поля в state v2.5). Во время initial `LoadWord` UI скрыт через `isLoading = true` (loading spinner overlay полностью закрывает интерактивные элементы) — отдельная UI-блокировка через `isPendingDbOp` для init не нужна. `WordLoaded` / `WordNotFound` reducer гарантированно держат `isPendingDbOp = false` (idempotent safety).

> 📎 guide: docs/guides/mate-framework.md — "initEffects запускают первую загрузку данных при создании экрана; антипаттерн LaunchedEffect → Msg.LoadingWord"
>
> 📎 guide: docs/guides/viewmodel-wiring.md — "ViewModel через @AssistedInject; runtime-аргументы (wordId, navigator) через @Assisted; initEffects = setOf(DatasourceEffect.LoadWord(wordId))"

`wordId` передаётся в ViewModel через `@AssistedInject` навигационный аргумент. `DatasourceEffect.LoadWord(wordId)` улетает ровно один раз при создании Mate; обработчик возвращает `WordLoaded(...)` либо `WordNotFound`. Никакой `Msg.LoadingWord` от UI **не существует** — это анти-паттерн.

> 📎 guide: docs/guides/mate-framework.md — "Антипаттерн LaunchedEffect → Msg.LoadingWord: триггер side-effect в UI нарушает single-direction; recompose-гонки; мёртвый Msg-вариант"

### Глобальный guard `isPendingDbOp` (мини-патч ит.7)

`contract_state` v2.5 ввёл флаг `WordCardState.isPendingDbOp: Boolean = false` — выставляется в `true` при отправке любого `DatasourceEffect` через UI Msg и сбрасывается в `false` при возврате соответствующего Datasource Msg (Refresh*/Word*/Show*/Navigate*). Назначение — заблокировать UI-клики в окне между отправкой Effect и приходом confirm-Msg, чтобы избежать гонок (двойной commit, удаление поверх pending insert, и т.п.).

**Глобальный guard:** любой UI Msg, отличный от `Init`/служебных, при `state.isPendingDbOp = true` ⇒ `state to emptySet()` (no-op). Эта проверка концептуально применяется ДО специфичного reducer-ветвления, **поверх** перечисленных ниже per-Msg guard'ов. На стороне UI Composable дополнительно блокируются все интерактивные элементы через `enabled = !state.isPendingDbOp` — но reducer'у нельзя полагаться на UI: при гонке рендера Msg всё равно может долететь.

**Применяется к** (все UI Msg, способные породить Effect либо концептуально требующие блокировки во время pending DB op):
- `CommitWordChanges`, `RemoveWord`, `EnterWordEditMode`
- `CreateLexeme`, `RemoveLexeme`, `OpenLexemeMenu`
- `CreateTranslation`, `EnterTranslationEditMode`, `CommitTranslationEdit`, `RemoveTranslation`
- `CreateDefinition`, `EnterDefinitionEditMode`, `CommitDefinitionEdit`, `RemoveDefinition`
- (далее везде, где per-Msg секция возвращает `state to setOf(DatasourceEffect.*)` хотя бы в одной ветке)

**НЕ применяется к** (локальные сбросы без Effect — допустимы и осмыслены при pending):
- `DismissNotification` — пользователь / таймаут гасит snackbar; не должен блокироваться сетевой/БД-операцией.
- `CloseTopBarMenu`, `CloseDeleteWordDialog` — закрытие popup/диалога.
- `CancelTranslationEdit`, `CancelDefinitionEdit`, `ExitWordEditMode` — локальная отмена ввода (effect'а не порождает в принципе).
- `UpdateWordInput`, `UpdateTranslationInput`, `UpdateDefinitionInput` — ввод в TextField (UI должен заблокировать поле через `enabled`, но если Msg долетел — игнорировать через обычный per-Msg guard `!isEditMode`).
- `NavigateBack` — навигация поверх pending допустима (ViewModel/Mate уничтожится с экраном, in-flight effect отменится в scope).
- `NoOperation` — служебный no-op.

Per-Msg секции ниже не дублируют ремарку про `isPendingDbOp` явно в каждом блоке — глобальный guard читается из этой секции и сводной таблицы guards.

### Хелпер reducer'а: `closeAllEditModes()` (под инвариант 9)

Инвариант 9 (`wordEdit + Σ lexEdits ≤ 1`) требует, чтобы любой Enter/Create любого edit-mode атомарно закрыл все остальные edit-mode флаги. Чтобы не размазывать эту логику по 6+ Msg-веткам, заводим вспомогательную extension-функцию (декларируется в reducer'е / `State.kt`):

```kotlin
internal fun WordCardState.closeAllEditModes(): WordCardState =
    copy(
        wordState = when (val w = wordState) {
            is WordState.NotLoaded -> w
            is WordState.Loaded    -> w.copy(isEditMode = false, edited = "")
        },
        lexemeList = lexemeList.map { l ->
            l.copy(
                translation = l.translation?.copy(isEdit = false, edited = ""),
                definition  = l.definition?.copy(isEdit = false, edited = ""),
            )
        }
    )
```

После закрытия всех edit-mode флагов одновременно очищается `edited` (инв. 1, инв. 4 — `isEdit == false ⇒ edited == ""`).

**Кто обязан вызывать `closeAllEditModes()` (после успешного guard'а, до специфичной мутации):**
- `EnterWordEditMode`
- `EnterTranslationEditMode`, `EnterDefinitionEditMode`
- `CreateTranslation`, `CreateDefinition` (создание `TextValueState` с `isEdit = true` — то же открытие edit-mode)
- `CreateLexeme` — создание новой лексемы концептуально перекрывает любой текущий ввод, фокус UX переезжает на новую карточку. Хотя сам `CreateLexeme` сразу не открывает `isEdit` ни у одного `TextValueState`, разумно закрыть текущий edit, чтобы не висел зомби-фокус IME во время асинхронного `CreateLexeme` flow.
- `OpenLexemeMenu(isShow=true)` — открытие меню рождается из view-mode карточки; держать активный edit при раскрытом popup'е семантически противоестественно (см. F007).

Все остальные Msg либо вообще не трогают edit-mode флаги, либо самостоятельно гасят свой собственный (Commit*/Cancel*/Exit*/RemoveWord и т.д.) и не нуждаются в `closeAllEditModes()`.

> **Правило immutable-mapping после `closeAllEditModes()` (F012/F017, ослаблено F029):**
> Хелпер возвращает **новый** `WordCardState` со сброшенными edit-mode флагами и обнулённым `edited` во **всех** `TextValueState`. Объём правила (что обязательно читать из результата хелпера, а что — допустимо из исходного `state`):
>
> - **`lexemeList` — обязательно из результата хелпера.** Любой последующий `.copy(lexemeList = ...)` обязан брать список из **результата** хелпера (через промежуточный val `closed` или `.let { it.copy(...) }`), **не** из исходного `state.lexemeList` (он immutable receiver: справа от `.copy(...)` ссылка ведёт на старый список, и слева результат хелпера затирается старой версией со всё ещё активным `isEdit/edited`).
>
> - **`wordState` — обязательно из результата хелпера, только если ветка мутирует то же поле.** `closeAllEditModes()` мутирует у `Loaded` исключительно `isEditMode` и `edited`; `value`, `id`, `added`, `showWarningDialog` он не трогает. Следовательно: ветки, которые читают `value` / `id` / `added` / `showWarningDialog` из исходного `w: Loaded` (`state.wordState as Loaded`), не теряют корректности — эти поля идентичны между `state.wordState` и `closed.wordState`.
>
> - **F033 — ослабление F029 для `EnterWordEditMode`:** допустимо `w.copy(...)` исходного `w` (а не `closed.wordState`), потому что мутируемые поля `isEditMode` / `edited` хелпер сбросил в `closed.wordState`, но reducer-ветка тут же перезаписывает их явно (`isEditMode = true`, `edited = w.value`). Перезапись поверх сброса — функционально эквивалентна перезаписи поверх «оригинала», потому что копируемые из `w` поля (`value`, `id`, `added`, `showWarningDialog`) одинаковы в обоих источниках. Pseudocode `EnterWordEditMode` (см. ниже) пишет `w.copy(...)` сознательно — это корректно при условии что мутируемые поля явно перезаписываются.
>
> Касается всех веток: `OpenLexemeMenu(isShow=true)`, `CreateTranslation`, `CreateDefinition`, `EnterTranslationEditMode`, `EnterDefinitionEditMode`, `CreateLexeme`, `EnterWordEditMode`.

> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 1: Только стейт (без эффектов) — цепочка расширений + пустой набор эффектов"

### Top bar menu

#### `OpenTopBarMenu`
- **Что:** пользователь раскрывает меню в шапке.
- **Trigger:** тап на иконке меню в TopBar.
- **State changes:** `topBarState.isMenuOpen = true`.
- **Effects:** нет.
- **Guard:** нет (toggle, безусловно идемпотентен).

#### `CloseTopBarMenu`
- **Что:** пользователь закрывает меню.
- **Trigger:** dismiss (тап вне меню).
- **State changes:** `topBarState.isMenuOpen = false`.
- **Effects:** нет.
- **Guard (F030):** `!state.topBarState.isMenuOpen` ⇒ `state to emptySet()`. Явный shortcut-ignore при повторном Close (например двойной dismiss от Compose `DropdownMenu` + back-handler) — экономит лишний `copy()` и делает no-op наблюдаемым в reducer-логе. Симметрично `CloseDeleteWordDialog` (F010) / `DismissNotification` (F011).

### Delete word

#### `OpenDeleteWordDialog`
- **Что:** пользователь инициирует удаление слова.
- **Trigger:** тап на пункте «Delete» в TopBar menu.
- **State changes (F026 — через `when`):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded ->
          state.copy(
              wordState   = w.copy(showWarningDialog = true),
              topBarState = state.topBarState.copy(isMenuOpen = false)
          ) to emptySet()
      WordState.NotLoaded -> state to emptySet()
  }
  ```
  Атомарно закрываем меню при переходе в диалог — между состояниями экран ещё виден, без сброса меню перекроет диалог.
- **Effects:** нет.
- **Guard:** `state.wordState !is WordState.Loaded` ⇒ `state to emptySet()` — нельзя открыть удаление того, чего ещё нет в БД (sealed закрывает связку `id ↔ added ↔ value`, отдельной проверки на `NOT_IN_DB` больше не нужно). В pseudocode выше реализован веткой `WordState.NotLoaded ->`.

> 📎 guide: docs/guides/state-modeling.md — "Top-level state = data class; внутри — sealed class когда нужно XOR"

#### `CloseDeleteWordDialog`
- **Что:** пользователь отменяет удаление.
- **Trigger:** dismiss диалога / тап «Отмена».
- **State changes (F026 — через `when`):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded ->
          if (!w.showWarningDialog) state to emptySet()
          else state.copy(wordState = w.copy(showWarningDialog = false)) to emptySet()
      WordState.NotLoaded -> state to emptySet()
  }
  ```
- **Effects:** нет.
- **Guard (F010):** `state.wordState !is WordState.Loaded || !state.wordState.showWarningDialog` ⇒ `state to emptySet()`. Явный shortcut-ignore вместо «idempotent overwrite» — экономит лишний `copy()` при повторных dismiss-событиях от UI и делает no-op наблюдаемым в reducer-логе. В pseudocode реализован веткой `WordState.NotLoaded ->` + внутренним `if (!w.showWarningDialog)`.

#### `RemoveWord(wordId)`
- **Что:** пользователь подтверждает удаление слова.
- **Trigger:** тап «Удалить» в confirmation-диалоге.
- **State changes (F031/F026 — полный pseudocode через `when`):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded -> {
          if (w.id != msg.wordId) state to emptySet()
          else state.copy(
              wordState      = w.copy(showWarningDialog = false),
              topBarState    = state.topBarState.copy(isMenuOpen = false),
              isPendingDbOp  = true,
          ) to setOf(DatasourceEffect.RemoveWord(w.id))
      }
      WordState.NotLoaded -> state to emptySet()
  }
  ```
  Закрываем меню и диалог сразу — между этим Msg и `NavigationEffect.Back` экран ещё виден; без сброса возможен визуальный артефакт. В effect передаётся `w.id`, а не `msg.wordId` — после guard'а они равны, но `w.id` уже smart-cast'нут компилятором как `Long` без unboxing.
  **Мини-патч ит.7 — `isPendingDbOp = true`:** ветка с effect выставляет pending; сброс — в `NavigateBack`-handler / `ShowNotification` (failure-путь) каноном в `contract_io`.
- **Effects (forward-ref):** `DatasourceEffect.RemoveWord(w.id)`; затем effect-handler эмитит `NavigationEffect.Back` после успешного удаления (каноном в `contract_io`).
- **Guard (F005):** `state.wordState !is WordState.Loaded || state.wordState.id != msg.wordId` ⇒ `state to emptySet()`. Сверка `id` из payload с `state.wordState.id` защищает от async race (меню задержалось — пользователь успел переключить word, но Msg долетел поздно с протухшим `wordId`). Дополнительная проверка `isLoading` избыточна — по инв. 11 `isLoading ⇒ NotLoaded`, что уже отсекается типом. В pseudocode реализован веткой `WordState.NotLoaded ->` + внутренним `if (w.id != msg.wordId)`.

> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 3: Стейт + эффекты — цепочка расширений вместе с эффектами"
>
> 📎 guide: docs/guides/navigation.md — "Conditional навигация в reducer, а не в composable"

### Word edit

#### `EnterWordEditMode`
- **Что:** пользователь входит в режим редактирования заголовка слова.
- **Trigger:** тап на заголовке / иконке pencil.
- **State changes (F003 — через `when`, не `as`):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded -> {
          val closed = state.closeAllEditModes()
          closed.copy(
              wordState = w.copy(isEditMode = true, edited = w.value)
          ) to emptySet()
      }
      WordState.NotLoaded ->
          state to emptySet()
  }
  ```
  Сначала `closeAllEditModes()` гасит чужие edit-mode (инв. 9), затем для текущего word ставим `isEditMode = true`, `edited = w.value` (буфер инициализируется текущим значением). `w.value` берётся из исходного `w: Loaded` — `closeAllEditModes()` не меняет `value`, только `isEditMode/edited`, так что для word здесь смена ссылки на `closed.wordState` неважна (`value` идентичен). Промежуточный val `closed` нужен для согласованного применения правила (см. раздел про `closeAllEditModes()`).
- **Effects:** нет.
- **Замечание:** идемпотентность повторного входа здесь не критична — `closeAllEditModes()` уже обнулит чужие edit-mode, затем выставит свой; даже если уже был свой `isEditMode=true`, повторный вход перезатрёт `edited = value`, что эквивалентно «откатить случайные правки». Приемлемая семантика; инв. 9 удерживается без дополнительной проверки.

> 📎 guide: docs/guides/state-and-extensions.md — "TextValueState — паттерн для toggle edit/view: origin (сохранённое) + edited (в процессе) + isEdit (режим)"

#### `UpdateWordInput(value)`
- **Что:** пользователь вводит текст заголовка.
- **Trigger:** `onValueChange` в `TextField` заголовка.
- **State changes (через `when`):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded ->
          if (!w.isEditMode) state to emptySet()
          else state.copy(wordState = w.copy(edited = msg.value)) to emptySet()
      WordState.NotLoaded -> state to emptySet()
  }
  ```
- **Effects:** нет.
- **Guard:** `state.wordState !is WordState.Loaded || !state.wordState.isEditMode` ⇒ `state to emptySet()`. Без guard'а Update вне edit-mode записал бы `edited != ""` при `isEditMode == false` — нарушение инв. 1.
- **Почему payload — одно поле:** простой editable (правило «Msg на изменение значения содержит только новое значение поля»).

> 📎 guide: docs/guides/messages.md — "data class (с данными) для действий с параметрами от UI или эффектов: ChangeWordValue(val value: String)"

#### `ExitWordEditMode`
- **Что:** пользователь отменяет редактирование заголовка без коммита.
- **Trigger:** тап «Отмена» / back-кнопка внутри edit-mode.
- **State changes (через `when`):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded ->
          if (!w.isEditMode) state to emptySet()
          else state.copy(wordState = w.copy(isEditMode = false, edited = "")) to emptySet()
      WordState.NotLoaded -> state to emptySet()
  }
  ```
  Сброс `edited` — инв. 1.
- **Effects:** нет.
- **Naming (асимметрия с translation/definition):** для word оставляем существующее `ExitWordEditMode` (cancel) + `CommitWordChanges` (commit). Пара глаголов уникальна внутри word (нет коллизии адресации), переименование `Exit→Cancel` дало бы только семантическую симметрию ценой массовой правки call-site. Для translation/definition пара `Commit*Edit` / `Cancel*Edit` симметрична намеренно — там сосуществуют две сущности (translation и definition), однообразие имён внутри них помогает.

#### `CommitWordChanges`
- **Что:** пользователь подтверждает изменения заголовка.
- **Trigger:** тап «Сохранить» / IME action Done.
- **Guard (F001 / F006 — критично для инв. 10):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded ->
          if (!w.isEditMode || w.edited.isBlank()) state to emptySet()
          else <commit-branch>
      WordState.NotLoaded -> state to emptySet()
  }
  ```
  Проверка `edited.isBlank()` — **жёсткая структурная защита инв. 10** (`Loaded ⇒ value != ""`). Отсекает пустые и whitespace-only коммиты независимо от того, заблокировал ли UI кнопку «Сохранить». Инв. 10 теперь удерживается reducer'ом, **не зависит** от UI-блокировки кнопки.
- **State changes (commit-branch):** `state.copy(wordState = w.copy(value = w.edited, isEditMode = false, edited = ""), isPendingDbOp = true)`. Сброс `edited` обязателен в том же reducer-шаге, чтобы не оставлять «грязный» буфер вне edit-mode (инв. 1). **Мини-патч ит.7 — `isPendingDbOp = true`:** ветка шлёт `UpdateWord` effect; сброс — в `RefreshWord` / `ShowNotification` (failure).
- **Effects (forward-ref):** `DatasourceEffect.UpdateWord(wordId = w.id, value = w.edited)`. Эффект конструируется со snapshot'ом `edited` ДО мутации (`val newValue = w.edited` сохранить перед `copy(...)`), затем подставить в эффект.
- **No-op `edited == value` (UI-side оптимизация):** UI-слой может не отправлять `CommitWordChanges`, если `edited == value` (effect лишний). Если всё же отправил — reducer выполнит ту же мутацию (`value = edited`, `isEditMode = false`, `edited = ""`), а effect `UpdateWord` с прежним значением идемпотентен на стороне БД. Дополнительная ветвь в reducer'е не вводится.
- **Альтернатива «пустой Commit ⇒ delete word»** отвергается: семантически удаление слова идёт через `RemoveWord` + диалог, не через editor. Пустой Commit — программная ошибка UI; reducer его молча игнорирует (shortcut-ignore).

### Lexeme (создание/удаление целиком)

#### `CreateLexeme`
- **Что:** пользователь создаёт новую пустую лексему через FAB. **Полностью локальная мутация** — в БД ничего не пишется (мини-патч ит.6: лексема создаётся в БД только при первом Commit Translation/Definition).
- **Trigger:** тап на FAB `9154-82532` («Добавить значение»).
- **State changes (через `when`):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded ->
          // Guard: уже есть локальная неподтверждённая лексема — игнорируем повторный тап
          if (state.lexemeList.any { it.id == NOT_IN_DB }) state to emptySet()
          else {
              val closed = state.closeAllEditModes()
              closed.copy(
                  lexemeList = closed.lexemeList + LexemeState(
                      id = NOT_IN_DB,
                      translation = null,
                      definition = null,
                      isMenuOpen = false,
                  )
              ) to emptySet()  // ← НИКАКОГО EFFECT
          }
      WordState.NotLoaded -> state to emptySet()
  }
  ```
  `closeAllEditModes()` гасит зомби-фокус IME перед добавлением новой карточки. Сам `wordState` не трогаем (он остаётся `Loaded`). Append идёт по `closed.lexemeList` (правило F012). Computed `state.isCreatingLexeme` после этой мутации становится `true` (производно от `lexemeList.any { it.id == NOT_IN_DB }` — см. contract_state v2.4).
- **Effects:** **нет.** Лексема живёт локально с `id = NOT_IN_DB` до первого Commit Translation/Definition. Записи в БД (insert лексемы) делает effect-handler `UpdateLexemeTranslation` / `UpdateLexemeDefinition` при `lexemeId == null` (см. `CommitTranslationEdit` / `CommitDefinitionEdit` ниже).
- **Guard:** `state.wordState !is WordState.Loaded || state.lexemeList.any { it.id == NOT_IN_DB }` ⇒ `state to emptySet()`. Защита от двойного нажатия (через предикат, эквивалентный computed `state.isCreatingLexeme`) + инв. 6 (`isCreatingLexeme ⇒ Loaded`). (Проверка `isLoading` избыточна — следует из инв. 11.)

> 📎 guide: docs/guides/effect-handlers.md — "DatasourceEffect: операции с БД, сетью, preferences; всегда на Dispatchers.IO"

#### `RemoveLexeme(lexemeId)`
- **Что:** пользователь удаляет лексему. **Для локальной (`id == NOT_IN_DB`) — чисто локальное удаление без effect** (мини-патч ит.6); для лексемы с реальным id — обычный effect с pessimistic ожиданием `RefreshLexemeList`.
- **Trigger:** тап «Удалить» в контекстном меню лексемы.
- **State changes (мини-патч ит.6 — ветка для `NOT_IN_DB`; F024 — обязательный сброс `isEdit` у translation/definition удаляемой лексемы для реальной лексемы; F026 — через `when`; F042 — conditional copy для target):**

  Сначала ветка для локальной лексемы (нет в БД):
  ```
  when (val w = state.wordState) {
      is WordState.Loaded -> {
          val target = state.lexemeList.firstOrNull { it.id == msg.lexemeId }
              ?: return state to emptySet()
          if (target.id == NOT_IN_DB) {
              // локальная — удаляем без effect
              state.copy(
                  lexemeList = state.lexemeList.filterNot { it.id == NOT_IN_DB }
              ) to emptySet()
          } else <branch-for-real-id>
      }
      WordState.NotLoaded -> state to emptySet()
  }
  ```

  Ветка `<branch-for-real-id>` — прежняя логика (мини-патч ит.7 — `isPendingDbOp = true`):
  ```
  state.copy(
      lexemeList = state.lexemeList.map { l ->
          if (l.id == msg.lexemeId) {
              val trDirty = l.translation?.let { it.isEdit || it.edited.isNotEmpty() } ?: false
              val dfDirty = l.definition?.let { it.isEdit || it.edited.isNotEmpty() } ?: false
              if (!trDirty && !dfDirty && !l.isMenuOpen) l
              else l.copy(
                  translation = l.translation?.copy(isEdit = false, edited = ""),
                  definition  = l.definition?.copy(isEdit = false, edited = ""),
                  isMenuOpen  = false,
              )
          } else l
      },
      isPendingDbOp = true,
  ) to setOf(DatasourceEffect.RemoveLexeme(wordId = w.id, lexemeId = msg.lexemeId))
  ```
  **Мини-патч ит.7:** ветка шлёт `RemoveLexeme` effect; pending сбрасывается через `RefreshLexemeList` / `LexemeCascadeRemoved` / `ShowNotification`.
  **F042 — conditional copy для target:** если у целевой лексемы все три поля уже в желаемом состоянии (`translation?.isEdit == false ∧ translation?.edited == ""` и аналогично для `definition`, и `isMenuOpen == false`) — `copy()` не вызывается. Симметрично F017/F038. Lexeme всё равно останется в списке до прихода ответа от data-слоя; identity сохраняется для уже-чистой лексемы (типичный сценарий — Remove из контекстного меню при view-mode без активных edit'ов).
  Сброс `translation.isEdit = false, edited = ""` и `definition.isEdit = false, edited = ""` обязателен (F024 — расширение F004): если пользователь активно редактировал перевод или определение этой лексемы и параллельно (через меню) запустил Remove — без сброса `isEdit=true` остался бы у удаляемой лексемы, **нарушая инв. 9** (формально активный edit на удаляемой сущности в счётчике single-edit-mode). Сама лексема остаётся в `lexemeList` до фактического возврата из БД — удаление приходит каноном из `contract_io`. Закрываем меню атомарно, поскольку Msg триггерится из меню; иначе меню зависло бы до прихода `Refresh`-like сообщения от data-слоя.
  **F040 (мини-патч из contract_io ит.4):** effect `RemoveLexeme` теперь несёт `(wordId, lexemeId)` — `Lexeme` entity не имеет `wordId`, поэтому payload передаёт его явно из `(w as WordState.Loaded).id`. UseCase impl делает DAO-delete по `lexemeId` + DAO-query списка по `wordId`.
- **Effects (forward-ref):** `DatasourceEffect.RemoveLexeme(wordId = w.id, lexemeId = msg.lexemeId)`.
- **Guard:** `state.wordState !is WordState.Loaded` ⇒ `state to emptySet()` (инв. 5: непустой `lexemeList` ⇒ Loaded; если `NotLoaded`, в списке физически не может быть искомой лексемы). + общий guard «лексема не найдена» (см. конец секции), реализован явно в pseudocode через `lexemeList.none { ... }`.

> 📎 guide: docs/guides/state-and-extensions.md — "Для модификации элементов в списках используется modifyFiltered из modules/core/tools"

#### `OpenLexemeMenu(lexemeId, isShow)`
- **Что:** пользователь открывает/закрывает контекстное меню конкретной лексемы.
- **Trigger:** тап на иконке «...» в карточке лексемы (open) или dismiss popup'а (close).
- **State changes (F007 — `closeAllEditModes()` при open; F012/F017 — промежуточный val + conditional copy; F015 — guard при close):**
  - Если `isShow == true`:
    ```
    if (state.lexemeList.none { it.id == lexemeId }) state to emptySet()
    else {
        val closed = state.closeAllEditModes()
        closed.copy(
            lexemeList = closed.lexemeList.map { l ->
                if (l.id == lexemeId)        (if (l.isMenuOpen) l else l.copy(isMenuOpen = true))
                else if (l.isMenuOpen)       l.copy(isMenuOpen = false)
                else                          l
            }
        ) to emptySet()
    }
    ```
    **F038 — conditional copy для target:** для целевой лексемы тоже `if (l.isMenuOpen) l else l.copy(isMenuOpen = true)` — если меню уже открыто (повторный Open для одной и той же лексемы), `copy()` не вызывается. Симметрично c F017 (closing untouched lexemes без copy), economy на identity-сохранении для Compose recomposition.
    `closeAllEditModes()` гарантирует инв. 9 — после открытия меню активных edit-mode нет. UX-обоснование: меню обычно открывается из view-mode лексемы; последовательность «редактирую → открыл меню → удалил» неестественна, попап перекрыл бы edit-инпут без явного коммита/cancel и оставил `isEdit=true` за кадром.
    Эксклюзивность: для лексемы с `id == lexemeId` `isMenuOpen = true`; **для всех остальных лексем — `isMenuOpen = false`** (DropdownMenu в Compose — модальный popup; одновременно открытое меню более чем на одной лексеме невозможно).
    **F017 — conditional copy:** `else if (l.isMenuOpen) l.copy(isMenuOpen = false) else l` вместо безусловного `l.copy(isMenuOpen = false)` — лишние `copy()` на уже-закрытых лексемах не нужны (Compose-friendly: identity сохраняется).
    **F012 — почему `closed.lexemeList`, а не `state.lexemeList`:** mapping обязан идти по списку из результата хелпера (`closed`); если читать `state.lexemeList`, результат `closeAllEditModes()` затрётся — в новом списке окажутся исходные `LexemeState` с активным `isEdit/edited`, и инв. 9 будет нарушен. Правило зафиксировано в разделе про `closeAllEditModes()`.
  - Если `isShow == false`:
    ```
    if (state.lexemeList.none { it.id == lexemeId }) state to emptySet()
    else state.copy(
        lexemeList = state.lexemeList.map { l ->
            if (l.id == lexemeId) (if (l.isMenuOpen) l.copy(isMenuOpen = false) else l)
            else l
        }
    ) to emptySet()
    ```
    **F038 — conditional copy для target (симметрично `isShow == true`):** если у целевой лексемы меню уже закрыто, `copy()` не вызывается (повторный Close — идемпотентен без аллокации).
    **F015 — guard «лексема не найдена»** добавлен явно в pseudocode (раньше тянулся неявно через общую сноску). При закрытии меню для несуществующего id (async-расхождение: лексема удалена параллельно) — shortcut-ignore без `copy()`.
    Только сброс `isMenuOpen` у целевой лексемы; `closeAllEditModes()` не нужен (закрытие меню не открывает новый edit, edit-mode флаги ортогональны), промежуточный val тоже не нужен (хелпер не вызывается).
- **Effects:** нет.
- **Почему payload не одно поле** (whitelist (a) — payload = id адресации + дискриминатор операции).
- **Локальный инвариант reducer'а (F028):** `forall l1, l2 ∈ lexemeList, l1 != l2: !(l1.isMenuOpen && l2.isMenuOpen)`. Это не структурный инвариант `contract_state` (его форма не покрывается общим списком инвариантов state); reducer **сохраняет** этот инвариант из определённого стартового state (дефолт `WordCardState()`, в котором у всех лексем `isMenuOpen = false`), полагаясь на то что входящий state уже удовлетворяет инварианту. Каждая reducer-ветвь, которая ставит `isMenuOpen = true` (только `OpenLexemeMenu(isShow=true)`), одновременно гасит `isMenuOpen` у всех остальных лексем — сохранение инварианта.
- **Guard:** общий «лексема не найдена».

### Translation chip (внутри лексемы)

#### `CreateTranslation(lexemeId)`
- **Что:** пользователь нажимает chip «Перевод» внутри карточки лексемы, у которой ещё нет перевода.
- **Trigger:** тап на chip «Перевод» (frame `9154-82625`) в `LexemeState.translation == null`.
- **State changes (F012 — промежуточный val после `closeAllEditModes()`):**
  ```
  val target = state.lexemeList.firstOrNull { it.id == lexemeId }
  if (target == null || target.translation != null) state to emptySet()
  else {
      val closed = state.closeAllEditModes()
      closed.copy(
          lexemeList = closed.lexemeList.map { l ->
              if (l.id == lexemeId)
                  l.copy(translation = TextValueState(origin = "", edited = "", isEdit = true))
              else l
          }
      ) to emptySet()
  }
  ```
  **`isEdit = true` обязан быть выставлен явно** — дефолт `TextValueState.isEdit` теперь `false` (см. contract_state v2.3 § `TextValueState`). Mapping идёт по `closed.lexemeList` (результат хелпера), не по `state.lexemeList` — иначе сброшенные `isEdit/edited` других лексем затрутся обратно (см. правило в разделе про `closeAllEditModes()`).
- **Effects:** нет.
  > **Reducer не шлёт effect (F021):** создание `TextValueState` — чисто локальная мутация state. В БД ничего не пишется. Запись произойдёт только при `CommitTranslationEdit` с непустым `edited` (через `UpdateLexemeTranslation` effect). Из этого следует: `CancelTranslationEdit` при `origin == ""` безопасно делает локальный `translation = null` без рассинхрона с БД (см. ремарку в `CancelTranslationEdit`).
- **Guard:** `state.lexemeList.none { it.id == lexemeId }` или у найденной `lexeme.translation != null` ⇒ `state to emptySet()` — повторный «Create» при уже существующем переводе бессмыслен.

> 📎 guide: docs/guides/state-modeling.md — "T? = count(T) + 1: nullable семантика — может быть null на момент создания state или в течение flow"

#### `UpdateTranslationInput(lexemeId, value)`
- **Что:** пользователь вводит текст перевода.
- **Trigger:** `onValueChange` в inline-`TextField` перевода.
- **State changes:** для лексемы с `id == lexemeId` — `translation.copy(edited = value)`.
- **Effects:** нет.
- **Guard:** `lexeme.translation == null || !lexeme.translation.isEdit` ⇒ `state to emptySet()` — Update без открытого edit-mode нарушит инв. 4.

#### `EnterTranslationEditMode(lexemeId)`
- **Что:** пользователь повторно входит в режим редактирования существующего перевода.
- **Trigger:** тап по значению перевода в view-mode (chip + текст уже есть).
- **State changes (F012 — промежуточный val; F025 — smart-cast через `let`):**
  ```
  val target = state.lexemeList.firstOrNull { it.id == lexemeId }
  if (target?.translation == null) state to emptySet()
  else {
      val closed = state.closeAllEditModes()
      closed.copy(
          lexemeList = closed.lexemeList.map { l ->
              if (l.id == lexemeId)
                  l.translation?.let { tr -> l.copy(translation = tr.copy(isEdit = true, edited = tr.origin)) }
                      ?: l
              else l
          }
      ) to emptySet()
  }
  ```
  Инициализируем буфер last known good значением (`origin`). Mapping по `closed.lexemeList` — после хелпера `l.translation` у целевой лексемы имеет `isEdit = false, edited = ""`, поэтому `l.translation?.let { tr -> ... tr.origin }` корректно поднимет edit-mode именно у целевой и сохранит закрытие у остальных. **F025 — `let`-форма:** smart-cast non-null через `?.let { tr -> ... }` чище двойного `?.copy(..., edited = l.translation.origin)` (Kotlin не делает smart-cast на mutable свойство класса по второму чтению, только через локальный val или `let`).
- **Effects:** нет.
- **Guard:** `lexeme.translation == null` ⇒ `state to emptySet()`. (Идемпотентность повторного Enter не критична — `closeAllEditModes()` + явное `isEdit=true, edited=origin` корректно перезатрёт само себя; инвариант 9 удерживается.)

#### `CommitTranslationEdit(lexemeId)`
- **Что:** пользователь коммитит изменения перевода.
- **Trigger:** тап вне поля / IME Done / тап на chip-toggle подтверждения.
- **State changes + Effects (F040 — единый внешний `when` вокруг guard'а и всех ветвей; F013 — explicit smart-cast):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded -> {
          val lexeme = state.lexemeList.find { it.id == lexemeId } ?: return state to emptySet()
          if (lexeme.translation == null || !lexeme.translation.isEdit) state to emptySet()
          else when {
              // ветвь 1a (F034) — свежесозданный без ввода
              lexeme.translation.edited.isBlank() && lexeme.translation.origin.isEmpty() ->
                  state.copy(
                      lexemeList = state.lexemeList.map { l ->
                          if (l.id == lexemeId) l.copy(translation = null) else l
                      }
                  ) to emptySet()
              // ветвь 1 — Remove (pessimistic, F002; ит.7 — isPendingDbOp = true)
              lexeme.translation.edited.isBlank() ->
                  state.copy(
                      lexemeList = state.lexemeList.map { l ->
                          if (l.id == lexemeId)
                              l.copy(translation = l.translation?.copy(isEdit = false, edited = ""))
                          else l
                      },
                      isPendingDbOp = true,
                  ) to setOf(DatasourceEffect.RemoveTranslation(lexemeId))
              // ветвь 2 — no-op (edited == origin)
              lexeme.translation.edited == lexeme.translation.origin ->
                  state.copy(
                      lexemeList = state.lexemeList.map { l ->
                          if (l.id == lexemeId)
                              l.copy(translation = l.translation?.copy(isEdit = false, edited = ""))
                          else l
                      }
                  ) to emptySet()
              // ветвь 3 — Update (мини-патч ит.6 — nullable lexemeId; ит.7 — isPendingDbOp = true)
              else -> {
                  val newValue = lexeme.translation.edited
                  val effectLexemeId = if (lexeme.id == NOT_IN_DB) null else lexeme.id
                  state.copy(
                      lexemeList = state.lexemeList.map { l ->
                          if (l.id == lexemeId)
                              l.copy(translation = l.translation?.copy(isEdit = false, edited = ""))
                          else l
                      },
                      isPendingDbOp = true,
                  ) to setOf(
                      DatasourceEffect.UpdateLexemeTranslation(
                          wordId = w.id,
                          lexemeId = effectLexemeId,
                          translation = newValue,
                      )
                  )
              }
          }
      }
      WordState.NotLoaded -> state to emptySet()
  }
  ```
- **Guard (общий для всех ветвей):** `state.wordState !is WordState.Loaded || lexeme.translation == null || !lexeme.translation.isEdit` ⇒ `state to emptySet()`. Проверка `wordState is Loaded` нужна для конструирования effect в ветви 3 (snapshot `wordId`); explicit smart-cast через единый внешний `when` (F040 — не «отдельный guard-`when` + отдельный ветвящий-`when`», а единая структура; компилятор видит smart-cast `w: Loaded` в ветви 3 без `as`-каста, F013 удерживается).
- **Мини-патч ит.6 — `lexemeId` для effect Update (ветвь 3):** `DatasourceEffect.UpdateLexemeTranslation.lexemeId` — **nullable Long**. Если `lexeme.id == NOT_IN_DB` (свежесозданная локально через FAB лексема, ещё не записанная в БД) — в effect передаётся `lexemeId = null`. Effect-handler знает: при `lexemeId == null` ⇒ сначала insert лексемы (получить реальный id), затем insert translation; в обоих случаях handler возвращает `RefreshTranslation(lexemeId = <реальный>, translation = newValue)` с реальным id. Если `lexeme.id > 0` — обычная ветка update (handler делает upsert/update translation по существующему `lexemeId`).
- **Мини-патч ит.6 — ветвь 1 для NOT_IN_DB невозможна:** для лексемы с `id == NOT_IN_DB` `translation.origin` всегда `""` (не было первого Commit — БД-запись ещё не существует, `Refresh*` с реальным origin никогда не приходил). Поэтому при `edited.isBlank()` для NOT_IN_DB всегда срабатывает **ветвь 1a** (`edited.isBlank() ∧ origin.isEmpty()`) — локальный nullify без effect. Ветвь 1 (Remove с pessimistic effect `RemoveTranslation(lexemeId)`) для NOT_IN_DB не достигается. Если каким-то багом state окажется в виде `(id == NOT_IN_DB, translation.origin != "")` — `RemoveTranslation` effect улетит с `lexemeId = NOT_IN_DB`, handler по конвенции `> -1` это отсеет (см. `DatasourceEffectHandler.kt`); но это аномалия. Дополнительный guard в reducer'е не нужен — ветвь 1a поглощает корректный случай, аномалия ловится handler'ом.
- **Ветвление по `edited` (4 ветви — F035 `isBlank()` вместо `isEmpty()` во всех empty-проверках):**
  1. **1a — `lexeme.translation.edited.isBlank() ∧ lexeme.translation.origin.isEmpty()`** (F034 — свежесозданный без ввода):
     - **Reducer:** для лексемы `translation = null` — локальный nullify без effect.
     - **Effect:** `emptySet()` — `CreateTranslation` не шлёт effect (F021), записи в БД нет, удалять нечего. Симметрично `CancelTranslationEdit` с `origin.isEmpty()`.
     - **Почему отдельная ветка:** без неё ветвь 1 (Remove) шлёт `RemoveTranslation` effect на несуществующую в БД запись — лишняя ошибка handler'а.
  2. **`lexeme.translation.edited.isBlank()`** (F002 — pessimistic, симметрично `RemoveTranslation`; **F035 — `isBlank()` вместо `isEmpty()`:** whitespace-only ввод считается пустым, отсекается тем же путём что и нулевая длина):
     - **Reducer:** для лексемы с `id == lexemeId` `translation = translation.copy(isEdit = false, edited = "")`. **`translation` не обнуляем** — фактическое отсутствие придёт через `RefreshTranslation(lexemeId, translation = null)` от data-слоя.
     - **Effect:** `DatasourceEffect.RemoveTranslation(lexemeId)`.
     - **Финал:** `RefreshTranslation(lexemeId, translation = null)` от handler'а — обнуляет `translation` (каноном в `contract_io`).
     - **Transient-окно:** между Commit-empty и Refresh `translation` остаётся non-null с `isEdit=false, edited="", origin=<старое>`. UI рендерит `toValue(isEdit=false)` ⇒ **показывает старое значение перевода (origin) до прихода Refresh**. Это легитимное transient-окно (политика «UI кратко покажет старый origin до прихода Refresh»). Симметрично `RemoveTranslation` из меню.
     - **Почему pessimistic, а не optimistic:** была две дороги к одному финалу (`CommitTranslationEdit` ветвь 1 раньше оптимистично nullify'ила `translation` в reducer'е, тогда как `RemoveTranslation` ждал Refresh). Унифицируем — обе ведут через effect → Refresh. Преимущество: при failure `RemoveTranslation` в БД UI не разъезжается со state (БД вернёт ошибку, Refresh не придёт с null, перевод останется виден).
  3. **`lexeme.translation.edited == lexeme.translation.origin`** ⇒ `translation = translation.copy(isEdit = false, edited = "")`, **никакого effect** (no-op коммит — пользователь вышел из edit без изменений, БД трогать не за чем). Сброс `edited = ""` обязателен — инв. 4.
  4. **Иначе** ⇒ для лексемы `translation = translation.copy(isEdit = false, edited = "")` + `DatasourceEffect.UpdateLexemeTranslation(wordId = w.id, lexemeId, translation = newValue)` — где `w` — внешняя `when`-смарт-кастнутая переменная (F013/F040), а `newValue = lexeme.translation.edited` снят в локальную переменную ДО сброса `edited`. `origin` не трогаем — синхронизируется через `RefreshTranslation` от data-слоя.
- **Замечание про инв. 4 в ветви 4:** после сброса `isEdit = false ∧ edited = ""` инв. 4 выполнен, при этом `origin` может ещё некоторое время отличаться от того, что в БД, до прихода `RefreshTranslation`. Это допустимо — `origin` не участвует в инв. 4. Альтернатива «писать `origin = edited` сразу в reducer'е» отвергается: reducer не знает, прошёл ли write в БД (failure → рассинхрон).

> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 5: Динамические эффекты — когда эффекты зависят от содержимого сообщения"
>
> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 4: Условный стейт — когда логика ветвится по данным сообщения"

#### `CancelTranslationEdit(lexemeId)`
- **Что:** пользователь отменяет редактирование перевода без коммита.
- **Trigger** (концептуальный набор, финальный выбор — в UI sub-flow):
  1. Системный back-жест внутри активного `TextField` (Compose IME back).
  2. Esc на физической клавиатуре (если поддерживается).
  3. Тап «Отмена» внутри chip в edit-mode (если такой control появится в макете).

  *Тап-вне-поля* и *IME Done* — **не** триггерят Cancel; они идут в `CommitTranslationEdit` (и при `edited == origin` или `edited.isBlank()` ветвление reducer'а сделает no-op / Remove / nullify соответственно — см. F035/F034).
- **State changes:**
  - Если `lexeme.translation.origin.isEmpty()` (свежесозданный перевод — пользователь нажал Cancel сразу после Create) ⇒ для лексемы с `id == lexemeId` `translation = null`. Симметрично политике «пустой Commit ⇒ Remove» и удерживает 2 наблюдаемых режима nullable: либо `null`, либо `TextValueState(origin != "", ...)`.
  - Иначе ⇒ `translation = translation.copy(isEdit = false, edited = "")` (инв. 4 — `edited` обнуляется при выходе из edit; `origin` остаётся как last known good).
- **Effects:** нет — отмена не идёт в БД.
- **Guard:** `lexeme.translation == null || !lexeme.translation.isEdit` ⇒ `state to emptySet()`.
- **F014 — Почему локальный nullify, а не pessimistic как в `CommitTranslationEdit` ветви 1:** Cancel при `origin == ""` означает «свежесозданный `TextValueState` через `CreateTranslation` без записи в БД» — `CreateTranslation` не шлёт effect (см. F021), `translation` в БД не существует. Effect-handler не дёргается, рассинхрона с БД не возникает. Симметрии с F002 (pessimistic при наличии effect `RemoveTranslation` → `RefreshTranslation(null)`) не требуется: там pessimistic нужен, потому что effect может зафейлить и БД останется с записью, а здесь effect'а нет в принципе.
- **F037 — Cross-contract assumption:** корректность ветки `origin.isEmpty() ⇒ translation = null` опирается на инвариант со стороны data-слоя: `contract_io` гарантирует, что `RefreshTranslation` никогда не возвращает `TextValueState(origin = "")`. Из pessimistic-политики `Remove*` (F002): БД не хранит пустых строк — `RemoveTranslation`-effect возвращает `RefreshTranslation(lexemeId, translation = null)`, а `UpdateLexemeTranslation` принимает только непустой `edited`. Следовательно комбинация `(translation != null, isEdit = false, origin = "")` возможна **только** как результат `CreateTranslation` без последующего `Commit*Edit` — то есть исключительно «свежесозданный без записи в БД». Это делает `translation = null` в Cancel-ветке безопасным: лексема ни разу не имела перевода в БД, локальный nullify не создаёт рассинхрона. Симметрично для definition.

> 📎 guide: docs/guides/state-modeling.md — "Считаем варианты State: помогает найти лишние/недостающие комбинации; исключать невалидные состояния на уровне типов"

#### `RemoveTranslation(lexemeId)`
- **Что:** пользователь удаляет перевод у лексемы (явный пункт, не через пустой Commit). **Для локальной (`id == NOT_IN_DB`) — локальный nullify; если у локальной лексемы удалена последняя суб-сущность — лексема удаляется из state целиком** (мини-патч ит.6, локальный аналог cascade).
- **Trigger:** тап «Удалить перевод» в контекстном меню лексемы (Msg рождается из меню — это важно для сброса `isMenuOpen`).
- **State changes (мини-патч ит.6 — ветка `NOT_IN_DB`; F004 — обязательный сброс `isEdit`; F027 — shortcut-guard `translation == null`):**
  ```
  val target = state.lexemeList.firstOrNull { it.id == lexemeId }
  if (target == null || target.translation == null) state to emptySet()
  else if (target.id == NOT_IN_DB) {
      // мини-патч ит.6 — локальный nullify, БД не трогаем
      val updated = target.copy(translation = null, isMenuOpen = false)
      if (updated.translation == null && updated.definition == null) {
          // локальная cascade: удалена последняя суб-сущность ⇒ лексема исчезает
          state.copy(
              lexemeList = state.lexemeList.filterNot { it.id == NOT_IN_DB }
          ) to emptySet()
      } else {
          state.copy(
              lexemeList = state.lexemeList.map { if (it.id == NOT_IN_DB) updated else it }
          ) to emptySet()
      }
  }
  else state.copy(
      lexemeList = state.lexemeList.map { l ->
          if (l.id == lexemeId)
              l.copy(
                  translation = l.translation?.copy(isEdit = false, edited = ""),
                  isMenuOpen  = false,
              )
          else l
      },
      isPendingDbOp = true,
  ) to setOf(DatasourceEffect.RemoveTranslation(lexemeId))
  ```
  **Мини-патч ит.7 — `isPendingDbOp = true`:** ветка real-id шлёт `RemoveTranslation` effect; сброс через `RefreshTranslation` / `LexemeCascadeRemoved` / `ShowNotification`. Для NOT_IN_DB ветки pending не выставляется (нет effect).
  **F027 — shortcut-guard `translation == null`:** если перевода у лексемы уже нет (async-расхождение: меню задержалось, перевод параллельно удалён через `RefreshTranslation(null)`), shortcut-ignore без `copy()` и без effect'а. Эффект `RemoveTranslation` для несуществующего перевода — лишняя нагрузка на БД и потенциальная ошибка handler'а.
  Сброс `translation.isEdit = false, edited = ""` обязателен: если пользователь активно редактировал перевод и одновременно (через меню) запустил Remove — без сброса `isEdit=true` остался бы у удаляемого поля, **нарушая инв. 9** (формально активный edit на удаляемом поле в счётчике single-edit-mode). Само `translation` пока не обнуляем — фактическое отсутствие придёт через `RefreshTranslation(lexemeId, translation=null)` от data-слоя (pessimistic, каноном в `contract_io`).
  **Мини-патч ит.6 — локальная cascade:** для лексемы с `id == NOT_IN_DB` `translation` обнуляем сразу (БД-записи нет, transient-окно с показом старого origin не нужно — origin всегда `""`). Если после этого у локальной лексемы оба поля `null` — удаляем лексему целиком из `lexemeList` (симметрия с серверным cascade `LexemeCascadeRemoved`, который data-слой делает для лексем в БД).
- **Effects (forward-ref):** `DatasourceEffect.RemoveTranslation(lexemeId)` — **только** для лексемы с реальным id (`> 0`). Для `NOT_IN_DB` — `emptySet()`.
- **Guard:** общий «лексема не найдена» + **F027 — `lexeme.translation == null` ⇒ `state to emptySet()`**.

### Definition chip (внутри лексемы)

Структурно симметрично Translation.

#### `CreateDefinition(lexemeId)`
- **Что / Trigger:** тап chip «Определение» (frame `9154-82625`) в `LexemeState.definition == null`.
- **State changes (F012 — промежуточный val):**
  ```
  val target = state.lexemeList.firstOrNull { it.id == lexemeId }
  if (target == null || target.definition != null) state to emptySet()
  else {
      val closed = state.closeAllEditModes()
      closed.copy(
          lexemeList = closed.lexemeList.map { l ->
              if (l.id == lexemeId)
                  l.copy(definition = TextValueState(origin = "", edited = "", isEdit = true))
              else l
          }
      ) to emptySet()
  }
  ```
  **`isEdit = true` обязан быть выставлен явно** (дефолт теперь `false`). Mapping по `closed.lexemeList`, не по `state.lexemeList`.
- **Effects:** нет.
  > **Reducer не шлёт effect (F021):** симметрично `CreateTranslation` — создание `TextValueState` локально, в БД ничего не пишется до `CommitDefinitionEdit` с непустым `edited`. Из этого следует: `CancelDefinitionEdit` при `origin == ""` безопасно делает локальный `definition = null` без рассинхрона с БД.
- **Guard:** `lexeme.definition != null` ⇒ `state to emptySet()`.

#### `UpdateDefinitionInput(lexemeId, value)`
- **Что / Trigger:** ввод в `TextField` определения.
- **State changes:** для лексемы с `id == lexemeId` — `definition.copy(edited = value)`.
- **Effects:** нет.
- **Guard:** `lexeme.definition == null || !lexeme.definition.isEdit` ⇒ `state to emptySet()`.

#### `EnterDefinitionEditMode(lexemeId)`
- **Что / Trigger:** повторный вход в edit существующего определения.
- **State changes (F012 — промежуточный val; F025 — smart-cast через `let`):**
  ```
  val target = state.lexemeList.firstOrNull { it.id == lexemeId }
  if (target?.definition == null) state to emptySet()
  else {
      val closed = state.closeAllEditModes()
      closed.copy(
          lexemeList = closed.lexemeList.map { l ->
              if (l.id == lexemeId)
                  l.definition?.let { df -> l.copy(definition = df.copy(isEdit = true, edited = df.origin)) }
                      ?: l
              else l
          }
      ) to emptySet()
  }
  ```
  Mapping по `closed.lexemeList`, `let`-форма — симметрично `EnterTranslationEditMode`.
- **Effects:** нет.
- **Guard:** `lexeme.definition == null` ⇒ `state to emptySet()`.

#### `CommitDefinitionEdit(lexemeId)`
- **Что / Trigger:** коммит определения.
- **State changes + Effects (F040 — единый внешний `when`, F034 / F035 — симметрично `CommitTranslationEdit`):**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded -> {
          val lexeme = state.lexemeList.find { it.id == lexemeId } ?: return state to emptySet()
          if (lexeme.definition == null || !lexeme.definition.isEdit) state to emptySet()
          else when {
              lexeme.definition.edited.isBlank() && lexeme.definition.origin.isEmpty() ->
                  state.copy(
                      lexemeList = state.lexemeList.map { l ->
                          if (l.id == lexemeId) l.copy(definition = null) else l
                      }
                  ) to emptySet()
              // ветвь 1 — Remove (pessimistic, F002; ит.7 — isPendingDbOp = true)
              lexeme.definition.edited.isBlank() ->
                  state.copy(
                      lexemeList = state.lexemeList.map { l ->
                          if (l.id == lexemeId)
                              l.copy(definition = l.definition?.copy(isEdit = false, edited = ""))
                          else l
                      },
                      isPendingDbOp = true,
                  ) to setOf(DatasourceEffect.RemoveDefinition(lexemeId))
              lexeme.definition.edited == lexeme.definition.origin ->
                  state.copy(
                      lexemeList = state.lexemeList.map { l ->
                          if (l.id == lexemeId)
                              l.copy(definition = l.definition?.copy(isEdit = false, edited = ""))
                          else l
                      }
                  ) to emptySet()
              // ветвь 3 — Update (мини-патч ит.6 — nullable lexemeId; ит.7 — isPendingDbOp = true)
              else -> {
                  val newValue = lexeme.definition.edited
                  val effectLexemeId = if (lexeme.id == NOT_IN_DB) null else lexeme.id
                  state.copy(
                      lexemeList = state.lexemeList.map { l ->
                          if (l.id == lexemeId)
                              l.copy(definition = l.definition?.copy(isEdit = false, edited = ""))
                          else l
                      },
                      isPendingDbOp = true,
                  ) to setOf(
                      DatasourceEffect.UpdateLexemeDefinition(
                          wordId = w.id,
                          lexemeId = effectLexemeId,
                          definition = newValue,
                      )
                  )
              }
          }
      }
      WordState.NotLoaded -> state to emptySet()
  }
  ```
- **Guard (общий):** `state.wordState !is WordState.Loaded || lexeme.definition == null || !lexeme.definition.isEdit` ⇒ `state to emptySet()` — реализован веткой `NotLoaded` и внутренней `if` (F040).
- **Мини-патч ит.6 — `lexemeId` для effect Update (ветвь 3):** симметрично `CommitTranslationEdit` — `DatasourceEffect.UpdateLexemeDefinition.lexemeId` nullable. Для `lexeme.id == NOT_IN_DB` ⇒ `lexemeId = null`, handler делает insert лексемы + insert definition, возвращает `RefreshDefinition(lexemeId = <реальный>, definition = newValue)`. Для `lexeme.id > 0` ⇒ обычный update.
- **Мини-патч ит.6 — ветвь 1 для NOT_IN_DB невозможна:** симметрично `CommitTranslationEdit`. Для лексемы с `id == NOT_IN_DB` `definition.origin` всегда `""`, потому при `edited.isBlank()` срабатывает ветвь 1a (локальный nullify без effect). Ветвь 1 (Remove pessimistic) для NOT_IN_DB не достигается.
- **Ветвление по `edited` (4 ветви — симметрично `CommitTranslationEdit`, F035 `isBlank()` во всех empty-проверках):**
  1. **1a — `lexeme.definition.edited.isBlank() ∧ lexeme.definition.origin.isEmpty()`** (F034) ⇒ `definition = null`, effect `emptySet()`. Свежесозданный через `CreateDefinition` без ввода — записи в БД нет (F021), локальный nullify симметрично `CancelDefinitionEdit`.
  2. **`lexeme.definition.edited.isBlank()`** (F002 — pessimistic, F035):
     - **Reducer:** для лексемы `definition = definition.copy(isEdit = false, edited = "")` (НЕ nullify).
     - **Effect:** `DatasourceEffect.RemoveDefinition(lexemeId)`.
     - **Финал:** `RefreshDefinition(lexemeId, definition = null)` от handler'а обнуляет `definition`.
     - **Transient-окно:** UI кратко покажет старое значение определения (`origin`) до прихода Refresh — легитимно.
  3. `lexeme.definition.edited == lexeme.definition.origin` ⇒ `definition = definition.copy(isEdit = false, edited = "")`, effect = `emptySet()`.
  4. Иначе ⇒ для лексемы `definition = definition.copy(isEdit = false, edited = "")` + `DatasourceEffect.UpdateLexemeDefinition(wordId = w.id, lexemeId, definition = newValue)` — где `w` — внешняя `when`-смарт-кастнутая переменная (F013/F040), а `newValue = lexeme.definition.edited` снят ДО сброса `edited`.

#### `CancelDefinitionEdit(lexemeId)`
- **Что / Trigger:** отмена редактирования определения без коммита; trigger-набор симметричен `CancelTranslationEdit`.
- **State changes:**
  - `lexeme.definition.origin.isEmpty()` ⇒ для лексемы `definition = null`.
  - Иначе ⇒ `definition = definition.copy(isEdit = false, edited = "")`.
- **Effects:** нет.
- **Guard:** `lexeme.definition == null || !lexeme.definition.isEdit` ⇒ `state to emptySet()`.
- **F014 — Почему локальный nullify, а не pessimistic как в `CommitDefinitionEdit` ветви 1:** симметрично `CancelTranslationEdit` — Cancel при `origin == ""` означает «свежесозданный через `CreateDefinition` без записи в БД» (F021 — `Create*` не шлёт effect). Рассинхрона с БД невозможен в принципе; симметрии с F002 (pessimistic при наличии `RemoveDefinition` effect) не требуется.
- **F037 — Cross-contract assumption (симметрично `CancelTranslationEdit`):** `contract_io` гарантирует что `RefreshDefinition` никогда не возвращает `TextValueState(origin = "")`. Комбинация `(definition != null, isEdit = false, origin = "")` возникает только из `CreateDefinition` без последующего `Commit*Edit` — локальный nullify в Cancel-ветке безопасен.

#### `RemoveDefinition(lexemeId)`
- **Что / Trigger:** удаление определения у лексемы (через контекстное меню лексемы). **Симметрично `RemoveTranslation`** — для `NOT_IN_DB` локальный nullify + локальная cascade.
- **State changes (мини-патч ит.6 — ветка `NOT_IN_DB`; F004 — обязательный сброс `isEdit`; F027 — shortcut-guard `definition == null`):**
  ```
  val target = state.lexemeList.firstOrNull { it.id == lexemeId }
  if (target == null || target.definition == null) state to emptySet()
  else if (target.id == NOT_IN_DB) {
      // мини-патч ит.6 — локальный nullify, БД не трогаем
      val updated = target.copy(definition = null, isMenuOpen = false)
      if (updated.translation == null && updated.definition == null) {
          // локальная cascade
          state.copy(
              lexemeList = state.lexemeList.filterNot { it.id == NOT_IN_DB }
          ) to emptySet()
      } else {
          state.copy(
              lexemeList = state.lexemeList.map { if (it.id == NOT_IN_DB) updated else it }
          ) to emptySet()
      }
  }
  else state.copy(
      lexemeList = state.lexemeList.map { l ->
          if (l.id == lexemeId)
              l.copy(
                  definition = l.definition?.copy(isEdit = false, edited = ""),
                  isMenuOpen = false,
              )
          else l
      },
      isPendingDbOp = true,
  ) to setOf(DatasourceEffect.RemoveDefinition(lexemeId))
  ```
  **Мини-патч ит.7 — `isPendingDbOp = true`:** ветка real-id шлёт effect; для NOT_IN_DB pending не выставляется.
  Симметрично `RemoveTranslation`: если пользователь редактировал определение и параллельно через меню запустил Remove — без сброса `definition.isEdit = false` инв. 9 нарушен. `definition` не обнуляем — придёт через `RefreshDefinition`. **F027 — shortcut-guard `definition == null`** симметрично `RemoveTranslation` — async-расхождение когда определения уже нет, ignore без effect'а. **Мини-патч ит.6 — локальная cascade** для `NOT_IN_DB` симметрично `RemoveTranslation`.
- **Effects (forward-ref):** `DatasourceEffect.RemoveDefinition(lexemeId)` — **только** для лексемы с реальным id (`> 0`). Для `NOT_IN_DB` — `emptySet()`.
- **Guard:** общий «лексема не найдена» + **F027 — `lexeme.definition == null` ⇒ `state to emptySet()`**.

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
- **State changes:** `snackbarState = state.snackbarState.copy(show = false)`. `title` сохраняется как есть — инв. 8 требует `show=true ⇒ title != ""`, но `show=false ⇒ ?` не запрещён. Перезаписывать `title=""` нет необходимости.
- **Effects:** нет.
- **Guard (F011):** `!state.snackbarState.show` ⇒ `state to emptySet()`. Явный shortcut-ignore при повторном Dismiss (например swipe + автотаймаут пришли подряд) — экономит лишний `copy()` и делает no-op наблюдаемым.
- **Почему отдельный Msg, а не `UiMsg.ShowNotification(show=false)`:**
  - `ShowNotification(text)` — Datasource Msg (effect-handler шлёт после успеха/ошибки в БД), каноном в `contract_io`. Это не действие пользователя — выходит из scope данного артефакта.
  - `DismissNotification` — UI Msg (пользовательский dismiss / автоматический таймаут со стороны UI), его место здесь.
  - Старый `UiMsg.ShowNotification(text, show)` склеивал два направления потока через payload-дискриминатор. Раздельная декомпозиция чище по scope.
  - Sub-interface `UiMsg : Msg` снимается как избыточная вертикаль: единственный его потребитель (`ShowNotification`) ушёл в `contract_io`, остаётся один `DismissNotification` — прямого члена `sealed interface Msg` достаточно.

> 📎 guide: docs/guides/messages.md — "Внутренние сообщения (UiMsg) — генерируемые UI-эффект-хендлерами, маркируются internal"
>
> 📎 guide: docs/guides/ui-patterns.md — "Показ snackbar — UI-эффект через флаг в state допустим для toast/snackbar"

### Datasource Msg reducer-логика — сброс `isPendingDbOp` (мини-патч ит.7)

> Это **Datasource Msg** (декларация и сигнатуры — в `contract_io`). Здесь фиксируется reducer-побочка: каждая Datasource Msg, завершающая pending DB-операцию, обязана сбросить `isPendingDbOp = false` в той же мутации.

| Datasource Msg | Сброс `isPendingDbOp` | Семантический финал |
|---|---|---|
| `WordLoaded(term)` | `false` | initial load завершён (success) |
| `WordNotFound` | `false` | initial load завершён (404 / failure) |
| `RefreshWord(term)` | `false` | `UpdateWord` подтверждён |
| `RefreshTranslation(lexemeId, translation?)` | `false` | `UpdateLexemeTranslation` / `RemoveTranslation` подтверждён (включая ветку замены `NOT_IN_DB → реальный id` — см. ниже) |
| `RefreshDefinition(lexemeId, definition?)` | `false` | симметрично `RefreshTranslation` |
| `RefreshLexemeList(lexemes)` | `false` | `RemoveLexeme` подтверждён |
| `LexemeCascadeRemoved(lexemeId)` | `false` | cascade-удаление лексемы завершено |
| `NavigateBack` | `false` | successful `RemoveWord` финализирован (effect улетел навигацией) |
| `ShowNotification(text)` | `false` | failure-путь завершает pending (effect-handler вернул error → snackbar) |

**Правило:** все ветки reducer'а для Datasource Msg, перечисленных в таблице, обязаны включать `isPendingDbOp = false` в результирующий `state.copy(...)`. Конкретные мутации (`wordState = Loaded(...)`, `lexemeList = ...` и т.д.) — каноном в `contract_io` / здесь в разделе ниже про замену `NOT_IN_DB`. Сброс — единообразный побочный эффект reducer-логики, не зависит от ветви.

**Почему `ShowNotification` тоже сбрасывает:** failure-путь любого effect (Update/Remove* в БД зафейлил) идёт через `ShowNotification(text)` от effect-handler'а. Reducer не различает success/failure source — он видит только, что pending-окно завершено. Без сброса в failure-ветке UI остался бы заблокирован навсегда.

### Datasource Msg reducer-логика — замена NOT_IN_DB (мини-патч ит.6)

> Это **Datasource Msg** (декларация и сигнатуры — в `contract_io`). Здесь фиксируется только **reducer-логика** замены `NOT_IN_DB → реальный id`, которая критична для завершения «первого Commit» лексемы.

#### `RefreshTranslation(lexemeId: Long, translation: String?)`
- **lexemeId всегда реальный** — после успешного `UpdateLexemeTranslation` effect-handler знает реальный id лексемы в БД (после insert новой лексемы при `lexemeId == null` в effect-payload, или для уже существующей лексемы — её исходный id).
- **Reducer-логика (мини-патч ит.7 — `isPendingDbOp = false`):**
  ```
  state.copy(
      lexemeList = state.lexemeList.map { l ->
          when {
              // обычная синхронизация origin для лексемы с реальным id (F073 ит.7 — сохраняем активный edit)
              l.id == lexemeId -> l.copy(
                  translation = when {
                      translation == null -> null
                      l.translation == null -> TextValueState(origin = translation, isEdit = false, edited = "")
                      else -> l.translation.copy(origin = translation)  // isEdit/edited сохраняем — Refresh не закрывает активный edit
                  }
              )
              // завершение «первого Commit»: NOT_IN_DB ⇒ реальный id
              l.id == NOT_IN_DB -> l.copy(
                  id = lexemeId,
                  translation = translation?.let {
                      TextValueState(origin = it, isEdit = false, edited = "")
                  } ?: l.translation,
              )
              else -> l
          }
      },
      isPendingDbOp = false,
  )
  ```
- **Семантика:** если в state есть лексема с `id == lexemeId` (реальная, уже была в БД до Update) — обычная синхронизация `origin`. Если её нет, но есть лексема с `id == NOT_IN_DB` — handler только что сделал insert этой лексемы вместе с translation; reducer заменяет `NOT_IN_DB` на `lexemeId` из payload. Условие веток в `when` гарантирует, что при наличии и реальной лексемы с `lexemeId`, и локальной с `NOT_IN_DB`, обрабатывается только первая (но по инв.2 одновременно может быть максимум одна `NOT_IN_DB`-лексема, и она не имеет реального id — конфликт не возникает).
- **Почему `?: l.translation` в ветке NOT_IN_DB:** если `translation == null` от handler'а (что в этом сценарии невозможно — handler только что записал непустой translation для первого Commit, но защита от теоретического `RefreshTranslation(lexemeId, null)` сразу после insert) — сохраняем текущий локальный `translation` (он содержит `edited` пользователя и `isEdit = false` после Commit-сброса). В обычной ветке `l.id == lexemeId` `translation = null` ⇒ полный nullify (стандартное поведение Remove).

#### `RefreshDefinition(lexemeId: Long, definition: String?)`

Симметрично `RefreshTranslation` (мини-патч ит.7 — `isPendingDbOp = false`):
```
state.copy(
    lexemeList = state.lexemeList.map { l ->
        when {
            // F073 ит.7 — сохраняем активный edit для существующей лексемы
            l.id == lexemeId -> l.copy(
                definition = when {
                    definition == null -> null
                    l.definition == null -> TextValueState(origin = definition, isEdit = false, edited = "")
                    else -> l.definition.copy(origin = definition)  // isEdit/edited сохраняем
                }
            )
            l.id == NOT_IN_DB -> l.copy(
                id = lexemeId,
                definition = definition?.let {
                    TextValueState(origin = it, isEdit = false, edited = "")
                } ?: l.definition,
            )
            else -> l
        }
    },
    isPendingDbOp = false,
)
```

Финальные сигнатуры `RefreshTranslation` / `RefreshDefinition` и точные правила effect-handler'а (когда `lexemeId == null` ⇒ insert лексемы) — каноном в `contract_io`.

### No-op

#### `NoOperation`
- **Что:** пустой Msg.
- **Trigger:** служебный (fallback в effect-handler'е, стартовый Msg в тестах).
- **State changes:** state не меняется.
- **Effects:** нет.

> 📎 guide: docs/guides/messages.md — "Empty Message — no-op; в редьюсере `is Msg.Empty -> state to emptySet()`"

### Общая сноска про «лексема не найдена»

Все Msg с `lexemeId` (`RemoveLexeme`, `OpenLexemeMenu`, `CreateTranslation`, `UpdateTranslationInput`, `EnterTranslationEditMode`, `CommitTranslationEdit`, `CancelTranslationEdit`, `RemoveTranslation`, и парный набор для `Definition`) применяют общий неявный guard: если `state.lexemeList.none { it.id == lexemeId }` ⇒ `state to emptySet()`. Базовая безопасность для async-расхождений (UI отправил Msg, лексема параллельно удалена через `Refresh*`-Msg).

По инв. 5 (`lexemeList.isNotEmpty ⇒ Loaded`): если `lexemeList` непуст, `wordState` гарантированно `Loaded` — проверять `wordState` отдельно не нужно при ненулевом результате поиска. **Исключение:** `CommitTranslationEdit` / `CommitDefinitionEdit` — им нужен `wordId` из `Loaded` для конструирования эффекта; они проверяют `wordState is Loaded` явно (даже если по инв. это следует из существования лексемы — explicit smart-cast для компилятора).

> 📎 guide: docs/guides/state-modeling.md — "Q: когда отдельный Idle state vs шорткат-getter; бизнес-логика игнорирует message при асинхронном рассинхроне"

### Сводная таблица guards

Все — shortcut-ignore (`state to emptySet()`). Общая сноска «лексема не найдена» применяется ко всем адресным Msg в дополнение к перечисленному.

> **Мини-патч ит.7 — глобальный guard `isPendingDbOp`:** ко всем Msg в таблице ниже (кроме `DismissNotification`, `CloseTopBarMenu`, `CloseDeleteWordDialog`, `CancelTranslationEdit`, `CancelDefinitionEdit`, `ExitWordEditMode`, `UpdateWordInput`, `UpdateTranslationInput`, `UpdateDefinitionInput`, `NavigateBack`, `NoOperation`) применяется глобальный shortcut-ignore: `state.isPendingDbOp == true ⇒ state to emptySet()`. Не дублирую в каждой строке таблицы — читается из секции «Глобальный guard `isPendingDbOp`».

> **F036 — shortcut-ignore асимметрия Open*/Close*:** shortcut-ignore guards (`!isMenuOpen`, `!showWarningDialog`, `!snackbarState.show`) применяются только к `Close*` / `Dismiss*` Msg. `Open*` Msg (`OpenTopBarMenu`, `OpenDeleteWordDialog`, `OpenLexemeMenu(isShow=true)`) — безусловно идемпотентны, симметричный guard «уже открыто» не добавляется. UX-инвариант: иконка / триггер `Open*` недоступна в UI, когда соответствующее состояние уже открыто (меню-иконка скрыта при раскрытом меню, кнопка delete недоступна при показанном диалоге). Повторный `Open*`-Msg в reducer'е физически не возникает.

| Msg | Условие игнора |
|---|---|
| `CreateLexeme` | `wordState !is Loaded \|\| lexemeList.any { it.id == NOT_IN_DB }` (эквивалент computed `state.isCreatingLexeme`) |
| `RemoveWord` | `wordState !is Loaded \|\| wordState.id != msg.wordId` |
| `OpenDeleteWordDialog` | `wordState !is Loaded` |
| `CloseDeleteWordDialog` | `wordState !is Loaded \|\| !wordState.showWarningDialog` |
| `EnterWordEditMode` | `wordState !is Loaded` |
| `UpdateWordInput` | `wordState !is Loaded \|\| !wordState.isEditMode` |
| `ExitWordEditMode` | `wordState !is Loaded \|\| !wordState.isEditMode` |
| `CommitWordChanges` | `wordState !is Loaded \|\| !wordState.isEditMode \|\| wordState.edited.isBlank()` |
| `RemoveLexeme` | `wordState !is Loaded` (+ лексема не найдена) |
| `OpenLexemeMenu` | лексема не найдена |
| `CreateTranslation` | `lexeme.translation != null` (+ лексема не найдена) |
| `CreateDefinition` | `lexeme.definition != null` (+ лексема не найдена) |
| `EnterTranslationEditMode` | `lexeme.translation == null` (+ лексема не найдена) |
| `EnterDefinitionEditMode` | `lexeme.definition == null` (+ лексема не найдена) |
| `UpdateTranslationInput` | `lexeme.translation == null \|\| !lexeme.translation.isEdit` (+ лексема не найдена) |
| `UpdateDefinitionInput` | `lexeme.definition == null \|\| !lexeme.definition.isEdit` (+ лексема не найдена) |
| `CommitTranslationEdit` | `wordState !is Loaded \|\| lexeme.translation == null \|\| !lexeme.translation.isEdit` (+ лексема не найдена) |
| `CommitDefinitionEdit` | `wordState !is Loaded \|\| lexeme.definition == null \|\| !lexeme.definition.isEdit` (+ лексема не найдена) |
| `CancelTranslationEdit` | `lexeme.translation == null \|\| !lexeme.translation.isEdit` (+ лексема не найдена) |
| `CancelDefinitionEdit` | `lexeme.definition == null \|\| !lexeme.definition.isEdit` (+ лексема не найдена) |
| `RemoveTranslation` | `lexeme.translation == null` (F027) (+ лексема не найдена) |
| `RemoveDefinition` | `lexeme.definition == null` (F027) (+ лексема не найдена) |
| `CloseTopBarMenu` | `!topBarState.isMenuOpen` (F030) |
| `DismissNotification` | `!snackbarState.show` |

> **F019 — симметрия guards:** для всех адресных Msg (с `lexemeId` в payload) условие «лексема не найдена» дописано явно. Это дублирует общую сноску ниже таблицы, но делает guards-сводку самодостаточной — читатель видит полный набор условий ignor'а в одной строке без cross-reference на сноску. Симметрично `RemoveLexeme`, где условие тоже было выписано явно.

> 📎 guide: docs/guides/testing-reducers.md — "Что тестировать: ветвление в редьюсере (if/else в обработке сообщений); граничные случаи: пустые списки, NOT_IN_DB id, null значения"

### Чек-лист инвариантов contract_state v2.4 (как обеспечен reducer'ом)

| Инвариант | Как обеспечен |
|---|---|
| 1. `Loaded ∧ !isEditMode ⇒ edited == ""` | `ExitWordEditMode`, `CommitWordChanges`, `closeAllEditModes()` явно ставят `edited = ""` |
| 2. `\| { l : l.id == NOT_IN_DB } \| ≤ 1` | `CreateLexeme` guard `lexemeList.any { it.id == NOT_IN_DB }` запрещает append второй локальной лексемы. `RefreshTranslation` / `RefreshDefinition` заменяют `NOT_IN_DB → реальный id` в той же мутации (count `NOT_IN_DB` уменьшается на 1). `RemoveLexeme` для `NOT_IN_DB` удаляет локальную лексему. Никакая ветка reducer'а не порождает второй `NOT_IN_DB`. |
| 3. Уникальность id лексем | append `NOT_IN_DB`-лексемы только при отсутствии другой `NOT_IN_DB` (инв.2). Реальные id уникальны по конструкции БД; replacement `NOT_IN_DB → реальный id` в `RefreshTranslation`/`RefreshDefinition` не создаёт дубль, так как реальная лексема с этим id ещё не существовала (handler только что сделал insert). |
| 4. `translation/definition: !isEdit ⇒ edited == ""` | `Commit*Edit`, `Cancel*Edit`, `closeAllEditModes()`, `RemoveTranslation`/`RemoveDefinition` (F004) обнуляют `edited` при сбросе `isEdit`. Nullable ветка `Cancel*Edit`: при `origin.isEmpty()` reducer ставит `translation/definition = null`, и инв. 4 на null **не применяется** (формула инварианта содержит guard `translation != null` / `definition != null` — при null левая часть импликации `false`, импликация тривиально истинна). То есть защита через nullable, не через `edited = ""`; обе ветви `Cancel*Edit` корректны для инв. 4. |
| 5. `lexemeList.isNotEmpty ⇒ Loaded` | UI Msg не добавляют лексем при `NotLoaded` (`CreateLexeme` guard `wordState is Loaded`). Datasource Msg (`RefreshTranslation`/`RefreshDefinition`/`RefreshLexemeList`) приходят только после `WordLoaded` (handler не вызывается до initial load). |
| 6. `isCreatingLexeme ⇒ Loaded` (эквивалентно `(∃ l : l.id == NOT_IN_DB) ⇒ Loaded`) | `CreateLexeme` guard: `wordState is Loaded` |
| 7. `isLoading ⇒ lexemeList.isEmpty` (производный) | следует из инв. 5 + инв. 11 |
| 8. `snackbar.show ⇒ title != ""` | `ShowNotification(text)` из `contract_io` принимает непустой text; `DismissNotification` не меняет title (оставляет валидным) |
| 9. Single-edit-mode (Σ isEdit* ≤ 1) | **F018 — порядок:** в `Enter*EditMode` / `Create*` / `CreateLexeme` / `OpenLexemeMenu(isShow=true)` (F007) последовательность строго: guard → `closeAllEditModes()` (после успешного guard'а) → специфичная мутация по `closed.lexemeList` (правило F012 — mapping из результата хелпера, не из исходного `state`). `RemoveTranslation`/`RemoveDefinition` отдельно гасят `isEdit` целевого поля (F004). **F024 — `RemoveLexeme` тоже гасит `isEdit` у translation **и** definition удаляемой лексемы** (расширение F004): без сброса активный edit на translation/definition удаляемой лексемы остался бы в счётчике single-edit-mode, формально нарушая инвариант на момент перед фактическим удалением из `lexemeList`. **Мини-патч ит.6** — `CreateLexeme` создаёт лексему с `translation = null ∧ definition = null` (`isEdit`-флаги отсутствуют, в сумму не вносят); инв.9 сохраняется. |
| 10. `Loaded ⇒ value != ""` | `CommitWordChanges` guard `edited.isBlank()` ⇒ `state to emptySet()` (F001/F006) — структурная защита reducer'ом, не зависит от UI-блокировки кнопки |
| 11. `isLoading ⇒ NotLoaded` | `WordLoaded` (canon `contract_io`) атомарно ставит `wordState = Loaded(...)` ∧ `isLoading = false` |

## Удаляемые / новые messages

### Удаляются (сверка с текущим `Message.kt`)

- **`LoadingWord`** — устаревший паттерн «UI шлёт инициирующий Msg через `LaunchedEffect`». Антипаттерн (см. `docs/guides/mate-framework.md` Конвенция 5). В текущем проекте `LoadingWord` — мёртвый код: `WordCardScreen.kt` его не шлёт, инициирующая загрузка идёт через `initEffects = setOf(DatasourceEffect.LoadWord(wordId))` в `WordCardViewModel.kt`. Текущая reducer-ветвь `is Msg.LoadingWord -> state.showLoading() to setOf(LoadWord(wordId = state.wordState.id))` теперь *структурно невозможна*: `state.wordState.id` существует только в `Loaded`, а в `Loaded` re-load невалиден (инв. 11). Удаляем.
- **`OpenAddLexemeDialog`** — bottom sheet «выбери что добавить» больше нет (макет-driven).
- **`CloseAddLexemeDialog`** — то же.
- **`EnableTranslationCreation(isAdded)`** — чекбокса нет; chip «Перевод» внутри лексемы использует `CreateTranslation(lexemeId)`.
- **`EnableDefinitionCreation(isAdded)`** — то же; используется `CreateDefinition(lexemeId)`.
- **`UiMsg.ShowNotification(text, show)`** и sub-interface `UiMsg`:
  - `ShowNotification(text)` ⇒ переезжает в `contract_io` как Datasource Msg.
  - `ShowNotification(show=false)` ⇒ становится `DismissNotification` (UI Msg).
  - Sub-interface `UiMsg` не нужен — нет других членов.

### Переносятся в `contract_io` (Datasource Msg)

Эти Msg существуют в текущем `Message.kt`, но не относятся к UI-стороне — выносятся в `contract_io`:
- `WordLoaded(term)`
- `WordNotFound`
- `RefreshTranslation(lexeme)` — **переносится с переработкой сигнатуры на `(lexemeId: Long, translation: String?)`** (финальная форма — в `contract_io`). Старая сигнатура `(lexeme)` пересылала весь объект; новая адресует точечно по `lexemeId` + nullable значение (`null` ⇒ удалить, non-null ⇒ обновить `origin`). См. forward-ref таблицу выше. **Мини-патч ит.6:** reducer-логика дополнительно заменяет `NOT_IN_DB → реальный lexemeId` — см. раздел «Datasource Msg reducer-логика — замена NOT_IN_DB».
- `RefreshDefinition(lexeme)` — **переносится с переработкой сигнатуры на `(lexemeId: Long, definition: String?)`** симметрично `RefreshTranslation`. Та же замена `NOT_IN_DB → реальный lexemeId`.

**Удалены (мини-патч ит.6):**
- `RefreshLexeme(lexeme)` — больше не существует (см. forward-ref таблицу).

### Переименовываются

- **`ExitTranslationEditMode` → `CommitTranslationEdit`** — старое имя обманывало (был commit, не cancel; единственная reducer-ветвь шлёт `UpdateLexemeTranslation`).
- **`ExitDefinitionEditMode` → `CommitDefinitionEdit`** — то же.
- Парные `CancelTranslationEdit` / `CancelDefinitionEdit` — **новые**, см. ниже.
- `ExitWordEditMode` (cancel у word) — **не переименовывается** (обоснование в Reducer-секции `ExitWordEditMode`).

### Новые Msg (3 шт.)

- **`CancelTranslationEdit(lexemeId)`** — отмена редактирования перевода без коммита; при `origin == ""` ⇒ `translation = null`.
- **`CancelDefinitionEdit(lexemeId)`** — то же для определения.
- **`DismissNotification`** — пользовательский dismiss snackbar (UI-side).

### Изменяется логика reducer'а у существующих Msg (сверка с `WordCardReducer.kt`)

Из-за sealed `WordState`:
- **Все Msg, читающие `state.wordState.id` / `state.wordState.value`** теперь обязаны делать `when (val w = state.wordState) { ... }` (F003 — везде через `when`, не `as`-cast). Затронуты: `RemoveWord`, `EnterWordEditMode`, `UpdateWordInput`, `ExitWordEditMode`, `CommitWordChanges`, `CreateLexeme`, `CommitTranslationEdit`, `CommitDefinitionEdit`.
- **`enableWordEdit()` extension** в текущем `State.kt` обращается к `wordState.value` напрямую — переписать под `Loaded`-ветку или удалить и встроить в reducer.
- **`setTerm(term)` extension** — после переноса `WordLoaded` в `contract_io` логика должна конструировать `WordState.Loaded(id, added, value)` напрямую, а не мутировать поля. Существующий extension инвалиден (мутирует поля sealed object).

Поведенческие изменения, не связанные с sealed:
- `RemoveWord` — добавлены сбросы `topBarState.isMenuOpen` и `wordState.showWarningDialog`; добавлен guard `wordState.id != msg.wordId` (F005).
- `CommitWordChanges` — добавлено обнуление `wordState.edited = ""`; добавлен guard `edited.isBlank()` (F001/F006 — структурная защита инв. 10).
- `EnterWordEditMode` — добавлен вызов `closeAllEditModes()`; псевдокод через `when` (F003); guard.
- `UpdateWordInput` — добавлен guard `!isEditMode`.
- `ExitWordEditMode` — добавлен явный guard `!isEditMode`.
- `OpenDeleteWordDialog` — добавлен guard и сброс `topBarState.isMenuOpen`.
- `CloseDeleteWordDialog` — добавлен guard `!showWarningDialog` (F010).
- `CommitTranslationEdit` / `CommitDefinitionEdit` — (а) переименование, (б) сброс `isEdit = false ∧ edited = ""` в reducer'е, (в) **четырёхветочное** ветвление эффекта `local-nullify / Remove / no-op / Update`: 1a (F034 — `edited.isBlank() ∧ origin.isEmpty()`) ⇒ локальный nullify без effect, 1 (F002/F035 — `edited.isBlank()`) ⇒ pessimistic Remove effect, 2 (`edited == origin`) ⇒ no-op, 3 ⇒ Update; **ветвь 1 переведена с optimistic на pessimistic** (F002) — `translation`/`definition` не nullify'ится в reducer, ждём `Refresh*(null)`; transient-окно с показом старого origin зафиксировано как легитимное. **F013/F040 — ветвь Update использует единый внешний `when (val w = state.wordState) { is Loaded -> ...; NotLoaded -> state to emptySet() }`** вокруг всего guard+branching (не два независимых `when`) — explicit smart-cast без unchecked-cast. **F035 — `isBlank()` вместо `isEmpty()`** во всех empty-проверках веток 1a и 1: whitespace-only ввод считается пустым. **Мини-патч ит.6 — nullable `lexemeId` в Update-effect:** если `lexeme.id == NOT_IN_DB` ⇒ effect `UpdateLexemeTranslation`/`UpdateLexemeDefinition` несёт `lexemeId = null` (handler сделает insert лексемы + insert суб-сущности, ответит `RefreshTranslation`/`RefreshDefinition` с реальным id; reducer заменит `NOT_IN_DB → реальный` в той же мутации). Ветвь 1 (Remove) для NOT_IN_DB структурно не достижима (origin всегда `""`, попадаем в 1a).
- `CreateTranslation` / `CreateDefinition`, `EnterTranslationEditMode` / `EnterDefinitionEditMode` — добавлен `closeAllEditModes()`; явное `isEdit = true` (компенсация смены дефолта `TextValueState.isEdit` на `false`); **mapping `lexemeList` после хелпера идёт по `closed.lexemeList`, не по `state.lexemeList`** (F012); **в `Create*` явно отмечено что reducer не шлёт effect** (F021 — БД не трогается до `Commit*Edit`, что обосновывает безопасность локального nullify в `Cancel*Edit` при `origin == ""`).
- `UpdateTranslationInput` / `UpdateDefinitionInput` — добавлены guards.
- `CreateLexeme` — **мини-патч ит.6 — полностью локальный**: `closeAllEditModes()` + append `LexemeState(id = NOT_IN_DB, ...)` в `closed.lexemeList`; **никакого effect** (БД не трогается, лексема живёт локально до первого `CommitTranslationEdit` / `CommitDefinitionEdit`); guard через предикат `lexemeList.any { it.id == NOT_IN_DB }` (эквивалент computed `state.isCreatingLexeme`). Раньше шёл `DatasourceEffect.CreateLexeme(wordId)` — удалён.
- `RemoveLexeme` — добавлен сброс `LexemeState.isMenuOpen = false` **+ сброс `translation.isEdit`/`definition.isEdit = false` и `edited = ""` у удаляемой лексемы** (F024 — расширение F004 для случая удаления всей лексемы; иначе нарушение инв. 9 при удалении лексемы с активно редактируемым полем). Pseudocode переписан через `when (val w = state.wordState)` (F026/F031). **Мини-патч ит.6 — ветка для `NOT_IN_DB`:** локальное удаление через `filterNot { it.id == NOT_IN_DB }` без effect (БД-записи нет).
- `RemoveTranslation` / `RemoveDefinition` — добавлен сброс `LexemeState.isMenuOpen = false` **+ сброс `translation.isEdit`/`definition.isEdit = false` и `edited = ""`** (F004 — иначе нарушение инв. 9 при удалении активно редактируемого поля) **+ shortcut-guard `translation == null` / `definition == null`** (F027 — async-расхождение когда поле уже удалено). **Мини-патч ит.6 — ветка для `NOT_IN_DB`:** локальный nullify `translation`/`definition` без effect; если после nullify обе суб-сущности `null` — локальная cascade (удаление лексемы из state, симметрия с серверным cascade).
- `RemoveWord` — pseudocode переписан через `when (val w = state.wordState)` с явной веткой `NotLoaded -> state to emptySet()` (F026/F031).
- `OpenDeleteWordDialog` / `CloseDeleteWordDialog` — pseudocode переписан через `when (val w = state.wordState)` (F026).
- `EnterTranslationEditMode` / `EnterDefinitionEditMode` — целевая мутация переписана через `?.let { tr -> ... tr.origin }` вместо двойного `?.copy(...)`-чтения mutable свойства (F025 — smart-cast через `let`).
- `CloseTopBarMenu` — добавлен shortcut-guard `!topBarState.isMenuOpen ⇒ state to emptySet()` (F030 — симметрично F010/F011).
- `OpenLexemeMenu` — добавлена эксклюзивность (закрытие меню остальных лексем при `isShow == true`) **+ вызов `closeAllEditModes()` при `isShow == true`** (F007 — гарантирует инв. 9 при открытии меню поверх активного edit) **+ промежуточный val `closed` для mapping'а `closed.lexemeList` после хелпера** (F012/F017 — иначе результат `closeAllEditModes()` затирался исходным списком) **+ conditional copy `else if (l.isMenuOpen)` вместо безусловного** (F017 — economy на не-затронутых лексемах) **+ явный guard «лексема не найдена» при `isShow == false`** (F015 — раньше тянулся неявно через общую сноску).
- `DismissNotification` — добавлен guard `!snackbarState.show` (F011).
- ~~`RefreshLexeme`~~ — **удалён мини-патчем ит.6**. Канала «свежесозданная лексема из БД» нет: лексема создаётся в БД через `UpdateLexemeTranslation`/`UpdateLexemeDefinition` при первом Commit (handler делает insert + возвращает `RefreshTranslation`/`RefreshDefinition` с реальным id; reducer заменяет `NOT_IN_DB → реальный id`).
- **`RefreshTranslation` / `RefreshDefinition` (Datasource Msg, мини-патч ит.6):** reducer-логика теперь содержит ветку замены `NOT_IN_DB → реальный lexemeId` для завершения «первого Commit». См. раздел «Datasource Msg reducer-логика — замена NOT_IN_DB».

### Удаляемые extension-функции в `State.kt`

Связано со сменой шейпа `WordState` и удалением `AddLexemeBottomState`:
- `WordCardState.setWordId(id)` / `setWordAdded(date)` — мутируют отдельные поля sealed-объекта; невозможны на `NotLoaded`. После `WordLoaded` `Loaded(...)` конструируется целиком — частичные сеттеры не нужны.
- `WordCardState.setTerm(term)` — переписать под конструирование `WordState.Loaded(id, added, value)` напрямую.
- `WordCardState.showLoading()` — больше не нужно (`isLoading=true` только в начальном `WordCardState()`; промежуточных Loading-переходов нет — `Loaded` финален).
- `WordCardState.showAddLexemeBottom()`, `hideAddLexemeBottom()`, `setTranslationCheck()`, `setDefinitionCheck()` — `AddLexemeBottomState` удалён целиком (см. `contract_state.md`).
- `WordCardState.toggleLexemeMenu()` — заменено `setLexemeMenuOpen(lexemeId, isOpen)` с явным состоянием (текущая реализация уже использует именно его, `toggleLexemeMenu` — мёртвый код).

Сохраняются: `setWordValue`, `enableWordEdit`/`disableWordEdit` (адаптируются под `Loaded`), `updateWordEdited`, `showWordWarningDialog`/`hideWordWarningDialog` (адаптируются под `Loaded`), `setLexemeList`, `addLexeme`, `updateLexeme`, `removeLexeme`, все `createLexemeTranslation` / `enableLexemeTranslationEdit` / ... (внутри `LexemeState` шейп не менялся), все `TextValueState.*` extensions (шейп `TextValueState` остался, изменился только дефолт `isEdit`).

## Расхождения spec ↔ code

**Не применимо.** Режим работы — 1 (макет-driven), не 2 (spec-driven). Спека отсутствует (`spec_filename = null`). Сверка кода со спецификацией не производится; источник истины — Figma макет + текущий `Message.kt`/`WordCardReducer.kt`. Сверка с этими источниками встроена в раздел «Удаляемые / новые messages».

---

## log_messages

- ит.4: closed 1 critical — F024 (`RemoveLexeme` сбрасывает `isEdit/edited` у `translation` **и** `definition` удаляемой лексемы, расширение F004; чек-лист инв.9 дополнен явным упоминанием `RemoveLexeme`).
- ит.4: closed 7 minor — F025 (`Enter*EditMode` через `?.let { tr -> ... }`), F026 (`OpenDeleteWordDialog`/`CloseDeleteWordDialog`/`RemoveWord`/`RemoveLexeme` — pseudocode через `when (val w = state.wordState)`), F027 (`Remove*` shortcut-guard на `null` поле), F028 (локальный инвариант `OpenLexemeMenu` переформулирован), F029 (правило F012 ослаблено), F030 (`CloseTopBarMenu` shortcut-guard), F031 (поглощено F026). Rejected: F032.
- ит.5: closed 10 minor — F033 (ослабление F029 для `EnterWordEditMode` зафиксировано в разделе хелпера), F034 (`Commit*Edit` ветвь 1a — `edited.isBlank() ∧ origin.isEmpty()` ⇒ локальный nullify без effect, симметрично `Cancel*Edit`), F035 (`Commit*Edit` ветвь 1 → `isBlank()` вместо `isEmpty()`), F036 (ремарка про shortcut-ignore Close*/Dismiss* vs идемпотентные Open*), F037 (cross-contract assumption в Cancel-секциях: `Refresh*` не возвращает `origin = ""`), F038 (`OpenLexemeMenu` conditional copy для target в обеих ветвях), F039 (Refresh* — «переносится с переработкой сигнатуры»), F040 (`Commit*Edit` — единый внешний `when` вокруг guard'а и всех ветвей), F041 (убран висячий тег F022 из чек-листа), F042 (`RemoveLexeme` conditional copy для target).
- мини-патч из `contract_io` ит.4 (2026-05-19T19:00:00-0600): F040 — `DatasourceEffect.RemoveLexeme` payload расширен `(lexemeId)` → `(wordId, lexemeId)`. Reducer-блок UI Msg `RemoveLexeme(lexemeId)` обновлён: шлёт `RemoveLexeme(wordId = (w as Loaded).id, lexemeId = msg.lexemeId)`. Версия артефакта остаётся v3 (формы Msg не меняются, только signature forward-ref effect'а).

### ит.7 (2026-05-19T20:55:00-0600) — мини-патч isPendingDbOp

Добавлен глобальный pending-флаг в Effect-sending reducer-ветках.
Глобальный guard блокирует UI Msg при pending.
Все Datasource Msg reducer-ветки сбрасывают флаг.

Затронуто:
- Новая секция «Глобальный guard `isPendingDbOp`» перед хелпером `closeAllEditModes()` — описывает применение/исключения.
- `RemoveWord`, `CommitWordChanges`, `RemoveLexeme` (ветка real-id), `CommitTranslationEdit` (ветви 1 Remove и 3 Update), `CommitDefinitionEdit` (1 и 3), `RemoveTranslation` (real-id), `RemoveDefinition` (real-id) — в каждом `state.copy(...)` шлющем effect добавлен `isPendingDbOp = true`.
- `CreateLexeme` / `RemoveLexeme` для `NOT_IN_DB` / `RemoveTranslation` для `NOT_IN_DB` / `RemoveDefinition` для `NOT_IN_DB` — pending **не** выставляется (нет effect — локальные мутации).
- Раздел `initEffects` — отметка про начальный `isPendingDbOp = true` (совместно с `isLoading = true`), блокирующий UI на время `LoadWord` effect.
- Новая секция «Datasource Msg reducer-логика — сброс `isPendingDbOp`» — таблица Datasource Msg + правило сброса в каждой ветке: `WordLoaded`, `WordNotFound`, `RefreshWord`, `RefreshTranslation`, `RefreshDefinition`, `RefreshLexemeList`, `LexemeCascadeRemoved`, `NavigateBack`, `ShowNotification` → `isPendingDbOp = false`.
- Pseudocode `RefreshTranslation` / `RefreshDefinition` в секции замены `NOT_IN_DB` — добавлен `isPendingDbOp = false` в `state.copy(...)`.
- Сводная таблица guards — ремарка про глобальный guard `isPendingDbOp`, исключения перечислены.

Версия v3.1 → v3.2 (минор — патч ит.6 расширен сквозным pending-флагом).

### ит.6 (2026-05-19T20:10:00-0600) — мини-патч NOT_IN_DB

Лексема создаётся в БД при первом Commit Translation/Definition, не при тапе FAB.
Удалены `Msg.RefreshLexeme` и `Msg.CreateLexemeFailed` (из forward-ref Datasource Msg). `CreateLexeme` UI Msg — полностью локальный: append `LexemeState(id = NOT_IN_DB, ...)` в `closed.lexemeList`, никакого effect. Guard через `lexemeList.any { it.id == NOT_IN_DB }`.
`CommitTranslationEdit` / `CommitDefinitionEdit` — ветвь Update (3) получила nullable `lexemeId`: `effectLexemeId = if (lexeme.id == NOT_IN_DB) null else lexeme.id`. Effect `UpdateLexemeTranslation` / `UpdateLexemeDefinition` теперь несёт nullable lexemeId (форма — в `contract_io`). Для NOT_IN_DB ветвь 1 (Remove pessimistic) структурно недостижима (origin всегда `""`).
`RemoveLexeme` / `RemoveTranslation` / `RemoveDefinition` — добавлены ветки для `NOT_IN_DB`: локальное удаление/nullify без effect; для `RemoveTranslation`/`RemoveDefinition` — локальная cascade (если обе суб-сущности `null` после nullify, лексема удаляется из state целиком).
Новый раздел: «Datasource Msg reducer-логика — замена NOT_IN_DB» — `RefreshTranslation` / `RefreshDefinition` reducer заменяет `NOT_IN_DB` на реальный `lexemeId` из payload (завершение «первого Commit»).
Чек-лист инвариантов обновлён под v2.4 contract_state: инв.2 — `≤ 1` лексема с NOT_IN_DB (вместо полного запрета); инв.6 — через `(∃ l : l.id == NOT_IN_DB)`. Все ссылки на хранимое `isCreatingLexeme` заменены на computed `state.isCreatingLexeme` / предикат `lexemeList.any { it.id == NOT_IN_DB }`.
Версия v3 → v3.1 (минор — патч ит.5 расширен на ui_msg).

---

_model: claude opus 4.7 (1M context)_
