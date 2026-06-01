# adaptive flow — документация

Документация к [`adaptive.yml`](adaptive.yml) и его sub-flow ([`business.yml`](business.yml), [`ui.yml`](ui.yml), [`data.yml`](data.yml), [`infra.yml`](infra.yml)).

Описание архитектуры и связей шагов на момент текущего состояния overlay. Дизайн-история (зачем и как пришли к такой структуре) живёт в `docs/FlowBacklog.md` (Closed-finding'и + поле «Реализация»).

---

## Назначение

`adaptive` — основной flow PolyTrainer для разработки фич / багфиксов. Адаптируется под скоуп задачи: на шаге `scope_analysis` выставляются флаги затронутых слоёв (`infra_touched` / `business_touched` / `ui_touched` / `data_touched`), и runner запускает только нужные sub-flow.

---

## Структура master flow

```
task
  → figma_dump
  → scope_analysis
  → subflow: infra        (if: infra_touched)
  → subflow: business     (if: business_touched)
  → subflow: ui           (if: ui_touched)
  → subflow: data         (if: data_touched)
  → check                 (lint / test / build)
  → global_code_review    (Architecture / Bugs / YAGNI + triage)
```

Все `subflow:` — **inline-разворот** через директиву `subflow:` из FF base (`spec/dsl.md` → раздел «Subflow»). Шаги вложенных flow разворачиваются плоско в master plan; артефакты живут в master workspace; отдельных child plan.yml / log.md / workspace нет.

Альтернативный механизм для запуска **изолированного процесса** с feedback loop — `child_flow:` в FF base. Сейчас в adaptive не используется, оставлен для будущих use-cases.

---

## Универсальный паттерн sub-flow: Discovery → Spec → Plan

Каждый sub-flow следует структуре:

1. **Discovery** — `<layer>_walkthrough`. Грепает реальный код своего слоя, собирает **факты** (что есть, какие конвенции, какие аналоги). Без дизайн-решений.
2. **Spec** — `<layer>_contract` (только в business) / `ui_layout` (в ui) / контрактная часть design_tree (в data, infra). Описывает **что должно быть** на основе фактов и гайдов через модуль `guides`.
3. **Plan** — `<layer>_design_tree` → `<layer>_test` → `<layer>_implement` → `<layer>_summary`. Декомпозиция, тесты, реализация, итог.

Цель: контракт пишется **не теоретически, а на фактах** (см. FlowBacklog § IS479-F1). Walkthrough получает на вход `scope_analysis.output`, его output подключается в input ближайшего следующего шага (`<layer>_layout` / `<layer>_design_tree`).

---

## Business sub-flow

Полная перестройка contract-блока (после IS479):

```
business_walkthrough        — Discovery: data-API, sealed-результаты, atomicity-методы, аналоги UseCase
  → business_contract       — Spec: State + Msg + Effect/IO + UseCase в одном артефакте
  → business_contract_review — внешний reviewer (reviews: business_contract), вердикт approved / changes_requested
  → business_contract_spec  — публикация черновика спеки в feature dir
  → business_design_tree    — декомпозиция реализации
  → business_test           — тесты
  → business_implement      — код
  → business_publish_spec   — публикация финальной спеки в spec_dir
  → business_summary
```

Между `business_contract` и `business_contract_review` работает reviewer-механика из FF base (`spec/dsl.md` → «feedback_required для обычных шагов»). Если review вернул `changes_requested` — runner делает `trigger_step_rerun` на `business_contract`, цикл до approved.

Модуль `guides` включён на `business_contract` / `business_contract_review` / далее — правила моделирования State / Msg / IO подкладываются автоматически из проектных гайдов (`docs/guides/state-modeling.md`, `state-and-extensions.md`, `reducer-patterns.md`).

---

## UI sub-flow

```
ui_walkthrough     — Discovery: core/ui примитивы, существующие composable, theme, ресурсы
  → ui_layout      — Spec: финальная UI-разметка в формате ui_layout (см. formats/ui_layout.md)
  → ui_design_tree
  → ui_implement
  → publish_ui
  → ui_summary
```

`ui_layout` декларирует `format: ui_layout` в frontmatter — runner инжектит спеку формата из [`../formats/ui_layout.md`](../formats/ui_layout.md). Описание виджетов — через примитивы (atoms + layouts) из гайда `docs/guides/ui-primitives.md` (подкладывается модулем `guides`).

---

## Data sub-flow

```
data_walkthrough        — Discovery: Entity / DAO / схема / миграции / FK / CoreDbApi
  → data_design_tree    — Spec + Plan
  → data_migration_test (if: needs_migration_tests)
  → data_implement
  → data_summary
```

---

## Infra sub-flow

```
infra_walkthrough     — Discovery: DI-граф / build / manifest / ProGuard / CI
  → infra_design_tree — Spec + Plan
  → infra_test        (if: needs_tests)
  → infra_implement
  → infra_summary
```

---

## Финальная фаза

После всех sub-flow:

- **`check`** — механическая проверка: `lint` / `test` / `build`. Без агентов.
- **`global_code_review`** — финальный архитектурный обзор всей фичи. Запускает 3 параллельных subagent'а (Architecture / Bugs / YAGNI), собирает findings в `REVIEW.md` с обязательным `Verify:` через встроенные Claude tools. На паузе conductor проводит triage с пользователем: `→ закрыть в фиче` / `→ backlog` / `→ rejected`.

---

## Связь между слоями

| Откуда | Куда | Зачем |
|---|---|---|
| `scope_analysis.output` | всем `<layer>_walkthrough` | контекст скоупа |
| `business_summary.output` | `ui_layout`, `ui_design_tree`, `ui_implement`, `data_*` | UI/data знают контракт TEA-логики |
| `infra_summary.output` | `data_design_tree`, `ui_design_tree` (опционально) | знают изменения инфры |
| `<layer>_walkthrough.output` | ближайший следующий шаг своего слоя | фактологический фундамент |

Nullable inputs (FF base) — отсутствующий шаг (skipped по `if:`) даёт `null` в input. Потребители обрабатывают null как «слой не запускался». Явный маркер `?` в input допустим как читаемость.

---

## Модули

`adaptive.yml` подключает (через `modules:`):
- `logging` — лог в `log.md` на `finalize:after` каждого шага.
- `review` — reviewer-механика (опционально включается на шаге через `with: review: agents:`). По дефолту прогоняет inquisitor; можно выключить опцией `inquisitor: false`.
- `guides` — подкладывает проектные гайды на шаги с `with: guides: enabled: true`.

---

## Связанные документы

- [`docs/FlowBacklog.md`](../../FlowBacklog.md) — журнал finding'ов и реализаций.
- [`docs/guides/`](../../guides/README.md) — проектные гайды (правила моделирования, паттерны, конвенции).
- `~/dev/forgeflow/spec/dsl.md` — формальный DSL ForgeFlow (subflow / child_flow / repeat / require_clean_iteration / format / feedback_required).
- `~/dev/forgeflow/spec/runner.md` — псевдокод runner'а.
