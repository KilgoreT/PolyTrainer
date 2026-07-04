# UI Layout: IS481 component_constructor phase 2 — конструктор пользовательских компонентов · UI cheat-sheet

## Источники

- **code (existing widgets)** — `modules/screen/components_manager/.../widget/*.kt` (8 файлов) + `modules/screen/per_dictionary_components/.../widget/*.kt` (8 файлов). Phase 2 baseline зафиксирован в `ui_walkthrough.md`.
- **concept docs** — `docs/features/IS481_component_constructor/concept/typed_views.md` (per-template architecture + Tier 2 widget module), `docs/features/IS481_component_constructor/concept/ui_placement.md` (multi-dict scope picker, downgrade preview UI).
- **business_summary.md § UI dependencies handed off to ui sub-flow** — декларация 10 пунктов (EditComponentDialog, CardinalityDowngradePreviewWidget, UserDefinedRowWidget Edit-button, multi-dict picker chip-list, shared widget module, RenameDialog/DeleteDialog extract, per-template architecture).
- **Spec phase 1 UI Layout** — `docs/features-spec/component-constructor.md` § UI Layout (рабочий snapshot уже интегрировал phase 2 inline; настоящий документ — оркестрационный фокус для UI sub-flow design_tree + implement).
- **Figma:** not used (`feature_has_figma=false`, stub `figma_dump.json` — Case A).

## 📋 ЧТО ДЕЛАЕМ

Phase 2 закрывает 5 UI расхождений phase 1 vs concept одним flow:

- **EditDialog (NEW)** — отредактировать name / template (gated) / isMulti + cardinality downgrade preview inline; mount'ится на обоих экранах.
- **Multi-dict scope picker** — расширение `CreateComponentDialog` в Manager-варианте: radio Global / PerDictionaries + chip-list (M3 FilterChip) с multi-select по `availableDictionaries`.
- **`:modules:widget:component_widgets` наполнение** — extract 8 дублированных widgets из обоих screen-модулей в shared module через примитивы + callbacks; добавить новый EditDialog, CardinalityDowngradePreview, per-template resolver (`TextWidget` / `ComponentBlock` / `ComponentByTemplate`).
- **Cardinality downgrade preview** — inline top-3 lexeme entries + drill-in кнопка «Показать все» (если size > 3); рендерится внутри EditDialog при `EditOutcome.CardinalityDowngradeBlocked`.
- **Edit action button** — `IconBoxed(ic_edit)` на `UserDefinedRowWidget` / `PerDictRowWidget` теперь callback'ит `Msg.OpenEditDialog(typeId)` (вместо `OpenRenameDialog`).

## 🏷 ЛЕГЕНДА

- **⚙️** — системный Material3 / Compose (Scaffold, Column, FlowRow, FilterChip, AssistChip, LazyColumn, Checkbox).
- **❇️** — новый кастомный виджет (в этой фиче впервые).
- **🔄** — кастомный, меняется в этой фиче.
- **📌** — кастомный, не меняется в этой фиче (вынос в shared module **не** считается изменением рендера — структура та же, меняется только location/API; в этом документе такие отмечены 🔄 если callbacks/API меняется, 📌 если только переезд package).
- **🚨** — отход от Figma (в phase 2 нет Figma → отсутствует).
- **ℹ️** — обычная пояснительная заметка.

---

## 🗺 Карта экрана

### Экран 1 — `ComponentsManagerScreen` (drill-in из Settings)

```
⚙️ Scaffold
├─ ⚙️ TopAppBar                                  title=R.string.components_manager_title  navigation=back-arrow
├─ ↘️ snackbarHost                               state-driven, ∀ state.snackbarState != null
└─ ⚙️ Box (content)                              padding=paddings  fillMaxSize
   ├─ ∀ state.isLoading && state.userDefinedTypes == null:
   │  └─ ⚙️ CircularProgressIndicator            align=Center
   ├─ ∀ !state.isLoading && state.userDefinedTypes == null:
   │  └─ 📌 <𝗘𝗿𝗿𝗼𝗿𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>                  message=R.string.components_manager_load_failed  onRetry→Msg.OnRetryClick
   ├─ ∀ state.isEmpty:
   │  └─ 🔄 <𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗘𝗺𝗽𝘁𝘆𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>     extract → shared widget module
   ├─ ∀ state.userDefinedTypes != null && !state.isEmpty:
   │  └─ ⚙️ LazyColumn (× N)                     contentPadding=h:16 v:8  spacing=8
   │     └─ 🔄 <𝗨𝘀𝗲𝗿𝗗𝗲𝗳𝗶𝗻𝗲𝗱𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>            row=UserDefinedRow  onEdit→Msg.OpenEditDialog  onDelete→Msg.OpenDeleteConfirm
   ↘️ FAB-slot                                   pos=BottomEnd  padding=16
   └─ 🔄 <𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗙𝗮𝗯>                   onClick→Msg.OpenCreateDialog
   ↘️ Dialog-overlay slots                       ∀ соответствующий dialog != null
   ├─ 🔄 <𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>               ∀ createDialog != null  (Manager variant — с scope picker)
   ├─ 🔄 <𝗥𝗲𝗻𝗮𝗺𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>               ∀ renameDialog != null
   ├─ 🔄 <𝗗𝗲𝗹𝗲𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗶𝗮𝗹𝗼𝗴>      ∀ deleteConfirm != null
   └─ ❇️ <𝗘𝗱𝗶𝘁𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>                ∀ editDialog != null   (phase 2 NEW)
```

### Экран 2 — `PerDictionaryComponentsScreen` (drill-in по «молоток» из `DictionaryAppBar`)

