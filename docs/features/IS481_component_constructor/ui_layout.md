# IS481 component_constructor — конструктор компонентов · UI cheat-sheet

## 📋 ЧТО ДЕЛАЕМ

- Новый screen `ComponentsManagerScreen` — aggregated CRUD view всех user-defined компонентов (drill-in из Settings).
- Новый screen `PerDictionaryComponentsScreen` — scoped CRUD для конкретного словаря (drill-in из `DictionaryAppBar` по icon «молоток»).
- Новый entry в `SettingsTab` — `ComponentsManageWidget` рядом с `LangManageWidget` (drill-in в `ComponentsManagerScreen`).
- Новый icon-button «молоток» в `actions` слоте `DictionaryAppBar` перед `DictDropDownWidget` (видим только при `currentDict != null`).
- Создание / переименование / soft-delete user-defined компонента; delete-confirm с `DeletionImpact` (values + dictionaries + quizConfigs + prefs).
- Snackbar (state-driven) для error/success нотификаций на обоих экранах.
- Empty-state на обоих экранах (0 user-defined компонентов в scope).

## 🏷 ЛЕГЕНДА

- **⚙️** — системный Material3 / Compose (`Scaffold`, `Column`, `LazyColumn`, `Dialog`, `TopAppBar`, `Checkbox`).
- **❇️** — новый кастомный виджет (в этой фиче впервые).
- **🔄** — кастомный, меняется в этой фиче.
- **📌** — кастомный, не меняется в этой фиче (но трогается callsite).
- **🚨** — отход от Figma / предупреждение в `notes:` (в этой фиче Figma нет — для миграционных пометок и зеркал из § ❌ УДАЛЯЕМ).
- **ℹ️** — обычная пояснительная заметка в `notes:`.

---

## 🗺 Карта экрана

### Экран 1 — `ComponentsManagerScreen` (drill-in из Settings)

```
⚙️ Scaffold
├─ ⚙️ TopAppBar                                  title=R.string.components_manager_title, navigation=back-arrow
├─ ↘️ snackbarHost                               state-driven, ∀ state.snackbarState != null
└─ ⚙️ Box (content)                              padding=paddings, fillMaxSize
   ├─ ∀ state.isLoading && state.userDefinedTypes == null:
   │  └─ ⚙️ CircularProgressIndicator            align=Center
   ├─ ∀ state.isEmpty (loaded && empty):
   │  └─ ❇️ <𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗘𝗺𝗽𝘁𝘆𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>     centered, with create-CTA
   ├─ ∀ state.userDefinedTypes != null && !state.isEmpty:
   │  └─ ⚙️ LazyColumn (× N rows)                contentPadding=h:16 v:8  spacing=8
   │     └─ ❇️ <𝗨𝘀𝗲𝗿𝗗𝗲𝗳𝗶𝗻𝗲𝗱𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>          row=UserDefinedRow, onEdit, onDelete
   ↘️ FAB-slot                                   pos=BottomEnd, padding=16
   └─ ❇️ <𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗙𝗮𝗯>                   visible=always, onClick→Msg.OpenCreateDialog
   ↘️ Dialog-overlay slots                       ∀ соответствующий dialog != null
   ├─ ❇️ <𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>               ∀ createDialog != null
   ├─ ❇️ <𝗥𝗲𝗻𝗮𝗺𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>               ∀ renameDialog != null
   └─ ❇️ <𝗗𝗲𝗹𝗲𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗶𝗮𝗹𝗼𝗴>       ∀ deleteConfirm != null
```

### Экран 2 — `PerDictionaryComponentsScreen` (drill-in по «молоток» из `DictionaryAppBar`)

```
⚙️ Scaffold
├─ ⚙️ TopAppBar                                  title=state.dictionaryName ?: R.string.per_dict_components_title, navigation=back-arrow
├─ ↘️ snackbarHost                               state-driven
└─ ⚙️ Box (content)                              padding=paddings, fillMaxSize
   ├─ ∀ state.isLoading && state.items == null:
   │  └─ ⚙️ CircularProgressIndicator            align=Center
   ├─ ∀ state.isEmpty:
   │  └─ ❇️ <𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗘𝗺𝗽𝘁𝘆𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>     centered, with create-CTA (variant=per-dict)
   ├─ ∀ state.items != null && !state.isEmpty:
   │  └─ ⚙️ LazyColumn (× N rows)                contentPadding=h:16 v:8  spacing=8
   │     └─ ❇️ <𝗣𝗲𝗿𝗗𝗶𝗰𝘁𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>               row=PerDictRow, onEdit, onDelete
   ↘️ FAB-slot                                   pos=BottomEnd, padding=16
   └─ ❇️ <𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗙𝗮𝗯>                   onClick→Msg.OpenCreateDialog (scope preselect=PerDictionaries(dictId))
   ↘️ Dialog-overlay slots                       ∀ соответствующий dialog != null
   ├─ ❇️ <𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>
   ├─ ❇️ <𝗥𝗲𝗻𝗮𝗺𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>
   └─ ❇️ <𝗗𝗲𝗹𝗲𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗶𝗮𝗹𝗼𝗴>
```

### Точка касания 3 — `SettingsTabScreen` (новый entry-row)

