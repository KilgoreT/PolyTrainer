# UI walkthrough: IS481 component_constructor phase 2

Факты собраны Read'ом; ссылки `file:line` указывают на конкретные точки в исходниках.

## 1. Existing CreateComponentDialog (manager + per_dictionary, duplicates)

### Manager variant
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/CreateComponentDialog.kt:36-131`.
- Signature: `internal fun CreateComponentDialog(createDialog: CreateDialogState, isSubmitting: Boolean, onNameChange: (String) -> Unit, onTemplateSelect: (ComponentTemplate) -> Unit, onMultiToggle: (Boolean) -> Unit, onSubmit: () -> Unit, onDismiss: () -> Unit)`.
- Принимает целиком `CreateDialogState` (`mate.CreateDialogState`, `modules/.../components_manager/mate/State.kt:73-85`) — это нарушает «примитивы + callbacks» pattern (виджет coupled на screen-specific state shape).
- `canSubmit` вычислен ВНУТРИ composable (`CreateComponentDialog.kt:46-48`): `name.isNotBlank() && nameError == null && !isSubmitting`. В phase 2 эта проверка перенесена в state как `CreateDialogState.canSubmit` extension (`State.kt:91-95`).
- Slots: `Text` title (`R.string.components_create_dialog_title`) → name section (`Text` label + `LexemeTextFieldWidget` + error `Text`) → template radio-group (`ComponentTemplate.entries.forEach { LexemeRadioRow(...) }`) → multi `Row` (`Checkbox` + `Text`) → actions `Row` (`CancelButtonWidget` + `PrimaryFullButtonWidget`).
- Контейнер: `LexemeDialog` (см. § 4).
- `nameError` маппится через `NameError.labelRes()` (`widget/NameErrorLabel.kt:12-16`).
- `ComponentTemplate.labelRes()` (`widget/ComponentTemplateLabel.kt:13-16`).
- KDoc уже фиксирует: «MVP scope=Global only (F158). UI не показывает scope-control… PerDictionaries multi-dict picker — Backlog phase 2.» — scope picker отсутствует.

### Per-dict variant
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/CreateComponentDialog.kt:33-124`.
- Signature **идентична** Manager-варианту (тот же набор параметров, ровно те же callbacks).
- Внутренности **по-байтно дублируют** Manager-вариант, кроме импорта `CreateDialogState` (свой пакет `me.apomazkin.per_dictionary_components.mate.CreateDialogState`, `per_dictionary_components/mate/State.kt:73-84`) — у per-dict нет `selectedDictionaryIds` поля.
- KDoc: «Scope hardcoded в Reducer как `Scope.PerDictionaries(listOf(state.dictionaryId))` — UI не показывает scope-control (F158)».

### Дубликация
Двое одинаковых composable'ов разъезжаются только пакетом + типом аргумента; вся render-цепочка идентична. Это прямой кандидат на вынос в shared module (см. § 5).

## 2. Existing other dialogs/widgets (duplicates pair)

### RenameComponentDialog
- Manager: `modules/screen/components_manager/.../widget/RenameComponentDialog.kt:27-98`.
- Per-dict: `modules/screen/per_dictionary_components/.../widget/RenameComponentDialog.kt:28-97`.
- Signature: `(renameDialog: RenameDialogState, isSubmitting: Boolean, onNameChange: (String) -> Unit, onSubmit: () -> Unit, onDismiss: () -> Unit)`.
- Тот же anti-pattern — принимает целый `RenameDialogState`. `canSubmit` внутри composable (`editedName.isNotBlank() && editedName != originalName && nameError == null && !isSubmitting`).
- Internals: title `Text` → original-name `Text`-pair → new-name `LexemeTextFieldWidget` + error → action `Row`. Идентичны между двумя модулями (кроме импорта `RenameDialogState`).

