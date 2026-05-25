---
status: done
---

# Summary — IS479 UI sub-flow

Sub-flow `ui` для фичи `wordcard_lexeme_inline` (IS479 — карточка слова с inline-механикой создания лексемы).
Корневая директория артефактов: `docs/features/IS479_wordcard_lexeme_inline/ui/`.

## Что сделано

### design_tree.md v2 (2 итерации: 1 + conductor patch на review findings)

DAG из 11 узлов:
- **3 [-] удаления:** `AddLexemeBottomWidget.kt`, `LexemeMeaningWidget.kt`, `ActionsWidget.kt` (весь bottom-sheet flow ликвидирован, директория `widget/addlexeme/` опустошена).
- **1 [+] создание:** `LexemeChipPlaceholderWidget.kt` — chip `+ Перевод` / `+ Определение` по Figma `9154-82521`/`82625` через Material3 `SuggestionChip`.
- **7 [~] изменения:** `ConfirmDeleteWordWidget`, `WordFieldWidget`, `AddLexemeWidget` (FAB → inline-кнопка по Figma `9154-82532`), `LexemeValueFieldWidget` (chip-edit с `Commit/Cancel`), `LexemeTitleWidget` (+ `isPendingDbOp`), `LexemeItemWidget` (chip-placeholder для null subentity + divider только когда оба заполнены), `WordCardScreen` (корень — sealed `WordState` cast в одном месте).

Conductor-патч ит.2 после review F-arch-1/2/3 (Preview compile-break + UiMsg import): фиксирует обязанность каждого `[~]` узла обновить Preview-блоки под новые сигнатуры/типы.

### implement.md v2 (2 итерации: основная имплементация + conductor patch на senior critical)

**11 файлов затронуто:**
- 3 удалено: `AddLexemeBottomWidget.kt`, `LexemeMeaningWidget.kt`, `ActionsWidget.kt` + пустой каталог `widget/addlexeme/`.
- 1 создано: `widget/lexeme/LexemeChipPlaceholderWidget.kt` — `SuggestionChip` с плюс-иконкой.
- 7 модифицировано:
  - `WordCardScreen.kt` — sealed `WordState` cast `as? WordState.Loaded` в корне (одна точка cast), удалён `import me.apomazkin.wordcard.mate.UiMsg`, `UiMsg.ShowNotification(false)` → `Msg.DismissNotification`, AddLexemeBottomWidget блок удалён. `enabled = !state.isPendingDbOp && !state.isCreatingLexeme` на FAB. Удалён импорт `mate.isCreatingLexeme` (теперь field, не extension).
  - `WordFieldWidget.kt` — параметр `loaded: WordState.Loaded` (не `state: WordState`), доступ к `loaded.value/edited/isEditMode` без cast.
  - `ConfirmDeleteWordWidget.kt` — параметр `loaded: WordState.Loaded`.
  - `AddLexemeWidget.kt` — переписан с FAB на inline-`Button` по Figma `9154-82532`.
  - `LexemeValueFieldWidget.kt` — chip-style view (`InputChip`) + edit mode (`BasicTextField` с `TextFieldValue`). `onCommitEdit(value)` / `onCancelEdit()` / `onRemove()` callbacks. ит.2: `DisposableEffect(Unit) { requestFocus() }` → `LaunchedEffect(Unit) { requestFocus() }`. `BasicTextField(value: String)` → `BasicTextField(value: TextFieldValue)` с локальным `var fieldValue by remember { mutableStateOf(TextFieldValue(value, selection = TextRange(value.length))) }` + sync через `LaunchedEffect(value)`.
  - `LexemeTitleWidget.kt` — добавлен параметр `isPendingDbOp: Boolean` для блокировки menu.
  - `LexemeItemWidget.kt` — рендерит `LexemeChipPlaceholderWidget` если суб-сущность null, иначе `LexemeValueFieldWidget`. `HorizontalDivider` только когда оба `translation` и `definition` имеют значение.

### Conductor-патч ит.2 (senior critical fixes)

- **F-snr-1 critical (DisposableEffect → LaunchedEffect):** `EditRow` в `LexemeValueFieldWidget`. Side-effect в composition phase — антипаттерн. Заменено на `LaunchedEffect(Unit) { focusRequester.requestFocus() }`.
- **F-snr-2 critical (BasicTextField TextFieldValue):** `value: String` теряет cursor/selection при recomposition. Заменено на локальный `TextFieldValue`-state с двусторонней синхронизацией. Сохраняет cursor position при edit.
- **`isCreatingLexeme` extension val → real field в State.kt:**
  - В `WordCardState` добавлено поле `val isCreatingLexeme: Boolean = false`.
  - Удалена `val WordCardState.isCreatingLexeme` extension property.
  - Добавлен helper `internal fun WordCardState.withCreatingFlag(): WordCardState` (вычисление из `lexemeList.any { it.id == NOT_IN_DB }`).
  - В `WordCardReducer.reduce` обёртка: `val (newState, effects) = reduceImpl(state, message); return newState.withCreatingFlag() to effects` — автоматическая синхронизация поля с lexemeList после любой ветки.
  - Соответствует project rule из MEMORY.md «explicit state flags, not computed in composable».