```
⚙️ Scaffold (existing)
└─ ⚙️ LazyColumn                                 contentPadding=h:16  spacing=8
   └─ item: 📌 <𝗦𝗲𝘁𝘁𝗶𝗻𝗴𝘀𝗦𝗲𝗰𝘁𝗶𝗼𝗻𝗪𝗶𝗱𝗴𝗲𝘁> (existing)
      ├─ 📌 <𝗟𝗮𝗻𝗴𝗠𝗮𝗻𝗮𝗴𝗲𝗪𝗶𝗱𝗴𝗲𝘁>                   (existing)
      ├─ ❇️ <𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗠𝗮𝗻𝗮𝗴𝗲𝗪𝗶𝗱𝗴𝗲𝘁>           onClick → Msg.OpenComponentsManager
      ├─ 📌 <𝗘𝘅𝗽𝗼𝗿𝘁𝗗𝗮𝘁𝗮𝗪𝗶𝗱𝗴𝗲𝘁>                   (existing)
      └─ 📌 <𝗜𝗺𝗽𝗼𝗿𝘁𝗗𝗮𝘁𝗮𝗪𝗶𝗱𝗴𝗲𝘁>                   (existing)
```

### Точка касания 4 — `DictionaryAppBar` (новый icon «молоток»)

```
⚙️ TopAppBar (existing, shared 3 tabs)
├─ title: 📌 <𝗔𝗽𝗽𝗕𝗮𝗿𝗧𝗶𝘁𝗹𝗲𝗪𝗶𝗱𝗴𝗲𝘁>               (existing)
└─ actions:
   ∀ state.isLoading:
   │  └─ ⚙️ CircularProgressIndicator            (existing)
   ∀ !state.isLoading:
   ├─ ∀ state.currentDict != null:
   │  └─ ❇️ <𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗧𝗼𝗼𝗹𝘀𝗜𝗰𝗼𝗻𝗕𝘂𝘁𝘁𝗼𝗻>        iconRes=ic_hammer, onClick→Msg.OpenPerDictionaryComponents(currentDict.id)
   └─ 📌 <𝗗𝗶𝗰𝘁𝗗𝗿𝗼𝗽𝗗𝗼𝘄𝗻𝗪𝗶𝗱𝗴𝗲𝘁>                   (existing)
```

---

## 🔍 Анализ виджетов

### Экран 1 (`ComponentsManagerScreen`) и Экран 2 (`PerDictionaryComponentsScreen`) — общие виджеты

Виджеты `CreateComponentDialog`, `RenameComponentDialog`, `DeleteComponentConfirmDialog`, `CreateComponentFab`, `ComponentsEmptyStateWidget` используются на обоих экранах и описаны единожды. Различия отмечены в `params:` либо `behavior:`.

---

#### ❇️ `<𝗨𝘀𝗲𝗿𝗗𝗲𝗳𝗶𝗻𝗲𝗱𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>`

```
   • structure:
       row  spacing=12  padding=h:16 v:12
         leading_slot:  icon  iconRes=ic_components  size=24  color=enableIconColor
         content_slot:
           column  spacing=4  weight=1
             title_slot:   text  source=row.name  style=LexemeStyle.BodyL
             meta_slot:
               row  spacing=8
                 template_chip: chip  variant=AssistChip  label=row.template.displayName  style=labelSmall
                 cardinality_chip: chip  variant=AssistChip  label="single"|"multi"  visible=always
                 usage_text: text  source="N · {dict1, dict2}"  style=LexemeStyle.BodyS  color=grayTextColor
         trailing_slot: icon  iconRes=ic_edit  size=44  onClick=onEdit
         trailing_slot_2: icon  iconRes=ic_trash  size=44  onClick=onDelete
   • type:        M3 Surface (Card-style) + Row через `SettingsItemWidget`-style scaffolding
   • size:        width=fill × height=hug
   • padding:     horizontal=16, vertical=12
   • spacing:     itemSpacing=12 (row), itemSpacing=4 (column inner), itemSpacing=8 (meta row)
   • shape:       borderRadius=12 (parent Surface)
   • colors:
       – background: MaterialTheme.colorScheme.surface
       – content: MaterialTheme.colorScheme.onSurface
       – meta: me.apomazkin.theme.grayTextColor (0xFF7B7E85)
       – icon enabled: me.apomazkin.theme.enableIconColor (0xff252628)
   • typography:
       – title: LexemeStyle.BodyL (17sp Regular)
       – meta_usage: LexemeStyle.BodyS
       – chip-label: MaterialTheme.typography.labelSmall
   • params:
       – row: UserDefinedRow
       – onEdit: (typeId: ComponentTypeId) -> Unit
       – onDelete: (typeId: ComponentTypeId) -> Unit
   • callbacks:
       – onEdit → Msg.OpenRenameDialog(typeId)
       – onDelete → Msg.OpenDeleteConfirm(typeId)
   • behavior:
       cardinality_chip всегда показывается (juser видит type-tag сразу).
       Если row.dictionaryNames пустой и row.usageCount=0 → meta показывает "0 · —".
       Если row.scope=Global → meta содержит "global" badge перед usage_text.
   • notes:
       ℹ️ Aggregated row — показывает scope (через dictionaryNames либо «global») и usage.
       ℹ️ Используется только на `ComponentsManagerScreen` (per-dict экран использует `<PerDictRowWidget>`).
       ℹ️ Callbacks передают только typeId; name берётся Reducer'ом из state.rows.
   • source:      проектное решение — single-row UI для CRUD list (precedent `DictionaryListItemWidget`, `SettingsItemWidget`).
```

#### ❇️ `<𝗣𝗲𝗿𝗗𝗶𝗰𝘁𝗥𝗼𝘄𝗪𝗶𝗱𝗴𝗲𝘁>`

