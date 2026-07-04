# UI Layout — wordcard

Финальный snapshot UI карточки слова: inline-механизм редактирования + создания лексемы (фича IS479), snackbar+undo для удалений, callbacks-only виджеты.

Источники: Figma frame `9154:82509`, текущий код `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/`.

## 📋 ЧТО ДЕЛАЕМ

- **Bottom-sheet добавления лексемы отсутствует.** Лексема создаётся локально (`NOT_IN_DB`) сразу по тапу FAB; реальный INSERT в БД — при первом Commit Translation/Definition (через `commitAndCloseAllEdits` либо `onFocusLost`).
- **Inline-редактирование с auto-commit:** тап по тексту chip → edit-mode (BasicTextField), потеря фокуса → автоматический commit/remove. Никаких отдельных кнопок «сохранить»/«отменить».
- **Snackbar+undo для удалений:** delete translation/definition/lexeme → snackbar "X удалён" с кнопкой «Отменить» (4 секунды через M3 `SnackbarDuration.Short`).
- **AddLexeme** — `M3 FloatingActionButton` icon-only в `Scaffold.floatingActionButton`.
- **Subentity-добавление** через chip-плейсхолдер (`+ Перевод` / `+ Определение`) в нижней части карточки лексемы — единый `SubentityChip` (`Surface` + `Row`).
- **Удаление лексемы** — `DeleteLexemeButton` (footer-action TextButton с trash-icon). Confirm-dialog показывается только для real-лексемы; для пустой `NOT_IN_DB` — удаление сразу.
- **Глобальная UI-блокировка** через `state.isPendingDbOp` — интерактивы передают `enabled = !isPendingDbOp` вниз. FAB использует computed `state.canAddLexeme`.
- **Callbacks-only виджеты** — никакой `sendMessage: (Msg) -> Unit` в Composable-функциях виджетов; UI-события идут через плоские callback'и, диспатчинг в `Msg` живёт только на уровне `WordCardScreen`.

> 📎 guide: docs/handbook/guides/state-and-extensions.md — "`NOT_IN_DB = -1L` для неинициализированных ID"
>
> 📎 guide: docs/handbook/guides/ui-patterns.md — раздел "Виджеты получают callbacks, не `sendMessage`"

## 🚷 ВНЕ СКОУПА

- **WordField (`9158:72107`).** Используется существующий `Surface` + `LexemeEditableText` для заголовка карточки. Точечная подгонка типографики под Figma вне скоупа IS479. DRIFT 🚨.
- **Расширенный TopBar (Figma "with btn").** Не применяется — закрытие word edit идёт через `onFocusLost` (auto-commit). DRIFT 🚨.

## 🏷 ЛЕГЕНДА

- **⚙️** — системный Material3 / Compose (`Scaffold`, `Column`, `Row`, `Surface`, `InputChip`, `SuggestionChip`).
- **❇️** — новый кастомный виджет (введён в IS479).
- **🔄** — кастомный, меняется в этой фиче.
- **📌** — кастомный, не меняется в IS479.

## 🗺 Карта экрана

```
⚙️ Scaffold                                                 containerColor=Transparent
├─ topBar = 🔄 <TopBarWidget>                               topBarState, onBackPress, onOpenMenu, onCloseMenu, onDeleteWord
├─ snackbarHost = ⚙️ SnackbarHost                           hostState=snackbarHostState (используется UiHostImpl)
├─ floatingActionButton = if (state.isLoaded) 🔄 <AddLexemeWidget>   enabled=state.canAddLexeme, onAddLexeme
└─ ⚙️ Box                                                   padding=Scaffold.padding, imePadding+navigationBarsPadding,
                                                            clickable→focusManager.clearFocus()
   └─ if (state.wordState is WordState.Loaded):
      └─ ⚙️ Column                                          fillMaxHeight, background=tertiary, padding=h:16, v:16, verticalScroll
         ├─ 🔄 <WordFieldWidget>                            loaded, enabled, onValueChange, onOpenEditMode, onCommit
         ├─ ⚙️ Spacer h=8
         └─ ⚙️ Column                                       fillMaxWidth, verticalArrangement=spacedBy(8.dp)
            └─ state.lexemeList.forEach { lexemeState ->    key(lexemeState.id)
                 ❇️ <LexemeCard>                           content slot
                 {
                   lexemeState.translation?.let { translation ->
                     ❇️ <LexemeMeaningField>               labelRes=translation, state, enabled,
                                                            onValueChange, onOpenEditMode, onCommitEdit, onRemove
                     Spacer h=12
                   }
                   lexemeState.definition?.let { definition ->
                     ❇️ <LexemeMeaningField>               labelRes=definition, state, enabled, ... callbacks
                     Spacer h=12
                   }
                   if (canAddTranslation || canAddDefinition):
                     ❇️ <AddLexemeMeaningRow>              canAddTranslation, canAddDefinition, enabled,
                                                            onCreateTranslation, onCreateDefinition
                   ❇️ <DeleteLexemeButton>                 enabled, onClick → Msg.OpenDeleteLexemeDialog
                 }
               }

⚙️ if (state.wordState is WordState.Loaded && state.wordState.showWarningDialog):
└─ 📌 <ConfirmDeleteWordWidget>                            onConfirm, onDismiss

⚙️ state.lexemeIdPendingDelete?.let { pendingId ->
└─ 📌 <ConfirmDeleteLexemeWidget>                          onConfirm, onDismiss
}
```

