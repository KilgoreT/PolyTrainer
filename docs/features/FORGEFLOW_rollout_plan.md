# ForgeFlow — план внедрения адаптивного flow

Применение [`FORGEFLOW_design.md`](FORGEFLOW_design.md) §9 в существующий ForgeFlow framework. Цель — внедрить адаптивный flow `lexeme_adaptive` без поломки `lexeme_bugfix`.

---

## Базовый принцип

**Backward compatibility — обязательна.** lexeme_bugfix должен работать как раньше после каждого этапа. Новый flow `lexeme_adaptive` сосуществует с ним.

Все изменения runner'а активируются ТОЛЬКО для шагов с `step.type == "subflow"` или при `plan.context.feedback_iteration > 0`. lexeme_bugfix эти ветки не задевает.

---

## Этап 1. Spec-документация

**Риск:** низкий. **Зависимости:** нет.

Физически применить §9 к существующим spec-файлам ForgeFlow.

### 1.1. `docs/forgeflow/spec/dsl.md`

Добавить:
- **Раздел Subflow** (между Group и Parallel):
  - Поля: `subflow:<name>`, `flow:<path>`, `workspace:<subdir>` (опц., default `<name>/`), `input:`, `if:`, `with:`, `pause:`
  - Output фиксирован = `<workspace>/summary.md`
  - Запрет сочетания с `repeat`, `sets`, `output`, `model`
- **Раздел Input → Optional input** — суффикс `?` на input-ref (`step.output?`)
- **Раздел Mode** — одна строка: «mode устанавливается в planning() master flow и наследуется в child sub-flow»
- **Раздел Статусы шага** — добавить `feedback_required` (только для subflow-шагов), описать переходы
- **Раздел Ограничения DSL** — добавить пункт «Глубина subflow — 1 уровень»

### 1.2. `docs/forgeflow/spec/runner.md`

Переписать / расширить функции (полный список — §9.2 design doc):

- `main(args)` — добавить args + do-while loop на `feedback_restart`
- `select_flow(config)` — нумерованный список (уже частично сделано)
- `planning(flow, config, args={}, inherited={}, dir_override=null)` — новая сигнатура
- `resolve_steps()` — ветка subflow + валидации (OR в if, запрет output/model, depth)
- `run()` — диспатч subflow + проверка `feedback_required` + skip flow_start при `feedback_restart`
- **Новая `execute_subflow(step, master_plan, master_phase_modules)`** — фазовый контракт + 13-шаговый порядок
- **Новая `trigger_feedback_loop(plan, failing_step)`** — phase 1 архивирования
- **Новая `archive_and_reset_subflows_after_scope(plan)`** — phase 2 (вызывается через `post_finalize_hook`)
- `find_plan(config)` — переработка: parent-фильтр + mtime fallback + subflow-traversal
- `resolve_ref(ref, plan)` — обработка суффикса `?`
- `resolve_input(step, plan)` — фильтр null'ов
- `execute_parallel()` — skip done/skipped + dispatch by type + aggregation с приоритетом error>feedback>done
- `resume(plan, phase_modules, start_from=null)` — условный сброс in_progress + feedback_required scan
- **Новый примитив `ask_numbered(question, options)`** — нумерованный список с «ввести своё»
- **Поддержка `post_finalize_hook`** в `run_finalize` — встроенная логика проверки frontmatter маркера
- **Раздел Plan** — добавить опциональные поля: `parent`, `context.depth`, `context.feedback_iteration`, `context.feedback_restart` + read-time defaults для backward compat

### 1.3. `docs/forgeflow/agents/embedded/conductor.md`

Добавить разделы:
- **Subflow context** — переключение master↔child
- **Subflow → logging** — master log = timeline + ссылка на child log
- **Subflow → modules** — независимость
- **ЖЕЛЕЗНОЕ ПРАВИЛО — глубина 1** — error на nested subflow
- **Feedback loop** — read-once summary, `feedback_required` штатный сигнал
- **Mode при resume** — наследуется из plan.context
- **Шаблон plan.yml** — пример subflow-шага + поля parent/depth/feedback_iteration

### Критерий завершения Этапа 1

- `lexeme_bugfix.yml` запускается и проходит без ошибок (smoke-test)
- Старый IS476 plan.yml resume'ится через новый runner — все шаги done, никаких ошибок

---

## Этап 2. Step-файлы для адаптивного flow

**Риск:** средний (требует обдуманных промптов). **Зависимости:** Этап 1.

### 2.1. Ключевой файл — `steps/scope_analysis.md`

Frontmatter:
```yaml
---
name: scope_analysis
output: 02_scope.md
context_output: [infra_touched, business_touched, ui_touched, data_touched, needs_tests, needs_migration_tests]
post_finalize_hook: archive_subflows
---
```

Body: промпт по §1.2 design doc — concept mapping, grep, чтение спек/гайдов, классификация по 4 слоям, чтение `02_feedback.md` при наличии (для feedback loop iter 2+).

### 2.2. Step-файлы для sub-flow

Создать новые step-файлы под каждый sub-flow (см. §3-§6 design doc):

- `steps/test_design_tree.md` — генерация дерева тестов
- `steps/code_design_tree.md` — генерация дерева кода
- `steps/tests.md` — TDD реализация
- `steps/migration_test_design_tree.md` — для Data
- `steps/migration_tests.md` — для Data
- `steps/summary.md` — финальный артефакт sub-flow

