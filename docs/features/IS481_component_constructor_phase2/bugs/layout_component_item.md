# Layout: айтем компонента (карточка в списке)

Контекст: список компонентов в `ComponentsManagerScreen` (вкладка Настройки →
Компоненты) и `PerDictionaryComponentsScreen` (вкладка Словарь → меню →
Компоненты). Каждый айтем — `UserDefinedRowWidget` (Manager) или
`PerDictRowWidget` (per-dict).

---

## Целевая схема (что должно быть)

```
┌────────────────────────────────────────────────────────────────────┐
│ Surface(rounded=12, color=surface, fillMaxWidth)                   │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ Column(padding=16h/12v, gap=8v)                                │ │
│ │                                                                │ │
│ │ ─[ROW 1: name + inline chips]──────────────────────────────    │ │
│ │   FlowRow(gap=8h, gap=4v, center)                              │ │
│ │     Text(name, BodyL, BLACK)                                   │ │
│ │     Chip("global")  [if isGlobal]                              │ │
│ │     Chip(isMulti?"много":"одно")                               │ │
│ │                                                                │ │
│ │ ─[ROW 2: icon + template + actions]────────────────────────    │ │
│ │   Row(gap=12h, center)                                         │ │
│ │     IconBoxed(ic_components, 24)                               │ │
│ │     Chip(template)                                             │ │
│ │     Spacer(weight=1)                                           │ │
│ │     IconBoxed(ic_edit, 44, click=onEdit)                       │ │
│ │     IconBoxed(ic_trash, 44, click=onDelete)                    │ │
│ │                                                                │ │
│ │ ─[ROW 3: meta]──────────────────────────────────────────────   │ │
│ │   Text(meta, BodyS, BLACK)                                     │ │
│ │     — PerDict: "Значений: N"                                   │ │
│ │     — Manager: "N · словарь1, словарь2"                        │ │
│ └────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────┘
```

**Чип (`BlueAssistChip`):**
- `Surface(shape = RoundedCornerShape(50%), color = primary)` — pill.
- `padding(horizontal = 8.dp, vertical = 2.dp)` — компактный.
- `Text(color = onPrimary, style = labelSmall.copy(fontSize = 10.sp))` — мелкий.

**Принцип разделения чипов:**
- `global` + cardinality (`одно`/`много`) — inline с name (короткие
  свойства, «как компонент существует»).
- `template` (`Текст`/`Картинка`) — в actions-row рядом с icon (концептуально
  «какие данные хранит» — связано с типом entity).

---

## Контракт слотов

| Слот   | Кто                                  | Стиль            | Цвет        |
|--------|--------------------------------------|------------------|-------------|
| icon   | `IconBoxed(ic_components, 24)`       | —                | `enableIconColor` |
| name   | `Text(name)`                         | `BodyL`          | `blackColor` |
| chips  | `FlowRow` из 1–3 `AssistChip`        | `labelSmall`     | контейнер: `primary` (синий), текст: `onPrimary` (белый) |
| meta   | `Text("Значений: N"` или `"N · ..."`)| `BodyS`          | `blackColor` |
| edit   | `IconBoxed(ic_edit, 44, clickable)`  | —                | `enableIconColor` |
| trash  | `IconBoxed(ic_trash, 44, clickable)` | —                | `enableIconColor` |

**Чипы — read-only:** `AssistChip(enabled = false, onClick = {})` с override
colors через `AssistChipDefaults.assistChipColors(disabledContainerColor =
primary, disabledLabelColor = onPrimary)`. Граница чипа — `null` (без border)
чтобы не пробивать рамку поверх синего.

**FlowRow для chips-row:** обязателен, потому что при 3 чипах + узком экране
обычный `Row` оверфлоу-чит, и текст «Значений: N» (раньше был в той же Row)
переносился по символу вертикально (см. `img.png`).

---

## Что было ДО (на скриншоте `img.png`)

```
┌─────────────────────────────────────────────────────────────┐
│ Row                                                          │
│  icon | Column                                | ✎ | 🗑       │
│         Row(title): name + global-chip                       │
│         Row(meta):  template-chip + multi-chip + "Значений" │
│                     └─ оверфлоу: "Значений: N" → ВЕРТИКАЛЬ! │
└─────────────────────────────────────────────────────────────┘
```

Проблемы:
1. `name` — серый (без explicit `color`), наследует `LocalContentColor` который
   в `Surface(color = surface)` через `contentColorFor` мапится на `onSurface`.
   В кастомной палитре `onSurface = blackColor`, но визуально юзер видит серый
   (Material3 может применять tonal blend или alpha). Лекарство — explicit
   `color = blackColor`.
2. `meta-row` смешивает чипы и текст в одной `Row` без переноса → overflow.
3. Чипы дефолтного M3 цвета — нужны синие.

---

## Что меняется в коде

### `PerDictRowWidget.kt`

| Было | Станет |
|---|---|
| `Row(title)`: name + (global if isGlobal) | просто `Text(name, BodyL, color=blackColor)` |
| `Row(meta)`: template + multi + Text valueCount | `FlowRow(chips)`: global + template + multi (синие), затем отдельный `Text("Значений: N", BodyS, color=blackColor)` |

### `UserDefinedRowWidget.kt`

| Было | Станет |
|---|---|
| `Text(name)` без color | `Text(name, color=blackColor)` |
| `Row(meta)`: template + multi + (global if isGlobal) | `FlowRow(chips)`: global + template + multi (синие) |
| `Text(usageCount · dictText)` без color | то же, `color=blackColor` |

### Общий синий-чип helper

В `:modules:widget:component_widgets` добавить локальный composable
`BlueAssistChip(@StringRes label: Int)` чтобы не дублировать override colors в
обоих row-widget'ах. (Не выносить выше — других callsite'ов нет.)