### DeleteComponentConfirmDialog
- Manager: `modules/screen/components_manager/.../widget/DeleteComponentConfirmDialog.kt:29-126`.
- Per-dict: `modules/screen/per_dictionary_components/.../widget/DeleteComponentConfirmDialog.kt:28-123`.
- Signature: `(deleteConfirm: DeleteConfirmState, isSubmitting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit)`.
- KDoc Manager: «Не использует `AlarmDialogWidget` напрямую — нужен dynamic impact content внутри dialog'а». Использует базовый `LexemeDialog` + ручную сборку actions Row с `CancelButtonWidget` + `AlarmButtonWidget`.
- Impact block: 3-ветвый `when` (loading / impact!=null с 4 опциональными `Text` строками — values/dicts/quiz/prefs / `else` unavailable). Полностью повторяется в обоих модулях.

### UserDefinedRowWidget vs PerDictRowWidget
- Manager: `modules/screen/components_manager/.../widget/UserDefinedRowWidget.kt:34-135` — params: `(row: UserDefinedRow, onEdit: (ComponentTypeId) -> Unit, onDelete: (ComponentTypeId) -> Unit)`.
- Per-dict: `modules/screen/per_dictionary_components/.../widget/PerDictRowWidget.kt:34-140` — params: `(row: PerDictRow, onEdit: (ComponentTypeId) -> Unit, onDelete: (ComponentTypeId) -> Unit)`.
- Структура: `Surface(RoundedCornerShape(12.dp))` → `Row(padding 16/12)` с слотами:
  1. leading `IconBoxed(ic_components, size=24)`.
  2. weight(1f) `Column` { name `Text(BodyL)` + meta `Row` { 2-3 `AssistChip` (template/cardinality/optional global) } + usage `Text(BodyS gray)` }.
  3. trailing `IconBoxed(ic_edit, size=44, onClick=onEdit)` + `IconBoxed(ic_trash, size=44, onClick=onDelete)`.
- Различия:
  - Manager: global chip — третий в meta row; usage line «`${usageCount} · ${dictText}`».
  - Per-dict: global chip — в **title row** (рядом с `name`, а не в chip-list); usage line — `R.string.per_dict_row_value_count` formatted с `valueCount`.
- `row.template.labelRes()` и chip-text для multi/single — дублируется один-в-один.

### ComponentsEmptyStateWidget
- Manager: `modules/screen/components_manager/.../widget/ComponentsEmptyStateWidget.kt:30-63`.
- Per-dict: `modules/screen/per_dictionary_components/.../widget/ComponentsEmptyStateWidget.kt:29-62`.
- Signature: `(headlineRes: Int, bodyRes: Int, onCreate: () -> Unit, ctaRes: Int = R.string.components_empty_cta)`.
- **Уже spec'нут как «общий для обоих экранов» в KDoc** (Manager file:25), но фактически дублируется кодом.
- Структура: `Column(fillMaxWidth, padding 32, spacedBy 16, alignCenter)` → `Icon(ic_components, 64dp)` → headline `Text(H6)` → body `Text(BodyL gray)` → `PrimaryFullButtonWidget(titleRes=ctaRes)`.
- В Manager-варианте есть `@PreviewWidget Preview()`; в per-dict — нет.

### CreateComponentFab
- Manager: `modules/screen/components_manager/.../widget/CreateComponentFab.kt:15-24`.
- Per-dict: `modules/screen/per_dictionary_components/.../widget/CreateComponentFab.kt:12-21`.
- Signature: `(onClick: () -> Unit)`. Тонкая обёртка над `PrimaryLongFabWidget(ic_add, R.string.components_create_cta, enabled=true, onClick)`.
- KDoc оба говорят «общий для обоих экранов» — фактически дублируется.

### ComponentTemplateLabel (extension)
- Manager: `widget/ComponentTemplateLabel.kt:13-16`.
- Per-dict: `widget/ComponentTemplateLabel.kt:12-15`.
- `internal fun ComponentTemplate.labelRes(): Int = when (this) { TEXT -> R.string.components_template_text; IMAGE -> R.string.components_template_image }`. Идентично.

### NameErrorLabel (extension)
- Manager: `widget/NameErrorLabel.kt:12-16`.
- Per-dict: `widget/NameErrorLabel.kt:12-16`.
- `internal fun NameError.labelRes(): Int` маппит 3 ветки (`Empty / SameScopeCollision / CrossScopeCollision`). Идентично.

## 3. `:modules:widget:component_widgets` module — текущее состояние

