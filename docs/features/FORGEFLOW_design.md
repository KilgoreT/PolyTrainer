# ForgeFlow — детали реализации адаптивного flow

Документ детализирует архитектуру из [`FORGEFLOW_scope_analysis.md`](FORGEFLOW_scope_analysis.md) §6. Решения по реализации: структура файлов, состав шагов, механики invocation/feedback.

---

## 0. Pre-flow: выбор flow

До запуска master flow conductor показывает пронумерованный список всех зарегистрированных flow и ждёт выбор номера. Без явного выбора master flow не запускается, даже если flow подразумевается из команды.

Это **не step** — это pre-flow действие conductor'а, выполняется ДО появления `plan.yml`. Артефакта нет (выбор фиксируется в `plan.yml.context` через `planning()`).

Реализовано в `spec/runner.md → select_flow()`.

---

## 1. Master flow

### 1.0. Стартовый контекст (через `planning()`)

Стартовый контекст собирается встроенной процедурой `planning()` в `spec/runner.md` — не отдельным step'ом. `dir = .../{ticket}_{name}/` зависит от собранных полей, поэтому `plan.yml` не может быть создан раньше, чем сбор контекста завершён.

**Что собирается:**

- `ticket` — идентификатор задачи (например `IS476`)
- `feature_name` — короткое название (snake_case, например `delete_all_dictionaries_crash`)
- `branch` — рабочая ветка
- `mode` — `manual` / `normal` / `autonomy`:
  - `manual` — пауза после КАЖДОГО шага
  - `normal` — пауза только на шагах с `pause: true`
  - `autonomy` — без пауз
- Дополнительные поля из `flow.context` (если есть)

**Контракт `planning()`:**

1. Pre-fill из аргументов команды. Поле, переданное как arg, не спрашивается.
2. **`mode` спрашивается ВСЕГДА**, даже если pre-fill. Режим критически влияет на pause-семантику flow, ошибочный pre-fill дорог.
3. Нумерованный формат для multi-choice вопросов (mode и др.), с опцией «ввести своё» последним пунктом.
4. Free input для свободных полей (ticket, feature_name, branch).
5. Результат фиксируется в `plan.yml.context`.

**Реализовано в `spec/runner.md`:** `main(args)`, `select_flow()`, `planning(flow, config, args)`, примитив `ask_numbered(question, options)`.

### 1.1. Структура и шаги

```
Master flow
├── 1. task
├── 2. scope_analysis             [+ review module: architect, analyst]
├── 3. run_subflows
│       │
│       ├── Infra sub-flow        → infra/summary.md
│       │       │
│       │       ▼
│       ├── Business sub-flow     → business/summary.md
│       │       │
│       │       ▼
│       │   ┌───┴───┐
│       │   ▼       ▼
│       └── UI ∥  Data            → ui/summary.md, data/summary.md
│
└── 4. check
```

Sub-flow в `run_subflows` запускаются только если соответствующий слой затронут (по результату scope_analysis). Если Infra не затронута — Business идёт первым. Если UI не затронут — Data идёт одна.

Master flow — **4 шага**:

1. **task** (`pause: true`) — формализация брифа в `00_task.md`.
2. **scope_analysis** (`pause: true`) — глубокий анализ задачи. Подробности — §1.2.
3. **run_subflows** — динамический набор вызовов sub-flow (1–4) в порядке, определённом scope_analysis. Сам шаг без `pause`; паузы — внутри sub-flow.
4. **check** (`pause: true`) — переиспользуем существующий `docs/forgeflow/steps/check.md` с `with.checks: [lint, test, build]`. Команды берутся из `forgeflow.yml → commands`.

**Все 4 шага выполняются всегда**, без skip-логики. UI-only фикс пройдёт через те же 4 шага включая review над scope_analysis. Предсказуемость важнее оптимизации.

> **Note:** интеграция модуля `checklist` (`checklist_init` + `checklist_run` + активация на subflow-шагах) отложена — см. [`FORGEFLOW_checklist_integration.md`](FORGEFLOW_checklist_integration.md) (§ Status: ПРОКЛЯТО). v1 адаптивного flow идёт без чеклиста.

### 1.2. Шаг `scope_analysis` — алгоритм

**Вход:** `00_task.md` + полный доступ к репозиторию.

**Review:** модуль `review` на фазе `review:after`, `agents: [architect, analyst]`. Architect — структурная корректность анализа; analyst — корректность понимания задачи. Прочие кандидаты (`qa_engineer`, `product_reviewer`, `security_reviewer`) — на будущее, per-task swap в текущем ForgeFlow не предусмотрен.

**Что делает агент:**

1. **Концептуальный mapping** — читает бриф и мапит его понятия (домен + поведение) в потенциальные модули и слои:
   - Домен из брифа («словарь», «квиз», «статистика») → модули `modules/screen/<domain>*/`, `modules/widget/<domain>*/`
   - Поведение из брифа («удаление», «отображение», «обновление») → функции в контрактах (`delete*`, `flow*`, `get*`)
   - Бриф редко содержит сигнатуры классов/методов — это нормально, модель восстанавливает по смыслу.

2. **Grep по коду** — для всего что нашлось концептуально:
   - Определения найденных контрактов и use case'ов
   - Все места использования (потребители контракта)
   - Если в брифе есть стектрейс — разобрать каждую строку (точка падения + цепочка)
   - Найти близнецов дефекта (тот же паттерн в других модулях)

3. **Чтение спек и гайдов:**
   - `docs/features-spec/<feature>.md` — спека релевантной фичи
   - `docs/guides/<subsystem>.md` — гайды затронутых подсистем (mate, dagger-di, navigation, prefs-datastore, testing-…)
   - Кросс-ссылки между спеками (user journey может быть размазан)

4. **Анализ зависимостей:**
   - Какие модули зависят от затронутого (через `build.gradle.kts`)
   - Имеет ли затронутый файл публичный контракт (`*/deps/`)
   - Что сломается при изменении контракта

