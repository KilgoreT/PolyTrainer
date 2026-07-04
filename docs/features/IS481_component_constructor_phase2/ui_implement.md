# ui_implement

Implementation шаг ui sub-flow phase 2 IS481. Все 32 узла DAG из `ui_design_tree.md`
реализованы; build (`assembleDebug`) и `testDebugUnitTest` — PASS. Структура `:modules:widget:component_widgets`
наполнена 14 файлами (3 templates + 7 widgets + 4 dialogs); 16 файлов из обоих
screen-модулей удалены; оба screen'а переключены на shared module + получили mount
для `EditComponentDialog`.

## Узлы реализованы (32)

### Tier 0 — templates (3 NEW)

- **#0 `templates/TextWidget.kt`** — `@Composable fun TextWidget(value, editable, onValueChange)`. Read-only → `Text(style=LexemeStyle.BodyL)`; editable → `LexemeTextFieldWidget`.
- **#1 `templates/ComponentBlock.kt`** — wrapper `Column(spacing=4) { Text(type.name, BodyS+onSurfaceVariant) + content() }`. Pure structural. `LexemeStyle.LabelM` отсутствует в проекте → подставил `BodyS` (ближайший по семантике label).
- **#2 `templates/ComponentByTemplate.kt`** — exhaustive `when (type.template)`. `TEXT` → `ComponentBlock { TextWidget(...) }` с TextValues unwrap; `IMAGE` → пустой `ComponentBlock` (backlog).

### Tier 1 — shared widgets (7 NEW)

- **#3 `widgets/ComponentTemplateLabel.kt`** — `internal fun ComponentTemplate.labelRes(): Int`. 1-в-1 миграция из обоих screen-модулей.
- **#4 `widgets/NameErrorLabel.kt`** — `internal fun NameError.labelRes(): Int`. 1-в-1. `EditNameError` mapping вынесен в каждый screen (host-local — design tree open Q).
- **#5 `widgets/ComponentsEmptyStateWidget.kt`** — `Column(padding=32, spacing=16, alignCenter)` → `Icon(ic_components, 64)` + headline `Text(H6)` + body `Text(BodyL gray)` + `PrimaryFullButtonWidget`. `@PreviewWidget Preview()` сохранён.
- **#6 `widgets/CreateComponentFab.kt`** — wrapper `PrimaryLongFabWidget(ic_add, components_create_cta)`. `@PreviewWidget` сохранён.
- **#7 `widgets/UserDefinedRowWidget.kt`** — **плоский API** (примитивы: `typeId, name, template, isMulti, isGlobal, usageCount, dictionaryNames`). Render: Surface(rounded-12) + Row + leading IconBoxed + content column (name + meta chips: template/cardinality/global + usage text "usageCount · dictText") + 2 trailing IconBoxed (edit/delete). Coupling на mate `UserDefinedRow` устранён (design tree open Q vote: flatten).
- **#8 `widgets/PerDictRowWidget.kt`** — зеркал #7, плоский API. Различия по `ui_walkthrough.md § 2`: global chip в **title row** (не в meta row); usage_text = `per_dict_row_value_count` formatted (F161).
- **#9 `widgets/CardinalityDowngradePreviewWidget.kt`** — `Column(background=errorContainer, rounded-12, padding=8, spacing=8)` → title `Text(BodyLBold + onErrorContainer)` + inline rows `Text(BodyM + onErrorContainer)` + drill-in `TextButton(stringResource(R.string.components_edit_show_all, totalCount))` (conditional). Плоский API: `inlineIds, totalCount, showAllVisible, lexemeLabel: (Long) -> String, onShowAll`. `PrimaryTextButtonWidget` не поддерживает StringRes с args → fallback на M3 `TextButton`.

### Tier 2 — dialogs (4 NEW)

- **#10 `dialogs/RenameComponentDialog.kt`** — **плоский API**: `originalName, editedName, nameError, isSubmitting, onNameChange, onSubmit, onDismiss`. `canSubmit` локально (editedName.isNotBlank && != originalName && nameError == null && !isSubmitting). Render 1-в-1 phase 1.
- **#11 `dialogs/DeleteComponentConfirmDialog.kt`** — плоский API + lightweight `DeletionImpactRef` DTO (valueCount, dictCount, quizCount, prefsCount). 3-way: loading / impact != null с 4 conditional `Text` строк (skip iff count == 0) / unavailable. AlarmButton + CancelButton. `DeletionImpactRef` declared в том же файле для cohesion.
- **#12 `dialogs/CreateComponentDialog.kt`** — phase 2 multi-dict picker. API: `name, template, isMulti, scope, nameError, isSubmitting, availableDictionaries: List<DictionaryRef>, selectedDictionaryIds: Set<Long>, hostVariant: HostVariant, callbacks`. `HostVariant.Manager` рендерит scope_slot (radio Global/PerDict + FlowRow{FilterChip×N}); `HostVariant.PerDict` полностью скрывает scope_slot. `canSubmit = trim.isNotEmpty && nameError == null && !isSubmitting && (scope is Global || selectedDictionaryIds.isNotEmpty)`. `HostVariant` enum + `DictionaryRef` data class declared в том же файле.
- **#13 `dialogs/EditComponentDialog.kt`** — **NEW phase 2**, плоский API (без `EditDialogState` coupling). Принимает name/template/isMulti + original{Name,Template,IsMulti} + `nameErrorRes: Int?` + 3 preview параметра + `lexemeLabel: (Long) -> String`. `canSubmit = trim.isNotBlank && nameErrorRes == null && !isSubmitting && (dirty: any of name/template/isMulti changed)`. Template radio clickable (UseCase enforces TemplateImmutable). Preview slot рендерит `CardinalityDowngradePreviewWidget` iff `previewInlineIds != null`. Mate `EditNameError` мапится на StringRes в host'е (private `EditNameError.toLabelRes()` в каждом screen).

