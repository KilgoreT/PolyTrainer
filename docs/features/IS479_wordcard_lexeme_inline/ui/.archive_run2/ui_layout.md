# ui_layout — IS479 wordcard_lexeme_inline

**Источники:**
- **Figma:** Mode A — один MCP-вызов `mcp__figma__get_figma_data(fileKey=w8GmGCdOZJUi99Cuv4q4W9)` без `nodeId` → локальный YAML-дамп `~/.claude/.../mcp-figma-get_figma_data-1779476258324.txt` (~459k строк). Парсится локально; повторные MCP API call'ы **запрещены** (rate limit). В дампе найдены все 9 target node-id'ов.
- **Спека:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features-spec/wordcard.md` (опубликована business sub-flow, ~442 строки).
- **Код:** `modules/screen/wordcard/...` — частично переписан под inline в первом UI-run (frames `9154-82532`, `9154-82519`, chip-стиль для `9154-82521`/`9154-82625` уже отражены в `AddLexemeWidget` / `LexemeChipPlaceholderWidget`).
- **Theme tokens:**
  - `modules/core/theme/src/main/java/me/apomazkin/theme/Color.kt` — `LexemeColor.*` (primary `#FF4A49BC`, secondary `#FF19191B`, tertiary `#FFF1E9FA`, onError `#FFDE2424`, onSecondary `#FFF2F2F3`) + standalone `whiteColor`, `blackColor`, `grayTextColor #FF7B7E85`, `unselectedGreyColor #FF95989D`, `enableIconColor #FF252628`, `dividerColor #FFE4E5E7`.
  - `modules/core/theme/src/main/java/me/apomazkin/theme/LexemeStyle.kt` — `LexemeStyle.{H1..H6, BodyXLBold, BodyXL, BodyLBold, BodyL, BodyMBold, BodyM, BodySBold, BodyS}`. Все на `defaultFontFamily = FontFamily.Default`, **не** Roboto (Figma даёт Roboto). См. «Открытые вопросы».

---

## Расшифровка 5 variant frames (`9154-82509/86012/86182/86353/86499`)

Brief описывает их как «variant state». В Figma-дампе это пять **самостоятельных** `word card` frame'ов (412×892 dp), каждый с полным экраном WordCard. Различаются они **состоянием Lexeme-блока** (фрейм `name: Lexeme` внутри `screen_content > cards`):

| Figma word-card | Lexeme child | layout/состояние | Семантика |
|---|---|---|---|
| `9154-82509` | `9154-82519` | layout_RVILFH (column, gap 12, padding 16, align flex-end). Children: `Frame 2043682685` (column IM161Z) с `Frame 2043682684` (row RBJAXR — chip "Перевод" filled-brand + trash + hint-текст "Сохранить" greyed) и Text field instance 6260:64584 (focused, value "appl"); затем `Frame 2043682683` (row 54DEHW — chip-group `tui-tag` "Определение", "Пример" outlined); затем `tui-button` "Удалить". | **EDITING translation (filled value):** активный inline-edit перевода, кнопка "Сохранить" текстом, под полем — кандидаты для следующих лексем (Определение / Пример) и Удалить. |
| `9154-86012` | `9154-86022` | layout_RVILFH (same outer), внутри Text field instance `29900:195776` — placeholder "Введите перевод" + cursor RECTANGLE 1×16. Chip-group: `tui-tag` "Определение" (trailing visible), "Пример" (trailing hidden). | **EDITING translation (empty value, focused):** пустое поле с курсором, placeholder grey. |
| `9154-86182` | `9154-86192` | Frame 2043682685 = `Frame 2043682688` (B17IMZ — row, align center) c Text field (large) + `16px / square-rounded-x-filled` clear-icon. Затем chip-group "Определение"/"Пример" outlined + "Удалить" tui-button. layout_QB6X0B на parent (column, hug). | **EDITING translation (in-row clear):** edit-режим с inline-кнопкой очистить (square-rounded-x-filled) на той же строке, что и поле. |
| `9154-86353` | `9154-86363` | Frame 2043682686 (WSLMRI — row, space-between) c `tui-tag` "Перевод" (variant без trash, leading icon) + `16px / pencil`. Затем Text field (large, value "apple", `Hint: true`). Затем `Frame 2043682691` (AZA3I3 — row, justify flex-end, padding-top 12, top-border 1px `#E4E5E7`) с "Добавить ещё" + `chevron-up`. Chip-group "Определение"/"Пример" + "Удалить". Также SnackBar instance с `Сохранено`. | **VIEW translation (saved, expanded):** уже сохранённое значение, pencil-edit вместо trash, divider + "Добавить ещё ▴" внизу, всплыл snackbar Сохранено. |
| `9154-86499` | `9154-86509` | layout_9MJ3UK (column, gap 12, padding 16, alignSelf stretch). Frame 2043682685 → `Frame 2043682685` (WSLMRI — chip "Перевод" + `Frame 2043682689` 1OA8PK c `arrow-back-up` иконка + "Сохранить" текст). Затем `Frame 2043682690` (1Q8R2L — row, fill) с Text field (large, "apple green", `componentId 6260:64590` — другая variant input) + `square-rounded-x-filled`. Затем `Frame 2043682691` AZA3I3 с "Добавить ещё ▾" (chevron-down). **Нет** "Определение"/"Пример" chip-группы и **нет** "Удалить". | **EDITING multi-line translation (alternate add-flow):** другой вариант inline-edit (выбран `1Q8R2L` row), есть `arrow-back-up` + "Сохранить" cluster, "Добавить ещё" с chevron-down (свёрнут другой блок). Нет chip-add ниже — компактный «один-блок» режим. |

**Главный вывод:** все 5 variants — это **состояния одной inline-edit-зоны** в Lexeme-блоке: empty input vs filled, view-mode vs edit-mode, в одном блоке («только translation») vs в композите с другими chip'ами под ним. Brief не разграничивает их явно — UI-implementer должен покрыть **минимум 3** состояния: `placeholder + cursor` (`86012`), `editing with value` (`82509`/`86182`), `view-mode saved with pencil-edit` (`86353`). Frame `86499` — это **другой стиль** edit-режима (с `arrow-back-up` и без под-chip'ов). На текущей итерации код реализует state-наборы `placeholder + cursor`/`editing with value`/`view-mode saved` (через `LexemeValueFieldWidget.isEdit`), но **без** pencil-варианта и **без** «Добавить ещё»-блока внизу.

