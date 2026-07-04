# UI design tree: IS481 component_constructor

DAG-граф UI-файлов для двух новых экранов (`ComponentsManagerScreen` / `PerDictionaryComponentsScreen`), 9 новых ❇️ composable-виджетов, 2 🔄 модифицируемых host-композайбла (`DictionaryAppBar` / `SettingsTabScreen`), а также resources (drawable + strings) и расширений `build.gradle.kts` под новые зависимости.

Контекст — `ui_layout.md` (14 widgets, 2 screens + 2 host-вставки). Infra phase уже создала placeholder Composables — UI пишется поверх scaffold.

## Часть 1: Граф

```yaml
# ============================================================
# Tier 0 — Resources (core-resources)
# ============================================================
- id: 1
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/drawable/ic_hammer.xml
  action: "+"
  depends: []

- id: 2
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/drawable/ic_components.xml
  action: "+"
  depends: []

- id: 3
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/values/strings.xml
  action: "~"
  depends: []

- id: 4
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/values-ru-rRU/strings.xml
  action: "~"
  depends: []

# ============================================================
# Tier 1 — build.gradle.kts (новые UI deps в screen-модулях)
# ============================================================
- id: 5
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/build.gradle.kts
  action: "~"
  depends: []

- id: 6
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/build.gradle.kts
  action: "~"
  depends: []

# ============================================================
# Tier 2 — Tier 2 ❇️ widgets (без зависимостей от других новых widget'ов)
# ============================================================
- id: 7
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/widgets/settings/items/ComponentsManageWidget.kt
  action: "+"
  depends: [2, 3]

- id: 8
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/widget/ComponentsToolsIconButton.kt
  action: "+"
  depends: [1, 3]

- id: 9
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/CreateComponentFab.kt
  action: "+"
  depends: [3]

- id: 10
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/ComponentsEmptyStateWidget.kt
  action: "+"
  depends: [2, 3]

- id: 11
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/UserDefinedRowWidget.kt
  action: "+"
  depends: [2, 3, 5]

- id: 12
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/PerDictRowWidget.kt
  action: "+"
  depends: [2, 3, 6]

- id: 13
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/CreateComponentDialog.kt
  action: "+"
  depends: [3, 5]

- id: 14
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/RenameComponentDialog.kt
  action: "+"
  depends: [3, 5]

- id: 15
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/DeleteComponentConfirmDialog.kt
  action: "+"
  depends: [3, 5]

# ============================================================
# Tier 3 — modify 🔄 хост-композайблы (используют новые Tier 2 widgets)
# ============================================================
- id: 16
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBar.kt
  action: "~"
  depends: [8]

- id: 17
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsTabScreen.kt
  action: "~"
  depends: [7]

# ============================================================
# Tier 4 — Screens (replace placeholders)
# ============================================================
- id: 18
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerScreen.kt
  action: "~"
  depends: [3, 9, 10, 11, 13, 14, 15]

- id: 19
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsScreen.kt
  action: "~"
  depends: [3, 9, 10, 12, 13, 14, 15]
```

Параллельность: Tier 0 (#1-4) идёт первой, всё остальное вне Tier 0 зависит от строк. Tier 1 (#5, #6) — параллельно. Tier 2 (#7-15) — параллельно после Tier 0/1. Tier 3 (#16, #17) — после своих Tier 2 деталей. Tier 4 (#18, #19) — последний, dependencies на FAB / EmptyState / RowWidget / 3 диалога.

## Часть 2: Детали изменений

### Tier 0 — Resources

#### #1 `ic_hammer.xml` [+]

Vector drawable «молоток / инструменты» для `ComponentsToolsIconButton` (`DictionaryAppBar.actions`). 24×24 dp, fillColor=`#FF252628` (matches `enableIconColor`). Material-style молоток (head + handle). Существующих icon'ов с такой семантикой нет (verified `ui_walkthrough.md` § DictionaryAppBar — список drawable'ов).

#### #2 `ic_components.xml` [+]

Vector drawable «компоненты» (стилизованный блок-каркас / 3 квадрата) для `ComponentsManageWidget` (Settings entry), `UserDefinedRowWidget` / `PerDictRowWidget` (leading icon), `ComponentsEmptyStateWidget` (centered icon). 24×24 dp.

#### #3 `strings.xml` (en) [~]

**Добавить** keys (плоский список, без namespace, parity с `settings_section_lang_management`):