```
   • structure:
       row  spacing=12  padding=h:16 v:12
         leading_slot: icon  iconRes=ic_components  size=24  color=enableIconColor
         content_slot:
           column  spacing=4  weight=1
             title_slot:
               row  spacing=8
                 name_text: text  source=row.name  style=LexemeStyle.BodyL
                 global_badge: chip  variant=AssistChip  label="global"  visible=∀ row.isGlobal
             meta_slot:
               row  spacing=8
                 template_chip: chip  variant=AssistChip  label=row.template.displayName
                 cardinality_chip: chip  variant=AssistChip  label="single"|"multi"
                 usage_text: text  source="N values"  style=LexemeStyle.BodyS  color=grayTextColor
         trailing_slot:  icon  iconRes=ic_edit  size=44  onClick=onEdit  enabled=true
         trailing_slot_2: icon  iconRes=ic_trash  size=44  onClick=onDelete
   • type:        M3 Surface + Row (parity с UserDefinedRow)
   • size:        width=fill × height=hug
   • padding:     horizontal=16, vertical=12
   • spacing:     itemSpacing=12
   • shape:       borderRadius=12
   • colors:
       – background: MaterialTheme.colorScheme.surface
       – content: MaterialTheme.colorScheme.onSurface
       – meta: me.apomazkin.theme.grayTextColor
   • typography:
       – title: LexemeStyle.BodyL
       – meta: LexemeStyle.BodyS
   • params:
       – row: PerDictRow
       – onEdit: (typeId: ComponentTypeId) -> Unit
       – onDelete: (typeId: ComponentTypeId) -> Unit
   • callbacks:
       – onEdit → Msg.OpenRenameDialog(typeId)
       – onDelete → Msg.OpenDeleteConfirm(typeId)
   • behavior:
       global_badge видим только при row.isGlobal=true (визуально маркирует global компонент,
       чтобы юзер не удивлялся «почему я его не могу убрать из словаря — он на все»).
       Edit/Delete у global компонента активны (UseCase enforce'ит правила; UI не блокирует).
   • notes:
       ℹ️ valueCount считается только в context'е текущего словаря (`PerDictionarySnapshot.valueCountByType`).
       ℹ️ Callbacks передают только typeId; name берётся Reducer'ом из state.rows.
   • source:      проектное решение — зеркально `UserDefinedRowWidget`, scoped.
```

#### ❇️ `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗘𝗺𝗽𝘁𝘆𝗦𝘁𝗮𝘁𝗲𝗪𝗶𝗱𝗴𝗲𝘁>`

```
   • structure:
       column  spacing=16  align=center  padding=32
         icon_slot: icon  iconRes=ic_components  size=64  color=unselectedGreyColor
         message_slot:
           column  spacing=8  align=center
             headline_slot: text  source=R.string.components_empty_headline  style=LexemeStyle.H6
             body_slot: text  source=R.string.components_empty_body  style=LexemeStyle.BodyL  color=grayTextColor
         cta_slot: button  variant=PrimaryFullButton  label=R.string.components_empty_cta  onClick=onCreate
   • type:        Column (centered) + PrimaryFullButtonWidget
   • size:        width=fill × height=hug, vertical center on screen via parent Box alignment
   • padding:     32
   • spacing:     16
   • colors:
       – icon: me.apomazkin.theme.unselectedGreyColor (0xff95989D)
       – body: me.apomazkin.theme.grayTextColor
   • typography:
       – headline: LexemeStyle.H6 (20sp Medium)
       – body: LexemeStyle.BodyL
   • params:
       – headlineRes: Int
       – bodyRes: Int
       – ctaRes: Int
       – onCreate: () -> Unit
   • callbacks:   onCreate → Msg.OpenCreateDialog
   • behavior:
       Один общий компонент с двумя preset-вариантами:
       (1) Manager: headline="У вас нет своих компонентов", body="Translation работает автоматически в каждом словаре."
       (2) PerDict: headline="В этом словаре только translation", body="Создайте дополнительный компонент."
       Variant выбирается callsite'ом через resIds.
   • notes:
       ℹ️ Reuse `PrimaryFullButtonWidget` (`core/ui/btn/`).
   • source:      проектное решение — empty-state стандартный паттерн.
```

#### ❇️ `<𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗙𝗮𝗯>`

```
   • structure:
       box
         content_slot: button  variant=PrimaryLongFab  iconRes=ic_add  label=R.string.components_create_cta
   • type:        PrimaryLongFabWidget (existing project FAB, `core/ui/btn/`)
   • size:        height=56 × width=hug (по convention longFab); либо PrimaryFabWidget (round) — финализация на implement
   • padding:     parent box: end=16, bottom=16 (Scaffold FAB slot)
   • shape:       borderRadius=28 (round-corners FAB)
   • colors:
       – background: MaterialTheme.colorScheme.primary
       – content: MaterialTheme.colorScheme.onPrimary
   • typography:
       – label: LexemeStyle.BodyLBold
   • params:
       – onClick: () -> Unit
   • callbacks:   onClick → Msg.OpenCreateDialog
   • notes:
       ℹ️ Reuse existing `PrimaryLongFabWidget` либо `PrimaryFabWidget`. Финальный выбор variant
       (long-with-label vs round-with-icon-only) — на ui_implement / design_tree.
   • source:      проектное решение — стандартный FAB-pattern (precedent: `VocabularyTabScreen` FAB add-word).
```

#### ❇️ `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗧𝗼𝗼𝗹𝘀𝗜𝗰𝗼𝗻𝗕𝘂𝘁𝘁𝗼𝗻>` (в `DictionaryAppBar`)

