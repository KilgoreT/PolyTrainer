---
name: publish_ui
output: publish_ui.md
output_criteria:
  - publish_ui.md существует в plan.dir
  - publish_ui.md содержит секцию Опубликовано
  - publish_ui.md содержит секцию Корректировки от implement
  - публикация выполнена — раздел `## UI Layout` в `spec_dir/<feature>.md` соответствует `ui_layout.output` (с учётом корректировок implement)
  - если файл `spec_dir/<feature>.md` не существует — создан минимальный шаблон с placeholder'ами для отсутствующих разделов + наполненный раздел `## UI Layout`
  - если файл существует но не соответствует каноническому формату спеки — шаг падает с error «spec not in canonical format»
---

UI-аналог business `publish_spec`. Мержит UI Layout (snapshot из `ui_layout.output` + корректировки из `implement.output`) в проектную спеку `spec_dir/<feature>.md`. Симметричен `publish_spec` business sub-flow, но не создаёт новые business-разделы — только UI Layout.

## 1. Источники

- `ui_layout.output` — финальный snapshot UI (из шага `ui_layout`).
- `implement.output` — артефакт реализации (для опциональной корректировки UI Layout под фактически реализованный код, если в impl что-то отошло от ui_layout).
- `spec_dir/<feature>.md` — существующая спека (может быть создана `publish_spec` business sub-flow ранее либо отсутствовать).

`spec_dir` определяется через `plan.context.workspace.spec_dir` (например `docs/features-spec/`).

## 2. Имя файла спеки

Берётся из контекста фичи (`spec_filename`, `feature_name` либо из META `contract_spec.md` business — если business sub-flow прошёл). Если есть несколько источников — приоритет: explicit `spec_filename` в plan.context > META в `contract_spec.md` > `feature_name`.snake_case.

## 3. Канонический формат спеки

Целевой порядок разделов в спеке (закон):

1. `# <Feature Name>`
2. `## Бизнес-описание`
3. `## User Stories`
4. `## State`
5. `## UI Messages`
6. `## UI Layout`  ← наш раздел
7. `## IO`
8. `## UseCase`
9. `## Тестовые сценарии` (опционально)

**Никаких эвристик «найти подходящее место».** Раздел `## UI Layout` имеет фиксированное место — между `## UI Messages` и `## IO`.

## 4. Алгоритм

### 4.1. Если файл `spec_dir/<feature>.md` существует

1. **Проверить канон.** Извлечь все H2-заголовки в порядке появления. Сравнить с каноническим набором (с учётом отсутствия опциональных).
   - **Если порядок не соответствует канону** → output:
     ```markdown
     ## Error: spec not in canonical format

     Файл `<path>` имеет разделы в неканоническом порядке (legacy формат).
     Перед публикацией UI Layout необходима миграция спеки.
     См. backlog `docs/Backlog.md` — «Миграция существующих спек».
     ```
     И завершить шаг со статусом `feedback_required`.

2. **Замёржить UI Layout.**
   - Если раздел `## UI Layout` уже есть — заменить содержимое (от `## UI Layout` до следующего `## ` либо EOF) на новое из `ui_layout.output`.
   - Если раздела нет — вставить между `## UI Messages` (после её содержимого) и `## IO` (либо перед `## UseCase` если `## IO` отсутствует — крайне редкий кейс).

3. **Применить корректировки от implement** — если в `implement.output` зафиксированы изменения UI vs `ui_layout.output` (новый виджет, переименование, разные размеры) — отразить их в спеке. Все корректировки также перечислить в разделе `## Корректировки от implement` артефакта шага.

### 4.2. Если файл `spec_dir/<feature>.md` НЕ существует

Создать новый файл с минимальным каноническим шаблоном:

```markdown
# <Feature Name>

## Бизнес-описание

*Раздел не заполнен: business sub-flow не запускался для этой фичи.*

## User Stories

*Раздел не заполнен.*

## State

*Раздел не заполнен.*

## UI Messages

*Раздел не заполнен.*

## UI Layout

<содержимое из ui_layout.output + корректировки implement>

## IO

*Раздел не заполнен.*

## UseCase

*Раздел не заполнен.*
```

Это покрывает кейс «чисто UI-фича без business sub-flow».

### 4.3. Проверка размера UI Layout

После вставки замерить количество строк раздела `## UI Layout` (от заголовка до следующего `## ` или EOF).

- Если **> 500 строк** — вынести содержимое в отдельный файл `spec_dir/<feature>-ui.md` (полная UI-спека), в основной `<feature>.md` оставить:
  ```markdown
  ## UI Layout

  См. подробную UI-разметку: [<feature>-ui.md](<feature>-ui.md).
  ```
- Если **≤ 500 строк** — inline в основной спеке.

## 5. Output `publish_ui.md` — что записать

```markdown
# publish_ui

## Опубликовано
- `<spec_dir>/<имя>.md` — <создан / обновлён>
- Режим: <create / update-section / replace-section / split-to-ui-file>
- Раздел: `## UI Layout`
- Размер раздела: ~N строк (inline / вынесен в `<feature>-ui.md`)

## Корректировки от implement
- **Обязательный раздел.**
- Если изменений нет — «без изменений: UI Layout опубликован as-is из `ui_layout.output`».
- Если есть — список конкретных изменений с указанием где в `implement.output` обнаружено.
```

## 6. Что НЕ делать

- Не править разделы кроме `## UI Layout`. Если у спеки есть `## State` / `## UseCase` / `## IO` — они не наша зона.
- Не «угадывать» порядок при non-canonical файле — падать с error.
- Не дописывать «история изменений UI Layout». Спека — снимок, не diff.
- Не плодить отдельные `<feature>-ui-v2.md` / `<feature>-ui-new.md` — переписывать существующий `<feature>-ui.md` либо inline.
