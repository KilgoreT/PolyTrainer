# Спека формата документа UI Layout

Канон формы UI Layout документа. Описывает структуру, маркеры, шаблон буллетов и договорённости.

Спека описывает только форму документа. Где он живёт, кем создаётся и когда применяется — решают использующие её процессы.

---

## 1. Структура секций (порядок — закон)

1. `# <FEATURE> — <название> · UI cheat-sheet` — заголовок.
2. `## 📋 ЧТО ДЕЛАЕМ` — bullet-список того что меняется в фиче.
3. `## 🏷 ЛЕГЕНДА` — маркеры (см. § 2).
4. `## 🗺 Карта экрана` — карта верхнего уровня для review (см. § 3).
5. `## 🔍 Анализ виджетов` — детальная разметка per-widget для имплементации (см. § 4).
6. `## ❇️ НОВЫЕ ВИДЖЕТЫ` — bullets имён новых виджетов.
7. `## 🔧 МЕНЯЕМ (ключевое)` — bullets с кратким описанием правки.
8. `## ❌ УДАЛЯЕМ (с миграцией)` — bullets с миграцией (см. § 5).
9. `## 🖼 ИКОНКИ К ИМПОРТУ` — bullets имён иконок.
10. `## 🆕 НОВЫЕ UX-СЦЕНАРИИ` — bullets опционально.
11. `## 🎨 ПАЛИТРА` — bullets опционально.

Пропускать пустые секции допустимо. Менять порядок — нет.

---

## 2. Маркеры

### Маркер виджета (ставится перед именем)

- **⚙️** — системный Material3 / Compose (Scaffold, Column, FlowRow, OutlinedTextField, FAB и т.д.).
- **❇️** — новый кастомный виджет (в этой фиче впервые).
- **🔄** — кастомный, меняется в этой фиче.
- **📌** — кастомный, не меняется в этой фиче.

### Маркер заметки в `notes:`

- **🚨** — отход от Figma или предупреждение об отклонении.
- **ℹ️** — обычная пояснительная заметка.

### Важно

Маркер виджета (⚙️/❇️/🔄/📌) отвечает на вопрос **"трогаем ли в этой фиче?"**, но НЕ отвечает на **"соответствует ли Figma?"**. Это две независимые оси.

Любое расхождение с Figma — независимо от маркера — фиксируется через **🚨 в `notes:`**. См. § 6.

---

## 3. Карта экрана

Карта верхнего уровня — для review человеком. Только виджеты и системные контейнеры. Без внутренностей виджетов.

### Договорённости по подаче

- **Имена кастомных виджетов** — Unicode bold sans-serif в треугольных скобках: `<𝗧𝗼𝗽𝗕𝗮𝗿>`, `<𝗪𝗼𝗿𝗱𝗙𝗶𝗲𝗹𝗱>`.
- **Системные узлы** (Scaffold, Column, FlowRow, LazyColumn и т.д.) — обычным шрифтом без скобок.
- **Тег-семантика** `⟦tag⟧` после имени виджета (категории: `container` / `text` / `icon` / `input` / `button` / `chip` / `image` / `divider`).
- **Slot-style params** в карте: `title=...`, `actions=[...]`, `text="..."`, `trailing=...`, `mode=...`.
- **Динамические списки** — `⚙️ LazyColumn (× N):`.
- **Условный / итеративный рендеринг** (0..N штук внутри обычного контейнера) — `∀ <условие>:`. Пример: `∀ active chip:` означает "для каждого активного chip отрисовать вложенный виджет".
- **FAB-слот Scaffold'а** — `↘️ FAB-slot:` (визуально маркирует правый нижний угол). Виджет внутри слота — обычным образом через `└─ <Widget>`.
- **Размеры в карте** — только **родительского контейнера** (padding / spacing / alignment). Размеры внутри виджетов — в § Анализе.