```
   • structure:
       box
         content_slot: icon  iconRes=ic_hammer  size=44  contentDescriptionRes=R.string.components_tools_description  onClick=onClick
   • type:        IconBoxed (existing Tier 1 primitive в `core/ui/IconBoxed.kt`)
   • size:        width=44 × height=44 (IconBoxed default)
   • padding:     IconBoxed internal
   • shape:       clipShape=CircleShape (IconBoxed default)
   • colors:
       – content: me.apomazkin.theme.enableIconColor (0xff252628)
   • params:
       – currentDictId: Long  (известно из state, прокидывается callsite в onClick lambda)
       – onClick: () -> Unit
   • callbacks:
       – onClick → Msg.OpenPerDictionaryComponents(currentDict.id)
   • behavior:
       Видим **только** при `state.currentDict != null && !state.isLoading`.
       Размещается **перед** `DictDropDownWidget` в actions slot `TopAppBar`.
   • notes:
       ℹ️ Требует новый vector drawable `ic_hammer.xml` в `core/core-resources/src/main/res/drawable/`.
       Существующих icon'ов с семантикой «молоток/инструменты» нет (см. ui_walkthrough.md § DictionaryAppBar).
       ℹ️ Принимает onClick → Msg.OpenPerDictionaryComponents из nav-chain (Reducer + NavigationEffect готовы в infra).
       ℹ️ Виджет автоматически появится на всех 3 табах (Vocabulary / Quiz / Statistic) — `DictionaryAppBar` shared widget.
   • source:      проектное решение — icon-button перед dropdown (ui_placement.md § Per-dictionary конструктор).
```

#### ❇️ `<𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝘀𝗠𝗮𝗻𝗮𝗴𝗲𝗪𝗶𝗱𝗴𝗲𝘁>` (в `SettingsTabScreen`)

```
   • structure:
       box
         content_slot: button  variant=SettingsItemWidget  iconRes=ic_components  titleRes=R.string.settings_section_components_management  showNextIcon=true  onClick=onClick
   • type:        SettingsItemWidget (existing project widget в `settingstab/widgets/settings/items/base/`)
   • size:        width=fill × height=hug
   • padding:     horizontal=8, vertical=12 (SettingsItemWidget internal)
   • spacing:     itemSpacing=12 (SettingsItemWidget internal Row)
   • shape:       borderRadius=12 (SettingsItemWidget Surface)
   • colors:
       – background: MaterialTheme.colorScheme.surface (parent SettingsSectionWidget)
       – title: MaterialTheme.colorScheme.onSurface
       – icon: me.apomazkin.theme.enableIconColor
       – next-icon: me.apomazkin.theme.unselectedGreyColor
   • typography:
       – title: LexemeStyle.BodyL
   • params:
       – onClick: () -> Unit
   • callbacks:
       – onClick → Msg.OpenComponentsManager (в `SettingsTabReducer`)
   • behavior:
       Размещение — внутри first `SettingsSectionWidget` рядом с `LangManageWidget`
       (между `LangManageWidget` и `ExportDataWidget` либо как separate section — финализация на implement).
   • notes:
       ℹ️ Требует новый icon drawable `ic_components.xml` (если не reuse `ic_hammer`) + string
       `settings_section_components_management` в `core-resources/strings.xml`.
       ℹ️ Прямой precedent — `LangManageWidget` (11 строк, identical shape).
   • source:      проектное решение — settings entry parity с `LangManageWidget` (ui_placement.md § Глобальный конструктор).
```

#### ❇️ `<𝗖𝗿𝗲𝗮𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>`

