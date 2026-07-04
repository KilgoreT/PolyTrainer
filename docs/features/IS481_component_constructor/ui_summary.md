---
status: done
---

# Summary — UI sub-flow (IS481 component_constructor)

## Что сделано

UI слой реализован поверх готовой Mate/business-обвязки (State / Msg / Reducer / ViewModel) двух новых экранов и двух host-вставок. Все 19 узлов `ui_design_tree.md` выполнены и опубликованы в spec через `publish_ui` (`update-section: ## UI Layout`, ~496 строк inline). Build/test для затронутых модулей зелёные; `assembleDebug` падает на pre-existing ошибках в `core/core-db-impl` (не относится к UI sub-flow).

### Resources (core-resources)

- `core/core-resources/src/main/res/drawable/ic_hammer.xml` — vector 24dp «молоток» для `ComponentsToolsIconButton` в `DictionaryAppBar.actions`.
- `core/core-resources/src/main/res/drawable/ic_components.xml` — vector 24dp «компоненты» для Settings entry, row leading icons, EmptyState.
- `core/core-resources/src/main/res/values/strings.xml` + `values-ru-rRU/strings.xml` — добавлено ~33 ключа в обе локали (settings entry, dictionaryappbar description, screen titles, EmptyState, FAB, Create/Rename/Delete dialogs, NameError labels, chip-labels, template labels, button labels, retry/load-failed).

### Tier 1 primitives — `:modules:core:ui`

- `modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeRadioRow.kt` — generic radio-row (F162). Full-row clickable через `Modifier.selectable(... role = Role.RadioButton)`. Используется в обоих `CreateComponentDialog` для template radio-group. `LexemeRadioMenuItem` оставлен как есть (используется в quiz/chat appbar menu).
- `modules/core/ui/src/main/java/me/apomazkin/ui/ErrorStateWidget.kt` — generic error-state с centered message + опциональным Retry button (F163).

### 14 новых widget'ов

