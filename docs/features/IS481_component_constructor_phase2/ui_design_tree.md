# UI design tree: IS481 component_constructor phase 2

DAG узлов UI слоя для phase 2. Источники: `ui_layout.md` (UI snapshot), `business_summary.md § UI dependencies handed off to ui sub-flow`, `ui_walkthrough.md` (existing file:line). Пути верифицированы Read'ом.

Конвенция меток: `[+]` — NEW, `[~]` — modify, `[-]` — delete.

Целевой shared module package: `modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/` (currently empty). Build/deps wiring уже готовы (см. `ui_walkthrough.md § 3`).

## Граф (YAML)

```yaml
nodes:
  # ─────────────────────────── Tier 0: per-template composables (zero deps) ───────────────────────────
  - id: '#0'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/templates/TextWidget.kt'
    summary: 'Per-template TEXT composable (read-only | editable mode).'
    deps: []

  - id: '#1'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/templates/ComponentBlock.kt'
    summary: 'Wrapper: name-label + content slot. Pure structural.'
    deps: []

  - id: '#2'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/templates/ComponentByTemplate.kt'
    summary: 'Per-template resolver — exhaustive when по ComponentTemplate; renders ComponentBlock + TextWidget (TEXT). IMAGE backlog.'
    deps: ['#0', '#1']

  # ─────────────────────────── Tier 1: shared widget primitives (base only) ───────────────────────────
  - id: '#3'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/widgets/ComponentTemplateLabel.kt'
    summary: 'internal fun ComponentTemplate.labelRes(): Int — мигрирует из двух screen-модулей.'
    deps: []

  - id: '#4'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/widgets/NameErrorLabel.kt'
    summary: 'internal fun NameError.labelRes(): Int — мигрирует из двух screen-модулей. Если EditNameError имеет тот же shape — добавить отдельную EditNameError.labelRes() в том же файле либо в EditDialog хосте.'
    deps: []

  - id: '#5'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/widgets/ComponentsEmptyStateWidget.kt'
    summary: 'Empty state Column: icon + headline + body + CTA. API: (headlineRes, bodyRes, ctaRes, onCreate). Render 1-в-1 phase 1.'
    deps: []

  - id: '#6'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/widgets/CreateComponentFab.kt'
    summary: 'Thin wrapper над PrimaryLongFabWidget(ic_add, components_create_cta).'
    deps: []

  - id: '#7'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/widgets/UserDefinedRowWidget.kt'
    summary: 'Row для Manager: leading icon + name/meta/usage + edit/delete buttons. onEdit→Msg.OpenEditDialog (НЕ OpenRenameDialog).'
    deps: ['#3']

  - id: '#8'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/widgets/PerDictRowWidget.kt'
    summary: 'Row для PerDict: title row содержит global chip; usage_text = per_dict_row_value_count. onEdit→Msg.OpenEditDialog.'
    deps: ['#3']

  - id: '#9'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/widgets/CardinalityDowngradePreviewWidget.kt'
    summary: 'Inline preview: top-3 lexeme rows + drill-in button (видна iff InlineWithDrillIn). lexemeLabel callback для resolve id→label.'
    deps: []

  # ─────────────────────────── Tier 2: dialogs (depend on widgets) ───────────────────────────
  - id: '#10'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/dialogs/RenameComponentDialog.kt'
    summary: 'Rename dialog (extract из обоих screen-модулей). API rewrite: плоские примитивы вместо RenameDialogState.'
    deps: ['#4']

  - id: '#11'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/dialogs/DeleteComponentConfirmDialog.kt'
    summary: 'Delete confirm dialog (extract). API rewrite: плоские примитивы + lightweight DeletionImpactRef DTO.'
    deps: []

  - id: '#12'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/dialogs/CreateComponentDialog.kt'
    summary: 'Create dialog с phase 2 multi-dict chip-list (FlowRow + FilterChip), управляется hostVariant=Manager|PerDict. API rewrite: плоские примитивы + DictionaryRef display-only тип.'
    deps: ['#3', '#4']

  - id: '#13'
    kind: '[+]'
    path: 'modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/dialogs/EditComponentDialog.kt'
    summary: 'NEW phase 2: name input + template radio (gated на UseCase) + isMulti checkbox + CardinalityDowngradePreview slot + actions. canSubmit учитывает dirty-state.'
    deps: ['#3', '#4', '#9']

  # ─────────────────────────── Tier 3: screen mounts (modify) ───────────────────────────
  - id: '#14'
    kind: '[~]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerScreen.kt'
    summary: 'Import paths → shared module; mount EditComponentDialog (visible iff state.editDialog != null); CreateComponentDialog получает availableDictionaries + selectedDictionaryIds + hostVariant=Manager + onDictionaryToggle/onScopeChange. DictionaryApiEntity маппится на host-уровне в DictionaryRef.'
    deps: ['#5', '#6', '#7', '#10', '#11', '#12', '#13']

  - id: '#15'
    kind: '[~]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsScreen.kt'
    summary: 'Import paths → shared module; mount EditComponentDialog (БЕЗ scope picker); CreateComponentDialog получает hostVariant=PerDict (scope hardcoded).'
    deps: ['#5', '#6', '#8', '#10', '#11', '#12', '#13']

  # ─────────────────────────── Tier 4: deletions (после screens переключены) ───────────────────────────
  - id: '#16'
    kind: '[-]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/CreateComponentDialog.kt'
    summary: 'Удалён, мигрирован в #12.'
    deps: ['#14']

  - id: '#17'
    kind: '[-]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/RenameComponentDialog.kt'
    summary: 'Удалён, мигрирован в #10.'
    deps: ['#14']

  - id: '#18'
    kind: '[-]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/DeleteComponentConfirmDialog.kt'
    summary: 'Удалён, мигрирован в #11.'
    deps: ['#14']

  - id: '#19'
    kind: '[-]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/UserDefinedRowWidget.kt'
    summary: 'Удалён, мигрирован в #7.'
    deps: ['#14']

  - id: '#20'
    kind: '[-]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/ComponentsEmptyStateWidget.kt'
    summary: 'Удалён, мигрирован в #5.'
    deps: ['#14']

  - id: '#21'
    kind: '[-]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/CreateComponentFab.kt'
    summary: 'Удалён, мигрирован в #6.'
    deps: ['#14']

  - id: '#22'
    kind: '[-]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/ComponentTemplateLabel.kt'
    summary: 'Удалён, мигрирован в #3.'
    deps: ['#14']

  - id: '#23'
    kind: '[-]'
    path: 'modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/NameErrorLabel.kt'
    summary: 'Удалён, мигрирован в #4.'
    deps: ['#14']

  - id: '#24'
    kind: '[-]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/CreateComponentDialog.kt'
    summary: 'Удалён, мигрирован в #12.'
    deps: ['#15']

  - id: '#25'
    kind: '[-]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/RenameComponentDialog.kt'
    summary: 'Удалён, мигрирован в #10.'
    deps: ['#15']

  - id: '#26'
    kind: '[-]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/DeleteComponentConfirmDialog.kt'
    summary: 'Удалён, мигрирован в #11.'
    deps: ['#15']

  - id: '#27'
    kind: '[-]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/PerDictRowWidget.kt'
    summary: 'Удалён, мигрирован в #8.'
    deps: ['#15']

  - id: '#28'
    kind: '[-]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/ComponentsEmptyStateWidget.kt'
    summary: 'Удалён, мигрирован в #5.'
    deps: ['#15']

  - id: '#29'
    kind: '[-]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/CreateComponentFab.kt'
    summary: 'Удалён, мигрирован в #6.'
    deps: ['#15']

  - id: '#30'
    kind: '[-]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/ComponentTemplateLabel.kt'
    summary: 'Удалён, мигрирован в #3.'
    deps: ['#15']

  - id: '#31'
    kind: '[-]'
    path: 'modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/NameErrorLabel.kt'
    summary: 'Удалён, мигрирован в #4.'
    deps: ['#15']
```