### 2.3. Контракт для Business

Когда определим детали контракта (отложено) — добавить:
- `steps/contract.md` — для шага `contract` в Business sub-flow

Существующие step-файлы (`task.md`, `update_spec.md`, `publish_spec.md`, `check.md`, `design_tree.md`, `implement.md`) — переиспользуем как есть.

### Критерий завершения Этапа 2

Все step-файлы открыты и компилируются (синтаксически валидный markdown + frontmatter).

---

## Этап 3. Flow-файлы

**Риск:** низкий (декларативные YAML). **Зависимости:** Этапы 1, 2.

Создать:

- `flows/lexeme_adaptive.yml` — master flow (§1.3 YAML)
- `flows/infra.yml` — Infra sub-flow (§3)
- `flows/business.yml` — Business sub-flow (§4)
- `flows/ui.yml` — UI sub-flow (§5)
- `flows/data.yml` — Data sub-flow (§6)

Каждый flow декларирует свои `modules:` (см. §2.1 контракт).

### Критерий завершения Этапа 3

- `select_flow` показывает `lexeme_bugfix` и `lexeme_adaptive` в списке
- `lexeme_adaptive` можно выбрать, planning отрабатывает, доходит до scope_analysis (без implement'а)

---

## Этап 4. Прочее (опционально, на вырост)

**Риск:** низкий. **Зависимости:** Этапы 1-3.

- `forgeflow.yml` — добавить `commands.build_release` если нужны release-only checks
- `BOOTSTRAP.md` — упомянуть subflow концепцию
- `README.md` фреймворка — обновить (по необходимости)

---

## Этап 5. Тестирование

**Риск:** высокий — здесь всплывают пропущенные edge cases.

### 5.1. Backward compatibility

- **Старый plan resume.** Открыть IS476 plan.yml, выполнить `find_plan` + `resume()` → должен корректно понять что всё done, выйти без ошибок.
- **Lexeme_bugfix smoke.** Создать новый тикет, запустить через `lexeme_bugfix`, пройти все 11 шагов до конца → результат идентичен текущему поведению.

### 5.2. Adaptive happy path (без feedback)

- Простой UI fix (типа IS453). Запустить через `lexeme_adaptive`.
- scope_analysis выставляет `ui_touched=true`, остальные false.
- Infra/Business/Data пропускаются через `if:`.
- UI sub-flow проходит: design_tree → implement → summary.
- `check` зелёный.

### 5.3. Adaptive с feedback loop

- Кейс типа IS474 (data-driven). Запустить через `lexeme_adaptive`.
- iter1: scope_analysis выставляет `business_touched=true`. Business sub-flow начинается, в `contract` шаге понимает что нужно менять prefs → возвращает `feedback_required` с reason.
- master: `trigger_feedback_loop` → архивирование 02_scope.md → `02_scope_iter1.md`, scope_analysis.status = pending, feedback_restart = true.
- main() do-while: run() заново. scope_analysis читает `02_feedback.md`, теперь выставляет `data_touched=true`, `business_touched=true`. Hook archive_subflows: business workspace → `business_iter1/`, data workspace создан.
- Data sub-flow выполняется. Business sub-flow перезапускается с новым контекстом → done. UI∥Data (но UI skipped через if). check зелёный.

### 5.4. Глубина 1 violation

- Создать child flow содержащий `subflow:` шаг.
- Запустить — runner должен упасть с error на `resolve_steps` (статическая проверка `visited not empty + subflow`).

### Критерий завершения Этапа 5

Все 4 сценария отработаны без неожиданностей. Возникшие баги — пофиксены или задокументированы как known issues.

---

## Стратегия rollback

На каждом этапе:
- Все изменения коммитятся в отдельные feature-ветки (`forgeflow/etap-1-spec`, `forgeflow/etap-2-steps`, и т.д.).
- Slack-checkpoint после Этапа 1 — проверка smoke-теста backward compat. Если ломается — откатить ветку.
- Не мержить в master пока все 5 этапов не прошли.

---

## Открытые вопросы (можно решать в процессе)

1. **`contract` шаг Business sub-flow** — детали отложены. Решаем во время Этапа 2 когда дойдём.
2. **Спеки в Business** — `update_spec` / `publish_spec` переиспользуются как есть, но могут потребовать adjustment'ов под адаптивный flow.
3. **forgeflow.yml `build_release`** — добавлять или нет — решаем на Этапе 4.

---

## Оценка трудозатрат (приблизительно)

| Этап | Сложность | Время |
|---|---|---|
| 1. Spec docs | Высокая (аккуратность) | 4-6 ч |
| 2. Step-файлы | Средняя (промпт-инжиниринг) | 3-5 ч |
| 3. Flow-файлы | Низкая (YAML) | 1-2 ч |
| 4. Прочее | Низкая | 0.5-1 ч |
| 5. Тестирование | Непредсказуемая | 2-6 ч (зависит от багов) |
| **Итого** | — | **10-20 часов** |

Реалистично — 2-3 рабочих сессии. Не за один раз.

---

_План создан после 5 раундов ревью дизайн-документа. Все архитектурные решения зафиксированы в [`FORGEFLOW_design.md`](FORGEFLOW_design.md). Все механики (sub-flow invocation, feedback loop, archive, resume) детально описаны._
