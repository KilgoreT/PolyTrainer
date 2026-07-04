---
status: done
---

# Summary — ui

UI sub-flow phase 2 IS481 component_constructor пройден полностью: walkthrough → layout → design_tree (32 узла) → implement (build + tests PASS) → publish_ui (UI Layout раздел в spec обновлён).

## Что сделано

### Артефакты sub-flow (5 .md документов)

- `ui_walkthrough.md` — собрана карта существующих 8 widget'ов дубликатов в Manager + PerDict + наличие пустого `:modules:widget:component_widgets` (gradle wiring готов, source-пакет пуст).
- `ui_layout.md` — UI cheat-sheet с картой двух экранов + EditDialog subscreen + § Анализ виджетов (5 NEW + 7 CHANGED + 4 unchanged baseline) + § Удаляем (16 миграций) + § Новые UX-сценарии.
- `ui_design_tree.md` — DAG из 32 узлов в 5 Tier'ов (templates → widgets → dialogs → screen mounts → deletions) с открытыми вопросами по coupling/lexemeLabel/drill-in.
- `ui_implement.md` — реализация всех 32 узлов, перечисление нетривиальных решений, лог тестов.
- `publish_ui.md` — публикация финального snapshot UI Layout в `docs/features-spec/component-constructor.md` + список 10 implement-корректировок.

### Shared widget module: 14 NEW source files

`modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/`:

- **templates/** (3): `TextWidget.kt`, `ComponentBlock.kt`, `ComponentByTemplate.kt` — фундамент per-template architecture (concept typed_views.md Tier 2).
- **widgets/** (7): `ComponentTemplateLabel.kt` + `NameErrorLabel.kt` (extension funcs), `ComponentsEmptyStateWidget.kt`, `CreateComponentFab.kt`, `UserDefinedRowWidget.kt`, `PerDictRowWidget.kt`, `CardinalityDowngradePreviewWidget.kt`.
- **dialogs/** (4): `RenameComponentDialog.kt`, `DeleteComponentConfirmDialog.kt`, `CreateComponentDialog.kt` (расширен phase 2 multi-dict picker через `HostVariant.Manager`), `EditComponentDialog.kt` (NEW phase 2).

### Screen mounts (2 modified)

- `ComponentsManagerScreen.kt` — все 8 widget imports переключены на shared module; добавлен mount `EditComponentDialog` (visible iff `state.editDialog != null`); `CreateComponentDialog` получает `availableDictionaries`/`selectedDictionaryIds`/`hostVariant=Manager` + новые callbacks (`onScopeChange`, `onDictionaryToggle`); `UserDefinedRowWidget` теперь `onEdit→Msg.OpenEditDialog`; добавлены private extensions `EditNameError.toLabelRes()`; маппинг mate state → плоские примитивы на mount-site.
- `PerDictionaryComponentsScreen.kt` — зеркально (БЕЗ scope picker; `hostVariant=PerDict` + emptyList/emptySet + no-op callbacks).

### 16 deletions

8 файлов из `modules/screen/components_manager/.../widget/` + 8 файлов из `modules/screen/per_dictionary_components/.../widget/` (CreateComponentDialog, RenameComponentDialog, DeleteComponentConfirmDialog, UserDefinedRowWidget/PerDictRowWidget, ComponentsEmptyStateWidget, CreateComponentFab, ComponentTemplateLabel, NameErrorLabel). Пустые `widget/` директории удалены через `rmdir`.

### 11 новых strings (по 8 ключей в en + ru)

`core/core-resources/src/main/res/values{,-ru-rRU}/strings.xml`:

- Edit dialog: `components_edit_dialog_title`, `components_edit_field_name`, `components_edit_field_template`, `components_edit_field_is_multi`, `components_edit_cardinality_blocked_title`, `components_edit_show_all` ("Show all (%1$d)"), `components_edit_lexeme_label` ("Lexeme #%1$d" — placeholder).
- Scope picker: `components_create_field_scope`, `components_create_scope_global`, `components_create_scope_per_dict`.

### Build + tests

`./scripts/cc-build.sh assembleDebug` — PASS. Sequential `testDebugUnitTest`: component_widgets / components_manager / per_dictionary_components / app — все PASS. 75 TDD-тестов от business sub-flow остались зелёными.

## Ключевые решения

1. **Плоские примитивы для shared widget API** — вместо принятия mate state-объектов (`UserDefinedRow`, `PerDictRow`, `CreateDialogState`, `RenameDialogState`, `EditDialogState`) все Tier 1/2 widgets принимают плоские параметры (Dependency Rule: shared widget не должен знать о screen-mate). Hosts раскладывают state на mount-site. Design tree предлагал 3 варианта (coupling / lift-types / flatten); выбран flatten.
2. **`HostVariant` enum для CreateDialog** — единый `CreateComponentDialog` управляет видимостью scope picker через `HostVariant.Manager | PerDict`. PerDict передаёт `emptyList` + no-op callbacks; scope hardcoded в Reducer на уровне `Scope.PerDictionaries(listOf(dictId))`. Альтернатива (два разных composable) отвергнута — render-цепочка идентична.
3. **`DeletionImpactRef` + `DictionaryRef` display-only DTO** declared inline в файлах самих диалогов (один dialog = один файл с DTO). Избегаем dep `:core:core-db-api` в shared widget module.
4. **`lexemeLabel` placeholder TODO** — `lexemeLabel: (Long) -> String` callback в `CardinalityDowngradePreviewWidget` резолвится через `context.getString(R.string.components_edit_lexeme_label, id)` = "Lexeme #N". `stringResource()` требует @Composable scope → fallback на `LocalContext.current.getString()`. Backlog: реальный label через UseCase query `getLexemesByIds`.
5. **`onShowAllImpacted` no-op TODO** — drill-in destination для «Показать все N» (bottom-sheet либо отдельный screen) — backlog. Callback no-op + TODO comment.
6. **Fallback на `LexemeStyle.BodyS/BodyLBold`** — `LabelM/LabelL` отсутствуют в `LexemeStyle` (есть только H1-H6 + BodyXL/L/M/S). `ComponentBlock` name_slot использует `BodyS+onSurfaceVariant`, title preview — `BodyLBold`.
7. **`PrimaryTextButtonWidget` без vararg formatArgs** → drill-in label с `%1$d` counter использует M3 `TextButton` + `stringResource(id, totalCount)` inline. Backlog: добавить overload.
8. **`EditNameError.toLabelRes()` host-local** — `EditNameError` живёт в двух разных mate-пакетах (Manager + PerDict). Чтобы не плодить ещё одну общую зависимость, mapping сделан private extension в каждом screen (re-use тех же `R.string.components_create_name_*` strings).
9. **`ImpactedLexemesPreview` sealed → 3 плоских параметра** на widget-уровне (`inlineIds: List<Long>`, `totalCount: Int`, `showAllVisible: Boolean`); reducer/host маппит sealed на mount-site.
10. **`DictionaryApiEntity.name`** (не `.title`) verified Read'ом — исправлено в Manager screen mapping.

## Backlog для будущих итераций

- **Реальный `lexemeLabel` resolve** — UseCase query `getLexemesByIds(ids: List<Long>): List<LexemeRef>` + замена host-local placeholder `context.getString(...)` на реальные имена лексем в `CardinalityDowngradePreviewWidget`.
- **Drill-in destination для «Показать все N»** — bottom-sheet либо отдельный screen `ImpactedLexemesScreen`. `Msg.OpenImpactedLexemes` + navigation contract. Сейчас callback no-op.
- **`LexemeStyle.LabelM` / `LabelL`** в theme — отсутствуют в `:modules:core:theme.LexemeStyle`. Сейчас fallback на `BodyS` (label) + `BodyLBold` (label-large). Добавить эти стили для точного соответствия Material spec.
- **`PrimaryTextButtonWidget(title: Int, vararg formatArgs: Any)` overload** — текущий вариант не поддерживает StringRes с args. После добавления overload — заменить inline M3 `TextButton` обратно на `PrimaryTextButtonWidget` в `CardinalityDowngradePreviewWidget`.
- **Унификация `UserDefinedRowWidget` + `PerDictRowWidget` в единый `ComponentRowWidget(row: ComponentRowState)`** где `ComponentRowState` sealed (`Aggregated` + `PerDict`). Сейчас два варианта из-за различий в title-row (global chip placement) и usage_text (aggregated vs per_dict_value_count).
- **`Rename`-only кнопка как отдельный UX-affordance** — `Msg.OpenRenameDialog` остался в коде, но row callback теперь триггерит `OpenEditDialog`. Можно вернуть отдельный `ic_rename` icon-button (двойной trailing slot) либо удалить RenameDialog полностью если Edit-dialog покрывает name-only edit case.
- **`HostVariant` / `DictionaryRef` / `DeletionImpactRef`** declared inline в dialog'ах — вынести в отдельный `shared/types.kt` файл widget module для cohesion.

## Артефакты

### Документы sub-flow (5)

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor_phase2/ui_walkthrough.md`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor_phase2/ui_layout.md`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor_phase2/ui_design_tree.md`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor_phase2/ui_implement.md`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor_phase2/publish_ui.md`

### NEW source files (14) в `modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/`

- `templates/TextWidget.kt`
- `templates/ComponentBlock.kt`
- `templates/ComponentByTemplate.kt`
- `widgets/ComponentTemplateLabel.kt`
- `widgets/NameErrorLabel.kt`
- `widgets/ComponentsEmptyStateWidget.kt`
- `widgets/CreateComponentFab.kt`
- `widgets/UserDefinedRowWidget.kt`
- `widgets/PerDictRowWidget.kt`
- `widgets/CardinalityDowngradePreviewWidget.kt`
- `dialogs/RenameComponentDialog.kt`
- `dialogs/DeleteComponentConfirmDialog.kt`
- `dialogs/CreateComponentDialog.kt`
- `dialogs/EditComponentDialog.kt`

### Deletions (16)

`modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/`:
- `CreateComponentDialog.kt`, `RenameComponentDialog.kt`, `DeleteComponentConfirmDialog.kt`, `UserDefinedRowWidget.kt`, `ComponentsEmptyStateWidget.kt`, `CreateComponentFab.kt`, `ComponentTemplateLabel.kt`, `NameErrorLabel.kt`

`modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/widget/`:
- `CreateComponentDialog.kt`, `RenameComponentDialog.kt`, `DeleteComponentConfirmDialog.kt`, `PerDictRowWidget.kt`, `ComponentsEmptyStateWidget.kt`, `CreateComponentFab.kt`, `ComponentTemplateLabel.kt`, `NameErrorLabel.kt`

### Modified files (4)

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerScreen.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsScreen.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/values/strings.xml`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/values-ru-rRU/strings.xml`

### Spec

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features-spec/component-constructor.md` — `## UI Layout` раздел заменён финальным snapshot phase 2 (~430 строк) + подраздел «Корректировки от implement» (10 пунктов).

_model: claude-opus-4-7[1m]_