- `modules/widget/component_widgets/build.gradle.kts` (35 lines).
- Plugins: `com.android.library`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose`.
- `namespace = "me.apomazkin.component_widgets"`, `compileSdk=35`, `minSdk=23`, `targetSdk=35`, JVM 17.
- Dependencies: `:modules:core:theme`, `:modules:core:ui`, `:modules:domain:lexeme`, `:core:core-resources` + `composeLibs.lifecycleViewmodelCompose` + `composeLibs.lifecycleRuntimeCompose`.
- **НЕТ зависимостей на `:core:core-db-api`** — значит вынос диалогов через примитивы + callbacks не пропустит `DeletionImpact`/`Scope`/`DictionaryApiEntity`/`UserDefinedRow`/etc, если не дополнить deps **или** не пробрасывать плоские примитивы (Int counts, String name, List<Long>, etc.) — это второе соответствует Open Q3 best-guess.
- Source dir: `modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/` существует, **пустой** (нет ни одного `.kt` файла).
- Регистрация:
  - `settings.gradle.kts:59` — `include(":modules:widget:component_widgets")`.
  - `app/build.gradle.kts:132` — `implementation(project("path" to ":modules:widget:component_widgets"))`.
  - `modules/screen/components_manager/build.gradle.kts:37` — implementation на `:modules:widget:component_widgets` уже прописано.
  - `modules/screen/per_dictionary_components/build.gradle.kts:37` — то же самое.
- Вывод: gradle-обвязка и dependency wiring готовы; «наполнение» сводится к добавлению `.kt` файлов в пустой пакет.

## 4. UI primitives в `:modules:core:ui` (релевантные phase 2)

### Dialog containers
- `modules/core/ui/src/main/java/me/apomazkin/ui/dialog/base/LexemeDialog.kt:25-54` — базовый `LexemeDialog(onDismissRequest, dismissOnBackPress=true, dismissOnClickOutside=true, usePlatformDefaultWidth=true, decorFitsSystemWindows=false, content: @Composable ColumnScope.() -> Unit)`. Surface + RoundedCornerShape(16) + padding 24, content слот.
- `modules/core/ui/src/main/java/me/apomazkin/ui/dialog/AlarmDialogWidget.kt:27-54` — обёртка над `LexemeDialog` для конструкции «content + Cancel + Alarm». Phase 2 Delete-диалог намеренно НЕ использует его (нужен dynamic impact block).

### Radio
- `modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeRadioRow.kt:32-56` — `LexemeRadioRow(textRes: Int, selected: Boolean, onClick, modifier)`. Full-row click через `Modifier.selectable(role=RadioButton)`. KDoc явно помечен `IS481 (F162): primitive`.
- `modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeRadioMenuItem.kt`, `LexemeSubmenuMenuItem.kt` — для DropdownMenu (не релевантно диалогам).

### Buttons (используются в существующих диалогах)
- `modules/core/ui/src/main/java/me/apomazkin/ui/btn/`:
  - `CancelButtonWidget.kt`, `PrimaryFullButtonWidget.kt`, `AlarmButtonWidget.kt`, `PrimaryLongFabWidget.kt`, `SecondaryButtonWidget.kt`, `PrimaryFabWidget.kt`, `SecondaryFabWidget.kt`, `ErrorButtonWidget.kt`, `PrimaryTextButtonWidget.kt`.

### Inputs
- `modules/core/ui/src/main/java/me/apomazkin/ui/input/PrimaryTextFieldWidget.kt`.
- `modules/core/ui/src/main/java/me/apomazkin/ui/input/base/LexemeTextFieldWidget.kt` — used в обоих Create/Rename диалогах.

### Chips primitive
- **В `:modules:core:ui` chip-primitive отсутствует.** Использование Material3 chips (`AssistChip`, `InputChip`, `SuggestionChip`, `FilterChip`) — inline в widget callsites:
  - `UserDefinedRowWidget.kt:70-107` — `AssistChip(enabled=false)` для read-only badges.
  - `PerDictRowWidget.kt:71-112` — то же.
- `modules/widget/chipPicker/src/main/java/me/apomazkin/chippicker/ChipPickerWidget.kt:50` — `ChipPickerWidget<T : ChipValue>(title, pickerValue: ChipPicker, chipList: List<T>, onChipSelect, onResetChip, editable, ...)`. **Single-select** (`ChipPicker.Off` или `ChipPicker.Selected(value)`), использует `SuggestionChip` + `InputChip`. Для multi-dict picker phase 2 (multi-select chip-list) **не подходит** без изменения API — нужен новый primitive **или** inline-сборка через `LazyRow` + `FilterChip` в новом dialog'е.

### Прочее
- `modules/core/ui/src/main/java/me/apomazkin/ui/IconBoxed.kt` — используется в Row widgets.
- `modules/core/ui/src/main/java/me/apomazkin/ui/preview/PreviewAnnotations.kt:15` — `@PreviewWidget` (двуязычный RU/En preview). Для phase 2 preview'ев новых widgets потребуется `compose-tooling` dep в `component_widgets/build.gradle.kts` (сейчас не подключен).
- `modules/core/ui/src/main/java/me/apomazkin/ui/ErrorStateWidget.kt` — используется обоими экранами для F163 error state.

### LazyColumn для preview lists
- В `:modules:core:ui` отдельного preview-list primitive **нет**. Оба экрана используют Material3 `LazyColumn` напрямую (`ComponentsManagerScreen.kt:126-130`, `PerDictionaryComponentsScreen.kt:133-137`).

## 5. Screen-уровень mount-инг диалогов

### ComponentsManagerScreen
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerScreen.kt:57-173`.
- Структура body: `Scaffold(topBar=TopAppBar, snackbarHost=SnackbarHost, floatingActionButton=CreateComponentFab)` → `Box` с 4-ветвым `when`:
  - `state.isLoading && rows == null` → `CircularProgressIndicator` (centered).
  - `!isLoading && rows == null` → `ErrorStateWidget` (F163 retry).
  - `state.isEmpty` → `ComponentsEmptyStateWidget`.
  - `rows != null` → `LazyColumn(contentPadding 16/8, spacedBy 8) { items(key={it.typeId.id}) { UserDefinedRowWidget(...) } }`.