### Tier 3 — screen mounts (2 modify)

- **#14 `ComponentsManagerScreen.kt`** [~]:
  - Все 8 widget imports переключены `me.apomazkin.components_manager.widget.*` → `me.apomazkin.component_widgets.{dialogs|widgets|templates}.*`.
  - `UserDefinedRowWidget` теперь принимает плоские примитивы (`typeId, name, template, isMulti, isGlobal=row.scope is Scope.Global, usageCount, dictionaryNames`). `onEdit` теперь `Msg.OpenEditDialog(it)` (раньше OpenRenameDialog).
  - `CreateComponentDialog` получает `availableDictionaries.map { DictionaryRef(it.id, it.name) }` (Manager DictionaryApiEntity.name), `selectedDictionaryIds`, `hostVariant=Manager`, новые callbacks (`onScopeChange`, `onDictionaryToggle`).
  - `DeleteComponentConfirmDialog` получает `DeletionImpactRef` (host маппит counts).
  - **Mount `EditComponentDialog`** (visible iff `state.editDialog != null`). Преобразует `ImpactedLexemesPreview` sealed → 3 параметра (inlineIds, totalCount, showAllVisible). `lexemeLabel` resolve через `LocalContext.current.getString(R.string.components_edit_lexeme_label, id)` (placeholder; backlog: реальный label via UseCase). `onShowAllImpacted` — no-op (backlog: drill-in destination).
  - Private `EditNameError.toLabelRes()` extension для маппинга nameError.

- **#15 `PerDictionaryComponentsScreen.kt`** [~]:
  - Зеркально #14 для PerDict.
  - `PerDictRowWidget` принимает плоские примитивы (`typeId, name, template, isMulti, isGlobal=row.isGlobal, valueCount`).
  - `CreateComponentDialog` получает `availableDictionaries=emptyList`, `selectedDictionaryIds=emptySet`, `hostVariant=PerDict`, no-op `onScopeChange/onDictionaryToggle`.
  - Mount `EditComponentDialog` (БЕЗ scope picker — параметры те же что для Manager).
  - Private `EditNameError.toLabelRes()` extension.

### Tier 4 — deletions (16 deletions)

Все 8 файлов из `modules/screen/components_manager/.../widget/` (`CreateComponentDialog`, `RenameComponentDialog`, `DeleteComponentConfirmDialog`, `UserDefinedRowWidget`, `ComponentsEmptyStateWidget`, `CreateComponentFab`, `ComponentTemplateLabel`, `NameErrorLabel`) + 8 файлов из `modules/screen/per_dictionary_components/.../widget/` (`CreateComponentDialog`, `RenameComponentDialog`, `DeleteComponentConfirmDialog`, `PerDictRowWidget`, `ComponentsEmptyStateWidget`, `CreateComponentFab`, `ComponentTemplateLabel`, `NameErrorLabel`) — удалены через `rm -f`; пустые `widget/` директории удалены через `rmdir`. Build PASS подтверждает что нет dangling references.

## Strings добавлены (8 новых ключей)

`core/core-resources/src/main/res/values/strings.xml` + `values-ru-rRU/strings.xml`:

- `components_edit_dialog_title` — "Edit component" / "Редактировать компонент"
- `components_edit_field_name` — "Name" / "Название"
- `components_edit_field_template` — "Type of value" / "Тип значения"
- `components_edit_field_is_multi` — "Allow multiple values per card" / "Разрешить несколько значений на карточке"
- `components_edit_cardinality_blocked_title` — title preview block
- `components_edit_show_all` — "Show all (%1$d)" / "Показать все (%1$d)" — drill-in label с counter
- `components_edit_lexeme_label` — "Lexeme #%1$d" / "Лексема №%1$d" — fallback resolver (placeholder, backlog: real label)
- `components_create_field_scope` + `components_create_scope_global` + `components_create_scope_per_dict` — scope picker labels (Manager variant)