`:modules:screen:settingstab/widgets/settings/items/`:
- `ComponentsManageWidget.kt` (#7) — drill-in entry в Settings, wrapper поверх `SettingsItemWidget`.

`:modules:widget:dictionaryappbar/widget/`:
- `ComponentsToolsIconButton.kt` (#8) — hammer icon-button в `DictionaryAppBar.actions`, wrapper поверх `IconBoxed`.

`:modules:screen:components_manager/widget/`:
- `CreateComponentFab.kt`, `ComponentsEmptyStateWidget.kt`, `UserDefinedRowWidget.kt`, `CreateComponentDialog.kt`, `RenameComponentDialog.kt`, `DeleteComponentConfirmDialog.kt`, `ComponentTemplateLabel.kt` (UI-extension `ComponentTemplate.labelRes()`), `NameErrorLabel.kt` (UI-extension `NameError.labelRes()`).

`:modules:screen:per_dictionary_components/widget/`:
- `PerDictRowWidget.kt`, + зеркальные копии `CreateComponentDialog.kt`, `RenameComponentDialog.kt`, `DeleteComponentConfirmDialog.kt`, `CreateComponentFab.kt`, `ComponentsEmptyStateWidget.kt`, `ComponentTemplateLabel.kt`, `NameErrorLabel.kt`. Дублирование обусловлено разными mate-пакетами (`components_manager.mate.*` vs `per_dictionary_components.mate.*`); вынос в shared `:modules:widget:component_widgets` — outside scope (см. ниже).

### 2 modify host'а

- `modules/widget/dictionaryappbar/.../DictionaryAppBar.kt` (#16) — добавлен `ComponentsToolsIconButton` перед `DictDropDownWidget` в `actions` slot, видим при `currentDict != null && !isLoading`. Появляется автоматически на всех 3 табах (Vocabulary / Quiz / Statistic).
- `modules/screen/settingstab/.../SettingsTabScreen.kt` (#17) — добавлен `ComponentsManageWidget` между `LangManageWidget` и `ExportDataWidget` в первую `SettingsSectionWidget`.

### 2 Screen replace (placeholder → реальный UI)

- `modules/screen/components_manager/.../ComponentsManagerScreen.kt` (#18) — Scaffold (TopAppBar back-arrow + FAB + SnackbarHost) + branching on (`isLoading`, `isEmpty`, `rows != null`, `error`) → CircularProgressIndicator / `ComponentsEmptyStateWidget` / `LazyColumn(UserDefinedRowWidget)` / `ErrorStateWidget(onRetry → Msg.OnRetryClick)`. 3 dialog overlays. `BackHandler { Msg.RequestBack }`. State-driven snackbar через `LaunchedEffect(snackbarState)`. `@file:OptIn(ExperimentalMaterial3Api::class)`.
- `modules/screen/per_dictionary_components/.../PerDictionaryComponentsScreen.kt` (#19) — зеркально #18; title = `state.dictionaryName ?: per_dict_components_title`, rows = `PerDictRowWidget`, EmptyState variant per-dict.

### Build.gradle deps

- `modules/screen/components_manager/build.gradle.kts` — `+ :core:core-resources`, `+ composeLibs.activityCompose` (BackHandler).
- `modules/screen/per_dictionary_components/build.gradle.kts` — то же.
- `modules/widget/dictionaryappbar/build.gradle.kts` — `+ :core:core-resources` (не было в design_tree — audit-gap, см. ниже).

### Mate-contract extension (минимальный)

- `Msg.OnRetryClick` добавлен в оба Msg.kt (components_manager + per_dictionary_components) — F163.
- `DatasourceEffect.LoadAllUserDefinedTypes` / `.LoadComponentsForDictionary` добавлены в соответствующие `DatasourceEffect.kt` для re-subscribe pattern. Reducer handles `Msg.OnRetryClick` → `isLoading=true` + эмитит Load* эффект.
- `AllUserDefinedTypesFlowHandler` / `ComponentsForDictionaryFlowHandler` — сохраняют `scope` + `send` при `subscribe()`, в `runEffect` отменяют существующий job через `unsubscribe()` и пересоздают подписку.

## Ключевые решения

- **F158 — MVP scope=Global only** [iter 3 fix, resolved]. `CreateComponentDialog` НЕ показывает scope-control. Reducer на `OpenCreateDialog` инициализирует scope по контексту: Manager → `Scope.Global`; PerDict → `Scope.PerDictionaries([currentDict])`. `Msg.CreateScopeChange` остаётся в контракте Msg.kt, но UI его не отправляет. PerDictionaries multi-dict picker из manager-экрана — phase 2 (Backlog).
- **F162 — `LexemeRadioRow` primitive** [iter 2 fix]. Generic radio-row в `:modules:core:ui` (полная row-area clickable через `Modifier.selectable(role = Role.RadioButton)`). Заменил `LexemeRadioMenuItem` в обоих `CreateComponentDialog` для template radio-group. Решение: переиспользуемый primitive для будущих radio-форм, без модификации существующего `LexemeRadioMenuItem` (используется в quiz/chat appbar).
- **F163 — `ErrorStateWidget` + Retry flow с re-subscribe** [iter 2 fix]. Generic centered error-message + опциональный Retry в `:modules:core:ui`. Retry flow: `Msg.OnRetryClick → isLoading=true + DatasourceEffect.Load*` → FlowHandler.runEffect отменяет существующий job через `unsubscribe()` и пересоздаёт подписку с сохранённым `scope` + `send`. Альтернатива (просто `isLoading=true` без re-subscribe) не сработала бы — collector job уже завершён после ошибки.
- **F161 — i18n PerDictRowWidget** [iter 2 fix]. Заменён hardcoded `"${row.valueCount} values"` на `stringResource(R.string.per_dict_row_value_count, row.valueCount)`. RU: `Значений: %d`.
- **F157 — UI-extension `ComponentTemplate.labelRes()` / `NameError.labelRes()`**. Domain enum'ы (`me.apomazkin.lexeme.ComponentTemplate`, `me.apomazkin.lexeme.NameError`) не содержат UI-strings; UI-фаза вводит локальные `internal` extensions в widget-папке каждого screen-модуля (два дубликата).
- **F155 — явный `composeLibs.activityCompose` dep** в обоих screen-модулях — `BackHandler` не приходит транзитивно из `:modules:core:ui` (parity с `:modules:screen:settingstab/build.gradle.kts`).
- **F153 — явный `:core:core-resources` dep** в обоих screen-модулях + `:modules:widget:dictionaryappbar` (transitive R-aggregation не работает между модулями).

### Что вне scope этого UI sub-flow

- **`data_implement` — новые `LexemeApi` методы в `core-db-impl`.** Существующий `assembleDebug` падает на pre-existing ошибках в `core/core-db-impl` (WIP data-layer refactor на ветке). `business_implement` и `infra_implement` уже определили контракт; реализация в `core-db-impl` — отдельный шаг `data_implement` (не запускался в этом sub-flow). UI компилируется (`:modules:screen:components_manager:compileDebugKotlin` + `:per_dictionary_components:compileDebugKotlin` + `:modules:core:ui:compileDebugKotlin` — PASS), но end-to-end build пока сломан.
- **`data_design_tree` — drop `ComponentValueData`.** Старый persistence-тип компонентов остаётся в коде; новый generic API ещё не подменил его на data-уровне. Это data-sub-flow задача.
- **Дублирование Dialog'ов между двумя screen-модулями.** Архитектурно правильное решение — вынести в `:modules:widget:component_widgets` с shared `CreateDialogState` / `RenameDialogState` / `DeleteConfirmState` (либо общие interface'ы). Требует рефакторинга mate-пакетов обоих модулей — outside UI scope. `:modules:widget:component_widgets` существует (infra phase создал gradle setup + AndroidManifest), но source files нет.
- **Composable instrumentation tests** — design_tree их не определял (требует Android device / emulator). Существующие unit-тесты Reducer'ов не задеты (contract Msg/State/Reducer для добавленных `Msg.OnRetryClick` обновлены, но публикация в spec через publish_ui не задевает test-сценарии).
- **`assembleDebug` / `lintDebug` end-to-end** — не запускались (pre-existing core-db-impl breakage). Per-module `compileDebugKotlin` + `testDebugUnitTest` для затронутых модулей PASS (см. ui_implement § Build/Tests).

### Аудит-gap, замеченный в design_tree

`ui_design_tree.md` #5/#6 описывали добавление `:core:core-resources` только в screen-модули, но `ComponentsToolsIconButton` (#8) живёт в `:modules:widget:dictionaryappbar` и ссылается на новые R-id. Добавил `:core:core-resources` в `dictionaryappbar/build.gradle.kts` независимо. Не блокирует sub-flow (compile прошёл), но stylistically design_tree должен был учесть.

## Артефакты

- `docs/features/IS481_component_constructor/ui_layout.md` — UI cheat-sheet (14 widgets + 2 host-вставки + 2 screens, structure / type / size / params / callbacks / behavior / notes / source per widget). История ревью: 3 итерации, F148-F151 fixed.
- `docs/features/IS481_component_constructor/ui_design_tree.md` — DAG 19 узлов (Tier 0 resources → Tier 1 build.gradle → Tier 2 widgets → Tier 3 host modifications → Tier 4 screens). История ревью: F152-F158 fixed (F158 — MVP scope=Global).
- `docs/features/IS481_component_constructor/ui_implement.md` — отчёт реализации: создано / модифицировано / нетривиальные решения / iter 2 fixes (F161/F162/F163) / per-module build statuses.
- `docs/features/IS481_component_constructor/publish_ui.md` — публикация в spec: `update-section: ## UI Layout` (~496 строк inline в `docs/features-spec/component-constructor.md`, 1366 строк итого; canonical H2 order preserved).
- `docs/features/IS481_component_constructor/ui_walkthrough.md` — стартовый walkthrough existing UI patterns (precedent'ы для всех новых виджетов).

_model: claude-opus-4-7[1m]_