5. **Классификация по слоям и аспектам** (на основе §1 / §4 `FORGEFLOW_scope_analysis.md`):
   - Какие из 4 слоёв реально затронуты (UI / Business / Data / Infra)
   - Какие аспекты активны (public_contract_change, cross_tab_subscription, db_migration, production_crash, …)

6. **Решение по sub-flow:**
   - Какие sub-flow запускать (1–4 из 4 возможных)
   - В каком порядке (Infra → Business → UI∥Data; пустые слои пропускаются)
   - Что передать каждому sub-flow на вход (контекст, файлы, спеки)

**Что НЕ делает scope_analysis:**

- Не спрашивает пользователя об уточнениях. Модель должна разобраться сама из брифа + кода. Если задача настолько мутная, что без вопросов нельзя — это проблема брифа, чинится переделкой `00_task.md`, не интерактивом внутри scope_analysis.
- Не пишет код. Не правит файлы.
- Не делает design / implementation решения по конкретным слоям — это работа sub-flow.

**Выход:** артефакт scope_analysis (например `02_scope.md`) содержит:

- Резюме понимания задачи (одним абзацем)
- Список затронутых слоёв с обоснованием
- Список аспектов с обоснованием
- Список конкретных файлов и почему они в скоупе (включая близнецов дефекта)
- Релевантные секции спек/гайдов (со ссылками)
- Список sub-flow к запуску, порядок, что передать каждому

**Архитектурное следствие:** sub-flows получают на вход подготовленный контекст и **не повторяют** research на своём уровне. Sub-flow сразу переходит к работе своего слоя.

### 1.3. Master flow в YAML

```yaml
name: lexeme_adaptive
description: "Адаптивный flow Lexeme — мастер + 4 sub-flow по слоям"

modules:
  - logging
  - review

steps:
  - step: task
    pause: true
    output: 00_task.md

  - step: scope_analysis
    pause: true
    input: task.output
    output: 02_scope.md
    sets: [infra_touched, business_touched, ui_touched, data_touched, needs_tests, needs_migration_tests]
    with:
      review:
        agents: [architect, analyst]

  - subflow: infra
    flow: flows/infra.yml
    if: infra_touched
    input: scope_analysis.output

  - subflow: business
    flow: flows/business.yml
    if: business_touched
    input:
      - scope_analysis.output
      - infra.output?           # суффикс `?` = optional: skip если infra пропущена

  - parallel:
      - subflow: ui
        flow: flows/ui.yml
        if: ui_touched
        input:
          - scope_analysis.output
          - business.output?    # optional — Business может быть skipped

      - subflow: data
        flow: flows/data.yml
        if: data_touched
        input:
          - scope_analysis.output
          - business.output?    # optional — Business может быть skipped

  - step: check
    pause: true
    with:
      checks: [lint, test, build]
```

**Ключевые места:**

- `scope_analysis.sets` — список булевых context-переменных, которые шаг устанавливает. Сами устанавливаются агентом в результате анализа.
- `if: <flag>` на subflow — пропускает sub-flow если слой не затронут.
- **Суффикс `?` на input-ссылке** (`infra.output?`) — optional: если шаг пропущен (skipped), ссылка возвращает empty/null, ошибки нет. Без `?` — стандартное поведение, ссылка на не-`done` шаг падает с ошибкой.
- `parallel:` оборачивает UI и Data — выполняются одновременно после Business.
- `subflow:` — новый step type (см. §1.4), реализация в `spec/runner.md`.

### 1.4. Шаг запуска sub-flow

**Sub-flow == обычный flow**, просто запускаемый из другого flow. Не отдельная сущность. Полный контракт sub-flow (структура, наследование, modules, summary) — в **§2.1**.

#### Почему не `group: flow:`

Существующий механизм `group:` с `flow: <path>` делает **inline-разворачивание** шагов дочернего flow в master `plan.yml`. Это repurpose-кусков, не независимое выполнение. Не подходит:

- Артефакты лежат вперемешку в одной директории
- Нет своего `plan.yml` у sub-flow → нельзя resume отдельно
- Feedback loop нечем реализовать (нет точки возврата)

#### Новый step type `subflow`

```yaml
- subflow: business
  flow: flows/business.yml
  input:
    - scope_analysis.output
```

**Поля:**
- `subflow: <name>` — обязательное, имя = ключ в plan.steps
- `flow: <path>` — обязательное, путь к flow-файлу
- `workspace:` — опциональное, default = `<name>/` (от plan.dir)
- `input:` — опциональное, ссылки на upstream-output'ы (с поддержкой `?`)
- `if:` / `with:` / `pause:` — опциональные, стандартные DSL-поля
- Output **фиксирован** = `<workspace>/summary.md`, не настраивается

**Семантика invocation:**

- Master запускает рекурсивный `main()` для child flow
- Workspace дочернего flow: `master.dir + workspace`
- После завершения child master читает `<workspace>/summary.md` и продолжает следующий шаг
- Если `summary.status == feedback_required` — master запускает feedback loop (§8)

#### Цепочка summary между sub-flow в master flow

```
scope_analysis.output → Infra sub-flow → infra/summary.md
                                              ↓
scope_analysis.output + infra/summary.md → Business sub-flow → business/summary.md
                                                                       ↓
scope_analysis.output + business/summary.md → UI sub-flow  ∥ Data sub-flow
```

Цепочка задаётся `input:` на каждом subflow-шаге master flow (см. §1.3 YAML). Downstream subflow получает `summary.md` upstream'ов как обычные input-файлы.

#### Optional input — суффикс `?`

Если sub-flow зависит от upstream'а, который может быть пропущен (`if:` = false), input-ссылка маркируется суффиксом `?`:

```yaml
input:
  - scope_analysis.output       # обязательно
  - infra.output?               # optional — skip если infra была пропущена
```

**Поведение `resolve_ref`:**
- Ссылка без `?`: шаг должен быть в статусе `done`, иначе ошибка (как сейчас).
- Ссылка с `?`: если шаг `done` — читается как обычно; если `skipped` — возвращается `null`/empty, ошибки нет; если другие статусы (`error`, `in_progress`) — ошибка.

