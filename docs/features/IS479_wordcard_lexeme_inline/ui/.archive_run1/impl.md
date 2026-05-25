---
status: done
---

# impl — IS479 UI sub-flow

UI-имплементация после business sub-flow. Восстанавливает компиляцию
`:modules:screen:wordcard` и реализует inline-механику создания/редактирования лексемы
по Figma `9154-82519`/`82521`/`82532`/`82625`.

## Файлы

### Удалены [-]

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/AddLexemeBottomWidget.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/LexemeMeaningWidget.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/ActionsWidget.kt`
- Пакет `widget/addlexeme/` удалён целиком (стал пустым).

### Созданы [+]

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeChipPlaceholderWidget.kt`
  — `SuggestionChip` с leading-плюс-иконкой + label `"+ Перевод"` / `"+ Определение"`,
  параметры `(labelRes, enabled, onClick, modifier)`. Material3 OptIn `ExperimentalMaterial3Api`.
  3 Preview-варианта (translation/definition/disabled).

### Изменены [~]

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt`
  — корневой экран:
  - Удалён `import me.apomazkin.wordcard.mate.UiMsg` (F-arch-3 critical).
  - Удалён FAB-слот `floatingActionButton` из Scaffold.
  - Удалён рендер `AddLexemeBottomWidget` блока.
  - Cast `val loaded = state.wordState as? WordState.Loaded` один раз на frame;
    при `loaded == null` контент карточки не рендерится.
  - Cast прокидывается в `WordFieldWidget` и `ConfirmDeleteWordWidget`.
  - `LexemeItemWidget` получает `isPendingDbOp` параметром.
  - `AddLexemeWidget` (inline-кнопка) рендерится после lexeme-list в основной колонке,
    `enabled = !state.isPendingDbOp && !state.isCreatingLexeme`,
    `onClick → Msg.CreateLexeme`.
  - `SnackbarLaunchEffect.onResetState` → `Msg.DismissNotification` (вместо удалённого `UiMsg.ShowNotification(false)`).
  - 2 Preview-блока обновлены под `WordState.Loaded(id=1L, added=Date(), value="Word")`.

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/ConfirmDeleteWordWidget.kt`
  — параметр `loaded: WordState.Loaded` (вместо `state: WordState`). `Msg.RemoveWord(loaded.id)`.
  Preview под `WordState.Loaded(id=1L, added=Date(), value="apple", showWarningDialog=true)`.
  Импорт `R` сменён на `me.apomazkin.core_resources.R` (явный namespace, прецедент TopBarWidget).

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/WordFieldWidget.kt`
  — параметры `loaded: WordState.Loaded` + `isPendingDbOp: Boolean`. Использует
  `loaded.value`/`loaded.edited`/`loaded.isEditMode`/`loaded.added` (non-nullable `Date`).
  `onCloseEditMode` шлёт только `Msg.CommitWordChanges` (reducer сам делает `disableWordEdit`,
  см. F-arch-4). Edit-режим блокируется при `isPendingDbOp`. Preview под `WordState.Loaded`.

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/AddLexemeWidget.kt`
  — переписан с FAB на inline-`Button` по Figma `9154-82532`. Сигнатура
  `(enabled: Boolean, onAddLexeme: () -> Unit, modifier)`. Iconography: `ic_add_value` + label
  `R.string.word_card_add_lexeme`. Импорт `R` сменён на `me.apomazkin.core_resources.R`.

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeValueFieldWidget.kt`
  — переписан под chip-edit:
  - View-mode: `InputChip(selected=true)` с label = `state.origin`, trailing icon
    `ic_circle_delete` (тап → `onRemove`), `onClick → onOpenEditMode`.
  - Edit-mode: `BasicTextField` (с `FocusRequester.requestFocus()` при входе) +
    `IconBoxed(ic_confirm)` (commit) + `IconBoxed(ic_close)` (cancel).
  - Старый параметр `onCloseEditMode` заменён парой `onCommitEdit` + `onCancelEdit`,
    добавлен параметр `enabled`. Старый `onActionIconClick` → `onRemove`.
  - Material3 OptIn.

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeTitleWidget.kt`
  — добавлен параметр `isPendingDbOp: Boolean`. Trigger dropdown (`onClickDropDown`,
  иконка `ic_more_horizonral`) блокируется при `isPendingDbOp`. Menu-items без изменений.
  Preview обновлён.

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeItemWidget.kt`
  — добавлен параметр `isPendingDbOp: Boolean`, прокидывается в `LexemeTitleWidget`,
  `LexemeValueFieldWidget`, `LexemeChipPlaceholderWidget`. Логика:
  - `translation != null` → `LexemeValueFieldWidget`,
    иначе `LexemeChipPlaceholderWidget(R.string.word_card_lexeme_add_translation)`.
  - Симметрично для `definition`.
  - `HorizontalDivider` только когда **оба** subentity имеют значение (F-arch-6).
  - Подключены новые Msg: `Commit*Edit`/`Cancel*Edit` вместо `Exit*EditMode`,
    `Create*` для chip-placeholder.
  Импорт `R` → `me.apomazkin.core_resources.R`.
  Добавлен Preview `PreviewEmpty` (оба chip-placeholder, NOT_IN_DB-сценарий).

## Compile status

`./gradlew :modules:screen:wordcard:compileDebugKotlin` — **passed** (`BUILD SUCCESSFUL`).
Без ошибок, без warnings относящихся к нашим изменениям (deprecation warnings на
`build.gradle.kts` — pre-existing project setup, вне скоупа IS479).

## Нетривиальные решения

1. **Импорт `R` сменён на `me.apomazkin.core_resources.R`** во всех изменённых виджетах
   (`WordFieldWidget`, `ConfirmDeleteWordWidget`, `AddLexemeWidget`, `LexemeItemWidget`).
   Обоснование: прецедент в этом же модуле (`TopBarWidget.kt`, `AddTranslationLexemeMenuItem.kt`,
   `DeleteLexemeMenuItem.kt`), явный namespace надёжнее чем зависимость от Android resource merging
   через локальный `me.apomazkin.wordcard.R`. Совместимо с обоими (ресурсы те же),
   но явнее в импорт-блоке.

2. **`LexemeChipPlaceholderWidget` использует `SuggestionChip`** (а не `AssistChip`).
   Прецедент — `ChipPickerWidget` в `modules/widget/chipPicker`, в дизайн-системе проекта
   `SuggestionChip` уже легитимный паттерн. `OptIn(ExperimentalMaterial3Api)` принят по
   дизайн-палитре. Иконка — `ic_add_value` (то же, что в `AddLexemeWidget`).

3. **`LexemeValueFieldWidget.EditRow` использует `BasicTextField` напрямую** вместо
   `PrimaryEditableWidget` или `LexemeEditableText`. Обоснование: исходные виджеты дают
   связку (text + один callback `onCloseEditMode`), а новый contract требует два
   независимых callback'а (`onCommitEdit` для галочки, `onCancelEdit` для крестика).
   Inline `BasicTextField` + два `IconBoxed` чище чем переусложнение `LexemeEditableText`
   ещё одним cancel-callback'ом (что вышло бы за рамки UI sub-flow).

4. **`FocusRequester.requestFocus()` в `EditRow`** через `DisposableEffect(Unit)` —
   гарантирует фокус на textfield сразу при входе в edit-mode (UX-эффект как в Figma
   `9154-82521`). Аналог логики `LexemeEditableText:55-58`.

5. **`onCloseEditMode` в `WordFieldWidget` шлёт только `Msg.CommitWordChanges`** (не пару
   `ExitWordEditMode` + `CommitWordChanges`). Обоснование: F-arch-4 в design_tree —
   reducer на `CommitWordChanges` сам делает `disableWordEdit()` (см. `WordCardReducer.kt:64-77`),
   две Msg подряд избыточны. `Msg.ExitWordEditMode` остаётся жив в контракте на случай
   будущего cancel-trigger (backlog).

6. **`WordCardScreen` рендерит контент только при `loaded != null`** — внешний `Box` остаётся
   (для размещения `ConfirmDeleteWordWidget`-overlay), но колонка с `WordFieldWidget` /
   lexeme-list / `AddLexemeWidget` скрыта при `WordState.NotLoaded`. Спиннер/skeleton
   не добавлены (`isLoading` остаётся в state, но визуально не отображается —
   `WordCardNavigationEffectHandler` обрабатывает `WordNotFound` через `NavigateBack`;
   `isLoading=true` без `loaded` — состояние гонки старта, < frame).

7. **Pre-existing `Msg.NoOperation`** в `Message.kt:46` оставлен — не используется
   в новых call-sites (нет нужды), но является частью текущего контракта.

## Лог итераций

- **ит.1** (2026-05-22T00:30:00+0000) — реализация всех узлов DAG (#0-#10) в один проход:
  3 удаления (`AddLexemeBottomWidget`, `LexemeMeaningWidget`, `ActionsWidget`), 1 создание
  (`LexemeChipPlaceholderWidget`), 7 модификаций (`ConfirmDeleteWordWidget`, `WordFieldWidget`,
  `AddLexemeWidget`, `LexemeValueFieldWidget`, `LexemeTitleWidget`, `LexemeItemWidget`,
  `WordCardScreen`). Все Preview-блоки обновлены под новые сигнатуры (F-arch-1/2 critical).
  Импорты `R` унифицированы на `me.apomazkin.core_resources.R` (прецедент TopBar).

  `compileDebugKotlin` — `BUILD SUCCESSFUL` за 2m 34s, без ошибок и без новых warnings.
  Stale references (`AddLexemeBottomWidget`/`UiMsg`/`OpenAddLexemeDialog` etc.) в
  `src/main` отсутствуют (`grep` пройден без матчей).

  Удалена директория `widget/addlexeme/` (пустая после удалений).

## Готово к check sub-flow

Production-код компилируется. Тесты транзитивно разблокированы business sub-flow'ом
(на `compileDebugKotlin` модуля). Следующий шаг — `:modules:screen:wordcard:testDebugUnitTest`
в check sub-flow.