Карта рисуется в кодоблоке (```), с псевдо-tree-структурой через `├─`, `└─`, `│`.

### Пример

```
⚙️ Scaffold
├─ 🔄 <𝗧𝗼𝗽𝗕𝗮𝗿>  ⟦container+icon⟧            title="Items", actions=[⋮]
└─ ⚙️ Column                                  padding=h:16  spacing=12
   ├─ 📌 <𝗦𝗲𝗮𝗿𝗰𝗵𝗙𝗶𝗲𝗹𝗱>  ⟦input⟧             hint="Search…"
   │
   └─ ⚙️ LazyColumn (× N)                     spacing=8
      🔄 <𝗜𝘁𝗲𝗺𝗖𝗮𝗿𝗱>  ⟦container⟧             padding=16  spacing=8
      ├─ ❇️ <𝗧𝗶𝘁𝗹𝗲𝗖𝗵𝗶𝗽>  ⟦chip⟧              text="Active"
      └─ 📌 <𝗗𝗲𝗹𝗲𝘁𝗲𝗕𝘂𝘁𝘁𝗼𝗻>  ⟦button+icon⟧
   ↘️ FAB-slot                                pos=BottomEnd
   └─ ❇️ <𝗔𝗱𝗱𝗜𝘁𝗲𝗺>  ⟦button+icon⟧
```

Что показано: ⚙️ для системных узлов, ❇️/🔄/📌 для кастомных, Unicode bold + ⟦tag⟧, slot-style params после имени, `(× N)` для динамики, размеры родительского контейнера справа от узла.

---

## 4. Анализ виджетов

Per-widget bullet-list. Для каждого виджета упомянутого в Карте экрана — отдельный блок.

### Заголовок блока

Маркер + имя в Unicode bold + тег-семантика:

```
🔄 <𝗧𝗼𝗽𝗕𝗮𝗿>  ⟦container+icon⟧
```

### Шаблон буллетов

Каждый блок — кодоблок с буллетами `• ключ: значение`. Поля в порядке:

```
   • type:        Compose-узел / M3-компонент или project-widget. См. § 6 про расхождения.
   • size:        width × height (fixed / hug / fill).
   • padding:     all=N / horizontal=N vertical=N / top/bottom/start/end.
   • spacing:     itemSpacing внутри контейнера.
   • shape:       borderRadius, strokeWeight.
   • colors:      ↓ под-bullets через –
       – background: token + (hex, fill_id из Figma) опционально.
       – content: token + (hex) опционально.
       – ... другие layers (border, label, value, ...).
   • typography:  ↓ под-bullets через –
       – label: LexemeStyle.X (Figma style id опционально).
       – value: LexemeStyle.X.
   • slots/content:  ↓ под-bullets через –
       – leading: ...
       – content: ...                         (если один child)
       – content[0]: ..., content[1]: ...     (если последовательность детей в Column / Row — индексы = z-order)
       – trailing: ...
   • params:      composable API. Параметры с типами.
       – paramName: Type
   • callbacks:   что отправляет наружу (Msg.X / () -> Unit).
                  Допускается черновое значение `TBD: <описание намерения>` если контракт сообщений ещё не зафиксирован.
                  При имплементации виджета с `TBD`-callback — callback обязан логировать error-level
                  при срабатывании (явный сигнал что Msg ещё не привязан).
   • behavior:    UX-правила (что disabled когда, какие переходы).
   • notes:       🚨 для отходов от Figma, ℹ️ для обычных заметок. См. § 6.
   • source:      figma <nodeId> или "проектное решение".
```

Пустые поля можно опустить. Поля не переименовывать.

### Источник (`source:`)

- `source: figma 9154:82531 (componentId=4620:58487)` — для виджета по Figma-источнику.
- `source: проектное решение — <короткое обоснование>` — для виджета без Figma-источника (только для виджетов с маркером ❇️).

### Пример

Кастомный виджет с Figma-источником и расхождением:

```
🔄 <𝗜𝘁𝗲𝗺𝗖𝗮𝗿𝗱>  ⟦container⟧
   • type:        M3 Surface (Card)
   • size:        width=fill × height=hug
   • padding:     all=16
   • spacing:     itemSpacing=8
   • shape:       borderRadius=12, shadowElevation=4
   • colors:
       – background: MaterialTheme.colorScheme.surface (#FFFFFF)
       – content: MaterialTheme.colorScheme.onSurface (#19191B)
   • typography:
       – title: LexemeStyle.H5 (24sp Medium)
   • slots/content:
       – leading: <𝗧𝗶𝘁𝗹𝗲𝗖𝗵𝗶𝗽>
       – content: Text title
       – trailing: <𝗗𝗲𝗹𝗲𝘁𝗲𝗕𝘂𝘁𝘁𝗼𝗻>
   • params:
       – item: ItemState
       – enabled: Boolean
   • callbacks:
       – onClick → Msg.OpenItem(id)
       – onDelete → TBD: открыть confirm-dialog, Msg ещё не зафиксирован
   • behavior:    disabled при isPendingOp.
   • notes:
       🚨 Figma corner radius=8 → реализация 12. Причина: единый радиус во всех card'ах проекта.
       ℹ️ Используется в LazyColumn списка items.
   • source:      figma 9154:82519 (componentId=4632:99285)
```

Новый виджет (проектное решение):

```
❇️ <𝗧𝗶𝘁𝗹𝗲𝗖𝗵𝗶𝗽>  ⟦chip⟧
   • type:        M3 InputChip
   • size:        width=hug × height=hug
   • padding:     horizontal=12, vertical=8
   • shape:       borderRadius=999 (pill)
   • colors:
       – background: MaterialTheme.colorScheme.primary
       – content: MaterialTheme.colorScheme.onPrimary
   • typography:
       – label: MaterialTheme.typography.labelSmall (11sp Medium)
   • params:
       – text: String
       – onClose: () -> Unit
   • callbacks:   onClose → Msg.RemoveTitle
   • notes:       ℹ️ UX-решение проекта, нет аналога в Figma.
   • source:      проектное решение — единый pill-стиль для chip-меток.
```

Виджет с последовательностью детей в `content` (индексы):

```
🔄 <𝗦𝘁𝗮𝗰𝗸𝗖𝗮𝗿𝗱>  ⟦container⟧
   • type:        M3 Surface (Card) + Column
   • size:        width=fill × height=hug
   • padding:     all=16
   • spacing:     itemSpacing=12
   • shape:       borderRadius=12, shadowElevation=4
   • colors:
       – background: MaterialTheme.colorScheme.surface
   • slots/content:
       – content[0]: <𝗧𝗶𝘁𝗹𝗲𝗖𝗵𝗶𝗽>
       – content[1]: Text body
       – content[2]: <𝗗𝗲𝗹𝗲𝘁𝗲𝗕𝘂𝘁𝘁𝗼𝗻>
   • params:
       – item: ItemState
   • callbacks:   onDelete → Msg.RemoveItem(id)
   • source:      figma 9154:82519
```

---

## 5. Секция УДАЛЯЕМ — bi-directional migration trace

### Формат

Каждый bullet — миграция:

```
## ❌ УДАЛЯЕМ (с миграцией)
- <WidgetName>.<method/callback> → <куда переехало>
- ...
```

Примеры:

```
- AddLexemeBottomWidget.onAdd        → SubentityChip.onActivate + Msg.Create{Translation|Definition}
- LexemeMeaningWidget.onTypeSelect   → разделено на Msg.CreateTranslation / Msg.CreateDefinition
- ActionsWidget (commit/cancel)      → trailing-icons LexemeValueField (commit) / out-of-field tap (cancel)
- OldLogWidget                       → удалено, нет аналога
```

Если логика удалена бесследно — явно писать `удалено, нет аналога`. Это сигнал что функциональность пропала намеренно.

### Зеркальная пометка в принимающем виджете

Любой виджет, который **получил** мигрировавшую логику, обязан в `notes:` упомянуть источник:

```
• notes:
    🚨 принимает onActivate из удалённого AddLexemeBottomWidget.onAdd (миграция <FEATURE>)
```

Цель — bi-directional trace: по `## ❌ УДАЛЯЕМ` видно куда переехало, по `notes:` принимающего виджета видно откуда пришло. Review проверяет зеркальность.

---

## 6. Отход от Figma — `🚨` в `notes:`

Любое расхождение между реализацией (или планируемой реализацией) и Figma-источником фиксируется через **🚨 в `notes:` виджета**, с явной формулировкой.

### Что считается расхождением

- **kind:** Figma component (`tui-input`, `tui-tag`, `tui-button`) → используемый M3 / project widget.
- **structure:** sibling vs nested, FlowRow vs Column-place в дереве.
- **numeric tokens:** gap / padding / border radius — если значение отличается от Figma.
- **variant:** Figma component variant (`Variant=with btn` и т.п.) → выбранный variant или замена.
- **typography:** style token из Figma → LexemeStyle.* в коде, если они не эквивалентны.

### Формат 🚨

```
• notes:
    🚨 Figma <что> → реализация <что>. Причина: <обоснование>.
```

### Формат `type:` при расхождении

Если расхождение по типу компонента — указать в `type:` пару:

```
• type: tui-input (Figma componentId=4620:57849) → LexemeEditableText (project inline-edit pattern)
```

### Маркер 📌 при отходе

Если виджет имеет маркер 📌 ("не трогаем в фиче") **И** имеет расхождение с Figma — маркер остаётся 📌 (он про дело фичи), но **обязательна** 🚨 в notes. Иначе документ врёт: Figma-долг скрыт за "без изменений".

---

## 7. Чек-лист дисциплины

Перед публикацией документа автор (агент или человек) проверяет:

1. **Маркеры 📌:** для каждого виджета с 📌 — либо нет расхождений с Figma, либо стоит 🚨 в `notes:`.
2. **Миграция:** для каждой строки в `## ❌ УДАЛЯЕМ` есть либо явная цель миграции, либо `удалено, нет аналога`.
3. **Зеркало:** для каждой ненулевой миграции в принимающем виджете в `notes:` стоит 🚨 с источником.
4. **Шаблон буллетов:** все непустые поля в `## 🔍 Анализ виджетов` следуют порядку из § 4.
5. **Карта экрана:** все кастомные виджеты в Unicode bold + ⟦tag⟧, системные — обычным шрифтом.

---

