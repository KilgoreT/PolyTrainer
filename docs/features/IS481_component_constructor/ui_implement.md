# ui_implement.md — IS481 component_constructor

UI слой (Compose) над existing State/Msg/ViewModel-обвязкой. 19 узлов design_tree
реализованы. MVP scope=Global confirmed (F158): UI не показывает scope-control
в `CreateComponentDialog`; reducer hardcode'ит scope по контексту
открытия (Manager → Global; PerDict → PerDictionaries([currentDict])).

## Создано

### Resources (drawable, core-resources)

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/drawable/ic_hammer.xml`
  — vector «молоток» 24dp для `ComponentsToolsIconButton`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/drawable/ic_components.xml`
  — vector «компоненты» 24dp для Settings entry, row leading icons, EmptyState.

### Tier 1 primitives — `:modules:core:ui`

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeRadioRow.kt`
  (F162) — generic radio-row для radio-group в формах/диалогах. Full-row clickable
  через `Modifier.selectable(... role = Role.RadioButton)`. Используется в обоих
  `CreateComponentDialog` (CM + PerDict) для template select.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/ErrorStateWidget.kt`
  (F163) — generic error-state с centered message + опциональным Retry button.

### Widgets — `:modules:screen:settingstab/widgets/settings/items/`

- `ComponentsManageWidget.kt` (#7) — drill-in entry в Settings.

### Widgets — `:modules:widget:dictionaryappbar/widget/`

- `ComponentsToolsIconButton.kt` (#8) — hammer icon-button в `DictionaryAppBar.actions`.

### Widgets — `:modules:screen:components_manager/widget/`

- `CreateComponentFab.kt` (#9) — FAB поверх `PrimaryLongFabWidget`.
- `ComponentsEmptyStateWidget.kt` (#10) — empty-state composable (variant resIds).
- `UserDefinedRowWidget.kt` (#11) — aggregated row (template chip, multi/single chip,
  global badge, usage text, edit/delete actions).
- `CreateComponentDialog.kt` (#13) — full-form create dialog (name + template radio
  + multi checkbox). Без scope-control (F158).
- `RenameComponentDialog.kt` (#14) — single-field rename dialog.
- `DeleteComponentConfirmDialog.kt` (#15) — confirm dialog с impact-preview блоком
  (values / dicts / quiz / prefs counts).
- `ComponentTemplateLabel.kt` (UI-extension `ComponentTemplate.labelRes()`).
- `NameErrorLabel.kt` (UI-extension `NameError.labelRes()`).

### Widgets — `:modules:screen:per_dictionary_components/widget/`

- `PerDictRowWidget.kt` (#12) — scoped row (зеркально #11, +global badge).
- `CreateComponentDialog.kt` — зеркально components_manager версии (типы из
  per_dict mate-пакета).
- `RenameComponentDialog.kt` — зеркально.
- `DeleteComponentConfirmDialog.kt` — зеркально.
- `CreateComponentFab.kt` — зеркально (внутренний `internal`-видимый из chest module).
- `ComponentsEmptyStateWidget.kt` — зеркально.
- `ComponentTemplateLabel.kt` (UI-extension).
- `NameErrorLabel.kt` (UI-extension).

## Модифицировано

### Tier 3 host-композайблы

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBar.kt`
  (#16) — добавлен `ComponentsToolsIconButton` перед `DictDropDownWidget` в actions slot;
  видим при `currentDict != null && !isLoading`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsTabScreen.kt`
  (#17) — добавлен `ComponentsManageWidget` между `LangManageWidget` и
  `ExportDataWidget` в первую `SettingsSectionWidget`.

### Tier 4 экраны (replace placeholders)

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerScreen.kt`
  (#18) — реальный UI поверх Mate-state. Scaffold (TopAppBar back-arrow + FAB +
  SnackbarHost) + LazyColumn (UserDefinedRowWidget) + EmptyState + 3 dialog-overlays.
  `BackHandler { RequestBack }`, state-driven snackbar (`LaunchedEffect(snackbar)`).
  `@file:OptIn(ExperimentalMaterial3Api::class)`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsScreen.kt`
  (#19) — зеркально #18; title = `state.dictionaryName ?: per_dict_components_title`;
  rows = `PerDictRowWidget`; EmptyState variant per-dict.

## Resources (drawable + strings)

### Drawables

- `core-resources/drawable/ic_hammer.xml` (new).
- `core-resources/drawable/ic_components.xml` (new).

### Strings (en + ru-rRU)

Добавлены 32 ключа в обе локали:

- Settings entry: `settings_section_components_management`.
- DictionaryAppBar: `components_tools_description`.
- Screen titles: `components_manager_title`, `per_dict_components_title`.
- EmptyState: 4 manager/per-dict headline/body + 1 CTA.
- FAB: `components_create_cta`.
- Create dialog: `components_create_dialog_title`, `components_create_field_name`,
  `components_create_name_placeholder`, `components_create_field_template`,
  `components_create_field_is_multi`.
- Rename dialog: 4 ключа (title / original / new_name / placeholder).
- Delete dialog: 7 ключей (title с %1$s, impact_values/dicts/quiz/prefs с %1$d,
  unavailable fallback, hint).
- NameError labels: 4 ключа (empty / too_long / same_scope / cross_scope).
- Chip labels: `components_chip_single`, `_multi`, `_global`.
- Template labels: `components_template_text`, `_image`.
- Button labels: `components_button_create`, `_save` (delete переиспользует
  `button_delete`, cancel — `button_cancel`).

Удалены `_picker_label` / `_scope_global` / `_scope_per_dict` keys из исходного
дизайна (F158 — UI не показывает scope-control).

## Build.gradle deps

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/build.gradle.kts`
  — `+ :core:core-resources`, `+ composeLibs.activityCompose` (BackHandler).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/build.gradle.kts`
  — `+ :core:core-resources`, `+ composeLibs.activityCompose`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/build.gradle.kts`
  — `+ :core:core-resources` (нетривиальное решение, см. ниже).

## Нетривиальные решения

1. **Dependency `:core:core-resources` для `:modules:widget:dictionaryappbar`** —
   design_tree #5/#6 описывают добавление в screen-модули, но `ComponentsToolsIconButton`
   (#8) живёт в `dictionaryappbar` (existing widget module) и ссылается на
   `R.drawable.ic_hammer` + `R.string.components_tools_description`. Audit-gap в
   design_tree: узла #16 («modify DictionaryAppBar.kt») не было достаточно — нужна
   симметричная gradle-модификация. Добавил независимо, чтобы сборка модуля
   проходила.

2. **Dialogs / EmptyState / FAB продублированы между двумя screen-модулями.**
   Design_tree #13/#14/#15 предлагали «single source for both screens (per-dict
   imports the same file)». Это невозможно — `internal`-видимые composable
   ссылаются на типы (`CreateDialogState`, `RenameDialogState`, `DeleteConfirmState`)
   из разных `mate`-пакетов (mate types in `components_manager.mate.*` vs
   `per_dictionary_components.mate.*`). Создал зеркальные копии в per_dict
   widget-папке. Альтернатива (вынос всех Dialog'ов в `:modules:widget:component_widgets`
   с shared State type) — отдельная архитектурная задача и outside scope.

3. **UI-extensions `ComponentTemplate.labelRes()` / `NameError.labelRes()`** —
   domain enum'ы (`me.apomazkin.lexeme.ComponentTemplate`, `me.apomazkin.lexeme.NameError`)
   не содержат UI-strings (verified F157). Создал per-module локальные `internal`
   extensions в widget-папке. Два модуля → два дубликата (см. п.2). Маппинги
   тривиальные.

4. **AssistChip без custom border** — F157 поднял вопрос `AssistChipDefaults.assistChipBorder(...)`.
   Оставил default chip styling без явного border override (Material3 1.3.2,
   `enabled = false` достаточно для визуальной маркировки read-only chip'ов).

5. **`LexemeTextFieldWidget` принимает `onKeyboardActions`** — required параметр в
   API; передаю пустую lambda для dialog'ов где Enter-on-keyboard не нужен.

## Tests

Composable tests для нового UI не определены в design_tree (узлов нет).
Bypassed by design — UI rendering tests требуют instrumentation harness
(Android device / emulator), not unit. Существующие unit-тесты модулей
(`SettingsTabReducerTest`, `DictionaryAppBarReducerTest`,
`ComponentsManagerReducerTest`, `PerDictionaryComponentsReducerTest`) не задеты:
contract Msg/State/Reducer не менялся.

Build/lint **не запускался** по step rules — оставлено на следующий шаг
(ui_test / publish).

## Известные TODO для последующих шагов

- **build/lint** — `./scripts/cc-build.sh assembleDebug` + `./scripts/cc-build.sh :app:lintDebug`
  для верификации компилируемости. Возможные правки: AssistChip API drift,
  `ComponentTypeId.id` vs `value` (используется `it.typeId.id` — verified в source).
- **MVP scope=Global cleanup** — `Msg.CreateScopeChange` остаётся в контракте Msg.kt
  (см. business_summary § ключевые решения), но UI его не отправляет. Можно
  спрятать или оставить как dead-callback (TBD).
- **CreateComponentDialog dismissOnClickOutside** — `LexemeDialog` default
  `dismissOnClickOutside=true` теряет заполненную форму при tap-вне. Не критично
  для MVP, но из § Open Q дизайна можно опционально override на `false` для
  Create/Rename.
- **TopAppBar `surface` color tinting** — оставлен default; если в проектной
  палитре есть кастомный topBar tint — apply через `colors = TopAppBarDefaults.topAppBarColors(...)`.
- **Phase 2: PerDictionaries multi-dict picker** — backlog (F158).

## История ревью

### Iter 2 (F161/F162/F163)

**F161 — i18n для PerDictRowWidget**

- Touched: `modules/screen/per_dictionary_components/.../PerDictRowWidget.kt`
  (заменён hardcoded `"${row.valueCount} values"` на
  `stringResource(R.string.per_dict_row_value_count, row.valueCount)`).
- Strings: `per_dict_row_value_count` (en: `%d values`, ru: `Значений: %d`).

**F162 — radio row primitive**

- Создан `LexemeRadioRow` в `:modules:core:ui` (см. § Создано). Полная row-area
  clickable через `Modifier.selectable(role = Role.RadioButton)`.
- Touched callsites: `components_manager.widget.CreateComponentDialog` и
  `per_dictionary_components.widget.CreateComponentDialog` — замена
  `LexemeRadioMenuItem(...)` на `LexemeRadioRow(...)` в template radio-group.
- `LexemeRadioMenuItem` оставлен в `:modules:core:ui` без изменений
  (используется в `quiz/chat/widget/appbar/menu/ComponentChoiceItem.kt`).

**F163 — error state + Retry**

- Создан `ErrorStateWidget` в `:modules:core:ui` (см. § Создано).
- Touched: `ComponentsManagerScreen.kt` и `PerDictionaryComponentsScreen.kt` —
  добавлена ветка `!isLoading && rows == null` → `ErrorStateWidget` с Retry button.
- Mate contract — добавлен `Msg.OnRetryClick` в оба Msg.kt и
  `DatasourceEffect.LoadAllUserDefinedTypes` / `LoadComponentsForDictionary` в
  соответствующих `DatasourceEffect.kt`. Reducer ветка `Msg.OnRetryClick`
  выставляет `isLoading=true` и эмитит соответствующий effect.
- `AllUserDefinedTypesFlowHandler` / `ComponentsForDictionaryFlowHandler` —
  сохраняют `scope` + `send` при `subscribe()`, и в `runEffect` на Load* эффект
  отменяют существующий `job` через `unsubscribe()` и пересоздают подписку
  (re-subscribe pattern).
- `DatasourceEffectHandler` (оба модуля) — фильтрует Load* эффекты в `filter()`
  (handled by FlowHandler), добавлены exhaustive `when` ветки `→ Msg.Empty` (dead code).
- Strings (en + ru): `components_manager_load_failed`,
  `components_per_dict_load_failed`, `components_error_retry`.

**Build/Tests**

- `./scripts/cc-build.sh :modules:screen:components_manager:compileDebugKotlin :modules:screen:per_dictionary_components:compileDebugKotlin :modules:core:ui:compileDebugKotlin` — PASS.
- `./scripts/cc-build.sh :modules:screen:components_manager:testDebugUnitTest :modules:screen:per_dictionary_components:testDebugUnitTest` — PASS.
- `assembleDebug` падает на pre-existing ошибках в `core/core-db-impl` (WIP
  data-layer refactor на ветке, не связан с iter 2 fixes).