## Детали изменений

### #0 `templates/TextWidget.kt` [+]

Per-template Tier-2 composable для `ComponentTemplate.TEXT`. Read-only режим — `Text(value, style=LexemeStyle.BodyL)`; editable режим — `LexemeTextFieldWidget(value, onValueChange)`. API: `(value: String, editable: Boolean = false, onValueChange: (String) -> Unit = {})`. Зависимостей нет (использует только Compose + `:modules:core:ui` `LexemeTextFieldWidget`).

### #1 `templates/ComponentBlock.kt` [+]

Структурный wrapper: `Column(spacing=4) { Text(type.name, LabelM, onSurfaceVariant) + content() }`. API: `(type: ComponentType, content: @Composable () -> Unit)`. Зависимостей нет (`ComponentType` уже доступен через dep `:modules:domain:lexeme`).

### #2 `templates/ComponentByTemplate.kt` [+]

Composable dispatcher — exhaustive `when` по `ComponentTemplate`. Для `TEXT` рендерит `ComponentBlock(type) { TextWidget(values.value.text, editable, onValueChange) }`. `IMAGE` — backlog (placeholder либо ничего). Зависит от `#0`, `#1`.

### #3 `widgets/ComponentTemplateLabel.kt` [+]

Top-level internal extension `fun ComponentTemplate.labelRes(): Int = when (this) { TEXT -> R.string.components_template_text; IMAGE -> R.string.components_template_image }`. Аккумулирует идентичные определения из обоих screen-модулей (`widget/ComponentTemplateLabel.kt` в каждом).

