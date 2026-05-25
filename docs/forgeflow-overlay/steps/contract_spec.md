---
name: contract_spec
output: contract_spec.md
output_criteria:
  - contract_spec.md существует
  - contract_spec.md содержит секцию Бизнес-описание
  - contract_spec.md содержит секцию User Stories
  - contract_spec.md содержит секцию State
  - contract_spec.md содержит секцию UI Messages
  - contract_spec.md содержит секцию IO
  - contract_spec.md содержит секцию UseCase
  - файл лежит в feature dir (НЕ в spec_dir — публикацию делает publish_spec)
  - содержит финальный формат спеки (Заголовок / Бизнес-описание / User Stories / State / UI Messages / IO / UseCase / опционально Тестовые сценарии)
  - имя файла спеки (для будущей публикации) явно зафиксировано в начале артефакта
  - State / UI Messages / IO / UseCase разделы скопированы из артефактов шагов 1-4 (с очисткой удаляемых элементов и черновых пометок)
---

Шаг 5 контрактного блока. **Агрегация** артефактов шагов 1-4 в черновик проектной спеки. Кладётся в **feature dir** как `contract_spec.md`. Физическая публикация в `spec_dir/` — на шаге `publish_spec` (после `implement`).

См. полные правила в `docs/features/FORGEFLOW_contract_design.md` § 5 `contract_spec` и § «Формат проектной спеки».

## Проверка input

Все 4 артефакта шагов 1-4 обязаны существовать и содержать ключевые секции:

1. **`contract_state.output`** — `## Режим работы` + State структура + computed properties (если есть) + инварианты. Без любого из этого — **error**: «contract_state неполный, разделы для агрегации в спеку отсутствуют».
2. **`contract_ui_msg.output`** — sealed interface Msg + reducer-логика per Msg. Header «черновик» **игнорируется при агрегации** (он был меткой работы). Без Msg структуры — **error**.
3. **`contract_io.output`** — `## Effects`, `## Subscribers (FlowHandlers)`, `## Проверка реактивности`, `## Бизнес-инварианты`. Без любого из этих 4 разделов — **error**: «contract_io неполный».
4. **`contract_usecase.output`** — interface UseCase с методами + комментариями consumer'ов (Effect/Subscriber). Без него — **error**.
5. **`parent.scope_analysis.output`** — для бизнес-описания / user stories / `spec_filename`. Без `spec_filename` — **error**: «scope_analysis не указал имя файла спеки, не знаю куда публиковать».

**Существующая спека в `spec_dir/<spec_filename>`** — опциональный input (читается если файл существует). Отсутствие — норма для новой фичи.

## Источники

- `contract_state.output` — State / Computed / Инварианты
- `contract_ui_msg.output` — UI Msg + reducer-логика (только UI-стороны; **игнорировать forward-refs на effects** — они черновик, канон в шаге 3)
- `contract_io.output` — Effects + Subscribers + Datasource Msg + Edge cases + Бизнес-инварианты (**основной источник** для Msg ↔ Effect связи)
- `contract_usecase.output` — UseCase interface
- `parent.scope_analysis.output` — для бизнес-описания / user stories / `spec_filename`
- **Существующая спека** в `spec_dir/<spec_filename>` (если есть, читается как **reference** — для сохранения бизнес-описания / user stories которые контракт не менял)

## Принципы агрегации

1. **Спека = срез финального состояния «как должно быть» после реализации этой задачи.** Не история, не было/стало, не план.
2. **Источник истины для Msg ↔ Effect связи — шаг 3** (`contract_io`). Forward-refs из шага 2 — игнорировать.
3. **Минимизация дублирования.** Не копипастить ADT-обоснования. Не копировать длинные edge case разборы — только итоговое поведение.
4. **Чтение существующей спеки — обязательно.** Если есть разделы которые не затронуты текущим контрактом (бизнес-описание, user stories) — **сохранить как есть**. Контракт меняет State/Msg/Effect/UseCase, обычно не бизнес-описание.
5. **При bugfix без значимых изменений** — спека минимально обновляется.
6. **Имя файла спеки** — `spec_filename` из `scope_analysis.output`. Если новая фича — выбрать имя по модулю/экрану (`dictionary-form.md`, `word-card.md`, snake-case).

## Структура черновика

```markdown
<!-- META: spec_filename: <имя>.md -->

# <Feature Name>

## Бизнес-описание

(1-2 абзаца — что фича делает, зачем нужна, без технических деталей)

## User Stories

(список «Как роль, я хочу действие, чтобы результат» — без искусственного ограничения количества)

## State

(сводный data class + nested + per-field описание Что / Почему / computed / инварианты — без удаляемых полей и черновых пометок)

## UI Messages

(обязательный header убрать — это был черновик-маркер. Сводный sealed interface Msg + категоризация / per Msg Trigger / State changes / Effects / Guard)

## IO

### Effects

(Datasource / Navigation / Ui — целостные блоки per Effect с Reducer-логикой)

### Subscribers

(per subscriber)

## UseCase

(interface + методы + комментарии Effect/Subscriber)

## Тестовые сценарии (опционально)

(если из артефактов выводимо — list сценариев в формате Предусловие / Действие / Ожидание)
```

## Что НЕ должно быть в черновике спеки

- Развёрнутый ADT-аудит (полное прохождение правил моделирования) — это процесс
- Раздел «Удаляемые поля / messages / methods» из артефактов шагов 1-4 — спека = срез финального состояния
- Раздел `## Расхождения spec ↔ code` из артефактов — это работа конкретного шага, не часть спеки
- Раздел `## Бизнес-инварианты` из `contract_io.output` — это **рабочее обоснование для ревью subscribers**, не часть финальной спеки. В черновик спеки **не копируется**
- Раздел `## Проверка реактивности` из `contract_io.output` — рабочая инвентаризация (Y/N + обоснование), не часть финальной спеки
- Header «черновик, канон в contract_io» из артефакта шага 2 — он был меткой работы, не часть спеки
- Детали реализации: классы reducer'а, пакеты, imports, конкретные имена handler'ов
- История изменений (было / стало)

## Что НЕ делать

- Не писать в `spec_dir/`. Только в feature dir (`contract_spec.md`). Публикация — на `publish_spec`.
- Не агрегировать forward-refs из шага 2 как канон Msg↔Effect. Канон — шаг 3.
- Не переписывать бизнес-описание / user stories если они есть в существующей спеке и контракт их не затронул.
- Не плодить структурные заголовки если категория пустая (например `### UI Effects` если нет UiEffect — раздел опускается).