---

## Экран WordCard — карточка слова

Экран открывается из списка слов. До `Msg.WordLoaded` `wordState = NotLoaded`, рендерится только `Scaffold` + `TopBar` (контент пустой). После — основной `Column` с inline-композицией.

### Дерево виджетов (Compose-стиль)

```
Scaffold [containerColor=Transparent, contentWindowInsets=zero]
├─ topBar = TopBarWidget [REUSE: .../widget/TopBarWidget.kt]
│  └─ TopAppBar (M3)
│     ├─ navigationIcon = IconBoxed(ic_back, size=44dp, tint=enableIconColor)
│     └─ actions = IconDropdownWidget [REUSE: modules/widget/icondropdowned]
│        └─ DeleteWordMenuItem [REUSE: .../widget/DeleteWordMenuItem.kt]
│
├─ snackbarHost = SnackbarHost(snackbarHostState)
│  ↳ управление: SnackbarLaunchEffect [REUSE: .../widget/SnackbarLaunchEffect.kt]
│  ↳ детали паттерна см. ремарку под этим блоком
│
└─ content = Box [fillMaxHeight, padding=scaffoldInsets, consumeWindowInsets, imePadding, navigationBarsPadding]
   ├─ if (loaded != null):
   │  Column [fillMaxHeight, background=colorScheme.tertiary, verticalScroll, padding=16dp]
   │  ├─ WordFieldWidget(loaded, isPendingDbOp, sendMessage) [REUSE: .../widget/WordFieldWidget.kt]
   │  ├─ Spacer(height=8dp)
   │  ├─ Column [fillMaxWidth, verticalArrangement=spacedBy(8dp)]
   │  │  └─ forEach lexemeState (key=lexemeState.id):
   │  │     └─ LexemeItemWidget(order, state, isPendingDbOp, sendMessage) [REUSE]
   │  ├─ Spacer(height=16dp)
   │  └─ AddLexemeWidget(enabled, onAddLexeme, modifier=fillMaxWidth) [REUSE: .../widget/AddLexemeWidget.kt]
   │
   └─ if (loaded != null && loaded.showWarningDialog):
      ConfirmDeleteWordWidget(loaded, sendMessage) [REUSE]
```

> **Snackbar — канон проекта** (см. `docs/guides/ui-patterns.md` § «Snackbar и one-shot UI-уведомления (UiHost)»):
>
> Канон — `UiHost` abstraction через `@AssistedInject`. Handler вызывает `uiHost.showSnackbar(message)` **напрямую**, без круга через Msg + State. Snackbar — one-shot side-effect, не часть data state.
>
> - `interface UiHost { suspend fun showSnackbar(message: String); fun showToast(message: String) }` — в screen-модуле либо общий в `core/ui`.
> - `UiEffectHandler @AssistedInject constructor(@Assisted private val uiHost: UiHost)` — handler получает host через AssistedInject. В `onEffect` — прямой вызов `uiHost.showSnackbar(effect.message)`.
> - `UiHostImpl(snackbarHostState, context)` — реализация на стороне Composable, держит `SnackbarHostState` и `Context`.
> - В корневом Composable: `snackbarHostState = remember { SnackbarHostState() }`, `uiHost = remember { UiHostImpl(snackbarHostState, context) }`, ViewModel получает `uiHost` через AssistedInject factory.
> - `SnackbarHost(snackbarHostState)` в слоте Scaffold — обязателен (хост для отображения).
> - Симметрично паттерну Navigation: handler получает `Navigator` тем же путём.
>
> **Legacy state-based** (IS479 на текущей итерации):
>
> Сейчас в коде wordcard — legacy: `state.snackbarState: SnackbarState`, `Msg.ShowNotification(text)` (Datasource) пишет в state, `SnackbarLaunchEffect` смотрит `state.snackbarState.show`, шлёт `Msg.DismissNotification` для сброса. Этот паттерн **устарел** по обновлённому гайду, мигрируется постепенно (см. `docs/Backlog.md` → «UiEffect: убрать круг Effect → Msg → State, показывать snackbar/toast напрямую через UiHost» + «Централизованная система ошибок и снекбаров»).
>
> **Решение по IS479:** миграция wordcard на UiHost — **отдельная задача, не в скоупе IS479**. На текущей фиче следуем существующему legacy state-based (не вводим новый код по нему — оставляем как есть). После закрытия IS479 — открыть отдельный тикет на миграцию по канону.

### Дерево виджетов внутри LexemeItemWidget (per-state, источник `9154-82519`)

```
Column(fillMaxWidth)
├─ LexemeTitleWidget(order, state, isPendingDbOp, sendMessage)
│  Row(fillMaxWidth, padding=horizontal 16dp, SpaceBetween)
│  ├─ Text("Value $order", style=BodyS, color=primary)
│  └─ IconDropdownWidget(trigger=IconBoxed(ic_more_horizonral, 24dp))
│     ├─ if translation==null → AddTranslationLexemeMenuItem
│     ├─ if definition==null  → AddDefinitionLexemeMenuItem
│     └─ DeleteLexemeMenuItem
└─ Surface(shape=RoundedCornerShape(12dp), color=whiteColor, shadowElevation=4dp)
   Column(fillMaxWidth, padding=vertical 8dp)
   ├─ if translation != null → LexemeValueFieldWidget(state=translation, titleRes=R.string.word_card_bottom_translation, ...)
   │   else → LexemeChipPlaceholderWidget(labelRes=R.string.word_card_lexeme_add_translation, ...)
   ├─ if (translation != null && definition != null) HorizontalDivider(padding=vertical 8dp, color=tertiary)
   └─ if definition != null → LexemeValueFieldWidget(state=definition, titleRes=R.string.word_card_bottom_definition, ...)
       else → LexemeChipPlaceholderWidget(labelRes=R.string.word_card_lexeme_add_definition, ...)
```

`LexemeValueFieldWidget` ветвится по `state.isEdit`:
- **view-mode (`!isEdit`):** `InputChip(selected=true, label=state.origin, trailingIcon=IconBoxed(ic_circle_delete, 18dp))` — Figma `82521` (chip "Перевод") как образец.
- **edit-mode (`isEdit`):** `EditRow(BasicTextField + IconBoxed(ic_confirm) + IconBoxed(ic_close))` — Figma `82519`/`86182`.