```xml
<!-- Settings entry -->
<string name="settings_section_components_management">Components</string>

<!-- DictionaryAppBar -->
<string name="components_tools_description">Manage components for this dictionary</string>

<!-- ComponentsManagerScreen -->
<string name="components_manager_title">Components</string>

<!-- PerDictionaryComponentsScreen -->
<string name="per_dict_components_title">Components in dictionary</string>

<!-- Empty state -->
<string name="components_empty_headline_manager">No custom components yet</string>
<string name="components_empty_body_manager">Translation works automatically in every dictionary.</string>
<string name="components_empty_headline_per_dict">Only translation in this dictionary</string>
<string name="components_empty_body_per_dict">Add another component to extend your cards.</string>
<string name="components_empty_cta">Create component</string>

<!-- FAB -->
<string name="components_create_cta">Create</string>

<!-- Create dialog -->
<string name="components_create_dialog_title">New component</string>
<string name="components_create_field_name">Name</string>
<string name="components_create_name_placeholder">e.g. Example sentence</string>
<string name="components_create_field_template">Type of value</string>
<string name="components_create_field_is_multi">Allow multiple values per card</string>
<string name="components_create_field_scope">Where to use</string>
<string name="components_create_scope_global">All dictionaries</string>
<string name="components_create_scope_per_dict">Selected dictionaries</string>
<string name="components_create_scope_picker_label">Choose dictionaries (%1$d)</string>

<!-- Rename dialog -->
<string name="components_rename_dialog_title">Rename component</string>
<string name="components_rename_original">Current name</string>
<string name="components_rename_new_name">New name</string>
<string name="components_rename_placeholder">New name</string>

<!-- Delete dialog -->
<string name="components_delete_dialog_title">Delete \"%1$s\"?</string>
<string name="components_delete_impact_values">%1$d existing values will be hidden</string>
<string name="components_delete_impact_dicts">Affects %1$d dictionary(s)</string>
<string name="components_delete_impact_quiz">Removed from %1$d quiz config(s)</string>
<string name="components_delete_impact_prefs">%1$d dictionary(s) will reset quiz pick</string>
<string name="components_delete_impact_unavailable">Impact preview unavailable</string>
<string name="components_delete_hint">Component will be soft-deleted. Values stay in the database but become invisible.</string>

<!-- NameError messages -->
<string name="components_name_error_empty">Name can\'t be empty</string>
<string name="components_name_error_too_long">Name is too long</string>
<string name="components_name_error_same_scope_collision">A component with this name already exists in this scope</string>
<string name="components_name_error_cross_scope_collision">A component with this name already exists elsewhere</string>

<!-- Common chip labels -->
<string name="components_chip_single">single</string>
<string name="components_chip_multi">multi</string>
<string name="components_chip_global">global</string>

<!-- Template labels (UI extension ComponentTemplate.labelRes(), see #11 note) -->
<string name="components_template_text">Text</string>
<string name="components_template_image">Image</string>

<!-- Button labels (re-use if existing project string aliases collide; here listed explicitly) -->
<string name="components_button_create">Create</string>
<string name="components_button_save">Save</string>
<string name="components_button_delete">Delete</string>
<string name="components_button_cancel">Cancel</string>
```

**Сигнатуры функций не меняются** — только resource ids. Реальные тексты (микрокопия) на implement — выше — best-guess дефолты.

#### #4 `strings.xml` (ru-rRU) [~]

Зеркальный набор RU-локализаций. Same keys, переводы наполняются на implement.

### Tier 1 — build.gradle.kts

#### #5 `modules/screen/components_manager/build.gradle.kts` [~]

**Было** (текущий dependencies блок):
```kotlin
dependencies {
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":modules:core:logger"))
    implementation(project("path" to ":modules:core:tools"))
    implementation(project("path" to ":modules:domain:lexeme"))
    ...
}
```

**Стало** — добавить:
```kotlin
implementation(project("path" to ":core:core-resources"))
implementation(composeLibs.activityCompose)
implementation(project("path" to ":modules:widget:component_widgets")) // если template-preview composable будет показываться в Create/Rename dialogs
```