#### Что нужно добавить в ForgeFlow

1. **`spec/dsl.md`** — описать step type `subflow` с полями `flow`, `workspace`, `input`, `output`. Описать суффикс `?` на input-ref.
2. **`spec/runner.md`:**
   - Новый хендлер `execute_subflow(step, plan, phase_modules)`. Параметр `phase_modules` здесь — **master'овский**: фазы шага subflow (prepare/finalize/...) обслуживаются master'овскими модулями. Child's `phase_modules` хендлер строит сам через `resolve_modules(child_flow)` для исполнения внутри child.
   - Контракт наследования context (master → child) — см. §2.1
   - `find_plan()` — корректно навигировать между master и child plan'ами (контекст «какой plan текущий»)
   - `resolve_ref` — обработка суффикса `?` для optional input
3. **`agents/embedded/conductor.md`** — описать поведение для шага `subflow`: где живёт текущий plan, как переключаться между master / child контекстами.

---

## 2. Sub-flow: общий контракт

### 2.1. Контракт sub-flow

Sub-flow — обычный flow ForgeFlow, запускаемый из master flow через step type `subflow` (механика invocation — §1.4). Все sub-flow следуют единому контракту.

#### Структура файлов

Каждый sub-flow имеет:
- `flows/<name>.yml` — описание flow (modules, steps)
- `<workspace>/plan.yml` — состояние выполнения
- `<workspace>/log.md` — лог выполнения
- `<workspace>/<step>.md` — артефакты шагов
- `<workspace>/summary.md` — обязательный output (см. ниже)

Где `<workspace>` — поддиректория от `master.dir`. По умолчанию = `<subflow_name>/`. Можно явно переопределить через поле `workspace:` на subflow-шаге master flow.

#### Workspace и изоляция

- Полная изоляция: master не лезет внутрь workspace sub-flow, читает только `summary.md`
- Если master нужно что-то из внутренних артефактов sub-flow — sub-flow обязан включить это в summary (со ссылкой)
- Master plan не наследует артефакты child напрямую

#### Context inheritance

Sub-flow `planning()` наследует из `master.context`:
- `ticket`
- `feature_name`
- `branch`
- `mode`
- `overlay`

Sub-flow **НЕ имеет** собственного `flow.context` с пользовательскими полями. Доп. параметры приходят либо из наследования, либо через `input:`. Если sub-flow «нужен дополнительный параметр» — он должен быть в `scope_analysis.output` или в проектной конфигурации, не в диалоге с пользователем.

#### Modules

Каждый sub-flow декларирует свои `modules:` в своём `flows/<name>.yml`. **Master'овские модули НЕ наследуются.**

Причины:
- **Изоляция scope'а модулей.** Master'овский `review` настроен для `scope_analysis`. Внутри sub-flow может быть другой review-конфиг — наследование требовало бы override-механизма.
- **Своё логирование.** Каждый sub-flow пишет в свой `log.md` — модуль `logging` нужен явно в каждом flow.

Пример объявления:

Master flow:
```yaml
modules:
  - logging
  - review
```

Business sub-flow:
```yaml
modules:
  - logging              # свой log.md в business/
  - review               # для своих шагов с reviewer'ами
  - guides               # если sub-flow затрагивает гайды
```

Infra sub-flow:
```yaml
modules:
  - logging
  # review не нужен — простой
```

Дублирование `logging` везде — boilerplate в 1 строку, приемлемая цена за явность. **DSL-изменений не требуется** — sub-flow читается как обычный flow.

#### Input

- `scope_analysis.output` — обязательно (доступен **внутри child sub-flow** через `parent.scope_analysis.output`, см. `spec/dsl.md → Parent-ref`)
- `summary.md` всех upstream sub-flow по цепочке (см. §1.4 «Цепочка summary») — также через `parent.<subflow>.output`
- Опциональные ссылки маркируются суффиксом `?` (см. §1.4)

**Внутренние шаги child** обращаются к master-уровневым артефактам через префикс `parent.`:
```yaml
# business.yml (child)
- step: contract_state
  input:
    - parent.scope_analysis.output      # шаг master plan
    - parent.infra.output?              # optional если infra skipped
```

Это убирает необходимость явно перечислять каждый master-артефакт в `input:` на subflow-шаге master flow (см. `spec/dsl.md → Parent-ref` для механики).

#### Output — обязательный `summary.md`

Каждый sub-flow ОБЯЗАН произвести **один общий** `summary.md` (фиксированный путь `<workspace>/summary.md`, не настраивается) с:

- Что было сделано (короткое резюме)
- Ключевые решения и артефакты (со ссылками на файлы внутри workspace)
- Sub-flow status в frontmatter: `done` или `feedback_required`

`summary.md` — единый для всех потребителей. Sub-flow НЕ делает отдельные «summary для UI» и «summary для Data». Sub-flow публикует свои решения, downstream сами выбирают релевантное.

Status `feedback_required` — сигнал master'у что нужен перезапуск scope_analysis (формат сигнала и поведение master — **§8**).

Status шага subflow в master plan: всегда `done` после физического завершения child flow. `feedback_required` это семантический сигнал в content, не runtime ошибка.

#### Внутренний пайплайн — строгий

Sub-flow внутри **не адаптивный**. Имеет фиксированный набор шагов под свой слой. Адаптивность только в master (через scope_analysis + условный `if:` на subflow-шагах).

Pause-семантика — стандартная DSL: `pause: true` на шагах + наследованный `mode` (manual / normal / autonomy) определяют когда conductor останавливается.

Конкретный пайплайн каждого sub-flow:
- Infrastructure sub-flow — §3
- Business sub-flow — §4
- UI sub-flow — §5
- Data sub-flow — §6

#### Глубина — 1 уровень

Sub-flow **не может** запускать sub-sub-flow. Допустимая глубина: master → child. Дальше не идём (master → child → grandchild — запрещено) до накопления опыта.

#### Возврат в master (feedback loop)

