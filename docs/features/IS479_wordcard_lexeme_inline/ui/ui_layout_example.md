# IS479 — wordcard lexeme inline · UI cheat-sheet

═══════════════════════════════════════════════════

## 📋 ЧТО ДЕЛАЕМ

- Убираем bottom-sheet добавления лексемы.
- Inline-механизм добавления — по Figma `9154-82519`.
- Все кнопки + chip "Перевод" — chip-стиль (`9154-82521`, `9154-82625`).
- Add Lexeme — FAB icon-only в правом нижнем углу (`9154-82532`).
- "Пример" (Example) — исключаем полностью.

## 🚷 ВНЕ СКОУПА (не трогаем, остаётся как есть)

- **`<TopBar>`** — остаётся в текущем виде (DropdownMenu с пунктом «Удалить»). Figma вариант `with btn` («Сохранить») не применяем — out of scope этой фичи.
- **`<WordField>`** — остаётся в текущем виде (Surface + LexemeEditableText). Точечная подгонка типографики под Figma — out of scope этой фичи.

Эти виджеты помечены маркером 📌 в Карте и в Анализе. Любые отходы от Figma по ним фиксируются 🚨 в notes как DRIFT, но НЕ закрываются в IS479.

## 🏷 ЛЕГЕНДА

- ⚙️ системный Material3/Compose
- ❇️ новый кастомный
- 🔄 кастомный, меняется
- 📌 кастомный, без изменений

## 🗺 Карта экрана

> Источник: Figma frame `9154:82509`. Только виджеты + системные контейнеры (Scaffold / Column / Row слотов), без внутренностей виджетов. Раскрытие каждого виджета — ниже в § «Детали виджетов».

```
⚙️ Scaffold
├─ 📌 <𝗧𝗼𝗽𝗕𝗮𝗿>                                  out of scope
└─ ⚙️ Column (screen_content)                   padding=h:16  spacing=12
   ├─ 📌 <𝗪𝗼𝗿𝗱𝗙𝗶𝗲𝗹𝗱>                            out of scope
   │
   └─ ⚙️ LazyColumn (× N)                       spacing=12
      🔄 <𝗟𝗲𝘅𝗲𝗺𝗲𝗜𝘁𝗲𝗺>                          padding=16  spacing=12
      ├─ ⚙️ ∀ active chip (сверху):
      │   ❇️ <𝗟𝗲𝘅𝗲𝗺𝗲𝗠𝗲𝗮𝗻𝗶𝗻𝗴𝗙𝗶𝗲𝗹𝗱>             (chip-заголовок + value field)
      │
      ├─ ⚙️ FlowRow (placeholder chip'ы)        spacing=12  Start  wrap
      │   ├─ ❇️ <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽> "Перевод"      (если ещё не активирован)
      │   ├─ ❇️ <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽> "Определение"   (если ещё не активирован)
      │   └─ ❇️ <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽> "Пример"        ← по фиче исключается
      │
      └─ ❇️ <𝗗𝗲𝗹𝗲𝘁𝗲𝗟𝗲𝘅𝗲𝗺𝗲𝗕𝘂𝘁𝘁𝗼𝗻>             alignSelf=Stretch
   ↘️ FAB-slot                                  pos=BottomEnd
   └─ 🔄 <𝗔𝗱𝗱𝗟𝗲𝘅𝗲𝗺𝗲>
```

> Размеры на этом уровне — только **родительский контейнер** (padding/spacing/alignment). Размеры внутри виджетов — в § Детали.
> Источник: Figma frame `9154:82509`. Спорные значения помечены `?` — уточнить при первом implement-проходе.

---

## 🔍 Анализ виджетов