```
⚙️ Scaffold
├─ ⚙️ TopAppBar                                  title=state.dictionaryName ?: R.string.per_dict_components_title  navigation=back-arrow
├─ ↘️ snackbarHost                               state-driven
└─ ⚙️ Box (content)                              padding=paddings  fillMaxSize
   ├─ ∀ state.isLoading && state.items == null:
   │  └─ ⚙️ CircularProgressIndicator            align=Center
   ├─ ∀ !state.isLoading && state.items == null:
   │  └─ 📌 <𝗘𝗿𝗿𝗼𝗿𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>                  message=R.string.components_per_dict_load_failed
   ├─ ∀ state.isEmpty:
   │  └─ 🔄 <𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗘𝗺𝗽𝘁𝘆𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>     variant=per-dict (headlineRes/bodyRes)
   ├─ ∀ state.items != null && !state.isEmpty:
   │  └─ ⚙️ LazyColumn (× N)                     contentPadding=h:16 v:8  spacing=8
   │     └─ 🔄 <𝗣𝗲𝗿𝗗𝗶𝗰𝘁𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>                row=PerDictRow  onEdit→Msg.OpenEditDialog  onDelete→Msg.OpenDeleteConfirm
   ↘️ FAB-slot                                   pos=BottomEnd  padding=16
   └─ 🔄 <𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗙𝗮𝗯>                   onClick→Msg.OpenCreateDialog
   ↘️ Dialog-overlay slots
   ├─ 🔄 <𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>               ∀ createDialog != null  (PerDict variant — БЕЗ scope picker)
   ├─ 🔄 <𝗥𝗲𝗻𝗮𝗺𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>               ∀ renameDialog != null
   ├─ 🔄 <𝗗𝗲𝗹𝗲𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗶𝗮𝗹𝗼𝗴>      ∀ deleteConfirm != null
   └─ ❇️ <𝗘𝗱𝗶𝘁𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>                ∀ editDialog != null   (phase 2 NEW)
```

### Subscreen — `EditComponentDialog` (NEW phase 2) — детализация поверх `LexemeDialog`

```
❇️ <𝗘𝗱𝗶𝘁𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>                       container=📌 <𝗟𝗲𝘅𝗲𝗺𝗲𝗗𝗶𝗮𝗹𝗼𝗴>
└─ ⚙️ Column                                     padding=24  spacing=16
   ├─ title_slot:   ⚙️ Text                      R.string.components_edit_dialog_title
   ├─ name_slot:    ⚙️ Column                    spacing=4
   │                  ├─ ⚙️ Text                 R.string.components_edit_field_name
   │                  ├─ 📌 <𝗟𝗲𝘅𝗲𝗺𝗲𝗧𝗲𝘅𝘁𝗙𝗶𝗲𝗹𝗱𝗪𝗶𝗱𝗴𝗲𝘁>  value=editDialog.name  onValueChange=onNameChange
   │                  └─ ∀ nameError != null:
   │                     └─ ⚙️ Text              source=editDialog.nameError.labelRes  color=error
   ├─ template_slot: ⚙️ Column                   spacing=4
   │                  ├─ ⚙️ Text                 R.string.components_edit_field_template
   │                  └─ ∀ ComponentTemplate.values():
   │                     └─ 📌 <𝗟𝗲𝘅𝗲𝗺𝗲𝗥𝗮𝗱𝗶𝗼𝗥𝗼𝘄>  selected=(t==editDialog.template)  onClick=onTemplateSelect(t)
   ├─ multi_slot:   ⚙️ Row                       spacing=8
   │                  ├─ ⚙️ Checkbox             checked=editDialog.isMulti  onCheckedChange=onMultiToggle
   │                  └─ ⚙️ Text                 R.string.components_edit_field_is_multi
   ├─ preview_slot: ∀ editDialog.impactedLexemesPreview != null:
   │                  └─ ❇️ <𝗖𝗮𝗿𝗱𝗶𝗻𝗮𝗹𝗶𝘁𝘆𝗗𝗼𝘄𝗻𝗴𝗿𝗮𝗱𝗲𝗣𝗿𝗲𝘃𝗶𝗲𝘄𝗪𝗶𝗱𝗴𝗲𝘁>  preview=editDialog.impactedLexemesPreview  onShowAll
   └─ actions_slot: ⚙️ Row                       spacing=12
                      ├─ 📌 <𝗖𝗮𝗻𝗰𝗲𝗹𝗕𝘂𝘁𝘁𝗼𝗻𝗪𝗶𝗱𝗴𝗲𝘁>  onClick=onDismiss
                      └─ 📌 <𝗣𝗿𝗶𝗺𝗮𝗿𝘆𝗙𝘂𝗹𝗹𝗕𝘂𝘁𝘁𝗼𝗻𝗪𝗶𝗱𝗴𝗲𝘁>  enabled=canSubmit  onClick=onSubmit
```

---

## 🔍 Анализ виджетов

### ❇️ `<𝗘𝗱𝗶𝘁𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>` (NEW, phase 2)

```
   • structure:
       column  padding=24  spacing=16  container=LexemeDialog
         title_slot:    text         source=R.string.components_edit_dialog_title  style=LexemeStyle.H6
         name_slot:
           column  spacing=4
             label:     text         source=R.string.components_edit_field_name  style=LexemeStyle.LabelM
             input:     input        variant=LexemeTextFieldWidget  value=editDialog.name
             error:     text         source=editDialog.nameError.labelRes  visible=∀ nameError != null  color=error
         template_slot:
           column  spacing=4
             label:     text         source=R.string.components_edit_field_template
             options:
               column  spacing=0
                 ∀ ComponentTemplate.entries:
                   option_row: button  variant=LexemeRadioRow  isSelected=(t==editDialog.template)  textRes=t.labelRes
         multi_slot:
           row  spacing=8
             checkbox:  button       variant=M3-Checkbox  checked=editDialog.isMulti
             label:     text         source=R.string.components_edit_field_is_multi
         preview_slot:
           ∀ editDialog.impactedLexemesPreview != null:
             content_slot: composite variant=CardinalityDowngradePreviewWidget  preview=editDialog.impactedLexemesPreview
         actions_slot:
           row  spacing=12
             cancel_btn: button      variant=CancelButtonWidget  onClick=onDismiss
             submit_btn: button      variant=PrimaryFullButtonWidget  enabled=canSubmit  onClick=onSubmit
   • type:        composite form-dialog поверх LexemeDialog (parity с CreateComponentDialog)
   • size:        width=hug (LexemeDialog usePlatformDefaultWidth=true) × height=hug
   • padding:     all=24 (наследует от LexemeDialog контента)
   • spacing:     itemSpacing=16
   • shape:       borderRadius=16 (от LexemeDialog Surface)
   • colors:
       – background: MaterialTheme.colorScheme.surface
       – content: MaterialTheme.colorScheme.onSurface
   • typography:
       – title: LexemeStyle.H6
       – label: LexemeStyle.LabelM
       – error: LexemeStyle.BodyS (color=error)
   • params:
       – editDialog: EditDialogState
       – isSubmitting: Boolean (= state.isEditing)
       – onNameChange: (String) -> Unit
       – onTemplateSelect: (ComponentTemplate) -> Unit
       – onMultiToggle: (Boolean) -> Unit
       – onShowAllImpacted: () -> Unit       (drill-in trigger; TBD Msg — backlog)
       – onSubmit: () -> Unit
       – onDismiss: () -> Unit
   • callbacks:
       – onNameChange → Msg.EditNameChange(value)
       – onTemplateSelect → Msg.EditTemplateChange(template)
       – onMultiToggle → Msg.EditMultiToggle(isMulti)
       – onShowAllImpacted → TBD: открыть bottom-sheet/drill-in screen со списком impacted lexemes; Msg ещё не зафиксирован
       – onSubmit → Msg.SubmitEdit
       – onDismiss → Msg.CloseEditDialog
   • behavior:
       canSubmit = name.trim().isNotBlank() && nameError == null && !isSubmitting
                  && (name != originalName || isMulti != originalIsMulti || template != originalTemplate).
       Template change на UI разрешён (radio row clickable) — immutability гасится UseCase'ом
       (EditOutcome.TemplateImmutable → snackbar + close).
       impactedLexemesPreview != null → preview_slot виден; диалог остаётся открытым (не закрывается на CardinalityDowngradeBlocked).
       isSubmitting → submit_btn disabled (защита от двойного тапа, mirrors Create/Rename pattern).
   • notes:
       ℹ️ Принимает onEdit из 🔄 UserDefinedRowWidget / 🔄 PerDictRowWidget (миграция IS481 phase 2).
       ℹ️ template_slot опционально может быть readonly per UX design — phase 2 решение оставлено как clickable radio (UI control видим) с UseCase-gate; альтернатива (readonly chips) — backlog.
   • source:      проектное решение — composite form-dialog mirrors phase 1 CreateComponentDialog с заменой fields на edit semantics.
```