### Compile-status

`./gradlew :modules:screen:wordcard:compileDebugKotlin` → **BUILD SUCCESSFUL** после всех правок. Тесты будут запускаться на шаге `check` parent flow.

## Ключевые решения

1. **Sealed `WordState` cast в одном месте (root composable).** `WordCardScreen.kt` делает `val loaded = state.wordState as? WordState.Loaded` один раз и прокидывает уже-cast `loaded` дочерним виджетам. Убирает дублирование `as?`-checks по дереву виджетов.

2. **Локальный `TextFieldValue` state в `EditRow` (`LexemeValueFieldWidget`).** Сохраняет cursor/selection при recomposition. Sync с external `value` через `LaunchedEffect(value)` (срабатывает только при действительном изменении).

3. **`isCreatingLexeme` как explicit field, не computed.** Reducer-обёртка `.withCreatingFlag()` синхронизирует поле с `lexemeList` после любой ветки. UI читает `state.isCreatingLexeme` без вычисления — соответствует MEMORY правилу.

4. **`HorizontalDivider` только когда оба subentity заполнены.** Свежий NOT_IN_DB-лексеме (оба placeholder) — без divider (Figma `9154-82519`).

5. **`SuggestionChip` для chip-placeholder.** Material3 — прецедент в `ChipPickerWidget.kt`. Принимает `enabled = !isPendingDbOp` для блокировки во время DB-операции.

## Артефакты

- `docs/features/IS479_wordcard_lexeme_inline/ui/design_tree.md` (v2)
- `docs/features/IS479_wordcard_lexeme_inline/ui/design_tree_review.md`
- `docs/features/IS479_wordcard_lexeme_inline/ui/impl.md`
- `docs/features/IS479_wordcard_lexeme_inline/ui/plan.yml`

Production-файлы (всего 11) в `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/`:
- `WordCardScreen.kt` [~]
- `widget/WordFieldWidget.kt` [~]
- `widget/ConfirmDeleteWordWidget.kt` [~]
- `widget/AddLexemeWidget.kt` [~]
- `widget/lexeme/LexemeValueFieldWidget.kt` [~]
- `widget/lexeme/LexemeTitleWidget.kt` [~]
- `widget/lexeme/LexemeItemWidget.kt` [~]
- `widget/lexeme/LexemeChipPlaceholderWidget.kt` [+]
- `widget/addlexeme/AddLexemeBottomWidget.kt` [-]
- `widget/addlexeme/LexemeMeaningWidget.kt` [-]
- `widget/addlexeme/ActionsWidget.kt` [-]

Также: `mate/State.kt` (added field `isCreatingLexeme`, helper `withCreatingFlag()`), `mate/WordCardReducer.kt` (reducer обёртка с `.withCreatingFlag()`) — формально business-слой, но патч во время UI sub-flow по review feedback.

## Tech debt / backlog

- **Cancel-путь `Msg.ExitWordEditMode` без UI-trigger** — `LexemeEditableText` имеет только `onCloseEditMode` → `CommitWordChanges`. Cancel-кнопка для word edit отсутствует. Accepted out-of-scope IS479.
- **Loading overlay** — при `isLoading = true && wordState is NotLoaded` UI рендерит чистый Scaffold без spinner. Figma `9154-82509` явно показывает spinner-state. Backlog UX-тикет.
- **InputChip(selected = true) антипаттерн API** — всегда-selected chip. Можно заменить на кастомный Row либо `AssistChip`. Косметика.
- **Modifier-параметр позиция** — несколько виджетов имеют `modifier` не первым optional либо его нет. Compose API guideline. Косметика.

## Метрика итераций

| Шаг          | Итераций | Заметка |
|--------------|----------|---------|
| design_tree  | 2        | 1 + conductor patch F-arch-1/2/3 (Preview compile-break, UiMsg import). |
| implement    | 2        | 1 + conductor patch на senior critical (DisposableEffect, TextFieldValue, isCreatingLexeme field). |
| summary      | 1        | — |

Итого: **3 шага, 5 итераций**.

## Вне scope (для master flow)

- **Final check** sub-flow — `lintDebug → testDebugUnitTest → assembleDebug` на полном проекте. Тесты business sub-flow теперь должны прогоняться (UI sub-flow восстановил компиляцию).
- 4 backlog items (см. § Tech debt).
