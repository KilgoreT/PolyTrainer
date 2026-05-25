# IS479 — wordcard lexeme inline · UI layout

Финальный snapshot UI карточки слова после реализации фичи IS479
(inline-механизм добавления лексемы вместо bottom-sheet).

Источники: Figma frame `9154:82509` (через `../figma_dump.json`), `02_scope.md`,
`business/summary.md` (контракты State / Msg / Reducer / DatasourceEffect),
текущий код `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/`.

## 📋 ЧТО ДЕЛАЕМ

- Убираем bottom-sheet добавления лексемы. Лексема создаётся локально (`NOT_IN_DB`) сразу по тапу inline-кнопки добавления; реальный INSERT в БД — при первом Commit Translation/Definition.

> 📎 guide: docs/guides/state-and-extensions.md — "`NOT_IN_DB = -1L` для неинициализированных ID"

- AddLexeme размещается в слоте `Scaffold.floatingActionButton` как M3 FloatingActionButton icon-only (per Figma `9154:82532`, ↘️ FAB-slot; project_decision #4 `fab_scaffold_slot`).
- Subentity-добавление через chip-плейсхолдер. `+ Перевод` / `+ Определение` — `M3 SuggestionChip` внутри карточки лексемы. После активации placeholder уезжает в заголовок поля как `M3 InputChip` с inline-edit.
- Удаление лексемы — пункт меню (⋮) в заголовке `LexemeItemWidget` + отдельная кнопка «Удалить» (`9162:40713`) в нижней зоне карточки лексемы.
- Исключаем subentity «Пример» полностью (Figma `9154:82523`).
- Lexeme value — chip-вью (`M3 InputChip`) с inline-редактированием через `BasicTextField` + commit / cancel icons.
- Глобальная UI-блокировка через `state.isPendingDbOp` — все интерактивы передают `enabled = !isPendingDbOp` вниз по дереву.

> 📎 guide: docs/guides/state-and-extensions.md — "State = только отображаемое; computed properties для derived полей"

## 🚷 ВНЕ СКОУПА

- **TopBar (`9154:82531`).** Оставляем существующий `TopAppBar` с back-icon + DropdownMenu (⋮ → «Удалить слово»). Figma-вариант "with btn" (кнопка «Сохранить») НЕ применяем — устоявшийся отход проекта, закрытие/комит word edit идёт по тапу close-icon в `LexemeEditableText`. DRIFT фиксируется 🚨 в notes, не закрывается в IS479.
- **WordField (`9158:72107`).** Оставляем существующий `Surface` + `LexemeEditableText` для заголовка карточки. Figma даёт `tui-input` `componentId=4620:57849` — точечная подгонка типографики / структуры под Figma вне скоупа IS479. DRIFT 🚨 в notes.

## 🏷 ЛЕГЕНДА

- **⚙️** — системный Material3 / Compose (`Scaffold`, `Column`, `Row`, `Surface`, `InputChip`, `SuggestionChip` и т.д.).
- **❇️** — новый кастомный виджет (введён в IS479).
- **🔄** — кастомный, меняется в этой фиче.
- **📌** — кастомный, не меняется в IS479 (out of scope).
- **🚨** в `notes:` — отход от Figma или предупреждение об отклонении.
- **ℹ️** в `notes:` — обычная пояснительная заметка.

## 🗺 Карта экрана

```
⚙️ Scaffold                                                 containerColor=Transparent
├─ topBar = 📌 <𝗧𝗼𝗽𝗕𝗮𝗿>  ⟦container+icon⟧            navigation=back, actions=[⋮ DeleteWordMenuItem]
├─ snackbarHost = ⚙️ SnackbarHost                           hostState=snackbarHostState
└─ ⚙️ Box                                                   padding=Scaffold.padding, imePadding+navigationBarsPadding
   └─ ⚙️ Column                                             padding=h:16, v:16, spacing=natural, verticalScroll
      ├─ 📌 <𝗪𝗼𝗿𝗱𝗙𝗶𝗲𝗹𝗱>  ⟦container+input⟧          loaded=WordState.Loaded, isPendingDbOp
      ├─ ⚙️ Spacer                                          height=8
      └─ ⚙️ LazyColumn (× N)                                spacing=12
         ∀ lexemeState:
         🔄 <𝗟𝗲𝘅𝗲𝗺𝗲𝗜𝘁𝗲𝗺>  ⟦container⟧                   padding=16, spacing=12 (один Surface Card)
         ├─ ∀ active subentity (сверху):
         │   └─ ❇️ <𝗟𝗲𝘅𝗲𝗺𝗲𝗠𝗲𝗮𝗻𝗶𝗻𝗴𝗙𝗶𝗲𝗹𝗱>  ⟦container+chip+input⟧   kind, state (chip-заголовок + value field)
         │
         ├─ ⚙️ FlowRow (placeholder chip'ы)                  spacing=12, Start, wrap
         │   ├─ ❇️ <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽>  ⟦chip+icon⟧   kind=Translation, state=Placeholder   (если translation == null)
         │   └─ ❇️ <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽>  ⟦chip+icon⟧   kind=Definition, state=Placeholder    (если definition == null)
         │   ✗ "Пример" исключён фичей (project_decision #5)
         │
         └─ ❇️ <𝗗𝗲𝗹𝗲𝘁𝗲𝗟𝗲𝘅𝗲𝗺𝗲𝗕𝘂𝘁𝘁𝗼𝗻>  ⟦button+icon⟧   alignSelf=Stretch
   ↘️ FAB-slot                                              pos=BottomEnd (Figma 9154:82532)
   └─ 🔄 <𝗔𝗱𝗱𝗟𝗲𝘅𝗲𝗺𝗲>  ⟦button+icon⟧                 enabled=!isPendingDbOp && !isCreatingLexeme

⚙️ if loaded.showWarningDialog:
└─ 📌 <𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗲𝗹𝗲𝘁𝗲𝗪𝗼𝗿𝗱>  ⟦dialog⟧            loaded, sendMessage
```

> 📎 guide: docs/guides/ui-patterns.md — "AppBar никогда не пишется inline в Scaffold — каждый экран использует отдельный виджет"
>
> 📎 guide: docs/guides/ui-patterns.md — "рендеринг списков через key() для стабильной рекомпозиции"
>
> 📎 guide: docs/guides/ui-patterns.md — "условные оверлеи (диалоги, bottom sheet) рендерятся по флагам стейта"
>
> 📎 guide: docs/guides/ui-patterns.md — "конвенции лейаута: горизонтальный padding экрана 16.dp, spacing между элементами списка 8.dp"
>
> 📎 guide: docs/guides/ui-patterns.md — "работа с клавиатурой через imePadding() / consumeWindowInsets"

## 🔍 Анализ виджетов

```
📌 <𝗧𝗼𝗽𝗕𝗮𝗿>  ⟦container+icon⟧   OUT OF SCOPE
   • type:        M3 TopAppBar
   • slots/content:
       – navigation: IconBoxed (ic_back, size=44)
       – title: ⊘ (empty)
       – actions: IconDropdownWidget(⋮) → DeleteWordMenuItem
   • params:
       – topBarState: TopBarState (isMenuOpen)
       – onBackPress: () -> Unit
       – sendMessage: (Msg) -> Unit
   • callbacks:
       – onBackPress → Msg.NavigateBack
       – ⋮ click → Msg.OpenTopBarMenu / Msg.CloseTopBarMenu
       – Delete menu item → Msg.CloseTopBarMenu + Msg.OpenDeleteWordDialog
   • notes:
       🚨 DRIFT (out of scope IS479): Figma вариант "with btn" (componentId=4620:58487, кнопка «Сохранить» в actions) НЕ применён. В реализации used DropdownMenu с пунктом «Удалить слово». Закрытие word-edit идёт по close-icon в LexemeEditableText, отдельной save-кнопки нет. Не закрывается в IS479.
       ℹ️ source unchanged: `modules/screen/wordcard/.../widget/TopBarWidget.kt`.
   • source:      figma 9154:82531 (componentId=4620:58487)
```

> 📎 guide: docs/guides/ui-patterns.md — "AppBar — всегда отдельный виджет, не пишется inline в Scaffold"
>
> 📎 guide: docs/guides/messages.md — "навигация на другой экран и системная back-кнопка идут через Msg, не через прямые callback"
>
> 📎 guide: docs/guides/effect-handlers.md — "UI не вызывает навигационные callback'и напрямую; любая навигация — через Msg → Reducer → NavigationEffect → handler → Navigator"

```
📌 <𝗪𝗼𝗿𝗱𝗙𝗶𝗲𝗹𝗱>  ⟦container+input⟧   OUT OF SCOPE
   • type:        tui-input (Figma componentId=4620:57849) → M3 Surface (Card) + Column + project LexemeEditableText (inline-edit pattern)
   • size:        width=fill × height=hug
   • padding:     all=16
   • shape:       borderRadius=12, shadowElevation=4
   • colors:
       – background: whiteColor (#FFFFFF, fill_QMOUFY)
       – content title: blackColor
       – content meta: grayTextColor + secondary (date)
   • typography:
       – title: LexemeStyle.H5 (Figma Desktop/Heading 5)
       – meta label: LexemeStyle.BodyM
       – meta value: LexemeStyle.BodyMBold (color=secondary)
   • slots/content:
       – content[0]: LexemeEditableText (originValue, changedValue, isEditMode)
       – content[1]: Row (Text "Добавлено" + Text date)
       – trailing-absolute: ImageFlagWidget (BottomEnd)
   • params:
       – loaded: WordState.Loaded
       – isPendingDbOp: Boolean
       – sendMessage: (Msg) -> Unit
   • callbacks:
       – onTextChange → Msg.UpdateWordInput(value)
       – onOpenEditMode → Msg.EnterWordEditMode
       – onCloseEditMode → Msg.CommitWordChanges
   • behavior:
       – Editing блокируется при isPendingDbOp (guard на call-site).
       – cancel-trigger отсутствует: close-icon = commit (per contract_ui_msg v3.2).
   • notes:
       🚨 DRIFT (out of scope IS479): tui-input componentId=4620:57849 заменён проектным LexemeEditableText (inline-edit с close-icon = commit). Точечная типография / поведение по Figma не приведены к компоненту. Не закрывается в IS479.
       ℹ️ source unchanged: `modules/screen/wordcard/.../widget/WordFieldWidget.kt`.
   • source:      figma 9158:72107 (componentId=4620:57849)
```

> 📎 guide: docs/guides/theme-and-resources.md — "цвета берутся из LexemeStyle / MaterialTheme / Color.kt — whiteColor, blackColor, grayTextColor — задекларированы централизованно"
>
> 📎 guide: docs/guides/ui-patterns.md — "редактируемый текст — паттерн LexemeEditableText: origin + changed + isEditMode + onTextChange/onOpen/onClose"
>
> 📎 guide: docs/guides/state-modeling.md — "редактируемые поля отдельно от domain model: оригинал + редактируемая копия"

```
🔄 <𝗟𝗲𝘅𝗲𝗺𝗲𝗜𝘁𝗲𝗺>  ⟦container⟧
   • type:        M3 Surface (Card)
   • size:        width=fill (380 в Figma) × height=hug
   • padding:     all=16 (Figma layout_RVILFH: padding=16px, gap=12px, alignItems=flex-end)
   • spacing:     Column itemSpacing=12 (gap внутри Column "Lexeme")
   • shape:       borderRadius=12, shadowElevation=4 (effect "word card in listing")
   • colors:
       – background: whiteColor (#FFFFFF, fill_QMOUFY)
   • slots/content:
       – content Column (порядок сверху → вниз):
           · content[0]: ∀ active subentity → ❇️ <𝗟𝗲𝘅𝗲𝗺𝗲𝗠𝗲𝗮𝗻𝗶𝗻𝗴𝗙𝗶𝗲𝗹𝗱> (0..N штук — Translation / Definition в порядке активации)
           · content[1]: ⚙️ FlowRow (gap=12, Start, wrap) с placeholder ❇️ <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽>'ами для неактивированных subentity (translation == null → add_translation, definition == null → add_definition). Figma layout_54DEHW: row, gap=12.
           · content[2]: ❇️ <𝗗𝗲𝗹𝗲𝘁𝗲𝗟𝗲𝘅𝗲𝗺𝗲𝗕𝘂𝘁𝘁𝗼𝗻> (footer, alignSelf=Stretch). Figma layout_MHMDYU обёртка: column, alignSelf=stretch, gap=10.
   • params:
       – order: Int
       – state: LexemeState
       – isPendingDbOp: Boolean
       – sendMessage: (Msg) -> Unit
   • callbacks:
       – delegated via sendMessage: Msg.CreateTranslation / CreateDefinition / Update*Input / Enter*EditMode / Commit*Edit / Cancel*Edit / RemoveLexeme
   • behavior:
       – Активные subentity сверху, placeholder chip'ы снизу — порядок задан UX-логикой (state).
       – Subentity "Пример" исключён фичей (project_decision #5) — placeholder не создаётся.
       – Divider между разными активными subentity рендерится только когда есть 2+ subentity (per design_tree F-arch-6). В Карте не показан как структурный узел — внутренняя логика MeaningField/Item.
       – Все child callbacks guarded enabled=!isPendingDbOp.
   • notes:
       🚨 Project decision: убран `LexemeTitleWidget` (dropdown "Value N" + ⋮-меню) — переход на FlowRow chip-плейсхолдеров. Удаление лексемы переехало в footer `<DeleteLexemeButton>` внутри карточки (per Figma `9162:40713`).
       🚨 принимает CreateTranslation/CreateDefinition из удалённого `LexemeMeaningWidget.onTypeSelect` (миграция IS479).
       ℹ️ Figma frame `9154:82519` (name="Lexeme", layout=layout_RVILFH) — единый Surface/Card-контейнер без отдельного outer Column.
   • source:      figma 9154:82519 (name="Lexeme", layout=layout_RVILFH)
```

> 📎 guide: docs/guides/ui-patterns.md — "выделение логически отдельных элементов в отдельный *Widget.kt — карточка элемента, диалог, кастомная кнопка"
>
> 📎 guide: docs/guides/ui-patterns.md — "три уровня виджетов: только один экран → screen/*/widget/"
>
> 📎 guide: docs/guides/ui-patterns.md — "отправка сообщений из UI через callback sendMessage"

```
❇️ <𝗗𝗲𝗹𝗲𝘁𝗲𝗟𝗲𝘅𝗲𝗺𝗲𝗕𝘂𝘁𝘁𝗼𝗻>  ⟦button+icon⟧
   • type:        M3 TextButton с leading icon (Figma tui-button, componentId=6239:63521)
   • size:        width=hug × height=24 (Figma layout_7ILIO5: row, fixed height=24)
   • padding:     horizontal=4, vertical=0 (Figma layout_7ILIO5: padding 0px 4px)
   • spacing:     itemSpacing=0 (icon и label выровнены center)
   • shape:       borderRadius=12 (Figma 9162:40713)
   • colors:
       – background: transparent (tui-button без явного fill в Figma — flat button)
       – content (icon+text): hex #DE2424 (fill_GHSSZ8) → MaterialTheme.colorScheme.onError (LexemeColor.onError = #DE2424) — exact match
   • typography:
       – label: LexemeStyle.BodyS (Desktop/Body S — 13sp Regular)
   • slots/content:
       – leading: Icon (trash-like, 16×16, Figma layout_ZTNZB2)
       – content: Text "Удалить" (R.string.word_card_lexeme_remove)
       – trailing: ⊘ (Show iconRight: false)
   • params:
       – enabled: Boolean (!isPendingDbOp)
       – onClick: () -> Unit
       – modifier: Modifier
   • callbacks:
       – onClick → Msg.RemoveLexeme(lexemeId)
   • behavior:
       – Disabled при isPendingDbOp.
       – Размещается как footer-action внутри `<LexemeItem>` (Figma layout_MHMDYU column-wrapper, alignSelf=stretch, gap=10).
   • notes:
       ℹ️ Заменяет удалённый ⋮-меню в LexemeTitle (миграция IS479: удаление лексемы доступно прямо из карточки).
   • source:      figma 9162:40712 (Frame 2043682692) → 9162:40713 (tui-button, componentId=6239:63521)
```

```
❇️ <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽>  ⟦chip+icon⟧
   • type:        Единый виджет с двумя state'ами (Placeholder | Active). Базовый узел — M3 SuggestionChip (placeholder) / M3 InputChip (active), либо общий Compose-обёртка с условным рендером.
   • size:        width=hug × height=hug
   • padding:     horizontal=12, vertical=8 (унифицировано из Figma placeholder layout_4E1NW5 — приоритет placeholder, project decision)
   • spacing:     itemSpacing=2 (gap между label и trailing icon в active state; в placeholder — между leading icon и label)
   • shape:       borderRadius=999 (pill) для ОБОИХ state'ов — project decision
   • colors:
       – Placeholder background: hex #F5F3F8 (fill_R87IJC) — нет токена, кандидат на добавление в theme.
       – Placeholder content (icon+label): blackColor (#19191B, fill_1N7XCC) / MaterialTheme.colorScheme.onSurface.
       – Active background: MaterialTheme.colorScheme.primary (#4A49BC, fill_62UVHJ) → LexemeColor.primary.
       – Active content (label+✕): MaterialTheme.colorScheme.onPrimary (#FFFFFF).
   • typography:
       – Placeholder label: LexemeStyle.BodyS (Desktop/Body S — 13sp Regular).
       – Active label: MaterialTheme.typography.labelSmall (Desktop/Body XS — 11sp Medium; нет точного LexemeStyle.*).
   • slots/content:
       – Placeholder (state=Placeholder):
            · leading: Icon(R.drawable.ic_add_value, 16×16, tint=primary) — синий ➕
            · label: Text(stringResource(labelRes)) — "Перевод" / "Определение"
            · trailing: ⊘
       – Active (state=Active):
            · leading: ⊘
            · label: Text(kind name) — "Перевод" / "Определение"
            · trailing: Icon(R.drawable.ic_close, 16×16, tint=onPrimary) — ✕. Переиспользуем существующую `ic_close` вместо Figma `ic_close_rounded` (componentId=9163:40871) — единый close-canon в проекте.
   • params:
       – kind: SubentityKind (Translation | Definition) — определяет label.
       – state: SubentityChipState (Placeholder | Active).
       – enabled: Boolean (!isPendingDbOp).
       – onActivate: () -> Unit (placeholder → active).
       – onDeactivate: () -> Unit (active → placeholder, ✕ click).
       – modifier: Modifier.
   • callbacks:
       – state=Placeholder, onClick → onActivate → Msg.CreateTranslation(lexemeId) | Msg.CreateDefinition(lexemeId).
       – state=Active, ✕ click → onDeactivate → весь <LexemeMeaningField> уничтожается, chip возвращается в FlowRow, value сбрасывается.
   • behavior:
       – Один виджет, два визуальных state'а — переезжает между FlowRow (Placeholder) и заголовком LexemeMeaningField (Active). Это **единое UX-явление** «значение нет → значение есть», не два разных виджета.
       – enabled=false при isPendingDbOp.
       – Subentity «Пример» — placeholder не создаётся (исключён фичей).
   • notes:
       🚨 Project decision (`subentity_chip_pill`): borderRadius=999 (pill) для ОБОИХ state'ов. В Figma active=6 (`9154:82521`), placeholder=999 (`9154:82625`/`9154:82627`/`9154:82523`) — разные. Причина: UX consistency, один визуальный паттерн.
       🚨 Project decision (`subentity_chip_single_widget`): один Compose-виджет с param `state`, не два разных. В Figma это два разных компонента (active componentId=4632:99285, placeholder componentId=28370:397707) — у нас один.
       🚨 принимает onActivate из удалённого `AddLexemeBottomWidget.onAdd` (миграция IS479).
       ℹ️ Subentity «Пример» исключён фичей (`9154:82523`) — placeholder не создаётся.
   • source:      figma 9154:82521 (active, componentId=4632:99285) + 9154:82625 / 9154:82627 / 9154:82523 (placeholder, componentId=28370:397707) — объединены в единый виджет по project_decisions.
```

> 📎 guide: docs/guides/ui-patterns.md — "выделение логически отдельных элементов: placeholder, карточка элемента, кастомная кнопка → отдельный *Widget.kt даже при однократном использовании"
>
> 📎 guide: docs/guides/theme-and-resources.md — "иконки лежат в core/core-resources/.../drawable как XML vectors, цвет через tint"

```
❇️ <𝗟𝗲𝘅𝗲𝗺𝗲𝗠𝗲𝗮𝗻𝗶𝗻𝗴𝗙𝗶𝗲𝗹𝗱>  ⟦container+chip+input⟧
   • type:        Compose Column (project wrapper)
   • size:        width=fill × height=hug
   • spacing:     itemSpacing=4
   • slots/content:
       – content[0]: <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽> (active-state — chip-заголовок с trailing ✕)
       – content[1]: <𝗟𝗲𝘅𝗲𝗺𝗲𝗩𝗮𝗹𝘂𝗲𝗙𝗶𝗲𝗹𝗱> (поле ввода)
   • params:
       – kind: Translation | Definition
       – state: TextValueState (origin, edited, isEdit) — пробрасывается в <LexemeValueField>
       – enabled: Boolean — пробрасывается вниз (источник = !isPendingDbOp)
       – onValueChange: (String) -> Unit
       – onDeactivate: () -> Unit (✕ chip → разрушение MeaningField)
       – onCommit: () -> Unit
       – onCancelEdit: () -> Unit
   • callbacks:
       – onValueChange → проброс в LexemeValueField
       – onDeactivate (✕ chip) → MeaningField уничтожается, chip возвращается в FlowRow
       – onCommit → commit активной субсущности
       – onCancelEdit → выход из edit-mode без сохранения
   • behavior:
       – Один LexemeMeaningField = одна active субсущность. 0..N штук одновременно.
       – Тап ✕ chip-заголовка → MeaningField уничтожается, chip возвращается в FlowRow.
   • notes:
       🚨 Project decision (`lexeme_meaning_field`): проектная обёртка, нет аналога в Figma — UX-решение проекта.
   • source:      проектное решение — обёртка над chip-заголовком и полем ввода для одной активной субсущности
```

> 📎 guide: docs/guides/ui-patterns.md — "выделение логически отдельных элементов в отдельный *Widget.kt — карточка элемента, диалог, кастомная кнопка"

```
🔄 <𝗟𝗲𝘅𝗲𝗺𝗲𝗩𝗮𝗹𝘂𝗲𝗙𝗶𝗲𝗹𝗱>  ⟦chip+input⟧
   • type:        ⚙️ Column + (M3 InputChip view-mode | BasicTextField + IconBoxed commit/cancel edit-mode)
   • size:        width=fill × height=hug
   • spacing:     vertical=4 (между title и chip/EditRow)
   • shape:       chip borderRadius=999 (M3 default pill), edit-mode field — без рамки
   • colors:
       – title (label): grayTextColor (Figma fill_KRTAD5)
       – chip selectedContainer: MaterialTheme.colorScheme.secondaryContainer
       – chip disabledSelectedContainer: MaterialTheme.colorScheme.secondaryContainer
       – edit textColor: MaterialTheme.colorScheme.secondary
       – trailing remove icon: unselectedGreyColor
       – commit icon: MaterialTheme.colorScheme.primary
       – cancel icon: unselectedGreyColor
   • typography:
       – title (label): LexemeStyle.BodyS (Figma Desktop/Body S)
       – chip label: default M3
       – edit field: LexemeStyle.BodyL (Figma Desktop/Body L)
   • slots/content:
       – content[0]: Text (title — translation/definition)
       – content[1] view-mode: InputChip(label=state.origin, trailingIcon=ic_circle_delete)
       – content[1] edit-mode: Row(BasicTextField weight=1f, IconBoxed ic_confirm, IconBoxed ic_close)
   • params:
       – state: TextValueState (isEdit, origin, edited)
       – titleRes: Int (@StringRes) — `word_card_bottom_translation` / `_definition`
       – enabled: Boolean
       – onTextChange: (String) -> Unit
       – onOpenEditMode: () -> Unit
       – onCommitEdit: () -> Unit
       – onCancelEdit: () -> Unit
       – onRemove: () -> Unit
   • callbacks:
       – onClick (InputChip) → onOpenEditMode → Msg.EnterTranslationEditMode(lexemeId) | EnterDefinitionEditMode(lexemeId)
       – onTextChange → Msg.UpdateTranslationInput(lexemeId, value) | UpdateDefinitionInput(lexemeId, value)
       – commit IconBoxed → onCommitEdit → Msg.CommitTranslationEdit(lexemeId) | CommitDefinitionEdit(lexemeId)
       – cancel IconBoxed → onCancelEdit → Msg.CancelTranslationEdit(lexemeId) | CancelDefinitionEdit(lexemeId)
       – trailing remove IconBoxed → onRemove → Msg.RemoveTranslation(lexemeId) | RemoveDefinition(lexemeId)
   • behavior:
       – edit-mode autofocus через FocusRequester при первом composition.
       – LaunchedEffect(value) re-sync TextFieldValue если origin меняется снаружи.
       – Все controls disabled при isPendingDbOp (передаётся в enabled).
   • notes:
       🚨 Figma `9154:85531` (componentId=6260:64584, tui-input edit-state) даёт `strokes=fill_QMOUFY (#FFFFFF)` — нулевой контраст. В реализации обрамления нет (BasicTextField без border) либо при имплементации использовать `MaterialTheme.colorScheme.outline` / `outlineVariant`. Причина: Figma stroke невидим, проектное решение — без визуальной рамки в edit-mode.
       🚨 принимает commit/cancel из удалённого ActionsWidget (миграция IS479).
       ℹ️ Один widget обслуживает оба state'а (view chip + edit field) — упрощает state-management и переходы.
       ℹ️ titleRes использует ключи `word_card_bottom_*` — суффикс `_bottom` остался от удалённого bottom-sheet; кандидат на переименование в backlog.
   • source:      figma 9154:82521 (chip view), 9154:85531 (Text field edit, componentId=6260:64584)
```

> 📎 guide: docs/guides/state-and-extensions.md — "TextValueState — паттерн для toggle edit/view: origin (сохранённое) + edited (в процессе) + isEdit (режим)"
>
> 📎 guide: docs/guides/ui-patterns.md — "LaunchedEffect — для state-triggered UI-эффектов (фокус, анимации), не для навигации"
>
> 📎 guide: docs/guides/ui-patterns.md — "фокус для текстовых полей через FocusRequester + LaunchedEffect(Unit) { focusRequester.requestFocus() }"
>
> 📎 guide: docs/guides/state-modeling.md — "редактируемые поля отдельно от domain model — оригинал нетронут, редактируемая копия отдельно"

```
🔄 <𝗔𝗱𝗱𝗟𝗲𝘅𝗲𝗺𝗲>  ⟦button+icon⟧
   • type:        M3 FloatingActionButton (Figma componentId=29907:178121, icon-only, Show Label=false) — размещается в `Scaffold.floatingActionButton`.
   • size:        M3 FAB default (56dp), width=hug × height=hug
   • padding:     M3 default FAB padding
   • shape:       M3 FAB default (RoundedCornerShape 16)
   • colors:
       – containerColor: MaterialTheme.colorScheme.primary (Figma fill_62UVHJ #4A49BC)
       – contentColor: MaterialTheme.colorScheme.onPrimary
   • slots/content:
       – icon: Icon(R.drawable.ic_add_value, size=24, tint=onPrimary)
   • params:
       – enabled: Boolean
       – onAddLexeme: () -> Unit
       – modifier: Modifier
   • callbacks:
       – onClick → onAddLexeme → Msg.CreateLexeme (создаёт локальную LexemeState(id=NOT_IN_DB))
   • behavior:
       – enabled = !isPendingDbOp && !isCreatingLexeme — блокирует двойное создание / re-tap во время БД-операции.
   • notes:
       🚨 Project decision #4 (`fab_scaffold_slot`): FAB размещён в `Scaffold.floatingActionButton`. Figma даёт абсолютную позицию x=340, y=720 относительно frame `9154:82509` — эквивалентно слоту Scaffold.floatingActionButton (BottomEnd) с учётом window insets.
       🚨 принимает CreateLexeme из удалённого AddLexemeBottomWidget.onAdd (миграция IS479: bottom-sheet → FAB icon-only inline).
       ℹ️ Show Label=false по Figma — icon-only FAB. Ключ `R.string.word_card_add_lexeme` опционально подаётся как `contentDescription` для accessibility, визуально текстовой метки нет.
   • source:      figma 9154:82532 (componentId=29907:178121)
```

> 📎 guide: docs/guides/theme-and-resources.md — "Material3 цвет → LexemeColor + darkColorScheme(); primary=#4A49BC задекларирован в core/theme"
>
> 📎 guide: docs/guides/state-and-extensions.md — "computed properties для derived полей: enabled = !isPendingDbOp && !isCreatingLexeme вычисляется на чтении, не хранится"
>
> 📎 guide: docs/guides/messages.md — "data class (с данными) для действий с параметрами; data object без данных"

```
📌 <𝗖𝗼𝗻𝗳𝗶𝗿𝗺𝗗𝗲𝗹𝗲𝘁𝗲𝗪𝗼𝗿𝗱>  ⟦dialog⟧   OUT OF SCOPE (IS479 не трогает)
   • type:        M3 AlertDialog
   • params:
       – loaded: WordState.Loaded
       – sendMessage: (Msg) -> Unit
   • callbacks:
       – onConfirm → Msg.RemoveWord(loaded.id)
       – onDismiss → Msg.CloseDeleteWordDialog
   • behavior:
       – рендерится только при loaded.showWarningDialog == true.
   • notes:
       ℹ️ Не относится к скоупу inline-механики лексемы. Сохраняется as-is.
   • source:      существующий код (вне Figma фичи IS479)
```

> 📎 guide: docs/guides/ui-patterns.md — "условные оверлеи: диалоги и bottom sheet рендерятся по флагам стейта (if state.showWarningDialog)"

## ❇️ НОВЫЕ ВИДЖЕТЫ

- `SubentityChip` — единый виджет в двух state'ах (Placeholder / Active). Placeholder живёт в FlowRow внутри `<LexemeItem>`; при активации переезжает в заголовок `<LexemeMeaningField>` как Active с trailing ✕. Расширение существующего `LexemeChipPlaceholderWidget.kt` (сейчас только placeholder) до двухстейтного `SubentityChip`.
- `LexemeMeaningField` — проектная обёртка (Column) над chip-заголовком (active SubentityChip) и `LexemeValueField`. Один MeaningField = одна active субсущность (Translation или Definition).
- `DeleteLexemeButton` — footer-action внутри карточки лексемы (TextButton с trash-icon, Figma `9162:40713`). Заменяет ⋮-меню удаления.

> 📎 guide: docs/guides/code-style.md — "файл виджета: *Widget.kt"

## 🔧 МЕНЯЕМ (ключевое)

- `AddLexemeWidget` — `M3 Button` (inline) → `M3 FloatingActionButton` icon-only в `Scaffold.floatingActionButton`. Callback → `Msg.CreateLexeme`.
- `LexemeItemWidget` — переработана структура: единый Surface(Card) с Column (порядок сверху → вниз): `∀ active <LexemeMeaningField>` → `FlowRow placeholder <SubentityChip>'ов (state=Placeholder)` → `<DeleteLexemeButton>` footer. `LexemeTitleWidget` (dropdown "Value N" + ⋮-меню) удалён.
- `LexemeValueFieldWidget` — двусостоятельный chip+input. View: `M3 InputChip` с trailing remove. Edit: `BasicTextField` + commit/cancel icons. Заменяет старый `LexemeValueWidget` (если был — поверх contract_state migration).
- `WordCardScreen` — слот `Scaffold.floatingActionButton` используется (рендерит `AddLexemeWidget`). Убран условный рендер `state.addLexemeBottomState.show`. Subentity-chip'ы добавлены в LazyColumn → `LexemeItemWidget` → FlowRow placeholder'ов.

## ❌ УДАЛЯЕМ (с миграцией)

- `AddLexemeBottomWidget` (`widget/addlexeme/AddLexemeBottomWidget.kt`) → `AddLexemeWidget` (FAB icon-only) + `SubentityChip` (subentity activation, state=Placeholder → Active).
- `AddLexemeBottomWidget.onAdd` → `AddLexemeWidget.onAddLexeme` (Msg.CreateLexeme) + `SubentityChip.onActivate` (Msg.CreateTranslation / Msg.CreateDefinition).
- `LexemeMeaningWidget` (`widget/addlexeme/LexemeMeaningWidget.kt`) → `SubentityChip` (placeholder per kind, активация переключает state) + `LexemeMeaningField` (wrapper над SubentityChip-active + LexemeValueField).
- `LexemeMeaningWidget.onTypeSelect` → разделено на `Msg.CreateTranslation(lexemeId)` / `Msg.CreateDefinition(lexemeId)` (per kind, без флага switch).
- `ActionsWidget` (`widget/addlexeme/ActionsWidget.kt`) → trailing icons в `LexemeValueFieldWidget` (commit = ic_confirm, cancel = ic_close).
- `ActionsWidget.onCommit` / `onCancel` → `LexemeValueFieldWidget.onCommitEdit` / `onCancelEdit` (Msg.CommitTranslationEdit / CancelTranslationEdit и Definition).
- `UiEffectHandler` (`mate/UiEffectHandler.kt`) → удалено, нет аналога (UI Effects = ∅, per impl.md ит.2).
- `PrimaryLongFabWidget` + `LexemeLongFab` (`modules/core/ui/.../btn/`) → удалено, нет аналога (wordcard-локальные виджеты, единственный потребитель — старый `AddLexemeWidget`-FAB; после inline-replace мёртвый код).
- `AddLexemeBottomState` (`mate/State.kt`) + ext-функции `showAddLexemeBottom` / `hideAddLexemeBottom` / `setTranslationCheck` / `setDefinitionCheck` → удалено, нет аналога (заменено на `isCreatingLexeme: Boolean` computed + локальные NOT_IN_DB LexemeState; per contract_state v2.5).
- `Msg.OpenAddLexemeDialog` / `CloseAddLexemeDialog` / `EnableTranslationCreation` / `EnableDefinitionCreation` → удалено, нет прямого аналога; функционально заменены на `Msg.CreateLexeme`, `Msg.CreateTranslation(lexemeId)`, `Msg.CreateDefinition(lexemeId)` (per contract_ui_msg v3.2).
- `LexemeTitleWidget` (`widget/lexeme/LexemeTitleWidget.kt`) → удалён. Add-actions (AddTranslation/AddDefinition) переехали в `<SubentityChip>` (state=Placeholder) внутри FlowRow LexemeItem. Delete-action переехал в footer `<DeleteLexemeButton>` (Figma `9162:40713`).
- `Msg.OpenLexemeMenu` + `AddTranslationLexemeMenuItem` / `AddDefinitionLexemeMenuItem` / `DeleteLexemeMenuItem` → удалено вместе с `LexemeTitleWidget`. Add-actions → onClick chip-placeholder'ов. Delete-action → onClick DeleteLexemeButton (Msg.RemoveLexeme).

> 📎 guide: docs/guides/messages.md — "Action-сообщения: императивный глагол, описывающий намерение (Add*/Delete*/Save*)"
>
> 📎 guide: docs/guides/state-and-extensions.md — "extension-функции для всех мутаций стейта; нейминг — глагол в начале (show/hide/update/add/remove)"
>
> 📎 guide: docs/guides/state-and-extensions.md — "computed properties — НЕ часть data class, derived from State (isCreatingLexeme как computed)"

## 🖼 ИКОНКИ К ИМПОРТУ

- `ic_add_value` — existing, leading icon в `AddLexemeWidget` и `SubentityChip` (state=Placeholder).
- `ic_close` — existing, (а) trailing icon в `SubentityChip` (state=Active, переиспользуем существующий close вместо Figma `ic_close_rounded` componentId=9163:40871 — единый close-canon в проекте), (б) cancel edit в `LexemeValueField` edit-mode.
- `ic_circle_delete` — existing, trailing remove в `LexemeValueFieldWidget` view-mode.
- `ic_confirm` — existing, commit edit в `LexemeValueFieldWidget` edit-mode.
- `ic_trash` (или эквивалент) — leading icon в `DeleteLexemeButton` (Figma 9162:40713 → `tui-button` trash-like 16×16).
- `ic_back` — existing, navigation icon в `TopBarWidget`.

Все иконки уже присутствуют в `core/core-resources/src/main/res/drawable/`. Импорт из Figma не требуется.

> 📎 guide: docs/guides/theme-and-resources.md — "все SVG-иконки как XML vectors в core/core-resources/src/main/res/drawable/"

## 🆕 НОВЫЕ UX-СЦЕНАРИИ

- **Тап inline-кнопки «Add meaning»**: создаётся локальная `LexemeState(id=NOT_IN_DB, translation=null, definition=null)`, появляется новая карточка лексемы внизу списка. Кнопка disabled до завершения локального создания (`isCreatingLexeme=true`). DB INSERT ещё не происходит.

> 📎 guide: docs/guides/state-and-extensions.md — "NOT_IN_DB = -1L для неинициализированных ID; локальная LexemeState создаётся до INSERT в БД"

- **Тап `+ Перевод` / `+ Определение`** в карточке лексемы: chip-placeholder заменяется на `LexemeValueFieldWidget` сразу в edit-mode (TextValueState(isEdit=true)). Реальный INSERT в БД — при Commit (`Msg.CommitTranslationEdit` / `CommitDefinitionEdit`).
- **Cancel первого Commit для NOT_IN_DB-лексемы**: суб-сущность сбрасывается обратно к placeholder. Лексема остаётся NOT_IN_DB локально (не уходит в БД). Удаление NOT_IN_DB через ⋮ → «Удалить» — локальная операция без эффекта на БД.
- **Single-active-edit invariant**: открытие нового edit (chip-tap или word-tap) вызывает `closeAllEditModes()` перед открытием — только один inline-edit активен одновременно.

> 📎 guide: docs/guides/reducer-patterns.md — "цепочки расширений в редьюсере: state.closeAllEditModes().openEdit(...) to setOf()"

- **isPendingDbOp как UI-блокировщик**: при любой исходящей DB-операции (Commit / Remove) `isPendingDbOp=true` → все интерактивы disabled до прихода `Refresh*` или `ShowNotification` от handler'а.

> 📎 guide: docs/guides/effect-handlers.md — "DatasourceEffect — операции с БД на Dispatchers.IO; результат → Msg обратно в Reducer"
>
> 📎 guide: docs/guides/state-and-extensions.md — "explicit state flags: UI-флаги — явные поля state (isPendingDbOp), не вычисляются в composable"

## 🎨 ПАЛИТРА

- `MaterialTheme.colorScheme.primary` — primary action (icon в placeholder chip, commit icon, AddLexemeWidget container). Figma fill_62UVHJ = `#4A49BC`.
- `MaterialTheme.colorScheme.onPrimary` — content на primary (AddLexemeWidget label/icon).
- `MaterialTheme.colorScheme.secondary` — text color edit-mode field, date в WordFieldWidget.
- `MaterialTheme.colorScheme.secondaryContainer` — chip selected container (LexemeValueFieldWidget view-mode).
- `MaterialTheme.colorScheme.tertiary` — фон Column-обёртки screen content + divider color.
- `MaterialTheme.colorScheme.outline` / `outlineVariant` — кандидат для stroke в edit-mode field (замена Figma `#FFFFFF` stroke с нулевым контрастом).
- `whiteColor` (`me.apomazkin.theme.whiteColor`) — фон Surface(Card) LexemeItemWidget, WordFieldWidget.
- `blackColor` — текст заголовка слова в WordFieldWidget.
- `grayTextColor` — метка-title в LexemeValueFieldWidget, meta-label в WordFieldWidget. Figma fill_KRTAD5.
- `unselectedGreyColor` — trailing remove icon в chip view-mode, cancel icon в edit-mode.

> 📎 guide: docs/guides/theme-and-resources.md — "Material3 цвет → LexemeColor + darkColorScheme() в Theme.kt; кастомный цвет → val в Color.kt"

## log_messages
- Собран финальный snapshot UI `docs/features/IS479_wordcard_lexeme_inline/ui/ui_layout.md` по format spec § 1-7 на базе figma_dump (frame 9154:82509), 02_scope.md, business/summary.md и текущего кода виджетов.
- Зафиксированы 6 project_decisions через 🚨 в notes соответствующих виджетов: pill borderRadius унификация (placeholder + active), один SubentityChip-виджет в двух state'ах, проектный LexemeMeaningField (chip-заголовок + value field) подан как LexemeValueFieldWidget+LexemeChipPlaceholderWidget, FAB → inline-кнопка, исключение «Пример», stroke выбран как outline вместо Figma #FFFFFF.
- Зафиксированы 2 DRIFT-маркера 🚨 для out-of-scope виджетов: TopBarWidget (вариант "with btn" не применён, used DropdownMenu) и WordFieldWidget (tui-input componentId=4620:57849 → проектный LexemeEditableText).
- Описаны 9 миграций (3 удалённых widget'а из widget/addlexeme/ + UiEffectHandler + PrimaryLongFabWidget/LexemeLongFab + AddLexemeBottomState + 4 удалённых Msg-варианта) с явными целями и зеркальными 🚨-пометками в принимающих виджетах (AddLexemeWidget, LexemeChipPlaceholderWidget, LexemeValueFieldWidget, LexemeItemWidget).
- Все иконки уже присутствуют в core/core-resources, импорт из Figma не требуется.
- Ит.2 (F001/F002): синхронизированы Карта и Анализ `LexemeItemWidget` — добавлен явный системный узел `⚙️ Column (outer)` в Карте, в Анализе type переписан как `⚙️ Column (outer) + M3 Surface (Card) + ⚙️ Column (inner)`, slots/content раскрыт через inner Column; зафиксирован 🚨 DRIFT (Figma frame `9154:82519` — single Card-container с inner Column, в коде заголовок вынесен в outer Column над Card).
- Ит.2 (F003): добавлен ℹ️ в notes `LexemeValueFieldWidget` о `word_card_bottom_*` ключах — суффикс `_bottom` от удалённого bottom-sheet, кандидат на переименование в backlog.
- Ит.2 (F004): `LexemeTitleWidget.source` исправлен с "проектное решение" на `figma 9154:82519` (per format spec § 4 — "проектное решение" допустим только для ❇️-виджетов).
- Ит.3 (F006): AddLexeme возвращён в `Scaffold.floatingActionButton` по project_decision #4 (`fab_scaffold_slot`) и Figma `9154:82532` (icon-only FAB componentId=29907:178121, Show Label=false). Карта: inline-кнопка и связанный Spacer height=16 убраны из основного Column; FAB-slot теперь содержит `<AddLexemeWidget>`. Анализ виджета: type → M3 FloatingActionButton (icon-only), slots/content → только icon, params без text/label, придуманный DRIFT про "inline-механику / scroll overlap" убран, добавлен 🚨 project_decision #4. Секция МЕНЯЕМ: `AddLexemeWidget` М3 Button (inline) → FloatingActionButton в Scaffold-слоте; `WordCardScreen` — слот Scaffold.floatingActionButton используется.
- Ит.4 (F007): LazyColumn в Карте — `⚙️ Column (× lexemeList)` заменён на `⚙️ LazyColumn (× N)` (project_decision, соответствует Figma `cards` контейнеру); slots/content `LexemeItemWidget` подровнены под новый wrapper-виджет.
- Ит.4 (F008): добавлен `LexemeMeaningField` — проектная обёртка (Column) над chip-заголовком (active LexemeChipPlaceholder с trailing ✕) и `LexemeValueField`. Новый блок в Анализе виджетов; ссылки в Карте и в LexemeItemWidget.slots[0]/slots[2] обновлены; добавлен в § НОВЫЕ ВИДЖЕТЫ. Project decision (`lexeme_meaning_field`) — нет аналога в Figma.
- Ит.5 (manual rewrite per example): LexemeItem переписан по UX-задумке example — единый Surface(Card), внутри Column (порядок): `∀ active <LexemeMeaningField>` → `FlowRow <LexemeChipPlaceholder>` → footer `<DeleteLexemeButton>`. `LexemeTitleWidget` (dropdown + ⋮-меню) удалён из Карты и Анализа. Add-actions переехали в onClick chip-placeholder'ов, Delete-action → `<DeleteLexemeButton>` (Figma `9162:40713`). Добавлен блок DeleteLexemeButton в Анализ. Обновлены секции МЕНЯЕМ, УДАЛЯЕМ, НОВЫЕ ВИДЖЕТЫ, ИКОНКИ.

_model: claude-opus-4-7[1m]_