> 📎 guide: docs/handbook/guides/ui-patterns.md — "AppBar никогда не пишется inline в Scaffold"
>
> 📎 guide: docs/handbook/guides/ui-patterns.md — "рендеринг списков через `key()` для стабильной рекомпозиции"
>
> 📎 guide: docs/handbook/guides/ui-patterns.md — "условные оверлеи (диалоги) рендерятся по флагам стейта"

## 🔍 Анализ виджетов

```
🔄 <TopBarWidget>                                          ⟦container+icon⟧
   • type:        M3 TopAppBar
   • slots/content:
       – navigation: IconBoxed (ic_back, size=44)
       – title: ⊘
       – actions: IconDropdownWidget(⋮) → DeleteWordMenuItem
   • params:
       – topBarState: TopBarState
       – onBackPress: () -> Unit
       – onOpenMenu: () -> Unit
       – onCloseMenu: () -> Unit
       – onDeleteWord: () -> Unit
   • callbacks (на сайте вызова в WordCardScreen):
       – onBackPress → sendMessage(Msg.NavigateBack)
       – onOpenMenu → sendMessage(Msg.OpenTopBarMenu)
       – onCloseMenu → sendMessage(Msg.CloseTopBarMenu)
       – onDeleteWord → sendMessage(Msg.CloseTopBarMenu); sendMessage(Msg.OpenDeleteWordDialog)
   • source:      figma 9154:82531
```

```
🔄 <WordFieldWidget>                                        ⟦container+input⟧
   • type:        M3 Surface (Card) + Column + LexemeEditableText
   • size:        width=fill × height=hug
   • padding:     all=16
   • shape:       borderRadius=12, shadowElevation=4
   • colors:
       – background: M3 default Surface color (MaterialTheme.colorScheme.surface)
       – content title: blackColor
       – meta: grayTextColor + secondary
   • typography:
       – title: LexemeStyle.H5
       – meta label: LexemeStyle.BodyM
       – meta value: LexemeStyle.BodyMBold
   • slots/content:
       – LexemeEditableText (originValue, changedValue, isEditMode, onFocusLost)
       – Row (Text "Добавлено" + Text date)
       – ImageFlagWidget (BottomEnd)
   • params:
       – loaded: WordState.Loaded
       – enabled: Boolean (= !state.isPendingDbOp)
       – onValueChange: (String) -> Unit
       – onOpenEditMode: () -> Unit
       – onCommit: () -> Unit
   • callbacks (на сайте вызова):
       – onValueChange → sendMessage(Msg.UpdateWordInput(value))
       – onOpenEditMode → sendMessage(Msg.EnterWordEditMode)
       – onCommit → sendMessage(Msg.CommitWordChanges)
   • behavior:
       – Auto-commit на потере фокуса: LexemeEditableText.onFocusLost → onCommit().
       – Cancel-trigger отсутствует.
       – Edit блокируется при !enabled.
   • source:      figma 9158:72107
```