Sub-flow может сигнализировать master о некорректности начального scope через `status: feedback_required` в frontmatter `summary.md`. Master читает summary, видит сигнал, запускает feedback loop. Полная механика, версионирование артефактов, лимиты — **§8**.

---

## 3. Infrastructure sub-flow

**Когда:** scope_analysis отметил `infra_touched`.

**Что меняет:** DI граф, mate framework, logger + sinks, navigation infra, core.ui / core.theme / core.tools, Build / ProGuard / BuildConfig, Manifest, CI/CD, Guides.

**Input:** `scope_analysis.output` (из master flow).

**Output:** `summary.md`.

**Pipeline:**

```yaml
steps:
  - step: design_tree            # одно дерево для тестов и кода
    pause: true
    with:
      review:
        agents: [architect]

  - step: test                   # условный — только если нужны тесты
    if: needs_tests
    pause: true

  - step: implement
    pause: true

  - step: summary
    pause: true
```

**Особенности:**
- Один `design_tree` описывает и тестовые, и кодовые узлы DAG'а с зависимостями. Pause после него = пользователь review'ит дерево целиком.
- Шаг `test` условный — большинство Infra изменений (ProGuard, Manifest, BuildConfig) не покрываются unit-тестами; включается только когда `needs_tests = true` (меняется тестируемый код, например `core/mate`).
- Гайды (`docs/guides/`) обновляются как часть `implement` — отдельного шага нет.

---

## 4. Business sub-flow

**Когда:** scope_analysis отметил `business_touched`.

**Что меняет:** TEA-логика (State / Msg / Reducer / Effect / Handlers), UseCase контракты + реализации, доменные сущности, спеки (`docs/features-spec/`).

**Input:** `scope_analysis.output` + `infra.output?` (optional — если Infra выполнялась).

**Output:** `summary.md`.

**Pipeline:**

```yaml
steps:
  - step: contract               # детали — отдельно
    pause: true

  - step: update_spec            # план изменений в спеках
    pause: true

  - step: design_tree            # одно дерево для тестов и кода
    pause: true
    with:
      review:
        agents: [architect]

  - step: test                   # для Business всегда выполняется
    pause: true

  - step: implement
    pause: true

  - step: publish_spec           # финализация спек
    pause: true

  - step: summary
    pause: true
```

**Особенности:**
- **Самый сложный sub-flow** — здесь живёт «решающая» часть фичи.
- `contract` шаг — рассматривается отдельно. Фиксирует сигнатуры контрактов (`UseCase.deps`) до того как тесты их используют.
- `update_spec` / `publish_spec` — по аналогии с `lexeme_bugfix` (план до tests, финализация после impl).
- Шаг `test` всегда активен — Business всегда тестируем (без `if:`).

---

## 5. UI sub-flow

**Когда:** scope_analysis отметил `ui_touched`.

**Что меняет:** composables, layouts, виджеты экрана / общие виджеты, ресурсы (strings, drawable).

**Input:** `scope_analysis.output` + `business.output?` (optional — если Business выполнялся).

**Output:** `summary.md`.

**Pipeline:**

```yaml
steps:
  - step: design_tree
    pause: true
    with:
      review:
        agents: [architect]

  - step: implement
    pause: true

  - step: summary
    pause: true
```

**Особенности:**
- **Нет шага `test`** — Compose UI тестов в проекте нет.
- Только `design_tree` (декомпозиция компонентов, layouts) + `implement`.

---

## 6. Data sub-flow

**Когда:** scope_analysis отметил `data_touched`.

**Что меняет:** DB API (`core/core-db-api`), DB Impl (Room entities, DAOs, миграции, schemas), Prefs (DataStore), Library-обёртки.

**Input:** `scope_analysis.output` + `business.output?` (optional — если Business выполнялся).

**Output:** `summary.md`.

**Pipeline:**

```yaml
steps:
  - step: design_tree            # одно дерево: schema + миграции + DAO + миграционные тесты
    pause: true
    with:
      review:
        agents: [architect]

  - step: migration_test         # условный — только если меняется schema
    if: needs_migration_tests
    pause: true

  - step: implement
    pause: true

  - step: summary
    pause: true
```

**Особенности:**
- Один `design_tree` описывает всё вместе: schema, миграции, DAO, узлы миграционных тестов.
- Шаг `migration_test` (отдельный от `test`, потому что использует `androidTest` инфраструктуру) условный — миграционные тесты нужны только при `RoomDatabase.version++` + изменении schema. Изменения только в DAO (без schema) — без миграций.

---

## 7. Файловая структура артефактов

```
docs/features/<TICKET>_<name>/
  plan.yml                       # master plan
  log.md                         # master log
  00_task.md                     # task step output
  02_scope.md                    # scope_analysis output
  02_scope_review.md             # review module: findings (опционально)
  02_scope_approved.md           # review module: approved (опционально)
  check.md                       # check step output

  infra/                         # workspace Infrastructure sub-flow (если запускался)
    plan.yml                     # child plan
    log.md                       # child log
    <шаги>.md                    # артефакты шагов внутри
    summary.md                   # обязательный output для master

  business/                      # workspace Business sub-flow (если запускался)
    plan.yml
    log.md
    <шаги>.md
    summary.md

  ui/                            # workspace UI sub-flow (если запускался)
    plan.yml
    log.md
    <шаги>.md
    summary.md

  data/                          # workspace Data sub-flow (если запускался)
    plan.yml
    log.md
    <шаги>.md
    summary.md
```

После итерации feedback loop (см. §8) — артефакты предыдущей итерации архивируются с суффиксом `_iterN`:

```
docs/features/<TICKET>_<name>/
  02_scope_iter1.md              # архив предыдущей итерации scope
  business_iter1/                # архив предыдущей итерации Business
  data_iter1/                    # архив предыдущей итерации Data
  02_scope.md                    # новая итерация
  business/                      # новая итерация
  data/                          # новая итерация
```

---

## 8. Feedback loop — детальная механика

### Формат сигнала

В frontmatter `summary.md` дочернего sub-flow:

```yaml
---
status: feedback_required
reason: "<почему scope некорректен — конкретная причина>"
suggested_changes:
  <ключ>: <значение>     # подсказки для нового scope_analysis (опционально)
---

(дальше обычное содержимое summary — что было сделано до момента обнаружения)
```

**Кто инициирует:** любой sub-flow, который во время работы обнаружил что начальный scope_analysis некорректен (например Business понял что без правок в Data контракт не закроется). Sub-flow сам решает по своим внутренним правилам.

### Структура feedback в master plan

При получении feedback master записывает его в `step.feedback` — **всегда массив** (даже для одного источника):

```yaml
step.feedback:
  - from: ui
    reason: "..."
    suggested_changes:
      <ключ>: <значение>
  - from: data
    reason: "..."
    suggested_changes:
      <ключ>: <значение>
```

**Почему массив:** parallel-блок (UI ∥ Data) может вернуть feedback с двух веток одновременно. Массив сохраняет источник и даёт agent'у scope_analysis на следующей итерации полный контекст. Слияние конфликтующих `suggested_changes` агент делает сам (он лучше понимает domain).

Для одиночного sub-flow (вне parallel) — массив из одного элемента.

### Поведение master

1. **НЕ продолжать** к следующему шагу master plan. Остановиться на текущем subflow step.
2. **Прочитать feedback content** — массив `step.feedback` целиком.
3. **Заархивировать артефакты текущей итерации** — `02_scope.md` → `02_scope_iter1.md`, и так со всеми артефактами master уровня; sub-flow workspace'ы (`infra/`, `business/`) переименовать в `infra_iter1/`, `business_iter1/`.
4. **Перезапустить шаг `scope_analysis`** со статусом `pending`. На вход агенту, помимо обычного `task.output`, подать:
   - Предыдущий scope output (`02_scope_iter1.md`)
   - Артефакты всех завершённых sub-flow прошлой итерации (`infra_iter1/summary.md`, `business_iter1/summary.md`, …)
   - Содержимое feedback (`reason`, `suggested_changes`)
5. **Review fires снова** — модуль `review` на scope_analysis рецензирует новую итерацию (могут ли рецензенты подтвердить что feedback учтён).
6. **Пройти по новому плану** sub-flow из обновлённого scope_analysis. Какие-то sub-flow перезапускаются (с новым input), какие-то пропускаются как уже сделанные, какие-то добавляются.

### Решение о повторном запуске sub-flow

Sub-flow **не откатываются автоматически**. Новый scope_analysis на основе артефактов предыдущей итерации решает что валидно:

- Если новый scope требует изменения в Infra/Business — те sub-flow перезапускаются заново
- Если scope считает уже сделанное достаточным — те sub-flow не запускаются повторно, master читает их `*_iter1/summary.md` через `input:`

scope_analysis несёт ответственность за такое решение, оно фиксируется в его выходе (какие subflow с `if: <flag>` пропускаются).

### Версионирование артефактов

- Перед каждой новой итерацией scope_analysis: переименовать существующие master-уровневые артефакты в `*_iterN.md` и sub-flow workspace'ы в `<name>_iterN/`
- Новая итерация работает в исходных именах (`02_scope.md`, `business/`)
- Audit trail сохраняется без потери

### Что показывается пользователю

На паузе после нового scope_analysis: ссылка на старую итерацию и новую — пользователь может сам сравнить, либо conductor подсвечивает diff (что в scope добавилось/убралось). Это позволяет понять что и почему пересматривается.

### Ограничения

- **Максимум 3 итерации.** Если после 3 sub-flow всё ещё `feedback_required` — `stop()` с сообщением пользователю. Это сигнал что задача структурно не закрывается через scope_analysis, нужен ручной анализ.
- Sub-flow не может направить feedback к **конкретному** другому sub-flow. Только через master через scope_analysis. В будущем можно усложнить, пока проще.

### Что нужно добавить в ForgeFlow

1. **`spec/runner.md`:**
   - В `execute_subflow` (или в фазе `finalize:after` шага subflow) — проверка `summary.frontmatter.status`. Если `feedback_required`:
     - Найти `scope_analysis` step в plan, изменить его status на `pending`
     - Заархивировать артефакты текущей итерации
     - Прервать стандартный цикл `run()` и начать с найденного `scope_analysis` step (как при ошибке, но иначе)
2. **`agents/embedded/conductor.md`** — описать поведение по `feedback_required`, навигацию по итерациям.
3. **Счётчик итераций** в `plan.context.feedback_iteration` (incremented каждый раз). Лимит 3.

---

## 9. Сводка изменений в ForgeFlow

Консолидировано после ревью architect + analyst. Каждый пункт — обязательное изменение в спецификации/реализации фреймворка для реализации этого дизайна.

### 9.1. `spec/dsl.md`

1. **Новый раздел «Subflow»** — параллельно с `Step`, `Group`, `Parallel`:
   ```yaml
   - subflow: <name>           # обязательное; имя = ключ в plan.steps
     flow: <path>              # обязательное; путь к flow-файлу
     workspace: <subdir>       # опциональное; default = name + "/"
     input: <input_ref[]>      # опциональное (поддерживает суффикс ?)
     if: <var>                 # опциональное
     with: <map>               # опциональное (для модулей master уровня — например logging)
     pause: <bool>             # опциональное
   ```
   Output фиксирован: `<workspace>/summary.md`. Поле не настраивается.
   Запретить сочетание с `repeat` и `sets`.

2. **Раздел «Input → Optional input»** — суффикс `?` на input-ref:
   - Без `?`: ссылка на не-`done` шаг → ошибка (как сейчас).
   - С `?`: для `done` → читается; для `skipped` → null; иначе ошибка.

3. **Раздел «Статусы шага»** — новый статус `feedback_required`, **применяется только к subflow-шагам**. Устанавливается во встроенной логике фазы validate, если `summary.frontmatter.status == feedback_required`. Если `status` отсутствует — трактуется как `done`.