```
   • structure:
       column  spacing=16  padding=24
         title_slot: text  source=R.string.components_create_dialog_title  style=LexemeStyle.H6
         name_slot:
           column  spacing=4
             label: text  source=R.string.components_create_field_name  style=LexemeStyle.BodyS  color=grayTextColor
             input: input  variant=LexemeTextFieldWidget  value=createDialog.name  placeholder=R.string.components_create_name_placeholder
             error: text  source=createDialog.nameError?.localized  style=LexemeStyle.BodyS  color=colorScheme.error  visible=∀ nameError != null
         template_slot:
           column  spacing=4
             label: text  source=R.string.components_create_field_template  style=LexemeStyle.BodyS  color=grayTextColor
             options:
               column  spacing=0
                 ∀ ComponentTemplate.values():
                   option_row: button  variant=LexemeRadioMenuItem  isSelected=(template == createDialog.template)  title=template.displayName  onSelect=onTemplateSelect
         multi_slot:
           row  spacing=8  align=center
             checkbox: button  variant=M3-Checkbox  checked=createDialog.isMulti  onCheckedChange=onMultiToggle
             label: text  source=R.string.components_create_field_is_multi  style=LexemeStyle.BodyL
         scope_slot:
           column  spacing=4
             label: text  source=R.string.components_create_field_scope  style=LexemeStyle.BodyS  color=grayTextColor
             options:
               column  spacing=0
                 global_option: button  variant=LexemeRadioMenuItem  isSelected=(scope is Scope.Global)  title=R.string.components_create_scope_global  onSelect=onScopeGlobal
                 per_dict_option: button  variant=LexemeRadioMenuItem  isSelected=(scope is Scope.PerDictionaries)  title=R.string.components_create_scope_per_dict  onSelect=onScopePerDict
                 per_dict_picker: button  variant=SecondaryButton  visible=∀ scope is Scope.PerDictionaries  label="Выбрать словари (N)"  onClick=onOpenDictionaryPicker
         actions_slot:
           row  spacing=12  padding=top:16
             cancel_btn: button  variant=CancelButtonWidget  weight=1  onClick=onDismiss
             submit_btn: button  variant=PrimaryFullButton  weight=1  enabled=canSubmit  label=R.string.button_create  onClick=onSubmit
   • type:        LexemeDialog (existing wrapper в `core/ui/dialog/base/`) + Column content
   • size:        width=hug (platform default) × height=hug
   • padding:     24 (LexemeDialog Column internal)
   • spacing:     itemSpacing=16 (sections), 4 (label+input), 8 (multi row), 12 (actions)
   • shape:       borderRadius=16 (LexemeDialog Surface)
   • colors:
       – background: MaterialTheme.colorScheme.surface
       – content: MaterialTheme.colorScheme.onSurface
       – label/error: grayTextColor / colorScheme.error
   • typography:
       – title: LexemeStyle.H6
       – field-label: LexemeStyle.BodyS
       – option-label: LexemeStyle.BodyL
   • params:
       – createDialog: CreateDialogState
       – isSubmitting: Boolean  (= state.isCreating)
       – onNameChange: (String) -> Unit
       – onTemplateSelect: (ComponentTemplate) -> Unit
       – onMultiToggle: (Boolean) -> Unit
       – onScopeGlobal: () -> Unit
       – onScopePerDict: () -> Unit
       – onOpenDictionaryPicker: () -> Unit
       – onSubmit: () -> Unit
       – onDismiss: () -> Unit
   • callbacks:
       – onNameChange → Msg.CreateNameChange(name)
       – onTemplateSelect → Msg.CreateTemplateChange(template)
       – onMultiToggle → Msg.CreateMultiToggle(isMulti)
       – onScopeGlobal → Msg.CreateScopeChange(Scope.Global)
       – onScopePerDict → Msg.CreateScopeChange(Scope.PerDictionaries(currentSelection))
       – onOpenDictionaryPicker → Msg.OpenDictionaryPicker
       – onSubmit → Msg.SubmitCreate
       – onDismiss → Msg.CloseCreateDialog
   • behavior:
       canSubmit (computed) = name.isNotBlank() && nameError == null && !isSubmitting.
       isSubmitting=true → submit_btn disabled + spinner overlay (либо replace label на CircularProgressIndicator).
       На `PerDictionaryComponentsScreen` — initial scope preselect = PerDictionaries(listOf(dictionaryId)).
       На `ComponentsManagerScreen` — initial scope = Global.
       Template после создания не меняется (см. `template_model.md`); rename выполняется в `RenameComponentDialog`.
       per_dict_picker — TBD: открытие picker'а словарей; для MVP может быть multi-select chip-list в самом dialog'е либо отдельный sub-dialog.
   • notes:
       ℹ️ Reuse Tier 1 primitives: `LexemeDialog`, `LexemeTextFieldWidget` / `PrimaryTextFieldWidget`,
       `LexemeRadioMenuItem`, `PrimaryFullButtonWidget`, `CancelButtonWidget`.
       ℹ️ `material3.Checkbox` используется напрямую — проектного wrapper'а нет (см. walkthrough § Theme tokens).
   • source:      проектное решение — composite form-dialog поверх `LexemeDialog` (MVP без отдельного fullscreen create-screen).
```

#### ❇️ `<𝗥𝗲𝗻𝗮𝗺𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗗𝗶𝗮𝗹𝗼𝗴>`

```
   • structure:
       column  spacing=16  padding=24
         title_slot: text  source=R.string.components_rename_dialog_title  style=LexemeStyle.H6
         original_name_slot:
           column  spacing=4
             label: text  source=R.string.components_rename_original  style=LexemeStyle.BodyS  color=grayTextColor
             value: text  source=renameDialog.originalName  style=LexemeStyle.BodyL
         input_slot:
           column  spacing=4
             label: text  source=R.string.components_rename_new_name  style=LexemeStyle.BodyS  color=grayTextColor
             input: input  variant=LexemeTextFieldWidget  value=renameDialog.editedName  placeholder=R.string.components_rename_placeholder
             error: text  source=renameDialog.nameError?.localized  style=LexemeStyle.BodyS  color=colorScheme.error  visible=∀ nameError != null
         actions_slot:
           row  spacing=12  padding=top:16
             cancel_btn: button  variant=CancelButtonWidget  weight=1  onClick=onDismiss
             submit_btn: button  variant=PrimaryFullButton  weight=1  enabled=canSubmit  label=R.string.button_save  onClick=onSubmit
   • type:        LexemeDialog + Column
   • size:        width=hug × height=hug
   • padding:     24
   • spacing:     16
   • shape:       borderRadius=16
   • colors:
       – background: MaterialTheme.colorScheme.surface
       – content: MaterialTheme.colorScheme.onSurface
   • typography:
       – title: LexemeStyle.H6
       – field-label: LexemeStyle.BodyS
       – field-value: LexemeStyle.BodyL
   • params:
       – renameDialog: RenameDialogState
       – isSubmitting: Boolean  (= state.isRenaming)
       – onNameChange: (String) -> Unit
       – onSubmit: () -> Unit
       – onDismiss: () -> Unit
   • callbacks:
       – onNameChange → Msg.RenameTextChange(editedName)
       – onSubmit → Msg.SubmitRename
       – onDismiss → Msg.CloseRenameDialog
   • behavior:
       canSubmit = editedName.isNotBlank() && editedName != originalName && nameError == null && !isSubmitting.
       Template / isMulti / scope не редактируются (после создания template immutable).
   • notes:
       ℹ️ Принимает onSubmit из удалённого «inline rename» pattern (миграция IS481 — в проекте rename ранее не было).
   • source:      проектное решение — minimal rename dialog (1 поле).
```

#### ❇️ `<𝗗𝗲𝗹𝗲𝘁𝗲𝗖𝗼𝗺𝗽𝗼𝗻𝗲𝗻𝘁𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗶𝗮𝗹𝗼𝗴>`