## Тесты

**Per-module test runs (sequential, не параллельно — per memory rule):**

- `./scripts/cc-build.sh :modules:widget:component_widgets:testDebugUnitTest` → **PASS** (нет unit tests, only Android resource compile).
- `./scripts/cc-build.sh :modules:screen:components_manager:testDebugUnitTest` → **PASS** (75 TDD tests от business sub-flow).
- `./scripts/cc-build.sh :modules:screen:per_dictionary_components:testDebugUnitTest` → **PASS**.
- `./scripts/cc-build.sh :app:testDebugUnitTest` → **PASS**.
- Full `./scripts/cc-build.sh testDebugUnitTest` → **PASS**.

Build (`./scripts/cc-build.sh assembleDebug`) — **PASS** на full app.

Тесты не правились (только code, как требуется).

## Нетривиальные решения

1. **Плоские примитивы для всех Tier 1/2 widgets** — UserDefinedRowWidget, PerDictRowWidget, RenameComponentDialog, DeleteComponentConfirmDialog, CreateComponentDialog, EditComponentDialog — все принимают плоские примитивы вместо mate state-объектов. Design tree открыл вопрос 3 варианта (coupling / lift-types / flatten); выбрал flatten — shared widget не знает о screen-mate (Dependency Rule). Pattern parity: каждый screen раскладывает state на параметры на mount-site.
2. **`LexemeStyle.LabelM` / `LabelL` отсутствуют** — fallback на `BodyS` (label) + `BodyLBold` (label-large). `LexemeStyle` объект содержит H1-H6 + BodyXL/L/M/S (+ Bold варианты), но не Label-семейство. Render семантика сохранена.
3. **`PrimaryTextButtonWidget` не поддерживает StringRes с args** — drill-in label `R.string.components_edit_show_all` использует `%1$d` placeholder для counter. Fallback на M3 `TextButton` + `stringResource(id, totalCount)` inline. Backlog: добавить overload `PrimaryTextButtonWidget(title: Int, vararg formatArgs: Any)`.
4. **`EditNameError` mapping host-local** — design tree упоминал «общая extension в shared module с двумя enum'ами», но `EditNameError` живёт в двух разных package'ах (mate Manager + mate PerDict, дублируется). Чтобы не плодить ещё одну общую mate-зависимость в widget module, mapping `EditNameError.toLabelRes()` сделан private extension в каждом screen. Strings те же что для `NameError` (Empty/Same/Cross).
5. **`lexemeLabel` resolve через `LocalContext.getString`** — `lexemeLabel: (Long) -> String` lambda вызывается внутри widget render, но не помечена @Composable. `stringResource()` требует @Composable scope → fallback на `context.getString(R.string.components_edit_lexeme_label, id)`. Backlog: реальный label через UseCase query `getLexemesByIds`; phase 2 — placeholder "Lexeme #N".
6. **`onShowAllImpacted` — no-op в обоих screen'ах** — drill-in destination (bottom-sheet / отдельный screen) — backlog (design tree open Q4). На phase 2 callback no-op + TODO comment.
7. **`HostVariant` enum + `DictionaryRef` + `DeletionImpactRef` declared в файлах самих диалогов** — а не отдельно. Один диалог = один файл с display-only DTO'шками. Альтернатива (отдельный shared types file) — backlog cleanup.
8. **`DictionaryApiEntity.name`** (не `.title` как было в design tree) — verified Read'ом из `core-db-api/.../DictionaryApiEntity.kt:8`. Маппинг исправлен в Manager screen.

## log_messages

- Read 4 артефакта (`ui_design_tree.md`, `ui_layout.md`, `ui_walkthrough.md`, `business_summary.md`) — собран план 32-узлового implement.
- Прочитал 16 файлов исходных widgets (обоих screen-модулей) + 2 screens + 2 mate State.kt + Msg.kt + UI primitives (LexemeDialog/LexemeRadioRow/LexemeTextFieldWidget/PrimaryFullButtonWidget/PrimaryLongFabWidget/CancelButtonWidget/IconBoxed/PrimaryTextButtonWidget) + domain (ComponentTemplate/ComponentType/ComponentValue/TemplateValues/Primitive/Scope/NameError/DeletionImpact) + Reducer.kt (verification F138 4-way mutual-exclusion).
- Added 14 .kt files в `:modules:widget:component_widgets` (3 templates + 7 widgets + 4 dialogs).
- Modified 2 screens (re-write полностью на shared module imports + EditDialog mount + plain-primitives расклад).
- Removed 16 obsolete .kt files + 2 empty `widget/` directories.
- Added 11 new strings (8 en + 8 ru — phase 2 edit/scope/preview labels).
- Sequential test runs (per memory rule «no parallel gradle calls»): component_widgets / components_manager / per_dictionary_components / app — все PASS.

_model: claude-opus-4-7[1m]_