Rationale (transitive R-aggregation):
- `:core:core-resources` — новые widgets (`UserDefinedRowWidget`, dialogs, `ComponentsEmptyStateWidget`) импортируют `R.drawable.ic_components` / `R.string.*` напрямую. На текущий момент модуль не зависит от `:core:core-resources` (см. existing build.gradle.kts:27-44). В Android resource references из соседнего модуля не аггрегируются транзитивно через `:modules:core:ui` — нужна явная dep.
- `composeLibs.activityCompose` — `ComponentsManagerScreen` использует `BackHandler { ... }` (см. узел #18). `BackHandler` живёт в `androidx.activity.compose`, который не приходит транзитивно из `:modules:core:ui`. Parity с `:modules:screen:settingstab/build.gradle.kts:40` (там тоже явный `implementation(composeLibs.activityCompose)` под `BackHandler`).

#### #6 `modules/screen/per_dictionary_components/build.gradle.kts` [~]

Симметрично #5: добавить:
```kotlin
implementation(project("path" to ":core:core-resources"))
implementation(composeLibs.activityCompose)
```

Опционально `:modules:widget:component_widgets` если template-preview reused.

Rationale тот же что и в #5 (transitive R-aggregation + `BackHandler` в `PerDictionaryComponentsScreen` #19).

### Tier 2 — ❇️ widgets

#### #7 `ComponentsManageWidget.kt` [+]

Settings drill-in entry, рядом с `LangManageWidget`. Прямой wrapper поверх `SettingsItemWidget` (parity с `LangManageWidget.kt`, 11 строк).

```kotlin
@Composable
fun ComponentsManageWidget(
    onClick: () -> Unit,
) {
    SettingsItemWidget(
        iconRes = R.drawable.ic_components,
        titleRes = R.string.settings_section_components_management,
        showNextIcon = true,
        onClick = onClick,
    )
}
```

Callback: `onClick → sendMessage(Msg.OpenComponentsManager)` (вызывается из `SettingsTabScreen.kt`). Location: `modules/screen/settingstab/.../widgets/settings/items/` рядом с `LangManageWidget.kt`.

#### #8 `ComponentsToolsIconButton.kt` [+]

Icon-button «молоток» в `DictionaryAppBar.actions`. Wrapper поверх `IconBoxed` (Tier 1 primitive).

```kotlin
@Composable
internal fun ComponentsToolsIconButton(
    onClick: () -> Unit,
) {
    IconBoxed(
        iconRes = R.drawable.ic_hammer,
        contentDescriptionRes = R.string.components_tools_description,
        enabled = true,
        colorEnabled = enableIconColor,
        size = 44,
        onClick = onClick,
    )
}
```

Callback: `onClick → sendMessage(Msg.OpenPerDictionaryComponents(currentDict.id))`. Visibility: контролируется callsite'ом в `DictionaryAppBar.kt` (видим при `currentDict != null && !isLoading`). Location: `modules/widget/dictionaryappbar/.../widget/` (рядом с `AppBarTitleWidget.kt`).

#### #9 `CreateComponentFab.kt` [+]

FAB на обоих экранах. Wrapper поверх `PrimaryLongFabWidget` (либо `PrimaryFabWidget` для round-only). Финальный variant — на implement.

```kotlin
@Composable
internal fun CreateComponentFab(
    onClick: () -> Unit,
) {
    PrimaryLongFabWidget(
        iconRes = R.drawable.ic_add,
        labelRes = R.string.components_create_cta,
        onClick = onClick,
    )
    // либо: PrimaryFabWidget(iconRes = R.drawable.ic_add, onClick = onClick)
}
```

Callback: `onClick → Msg.OpenCreateDialog`. Live в `:modules:screen:components_manager` source set, **переиспользуется в обоих screens** через `internal` модификатор (либо вытащить в `:modules:widget:component_widgets`, если потребуется third-party-reuse). Финализация — на implement; для DAG достаточно «один файл, импортируем из обоих Screens».

#### #10 `ComponentsEmptyStateWidget.kt` [+]

Empty-state composable, общий для обоих экранов. Variant через resIds (`headlineRes`, `bodyRes`).

```kotlin
@Composable
internal fun ComponentsEmptyStateWidget(
    @StringRes headlineRes: Int,
    @StringRes bodyRes: Int,
    @StringRes ctaRes: Int = R.string.components_empty_cta,
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painter = painterResource(R.drawable.ic_components), tint = unselectedGreyColor, modifier = Modifier.size(64.dp))
        Text(text = stringResource(headlineRes), style = LexemeStyle.H6)
        Text(text = stringResource(bodyRes), style = LexemeStyle.BodyL, color = grayTextColor)
        PrimaryFullButtonWidget(text = stringResource(ctaRes), onClick = onCreate)
    }
}
```

Callsite variants (см. ui_layout `behavior:`):
- Manager: `headlineRes = R.string.components_empty_headline_manager`, `bodyRes = R.string.components_empty_body_manager`.
- PerDict: `R.string.components_empty_headline_per_dict`, `R.string.components_empty_body_per_dict`.

Callback: `onCreate → Msg.OpenCreateDialog`. Location: `:modules:screen:components_manager/widget/` (либо `:modules:widget:component_widgets`, если хочется shared module — на implement).

#### #11 `UserDefinedRowWidget.kt` [+]

Aggregated CRUD row для `ComponentsManagerScreen` (1 row per UserDefinedRow).

Сигнатура:
```kotlin
@Composable
internal fun UserDefinedRowWidget(
    row: UserDefinedRow,
    onEdit: (ComponentTypeId) -> Unit,
    onDelete: (ComponentTypeId) -> Unit,
)
```

Структура (см. `ui_layout.md` § UserDefinedRowWidget):
- Surface `RoundedCornerShape(12.dp)` + Row spacing=12, padding h:16 v:12.
- Leading: `Icon(painter = R.drawable.ic_components, size = 24, tint = enableIconColor)`.
- Content Column (weight 1, spacing 4):
  - Title: `Text(row.name, style = LexemeStyle.BodyL)`.
  - Meta Row spacing 8:
    - Template chip `AssistChip(label = stringResource(row.template.labelRes()), style = labelSmall)`.
    - Cardinality chip `AssistChip(label = stringResource(if (row.isMulti) R.string.components_chip_multi else R.string.components_chip_single))`.
    - Usage text `"${row.usageCount} · ${row.dictionaryNames.joinToString(", ").ifBlank { "—" }}"`, BodyS / grayTextColor.
    - Если `row.scope is Scope.Global` — global badge chip перед usage_text.
- Trailing: `IconBoxed(R.drawable.ic_edit, 44, onClick = { onEdit(row.typeId) })`.
- Trailing 2: `IconBoxed(R.drawable.ic_trash, 44, onClick = { onDelete(row.typeId) })`.

Callbacks: `onEdit → Msg.OpenRenameDialog(typeId)`, `onDelete → Msg.OpenDeleteConfirm(typeId)`. Location: `:modules:screen:components_manager/widget/`.

**⚠ Verified API gap**: `ComponentTemplate.displayName` **не существует** (см. `modules/domain/lexeme/.../ComponentTemplate.kt:11-21` — есть только `key: String` + `fields: List<Field>`). UI-фаза вводит локальный UI-extension `ComponentTemplate.labelRes(): Int` (mapping enum → StringRes), размещённый в `:modules:screen:components_manager/widget/ComponentTemplateLabel.kt` (либо inline в `UserDefinedRowWidget.kt` — на implement). Mapping:
- `ComponentTemplate.TEXT → R.string.components_template_text` (новый key в #3/#4).
- `ComponentTemplate.IMAGE → R.string.components_template_image` (новый key в #3/#4).

Доп. string keys для #3/#4:
```xml
<string name="components_template_text">Text</string>
<string name="components_template_image">Image</string>
```

#### #12 `PerDictRowWidget.kt` [+]

Зеркально #11 для `PerDictionaryComponentsScreen`.

Сигнатура:
```kotlin
@Composable
internal fun PerDictRowWidget(
    row: PerDictRow,
    onEdit: (ComponentTypeId) -> Unit,
    onDelete: (ComponentTypeId) -> Unit,
)
```

Отличия от #11:
- Title slot: Row `row.name` + `if (row.isGlobal) AssistChip(stringResource(R.string.components_chip_global))`.
- Meta: template/cardinality chips (использует тот же `ComponentTemplate.labelRes()` extension из #11) + `"${row.valueCount} values"`.
- Edit-icon enabled=true (UseCase enforce'ит rule-set).

Callbacks: те же `onEdit`/`onDelete`. Location: `:modules:screen:per_dictionary_components/widget/`.

#### #13 `CreateComponentDialog.kt` [+]

Full-form диалог (name + template + isMulti). **MVP scope=Global only** — UI не показывает scope-section, reducer hardcode'ит `scope = Scope.Global` (либо `scope = Scope.PerDictionaries(listOf(state.dictionaryId))` если открыто из `PerDictionaryComponentsScreen`, см. #19). PerDictionaries-выбор из manager-экрана — phase 2 (Backlog). Поверх `LexemeDialog` wrapper (Tier 1 primitive в `core/ui/dialog/base/`).

Сигнатура:
```kotlin
@Composable
internal fun CreateComponentDialog(
    createDialog: CreateDialogState,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onTemplateSelect: (ComponentTemplate) -> Unit,
    onMultiToggle: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
)
```

Структура (`ui_layout.md` § CreateComponentDialog) — Column inside `LexemeDialog`:
- title (`H6`).
- Name section: label + `LexemeTextFieldWidget(value, onValueChange = onNameChange)` + error (visible ∀ nameError != null).
- Template radio-group: `forEach(ComponentTemplate.values()) { LexemeRadioMenuItem(isSelected = (it == createDialog.template), title = stringResource(it.labelRes()), onSelect = { onTemplateSelect(it) }) }` (использует UI-extension из #11; `ComponentTemplate.displayName` НЕ существует в domain).
- Multi row: `Checkbox(checked = createDialog.isMulti, onCheckedChange = onMultiToggle)` + label.
- **Scope section — DROPPED for MVP** (см. § Open Q ниже).
- Actions Row: `CancelButtonWidget(weight 1, onClick = onDismiss)` + `PrimaryFullButtonWidget(weight 1, enabled = canSubmit, label = button_create, onClick = onSubmit)`.

`canSubmit = createDialog.name.isNotBlank() && createDialog.nameError == null && !isSubmitting`.

Callbacks → Msg:
- `onNameChange → Msg.CreateNameChange(value)`.
- `onTemplateSelect → Msg.CreateTemplateChange(template)`.
- `onMultiToggle → Msg.CreateMultiToggle(isMulti)`.
- `onSubmit → Msg.SubmitCreate`.
- `onDismiss → Msg.CloseCreateDialog`.

**MVP scope decision**: `business_design_tree.md` контракт `Msg.CreateScopeChange(scope: Scope)` остаётся в Mate (reducer вызывает internally / ignored для UI вызовов), но UI **не показывает** scope-control. Reducer на `OpenCreateDialog` инициализирует:
- из `ComponentsManagerScreen` → `scope = Scope.Global`.
- из `PerDictionaryComponentsScreen` → `scope = Scope.PerDictionaries(listOf(dictionaryId))`.

PerDictionaries-выбор multiple-dicts из manager-экрана — phase 2 (см. § Open Q и Backlog запись).

Location: `:modules:screen:components_manager/widget/` (single source for both screens; per-dict screen импортирует тот же файл).

#### #14 `RenameComponentDialog.kt` [+]

Minimal single-field диалог переименования.

Сигнатура:
```kotlin
@Composable
internal fun RenameComponentDialog(
    renameDialog: RenameDialogState,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
)
```

Структура (`ui_layout.md` § RenameComponentDialog) — Column inside `LexemeDialog`:
- title (`H6`).
- Original-name display (read-only label + value).
- New-name input: `LexemeTextFieldWidget(value = renameDialog.editedName, onValueChange = onNameChange)` + error.
- Actions Row: Cancel + PrimaryFullButton (Save, `enabled = editedName.isNotBlank() && editedName != originalName && nameError == null && !isSubmitting`).

Callbacks → Msg:
- `onNameChange → Msg.RenameTextChange(value)`.
- `onSubmit → Msg.SubmitRename`.
- `onDismiss → Msg.CloseRenameDialog`.

Location: `:modules:screen:components_manager/widget/`.

#### #15 `DeleteComponentConfirmDialog.kt` [+]

Confirm-delete с impact-preview. **Не использует `AlarmDialogWidget`** напрямую (нужен dynamic impact-content) — сборка поверх `LexemeDialog`.

Сигнатура:
```kotlin
@Composable
internal fun DeleteComponentConfirmDialog(
    deleteConfirm: DeleteConfirmState,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
)
```

Структура (`ui_layout.md` § DeleteComponentConfirmDialog) — Column inside `LexemeDialog`:
- title `R.string.components_delete_dialog_title` (с `name` substitution).
- impact block:
  - `if (deleteConfirm.isLoadingImpact && deleteConfirm.impact == null)` → `CircularProgressIndicator(size = 24)`.
  - `if (deleteConfirm.impact != null)` → counts:
    - values_line: `stringResource(R.string.components_delete_impact_values, impact.valueCount)`.
    - dicts_line: `..._dicts, impact.dictionariesWithValues.size`.
    - quiz_line: `..._quiz, impact.quizConfigsAffected.size` (visible if > 0).
    - prefs_line: `..._prefs, impact.affectedPrefs.size` (visible if > 0).
  - `if (impact == null && !isLoadingImpact)` → `Text(R.string.components_delete_impact_unavailable, BodyL, grayTextColor)`.
- hint: `R.string.components_delete_hint`, BodyS, grayTextColor.
- Actions Row: `CancelButtonWidget(weight 1, onClick = onDismiss)` + `AlarmButtonWidget(weight 1, enabled = canConfirm, label = button_delete, onClick = onConfirm)`.

`canConfirm = !deleteConfirm.isLoadingImpact && !isSubmitting`.

Callbacks → Msg:
- `onConfirm → Msg.ConfirmDelete`.
- `onDismiss → Msg.CloseDeleteConfirm`.

Location: `:modules:screen:components_manager/widget/`.

### Tier 3 — 🔄 host modifications

#### #16 `DictionaryAppBar.kt` [~]

**Было** (current actions slot, DictionaryAppBar.kt:50-75):
```kotlin
TopAppBar(
    title = { AppBarTitleWidget(titleResId = titleResId) },
    actions = {
        if (state.isLoading) {
            CircularProgressIndicator(...)
        } else {
            DictDropDownWidget(...)
        }
    }
)
```

**Стало** — добавить icon-button «молоток» перед `DictDropDownWidget` в else-ветке (видим при `currentDict != null`):
```kotlin
TopAppBar(
    title = { AppBarTitleWidget(titleResId = titleResId) },
    actions = {
        if (state.isLoading) {
            CircularProgressIndicator(...)
        } else {
            if (state.currentDict != null) {
                ComponentsToolsIconButton(
                    onClick = { sendMessage(Msg.OpenPerDictionaryComponents(dictionaryId = state.currentDict.id)) },
                )
            }
            DictDropDownWidget(...)
        }
    }
)
```

`Msg.OpenPerDictionaryComponents(dictionaryId: Long)` уже определён (infra phase, `mate/Message.kt:37`); reducer + navigator chain готовы. UI задача — единственный insertion.

#### #17 `SettingsTabScreen.kt` [~]

**Было** (current first section, SettingsTabScreen.kt:90-99):
```kotlin
item {
    SettingsSectionWidget {
        LangManageWidget(onClick = { sendMessage(Msg.OpenLangManagement) })
        ExportDataWidget(...)
        ImportDataWidget(...)
    }
}
```

**Стало** — вставить `ComponentsManageWidget` между `LangManageWidget` и `ExportDataWidget`:
```kotlin
item {
    SettingsSectionWidget {
        LangManageWidget(onClick = { sendMessage(Msg.OpenLangManagement) })
        ComponentsManageWidget(onClick = { sendMessage(Msg.OpenComponentsManager) })
        ExportDataWidget(...)
        ImportDataWidget(...)
    }
}
```

`Msg.OpenComponentsManager` уже определён (infra phase, `logic/Message.kt:10`); reducer + navigator chain готовы.

### Tier 4 — Screens

#### #18 `ComponentsManagerScreen.kt` [~]

**Замечание про file-opt-in**: файл использует `TopAppBar` (material3 experimental API), поэтому требуется `@file:OptIn(ExperimentalMaterial3Api::class)` в первой строке файла (parity с `DictionaryAppBar.kt:1`).

**Было** (current placeholder, ComponentsManagerScreen.kt:17-32):
```kotlin
@Composable
fun ComponentsManagerScreen(factory, navigator, viewModel = viewModel(...)) {
    Box(fillMaxSize, contentAlignment = Alignment.Center) {
        Text("TODO: UI in ui_implement")
    }
}
```

**Стало** — реальный UI поверх state-driven Mate-обвязки:
```kotlin
@Composable
fun ComponentsManagerScreen(
    factory: ComponentsManagerViewModel.Factory,
    navigator: ComponentsManagerNavigator,
    viewModel: ComponentsManagerViewModel = viewModel(factory = viewModelFactory { factory.create(navigator) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler { viewModel.accept(Msg.RequestBack) }

    // State-driven snackbar
    LaunchedEffect(state.snackbarState) {
        state.snackbarState?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.accept(Msg.DismissSnackbar)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.components_manager_title)) },
                            navigationIcon = { IconButton(onClick = { viewModel.accept(Msg.RequestBack) }) { Icon(R.drawable.ic_back, null) } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { CreateComponentFab(onClick = { viewModel.accept(Msg.OpenCreateDialog) }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.userDefinedTypes == null ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.isEmpty ->
                    ComponentsEmptyStateWidget(
                        headlineRes = R.string.components_empty_headline_manager,
                        bodyRes = R.string.components_empty_body_manager,
                        onCreate = { viewModel.accept(Msg.OpenCreateDialog) },
                    )
                state.userDefinedTypes != null ->
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.userDefinedTypes!!, key = { it.typeId.value }) { row ->
                            UserDefinedRowWidget(
                                row = row,
                                onEdit = { viewModel.accept(Msg.OpenRenameDialog(it)) },
                                onDelete = { viewModel.accept(Msg.OpenDeleteConfirm(it)) },
                            )
                        }
                    }
            }
        }
    }

    // Dialog overlays
    state.createDialog?.let { dialog ->
        CreateComponentDialog(
            createDialog = dialog,
            isSubmitting = state.isCreating,
            onNameChange = { viewModel.accept(Msg.CreateNameChange(it)) },
            onTemplateSelect = { viewModel.accept(Msg.CreateTemplateChange(it)) },
            onMultiToggle = { viewModel.accept(Msg.CreateMultiToggle(it)) },
            onSubmit = { viewModel.accept(Msg.SubmitCreate) },
            onDismiss = { viewModel.accept(Msg.CloseCreateDialog) },
        )
    }
    state.renameDialog?.let { dialog ->
        RenameComponentDialog(
            renameDialog = dialog,
            isSubmitting = state.isRenaming,
            onNameChange = { viewModel.accept(Msg.RenameTextChange(it)) },
            onSubmit = { viewModel.accept(Msg.SubmitRename) },
            onDismiss = { viewModel.accept(Msg.CloseRenameDialog) },
        )
    }
    state.deleteConfirm?.let { dialog ->
        DeleteComponentConfirmDialog(
            deleteConfirm = dialog,
            isSubmitting = state.isDeleting,
            onConfirm = { viewModel.accept(Msg.ConfirmDelete) },
            onDismiss = { viewModel.accept(Msg.CloseDeleteConfirm) },
        )
    }
}
```

#### #19 `PerDictionaryComponentsScreen.kt` [~]

**Замечание про file-opt-in**: то же что #18 — `@file:OptIn(ExperimentalMaterial3Api::class)` обязателен (TopAppBar — experimental).

Зеркально #18. Отличия:
- Сигнатура: `PerDictionaryComponentsScreen(dictionaryId: Long, factory, navigator)` — `dictionaryId` already принимается, прокидывается `factory.create(dictionaryId, navigator)` (infra phase).
- Title: `Text(state.dictionaryName ?: stringResource(R.string.per_dict_components_title))`.
- Rows: `PerDictRowWidget(row, onEdit, onDelete)` (PerDictRow, не UserDefinedRow).
- EmptyState variant: `headlineRes = R.string.components_empty_headline_per_dict`, `bodyRes = R.string.components_empty_body_per_dict`.
- Msg/Effect/Reducer — параллельные типы из `:modules:screen:per_dictionary_components.mate.*`.
- Scope preselect — обработан в Reducer (`OpenCreateDialog` инициализирует `scope = Scope.PerDictionaries(listOf(state.dictionaryId))`, см. business_summary § Tier 6); UI не задаёт.

## Часть 3: UI dependencies (N/A)

Этот шаг — sub-flow `ui`, UI dependencies от business sub-flow получены через `business_design_tree.md` § UI dependencies (см. business_summary). Этот документ — финальный UI DAG, дальше декларировать нечего.

---

## Audit checklist (lesson IS481cc-F3)

Применимы критерии layer-boundary, callsite-валидность, mate-contract synchronization.

### 1. Все file-paths проверены и существуют (для `~`) либо имеют валидный parent dir (для `+`)

- [x] #1, #2 `core-resources/drawable/` — parent существует (см. find `ic_*.xml`).
- [x] #3, #4 `core-resources/values/strings.xml`, `values-ru-rRU/strings.xml` — existing file (key `settings_section_lang_management` уже там).
- [x] #5, #6 `build.gradle.kts` — existing files (content прочитан).
- [x] #7 Settings widget — parent `widgets/settings/items/` существует (см. LangManageWidget.kt). Note: ❗ нельзя создать `:modules:screen:settingstab/.../R.drawable.ic_components` reference из widget'а если settingstab уже зависит от core-resources — проверить existing dep.
- [x] #8 `dictionaryappbar/widget/` — existing (AppBarTitleWidget.kt:13).
- [x] #9-15 — все `:modules:screen:components_manager/widget/` либо `per_dictionary_components/widget/` — parent dirs не существуют, будут созданы. Это **expected** (новый widget package).
- [x] #16, #17 — `~` на existing files.
- [x] #18, #19 — `~` placeholder Screen files, существуют.
- [x] mate-контракт sync с `SettingsTabReducer.kt:34-36` — verified.

### 2. Все Tier 2 widgets dependancy-связаны с Tier 0 resources

- [x] #7 ComponentsManageWidget → ic_components (#2) + R.string.settings_section_components_management (#3).
- [x] #8 ComponentsToolsIconButton → ic_hammer (#1) + R.string.components_tools_description (#3).
- [x] #9 CreateComponentFab → ic_add (existing) + R.string.components_create_cta (#3).
- [x] #10 ComponentsEmptyStateWidget → ic_components (#2) + 5 string keys (#3).
- [x] #11 UserDefinedRowWidget → ic_components/edit/trash (#2 + existing) + chip-labels (#3).
- [x] #12 PerDictRowWidget — то же.
- [x] #13 CreateComponentDialog → 10 string keys (#3) + #5 (build.gradle dep).
- [x] #14 RenameComponentDialog → 4 string keys (#3) + #5.
- [x] #15 DeleteComponentConfirmDialog → 7 string keys (#3) + #5.

### 3. Все Mate-контракты (Msg / Effect / State) уже определены в business-фазе

- [x] Все callbacks → Msg сверены с `:modules:screen:components_manager/mate/Msg.kt` и `:modules:screen:per_dictionary_components/mate/Msg.kt` (прочитаны).
- [x] State shapes (`ComponentsManagerScreenState`, `PerDictionaryComponentsScreenState`, `UserDefinedRow`, `PerDictRow`, `CreateDialogState`, `RenameDialogState`, `DeleteConfirmState`, `SnackbarState`) — определены в `mate/State.kt` обоих модулей.
- [x] UI-фаза **не определяет новых** Msg / Effect / State полей. Все widget params читают existing state shapes.

### 4. Layer boundary

- [x] **Ни один узел не модифицирует** `logic/` / `mate/` business-файлы (Reducer уже работает; mate-контракт sync verified в § Audit checklist § 1).
- [x] **Все узлы — UI** (Composables / resources / build.gradle.kts UI-deps).
- [x] Нет узлов в `domain/`, `core-db-api/`, `core-db-impl/`, `app/di/module/`.

### 5. Циклов в DAG нет

- [x] Сверка зависимостей: #18 → {#9, #10, #11, #13, #14, #15, #3}; #19 → {#9, #10, #12, #13, #14, #15, #3}. Tier 2 widgets → Tier 0/1 only. Tier 0 → []. Цикл невозможен.

### 6. Каждое создание `[+]` имеет полный путь, описание назначения, ключевые сигнатуры / структура

- [x] #1-15 — каждое описано в Часть 2.

### 7. Каждое изменение `[~]` имеет «было → стало»

- [x] #3, #4 strings.xml — «было» = existing file, «стало» = добавить keys (полный список).
- [x] #5, #6 build.gradle.kts — «было» = текущие deps, «стало» = +`:core:core-resources`.
- [x] #16 DictionaryAppBar.kt — «было»/«стало» приведены.
- [x] #17 SettingsTabScreen.kt — то же.
- [x] #18, #19 Screen-композайблы — «было» = placeholder Box+Text, «стало» = pseudocode под Mate-обвязку.

### 8. Отсутствие новых проектов / новых modulов

- [x] DAG **не добавляет новых модулей** — все Tier 2 widgets в existing modules (`:modules:screen:components_manager`, `:modules:screen:per_dictionary_components`, `:modules:screen:settingstab`, `:modules:widget:dictionaryappbar`). Infra phase уже создала `:modules:widget:component_widgets` — пуст и НЕ используется в этом DAG (см. open Q ниже).

### 9. Open questions

- **`:modules:widget:component_widgets` — пустой модуль**. Infra создал gradle setup + AndroidManifest, но source files нет. Текущий ui_design_tree размещает все widgets в screen-модулях (`:modules:screen:components_manager/widget/`). Если выяснится, что нужен template-preview composable (per `ComponentTemplate.fields` schema) или `ComponentBlock(name, content)` wrapper — нужен отдельный Tier 2 узел в `:modules:widget:component_widgets`. Решение отложено до implement (не блокирует текущий DAG).
- **Scope=Global only в MVP** [F158-resolved]: UI `CreateComponentDialog` НЕ показывает scope-control. Reducer инициализирует `scope` по контексту открытия (Manager → Global; PerDict → PerDictionaries([currentDict])). PerDictionaries multi-dict picker из manager-экрана — phase 2 (см. Backlog запись `IS481 phase 2: PerDictionaries multi-dict picker`).
- **`PrimaryFabWidget` vs `PrimaryLongFabWidget`** — финальный variant FAB на implement; контракт `#9` принимает любой из двух (один файл, один `onClick`).
- **Dialog dismissOnClickOutside / dismissOnBackPress поведение** — для Create/Rename диалогов есть аргумент против `dismissOnClickOutside = true` (юзер случайно теряет заполненную форму). Default `LexemeDialog` — на implement; узел не меняется.

---

## Сводка

- **Узлов:** 19.
- **Tiers:** 5 (0..4).
- **Создание `+`:** 13 (Tier 0 ресурсы + Tier 2 widgets).
- **Изменение `~`:** 6 (Tier 1 build.gradle + Tier 3/4 hosts/screens + strings).
- **No-op `.`:** 0.
- **Удаления:** 0 (все изменения additive, см. ui_layout § ❌ УДАЛЯЕМ).
- **Параллелизм:** Tier 0 → Tier 1 (параллельно по Tier 2) → Tier 3 → Tier 4.

---

## История ревью

### iter 2 (2026-06-17): F152 fixed

- Удалён узел #18 no-op marker; смысл перенесён в audit checklist.

### iter 3 (2026-06-17): F153-F158 fixes (6 findings, 1 critical F155, 5 minor)

- F153: rationale rewrite #5/#6 (transitive R-aggregation).
- F154: enableIconColor only.
- F155: +activity-compose dep в #5/#6 build.gradle.kts.
- F156: opt-in material3 annotation note.
- F157: ComponentTemplate.displayName verified, fix noted.
- F158: scope=Global MVP only; PerDictionaries → Backlog.

_model: claude-opus-4-7[1m]_
