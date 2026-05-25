---
name: contract_ui_msg
output: contract_ui_msg.md
output_criteria:
  - contract_ui_msg.md существует
  - contract_ui_msg.md содержит секцию UI Messages
  - contract_ui_msg.md содержит секцию Reducer-логика per Msg
  - contract_ui_msg.md содержит секцию Расхождения spec ↔ code
  - содержит обязательный header «черновик, канон в contract_io»
  - содержит sealed interface Msg + варианты в code-block
  - категоризация без раздела «Результаты данных»
  - per Msg описание (trigger / state changes / forward-ref на effects если нужны)
---

Шаг 2 контрактного блока. Описать UI Messages (Msg от пользователя) + reducer-логика для них. Forward-refs на effects — гипотезы, канон на шаге 3.

См. полные правила в `docs/features/FORGEFLOW_contract_design.md` § 2 `contract_ui_msg` и § «Формат проектной спеки → Раздел 5. UI Messages».

## Проверка input

1. **`contract_state.output` содержит `## Режим работы`** — раздел с выбранным режимом (1/2/3) из шага 1. Без него — **error**: «contract_state не объявил режим работы, не могу выбрать источник UI Msg».
2. **`contract_state.output` содержит структуру State** — code-block с `data class` или явный textual список полей. Без него — **error**: «State структура отсутствует, нечего использовать в reducer-логике».
3. **`parent.scope_analysis.output` доступен** (валидация уже прошла на шаге 1). Если файл недоступен / повреждён — **error**.

## Определение режима

Берётся из output шага 1 (`contract_state.md` → раздел `## Режим работы`). Источник Msg:

- **Режим 1 (макет-driven):** макет — точки взаимодействия (кнопки, поля, тапы, жесты)
- **Режим 2 (spec-driven):** спека `## UI Messages` раздел
- **Режим 3 (code-driven):** `Message.kt` (sealed interface Msg) + `Reducer.kt` (reducer-логика per Msg)

## Обязательный header в output

В начале `contract_ui_msg.md` написать:

```
> ⚠ **Этот артефакт — черновик.** Финал Msg-списка и связи Msg ↔ Effect живёт в `contract_io.output`.
> Forward-references на effects ниже — гипотезы. Ревью на этом шаге: UI-триггеры, state changes, категоризация Msg.
> Не фиксировать Msg ↔ Effect связь как окончательную.
```

## Что делать

1. **Извлечь UI Msg** в зависимости от режима. Каждый тап / инпут / жест / lifecycle event = один Msg.
2. Описать `sealed interface Msg` в code-block.
3. **Категоризация** (только UI-стороны):
   - Действия пользователя (Submit, UpdateField, SelectItem)
   - Навигация (RequestBack, OpenDetails) — intent от UI, не команда
   - UI feedback (ShowError, ShowSnackbar)
   - Переключатели (EarliestOn / EarliestOff)
   - No-op (Msg.Empty)

   **Datasource Msg (результаты данных от effects) на этом шаге НЕ описываются** — они появляются на шаге 3 (`contract_io`).
4. **Per Msg** написать:
   - **Что** — что делает пользователь / что происходит
   - **Trigger** — конкретная точка UI (тап на кнопке X, ввод в поле Y)
   - **State changes** — какие поля State меняются. Семантически («устанавливается флаг загрузки», «обновляется поле имени»), НЕ через имена extension-функций (`state.startSubmit()`)
   - **Effects** — forward-reference: «здесь нужен effect X» (гипотеза)
   - **Guard** (если есть) — условие при котором Msg обрабатывается (`if state.canSubmit`)
   - **Почему** — обязательно при whitelist триггерах:
     - (a) payload — не одно простое поле и не id
     - (b) одно действие split на несколько Msg (`On/Off` вместо `Toggle(Boolean)`)
     - (c) `RequestX` вместо `DoX` (reducer-controlled паттерн)
     - (d) изменение существующего Msg
   - Для тривиальных Msg — без «Почему».
5. Если переделка — раздел «Удаляемые / новые messages».

## В режиме 2 — обязательная сверка spec ↔ code

- Grep `^\s*data (object|class)` по `Message.kt` → выписать все варианты Msg
- Grep `is Msg\.` по `Reducer.kt` → перечислить какие Msg реально обрабатываются
- Сравнить со спекой `## UI Messages`. Расхождения → `## Расхождения spec ↔ code` в output (обязателен даже если расхождений нет).
- При расхождении в технических декларациях — приоритет коду.

## Правила моделирования

1. **Msg = sum type.** Каждый вариант = одна ветка sealed. Не плодить Msg-варианты для UI-**состояний** (loading/error — это поле State, не Msg).
2. **Computed properties в reducer-логике.** Если решение ветвится по derived условию — читать `state.canSubmit` (computed), не дублировать условие.
3. **Shortcut-игнор Msg.** Если действие не имеет смысла в текущем state — `state to emptySet()` (явный игнор). При асинхронном рассинхроне reducer спокойно ничего не делает.
4. **Editable-сценарии** (только для форм редактирования) — Msg на изменение значения содержит **только новое значение поля** (`UpdateName(value: String)`), не всю domain-модель. Reducer обновляет editable копию, не original.

## Что НЕ делать

- Не описывать Datasource Msg (это шаг 3)
- Не углубляться в effects (только forward-ref)
- Не использовать имена extension-функций в state changes — семантическое описание
- Не плодить Msg для состояний экрана