4. **Раздел «Ограничения DSL»** — добавить пункт: «Глубина subflow — 1 уровень. Sub-flow не может содержать `subflow:` шаги». Лимит итераций feedback loop и правило про backjump — runtime, описано в `runner.md` (§9.2).

### 9.2. `spec/runner.md`

1. **`resolve_steps()`** — добавить ветку для subflow с валидацией:
   ```
   else if item has "subflow":
     # Валидация
     if item.if contains " OR ":
       error("subflow.if must be simple variable, OR forbidden: " + item.if)
     if item has "output" or item has "model":
       error("subflow does not support 'output' / 'model' fields")
     # Если родительский flow — child (visited не пустой), запрещаем
     if visited is not empty:
       error("nested subflow not allowed (depth > 1)")

     result.append({ type: "subflow", name: item.subflow, flow: item.flow,
                     workspace: item.workspace, input: item.input,
                     if: item.if, with: item.with, pause: item.pause })
   ```
   Output фиксирован = `<workspace>/summary.md`, не копируется из YAML. Глубина проверяется статически (resolve_steps), runtime-проверка в `execute_subflow` — дополнительная страховка.

2. **`run()`** — добавить ветку:
   ```
   else if step.type == "subflow":
     execute_subflow(step, plan, phase_modules)
   ```
   После `execute_step` / `execute_subflow`:
   ```
   if step.status == "feedback_required":
     trigger_feedback_loop(plan, step)   # set plan.context.feedback_restart = true
     return                              # выходим из текущего run() без flow_end
   ```
   В начале `run()` (до `run_phase("flow_start", ...)`): если `plan.context.feedback_restart == true` — сбросить флаг, **пропустить `flow_start`** и сразу идти в основной цикл итерации шагов. Это устраняет повторный `flow_start` (и дубль `logging "flow started"`) при feedback loop.

3. **Новая функция `execute_subflow(step, master_plan, master_phase_modules)`** с явным фазовым контрактом:
   - **Pre-check (`if`):** в начале функции — `if step.if exists and eval_if(step.if, master_plan.context) != true → step.status = "skipped", return`. Симметрично с `execute_step`. Нужно для случая subflow внутри `parallel:` блока (execute_parallel диспатчит напрямую в execute_subflow, без if-проверки на верхнем уровне run()).
   - **prepare** — модули master'а работают; `input_criteria` не используются (нет step-файла).
   - **prevalidate** — модули master'а.
   - **execute** — встроенная логика. **СТРОГИЙ порядок write'ов** + проверка depth ПЕРЕД любыми write'ами:
     ```
     1. inherited.depth = (master_plan.context.depth or 0) + 1
     2. Если inherited.depth > 1 → stop("subflow depth limit exceeded")
                                   # никаких write'ов до этого
     3. workspace_path = step.workspace or (step.name + "/")
     4. child_dir = master_plan.dir + workspace_path
     5. child_plan_path = child_dir + "plan.yml"
     6. master_plan.steps[step.name].status = "in_progress"
     7. master_plan.steps[step.name].child_plan_path = child_plan_path
     8. write(master_plan.dir + "plan.yml")          # АТОМАРНО до создания child
     9. inherited = { ticket, feature_name, branch, mode, overlay,
                      depth: inherited.depth,
                      parent: master_plan.dir + "plan.yml" }
    10. child_flow = read(resolve_path("flows", step.flow, master_plan))
    11. child_plan = planning(child_flow, config, args={}, inherited=inherited,
                             dir_override=child_dir)
        # planning пишет child_plan.yml в child_dir, с parent
    12. child_phase_modules = resolve_modules(child_flow)
    13. run(child_plan, child_phase_modules)
     ```
     Между шагами 8 и 11 — окно, в котором master помечен `in_progress` + `child_plan_path` указывает на ещё несуществующий файл. Resume в этом окне обрабатывается в `find_plan`. `planning(child)` должна быть идемпотентной: если `child_plan.yml` уже существует — перезаписать (старый файл считается halfway-write).

     **Альтернативный путь для resume:** если на старте `execute_subflow` обнаружено что `child_plan.yml` существует И все его шаги `done` (child завершился полностью, но master не дотянул фазы) — пропустить шаги 6–13, перейти сразу к фазе **validate**.

   - **validate** — встроенное: прочитать `child_dir + "summary.md"`, парсить frontmatter. Если файла нет (child упал) → `step.status = "error"`. Если есть и `summary.frontmatter.status == feedback_required` → положить `{ from: step.name, reason, suggested_changes }` в `step.feedback` (массив из одного элемента), установить `step.status = "feedback_required"`. Иначе обычное validate.
   - **finalize** — модули master'а; `step.output = workspace_path + "summary.md"` (фиксировано, не настраивается).

4. **Новая функция `planning(flow, config, args={}, inherited={}, dir_override=null)`** — расширение сигнатуры:
   - Поля из `inherited` (включая `mode`) не спрашиваются.
   - `args` остаётся для CLI-аргументов.
   - `plan.context.feedback_iteration = 0` (инициализация).
   - `plan.context.depth = inherited.depth or 0`.
   - `plan.parent = inherited.parent or null` — пишется в plan.yml сразу при создании, чтобы избежать race-окна между созданием и установкой parent.
   - **`dir_override`:** если задан — `plan.dir = dir_override` напрямую, **минуя** генерацию из шаблона `{ticket}_{name}`. Нужен для child sub-flow: master передаёт `dir_override = master.dir + step.workspace + "/"`, иначе child получил бы тот же `dir` что master (по наследованным `ticket`/`feature_name`) и перезаписал бы master `plan.yml`.