```
   • structure:
       column  spacing=16  padding=24
         title_slot: text  source=R.string.components_delete_dialog_title (with name placeholder)  style=LexemeStyle.H6
         impact_slot:
           column  spacing=8
             ∀ deleteConfirm.isLoadingImpact && impact == null:
               progress: progress  variant=CircularProgressIndicator  size=24
             ∀ impact != null:
               column  spacing=4
                 values_line: text  source=R.string.components_delete_impact_values(impact.valueCount)  style=LexemeStyle.BodyL
                 dicts_line: text  source=R.string.components_delete_impact_dicts(impact.dictionariesWithValues.size)  style=LexemeStyle.BodyL
                 quiz_line: text  source=R.string.components_delete_impact_quiz(impact.quizConfigsAffected.size)  style=LexemeStyle.BodyL  visible=∀ size > 0
                 prefs_line: text  source=R.string.components_delete_impact_prefs(impact.affectedPrefs.size)  style=LexemeStyle.BodyL  visible=∀ size > 0
             ∀ impact == null && !isLoadingImpact:
               error: text  source=R.string.components_delete_impact_unavailable  style=LexemeStyle.BodyL  color=grayTextColor
         hint_slot: text  source=R.string.components_delete_hint  style=LexemeStyle.BodyS  color=grayTextColor
         actions_slot:
           row  spacing=12  padding=top:16
             cancel_btn: button  variant=CancelButtonWidget  weight=1  onClick=onDismiss
             confirm_btn: button  variant=AlarmButton  weight=1  enabled=canConfirm  label=R.string.button_delete  onClick=onConfirm
   • type:        LexemeDialog + Column (отдельно от `AlarmDialogWidget` — нужен kаstomный impact-preview блок)
   • size:        width=hug × height=hug
   • padding:     24
   • spacing:     16
   • shape:       borderRadius=16
   • colors:
       – background: MaterialTheme.colorScheme.surface
       – content: MaterialTheme.colorScheme.onSurface
       – hint: me.apomazkin.theme.grayTextColor
       – confirm-btn: AlarmButtonWidget colors (red palette)
   • typography:
       – title: LexemeStyle.H6
       – impact-lines: LexemeStyle.BodyL
       – hint: LexemeStyle.BodyS
   • params:
       – deleteConfirm: DeleteConfirmState
       – isSubmitting: Boolean  (= state.isDeleting)
       – onConfirm: () -> Unit
       – onDismiss: () -> Unit
   • callbacks:
       – onConfirm → Msg.ConfirmDelete
       – onDismiss → Msg.CloseDeleteConfirm
   • behavior:
       canConfirm = !isLoadingImpact && !isSubmitting.
       isLoadingImpact → spinner на месте impact-блока, confirm disabled (юзер видит counts перед confirm).
       isSubmitting (после tap Confirm) → confirm-btn → spinner (предотвращение double-tap дополнительно
       к Reducer guard `isDeleting=true → ignore ConfirmDelete`).
       impact==null && !isLoadingImpact = preview-load failed → показать «impact недоступен» (юзер
       решает confirm without numbers либо cancel).
   • notes:
       ℹ️ Reuse `LexemeDialog`, `AlarmButtonWidget`, `CancelButtonWidget` (Tier 1 в `core/ui/btn/`).
       ℹ️ Не использует `AlarmDialogWidget` напрямую (он не подходит под dynamic impact-content).
       ℹ️ Принимает confirm-flow из удалённого hard-delete pattern для component_types (миграция M12→M13:
       soft-delete + impact preview как explicit step).
   • source:      проектное решение — extension `AlarmDialogWidget` pattern с custom content (precedent `ConfirmDeleteDictionaryWidget`).
```

---

### Touch widgets (📌 без визуальных изменений)

#### 📌 `<𝗦𝗲𝘁𝘁𝗶𝗻𝗴𝘀𝗦𝗲𝗰𝘁𝗶𝗼𝗻𝗪𝗶𝗱𝗴𝗲𝘁>`

```
   • structure:
       column  spacing=4  padding=8
         section_slot: any-children
   • type:        M3 Surface (existing) — `settingstab/widgets/settings/SettingsSectionWidget.kt`
   • notes:
       ℹ️ Не меняется. Добавляется новый child (`ComponentsManageWidget`) в LazyColumn item.
       ℹ️ Принимает дополнительного ребёнка в первой секции SettingsTabScreen LazyColumn.
   • source:      existing (не Figma).
```

#### 📌 `<𝗟𝗮𝗻𝗴𝗠𝗮𝗻𝗮𝗴𝗲𝗪𝗶𝗱𝗴𝗲𝘁>`

```
   • notes:
       ℹ️ Не меняется. Используется как reference для `ComponentsManageWidget` (visual parity).
   • source:      existing.
```

#### 📌 `<𝗗𝗶𝗰𝘁𝗗𝗿𝗼𝗽𝗗𝗼𝘄𝗻𝗪𝗶𝗱𝗴𝗲𝘁>`

```
   • notes:
       ℹ️ Не меняется. Остаётся как второй элемент в `actions` slot `DictionaryAppBar` (после нового icon-button «молоток»).
   • source:      existing.
```

#### 📌 `<𝗔𝗽𝗽𝗕𝗮𝗿𝗧𝗶𝘁𝗹𝗲𝗪𝗶𝗱𝗴𝗲𝘁>`

```
   • notes:
       ℹ️ Не меняется. Остаётся title slot `DictionaryAppBar`.
   • source:      existing.
```

### Touch widgets (🔄 меняется в этой фиче — composite container'ы)