### #4 `widgets/NameErrorLabel.kt` [+]

Top-level internal extension `fun NameError.labelRes(): Int` — 3 ветки (`Empty/SameScopeCollision/CrossScopeCollision`). Если для EditDialog нужен отдельный mapping `EditNameError.labelRes()` — добавить вторую top-level функцию в этом же файле (текущий `EditNameError` имеет идентичный shape, можно делать общую функцию через два enum'а).

### #5 `widgets/ComponentsEmptyStateWidget.kt` [+]

`Column(fillMaxWidth, padding=32, spacedBy=16, horizontalAlignment=Center)` → `Icon(ic_components, 64dp)` + `Text(headlineRes, H6)` + `Text(bodyRes, BodyL gray)` + `PrimaryFullButtonWidget(titleRes=ctaRes, onClick=onCreate)`. API: `(headlineRes: Int, bodyRes: Int, ctaRes: Int = R.string.components_empty_cta, onCreate: () -> Unit)`. Render 1-в-1 с phase 1. Preview в Manager-варианте сохранить (двуязычный `@PreviewWidget`).

### #6 `widgets/CreateComponentFab.kt` [+]

Тонкая обёртка `PrimaryLongFabWidget(ic_add, R.string.components_create_cta, enabled=true, onClick)`. API: `(onClick: () -> Unit)`. Render 1-в-1.

### #7 `widgets/UserDefinedRowWidget.kt` [+]

Row для Manager (`Surface(rounded-12) + Row(padding 16/12, spacing=12)`):
- leading: `IconBoxed(ic_components, 24)`.
- weight(1f) Column: `Text(row.name, BodyL)` + meta `Row { template_chip + cardinality_chip + global_chip (visible iff row.isGlobal) }` + `Text(usage_aggregated, BodyS gray)`.
- trailing: `IconBoxed(ic_edit, 44, onClick=onEdit)` + `IconBoxed(ic_trash, 44, onClick=onDelete)`.

API: `(row: UserDefinedRow, onEdit: (ComponentTypeId) -> Unit, onDelete: (ComponentTypeId) -> Unit)`. **Callback переключение phase 2**: `onEdit` теперь триггерится для `Msg.OpenEditDialog(typeId)` (раньше `OpenRenameDialog`) — переключение делает screen в `#14`. Зависит от `#3` (использует `template.labelRes()`).

Open Q: `UserDefinedRow` тип импортируется из `:modules:screen:components_manager.mate` — это создаёт обратную coupling. Альтернатива (плоские примитивы) — backlog (parity row widgets в единый `ComponentRowWidget(row: ComponentRowState)`). На phase 2 — оставляем coupling через dep `:modules:screen:components_manager` либо лифтим тип в `:modules:domain:lexeme` (требует обсуждения; решение implement-уровня).

### #8 `widgets/PerDictRowWidget.kt` [+]

Зеркальный `#7` для PerDict. Различия (из `ui_walkthrough.md § 2`):
- global chip в **title row** (рядом с name), не в meta row.
- usage_text использует `R.string.per_dict_row_value_count` formatted с `row.valueCount` (F161).

API: `(row: PerDictRow, onEdit: (ComponentTypeId) -> Unit, onDelete: (ComponentTypeId) -> Unit)`. Зависит от `#3`. То же замечание про coupling на screen-mate тип.

### #9 `widgets/CardinalityDowngradePreviewWidget.kt` [+]

`Column(padding=8, spacing=8, background=errorContainer, shape=rounded-12)`:
- title: `Text(R.string.components_edit_cardinality_blocked_title, LabelL)`.
- inline rows: `∀ preview is InlineOnly: preview.impactedLexemeIds.forEach { Text(lexemeLabel(id), BodyM) }`.
- `∀ preview is InlineWithDrillIn`: `preview.inlineIds.forEach { Text(lexemeLabel(id), BodyM) }` + `drill_in_btn: PrimaryTextButtonWidget(label=components_edit_show_all, arg=preview.impactedLexemeIds.size, onClick=onShowAll)`.

API: `(preview: ImpactedLexemesPreview, lexemeLabel: (Long) -> String, onShowAll: () -> Unit)`. Drill-in callback Msg — TBD backlog (zaglushka в screen). Зависимостей от других widgets нет.

Open Q: `ImpactedLexemesPreview` — sealed класс из `:modules:screen:components_manager.mate` (и зеркало в per_dict). Та же coupling-проблема как у Row widgets — backlog лифтить в domain либо принимать плоские поля `(inlineIds: List<Long>, totalSize: Int, showAllVisible: Boolean)`.

### #10 `dialogs/RenameComponentDialog.kt` [+]

Extract из обоих screen-модулей. Container `LexemeDialog`, structure: title `Text` → original `Row(label + value)` → input `LexemeTextFieldWidget` + error → actions `Row(CancelButton + PrimaryFullButton)`.

API rewrite (плоские примитивы): `(originalName: String, editedName: String, nameError: NameError?, isSubmitting: Boolean, onNameChange: (String) -> Unit, onSubmit: () -> Unit, onDismiss: () -> Unit)`. `canSubmit` вычисляется внутри composable из API parametrs. Зависит от `#4`.

### #11 `dialogs/DeleteComponentConfirmDialog.kt` [+]

Extract из обоих screen-модулей. Container `LexemeDialog`, structure: title `Text(arg=name)` → impact_slot (3-way: loading / impact != null с 4 опциональными `Text` строками / unavailable) → actions `Row(CancelButton + AlarmButton)`.

API rewrite: `(name: String, impact: DeletionImpactRef?, isLoadingImpact: Boolean, isSubmitting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit)`. `DeletionImpactRef` — lightweight display-only DTO с counts (`valueCount: Int`, `dictCount: Int`, `quizCount: Int`, `prefsCount: Int`) объявляется в shared widget module (избегаем import full `DeletionImpact` domain). Зависимостей от других widgets нет.

### #12 `dialogs/CreateComponentDialog.kt` [+]

Extract + phase 2 scope picker. Structure: title → name (label + LexemeTextFieldWidget + error) → template (label + radio rows) → multi (Checkbox + label) → **scope_slot** (Manager variant only: label + 2 `LexemeRadioRow`s + `∀ scope is PerDictionaries: FlowRow{ FilterChip × N }`) → actions.

API rewrite: `(name: String, template: ComponentTemplate, isMulti: Boolean, scope: Scope, nameError: NameError?, isSubmitting: Boolean, availableDictionaries: List<DictionaryRef>, selectedDictionaryIds: Set<Long>, hostVariant: HostVariant, onNameChange, onTemplateSelect, onMultiToggle, onScopeChange, onDictionaryToggle, onSubmit, onDismiss)`.

- `HostVariant`: enum `Manager | PerDict` — управляет видимостью `scope_slot`.
- `DictionaryRef`: lightweight display-only DTO `(id: Long, name: String)` — объявляется в shared module; host (screen) мапит `DictionaryApiEntity → DictionaryRef`.
- PerDict variant: scope hardcoded к `Scope.PerDictionaries(listOf(dictId))` в Reducer; widget просто не рендерит scope_slot.

Зависит от `#3`, `#4`.

### #13 `dialogs/EditComponentDialog.kt` [+] NEW phase 2

Container `LexemeDialog`, structure: title → name (label + LexemeTextFieldWidget + error) → template (label + `LexemeRadioRow × ComponentTemplate.entries`; clickable, gating на UseCase) → multi (Checkbox + label) → **preview_slot** (`∀ editDialog.impactedLexemesPreview != null: CardinalityDowngradePreviewWidget`) → actions.

API: `(editDialog: EditDialogState, isSubmitting: Boolean, onNameChange, onTemplateSelect, onMultiToggle, onShowAllImpacted, onSubmit, onDismiss)`. `canSubmit = name.trim().isNotBlank() && nameError == null && !isSubmitting && (dirty: name/template/isMulti changed)`. Зависит от `#3`, `#4`, `#9`.

Open Q: API принимает целый `EditDialogState` (mate тип) — нарушает «плоские примитивы» принцип. Альтернатива: разложить на плоские поля. Решение оставлено за implement-уровнем; parity с phase 1 dialogs которые принимают полные state объекты (после rewrite — плоские). Для consistency рекомендуется плоский API: `(name, template, isMulti, originalName, originalTemplate, originalIsMulti, nameError, impactedLexemesPreview, ...)`.

### #14 `ComponentsManagerScreen.kt` [~]

Изменения:
- **Import paths**: все 8 widget imports переключаются с `me.apomazkin.components_manager.widget.*` на `me.apomazkin.component_widgets.{dialogs|widgets|templates}.*`.
- **Mount EditComponentDialog**: `state.editDialog?.let { EditComponentDialog(it, state.isEditing, onNameChange = { sendMessage(Msg.EditNameChange(it)) }, onTemplateSelect = ..., onMultiToggle = ..., onShowAllImpacted = { /* TBD */ }, onSubmit = { sendMessage(Msg.SubmitEdit) }, onDismiss = { sendMessage(Msg.CloseEditDialog) }) }`.
- **CreateComponentDialog** получает `availableDictionaries = state.availableDictionaries.map { DictionaryRef(it.id, it.title) }`, `selectedDictionaryIds = state.createDialog.selectedDictionaryIds`, `hostVariant = HostVariant.Manager`, `onDictionaryToggle = { sendMessage(Msg.CreateDictionaryToggle(it)) }`, `onScopeChange = { sendMessage(Msg.CreateScopeChange(it)) }`.
- **UserDefinedRowWidget** теперь `onEdit = { sendMessage(Msg.OpenEditDialog(it)) }` (раньше `Msg.OpenRenameDialog`).
- `lexemeLabel` для `CardinalityDowngradePreviewWidget` — простейший stub `{ id -> "Lexeme #$id" }` либо resolve через `state.userDefinedTypes` index; точная реализация — implement-уровень.

Зависит от `#5..#13` (все widgets/dialogs которые mount'ит).

### #15 `PerDictionaryComponentsScreen.kt` [~]

Зеркальные изменения для PerDict:
- Import paths → shared module.
- Mount `EditComponentDialog` (БЕЗ multi-dict picker; widget сам не покажет scope_slot потому что вызов `CreateComponentDialog(hostVariant = HostVariant.PerDict)`).
- `PerDictRowWidget` — `onEdit = Msg.OpenEditDialog(it)`.
- `CreateComponentDialog` параметры: `availableDictionaries = emptyList()`, `selectedDictionaryIds = emptySet()`, `hostVariant = HostVariant.PerDict`, `onDictionaryToggle = {}` (no-op), `onScopeChange = {}` (no-op — scope hardcoded в Reducer).

Зависит от `#5, #6, #8, #10..#13`.

### #16..#23 Удаления Manager widget/ (8 файлов) [-]

Все 8 файлов из `modules/screen/components_manager/.../widget/` удаляются после того как `#14` переключён на shared module. Зависимость только от `#14` (screen больше не импортирует эти пакеты).

### #24..#31 Удаления PerDict widget/ (8 файлов) [-]

Все 8 файлов из `modules/screen/per_dictionary_components/.../widget/` удаляются после `#15`.

---

## Порядок реализации (топологически)

1. **Tier 0** (#0..#2): `templates/*` — независимые композиты, можно параллельно.
2. **Tier 1** (#3..#9): widgets — extension funcs + Empty/Fab/Row/Preview, base primitives only.
3. **Tier 2** (#10..#13): dialogs — depend на Tier 1.
4. **Tier 3** (#14, #15): screens — переключают import paths и mount'ят dialogs.
5. **Tier 4** (#16..#31): deletions — только после того как `gradle assembleDebug` PASS с новыми путями (screens больше не используют старые widget'ы).

## Открытые вопросы

- **Coupling Row widgets и EditDialog на mate-типы** (`UserDefinedRow`, `PerDictRow`, `EditDialogState`, `ImpactedLexemesPreview`). Варианты:
  1. Принять coupling — shared module dep'ится на оба screen-модуля (создаёт цикл деп: shared widget ← screen, screen ← shared widget — циклически валидно, но плохо архитектурно).
  2. Лифтить типы в `:modules:domain:lexeme` либо в shared widget module (как display-only).
  3. Разложить API на плоские примитивы (длинная сигнатура, но clean).
  Решение — implement-уровень. Рекомендация: вариант 3 (плоские), потому что widget не должен знать о screen-mate.
- **`lexemeLabel` callback resolve** для `CardinalityDowngradePreviewWidget`. На phase 2 hostside есть только ids; нужна либо отдельная UseCase query (`getLexemesByIds(ids: List<Long>)`), либо stub `"Lexeme #$id"`. Backlog data sub-flow.
- **Drill-in destination** для «Показать все» — backlog (bottom-sheet либо отдельный screen). На phase 2 callback no-op либо TODO snackbar.
- **DictionaryRef vs DictionaryApiEntity**: маппинг на host-уровне; `DictionaryRef` объявляется в shared widget module (избегаем dep `:core:core-db-api` в widget module).

## log_messages

- Read ui_layout.md (720 строк), business_summary.md (140), ui_walkthrough.md (190); собран DAG из 32 узлов: 14 NEW shared widget + 2 screen modify + 16 deletions.
- Верифицированы реальные пути ls: 8 файлов в каждом screen widget/ (Manager + PerDict зеркальны кроме UserDefinedRowWidget vs PerDictRowWidget); shared module package пуст.
- Зафиксированы 3 открытых вопроса для implement-уровня: coupling shared widget на mate-типы, lexemeLabel resolve, drill-in destination.

_model: claude-opus-4-7[1m]_
