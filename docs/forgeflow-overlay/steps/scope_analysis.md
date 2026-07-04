---
name: scope_analysis
output: 02_scope.md
input_criteria:
  - task.md существует
  - если `feature_has_figma=true` → `figma_dump.json` существует в `plan.dir`
output_criteria:
  - 02_scope.md существует
  - 02_scope.md содержит секцию Замысел задачи
  - 02_scope.md содержит секцию Spec target
  - перечислены затронутые слои с обоснованием
  - перечислены аспекты
  - перечислены затронутые файлы
  - указан список sub-flow к запуску
  - 02_scope.md содержит секцию `## Open questions` — список неоднозначностей с best-guess + альтернативами (либо явное «нет open questions» если всё однозначно)
context_output:
  - infra_touched
  - business_touched
  - ui_touched
  - data_touched
  - needs_tests
  - needs_migration_tests
  - feature_has_ui_contract
  - spec_filename
# post_finalize_hook: archive_subflows  # legacy child_flow механика — не нужна при inline-разворот subflow.
---

Прочитай бриф из `00_task.md`. Глубоко проанализируй задачу — определи какие архитектурные слои затронуты. На основе этого master flow решит какие sub-flow запускать.

## Figma-данные

**MCP-вызовы к Figma запрещены на этом шаге.** Источник Figma-структуры — единственный артефакт `figma_dump.json` в `plan.dir` (создаётся отдельным шагом `figma_dump`).

- Если `feature_has_figma=true` **и** `figma_dump.json` присутствует в `plan.dir` → парсить его локально для определения скоупа UI.
- Если `feature_has_figma=true` **и** `figma_dump.json` отсутствует → **fail** с явной ошибкой "ожидался figma_dump.json в plan.dir, но не найден; проверь шаг figma_dump". Не пытайся восполнить через MCP.
- Если `feature_has_figma=false` → Figma не используется, работай без неё.

## Алгоритм

### 1. Концептуальный mapping

Из брифа извлеки **домен** (словарь, квиз, статистика, настройки) и **поведение** (создание, удаление, отображение, миграция). Замапь на потенциальные модули и слои:

- Домен → `modules/screen/<domain>*/`, `modules/widget/<domain>*/`
- Поведение → функции в контрактах (`delete*`, `flow*`, `get*`, `migrate*`)
- Стектрейс (если есть) → разбери каждую строку: точка падения + цепочка вызова

Бриф редко содержит сигнатуры классов. Восстанавливай по смыслу — модель справится.

### 2. Grep по коду

Для найденного концептуально:
- Определения контрактов и use case'ов
- Места использования (потребители контракта)
- **Близнецы дефекта** для bug-кейсов: тот же паттерн в других модулях

### 3. Чтение спек и гайдов

- `docs/handbook/specs/<feature>.md` — спека релевантной фичи
- `docs/handbook/guides/<subsystem>.md` — гайды затронутых подсистем (mate-framework, dagger-di, navigation, prefs-datastore, testing-…)

### 4. Анализ зависимостей

- Какие модули зависят от затронутого (через `build.gradle.kts`)
- Имеет ли затронутый файл публичный контракт (`*/deps/`)
- Что сломается при изменении контракта

### 5. Классификация

**Затронутые слои** (4 булевы переменные в `context_output`):
- `infra_touched` — DI граф, mate framework, logger, navigation infra, core.ui/theme, Build/ProGuard, Manifest, CI/CD, Guides
- `business_touched` — TEA-логика (State/Msg/Reducer/Effect), UseCase контракты+impl, доменные сущности, Specs
- `ui_touched` — composables, layouts, виджеты, ресурсы
- `data_touched` — DB (api+impl+миграции+schemas), Prefs, Library-обёртки

**Дополнительные флаги** (булевы переменные в `context_output`):
- `needs_tests` — нужны ли unit-тесты (Reducer, UseCaseImpl). Для тривиальных Infra-фиксов (ProGuard rule, Manifest entry) — false.
- `needs_migration_tests` — нужны ли миграционные тесты (только при `RoomDatabase.version++` + изменении schema).
- `feature_has_ui_contract` — есть ли у фичи UI-контракт (State/Msg/Reducer + Composable). True для UI-экранов / виджетов со своим reducer'ом. False для headless-фич (background sync, scheduler, periodic jobs). Управляет запуском контракт-блока в business sub-flow.