#### 🔄 `<𝗗𝗶𝗰𝘁𝗶𝗼𝗻𝗮𝗿𝘆𝗔𝗽𝗽𝗕𝗮𝗿>`

```
   • structure:
       scaffold/topbar
         title_slot: text  variant=AppBarTitleWidget  titleResId=input
         actions_slot:
           row  spacing=0
             ∀ state.isLoading: progress  variant=CircularProgressIndicator  size=24
             ∀ !state.isLoading:
               row  spacing=4
                 tools_icon_slot: button  variant=IconBoxed  iconRes=ic_hammer  visible=∀ state.currentDict != null  onClick=Msg.OpenPerDictionaryComponents
                 picker_slot: button  variant=DictDropDownWidget
   • type:        M3 TopAppBar (existing) — `dictionaryappbar/DictionaryAppBar.kt`
   • size:        width=fill × height=56 (TopAppBar default)
   • padding:     TopAppBar internal
   • colors:
       – background: M3 TopAppBar default (transparent / surface)
   • params:
       – titleResId: Int  (unchanged)
       – factory + navigator  (unchanged)
   • callbacks:
       – icon-button onClick → Msg.OpenPerDictionaryComponents(currentDict.id)  (NEW)
       – остальные действия — unchanged.
   • behavior:
       Изменение: добавлен новый icon-button перед `DictDropDownWidget` в `actions` slot;
       видим только при `currentDict != null && !isLoading`.
   • notes:
       ℹ️ Shared widget на 3 tab'ах (Vocabulary / Quiz / Statistic) — icon-button «молоток» появится
       автоматически на всех 3 табах через `CompositionRootImpl` host'ы (F043/F048).
       ℹ️ Принимает onClick для `Msg.OpenPerDictionaryComponents` из удалённого «нет entry-point в
       per-dict конструктор» pattern (миграция IS481: новый widget внутри existing TopAppBar).
   • source:      existing widget с расширением actions slot (ui_placement.md § Per-dictionary).
```

#### 🔄 `<𝗦𝗲𝘁𝘁𝗶𝗻𝗴𝘀𝗧𝗮𝗯𝗦𝗰𝗿𝗲𝗲𝗻>`

```
   • structure:
       scaffold
         topbar_slot: 📌 SettingsAppBar (existing)
         content_slot:
           lazy-column  contentPadding=h:16  spacing=8
             section_1_slot:
               column  variant=SettingsSectionWidget
                 lang_slot: 📌 LangManageWidget
                 components_slot: ❇️ ComponentsManageWidget   (NEW)
                 export_slot: 📌 ExportDataWidget
                 import_slot: 📌 ImportDataWidget
             section_2_slot:
               column  variant=SettingsSectionWidget
                 privacy_slot: 📌 PrivacyPolicyWidget
                 about_slot: 📌 AboutAppWidget
   • type:        M3 Scaffold + LazyColumn (existing)
   • notes:
       ℹ️ Изменение: добавлен `ComponentsManageWidget` в первую секцию.
       ℹ️ Принимает onClick → Msg.OpenComponentsManager из удалённого «нет entry-point в конструктор»
       pattern (миграция IS481).
   • source:      existing screen с расширением одной LazyColumn item (ui_placement.md § Глобальный конструктор).
```

---

## ❇️ НОВЫЕ ВИДЖЕТЫ

- `<UserDefinedRowWidget>` — row в `ComponentsManagerScreen` (per-row UI с edit/delete).
- `<PerDictRowWidget>` — row в `PerDictionaryComponentsScreen`.
- `<ComponentsEmptyStateWidget>` — empty-state для обоих экранов (variant via resIds).
- `<CreateComponentFab>` — FAB на обоих экранах.
- `<CreateComponentDialog>` — full-form (name + template + isMulti + scope).
- `<RenameComponentDialog>` — minimal (single field).
- `<DeleteComponentConfirmDialog>` — impact-preview + confirm.
- `<ComponentsManageWidget>` — settings entry-row.
- `<ComponentsToolsIconButton>` — «молоток» в `DictionaryAppBar.actions`.

## 🔧 МЕНЯЕМ (ключевое)

- `<DictionaryAppBar>` — добавлен icon-button «молоток» перед `DictDropDownWidget` в `actions` slot. Видим при `currentDict != null && !isLoading`.
- `<SettingsTabScreen>` — в первую `SettingsSectionWidget` добавлен `ComponentsManageWidget` (между `LangManageWidget` и `ExportDataWidget`).

## ❌ УДАЛЯЕМ (с миграцией)