```
❇️ <LexemeCard>                                             ⟦container slot⟧
   • type:        M3 Surface (Card) + Column + торцевые Spacer'ы (slot-pattern)
   • size:        width=fill × height=hug
   • padding:     horizontal=16
   • shape:       borderRadius=12, shadowElevation=4
   • colors:
       – background: whiteColor (#FFFFFF)
   • slots/content:
       – content: @Composable ColumnScope.() -> Unit (trailing lambda)
       – верхний Spacer h=12 и нижний Spacer h=4 — внутри обёртки
   • params:
       – content: @Composable ColumnScope.() -> Unit
   • behavior:
       – 0 callbacks, чистый layout-контейнер. Содержимое лексемы (translation/definition/AddRow/DeleteButton)
         собирается на сайте вызова в WordCardScreen.
   • notes:
       ℹ️ Заменяет монолитный LexemeItemWidget (удалён при декомпозиции). По правилу гайда
         "callbacks > 5-7 → декомпозируй": вместо 12 callbacks в одном виджете — slot + sub-widgets.
   • source:      figma 9154:82519 (name="Lexeme", layout=layout_RVILFH) — single Surface/Card
```

```
❇️ <LexemeMeaningField>                                     ⟦container+chip+input⟧
   • type:        Compose Column (project wrapper)
   • size:        width=fill × height=hug
   • spacing:     itemSpacing=4
   • slots/content:
       – content[0]: <SubentityChip> (state=Active — chip-заголовок с trailing ✕)
       – content[1]: LexemeEditableText (inline view/edit поле)
   • params:
       – labelRes: Int (@StringRes) — word_card_bottom_translation / _definition для chip-Active label
       – state: TextValueState (origin, edited, isEdit)
       – enabled: Boolean
       – onValueChange: (String) -> Unit
       – onOpenEditMode: () -> Unit
       – onCommitEdit: () -> Unit
       – onRemove: () -> Unit
   • behavior:
       – Один LexemeMeaningField = одна активная sub-сущность.
       – Тап ✕ chip-Active → onRemove().
       – Потеря фокуса в edit-mode (LexemeEditableText.onFocusLost):
           · если value.isEmpty() → onRemove();
           · иначе → onCommitEdit().
       – Reducer-side обработка onRemove:
           · NOT_IN_DB → локальный nullify (cascade если оба subentity = null);
           · real-id с empty origin → локальный nullify без эффекта (баг-фикс);
           · real-id с непустым origin → DatasourceEffect.Remove* + snackbar+undo от handler'а.
   • notes:
       ℹ️ Project decision: проектная обёртка над chip-Active + поле ввода.
```

```
🔄 <LexemeEditableText>                                     ⟦text|input⟧   (в core/ui/text/base/)
   • type:        view-mode: Text; edit-mode: BasicTextField
   • size:        width=fill × height=hug
   • shape:       без рамки
   • colors:
       – text: textColor (передаётся вызывающим)
   • typography:  LexemeStyle.BodyL (default) либо передаётся через textStyle
   • slots/content:
       – view-mode: Text(originValue, modifier.clickable→onOpenEditMode)
       – edit-mode: BasicTextField(value=fieldValue, onValueChange, modifier.focusRequester+onFocusChanged)
   • params:
       – originValue: String
       – changedValue: String
       – isEditMode: Boolean
       – onTextChange: (String) -> Unit
       – onOpenEditMode: () -> Unit
       – onFocusLost: (currentValue: String) -> Unit  (non-nullable)
       – textColor: Color
       – textStyle: TextStyle = defaultTextStyle
   • behavior:
       – Тап по тексту в view-mode → onOpenEditMode().
       – При isEditMode=true автофокус через FocusRequester.requestFocus().
       – LaunchedEffect(changedValue) re-sync TextFieldValue если origin меняется снаружи;
         cursor offset сохраняется при parent-трансформации (selection = TextRange(minOf(oldOffset, length))).
       – Потеря фокуса (onFocusChanged) → onFocusLost(fieldValue.text).
   • notes:
       ℹ️ Никаких отдельных commit/cancel icons — всё через potery focus.
       ℹ️ onFocusLost non-nullable (PrimaryEditableWidget удалён, default не нужен).
```