**Имя файла существующей спеки** (`spec_filename` в `context_output`, строковая переменная):
- Если для этой фичи **уже есть спека** в `docs/handbook/specs/` — `spec_filename: <имя>.md` (например `dictionary-list.md`). Найди через grep по `handbook/specs/README.md` + по упомянутым модулям.
- Если **нет спеки** (новая фича / legacy без миграции / headless / `business_touched=false`) — `spec_filename: null`.

Имя файла для **новой** спеки выбирает шаг `contract_spec` сам, когда контракт уже описан — scope_analysis имя не выбирает.

**Аспекты** (только текстом в `02_scope.md`, не в context_output): `public_contract_change`, `cross_tab_subscription`, `db_migration`, `production_crash`, `release_only_bug`, `new_dependency` и т.п.

### 6. Решение по sub-flow

На основе слоёв определи какие sub-flow master запустит:
- Порядок фиксирован: Infra → Business → UI ∥ Data
- Sub-flow с `<layer>_touched = false` пропускается через `if:` в master flow

## Feedback iteration (только при `plan.context.feedback_iteration > 0`)

Если в `plan.dir` есть файл `02_feedback.md` — это значит предыдущая итерация дала feedback от sub-flow. **Обязательно прочитай** перед анализом.

Формат `02_feedback.md` (создаётся runner'ом через `trigger_feedback_loop`):

```markdown
# Feedback от sub-flow

## <subflow_name>

**Reason:** <конкретная причина почему scope некорректен>

**Suggested changes:**
<yaml-блок с подсказками>
```

В массиве может быть несколько секций `## <subflow_name>` — это значит несколько sub-flow в parallel вернули feedback одновременно. Учти **все**.

Дополнительно прочитай артефакты предыдущей итерации:
- `02_scope_iter{N}.md` (где N = `feedback_iteration`) — предыдущий scope
- Workspace'ы завершённых sub-flow прошлой итерации (`<name>_iter{N}/summary.md`)

Учти feedback в новой классификации — поправь `<layer>_touched` флаги так чтобы покрыть упущенное.

## Формат `02_scope.md`

```markdown
# Scope analysis: <ticket>

## Резюме

<одним абзацем — твоё понимание задачи>

## Затронутые слои

- **Infrastructure** — да/нет — <обоснование если да>
- **Business logic** — да/нет — <обоснование если да>
- **UI** — да/нет — <обоснование если да>
- **Data** — да/нет — <обоснование если да>

## Аспекты

- `<aspect_name>` — <обоснование>
- ...

## Затронутые файлы

С обоснованием почему каждый в скоупе. Включая близнецов дефекта (для bug-кейсов).

## Релевантные спеки и гайды

Список со ссылками на файлы и конкретные секции.

## Sub-flow для запуска

| Sub-flow | Запускать? | Обоснование |
|---|---|---|
| Infrastructure | да/нет | ... |
| Business | да/нет | ... |
| UI | да/нет | ... |
| Data | да/нет | ... |

## Open questions

(перечень неоднозначностей которые sub-agent не смог решить однозначно из брифа; если всё однозначно — явно написать «нет open questions»)

- **<кратко вопрос>** — best-guess: <вариант агента>; альтернативы: <вариант B>, <вариант C>. Обоснование best-guess: <одна строка>.
- ...
```

## Что вернуть в `context_output`

В ответе агента должны быть установлены **все переменные из frontmatter `context_output`**:

```yaml
infra_touched: <bool>
business_touched: <bool>
ui_touched: <bool>
data_touched: <bool>
needs_tests: <bool>
needs_migration_tests: <bool>
feature_has_ui_contract: <bool>
spec_filename: <string|null>
```

Без них master flow не сможет правильно решить какие sub-flow запускать и какие зависимости тянуть.

## Что НЕ делать

- **Не спрашивай пользователя в середине шага** (через `inquest` / pause). Всё неоднозначное — собирается в секцию `## Open questions` артефакта `02_scope.md` с явным best-guess + альтернативами. Pause после шага = review пользователем, где он либо принимает best-guess либо переписывает. Без зацикленных rerun'ов на угадывание.
- **Не пиши код**, не правь файлы.
- **Не делай design-решений** по конкретным слоям — это работа sub-flow.
- **Не дублируй research** который sub-flow сделают сами по своему слою. Твоя задача — классификация уровня «затронут / не затронут», не глубокая проработка.

## Архитектурное следствие

Sub-flow получат на вход подготовленный контекст и **не повторят** research на своём уровне. Поэтому работай тщательно — точность классификации критична для всей цепочки.