В этой фиче существующих UI-виджетов не удаляется. Все изменения — additive (новые виджеты + расширение existing slot'ов).

Миграция с предыдущей итерации (M12 → M13 / новая фича):

- `(M12) ComponentType has no UI CRUD` → `<UserDefinedRowWidget> / <PerDictRowWidget> + edit/delete actions + Create/Rename/Delete dialogs` (новый UI слой над soft-delete механизмом, который существовал в DAO без UI триггера, см. `deletion_concept.md`).
- `(M12) ComponentTypeDao.softDelete orphan API` → принимающие виджеты: `<DeleteComponentConfirmDialog>.onConfirm` (`Msg.ConfirmDelete → softDeleteComponentType`) + chain через UseCase.
- `(M12) No entry-point в конструктор` → `<ComponentsManageWidget>` (Settings) + `<ComponentsToolsIconButton>` (DictionaryAppBar) — два независимых пути.

## 🖼 ИКОНКИ К ИМПОРТУ

- `ic_hammer.xml` — vector drawable «молоток / инструменты» для `<ComponentsToolsIconButton>` (DictionaryAppBar) и `<ComponentsManageWidget>` (Settings, либо отдельный `ic_components.xml`). Существующих icon'ов с такой семантикой в `core/core-resources/src/main/res/drawable/` нет (verified в walkthrough § DictionaryAppBar). TODO: импорт `ic_hammer` (проектное решение, без Figma source).
- `ic_components.xml` (optional alternative) — если хочется отличать settings-entry icon от toolbar-icon. TODO: импорт `ic_components`.

## 🆕 НОВЫЕ UX-СЦЕНАРИИ

- **Drill-in в global конструктор:** Settings tab → `ComponentsManageWidget` → `ComponentsManagerScreen` → list of user-defined types (across all dicts) → tap FAB → `CreateComponentDialog` → fill name/template/scope/isMulti → submit → snackbar success → list refresh.
- **Drill-in в per-dict конструктор:** any tab (Vocabulary/Quiz/Statistic) → DictionaryAppBar → «молоток» icon → `PerDictionaryComponentsScreen` → list components в текущем словаре (global + per-dict) → tap FAB → `CreateComponentDialog` (scope preselect=PerDictionaries(currentDict)) → submit.
- **Rename:** tap edit-icon в row → `RenameComponentDialog` → edit → submit → snackbar success либо NameError (`NameEmpty/SameScopeCollision/CrossScopeCollision`).
- **Soft-delete:** tap delete-icon → `DeleteComponentConfirmDialog` → preview loads → юзер видит `N values + K dicts + M quizConfigs + L prefs reset` → tap Delete → row исчезает + snackbar.
- **Empty state:** 0 user-defined в scope → `<ComponentsEmptyStateWidget>` с CTA → tap → open `CreateComponentDialog`.
- **Snackbar:** state-driven (`state.snackbarState`), `LaunchedEffect(snackbarState)` shows + `Msg.DismissSnackbar` clears.

## 🎨 ПАЛИТРА

Используются existing tokens из `modules/core/theme/src/main/java/me/apomazkin/theme/`:

- `MaterialTheme.colorScheme.surface` / `onSurface` — card/dialog backgrounds.
- `MaterialTheme.colorScheme.primary` / `onPrimary` — FAB, primary buttons.
- `MaterialTheme.colorScheme.error` — name validation error text.
- `me.apomazkin.theme.enableIconColor` (0xff252628) — icon tint enabled.
- `me.apomazkin.theme.unselectedGreyColor` (0xff95989D) — empty-state icon, next-icon (chevron).
- `me.apomazkin.theme.grayTextColor` (0xFF7B7E85) — body subtitle / hint / meta.
- `me.apomazkin.theme.dividerColor` (0xffE4E5E7) — section border (через `SettingsSectionWidget`).

Типографика:

- `LexemeStyle.H6` (20sp Medium) — dialog titles.
- `LexemeStyle.BodyL` (17sp Regular) — row name, dialog labels.
- `LexemeStyle.BodyLBold` (17sp Bold) — FAB label, button labels.
- `LexemeStyle.BodyS` — meta-info, hints, field-labels.
- `MaterialTheme.typography.labelSmall` (11sp Medium) — chip labels (template, cardinality, global badge).

Новые цветовые токены **не нужны** (использование existing palette + M3 colorScheme).

---

## Источники

- `docs/features/IS481_component_constructor/02_scope.md` — UI scope (затронутые файлы UI; entry-points).
- `docs/features/IS481_component_constructor/business_summary.md` — State / Msg / Reducer контракт обоих экранов (snapshot финального state для UI рендеринга).
- `docs/features/IS481_component_constructor/concept/ui_placement.md` — design source: где живут entry-points, что показывается per строка, операции CRUD, scope semantics.
- `docs/features/IS481_component_constructor/concept/typed_views.md` — typed view per template (`TextValues` MVP) для template-preview composables; per-template widget живёт в `modules/widget/component_widgets/`.
- `docs/features/IS481_component_constructor/concept/deletion_concept.md` — soft-delete механика (impact preview обязателен; recovery вне scope; `DeletionImpact` shape).
- `docs/features/IS481_component_constructor/ui_walkthrough.md` — existing patterns: `SettingsItemWidget` для row-pattern, `LexemeDialog`/`AlarmDialogWidget` для dialogs, `IconBoxed` для «молотка», `DictionaryListScreen` как ближайший shape-analog, state-driven snackbar precedent (`VocabularyTabScreen`), nav-chain уже подведён в infra.
- `modules/screen/components_manager/.../mate/State.kt` — `ComponentsManagerScreenState`, `UserDefinedRow`, `CreateDialogState`, `RenameDialogState`, `DeleteConfirmState`, `SnackbarState` shapes.
- `modules/screen/per_dictionary_components/.../mate/State.kt` — `PerDictionaryComponentsScreenState`, `PerDictRow` shapes.
- `docs/guides/ui-primitives.md` — словарь примитивов для `structure:` блоков.

**Figma:** отсутствует (`plan.context.feature_has_figma=false`). Все 🚨 в `notes:` — миграционные пометки (зеркала для § ❌ УДАЛЯЕМ), не отходы от Figma. Все custom-виджеты с маркером ❇️ имеют `source: проектное решение`.

---

## История ревью

### iter 1 (2026-06-17): 2 critical findings

- F148: устаревшие Msg names → renamed.
- F149: Row callback signatures → typeId only.

### iter 2 (2026-06-17): F148 + F149 fixed.

### iter 3 (2026-06-17): F150 + F151 fixed

- F150: CreateComponentDialog params aligned с structure (3 callback'а вместо 1).
- F151: PerDictRowWidget trailing_slot.enabled simplified to true.