### ❇️ `<𝗖𝗮𝗿𝗱𝗶𝗻𝗮𝗹𝗶𝘁𝘆𝗗𝗼𝘄𝗻𝗴𝗿𝗮𝗱𝗲𝗣𝗿𝗲𝘃𝗶𝗲𝘄𝗪𝗶𝗱𝗴𝗲𝘁>` (NEW, phase 2)

```
   • structure:
       column  padding=h:8 v:8  spacing=8  background=errorContainer  shape=rounded-12
         title_slot:  text  source=R.string.components_edit_cardinality_blocked_title  style=LexemeStyle.LabelL
         ∀ preview is ImpactedLexemesPreview.InlineOnly:
           ∀ id in preview.impactedLexemeIds:
             lexeme_row: text  source=lexemeLabel(id)  style=LexemeStyle.BodyM
         ∀ preview is ImpactedLexemesPreview.InlineWithDrillIn:
           ∀ id in preview.inlineIds:
             lexeme_row: text  source=lexemeLabel(id)  style=LexemeStyle.BodyM
           drill_in_btn: button  variant=PrimaryTextButtonWidget  label=R.string.components_edit_show_all  arg=preview.impactedLexemeIds.size
   • type:        Compose Column + conditional rendering
   • size:        width=fill × height=hug
   • padding:     horizontal=8, vertical=8
   • spacing:     itemSpacing=8
   • shape:       borderRadius=12
   • colors:
       – background: MaterialTheme.colorScheme.errorContainer
       – content: MaterialTheme.colorScheme.onErrorContainer
   • typography:
       – title: LexemeStyle.LabelL
       – lexeme_row: LexemeStyle.BodyM
   • params:
       – preview: ImpactedLexemesPreview      (sealed: InlineOnly | InlineWithDrillIn)
       – lexemeLabel: (Long) -> String         (host-supplied; resolves id → display label)
       – onShowAll: () -> Unit                 (visible iff preview is InlineWithDrillIn)
   • callbacks:
       – onShowAll → TBD: открыть bottom-sheet/drill-in со списком impacted lexemes; Msg ещё не зафиксирован
   • behavior:
       InlineOnly (size ≤ 3) → only inline rows; drill_in_btn скрыт.
       InlineWithDrillIn (size > 3) → top-3 inline (preview.inlineIds) + drill_in_btn видна с count = preview.impactedLexemeIds.size.
       Deterministic sort top-3 фиксирован на data-уровне (ORDER BY component_values.updated_at DESC, lexeme_id ASC LIMIT 3) — widget сортировку не делает.
   • notes:
       ℹ️ Размещается inline внутри EditComponentDialog preview_slot.
       ℹ️ `lexemeLabel` callback позволяет host'у (screen) резолвить id → label; widget не знает domain (Dependency Rule).
   • source:      проектное решение — UX requirement из ui_placement.md "inline top-3 + drill-in если больше".
```

### 🔄 `<𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>` (changed, phase 2 — extract + multi-dict picker)