---

## Виджеты (per-widget detail)

### `[REUSE] TopBarWidget`

- **Файл:** `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/TopBarWidget.kt`
- **Параметры:** `topBarState: TopBarState, onBackPress: () -> Unit, sendMessage: (Msg) -> Unit`.
- **Layout:** Material3 `TopAppBar` (OptIn `ExperimentalMaterial3Api`).
- **Slots:**
  - `navigationIcon`: `IconBoxed(ic_back, size=44dp, colorEnabled=enableIconColor #FF252628)` → `Msg.NavigateBack`.
  - `title`: пустой.
  - `actions`: `IconDropdownWidget` тригер по `ic_more` → `Msg.OpenTopBarMenu`; dismiss → `Msg.CloseTopBarMenu`. Внутри — `DeleteWordMenuItem`.
- **Размеры:** M3 `TopAppBar` высота 64dp.
- **Цвета:** дефолтные M3 surface.
- **Типографика:** N/A.
- **Иконки:** `R.drawable.ic_back`, `R.drawable.ic_more` (или `_horizonral`).
- **States:** default / pressed (M3 ripple); disabled / focused / error / loading — N/A.
- **Touch target:** `IconBoxed(44dp)` — на 4dp ниже M3 48dp guideline; компенсируется обёрткой `TopAppBar` 64dp min-height.
- **Accessibility:** `IconBoxed` без явного `contentDescription` — **regression-кандидат** (back-кнопка должна иметь «Назад»).
- **Анимации:** дефолтные M3.

### `[REUSE] DeleteWordMenuItem`

- **Файл:** `.../widget/DeleteWordMenuItem.kt`
- **Layout:** `MenuItem.withIcon(...).Widget()` (из `modules/widget/icondropdowned`).
- **Иконка:** `R.drawable.ic_delete` с tint `colorScheme.onError = #FFDE2424`.
- **Текст:** `R.string.button_delete`, style `LexemeStyle.BodyL` (17sp Normal), color `colorScheme.onError`.
- **Callback:** `onDeleteClick` → `Msg.CloseTopBarMenu` + `Msg.OpenDeleteWordDialog`.
- **States:** default / pressed (M3 ripple). M3 `DropdownMenuItem` min-height 48dp.
- **Accessibility:** текст accessible; иконка decorative.

### `[REUSE] WordFieldWidget`

- **Файл:** `.../widget/WordFieldWidget.kt`
- **Параметры:** `loaded: WordState.Loaded, isPendingDbOp, sendMessage`.
- **Layout:** `Surface(shape=RoundedCornerShape(12dp), shadowElevation=4dp, color=whiteColor)` → `Box(fillMaxWidth, padding=16dp)` → `Column(fillMaxWidth)`:
  1. `LexemeEditableText` [REUSE: `modules/core/ui/.../text/base/LexemeEditableText.kt`] — inline-edit слова:
     - `originValue=loaded.value, changedValue=loaded.edited, isEditMode=loaded.isEditMode`.
     - `textColor=blackColor`, `textStyle=LexemeStyle.H5` (24sp Medium).
     - `onTextChange` → `Msg.UpdateWordInput(value)` (guarded).
     - `onOpenEditMode` → `Msg.EnterWordEditMode`.
     - `onCloseEditMode` → `Msg.CommitWordChanges` (close-icon = save).
  2. `Row(fillMaxWidth, padding=vertical 4dp, horizontalArrangement=spacedBy(8dp))`:
     - `Text("Added", style=BodyM 15sp Normal, color=grayTextColor #FF7B7E85)`.
     - `Text(getDate(loaded.added), style=BodyMBold 15sp Bold, color=colorScheme.secondary #FF19191B)`.
- **Доп.:** `ImageFlagWidget(flagRes=example_ic_flag_gb, modifier=align(BottomEnd))`.
- **Figma-параллель:** соответствует `word header` frame (id `9158:72107` и др.) в каждом variant: `tui-input` instance + `items+flag` блок. Figma даёт fontSize **17sp** на `Введите перевод` (`Desktop/Body L`) и **24sp** на value `apple` (`Desktop/Heading 5`) — в коде используется `LexemeStyle.H5 = 24sp Medium`. Совпадает.
- **States:** default / editing / disabled (через `isPendingDbOp` guard).
- **Touch target:** `LexemeEditableText` — внутренний.
- **Accessibility:** текст value accessible; флаг decorative (stub).
- **Анимации:** N/A.

### `[REUSE] LexemeItemWidget`

- **Файл:** `.../widget/lexeme/LexemeItemWidget.kt`
- **Параметры:** `order: Int, state: LexemeState, isPendingDbOp, sendMessage`.
- **Layout:** `Column(fillMaxWidth)`:
  1. `LexemeTitleWidget(...)`.
  2. `Surface(shape=RoundedCornerShape(12dp), color=whiteColor, shadowElevation=4dp)` → `Column(fillMaxWidth, padding=vertical 8dp)`:
     - **TRANSLATION block:** `LexemeValueFieldWidget(state=translation, padding=horizontal 16dp)` ИЛИ `LexemeChipPlaceholderWidget(labelRes=word_card_lexeme_add_translation, padding=horizontal 16dp)`.
     - **HorizontalDivider** только когда `translation != null && definition != null`, padding=vertical 8dp, color=colorScheme.tertiary `#FFF1E9FA`.
     - **DEFINITION block:** симметрично.
- **Figma-параллель:** соответствует `Lexeme` frame (`9154:82519`/`86022`/`86192`/`86363`/`86509`). Figma `layout_RVILFH` даёт `padding: 16px`, `gap: 12px`, `alignItems: flex-end` — **расхождение**: в коде используется `Surface(padding vertical 8dp)` без `padding horizontal`, а padding 16dp применяется на children индивидуально. **Не критично** визуально, но pixel-perfect требует выровнять на column-level.
- **States:** проксируются от children.
- **Touch target:** распределён по children.
- **Accessibility:** проксируется.

### `[REUSE] LexemeTitleWidget`