5. **Новая функция `trigger_feedback_loop(plan, failing_step)`** — фаза **1** (вызывается сразу при получении `feedback_required`):
   - Проверка `plan.context.feedback_iteration < 3`, иначе `stop("Feedback исчерпан (3 итерации). Артефакты итераций: 02_scope_iter1.md … 02_scope_iter3.md")`.
   - Increment `plan.context.feedback_iteration`.
   - Архивирование **master-уровневых** артефактов: `mv 02_scope.md 02_scope_iter{N}.md` (идемпотентно). Subflow workspace'ы **не трогаем** — решение что архивировать ещё не известно.
   - **Записать `02_feedback.md`** в master.dir с содержимым `failing_step.feedback` (массив) + ссылками на архивные scope и summary'и. Файл перезаписывается на каждой итерации. scope_analysis step-файл должен в своём промпте читать `02_feedback.md` если он есть (поведенческий контракт, не DSL).
   - Сброс статуса `scope_analysis.status = pending`. Subflow-шаги пока **не сбрасываем** — остаются `done` с output на старый workspace.
   - Установить флаг `plan.context.feedback_restart = true` — runner на следующей итерации `run()` пропустит `flow_start`.
   - `write(plan)` и `return` — без вызова `run()` рекурсивно. Внешний `main()` подхватит и продолжит.

6. **Новая функция `archive_and_reset_subflows_after_scope(plan)`** — фаза **2**:
   - **Вызов:** через **frontmatter-маркер step-файла**. Любой step-файл может объявить `post_finalize_hook: <hook_name>` в frontmatter. Runner на `run_finalize` после `step.status = done` (перед `run_phase_after("finalize")`) проверяет маркер и вызывает соответствующий хук. Для `archive_and_reset_subflows_after_scope` маркер = `archive_subflows`. В `steps/scope_analysis.md` прописывается `post_finalize_hook: archive_subflows`. Runner не знает про конкретные имена шагов — только про hook names.
   - **Сопоставление subflow → flag:** используем поле `step.if` subflow-шага (`if: business_touched` → flag = `business_touched`). Это переиспользование существующего поля DSL, без дубликата. **Ограничение:** `step.if` на subflow-шаге обязан быть простой переменной — OR-выражения запрещены (концептуально subflow привязан к одному слою). Runner валидирует это в `resolve_steps`.
   - Для каждого subflow-шага читаем новое значение flag из `plan.context`:
     - Если flag == `true` И workspace физически существует → `mv <workspace>/ <workspace>_iter{N}/`, сбросить `step.status = pending`. Здесь `N` — текущее значение `feedback_iteration` (уже инкрементированное в фазе 1).
     - Если flag == `true` И workspace НЕ существует (subflow был skipped в прошлой итерации) → просто сбросить `step.status = pending`.
     - Если flag == `false`:
       - Если `step.status == done` или `skipped` → ничего не делаем.
       - Если `step.status == feedback_required` → перевести в `skipped` (новый scope_analysis сказал что этот слой не нужен).
   - **Сброс родительских блоков.** Если subflow-шаг находится внутри `parallel:` или `group:` блока И сам subflow был сброшен в `pending` — сбросить статус родительского блока тоже в `pending` (иначе главный цикл `run()` пропустит блок целиком как `done`/`skipped`, и subflow не перезапустится).
   - `write(plan)`.
   - Дальше `run()` продолжает по плану.

7. **`find_plan(config)`** — переработка:
   - Plan имеет опциональное поле `parent: <path>` (null для master).
   - Среди найденных plan'ов оставляем корневые (без `parent`). Если их несколько — берём **самый свежий по mtime** (существующий fallback сохраняется).
   - Если в master.plan.steps есть subflow-шаг со статусом `in_progress`:
     - **И** `child_plan_path` существует на диске → возвращает child plan для resume.
     - **И** `child_plan_path` НЕ существует (halfway-write между master write и planning(child)) → возвращает master, runner должен **перезапустить** `planning(child_flow, ...)` в том же workspace (статус остаётся `in_progress`, child создаётся заново).
     - **И** child plan существует но все его шаги `done` (child завершился, master не закрыл шаг) → возвращает master, runner довыполняет фазы validate + finalize шага subflow.

8. **`resolve_ref(ref, plan)`** — обработка суффикса `?`:
   - `optional = ref.endsWith("?")`; обрезать `?`.
   - `done` → читается; `skipped` + `optional` → null; иначе ошибка. Статус `feedback_required` сюда не доходит — runner прерывается раньше через `trigger_feedback_loop`.

9. **`resolve_input(step, plan)`** — фильтровать null'ы из optional refs.

10. **`execute_parallel()`** — расширить:
    - **Skip done/skipped children.** Перед диспатчем — для каждого подшага: если `step.status == "done"` или `"skipped"` → пропустить, не диспатчить. Нужно для feedback loop: при повторном входе в parallel блок (после сброса parent.status в pending через §9.2.6) только сброшенные дети перезапускаются, уже-done остаются как есть.
    - **Диспатч по типу шага.** Для остальных подшагов: если `step.type == "subflow"` — async `execute_subflow(step, plan, phase_modules)`. Иначе — `execute_step_safe(step, plan, phase_modules)` (как сейчас).
    - **Aggregation:** после `wait_all` приоритет статуса блока: `error > feedback_required > done`. Если ≥1 ребёнок `error` → `block.status = "error"`. Иначе если ≥1 `feedback_required` → `block.status = "feedback_required"`. `block.feedback` = **конкатенация массивов** `step.feedback` всех subflow-подшагов со статусом `feedback_required` (каждый элемент уже содержит `from`).
    - `if:` подшагов обрабатывается стандартно через `execute_step`/`execute_subflow` — отдельной проверки в parallel не нужно.

11. **Раздел Plan** — добавить опциональные поля:
    - `parent: <path>` (null для master)
    - `context.depth: int` (0 для master, 1 для child)
    - `context.feedback_iteration: int`
    - `context.feedback_restart: bool` (флаг для пропуска `flow_start` после feedback)

    **Read-time defaults:** при чтении старого plan.yml (без этих полей) — runner трактует отсутствующие как `parent=null`, `depth=0`, `feedback_iteration=0`, `feedback_restart=false`. Backward-совместимость с plan'ами от старой версии runner'а (например IS476) — без миграции файлов.

    `context.archiving` флаг не нужен: каждое `mv` POSIX-атомарно. Перенумерование делается идемпотентно (если source отсутствует и target существует — уже перенесено, пропустить). Resume просто повторяет шаги архивирования.