```
   • structure:
       column  padding=24  spacing=16  container=LexemeDialog
         title_slot:    text     source=R.string.components_create_dialog_title
         name_slot:
           column  spacing=4
             label:     text     source=R.string.components_create_field_name
             input:     input    variant=LexemeTextFieldWidget  value=createDialog.name
             error:     text     source=createDialog.nameError.labelRes  visible=∀ nameError != null
         template_slot:
           column  spacing=4
             label:     text     source=R.string.components_create_field_template
             options:
               column  spacing=0
                 ∀ ComponentTemplate.entries:
                   option_row: button  variant=LexemeRadioRow  isSelected=(t==createDialog.template)
         multi_slot:
           row  spacing=8
             checkbox:  button   variant=M3-Checkbox  checked=createDialog.isMulti
             label:     text     source=R.string.components_create_field_is_multi
         scope_slot:                                                   (phase 2 NEW — только Manager variant)
           ∀ hostVariant == Manager:
             column  spacing=8
               label:     text   source=R.string.components_create_field_scope
               scope_radio_global:    button  variant=LexemeRadioRow  isSelected=(scope is Scope.Global)
               scope_radio_per_dict:  button  variant=LexemeRadioRow  isSelected=(scope is Scope.PerDictionaries)
               ∀ scope is Scope.PerDictionaries:
                 chip_group:
                   flow_row  spacing=8  wrap=true                      (M3 FlowRow для wrap)
                     ∀ dict in availableDictionaries:
                       chip: button  variant=FilterChip  selected=(dict.id in selectedDictionaryIds)  label=dict.name
         actions_slot:
           row  spacing=12
             cancel_btn: button   variant=CancelButtonWidget  onClick=onDismiss
             submit_btn: button   variant=PrimaryFullButtonWidget  enabled=canSubmit  onClick=onSubmit
   • type:        composite form-dialog поверх LexemeDialog
   • size:        width=hug × height=hug
   • padding:     all=24
   • spacing:     itemSpacing=16
   • shape:       borderRadius=16 (LexemeDialog Surface)
   • colors:
       – background: MaterialTheme.colorScheme.surface
       – chip-selected: MaterialTheme.colorScheme.secondaryContainer
   • typography:
       – title: LexemeStyle.H6
       – label: LexemeStyle.LabelM
       – chip: MaterialTheme.typography.labelMedium
   • params:                                                          (phase 2 API — plain primitives + callbacks; не принимает CreateDialogState)
       – name: String
       – template: ComponentTemplate
       – isMulti: Boolean
       – scope: Scope                                                  (Manager only; PerDict передаёт hardcoded Scope.PerDictionaries(listOf(dictId)) и не рендерит scope_slot)
       – nameError: NameError?
       – isSubmitting: Boolean
       – availableDictionaries: List<DictionaryRef>                    (Manager only; PerDict передаёт emptyList)
       – selectedDictionaryIds: Set<Long>                              (Manager only)
       – hostVariant: HostVariant                                      (enum Manager | PerDict — управляет видимостью scope_slot)
       – onNameChange / onTemplateSelect / onMultiToggle / onScopeChange / onDictionaryToggle / onSubmit / onDismiss
   • callbacks:
       – onScopeChange(Scope) → Msg.CreateScopeChange(scope)
       – onDictionaryToggle(Long) → Msg.CreateDictionaryToggle(id)
       – остальные callbacks — parity с phase 1
   • behavior:
       canSubmit = name.trim().isNotEmpty() && nameError == null && !isSubmitting
                  && (scope is Global || (scope is PerDictionaries && selectedDictionaryIds.isNotEmpty())).
       Submit disabled при пустом PerDictionaries selection (preventive UX).
       scope_slot отрендерен только в Manager variant; PerDict variant scope_slot пропускает (scope hardcoded в reducer).
       Chip-staleness обрабатывается в reducer (Msg.DictionariesLoaded фильтрует selectedDictionaryIds ∩ list.ids).
   • notes:
       ℹ️ Извлечён из обоих screen-модулей в `:modules:widget:component_widgets`. API сменился: вместо `CreateDialogState` принимает плоские примитивы (Dependency Rule: shared widget не coupled на screen-specific state shape).
       ℹ️ DictionaryRef — лёгкий display-only тип (id + name); domain `DictionaryApiEntity` маппится на host-уровне (screen → widget).
       ℹ️ M3 ChipPicker (existing `:modules:widget:chipPicker`) — single-select, не подходит; используем inline FlowRow + FilterChip.
   • source:      проектное решение — phase 1 + phase 2 multi-dict picker (concept ui_placement.md).
```

### 🔄 `<𝗥𝗲𝗻𝗮𝗺𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>` (changed, phase 2 — extract в shared module)

```
   • structure:
       column  padding=24  spacing=16  container=LexemeDialog
         title_slot:  text   source=R.string.components_rename_dialog_title
         original_slot:
           row  spacing=8
             label: text  source=R.string.components_rename_field_original
             value: text  source=originalName  style=LexemeStyle.BodyL
         input_slot:
           column  spacing=4
             label: text   source=R.string.components_rename_field_new
             input: input  variant=LexemeTextFieldWidget  value=editedName
             error: text   source=nameError.labelRes  visible=∀ nameError != null
         actions_slot:
           row  spacing=12
             cancel_btn: button  variant=CancelButtonWidget  onClick=onDismiss
             submit_btn: button  variant=PrimaryFullButtonWidget  enabled=canSubmit  onClick=onSubmit
   • type:        composite form-dialog поверх LexemeDialog
   • size:        width=hug × height=hug
   • padding:     all=24
   • spacing:     itemSpacing=16
   • params:
       – originalName: String
       – editedName: String
       – nameError: NameError?
       – isSubmitting: Boolean
       – onNameChange: (String) -> Unit
       – onSubmit: () -> Unit
       – onDismiss: () -> Unit
   • callbacks:
       – onNameChange → Msg.RenameTextChange(value)
       – onSubmit → Msg.SubmitRename
       – onDismiss → Msg.CloseRenameDialog
   • behavior:    canSubmit = editedName.isNotBlank() && editedName != originalName && nameError == null && !isSubmitting.
   • notes:
       ℹ️ Извлечён из обоих screen-модулей в `:modules:widget:component_widgets`. API сменился на плоские примитивы (без `RenameDialogState`).
   • source:      проектное решение — phase 1 рендер сохранён 1-в-1, переезд location + API rewrite.
```

### 🔄 `<𝗗𝗲𝗹𝗲𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗶𝗮𝗹𝗼𝗴>` (changed, phase 2 — extract в shared module)

```
   • structure:
       column  padding=24  spacing=16  container=LexemeDialog
         title_slot:  text  source=R.string.components_delete_dialog_title  arg=name
         impact_slot:
           ∀ isLoadingImpact:
             progress: ⚙️ CircularProgressIndicator  size=small
           ∀ impact != null:
             column  spacing=4
               line_values:   text  source=R.string.components_delete_impact_values  arg=impact.valueCount       visible=∀ impact.valueCount > 0
               line_dicts:    text  source=R.string.components_delete_impact_dicts   arg=impact.dictionariesWithValues.size  visible=∀ list non-empty
               line_quiz:     text  source=R.string.components_delete_impact_quiz    arg=impact.affectedQuizConfigs.size     visible=∀ list non-empty
               line_prefs:    text  source=R.string.components_delete_impact_prefs   arg=impact.affectedPrefs.size           visible=∀ list non-empty
           ∀ !isLoadingImpact && impact == null:
             text  source=R.string.components_delete_impact_unavailable
         actions_slot:
           row  spacing=12
             cancel_btn:  button  variant=CancelButtonWidget  onClick=onDismiss
             confirm_btn: button  variant=AlarmButtonWidget   enabled=!isSubmitting  onClick=onConfirm
   • type:        composite confirm-dialog поверх LexemeDialog (без AlarmDialogWidget — нужен dynamic impact block)
   • size:        width=hug × height=hug
   • padding:     all=24
   • spacing:     itemSpacing=16
   • params:
       – name: String
       – impact: DeletionImpactRef?            (lightweight display-only; counts only)
       – isLoadingImpact: Boolean
       – isSubmitting: Boolean
       – onConfirm: () -> Unit
       – onDismiss: () -> Unit
   • callbacks:
       – onConfirm → Msg.ConfirmDelete
       – onDismiss → Msg.CloseDeleteConfirm
   • behavior:    3-way conditional: loading | impact | unavailable. confirm_btn disabled when isSubmitting.
   • notes:
       ℹ️ Извлечён из обоих screen-модулей в shared module. `DeletionImpactRef` — display-only DTO с counts (избегаем impоrt full `DeletionImpact` domain в shared widget — Dependency Rule).
   • source:      проектное решение — phase 1 рендер сохранён, переезд location + API rewrite.
```