```
📌 <𝗧𝗼𝗽𝗕𝗮𝗿>  ⟦container+icon⟧  — OUT OF SCOPE IS479
   • type: M3 TopAppBar (CenterAligned не задан — стандартный TopAppBar)
   • size: width=412 (fixed) × height=hug (Auto Layout row)
   • padding: horizontal=4, vertical=10
   • spacing: justifyContent=space-between (navigation ↔ actions)
   • shape: без borderRadius (плоский)
   • colors:
       – background: token MaterialTheme.colorScheme.background (hex #FFFFFF, fill_QMOUFY)
   • slots/content:
       – navigation: IconBoxed(ic_back, size=44, colorEnabled=enableIconColor)
       – title: пусто (Figma title screen — пусто)
       – actions: IconDropdownWidget → ⋮ (more horizontal) + DropdownMenu c DeleteWordMenuItem
   • params (composable API):
       – topBarState: TopBarState (isMenuOpen)
       – onBackPress: () -> Unit
       – sendMessage: (Msg) -> Unit
   • callbacks:
       – onBackPress
       – Msg.OpenTopBarMenu / Msg.CloseTopBarMenu / Msg.OpenDeleteWordDialog
   • behavior:
       – Тап на ⋮ → открыть dropdown с пунктом "Удалить"
       – Тап "Удалить" → закрыть menu + открыть ConfirmDeleteWordDialog
   • notes:
       📌 Без изменений в зоне ответственности фичи (TopBarWidget уже существует).
   • source: figma 9154:82531 (componentId=4620:58487, IMAGE-SVG — Figma не раскрывает внутренности)

📌 <𝗪𝗼𝗿𝗱𝗙𝗶𝗲𝗹𝗱>  ⟦container+text+text+image⟧  — OUT OF SCOPE IS479
   • type: M3 Surface (Card) + Box + Column + Row
   • size: width=fill (380 в Figma) × height=hug
   • padding: all=16 (Figma layout_LEN7RP: padding 2px 0 16px on column; внешний контейнер 16dp по обоим осям)
   • spacing: between rows внутри Column — Arrangement.spacedBy(4dp) для subtitle row
   • shape: borderRadius=12dp, shadowElevation=4dp (Figma effect "word card in listing" — boxShadow 0px 2px 4px rgba(74,73,188,0.08))
   • colors:
       – background: token whiteColor / MaterialTheme.colorScheme.surface (hex #FFFFFF, fill_QMOUFY)
       – content title: blackColor (hex #19191B, fill_1N7XCC) [Figma], в коде — blackColor
       – content subtitle "Добавлено": grayTextColor (hex #7B7E85, fill_KRTAD5)
       – content date: MaterialTheme.colorScheme.secondary (hex #19191B)
   • typography:
       – title: LexemeStyle.H5 (Desktop/Heading 5 — 24sp, Medium, lineHeight=28)
       – subtitle label: LexemeStyle.BodyM (Desktop/Body M — 15sp Regular в Figma; в коде используется BodyM, lineHeight=24)
       – subtitle date: LexemeStyle.BodyMBold (project bold вариант BodyM)
   • slots/content:
       – content: LexemeEditableText (origin/edited/isEdit) для inline-редактирования слова "apple"
       – subtitle Row: Text "Добавлено" + Text "12 июня 2024"
       – trailing (BottomEnd): ImageFlagWidget (Figma 4108:35935, "United Kingdom..." flag)
   • params (composable API):
       – loaded: WordState.Loaded (id, added, value, edited, isEditMode)
       – isPendingDbOp: Boolean
       – sendMessage: (Msg) -> Unit
   • callbacks:
       – Msg.UpdateWordInput / Msg.EnterWordEditMode / Msg.CommitWordChanges
   • behavior:
       – Tap title → enter edit-mode (LexemeEditableText)
       – close icon в edit-mode = save (commit) — cancel-trigger в UI отсутствует
       – Edit-режим блокируется при isPendingDbOp
   • notes:
       📌 Виджет уже существует, в фиче — точечная подгонка типографики/паддингов под Figma.
       ⚠ Внутренний tui-input (Figma componentId=4620:57849) в реализации заменён на LexemeEditableText
         (project-специфичный inline-edit pattern) — упрощение, не дублируем M3 OutlinedTextField там, где не нужно.
   • source: figma 9158:72107 (name="word header", layout=layout_LEN7RP)

🔄 <𝗟𝗲𝘅𝗲𝗺𝗲𝗜𝘁𝗲𝗺>  ⟦container⟧
   • type: M3 Surface (Card)
   • size: width=fill (380 в Figma) × height=hug
   • padding: all=16 (Figma layout_RVILFH: padding=16px, gap=12px, alignItems=flex-end)
   • spacing: Column itemSpacing=12 (gap внутри Column "Lexeme")
   • shape: borderRadius=12dp, shadowElevation=4dp (effect "word card in listing")
   • colors:
       – background: whiteColor (hex #FFFFFF, fill_QMOUFY)
   • slots/content:
       – content Column (порядок сверху → вниз):
            1. ∀ active subentity → <𝗟𝗲𝘅𝗲𝗺𝗲𝗠𝗲𝗮𝗻𝗶𝗻𝗴𝗙𝗶𝗲𝗹𝗱>
            2. FlowRow (gap=12, Start, wrap) с placeholder <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽>'ами
               (Figma layout_54DEHW: row, gap=12)
            3. <𝗗𝗲𝗹𝗲𝘁𝗲𝗟𝗲𝘅𝗲𝗺𝗲𝗕𝘂𝘁𝘁𝗼𝗻> (Figma layout_MHMDYU обёртка: column, alignSelf=stretch, gap=10)
       – TextButton "Сохранить" (Figma id=9158:72057 → "Сохранить" 9158:72039, Desktop/Body S, fill_9PL09E=#B0B2B6) —
         показывается только в edit-mode конкретного MeaningField'а (commit trigger)
   • params (composable API):
       – order: Int
       – state: LexemeState
       – isPendingDbOp: Boolean
       – sendMessage: (Msg) -> Unit
   • behavior:
       – Активные subentity сверху, placeholder chip'ы снизу — порядок задан UX-логикой (state).
       – Divider между разными активными subentity рендерится только когда есть 2+ subentity (per F-arch-6).
   • notes:
       📌 Объединяет существующие LexemeTitleWidget+LexemeValueFieldWidget; в фиче меняем структуру:
          убираем LexemeTitleWidget (dropdown "Value N") → переход на FlowRow chip-плейсхолдеров.
       ⚠ В Figma внутри Lexeme есть отдельный "Frame 2043682685" с title-row ("Перевод" chip + "Сохранить") и
          текущим edit-полем. В реализации title-row растворяется в FlowRow/MeaningField (см. SubentityChip).
   • source: figma 9154:82519 (name="Lexeme", layout=layout_RVILFH)

❇️ <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽>  ⟦chip+icon⟧
   • type: M3 SuggestionChip / InputChip (зависит от state)
   • size: width=hug × height=hug
   • padding:
       – placeholder (Figma 9154:82627 layout_4E1NW5): horizontal=12, vertical=8
       – active (Figma 9154:82521 layout_52S1OH): padding 2px 2px 2px 6px, gap=2
       (унаследовано: реализация использует одни padding'и для обоих state'ов — выбрать ближе к placeholder)
   • spacing: itemSpacing=2 (gap между label и trailing icon)
   • shape:
       – placeholder: borderRadius=999dp (pill, Figma 9154:82625/82523/82627)
       – active: borderRadius=6dp (Figma 9154:82521)
       (унаследовано: проектное решение оставить 999dp pill в обоих state'ах для визуальной консистентности — pending design review)
   • colors:
       – placeholder background: hex #F5F3F8 (fill_R87IJC) — нет токена, кандидат на добавление (палитра уже зафиксировала как "chip placeholder bg")
       – active background: MaterialTheme.colorScheme.primary (hex #4A49BC, fill_62UVHJ) → LexemeColor.primary
       – placeholder content (text+icon): blackColor (hex #19191B, fill_1N7XCC)
       – active content (text+icon): whiteColor (hex #FFFFFF, fill_QMOUFY) / MaterialTheme.colorScheme.onPrimary
   • typography:
       – placeholder label: LexemeStyle.BodyS (Desktop/Body S — 13sp Regular)
       – active label: MaterialTheme.typography.labelSmall (Desktop/Body XS — 11sp Medium, нет точного LexemeStyle.* токена)
   • slots/content:
       – leading: пусто (Figma "Show Leading Icon: false" для placeholder, true для active — в реализации унифицируем)
       – content: Text label ("Перевод" / "Определение" / "Пример")
       – trailing: Icon 16×16 (Figma layout_LP0NHO)
            • state=Placeholder → ic_add_value (➕) — токен LexemeColor.primary (синий ➕)
            • state=Active      → ic_close_rounded (✕) — Figma componentId=9163:40871, 16×16
   • params (composable API):
       – kind: Translation | Definition | Example
       – state: Placeholder | Active
       – enabled: Boolean
       – onActivate / onDeactivate: () -> Unit
   • callbacks: onActivate (placeholder→active), onDeactivate (active→placeholder)
   • behavior:
       – placeholder тап → активировать chip → переезжает в заголовок <LexemeMeaningField>, исчезает из FlowRow
       – active тап на ✕ → деактивировать → возвращается в FlowRow, <LexemeMeaningField> уничтожается, значение сбрасывается
       – enabled=false при isPendingDbOp
   • notes:
       📌 Один виджет в двух state'ах. В placeholder-state живёт в FlowRow.
          В active-state переезжает наверх как заголовок <LexemeMeaningField>.
       ⚠ Отход от Figma: в Figma title-chip (9154:82521, borderRadius=6px, brand fill) и placeholder
          (9154:82625/82627, borderRadius=999px, grey fill) — разные компоненты.
          По UX-намерению — это один виджет, перемещающийся между двумя слотами; форму/цвет
          переключаем по state. Размеры и токены — унаследованы из ближайших Figma-аналогов.
   • source: проектное решение — объединение figma 9154:82521 (active/title) + 9154:82625/82627 (placeholder, group) + 9154:82523 (placeholder, standalone)

❇️ <𝗟𝗲𝘅𝗲𝗺𝗲𝗠𝗲𝗮𝗻𝗶𝗻𝗴𝗙𝗶𝗲𝗹𝗱>  ⟦container+chip+input⟧
   • type: Compose Column (не M3-base — обёртка)
   • size: width=fill × height=hug
   • padding: horizontal=0, vertical=0 (внешний padding задаёт <LexemeItemWidget>)
   • spacing: itemSpacing=4 (Figma layout_5TPG0B: gap=4 для column tui-input)
   • shape: без shape (обёртка)
   • colors:
       – background: transparent (наследует от LexemeItem)
   • slots/content:
       – content[0]: <𝗦𝘂𝗯𝗲𝗻𝘁𝗶𝘁𝘆𝗖𝗵𝗶𝗽> (state=Active) — заголовок, тот же chip из FlowRow
       – content[1]: <𝗟𝗲𝘅𝗲𝗺𝗲𝗩𝗮𝗹𝘂𝗲𝗙𝗶𝗲𝗹𝗱> — поле значения
   • params (composable API):
       – kind: Translation | Definition | Example
       – value: String
       – isEditMode: Boolean (явный флаг, не computed — per memory feedback_explicit_state_flags)
       – onValueChange: (String) -> Unit
       – onDeactivate: () -> Unit
       – onCommit / onCancelEdit: () -> Unit
   • callbacks:
       – onValueChange (typing)
       – onDeactivate (✕ на chip-заголовке)
       – onCommit / onCancelEdit
   • behavior:
       – Один <LexemeMeaningField> = одна active суб-сущность.
       – Может быть 0..N штук одновременно в порядке активации.
       – Тап на ✕ chip-заголовка → весь <LexemeMeaningField> уничтожается, chip возвращается в FlowRow, value сбрасывается.
   • notes:
       📌 Новый виджет — обёртка над chip-заголовком и полем ввода (Auto Layout column-аналог
          Figma "Frame 2043682685", только без правой кнопки "Сохранить" — её роль перенесена в footer LexemeItem).
       ⚠ Нет аналога в Figma — UX-решение проекта. Базовые размеры/spacing унаследованы из
          Figma layout_5TPG0B (column gap=4) и layout_IM161Z (column родителя).
   • source: проектное решение, нет аналога в Figma (унаследовано из layout_5TPG0B / layout_IM161Z)

🔄 <𝗟𝗲𝘅𝗲𝗺𝗲𝗩𝗮𝗹𝘂𝗲𝗙𝗶𝗲𝗹𝗱>  ⟦input⟧
   • type: M3 OutlinedTextField (Figma tui-input, componentId=6260:64584)
   • size: width=fill × height=hug (внутренний Field — layout_TVVZF7: row, fill, gap=8)
   • padding: horizontal=8 (Figma layout_TVVZF7: gap=8 между field и trailing-area; на FieldContent — vertical=6, layout_7EH74I)
   • spacing: itemSpacing=8
   • shape: borderRadius=12dp, strokeWeight=1px (Figma I9154:85531;6260:64585;29896:193284)
   • colors:
       – background: whiteColor (hex #FFFFFF, fill_QMOUFY) / MaterialTheme.colorScheme.surface
       – border (stroke): whiteColor (hex #FFFFFF, fill_QMOUFY) — нулевой контраст в Figma (полупрозрачный outline?),
         в реализации заменить на MaterialTheme.colorScheme.outline / outlineVariant — нет токена для exact white-on-white
       – content (text): MaterialTheme.colorScheme.secondary (hex #19191B)
       – label (placeholder): grayTextColor (hex #7B7E85, fill_KRTAD5)
   • typography:
       – label: LexemeStyle.BodyL (Desktop/Body L — 17sp Regular, lineHeight=28)
       – value: LexemeStyle.BodyL (тот же стиль для введённого текста)
   • slots/content:
       – content: Field (Auto Layout row) → FieldContent (column, justifyContent=center, padding 6px vertical) →
         FieldLabel ("Введите перевод" / "Введите определение") + (при non-empty) FieldValue
   • params (composable API):
       – state: TextValueState (origin, edited, isEdit)
       – titleRes: @StringRes
       – enabled: Boolean
       – onTextChange / onCommitEdit / onCancelEdit: (String) -> Unit / () -> Unit
   • callbacks: onTextChange, onCommitEdit, onCancelEdit
   • behavior:
       – Без изменений в зоне ответственности — простой OutlinedTextField для значения.
       – Используется внутри <LexemeMeaningField>.
   • notes:
       ⚠ Trailing clear-icon (`square-rounded-x-filled` 9154:86182) — из UX-сценариев,
         в базовом state'е Figma componentProperty "Clear: false".
   • source: figma 9154:85531 (tui-input, componentId=6260:64584, layout=layout_1Q8R2L → layout_TVVZF7)

❇️ <𝗗𝗲𝗹𝗲𝘁𝗲𝗟𝗲𝘅𝗲𝗺𝗲𝗕𝘂𝘁𝘁𝗼𝗻>  ⟦button+icon⟧
   • type: M3 OutlinedButton / TextButton с leading icon (Figma tui-button, componentId=6239:63521)
   • size: width=hug × height=24dp (Figma layout_7ILIO5: row, fixed height=24)
   • padding: horizontal=4, vertical=0 (Figma layout_7ILIO5: padding 0px 4px)
   • spacing: itemSpacing=0 (icon и label выровнены center)
   • shape: borderRadius=12dp (Figma 9162:40713)
   • colors:
       – background: transparent (tui-button без явного fill в Figma — flat button)
       – content (icon+text): hex #DE2424 (fill_GHSSZ8) → MaterialTheme.colorScheme.onError (LexemeColor.onError = #DE2424) — exact match
   • typography:
       – label: LexemeStyle.BodyS (Desktop/Body S — 13sp Regular)
   • slots/content:
       – leading: Icon (trash-like, размер задан Show icon: true — Figma layout_ZTNZB2: 16×16 fixed)
       – content: Text "Удалить"
       – trailing: пусто (Show iconRight: false)
   • params (composable API):
       – enabled: Boolean
       – onClick: () -> Unit
   • callbacks: onClick → Msg.RemoveLexeme(lexemeId)
   • behavior:
       – Disabled при isPendingDbOp.
   • notes:
       📌 Используется как footer-action внутри <LexemeItemWidget> (Figma layout_MHMDYU column-wrapper).
   • source: figma 9162:40712 (Frame 2043682692) → 9162:40713 (tui-button, componentId=6239:63521)

🔄 <𝗔𝗱𝗱𝗟𝗲𝘅𝗲𝗺𝗲>  ⟦button+icon⟧
   • type: M3 FloatingActionButton (Figma FAB, componentId=29907:178121)
   • size: width=hug × height=hug (Figma layout_453IT6: row, hug × hug; абсолютная позиция x=340, y=720)
   • padding: 0 (без явного padding на FAB — внутренности icon-only)
   • spacing: itemSpacing=0
   • shape: borderRadius=12dp (Figma 9154:82532)
   • colors:
       – background: MaterialTheme.colorScheme.primary (hex #4A49BC, fill_62UVHJ) → LexemeColor.primary — exact match
       – content (icon): MaterialTheme.colorScheme.onPrimary (whiteColor, унаследовано из FAB defaults)
   • slots/content:
       – content: icon-only (Show icon: true)
       – label: скрыт (Show Label: false) — текст "Добавить значение" присутствует в componentProperties, но не рендерится
   • params (composable API):
       – enabled: Boolean (!isPendingDbOp && !isCreatingLexeme)
       – onAddLexeme: () -> Unit
   • callbacks: onAddLexeme → Msg.CreateLexeme (создание локальной NOT_IN_DB-лексемы)
   • behavior:
       – Disabled блокирует двойное создание во время БД-операции.
       – Помещается в Scaffold floatingActionButton слот (в Figma — absolute x=340, y=720 от screen_content,
         в реализации — стандартный FAB-slot Scaffold с EndBottom alignment).
   • notes:
       📌 Текущая реализация (AddLexemeWidget.kt) — M3 Button с label; в фиче меняется на FAB icon-only.
       ⚠ Figma позиционирует FAB как абсолют в screen_content (x=340, y=720); в реализации —
         через Scaffold floatingActionButton slot, что эквивалентно по UX.
   • source: figma 9154:82532 (name="FAB", componentId=29907:178121, layout=layout_453IT6)
```