```
❇️ <SubentityChip>                                          ⟦chip+icon⟧
   • type:        Единый Surface + Row + Icon + Text. Один Composable для Placeholder и Active
                  (отличаются только iconRes/labelRes на сайте вызова).
   • size:        width=hug × height=hug
   • shape:       RoundedCornerShape(6.dp)
   • colors:      MaterialTheme.colorScheme.primary / onPrimary
   • params:
       – labelRes: Int (@StringRes)
       – iconRes: Int (@DrawableRes)
       – enabled: Boolean
       – onClick: () -> Unit
   • slots/content:
       – Icon(painterResource(iconRes), size=16) (androidx.compose.material3.Icon)
       – Text(stringResource(labelRes))
   • behavior:
       – Placeholder используется в AddLexemeMeaningRow для активации (iconRes = ic_add).
       – Active используется в LexemeMeaningField как chip-заголовок (iconRes = ic_close для ✕).
   • notes:
       ℹ️ Project decision: один Compose-виджет, две роли. Семантика передаётся через iconRes (ic_add | ic_close).
```

```
❇️ <AddLexemeMeaningRow>                                    ⟦row+chips⟧
   • type:        Column { HorizontalDivider + Spacer + FlowRow }
   • size:        width=fill × height=hug
   • slots/content:
       – HorizontalDivider (33% width, alignEnd, primary alpha=0.3)
       – Spacer h=12
       – FlowRow (gap=12, Alignment.End, spacedBy 8) с placeholder SubentityChip'ами
       – Spacer h=6
   • params:
       – canAddTranslation: Boolean
       – canAddDefinition: Boolean
       – enabled: Boolean
       – onCreateTranslation: () -> Unit
       – onCreateDefinition: () -> Unit
   • behavior:
       – Рендерится только когда canAddTranslation || canAddDefinition (есть что добавлять).
       – Каждый chip отображается только если соответствующий canAdd*.
   • notes:
       ℹ️ Имя `AddLexemeMeaningRow` — семейство с `LexemeMeaningField` (показ ↔ добавление).
```

```
❇️ <DeleteLexemeButton>                                     ⟦button+icon⟧
   • type:        M3 TextButton с leading icon
   • size:        width=hug × M3 default height (MinHeight ~40dp)
   • padding:     horizontal=4
   • shape:       M3 TextButton default
   • colors:
       – content (icon+text): MaterialTheme.colorScheme.onError (#DE2424)
   • typography:  LexemeStyle.BodyS
   • slots/content:
       – leading: Icon(ic_trash, 16×16)
       – content: Text(R.string.word_card_lexeme_remove "Удалить")
   • params:
       – enabled: Boolean
       – onClick: () -> Unit
   • callbacks (на сайте вызова):
       – onClick → sendMessage(Msg.OpenDeleteLexemeDialog(lexemeState.id))
   • behavior:
       – Disabled при !enabled.
       – Footer-action внутри LexemeCard.
       – Reducer открывает ConfirmDeleteLexemeWidget для real-лексемы;
         для пустой NOT_IN_DB — удаление сразу без диалога.
   • source:      figma 9162:40713
```

```
🔄 <AddLexemeWidget>                                        ⟦FAB⟧
   • type:        M3 FloatingActionButton icon-only в Scaffold.floatingActionButton
   • size:        M3 FAB default (56dp)
   • shape:       FloatingActionButtonDefaults.shape (RoundedCornerShape 16)
   • colors:
       – containerColor: MaterialTheme.colorScheme.primary (#4A49BC)
       – contentColor: onPrimary
   • slots/content:
       – icon: Icon(ic_add_value, size=24, contentDescription=word_card_add_lexeme)
   • params:
       – enabled: Boolean (= state.canAddLexeme)
       – onAddLexeme: () -> Unit
   • behavior:
       – enabled блокирует двойное создание (вычисляется как !isPendingDbOp && !isCreatingLexeme через computed `state.canAddLexeme`).
       – Disabled-визуал через Modifier.alpha(if (enabled) 1f else 0.38f);
         onClick guard'ится `{ if (enabled) onAddLexeme() }` — M3 FAB не имеет нативного `enabled`.
   • notes:
       🚨 DRIFT: M3 FAB не имеет нативного `enabled` параметра — реализовано через alpha + no-op onClick.
   • source:      figma 9154:82532
```

```
🔄 <ConfirmDeleteWordWidget>                                ⟦dialog⟧
   • type:        AlarmDialogWidget (project)
   • params:
       – onConfirm: () -> Unit
       – onDismiss: () -> Unit
   • callbacks (на сайте вызова):
       – onConfirm → sendMessage(Msg.RemoveWord(state.wordState.id))
       – onDismiss → sendMessage(Msg.CloseDeleteWordDialog)
   • behavior:
       – Рендерится только при wordState is Loaded && wordState.showWarningDialog == true.
```