### 🔄 `<𝗨𝘀𝗲𝗿𝗗𝗲𝗳𝗶𝗻𝗲𝗱𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>` (changed, phase 2 — Edit-action callback + extract)

```
   • structure:
       row  spacing=12  padding=h:16 v:12  container=Surface  shape=rounded-12
         leading_slot: icon  variant=IconBoxed  iconRes=ic_components  size=24
         content_slot:
           column  spacing=4  weight=1
             title_slot: text  source=row.name  style=LexemeStyle.BodyL
             meta_slot:
               row  spacing=8
                 template_chip:    chip  variant=AssistChip  label=row.template.labelRes
                 cardinality_chip: chip  variant=AssistChip  label=R.string.components_chip_multi|single
                 global_chip:      chip  variant=AssistChip  label=R.string.components_chip_global  visible=∀ row.isGlobal
             usage_text: text  source=R.string.components_row_usage_aggregated  args=(usageCount, dictNames)  style=LexemeStyle.BodyS  color=gray
         trailing_slot:   icon  variant=IconBoxed  iconRes=ic_edit   size=44  onClick=onEdit
         trailing_slot_2: icon  variant=IconBoxed  iconRes=ic_trash  size=44  onClick=onDelete
   • type:        Surface + Row composite
   • size:        width=fill × height=hug
   • padding:     horizontal=16, vertical=12
   • spacing:     itemSpacing=12
   • shape:       borderRadius=12
   • colors:
       – background: MaterialTheme.colorScheme.surface
       – icon-trailing: MaterialTheme.colorScheme.onSurfaceVariant
   • typography:
       – title: LexemeStyle.BodyL
       – usage: LexemeStyle.BodyS (color=gray)
   • params:
       – row: UserDefinedRow
       – onEdit: (ComponentTypeId) -> Unit                   (phase 2: now triggers EditDialog, not RenameDialog)
       – onDelete: (ComponentTypeId) -> Unit
   • callbacks:
       – onEdit → Msg.OpenEditDialog(typeId)                 (phase 2 CHANGED — раньше OpenRenameDialog)
       – onDelete → Msg.OpenDeleteConfirm(typeId)
   • behavior:    rename functionality остаётся доступной как ветка внутри EditDialog (name-only edit); rename как отдельный flow — backlog cleanup.
   • notes:
       ℹ️ Извлечён в shared module. Раньше передавал onEdit→Msg.OpenRenameDialog; phase 2 — onEdit→Msg.OpenEditDialog (передача callback наружу — само переключение делает screen).
       🚨 Концептуально: Rename-only кнопка как отдельный UX-affordance может быть восстановлена backlog'ом (separate ic_rename + ic_edit), сейчас одна кнопка Edit покрывает оба case'а.
   • source:      проектное решение — phase 1 рендер сохранён, callback semantic меняется.
```

### 🔄 `<𝗣𝗲𝗿𝗗𝗶𝗰𝘁𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>` (changed, phase 2 — Edit-action callback + extract)

```
   • structure:
       row  spacing=12  padding=h:16 v:12  container=Surface  shape=rounded-12
         leading_slot: icon  variant=IconBoxed  iconRes=ic_components  size=24
         content_slot:
           column  spacing=4  weight=1
             title_slot:
               row  spacing=8
                 name_text:   text  source=row.name  style=LexemeStyle.BodyL
                 global_chip: chip  variant=AssistChip  label=R.string.components_chip_global  visible=∀ row.isGlobal
             meta_slot:
               row  spacing=8
                 template_chip:    chip  variant=AssistChip  label=row.template.labelRes
                 cardinality_chip: chip  variant=AssistChip  label=R.string.components_chip_multi|single
             usage_text: text  source=R.string.per_dict_row_value_count  arg=row.valueCount  style=LexemeStyle.BodyS  color=gray
         trailing_slot:   icon  variant=IconBoxed  iconRes=ic_edit   size=44  onClick=onEdit
         trailing_slot_2: icon  variant=IconBoxed  iconRes=ic_trash  size=44  onClick=onDelete
   • type:        Surface + Row composite
   • size:        width=fill × height=hug
   • padding:     horizontal=16, vertical=12
   • spacing:     itemSpacing=12
   • shape:       borderRadius=12
   • params:
       – row: PerDictRow
       – onEdit: (ComponentTypeId) -> Unit
       – onDelete: (ComponentTypeId) -> Unit
   • callbacks:
       – onEdit → Msg.OpenEditDialog(typeId)                 (phase 2 CHANGED)
       – onDelete → Msg.OpenDeleteConfirm(typeId)
   • behavior:    отличие от UserDefinedRow: global_chip — в title row (рядом с name), а не в meta row; usage_text использует i18n string `per_dict_row_value_count` formatted с count (F161).
   • notes:
       ℹ️ Извлечён в shared module параллельно с UserDefinedRowWidget. Backlog: параметризовать в единый `ComponentRowWidget(row: ComponentRowState)` где `ComponentRowState` — sealed; пока два варианта.
   • source:      проектное решение — phase 1 рендер сохранён, callback semantic меняется.
```

### 🔄 `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗘𝗺𝗽𝘁𝘆𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>` (changed, phase 2 — extract в shared module)