- **Файл:** `.../widget/lexeme/LexemeTitleWidget.kt`
- **Layout:** `Row(fillMaxWidth, padding=horizontal 16dp, SpaceBetween, CenterVertically)`:
  - `Text("Value $order", color=colorScheme.primary #FF4A49BC, style=LexemeStyle.BodyS 13sp Normal)`.
  - `IconDropdownWidget(isDropDownOpen=state.isMenuOpen, ...)`:
    - trigger: `IconBoxed(ic_more_horizonral, size=24, colorEnabled=colorScheme.primary, enabled=!isPendingDbOp)`.
    - тап → `Msg.OpenLexemeMenu(lexemeId, isShow=true)`.
    - dismiss → `Msg.OpenLexemeMenu(lexemeId, isShow=false)`.
    - **Menu items (условно):**
      - `translation == null` → `AddTranslationLexemeMenuItem`.
      - `definition == null` → `AddDefinitionLexemeMenuItem`.
      - всегда → `DeleteLexemeMenuItem`.
- **Figma-параллель:** **расхождение с Figma 86182.** В Figma `9154-86182` лексема имеет на верху ряд **с trailing trash-иконкой**, а не dropdown menu. В коде используется dropdown menu — это упрощение, не покрывающее Figma-визуал. Также Figma не показывает текст "Value $order" — там есть chip "Перевод" с trailing trash/leading icon. **Несоответствие фиксируется как открытый вопрос — текущая реализация это решение из старого UI, до redesign'а.**
- **States:** disabled-trigger при `isPendingDbOp`.
- **Touch target:** `IconBoxed(24dp)` — **открытый вопрос** ≥ 48dp.
- **Accessibility:** `IconBoxed` без `contentDescription` — regression-кандидат.
- **Анимации:** M3 dropdown enter/exit.

### `[REUSE] AddTranslationLexemeMenuItem` / `AddDefinitionLexemeMenuItem`

- **Файлы:** `.../widget/lexeme/AddTranslationLexemeMenuItem.kt`, `.../AddDefinitionLexemeMenuItem.kt`.
- **Layout:** `MenuItem.text(...).Widget()`.
- **Текст:** `R.string.word_card_lexeme_add_translation` / `_add_definition`, style `LexemeStyle.BodyL` (17sp Normal).
- **Color:** дефолтный (translation) / `blackColor` (definition).
- **Callback:** `Msg.CreateTranslation(lexemeId)` / `Msg.CreateDefinition(lexemeId)` + `Msg.OpenLexemeMenu(isShow=false)`.

### `[REUSE] DeleteLexemeMenuItem`

- **Файл:** `.../widget/lexeme/DeleteLexemeMenuItem.kt`
- **Текст:** `R.string.word_card_lexeme_delete`, style `LexemeStyle.BodyL`, color `colorScheme.onError #FFDE2424`.

### `[REUSE] LexemeChipPlaceholderWidget`

- **Файл:** `.../widget/lexeme/LexemeChipPlaceholderWidget.kt`
- **Параметры:** `@StringRes labelRes, enabled, onClick, modifier`.
- **Layout:** Material3 `SuggestionChip` (OptIn).
- **Slots:**
  - `icon`: `Icon(painter=R.drawable.ic_add_value, size=18dp, tint=colorScheme.primary, contentDescription=null)`.
  - `label`: `Text(stringResource(labelRes), color=colorScheme.primary)`.
- **Размеры (M3 default `SuggestionChip`):** height 32dp, content padding horizontal 8–16dp / vertical 6dp, shape `RoundedCornerShape(8dp)` outlined.
- **Figma-параллель `9154-82625` (Frame 2043682683):** layout_54DEHW — row, gap 12px, hug; children — `tui-tag` instances (componentId `28370:397707`, "Определение"/"Пример"), layout_4E1NW5 — row gap 4px, padding `8px 12px`, borderRadius `999px`, fill `#F5F3F8` (R87IJC). **Расхождение:** Figma использует пилевидный chip (border-radius **999px**) с fill `#F5F3F8` light-gray. Код — M3 `SuggestionChip` (outlined, RoundedCornerShape 8dp, transparent). **Кандидат на правку:** добавить `SuggestionChipDefaults.suggestionChipColors(containerColor=Color(0xFFF5F3F8))` + `shape = CircleShape` (или `RoundedCornerShape(999.dp)`).
- **Типографика:** label `M3 labelLarge 14sp`; Figma label = `Desktop/Body S (63:46) 13sp Normal`. **Расхождение:** Figma `13sp`, M3 `14sp`. Маппится на `LexemeStyle.BodyS`. Рекомендация — явный `style = LexemeStyle.BodyS`.
- **Иконка:** `R.drawable.ic_add_value` (24x24 vector, fillColor `#ffffff`, перекрашен tint primary). **Note:** в Figma `9154-82625` иконки **нет** (только trailing icon). Лидирующий "+" — это design-decision UI-implementer'а: текущий «add»-плейсхолдер использует "+" иконку, Figma не показывает. Сохранить.
- **States:**
  - `default` — M3 outlined container + primary content.
  - `pressed` — ripple.
  - `focused` — outline.
  - `disabled` — alpha 0.38.
  - `error`, `loading` — N/A.
- **Touch target:** M3 `SuggestionChip` обеспечивает 48dp.
- **Accessibility:** label accessible; icon decorative.
- **Анимации:** M3 ripple.

### `[REUSE] LexemeValueFieldWidget`