- Dialog mounting — по nullable flag в state, после Scaffold (lines 145-172):
  - `state.createDialog?.let { CreateComponentDialog(dialog, state.isCreating, onNameChange=Msg.CreateNameChange, onTemplateSelect=Msg.CreateTemplateChange, onMultiToggle=Msg.CreateMultiToggle, onSubmit=Msg.SubmitCreate, onDismiss=Msg.CloseCreateDialog) }`.
  - `state.renameDialog?.let { RenameComponentDialog(...) }`.
  - `state.deleteConfirm?.let { DeleteComponentConfirmDialog(...) }`.
- **EditDialog НЕ примонтирован** — `state.editDialog` поле уже добавлено в state (`State.kt:47`), но screen его пока не рендерит. Phase 2 должен добавить четвёртый `state.editDialog?.let { EditComponentDialog(...) }` блок.
- **CreateComponentDialog не имеет scope picker** в UI — в phase 2 нужно расширить (Manager-only).
- Snackbar dispatch: `LaunchedEffect(snackbar)` → `snackbarHostState.showSnackbar(text)` → `Msg.DismissSnackbar` (`ComponentsManagerScreen.kt:70-75`).
- Передача state в widgets: целиком `dialog` объект (state-coupling), плюс explicit `isSubmitting=state.isCreating/isRenaming/isDeleting` отдельно. После phase 2 vынос в shared нужно либо разорвать coupling (плоские примитивы), либо передавать целый `DialogState` через `domain`-level data class.