```
   • structure:
       column  padding=32  spacing=16  align=center
         icon_slot:     icon  iconRes=ic_components  size=64
         headline_slot: text  source=headlineRes  style=LexemeStyle.H6
         body_slot:     text  source=bodyRes  style=LexemeStyle.BodyL  color=gray
         cta_slot:      button  variant=PrimaryFullButtonWidget  titleRes=ctaRes  onClick=onCreate
   • type:        Column composite
   • size:        width=fill × height=hug
   • padding:     all=32
   • spacing:     itemSpacing=16
   • alignment:   horizontal=Center
   • params:
       – headlineRes: Int
       – bodyRes: Int
       – ctaRes: Int = R.string.components_empty_cta
       – onCreate: () -> Unit
   • callbacks:   onCreate → Msg.OpenCreateDialog
   • notes:
       ℹ️ Извлечён в shared module; раньше дублировался pixel-perfect между двумя screen-модулями (KDoc уже декларировал "общий", но фактически жил в двух местах).
   • source:      проектное решение — phase 1 рендер сохранён.
```

### 🔄 `<𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗙𝗮𝗯>` (changed, phase 2 — extract в shared module)

```
   • structure:
       box
         content_slot: button  variant=PrimaryLongFabWidget  iconRes=ic_add  titleRes=R.string.components_create_cta  enabled=true  onClick=onClick
   • type:        thin wrapper над PrimaryLongFabWidget
   • size:        width=hug × height=hug
   • params:
       – onClick: () -> Unit
   • callbacks:   onClick → Msg.OpenCreateDialog
   • notes:
       ℹ️ Извлечён в shared module; раньше дублировался pixel-perfect.
   • source:      проектное решение — phase 1 рендер сохранён.
```

### ❇️ `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗕𝘆𝗧𝗲𝗺𝗽𝗹𝗮𝘁𝗲>` (NEW, phase 2 — per-template resolver)

```
   • structure:
       composite  resolver=exhaustive when по ComponentTemplate
         ∀ template is ComponentTemplate.TEXT:
           content_slot: composite variant=ComponentBlock  template_slot=TextWidget(value=values.value.text)
         ∀ template is ComponentTemplate.IMAGE:
           content_slot: composite variant=ComponentBlock  template_slot=ImageWidget(value=values.value.uri)   (backlog — IMAGE template UI)
   • type:        Compose dispatcher composable
   • params:
       – type: ComponentType
       – values: TemplateValues                 (sealed: TextValues | ImageValues)
       – editable: Boolean = false
       – onValueChange: (TemplateValues) -> Unit = {}
   • callbacks:
       – onValueChange → передаётся хосту (lexeme edit-flow); в phase 2 read-only mode default
   • behavior:    exhaustive when по template; unknown template (fail-soft на data слое возвращает null) — composable не рисует ничего либо placeholder.
   • notes:
       ℹ️ Фундамент per-template architecture (typed_views.md Tier 2). MVP: только TEXT template имеет рендер.
       ℹ️ Composable использует domain ComponentTemplate enum напрямую (модуль уже зависит от `:modules:domain:lexeme`).
   • source:      проектное решение — concept docs typed_views.md.
```

### ❇️ `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗕𝗹𝗼𝗰𝗸>` (NEW, phase 2 — name + content wrapper)

```
   • structure:
       column  spacing=4
         name_slot:    text  source=type.name  style=LexemeStyle.LabelM  color=onSurfaceVariant
         content_slot: composite slot  (passed via composable lambda)
   • type:        Compose Column wrapper
   • size:        width=fill × height=hug
   • spacing:     itemSpacing=4
   • params:
       – type: ComponentType
       – content: @Composable () -> Unit
   • callbacks:   none (pure structural)
   • notes:
       ℹ️ Wrapper для отрисовки name-label + произвольный content в едином стиле; используется ComponentByTemplate.
   • source:      проектное решение — concept typed_views.md.
```

### ❇️ `<𝗧𝗲𝘅𝘁𝗪𝗶𝗱𝗴𝗲𝘁>` (NEW, phase 2 — per-template composable for TEXT)

```
   • structure:
       ∀ !editable:
         content_slot: text  source=value  style=LexemeStyle.BodyL
       ∀ editable:
         content_slot: input  variant=LexemeTextFieldWidget  value=value  onValueChange=onValueChange
   • type:        Compose Text | LexemeTextFieldWidget (mode-dependent)
   • size:        width=fill × height=hug
   • params:
       – value: String
       – editable: Boolean = false
       – onValueChange: (String) -> Unit = {}
   • callbacks:
       – onValueChange → upstream (composed в TemplateValues.TextValues на хосте, далее в WordCard / future Lexeme edit-flow)
   • behavior:    read-only vs editable выбирается по `editable` параметру.
   • notes:
       ℹ️ Per-template Tier-2 widget. IMAGE template — backlog (не входит в phase 2 MVP).
   • source:      проектное решение — concept typed_views.md.
```

### 📌 `<𝗟𝗲𝘅𝗲𝗺𝗲𝗗𝗶𝗮𝗹𝗼𝗴>` (unchanged baseline)

```
   • structure:
       box  shape=rounded-16  padding=24  background=surface
         content_slot: column  (passed via composable lambda)
   • type:        Compose Dialog + Surface wrapper
   • params:
       – onDismissRequest: () -> Unit
       – dismissOnBackPress / dismissOnClickOutside / usePlatformDefaultWidth / decorFitsSystemWindows
       – content: @Composable ColumnScope.() -> Unit
   • notes:       ℹ️ Используется как container для всех 4 dialog'ов фичи. Phase 2 не меняет.
   • source:      existing `:modules:core:ui` primitive.
```

### 📌 `<𝗟𝗲𝘅𝗲𝗺𝗲𝗥𝗮𝗱𝗶𝗼𝗥𝗼𝘄>` (unchanged baseline, F162)

```
   • structure:
       row  padding=h:8 v:8  selectable=role:RadioButton
         radio_slot:  ⚙️ RadioButton  selected=selected
         label_slot:  ⚙️ Text  source=textRes  style=LexemeStyle.BodyL
   • params:      textRes: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier
   • notes:       ℹ️ Используется в template_slot (Create/Edit dialogs) и scope_slot (Create Manager variant).
   • source:      existing `:modules:core:ui` primitive (F162 phase 1).
```