- **Файл:** `.../widget/lexeme/LexemeValueFieldWidget.kt`
- **Параметры:** `state: TextValueState, @StringRes titleRes, enabled, onTextChange, onOpenEditMode, onCommitEdit, onCancelEdit, onRemove, modifier`.
- **Layout:** `Column(fillMaxWidth, verticalArrangement=spacedBy(4dp))`:
  1. `Text(stringResource(titleRes), style=LexemeStyle.BodyS 13sp Normal, color=grayTextColor #FF7B7E85)` — «Translation» / «Definition».
  2. ветвление по `state.isEdit`:
     - **View-mode:** M3 `InputChip(selected=true, enabled, onClick=onOpenEditMode)`:
       - `label`: `Text(state.origin)`.
       - `trailingIcon`: `IconBoxed(ic_circle_delete, size=18, colorEnabled=unselectedGreyColor #FF95989D, onClick=onRemove)`.
       - `colors = InputChipDefaults.inputChipColors(selectedContainerColor=secondaryContainer #FF19191B, disabledSelectedContainerColor=secondaryContainer)`.
     - **Edit-mode:** `EditRow` локальный composable:
       - `Row(fillMaxWidth, CenterVertically, spacedBy(4dp))`:
         - `BasicTextField(weight=1f, focusRequester, value=TextFieldValue(state.edited, selection=TextRange(length)), enabled, textStyle=LexemeStyle.BodyL 17sp Normal color=secondary)`.
         - `IconBoxed(ic_confirm, size=18, colorEnabled=colorScheme.primary, onClick=onCommit)` → `Msg.CommitTranslationEdit` / `CommitDefinitionEdit`.
         - `IconBoxed(ic_close, size=18, colorEnabled=unselectedGreyColor, onClick=onCancel)` → `Msg.CancelTranslationEdit` / `CancelDefinitionEdit`.
       - `LaunchedEffect(Unit)`: `focusRequester.requestFocus()` — auto-focus при входе.
       - `LaunchedEffect(value)`: ресинк `fieldValue` если внешний `value` поменялся.
- **Figma-параллель view-mode (`9154-82521` chip "Перевод"):**
  - layout_52S1OH: row, gap 2px, padding `2px 2px 2px 6px`, borderRadius 6px, fill `#4A49BC` (62UVHJ = primary).
  - label: `Desktop/Body XS (63:48)` = 11sp Medium, color `#FFFFFF` (QMOUFY = onPrimary).
  - trailing icon: `16px/close-rounded` (componentId 9163:40871), 16x16.
  - **Расхождение:** Figma chip — **fill primary**, **label onPrimary**, **height ~20dp** (padding 2+inner+2), **labelStyle 11sp Medium**. Код — M3 `InputChip` с `selectedContainerColor = secondaryContainer = #FF19191B` (dark gray), label M3 default `labelLarge 14sp`, height M3 default ~32dp. **Цветовая инверсия:** Figma — brand-blue selected, код — dark gray selected. **Кандидат на правку:** `InputChipDefaults.inputChipColors(selectedContainerColor=primary, selectedLabelColor=onPrimary, selectedTrailingIconColor=onPrimary)`.
  - **trailing icon** в Figma — `close-rounded` (X в кружке); в коде используется `ic_circle_delete` (трешь в кружке). Возможно совпадает по визуалу — оба «remove» glyph'а в кружке.
- **Figma-параллель edit-mode (`9154-82519`):**
  - Text field instance `6260:64584` → tui-input variant `29896:193283` (Size=L, Filled, Outlined): layout_TVVZF7 row, gap 8px, padding `0px 8px 0px 16px`, height 56dp, borderRadius 12px, fill `#FFFFFF`, stroke `#FFFFFF` 1px. Внутри FieldContent column gap 4px padding `6px 0px`, FieldLabel row label "Введите перевод" `Desktop/Body L 17sp`, FieldValue text "appl" `Desktop/Body L 17sp` color `#19191B`.
  - **Расхождение:** Figma input field = **готовый M3 OutlinedTextField/TextField с label-внутри** (Material 56dp height). Код — `BasicTextField` без рамки/label, просто текст. **Это сильное упрощение.** Текущий код визуально не похож на Figma input. **Кандидат на правку:** заменить `BasicTextField` на `OutlinedTextField`/`TextField` с label "Введите перевод" + соответствующими цветами.
  - **Buttons:** в Figma справа от label — текст "Сохранить" `Desktop/Body S (63:46) 13sp` color `#B0B2B6` (9PL09E light-grey, disabled) ИЛИ `#4A49BC` (primary, enabled). В коде вместо текста — два icon-button'а: галочка commit + крестик cancel. **Расхождение semantic:** Figma — единая кнопка-action «Сохранить» (disabled/enabled); код — пара (commit/cancel). Сохранить структуру кода (она богаче), но визуально привести к Figma — кандидат.
- **Figma-параллель view-mode «saved» (`9154-86353`):**
  - Frame 2043682686 (WSLMRI — row, space-between): chip "Перевод" filled-brand + `16px / pencil` icon (componentId 9158:76208) на правой стороне. Это **«saved chip + edit pencil»** паттерн. Код этого паттерна **не реализует** — pencil-edit отсутствует.
- **Цвета:**
  - `secondaryContainer = #FF19191B` (текущий код).
  - `unselectedGreyColor = #FF95989D` (trailing/cancel icon).
  - `colorScheme.primary = #FF4A49BC` (confirm).
  - `grayTextColor = #FF7B7E85` (заголовок).
- **Типографика:**
  - Заголовок — `LexemeStyle.BodyS` (13sp Normal) — **совпадает** с Figma 13sp.
  - Edit-text — `LexemeStyle.BodyL` (17sp Normal) — **совпадает** с Figma `Desktop/Body L 17sp`.
  - Chip-label — M3 `labelLarge 14sp`; Figma 11sp `Desktop/Body XS (63:48)` — **расхождение**.
- **Иконки:** `ic_circle_delete`, `ic_confirm`, `ic_close`. Figma upstream использует `close-rounded` (X в кружке) — `ic_circle_delete` визуально похож, но другой glyph. **TODO:** импортировать `ic_close_rounded` из `9163:40871`.
- **States:** default (view) / editing / disabled / error N/A / loading (via `enabled`).
- **Touch target:** chip M3 ≥ 48dp; `IconBoxed(18dp)` — open question ≥ 48dp.
- **Accessibility:** `BasicTextField` без `Modifier.semantics(contentDescription=...)` — regression-кандидат; `IconBoxed` без `contentDescription` — regression-кандидат.
- **Анимации:** N/A (нет custom).

### `[REUSE] AddLexemeWidget`

- **Файл:** `.../widget/AddLexemeWidget.kt`
- **Параметры:** `enabled, onAddLexeme, modifier`.
- **Назначение:** inline-кнопка «Add meaning» (Figma `9154-82532`). В основном Column после lexeme-списка.
- **Layout:** Material3 `Button(modifier, enabled, onClick, colors=buttonColors(containerColor=primary, contentColor=onPrimary))`:
  - `Icon(ic_add_value, size=18dp, contentDescription=null)`.
  - `Text(padding(start=8dp), text=word_card_add_lexeme = «Add meaning»)`.
