# UI-примитивы и структура виджета

Формальный словарь примитивов для описания UI-виджетов в спецификациях (артефакт `ui_layout.md`). Цель — устранить разночтения: вместо коротких тегов вроде `⟦chip+input⟧` использовать **явную структуру** «слот → примитив».

Без этого словаря исполнитель интерпретирует семантику виджета на свой вкус. Реальный кейс: тег `⟦chip+input⟧` для view-mode field был обёрнут в `M3 InputChip`, хотя задумка была «chip — это заголовок выше, input — отдельное поле для значения». Бага не было видно на review, потому что код был синтаксически корректен.

## Две категории примитивов

### Atoms — контентные

| Примитив | Что означает | Compose-аналоги |
|---|---|---|
| `text` | Отображаемый текст. Однострочный или многострочный. | `Text` |
| `input` | Поле редактирования текста (single-line или multi-line). | `BasicTextField`, `OutlinedTextField`, `TextField` |
| `button` | Интерактивная кнопка с обработкой нажатия. | `Button`, `OutlinedButton`, `TextButton`, `FloatingActionButton` |
| `icon` | Векторный/растровый знак, обычно интерактивный или информационный. | `Icon`, `IconButton` |
| `chip` | Компактный контейнер-маркер с состоянием (selected / unselected). | `AssistChip`, `InputChip`, `FilterChip`, `SuggestionChip` |
| `image` | Растровое изображение / иллюстрация. | `Image`, `AsyncImage` |
| `progress` | Индикатор загрузки / прогресса. | `CircularProgressIndicator`, `LinearProgressIndicator` |

### Layouts — структурные

| Примитив | Что означает | Compose-аналоги |
|---|---|---|
| `column` | Вертикальное расположение детей. | `Column` |
| `row` | Горизонтальное расположение детей. | `Row` |
| `box` | Overlapping (стек) — дети накладываются друг на друга. | `Box` |
| `flow-row` | Горизонтальный поток с переносом на следующую строку. | `FlowRow` |
| `flow-column` | Вертикальный поток с переносом. | `FlowColumn` |
| `lazy-column` | Виртуализированный вертикальный список (динамика, переменный N). | `LazyColumn` |
| `lazy-row` | Виртуализированный горизонтальный список. | `LazyRow` |
| `scaffold` | Slotted-template со слотами `top-bar` / `bottom-bar` / `fab` / `content`. | `Scaffold` |

## Правила построения виджета

**Виджет = иерархия layouts с atoms в слотах.**

Базовые правила:

1. **Виджет всегда начинается с layout-примитива** (не с atom). Atom без layout-обёртки в спецификации не описывается — он часть слота родителя.
2. **Слот именуется по семантике**, не по позиции. Хорошие имена: `header_slot`, `value_slot`, `action_slot`, `trailing_slot`, `leading_slot`. Плохие: `child1`, `top`, `slot_a`.
3. **В одном слоте — один примитив** (layout или atom). Если в слоте несколько элементов — это вложенный layout.
4. **Динамический контент** — `lazy-column` / `lazy-row` с пометкой `(× N)` после имени.
5. **Mode-dependent** — если содержимое слота меняется в зависимости от state, явно указать: `value_slot: text-or-input (mode-dependent)` с комментарием какой mode когда.

## Формат описания виджета

```
<WidgetName>:
  <layout-primitive> [params]
    <slot_name>: <primitive> [params]
    <slot_name>:
      <nested-layout> [params]
        <slot_name>: <primitive>
        ...
```

Где `params` — атрибуты примитива релевантные UX (gap / padding / variant / alignment / ...).

## Пример

`LexemeValueField` (поле значения перевода / определения):

```
LexemeValueField:
  column  spacing=4
    header_slot: chip  variant=SubentityChip  label="Translation"
    value_slot: text-or-input (mode-dependent)
      # mode=view  → text  clickable=onOpenEditMode
      # mode=edit  → input  BasicTextField
    action_slot: icon  position=trailing  iconRes=ic_close  visible=when isEdit
```

Это однозначно описывает структуру: column → 3 слота с явными примитивами. У исполнителя нет места для интерпретации «обернуть value в chip» — `value_slot: text-or-input`, не `value_slot: chip`.

## Anti-patterns

- **Тег вместо структуры.** `⟦chip+input⟧` без слотов — запрещено. Слово `chip` ничего не говорит о роли (заголовок? обёртка? trailing?).
- **Слот без примитива.** `header_slot: ...` — каждый слот обязан иметь явный примитив из словаря.
- **Atom-only widget.** Если виджет = просто `Text` без обёртки — это не виджет уровня спецификации, это inline в родительском layout'е.
- **Generic «container».** Раньше иногда писали `container` — это размытие. Конкретный layout (column / row / box / ...) даёт UX-семантику.

## Где описаны существующие виджеты

Reference-виджеты проекта (как они построены) — в `modules/core/ui/*` и в существующих экранах. При написании `ui_layout.md` используй grep / Read чтобы найти аналог в коде и сослаться.

Если виджета-аналога нет — описывай через примитивы с нуля, опираясь на этот словарь.

## Расширение словаря

Если в проекте появляется устойчивый паттерн UI который не покрывается текущим словарём (например, новый layout-тип) — добавь его сюда (в категорию `atoms` или `layouts`) с краткой семантикой и compose-аналогом. Не вводи синонимы существующим примитивам.