### 📌 `<𝗟𝗲𝘅𝗲𝗺𝗲𝗧𝗲𝘅𝘁𝗙𝗶𝗲𝗹𝗱𝗪𝗶𝗱𝗴𝗲𝘁>` / `<𝗖𝗮𝗻𝗰𝗲𝗹𝗕𝘂𝘁𝘁𝗼𝗻𝗪𝗶𝗱𝗴𝗲𝘁>` / `<𝗣𝗿𝗶𝗺𝗮𝗿𝘆𝗙𝘂𝗹𝗹𝗕𝘂𝘁𝘁𝗼𝗻𝗪𝗶𝗱𝗴𝗲𝘁>` / `<𝗔𝗹𝗮𝗿𝗺𝗕𝘂𝘁𝘁𝗼𝗻𝗪𝗶𝗱𝗴𝗲𝘁>` / `<𝗣𝗿𝗶𝗺𝗮𝗿𝘆𝗟𝗼𝗻𝗴𝗙𝗮𝗯𝗪𝗶𝗱𝗴𝗲𝘁>` / `<𝗣𝗿𝗶𝗺𝗮𝗿𝘆𝗧𝗲𝘅𝘁𝗕𝘂𝘁𝘁𝗼𝗻𝗪𝗶𝗱𝗴𝗲𝘁>` / `<𝗜𝗰𝗼𝗻𝗕𝗼𝘅𝗲𝗱>` (unchanged baseline)

```
   • notes:       ℹ️ Existing `:modules:core:ui` primitives — переиспользуются в shared widget module без изменений.
   • source:      existing.
```

### 📌 `<𝗘𝗿𝗿𝗼𝗿𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>` (unchanged baseline, F163)

```
   • notes:       ℹ️ Используется для load-failed ветки экранов. Phase 2 не меняет.
   • source:      existing `:modules:core:ui`.
```

### 📌 `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗧𝗲𝗺𝗽𝗹𝗮𝘁𝗲𝗟𝗮𝗯𝗲𝗹>` / `<𝗡𝗮𝗺𝗲𝗘𝗿𝗿𝗼𝗿𝗟𝗮𝗯𝗲𝗹>` (extension funcs — extract)

```
   • notes:       ℹ️ Extension functions ComponentTemplate.labelRes() / NameError.labelRes() — переезжают в shared widget module как top-level internal extensions (не composables); рендер семантика идентична phase 1.
   • source:      existing extract.
```

---

## ❇️ НОВЫЕ ВИДЖЕТЫ

- `<𝗘𝗱𝗶𝘁𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>` — name input + template radio (gated) + isMulti checkbox + downgrade preview + actions.
- `<𝗖𝗮𝗿𝗱𝗶𝗻𝗮𝗹𝗶𝘁𝘆𝗗𝗼𝘄𝗻𝗴𝗿𝗮𝗱𝗲𝗣𝗿𝗲𝘃𝗶𝗲𝘄𝗪𝗶𝗱𝗴𝗲𝘁> ` — inline top-3 lexeme list + drill-in button (видна iff InlineWithDrillIn).
- `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗕𝘆𝗧𝗲𝗺𝗽𝗹𝗮𝘁𝗲>` — exhaustive-when resolver по ComponentTemplate; фундамент per-template architecture.
- `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗕𝗹𝗼𝗰𝗸>` — name + content wrapper для отрисовки компонента в едином стиле.
- `<𝗧𝗲𝘅𝘁𝗪𝗶𝗱𝗴𝗲𝘁>` — per-template Tier-2 composable для TEXT template (read-only + editable modes).

---

## 🔧 МЕНЯЕМ (ключевое)

- `<𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>` — добавлен scope_slot (radio Global / PerDictionaries + FilterChip group по `availableDictionaries`) для Manager variant; API rewrite: плоские примитивы вместо `CreateDialogState`; переезд в shared module.
- `<𝗥𝗲𝗻𝗮𝗺𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>` — переезд в shared module; API rewrite (плоские примитивы вместо `RenameDialogState`).
- `<𝗗𝗲𝗹𝗲𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗶𝗮𝗹𝗼𝗴>` — переезд в shared module; API rewrite (плоские примитивы + lightweight `DeletionImpactRef` display-DTO).
- `<𝗨𝘀𝗲𝗿𝗗𝗲𝗳𝗶𝗻𝗲𝗱𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>` — переезд в shared module; `onEdit` callback теперь триггерит `Msg.OpenEditDialog` (раньше `OpenRenameDialog`).
- `<𝗣𝗲𝗿𝗗𝗶𝗰𝘁𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>` — переезд в shared module; `onEdit` callback теперь триггерит `Msg.OpenEditDialog`.
- `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗘𝗺𝗽𝘁𝘆𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>` — переезд в shared module (рендер 1-в-1).
- `<𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗙𝗮𝗯>` — переезд в shared module (рендер 1-в-1).
- `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗧𝗲𝗺𝗽𝗹𝗮𝘁𝗲𝗟𝗮𝗯𝗲𝗹>` / `<𝗡𝗮𝗺𝗲𝗘𝗿𝗿𝗼𝗿𝗟𝗮𝗯𝗲𝗹>` extension funcs — переезд в shared module как internal top-level extensions.
- `ComponentsManagerScreen` — добавлен mount `<𝗘𝗱𝗶𝘁𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>` (visible iff `state.editDialog != null`); подключение multi-dict picker (passing `availableDictionaries` + `selectedDictionaryIds` в `CreateComponentDialog`); все import paths меняются на `:modules:widget:component_widgets`.
- `PerDictionaryComponentsScreen` — добавлен mount `<𝗘𝗱𝗶𝘁𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>` (БЕЗ scope picker); import paths меняются на shared module.

---

## ❌ УДАЛЯЕМ (с миграцией)

**Из `modules/screen/components_manager/.../widget/`:**

- `CreateComponentDialog.kt` → `:modules:widget:component_widgets/dialogs/CreateComponentDialog.kt` (+ scope picker phase 2)
- `RenameComponentDialog.kt` → `:modules:widget:component_widgets/dialogs/RenameComponentDialog.kt`
- `DeleteComponentConfirmDialog.kt` → `:modules:widget:component_widgets/dialogs/DeleteComponentConfirmDialog.kt`
- `UserDefinedRowWidget.kt` → `:modules:widget:component_widgets/widgets/UserDefinedRowWidget.kt` (+ onEdit→OpenEditDialog)
- `ComponentsEmptyStateWidget.kt` → `:modules:widget:component_widgets/widgets/ComponentsEmptyStateWidget.kt`
- `CreateComponentFab.kt` → `:modules:widget:component_widgets/widgets/CreateComponentFab.kt`
- `ComponentTemplateLabel.kt` → `:modules:widget:component_widgets/widgets/ComponentTemplateLabel.kt` (top-level extension)
- `NameErrorLabel.kt` → `:modules:widget:component_widgets/widgets/NameErrorLabel.kt` (top-level extension)

