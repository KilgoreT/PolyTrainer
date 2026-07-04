---
name: business_contract_spec
output: business_contract_spec.md
input_criteria:
  - 02_scope.md существует
  - business_contract.md существует
output_criteria:
  - business_contract_spec.md существует
  - содержит секцию Бизнес-описание
  - содержит секцию User Stories
  - содержит разделы State / UI Messages / IO / UseCase (из business_contract)
  - имя файла спеки (для будущей публикации) явно зафиксировано в начале артефакта
  - файл лежит в feature dir (НЕ в spec_dir — публикацию делает publish_spec)
---

Подготовка черновика проектной спеки на основе `business_contract` и `scope_analysis`. Кладётся в **feature dir** как `business_contract_spec.md`. Физическая публикация в `spec_dir/` — на шаге `business_publish_spec` (после `implement`).

См. полные правила в `docs/features/FORGEFLOW_contract_design.md` § «Формат проектной спеки».

## Проверка input

1. **`business_contract.output`** — обязан содержать разделы State / Msg / Effect/IO / UseCase. Без любого — **error**: «business_contract неполный, разделы для агрегации в спеку отсутствуют».
2. **`scope_analysis.output`** — для бизнес-описания / user stories / `spec_filename`. Без `spec_filename` — **error**: «scope_analysis не указал имя файла спеки».
3. **Существующая спека в `spec_dir/<spec_filename>`** — опциональный input (читается если файл существует). Отсутствие — норма для новой фичи.

## Что делать

1. Прочитай `business_contract.output` — источник State / Msg / IO / UseCase.
2. Прочитай `scope_analysis.output` — оттуда бизнес-описание / user stories / `spec_filename`.
3. Если в `spec_dir/<spec_filename>` уже есть спека — прочитай: разделы которые контракт не затронул (бизнес-описание, user stories) **сохраняй как есть**.
4. Собери черновик в `business_contract_spec.md` в feature dir.

## Принципы

1. **Спека = срез финального состояния** «как должно быть» после реализации. Не история, не было/стало, не план.
2. **Имя файла спеки** — `spec_filename` из `scope_analysis.output`. Если новая фича — выбрать имя по модулю/экрану (`dictionary-form.md`, `word-card.md`, snake-case).
3. **Минимизация дублирования.** Не копировать длинные обоснования из контракта — только итоговое поведение.
4. **Чтение существующей спеки — обязательно** если `spec_filename` указан и файл существует. Не переписывать бизнес-описание / user stories если контракт их не затронул.

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

(sealed interface Msg + категоризация / per Msg Trigger / State changes / Effects / Guard)

## IO

### Effects

(Datasource / Navigation / Ui — целостные блоки per Effect с Reducer-логикой)

### Subscribers

(per subscriber)

## UseCase

(interface + методы + комментарии Effect/Subscriber)

## Тестовые сценарии (опционально)

(если из контракта выводимо — list сценариев в формате Предусловие / Действие / Ожидание)
```

## Что НЕ должно быть в черновике спеки

- Развёрнутый ADT-аудит (полное прохождение правил моделирования) — это процесс
- Раздел «Удаляемые поля / messages / methods» — спека = срез финального состояния
- Раздел `## Расхождения spec ↔ code` — это работа конкретного шага, не часть спеки
- Раздел `## Бизнес-инварианты` (рабочее обоснование для ревью) — в черновик спеки не копируется
- Раздел `## Проверка реактивности` — рабочая инвентаризация, не часть финальной спеки
- Детали реализации: классы reducer'а, пакеты, imports, конкретные имена handler'ов
- История изменений (было / стало)

## Что НЕ делать

- Не писать в `spec_dir/`. Только в feature dir (`business_contract_spec.md`). Публикация — на `business_publish_spec`.
- Не переписывать бизнес-описание / user stories если они есть в существующей спеке и контракт их не затронул.
- Не плодить структурные заголовки если категория пустая (например `### UI Effects` если нет UiEffect — раздел опускается).
