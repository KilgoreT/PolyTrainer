---
name: figma_dump
output: figma_dump.json
input: task.output
output_criteria:
  - если `feature_has_figma=true` — файл `figma_dump.json` существует в корне фичи (`plan.dir`)
  - если `feature_has_figma=false` — артефакт отсутствует, output не публикуется
  - сделано не более ОДНОГО MCP-вызова `get_figma_data` за весь шаг
context_output:
  - feature_has_figma
---

Единственный шаг flow, которому разрешено лезть в Figma. Скачивает структуру файла **одним запросом** и сохраняет в корне фичи. Все последующие шаги (scope_analysis, ui_layout, design_tree, implement и т.д.) обязаны читать **только** этот дамп — никаких собственных MCP-вызовов.

## Контракт

- **Только один MCP-вызов** на весь шаг: `mcp__figma__get_figma_data(fileKey)` без `nodeId` (получает структуру всего файла).
- Output — `figma_dump.json` в `plan.dir` (корень фичекаталога), не в подпапке `ui/` / `business/`.
- Переменная `feature_has_figma` в `context_output`:
  - `true` — Figma URL / fileKey найден в брифе и дамп успешно сохранён.
  - `false` — Figma не упомянута в брифе, дамп не нужен.

## Алгоритм

### 1. Прочитать бриф

`plan.dir/00_task.md` — найти упоминания Figma:
- Полный URL `figma.com/design/<fileKey>/...` или `figma.com/file/<fileKey>/...`.
- Прямое указание `fileKey: <id>`.
- Список `node-id` в контексте — сигнал что Figma подразумевается, искать `fileKey` рядом.

### 2. Решение

#### Case A — Figma в брифе нет

- `feature_has_figma: false`.
- Output не создаётся.
- Шаг завершён.

#### Case B — Figma URL / fileKey найден

- Извлечь `fileKey` из URL (часть после `/design/` или `/file/`).
- Один вызов `mcp__figma__get_figma_data(fileKey)` без `nodeId`. Если файл крупный — добавить параметр `depth` (например 8) чтобы уместить ответ.
- Сохранить полный JSON-ответ в `plan.dir/figma_dump.json`.
- `feature_has_figma: true`.

### 3. Обработка ошибок

- **429 Rate Limit** — НЕ retry, НЕ Mode B. Падать с явной ошибкой: "Figma quota exceeded, дамп не получен. Повторить шаг после сброса лимита." Шаг fail, master flow паузится.
- **Сетевая ошибка / 5xx** — fail с описанием.
- **Невалидный fileKey** — fail с указанием того fileKey что пытались использовать.

Никаких "тихих fallback'ов" к Mode B быть не должно. Дамп либо есть, либо шаг fail.

## Контракт для зависимых шагов

Любой шаг flow, использующий Figma-данные, обязан:

1. Принимать `figma_dump.output?` в input.
2. Читать `feature_has_figma` из context.
3. Если `feature_has_figma=true` **и** файла `figma_dump.json` нет на диске → fail с явной ошибкой "ожидался figma_dump.json в корне фичи, но не найден; проверь шаг figma_dump".
4. Если `feature_has_figma=true` **и** файл есть → парсить локально, никаких MCP-вызовов.
5. Если `feature_has_figma=false` → работать без Figma-данных (Mode B по существу), не пытаться обратиться к MCP.

Любой MCP-вызов вне шага `figma_dump` — **нарушение протокола**.

## Что НЕ делать

- НЕ делать дополнительных MCP-вызовов (точечные `get_design_context`, повторные `get_figma_data` с разными nodeId).
- НЕ скачивать иконки на этом шаге — это работа отдельного шага позже (на основе списка из ui_layout).
- НЕ парсить дамп — это работа зависимых шагов.
- НЕ принимать решения о скоупе фичи — это работа `scope_analysis`.