```
🔄 <ConfirmDeleteLexemeWidget>                              ⟦dialog⟧
   • type:        AlarmDialogWidget (project)
   • params:
       – onConfirm: () -> Unit
       – onDismiss: () -> Unit
   • callbacks (на сайте вызова):
       – onConfirm → sendMessage(Msg.RemoveLexeme(pendingLexemeId))
       – onDismiss → sendMessage(Msg.CloseDeleteLexemeDialog)
   • behavior:
       – Рендерится только при state.lexemeIdPendingDelete != null.
       – Reducer не открывает диалог для пустой NOT_IN_DB-лексемы — она удаляется сразу.
```

## ❇️ КЛЮЧЕВЫЕ ВИДЖЕТЫ ФИЧИ

- **`LexemeCard`** — slot-обёртка карточки лексемы (Surface + Column + торцевые Spacer). 0 callbacks. Содержимое собирается на сайте вызова в WordCardScreen.
- **`LexemeMeaningField`** — wrapper (Column) над chip-Active + LexemeEditableText. Один LexemeMeaningField = одна активная sub-сущность.
- **`AddLexemeMeaningRow`** — блок placeholder-chip'ов (Divider + FlowRow) для добавления отсутствующих sub-сущностей.
- **`SubentityChip`** — единый виджет в двух state'ах (Placeholder для FlowRow / Active для chip-заголовка). 4 callback'а каждый со своей семантикой.
- **`DeleteLexemeButton`** — footer-action TextButton с trash-icon.
- **`UiHost`** + **`UiHostImpl`** (`widget/internal/`) — абстракция для one-shot UI-уведомлений (snackbar). Инжектится в ViewModel через `@AssistedInject`.

## 🔧 МЕНЯЕМ (ключевое)

- **`AddLexemeWidget`** — `M3 FloatingActionButton` icon-only в `Scaffold.floatingActionButton`. Параметр `modifier` удалён (никто не передаёт).
- **`LexemeCard`** заменяет `LexemeItemWidget` — slot-pattern вместо монолитного виджета с 12 callback'ами.
- **`LexemeEditableText`** — `onFocusLost: (String) -> Unit` non-nullable (раньше был nullable default). Никаких commit/cancel icons внутри — всё через potery focus.
- **`TopBarWidget`** — плоские callbacks (`onBackPress, onOpenMenu, onCloseMenu, onDeleteWord`) вместо `sendMessage: (Msg) -> Unit`.
- **`WordFieldWidget`** — плоские callbacks (`onValueChange, onOpenEditMode, onCommit`); `isPendingDbOp` → `enabled` (widget-level контракт).
- **`ConfirmDelete*Widget`** — плоские callbacks (`onConfirm, onDismiss`); параметры `loaded`/`lexemeId` удалены (id уезжает наверх через callback-builder).

## ❌ УДАЛЯЕМ (с миграцией)

- **`AddLexemeBottomWidget`** + bottom-sheet модель → `AddLexemeWidget` (FAB) + `AddLexemeMeaningRow` (subentity placeholder).
- **`LexemeItemWidget`** (монолит) → `LexemeCard` (slot) + sub-widgets собираются в WordCardScreen.
- **`LexemeValueFieldWidget`** (chip + input + commit/cancel icons) → `LexemeEditableText` напрямую в `LexemeMeaningField`.
- **`LexemeTitleWidget`** + ⋮-меню лексемы → `DeleteLexemeButton` (footer-action).
- **`LexemeChipPlaceholderWidget`** → `SubentityChip` с двумя state'ами.
- **`ActionsWidget`** (commit/cancel icons) → удалён (auto-commit на focus loss).
- **`PrimaryEditableWidget`** + **`EditableText`** — dead виджеты, не использовались.
- **`SnackbarLaunchEffect`** + `SnackbarState` + `Msg.ShowNotification(String)` + `Msg.DismissNotification` → миграция на `UiHost` / `UiEffect.ShowErrorSnackbar` / `Msg.ShowError(@StringRes)`.
- **`Msg.OpenLexemeMenu`** + `LexemeState.isMenuOpen` + ⋮-меню лексемы → удалены вместе с `LexemeTitleWidget`. Удаление переехало в `DeleteLexemeButton`.
- **`Msg.Cancel*Edit`** → удалены. Отмена реализована через `onFocusLost` с пустым значением → `onRemove()`.
- **`Msg.ExitWordEditMode`** → удалён. Закрытие word edit через `CommitWordChanges` (auto-commit на focus loss).
- **`Msg.LexemeCascadeRemoved`** (legacy без undo) → удалён; используется только `LexemeCascadeRemovedWithUndo`.