- **Размеры (M3 default Button):** height ≈ 40dp, content padding horizontal 24dp / vertical 8dp, shape `RoundedCornerShape(20dp)` full-rounded.
- **Figma-параллель `9154-82532`:** в Figma frame — **FAB** (IMAGE-SVG, componentId `29907:178121`, layout_453IT6: row center hug, locationRelativeToParent x=340 y=720 → нижний-правый угол), borderRadius 12px, fill `#4A49BC` (primary), `Show Label: false`, `Show icon: true` — **только icon**, без label. **Сильное расхождение:** Figma `9154-82532` — это **FAB с одной иконкой "Добавить значение"** в правом нижнем углу, а **не** inline-кнопка с надписью «Add meaning». Brief сказал «заменяемая Add-кнопка» — её **роль** заменяется, но визуал отличается. **Решение в брифе:** в task.md написано «Механизм добавления (inline)» = `9154-82519`, а frame `9154-82532` упомянут как «Заменяемая кнопка добавления». В новом UI **9154-82532 уходит**, его место занимает inline-механизм по `9154-82519`. Текущий код **сохраняет** Button — это компромисс (inline-кнопка с visible label). **Уточнить:** должен ли быть отдельный Add-button в новом дизайне, или его роль полностью растворяется в `Frame 2043682683` (chip-add Определение/Пример) внутри Lexeme-блока. На Figma `86353` есть **обе** сущности: chip-add + FAB. То есть FAB сохраняется. Текущий код заменил FAB на inline Button — **рисковое отклонение**. См. «Главные расхождения».
- **Цвета:** container `colorScheme.primary #FF4A49BC`, content `colorScheme.onPrimary #FFFFFFFF` — **совпадает** с Figma fill `62UVHJ`.
- **Типографика:** M3 `Button` `labelLarge 14sp Medium` (default).
- **Иконка:** `R.drawable.ic_add_value`.
- **States:** default / pressed (ripple) / focused / disabled (M3 default alpha) / loading (через `enabled=!isPendingDbOp && !isCreatingLexeme`).
- **Touch target:** M3 Button ≥ 48dp.
- **Accessibility:** label accessible; icon decorative.
- **Анимации:** M3 ripple.

### `[REUSE] ConfirmDeleteWordWidget`

- **Файл:** `.../widget/ConfirmDeleteWordWidget.kt`
- **Layout:** `AlarmDialogWidget` [REUSE]:
  - `alarmButtonText = R.string.button_delete`.
  - `onAlarmClick` → `Msg.RemoveWord(loaded.id)`.
  - `onDismissRequest` → `Msg.CloseDeleteWordDialog`.
  - content:
    - `Text(R.string.vocabulary_delete_word, style=LexemeStyle.H6 20sp Medium, color=colorScheme.secondary)`.
    - `Spacer(height=8dp)`.
    - `Text(R.string.word_card_delete_word_subheading, style=LexemeStyle.BodyL 17sp Normal, color=grayTextColor)`.
- **Figma-параллель:** Figma `9154-86649` (Snackbar `Сохранено`) — это **другой** компонент (success-snack, не delete-dialog). Confirm-delete в Figma-дампе target frames **не найден**. Сохранить как есть.
- **States:** alarm destructive.
- **Анимации:** дефолтный M3 dialog scale/fade.

### `[REUSE] SnackbarLaunchEffect`

- **Файл:** `.../widget/SnackbarLaunchEffect.kt`
- **Назначение:** non-visual `LaunchedEffect`-обвязка; `host.showSnackbar(snackState.title)` при `snackState.show == true`; шлёт `onResetState()`.
- **Figma-параллель:** Figma `9154-86649` snack `Сохранено` (componentId `4309:44915`) с layout_0ZNDS8 — row, padding `0px 8px 0px 12px`, locationRelativeToParent x=138 y=72, fill `#19191B` (secondary), borderRadius 12px. Внутри — leading-icon + `Text("Сохранено", Desktop/Body M 15sp, color=#FFFFFF)`. Соответствует M3 Snackbar.

---

## Ассеты

| Иконка | Путь | Источник | Использование | Figma node |
|---|---|---|---|---|
| `ic_add_value` | `core/core-resources/src/main/res/drawable/ic_add_value.xml` (24x24, vector, fillColor `#ffffff`) | существует | `AddLexemeWidget` (white tint), `LexemeChipPlaceholderWidget` (primary tint) | nope (в Figma `82625` — нет лидирующего "+") |
| `ic_circle_delete` | `core/core-resources/.../ic_circle_delete.xml` (16x16, fillColor `#95989D`) | существует | `LexemeValueFieldWidget` trailing | Figma `9163:40871` = `16px/close-rounded` (близко, но разный glyph) |
| `ic_confirm` | `core/core-resources/.../ic_confirm.xml` (24x24, fillColor `#ffffff`) | существует | `LexemeValueFieldWidget` commit | нет в Figma (Figma использует текст «Сохранить») |
| `ic_close` | `core/core-resources/.../ic_close.xml` | существует | `LexemeValueFieldWidget` cancel | нет в Figma (Figma не имеет cancel-icon) |
| `ic_back` | `core/core-resources/.../ic_back.xml` | существует | `TopBarWidget` navigation | дефолтный M3 (Figma TopAppBar не разбирается) |
| `ic_more_horizonral` | `core/core-resources/.../ic_more_horizonral.xml` | существует (опечатка in name — историческая) | `LexemeTitleWidget` dropdown trigger | нет в Figma (Figma вместо menu — chip+trash) |
| `ic_delete` | `core/core-resources/.../ic_delete.xml` | существует | `DeleteWordMenuItem` | — |
| `example_ic_flag_gb` | `core/core-resources/.../example_ic_flag_gb.xml` | существует (stub) | `WordFieldWidget` | Figma `9158:72113` (componentId `4108:35935`, "United Kingdom of Great Britain and Northern Ireland") |

**TODO: импортировать из Figma (quota exceeded — на следующем прогоне):**

