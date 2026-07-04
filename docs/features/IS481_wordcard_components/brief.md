# IS481 — Inline component-значения в WordCard

## Контекст

`IS481` phase 2 завершён: словари + компоненты (global + per-dict) уже создаются/редактируются/удаляются в Manager и PerDict экранах. Но **в самой лексеме** (WordCard) добавить значение для созданного компонента нельзя — там только встроенный «Перевод» как отдельная кнопка.

Эта мини-фича добавляет inline-редактирование component-значений на WordCard, по той же UX-модели что текущий «Перевод».

## Goal

Юзер на экране WordCard может:
1. Видеть список доступных компонентов (global + per-dict для текущего словаря) как chip'ы.
2. Тапом на chip добавить значение этого компонента к лексеме.
3. Редактировать значение inline (как «Перевод» сейчас).
4. Удалить добавленное значение через trash-кнопку.
5. Для multi-компонентов — добавить несколько значений.

## UX behavior

### Layout

Вертикальный стек под карточкой лексемы:

```
[Карточка лексемы «арг» + флаг]
─────────────────────
Перевод: "argument" [🗑]              ← добавленное (1)
Пример: "good argument" [🗑]          ← добавленное (2)
Пример: "weak argument" [🗑]          ← добавленное (3, multi)
─────────────────────
[chip Пример]                        ← multi, остался
[chip Definition]                    ← доступный
[chip Synonym]                       ← доступный
```

- **Добавленные значения** сверху, в порядке добавления (chronologically).
- **Доступные chip'ы** снизу.
- Между ними разделитель не обязателен (визуальная группировка через padding).

### Поведение

1. **Тап на chip:**
   - Под последним добавленным значением (или сразу под лексемой если первое) появляется новый input с label = имя компонента.
   - Input в pristine state — пустое поле, ready to type.
   - **Если компонент `isMulti=false`** → chip пропадает из списка доступных.
   - **Если компонент `isMulti=true`** → chip остаётся, можно тапнуть ещё раз → ещё один input ниже.

2. **Редактирование:**
   - Inline в input field.
   - Автосейв при blur (как «Перевод» сейчас) — точно та же механика.
   - Empty input после blur — допустимо ли удалить запись? Пока **нет** — пустое значение остаётся (юзер сам удалит через trash если хочет).

3. **Удаление (trash):**
   - Каждое добавленное значение имеет trash-кнопку (точно как «Перевод»).
   - После удаления:
     - Если non-multi → chip возвращается в список доступных.
     - Если multi → chip продолжает быть в доступных (он там и был).

4. **Built-in «Перевод»:**
   - Рендерится как любой другой chip (унифицированный список).
   - В коде остаётся built-in (`system_key='translation'`).

## Out of scope

- **Image template** — только TEXT. `BlueAssistChip` для image не показывается / disabled.
- Reorder добавленных значений (drag-and-drop) — нет.
- Validation длины/формата текста — нет (доверяем юзеру).
- Onboarding/empty state «у вас нет компонентов» — отдельно (если 0 компонентов — chip-list пустой, видна только зона добавленных + Перевод).

## Acceptance criteria

1. На экране WordCard для лексемы из словаря с N global + M per-dict компонентами видна chip-row с (N+M) chips. Built-in «Перевод» — первый из N global (возвращается `flowTypesForDictionary` вместе с остальными, не добавляется сверху).
2. Тап на chip non-multi → input появляется, chip пропадает.
3. Тап на chip multi → input появляется, chip остаётся.
4. Печать в input + blur → значение сохранено в `component_values` table (typeId + lexemeId + text).
5. Открытие лексемы снова показывает сохранённые значения.
6. Trash на добавленном значении удаляет его + (для non-multi) возвращает chip.
7. «Перевод» работает идентично остальным chips (под капотом — built-in компонент).

## Технический скоуп (rough estimate)

### Затронутые файлы (~6-10)

| Слой | Файл | Изменение |
|---|---|---|
| State | `WordCardScreenState` (или эквивалент) | + `availableComponents: List<ComponentType>`, + `addedValues: List<ComponentValue>` |
| Reducer | `WordCardReducer` | + Msg.AddComponentValue/RemoveComponentValue/UpdateComponentText handlers |
| UseCase | `WordCardUseCase` | + `flowAvailableComponentsForDict(dictId)`, использование existing `addComponentValue/updateComponentValue/deleteComponentValue` (был backlog'ом как dead code) |
| Effect handler | `DatasourceEffectHandler` | + wire новые effects к UseCase |
| UI | `WordCardScreen` (или equivalent) | + chip-row composable + per-value inline TextWidget rendering (переиспользовать `BlueAssistChip` + `ComponentByTemplate` из `:modules:widget:component_widgets`) |
| Domain | `Lexeme` или связано | проверить если addedValues уже модель есть (или вынести как separate) |

### Effort

**Medium** (3-5 часов работы по моей оценке без полного flow):
- Reducer + Msg/Effect: 1ч.
- UseCase + DAO calls: 0.5ч (уже существующие API).
- UI: 1.5-2ч (chip-row + inline render + autosave wire).
- Tests: 1ч.
- Manual smoke + debug: 0.5ч.

**Risk:**
- Скорее всего обнаружится pre-existing technical debt в WordCard Reducer (architectural inconsistency со state shape для added values).
- Multi-add UX edge cases (что если юзер быстро тапнул chip 5 раз — 5 input'ов появится мгновенно?).

## Trigger

Сейчас.

## Связано

- `IS481_component_constructor` (phase 1) — создание/редактирование компонентов.
- `IS481_component_constructor_phase2` — edit + cardinality + UI polish (закоммичено `b54ea8f`).
- Backlog `nav_stuck_after_dict_recreate.md` — нав-баг не блокирует этот scope.