**Из `modules/screen/per_dictionary_components/.../widget/`:**

- `CreateComponentDialog.kt` → `:modules:widget:component_widgets/dialogs/CreateComponentDialog.kt` (общая для обоих screen-модулей; PerDict передаёт hostVariant=PerDict)
- `RenameComponentDialog.kt` → `:modules:widget:component_widgets/dialogs/RenameComponentDialog.kt`
- `DeleteComponentConfirmDialog.kt` → `:modules:widget:component_widgets/dialogs/DeleteComponentConfirmDialog.kt`
- `PerDictRowWidget.kt` → `:modules:widget:component_widgets/widgets/PerDictRowWidget.kt` (+ onEdit→OpenEditDialog)
- `ComponentsEmptyStateWidget.kt` → `:modules:widget:component_widgets/widgets/ComponentsEmptyStateWidget.kt`
- `CreateComponentFab.kt` → `:modules:widget:component_widgets/widgets/CreateComponentFab.kt`
- `ComponentTemplateLabel.kt` → `:modules:widget:component_widgets/widgets/ComponentTemplateLabel.kt`
- `NameErrorLabel.kt` → `:modules:widget:component_widgets/widgets/NameErrorLabel.kt`

**Зеркальные пометки** (`notes:` принимающих widgets в shared module):

- `:modules:widget:component_widgets/dialogs/CreateComponentDialog.kt` — `ℹ️ принимает structure из удалённых widget/CreateComponentDialog.kt (обоих screen-модулей; миграция IS481 phase 2). Phase 2 расширение: scope_slot (Manager variant only).`
- `:modules:widget:component_widgets/dialogs/RenameComponentDialog.kt` — `ℹ️ принимает structure из удалённых widget/RenameComponentDialog.kt (обоих screen-модулей; миграция IS481 phase 2).`
- `:modules:widget:component_widgets/dialogs/DeleteComponentConfirmDialog.kt` — `ℹ️ принимает structure из удалённых widget/DeleteComponentConfirmDialog.kt (обоих screen-модулей; миграция IS481 phase 2).`
- `:modules:widget:component_widgets/widgets/UserDefinedRowWidget.kt` — `ℹ️ принимает structure из удалённого components_manager/widget/UserDefinedRowWidget.kt; onEdit callback переключён с Msg.OpenRenameDialog на Msg.OpenEditDialog (миграция IS481 phase 2).`
- `:modules:widget:component_widgets/widgets/PerDictRowWidget.kt` — `ℹ️ принимает structure из удалённого per_dictionary_components/widget/PerDictRowWidget.kt; onEdit callback переключён на Msg.OpenEditDialog (миграция IS481 phase 2).`
- `:modules:widget:component_widgets/widgets/ComponentsEmptyStateWidget.kt` — `ℹ️ принимает structure из удалённых widget/ComponentsEmptyStateWidget.kt (обоих screen-модулей; миграция IS481 phase 2).`
- `:modules:widget:component_widgets/widgets/CreateComponentFab.kt` — `ℹ️ принимает structure из удалённых widget/CreateComponentFab.kt (обоих screen-модулей; миграция IS481 phase 2).`
- `:modules:widget:component_widgets/widgets/ComponentTemplateLabel.kt` / `NameErrorLabel.kt` — `ℹ️ принимает extension functions из удалённых одноимённых файлов обоих screen-модулей (миграция IS481 phase 2).`

**Удалено бесследно:** нет — все 16 удаляемых файлов имеют целевую миграцию в shared module.

---

## 🖼 ИКОНКИ К ИМПОРТУ

Phase 2 не добавляет новых иконок. Используются existing:

- `ic_components.xml` — leading icon в Row widgets + ComponentsEmptyStateWidget (existing phase 1).
- `ic_edit.xml` — trailing edit button в Row widgets (existing phase 1; callback в phase 2 меняется на OpenEditDialog).
- `ic_trash.xml` — trailing delete button (existing phase 1).
- `ic_add.xml` — FAB (existing phase 1).
- `ic_hammer.xml` — DictionaryAppBar entry-point (existing phase 1).

---

## 🆕 НОВЫЕ UX-СЦЕНАРИИ

- **Edit user-defined component** (rename + cardinality toggle, template gated).
- **Cardinality downgrade blocked → inline preview** (top-3 lexemes) + drill-in кнопка «Показать все N» если N > 3 (drill-in destination — backlog: bottom-sheet или отдельный screen).
- **Multi-dict create** — Manager: создание одного компонента сразу в N словарях одним диалогом (chip-multi-select).
- **Chip-staleness auto-filter** — словарь удалён out-of-band пока CreateDialog открыт → chip автоматически снимается из selection; submit disabled если selection опустел.
- **Removed-race snackbar** — Edit/Rename/Delete on soft-deleted type → snackbar «Компонент удалён» + close dialog (отдельно от «встроенный нельзя трогать»).

---

## 🎨 ПАЛИТРА

Phase 2 не вводит новых color tokens. Используются existing:

- `MaterialTheme.colorScheme.surface` / `.onSurface` — dialog containers + Row widgets.
- `MaterialTheme.colorScheme.errorContainer` / `.onErrorContainer` — `CardinalityDowngradePreviewWidget` background.
- `MaterialTheme.colorScheme.secondaryContainer` — FilterChip selected state (M3 default).
- `MaterialTheme.colorScheme.error` — name error text.

---

## log_messages

- read 02_scope.md (425 lines) + business_summary.md (140 lines) + ui_walkthrough.md (190 lines) + spec UI Layout раздел + format spec ui_layout.md — компиляция выполнена по существующему коду + concept docs (Figma не используется, Case A).
- собрана карта двух экранов + EditDialog subscreen; 12 widgets в § Анализ виджетов (3 NEW dialog/preview + 2 NEW per-template + 1 NEW resolver + 7 CHANGED extract; 4 unchanged baseline primitive references).
- 16 удалений зафиксированы с целевой миграцией в `:modules:widget:component_widgets` (8 файлов × 2 screen-модуля); зеркальные пометки сформулированы для 8 принимающих widgets.

_model: claude-opus-4-7[1m]_