| Имя | Figma node | Куда подключить |
|---|---|---|
| `ic_close_rounded` (16px / close-rounded) | `9163:40871` (componentId упомянут в Lexeme variants) | Заменить `ic_circle_delete` в `LexemeValueFieldWidget.trailingIcon` |
| `ic_pencil_16` (16px / pencil) | `9158:76208` (упомянут в `9154-86353`) | Нужен если реализуем pencil-edit-режим (view-mode chip + pencil-icon) |
| `ic_chevron_down_16` (16px / chevron-down) | `9158:76281` (упомянут в `9154-86499`) | Нужен если реализуем «Добавить ещё ▾» collapse-row |
| `ic_chevron_up_16` (16x / chevron-up) | `9158:76303` (упомянут в `9154-86353`) | Симметрично, для expand |
| `ic_arrow_back_up_16` (16px / arrow-back-up) | `9158:71624` (упомянут в `9154-86499`) | Нужен для alternate "Сохранить"-кнопки c arrow-back-up |
| `ic_square_rounded_x_filled_16` (16px / square-rounded-x-filled) | `9158:72308` (упомянут в `9154-86182` / `86499`) | Нужен для inline clear-input иконки внутри Text field |
| `ic_trash_16` (16px / trash) | `9158:40710` (упомянут в variant chips view-mode) | Заменить trailing trash в Lexeme view-mode (отличается от текущего `ic_delete`) |
| `ic_minus_circle_contained_24` | `6259:67372` | в дампе используется в `chipPicker`, к фиче IS479 не относится |

**Никаких MCP `download_figma_images` вызовов на этой итерации — quota exceeded.** Импорт делается на следующем прогоне (отдельный шаг или ручной экспорт).

---

## Маппинг Figma → проект

| Figma node-id | Назначение по брифу | Содержимое (раскрыто из дампа) | Проектный виджет | Статус |
|---|---|---|---|---|
| `9154-82509` | variant state | word-card frame с Lexeme `9154:82519` в EDITING-translation-filled-state | `WordCardScreen` + `LexemeItemWidget` + `LexemeValueFieldWidget.EditRow` (без `OutlinedTextField`-стилизации) | **частично** |
| `9154-86012` | variant state | word-card frame с Lexeme `9154:86022` в EDITING-translation-empty-with-cursor | то же; покрывается `EditRow.BasicTextField` (placeholder отсутствует — Field неявно показывает только cursor) | **частично** |
| `9154-86182` | variant state | word-card frame с Lexeme `9154:86192`: row с `square-rounded-x-filled` clear-icon | `EditRow` без `square-rounded-x-filled` icon | **не сверено / TODO icon** |
| `9154-86353` | variant state | word-card frame с Lexeme `9154:86363`: view-mode chip + pencil-edit + Snackbar `Сохранено` | view-mode = `InputChip`; pencil-icon **не реализован**; snackbar реализован через `SnackbarHost` | **частично / pencil missing** |
| `9154-86499` | variant state | word-card frame с Lexeme `9154:86509`: alternate edit с arrow-back-up + «Добавить ещё ▾» | **не реализован** — это другой вариант edit-режима | **не реализован** |
| `9154-82532` | заменяемая Add-кнопка (Figma — FAB icon-only в углу) | `Show Label: false`, fill primary, layout_453IT6 x=340 y=720 | `AddLexemeWidget` — Material3 Button с **label** (расхождение visual) | **расхождение** |
| `9154-82519` | inline-механизм добавления | подробно расписан выше (Lexeme frame в `82509` parent) | `LexemeItemWidget` + `LexemeValueFieldWidget` | **частично** |
| `9154-82521` | chip «Перевод» (образец стиля) | tui-tag componentId `4632:99285`, fill primary `#4A49BC`, label onPrimary 11sp Medium, trailing `16px/close-rounded` | `InputChip` selected with `secondaryContainer` dark (**цветовая инверсия!**) + `ic_circle_delete` trailing | **расхождение колора + размера + типографики** |
| `9154-82625` | кнопки под chip-стиль (group) | layout_54DEHW row gap 12; children = tui-tag `28370:397707` outlined pill fill `#F5F3F8` border-radius 999px, label 13sp `Desktop/Body S (63:46)` color secondary | `LexemeChipPlaceholderWidget` (M3 `SuggestionChip` outlined RoundedCornerShape 8dp transparent) | **расхождение shape + color** |

---

## Открытые вопросы

1. **`AddLexemeWidget` (Figma `9154-82532`) — FAB icon-only vs inline Button с label.** Текущий код — inline Button с label «Add meaning», Figma — FAB в правом нижнем углу без label. На `86353` присутствуют **обе** сущности (FAB + inline chip-group). Решение `implement` шага: либо вернуть FAB-слот в Scaffold + сохранить inline chip-add, либо ввести оба widget'а (FAB + label-button).
2. **`LexemeValueFieldWidget.InputChip` цветовая инверсия.** Figma chip-selected = **primary brand-blue** (`#4A49BC`) с **onPrimary label** (`#FFFFFF`), 11sp Medium, height ~20dp. Код — **dark gray** (`#FF19191B`) с default M3 label 14sp, height ~32dp. **Кандидат на правку:** `InputChipDefaults.inputChipColors(selectedContainerColor=primary, selectedLabelColor=onPrimary)` + `Text(style=LexemeStyle.labelSmall)` (11sp Medium).
3. **`LexemeChipPlaceholderWidget` shape + container.** Figma — pill (`borderRadius 999px`) с fill `#F5F3F8` (light-gray = `R87IJC`). Код — M3 `SuggestionChip` 8dp outlined. **Кандидат:** `SuggestionChipDefaults.suggestionChipColors(containerColor=Color(0xFFF5F3F8))` + `shape=CircleShape` или `RoundedCornerShape(999.dp)`. Также — убрать ведущий "+" icon (Figma не показывает).
4. **`LexemeValueFieldWidget.EditRow` — структура.** Figma даёт полноценный M3 OutlinedTextField/TextField (56dp height, label-внутри, fill + stroke). Код — `BasicTextField` без рамки. **Кандидат:** заменить `BasicTextField` на `OutlinedTextField` с label.
5. **`LexemeValueFieldWidget.EditRow` save-кнопка.** Figma — единичный «Сохранить» (text-button, color `#B0B2B6` disabled / `#4A49BC` enabled). Код — пара (commit ✓ + cancel ✕). **Решение по UX:** сохранить пару (richer cancel-семантика — IS479 ничего не говорит про удаление cancel), но визуально перерисовать commit-как-текст «Сохранить» если этого требует продукт.
6. **`LexemeTitleWidget` — dropdown menu vs chip+trash.** В Figma Lexeme-header — chip "Перевод" + trailing trash-иконка (либо pencil для saved-режима), без dropdown menu. Код — текст «Value N» + dropdown menu. **Это самое сильное расхождение в архитектуре блока.** На текущей итерации остаётся as-is (backward compat); решение по перепланировке — отдельный design-вопрос.
7. **Pencil edit-режим (`9154-86353`).** Не реализован — view-mode chip без pencil-icon. Если требуется — добавить state-flag «saved» (origin == edited после успешного RefreshTranslation) → `IconBoxed(ic_pencil_16, onClick=onOpenEditMode)`.
8. **«Добавить ещё ▾/▴» collapse-row (`9154-86353`/`86499`).** Не реализован. Если требуется UX расширение — добавить state-flag `isExpanded` на лексему и `Row` с border-top + chevron-toggle.
9. **`9154-86499` alternate edit-flow.** Это другой стиль edit-режима (с `arrow-back-up` save + без chip-add ниже). Возможно, brief предполагает 2 разных режима (full-edit vs compact-edit) — требует уточнения.
10. **Typography:** проектный `LexemeStyle` использует `FontFamily.Default` (system); Figma — `Roboto`. Незаметно на Android (default = Roboto на большинстве устройств), но pixel-perfect требует явный `FontFamily(Font(R.font.roboto_regular))`. Out-of-scope IS479, фиксируется как backlog-кандидат.
11. **Accessibility:** `IconBoxed` (back, more, confirm, cancel, delete, remove) без явного `contentDescription` — сквозной regression-кандидат на уровне `core/ui/IconBoxed`.
12. **Touch target < 48dp кандидаты:** `IconBoxed(size=44dp)` (TopBar), `IconBoxed(size=24dp)` (LexemeTitle dropdown), `IconBoxed(size=18dp)` (LexemeValueField). M3 `IconButton` обеспечивает 48dp wrap независимо от visual size — `IconBoxed` должен делать то же; верификация в `design_tree`.