> 📎 guide: docs/handbook/guides/ui-patterns.md — раздел "Виджеты получают callbacks, не `sendMessage`"
>
> 📎 guide: docs/handbook/guides/data-layer.md — раздел "Нормализация текстового ввода (trim)"

## 🖼 ИКОНКИ

- `ic_add_value` — leading icon в `AddLexemeWidget` (FAB).
- `ic_add` — leading icon в `SubentityChip` Placeholder.
- `ic_close` — `material3.Icon` в `SubentityChip` Active (trailing ✕).
- `ic_trash` — leading icon в `DeleteLexemeButton`.
- `ic_back` — navigation icon в `TopBarWidget`.

Все иконки в `core/core-resources/src/main/res/drawable/`.

## 🆕 UX-СЦЕНАРИИ

- **Тап FAB «Add lexeme»**: создаётся локальная `LexemeState(NOT_IN_DB, ...)` в **начало** списка (newest-first). `state.canAddLexeme` становится `false` до завершения локального создания. БД INSERT ещё не происходит.
- **Тап `+ Перевод` / `+ Определение`**: chip-placeholder заменяется на `LexemeMeaningField` сразу в edit-mode. INSERT в БД — при первом коммите (focus loss с непустым value либо `commitAndCloseAllEdits`).
- **Inline-edit + auto-commit**: тап по тексту chip → edit-mode (BasicTextField, autofocus). Потеря фокуса:
  - value пуст → `onRemove()` → reducer: для NOT_IN_DB — локальный nullify (+ cascade-check); для real с empty origin — локальный nullify; для real с непустым origin — `DatasourceEffect.Remove*` → handler → `*Deleted` Msg → snackbar+undo.
  - value не пуст → `onCommit()` → reducer ветвление 1/2/3.
- **Snackbar+undo для всех удалений** (translation / definition / lexeme / cascade):
  - 4 секунды M3 `SnackbarDuration.Short`;
  - кнопка «Отменить» отправляет соответствующий `UndoRemove*` Msg → reducer формирует `DatasourceEffect.UpdateLexeme*` (re-INSERT для cascade-случая через `lexemeId = null`) либо `RestoreLexeme`.
- **Single-active-edit invariant**: открытие нового edit (`Enter*EditMode`, `Create*`, `CreateLexeme`, `EnterWordEditMode`) вызывает `commitAndCloseAllEdits()` — все остальные активные edit'ы коммитятся / закрываются атомарно. Pending edits не теряются.
- **isPendingDbOp как UI-блокировщик**: при любой исходящей DB-операции `isPendingDbOp=true` → все интерактивы disabled. Снимается при возврате любого Datasource Msg.
- **DeleteLexemeButton для пустой NOT_IN_DB**: confirm-dialog не открывается, лексема удаляется сразу (нечего подтверждать).

> 📎 guide: docs/handbook/guides/state-and-extensions.md — "`NOT_IN_DB = -1L`; локальная LexemeState до INSERT в БД"
>
> 📎 guide: docs/handbook/guides/effect-handlers.md — "DatasourceEffect — операции с БД на Dispatchers.IO; результат → Msg обратно в Reducer"

## 🎨 ПАЛИТРА

- `MaterialTheme.colorScheme.primary` — primary action (icon в placeholder chip, FAB container, SubentityChip Active background). #4A49BC.
- `MaterialTheme.colorScheme.onPrimary` — content на primary.
- `MaterialTheme.colorScheme.secondary` — text edit-mode, date в WordFieldWidget.
- `MaterialTheme.colorScheme.tertiary` — фон Column-обёртки content + divider.
- `MaterialTheme.colorScheme.onError` — цвет icon+text в `DeleteLexemeButton`. #DE2424.
- `whiteColor` (`me.apomazkin.theme.whiteColor`) — фон Surface(Card) `LexemeCard`. `WordFieldWidget` использует M3 default surface.
- `blackColor` — текст заголовка слова.
- `grayTextColor` — meta-label в WordFieldWidget.

> 📎 guide: docs/handbook/guides/theme-and-resources.md — "Material3 цвет → LexemeColor + darkColorScheme()"