### PerDictionaryComponentsScreen
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsScreen.kt:58-179`.
- Структура **зеркальна** Manager-варианту за исключением:
  - assisted-инжект `dictionaryId: Long` (`viewModelFactory { factory.create(dictionaryId, navigator) }`).
  - `TopAppBar.title = state.dictionaryName ?: per_dict_components_title`.
  - `rows = state.items` (`PerDictRow`), отрисовка через `PerDictRowWidget`.
  - `ComponentsEmptyStateWidget(headlineRes=components_empty_headline_per_dict, bodyRes=components_empty_body_per_dict)`.
- Те же 3 mount'а диалогов (Create/Rename/DeleteConfirm). EditDialog тоже **не примонтирован**.
- `state.editDialog` поле уже добавлено в state per-dict (`per_dictionary_components/mate/State.kt:52`). Phase 2 — добавить mount.

### Msg структура (Manager — для контекста чему виджет шлёт callbacks)
- `modules/screen/components_manager/.../mate/Msg.kt:20-102` — sealed Msg, уже содержит phase 2 семейство:
  - `OpenEditDialog(typeId)`, `CloseEditDialog`, `EditNameChange(name)`, `EditTemplateChange(template)`, `EditMultiToggle(isMulti)`, `SubmitEdit`, `EditResult(epochId, outcome)` (lines 48-55).
  - `CreateDictionaryToggle(dictionaryId)`, `DictionariesLoaded(dictionaries)` (lines 58-67).
- Значит UI-binding для EditDialog нужно только подключить в `ComponentsManagerScreen.kt` (передать ссылки на эти Msg ctors). Сами Msg уже есть.

## Сводная карта дубликатов phase 2 для extract → shared

| Имя | Manager file:line | Per-dict file:line | Идентичны? |
|---|---|---|---|
| CreateComponentDialog | `widget/CreateComponentDialog.kt:36-131` | `widget/CreateComponentDialog.kt:33-124` | yes (только импорт state) |
| RenameComponentDialog | `widget/RenameComponentDialog.kt:27-98` | `widget/RenameComponentDialog.kt:28-97` | yes |
| DeleteComponentConfirmDialog | `widget/DeleteComponentConfirmDialog.kt:29-126` | `widget/DeleteComponentConfirmDialog.kt:28-123` | yes |
| ComponentsEmptyStateWidget | `widget/ComponentsEmptyStateWidget.kt:30-63` (+Preview) | `widget/ComponentsEmptyStateWidget.kt:29-62` | yes (без Preview) |
| CreateComponentFab | `widget/CreateComponentFab.kt:15-24` (+Preview) | `widget/CreateComponentFab.kt:12-21` | yes |
| ComponentTemplateLabel | `widget/ComponentTemplateLabel.kt:13-16` | `widget/ComponentTemplateLabel.kt:12-15` | yes |
| NameErrorLabel | `widget/NameErrorLabel.kt:12-16` | `widget/NameErrorLabel.kt:12-16` | yes |
| UserDefinedRowWidget vs PerDictRowWidget | `widget/UserDefinedRowWidget.kt:34-135` | `widget/PerDictRowWidget.kt:34-140` | similar — обвязка одинакова, разные `row` типы; нужен либо параметризованный `ComponentRowWidget` либо 2 варианта |
| EditComponentDialog | **отсутствует** | **отсутствует** | NEW phase 2 |
| TextWidget / ComponentBlock / ComponentByTemplate | **отсутствует** | **отсутствует** | NEW phase 2 |

8 dialog/widget точек извлечения + 3 NEW per-template composable'а + 1 NEW EditDialog. Целевая dest: `modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/` (currently empty).

## Вердикт

Аналог: **не найден**

`EditComponentDialog`, multi-dict scope chip-list, cardinality downgrade preview UI, per-template (`TextWidget`/`ComponentBlock`/`ComponentByTemplate`) composables в кодбазе **отсутствуют полностью**. `:modules:widget:component_widgets` уже зарегистрирован в gradle / settings / оба screen-модуля, но source-package **пуст**. Существующие 8 widget'ов дублируются в `components_manager/widget/` и `per_dictionary_components/widget/` — это базис для extract'а в shared с переходом на «примитивы + callbacks» API (по Open Q3 best-guess). `:modules:core:ui` поставляет dialog/radio/button/textfield primitives, но НЕ предоставляет chip-list primitive (нужен либо новый primitive в core/ui, либо inline `LazyRow` + `FilterChip` внутри будущего `CreateComponentDialog` shared-варианта). `chipPicker` widget — single-select, для multi-dict не подходит без рефакторинга.

## log_messages

- read scope.md (425 lines): 5 областей UI phase 2 идентифицированы.
- collected file:line refs для 16 widget/screen/primitive точек; зафиксированы 8 дубликатов и 3+1 NEW.
- `:modules:widget:component_widgets` source package пуст; deps wired; ChipPicker single-select не подходит для multi-dict picker.

_model: claude-opus-4-7[1m]_