12. **`main()`** — обернуть `run()` в **do-while** цикл:
    ```
    do:
      run(plan, phase_modules)
    while plan.context.feedback_restart == true
    ```
    Форма `do-while` обязательна — обычный flow (без feedback) должен пройти `run()` ровно один раз (`feedback_restart=false` → выйти после одной итерации). Обычный `while` пропустит запуск при `false`. На входе в `run()` (§9.2.2) — если `feedback_restart == true`, сбросить флаг и пропустить `flow_start`.
    - **Resume child → возврат в master.** Если `find_plan` вернул child plan, после `resume(child)` отрабатывает до завершения. Затем `main()` проверяет `child_plan.parent`: если установлен — загружает master plan, ищет subflow-шаг с `child_plan_path` указывающим на этот child, и вызывает `resume(master, phase_modules, start_from=<step.name>)`. Шаг subflow в master plan остаётся `in_progress` (благодаря `start_from` см. п.13) — `execute_subflow` через альтернативный путь (§9.2.3 «если child plan существует И все шаги done») пропустит execute и сразу пойдёт в validate.

13. **`resume()`** — расширить сигнатуру:
    ```
    function resume(plan, phase_modules, start_from=null):
    ```
    - `start_from: <step_name>` — начать с конкретного шага. Если задан — runner **НЕ сбрасывает** `in_progress → pending` для этого шага (нужно сохранить in_progress чтобы `execute_subflow` распознал «child уже завершён, идти на validate» через альтернативный путь в §9.2.3).
    - Без `start_from` — стандартное поведение: сбросить все `in_progress → pending`, найти первый `in_progress` или `pending`.
    - **Subflow-шаг со статусом `feedback_required`.** Если при resume обнаружен такой шаг — runner НЕ сбрасывает статус, а вызывает `trigger_feedback_loop(plan, step)` напрямую (восстановление прерванного feedback loop).
    - **`feedback_restart == true` при resume.** Если флаг установлен — пропустить `flow_start`, идти сразу в основной цикл (как в §9.2.2).

### 9.3. `agents/embedded/conductor.md`

1. **Раздел «Subflow context»** — описать переключение master ↔ child:
   - При `execute_subflow` conductor работает с child plan; после `run(child)` возвращается к master.
   - При `resume`: если найден in-progress child через `find_plan` — продолжаем child до завершения, потом master.

2. **Раздел «Subflow → logging»** — master logging пишет **только верхнеуровневые события** subflow-шага со ссылкой на child log:
   - На `execute:before` subflow-шага → запись в master log: `[<time>] subflow: <name> → старт. См. <workspace>/log.md`
   - На `finalize:after` subflow-шага → запись в master log: `[<time>] subflow: <name> → <status>` (где status: `done` / `feedback_required` / `error`)
   - **Внутренние события** child flow (фазы child-шагов, события его модулей) пишутся в **child log**, master их не дублирует.
   - Это даёт master log роль timeline-навигатора, child log — детального audit'а.

3. **Раздел «Subflow → modules»** — master и child имеют **независимые** `phase_modules`. Наследования модулей нет.

4. **ЖЕЛЕЗНОЕ ПРАВИЛО — глубина 1.** Если в child-flow встречен `subflow:` шаг — error.

5. **Раздел «Feedback loop»**:
   - Чтение `summary.frontmatter.status` в фазе validate шага subflow. **Читается один раз** — ручная правка summary после фактической обработки не учитывается. Чтобы перепринять решение — сбросить статус шага в `pending` руками в plan.yml.
   - `feedback_required` это **штатный сигнал**, не error.
   - Архивирование и перезапуск scope_analysis — через `trigger_feedback_loop` (не `planning()`).
   - Conductor печатает ссылки `02_scope_iter{N}.md` (предыдущая итерация) и `02_scope.md` (новая) в консоль на паузе после нового scope. Diff не строит.

6. **Раздел «Mode»** — уточнить: «mode СПРАШИВАЕТСЯ ВСЕГДА только в `planning()` при создании plan. При `resume` и при subflow-invocation — наследуется».

7. **Шаблон plan.yml** — добавить пример subflow-шага и поля `parent`, `context.depth`, `context.feedback_iteration`.

### 9.4. `forgeflow.yml` / `config.yml`

Возможно понадобится опциональное `commands.build_release: "./gradlew assembleRelease ..."` для тех Infra-фиксов (ProGuard, R8), которые проявляются только в release-сборке. scope_analysis может расширить `with.checks` на `build_release` в дополнение к `build`. На стартовой реализации можно опустить.

### 9.5. Открытые вопросы (вне scope этого дизайна)

Эти кейсы были подняты в ревью, но осознанно вынесены в бэклог:

1. **Data-driven контракт.** Если контракт диктуется снизу (например IS474: меняется `PrefsProvider.getLongFlow` → Flow<Long?>), Data должна идти ПЕРЕД Business. Текущий порядок Infra → Business → UI∥Data это не покрывает. **Обходится через feedback loop**: Business увидит что без правки Data контракт не закроется → вернёт `feedback_required` → scope_analysis на iter2 правильно расставит. Цена: одна лишняя итерация. По истории IS441-IS476 ожидаемая частота data-driven кейсов ~10%. Если на практике превысит 20% — поднять как отдельный дизайн с `data_drives_contract` флагом.

2. **Тривиальные Infra-фиксы.** IS472 (одна строка в ProGuard) проходит через все master-шаги + полный Infra sub-flow = много пауз. Возможное решение — флаг `is_trivial_infra` от scope_analysis, который пропускает design_tree внутри Infra. Не в рамках этого дизайна.

3. **Diff между итерациями scope.** Сейчас conductor только печатает ссылки на старую/новую итерацию. Полноценный diff — отдельная задача.

4. **Смена mode при resume.** Если пользователь хочет переключить mode после dropout — нужна отдельная команда (`/forgeflow mode <new>`). Out of scope.

~~5. Противоречие со scope_analysis.md §6.5~~ — закрыто, scope_analysis.md обновлён в этом же документе-патче.