---

## ❇️ НОВЫЕ ВИДЖЕТЫ

- **LexemeChipPlaceholderWidget** — pill chip "+ Перевод" / "+ Определение". Figma `9154-82521`.

## 🔧 МЕНЯЕМ (ключевое)

- **AddLexemeWidget** — Button с label → FAB icon-only.
- **LexemeValueFieldWidget** (view) — `secondaryContainer` 32dp → `primary` brand-blue 20dp 11sp Medium.
- **LexemeValueFieldWidget** (edit) — `BasicTextField` без рамки → M3 `OutlinedTextField` 56dp с label-внутри.
- **LexemeTitleWidget** — dropdown "Value N" → chip "Перевод" + trash/pencil icon.
- **ConfirmDeleteWordWidget**, **LexemeItemWidget**, **DeleteWordMenuItem** — точечная подгонка под Figma frames.

Полный список + параметры (размеры/цвета/типографика) — `ui_layout.md` (agent-doc).

## ❌ УДАЛЯЕМ

- AddLexemeBottomWidget (bottom-sheet)
- LexemeMeaningWidget (переключатели Перевод/Definition)
- ActionsWidget (кнопки Добавить/Отмена внутри bottom-sheet)

## 🖼 ИКОНКИ К ИМПОРТУ (7)

`ic_close_rounded`, `ic_pencil_16`, `ic_chevron_down_16`, `ic_chevron_up_16`, `ic_arrow_back_up_16`, `ic_square_rounded_x_filled_16`, `ic_trash_16`.

Импорт — `download_figma_images` batch'ом на следующем прогоне (Figma API quota исчерпана).

## 🆕 НОВЫЕ UX-СЦЕНАРИИ

- Snackbar "Сохранено" после Commit (`9154-86353`).
- "Добавить ещё ▾/▴" collapse-row при множественных значениях.
- Inline clear-icon (`square-rounded-x-filled`) в TextField (`9154-86182`).
- Alternate edit-flow с `arrow-back-up` save (`9154-86499`).
- pencil-edit для saved-режима chip.

## 🎨 ПАЛИТРА

- chip primary: brand-blue (token TBD после design-system аудита).
- chip placeholder bg: `#F5F3F8`.
- snackbar: `#19191B` (secondary) / text `#FFFFFF`.

═══════════════════════════════════════════════════

🤖 Полная разметка для имплементации → `ui_layout.md` (agent-doc, ~388 строк).