---

## Figma read status

- Mode: **A** (Figma + код + старая спека).
- One MCP call: `mcp__figma__get_figma_data(fileKey=w8GmGCdOZJUi99Cuv4q4W9)` → 459k-line YAML dump (parsed locally).
- Извлечено node-id'ов: **9 из 9** (все target frames найдены).
- Иконок к импорту: **7** (помечены `TODO`, quota exceeded — следующий прогон).

---

## Лог итераций

### ит.2 (2026-05-22T11:00:00-0600)

Rerun с Figma источником (Mode A). Один MCP-вызов — `get_figma_data(fileKey)` без `nodeId`, локальный YAML dump ~19MB. Извлечение target frames из локального файла grep'ом + Read offset/limit.

- Прочитано 9 node-id'ов: **9** (все 5 variant states + 4 chip/button referencs).
- Найдены layouts: `T3JJ2H` (412×892 word card), `7DWQ07` (screen_content), `UYS723` (cards 380w gap 8), `LEN7RP` (word header), `RVILFH` (Lexeme column gap 12 padding 16), `IM161Z`/`QB6X0B`/`9MJ3UK` (variant Lexeme outer), `RBJAXR`/`WSLMRI`/`B17IMZ`/`1Q8R2L` (sub-rows), `52S1OH`/`4E1NW5`/`MRY035` (chip variants), `MHMDYU` (Удалить-block), `AZA3I3`/`01N4DF` («Добавить ещё»-row), `LP0NHO` (16dp icons), `FD7GWA` (cursor row), `Q5MY7S` (cursor 1×16), `453IT6` (FAB icon-only).
- Найдены colors: `#FFFFFF` (QMOUFY=primary+onPrimary on chip), `#F5F3F8` (R87IJC=background+chip-outline), `#19191B` (1N7XCC=secondary text), `#4A49BC` (62UVHJ=primary brand), `#7B7E85` (KRTAD5=gray text), `#95989D` (95989D=unselected — не найден среди styles, есть в коде), `#B0B2B6` (9PL09E=disabled), `#DE2424` (GHSSZ8=onError), `#E4E5E7` (NP52E5=divider), `#FFFFFF` cursor.
- Найдены typography: `Desktop/Heading 5` 24sp Medium Roboto, `Desktop/Body L` 17sp Normal Roboto, `Desktop/Body M` 15sp Normal Roboto, `Desktop/Body S` 13sp Normal Roboto, `Desktop/Body S (63:46)` 13sp Normal Roboto, `Desktop/Body XS (63:48)` 11sp Medium Roboto letter-spacing 3.64%, `Desktop/Body XL Bold` 19sp Bold Roboto.
- Иконок к импорту: **7** (TODO, quota exceeded — следующий прогон).
- Виджеты: **[NEW] 0**, **[REUSE] 13** (включая non-visual `SnackbarLaunchEffect`).
- **Главные расхождения текущий код vs Figma (приоритет для `implement`):**
  1. `LexemeValueFieldWidget.InputChip` view-mode — цветовая инверсия (dark vs brand-blue) + размер (32dp vs 20dp) + label-typo (14sp vs 11sp Medium).
  2. `LexemeChipPlaceholderWidget` — shape (8dp vs 999px pill) + container (transparent vs `#F5F3F8`) + лишний "+" icon.
  3. `LexemeValueFieldWidget.EditRow` — `BasicTextField` без рамки vs Figma full `OutlinedTextField` 56dp с label.
  4. `LexemeValueFieldWidget.EditRow` save-кнопка — пара commit+cancel icon'ов vs Figma «Сохранить» текст-кнопка.
  5. `AddLexemeWidget` — inline Button с label vs Figma FAB icon-only в правом нижнем углу.
  6. `LexemeTitleWidget` — dropdown menu vs Figma chip "Перевод" + trash-icon (или pencil) внутри блока.
  7. Не реализованы: pencil-edit для view-mode-saved, «Добавить ещё ▾/▴» collapse-row, alternate edit-flow `9154-86499`, `square-rounded-x-filled` clear-icon в текстовом поле.

### ит.1 (предыдущий run, archived)

Mode B (код + старая спека) — Figma недоступна из-за rate limit. Архив: `.archive_run1/`. Артефакт был code-derived snapshot без Figma-сверки.
