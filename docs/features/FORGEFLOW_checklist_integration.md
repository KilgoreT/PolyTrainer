# ForgeFlow — интеграция checklist в адаптивный flow

Решения по тому, как модуль `checklist` встраивается в `lexeme_adaptive` master flow и его sub-flow. Дополнение к [`FORGEFLOW_design.md`](FORGEFLOW_design.md).

---

## Принципы

- **Один checklist на весь flow** (master + все sub-flow). Не плодим файлы по слоям.
- Файл живёт в `master.dir/checklist.md`.
- **Назначение чеклиста** (важно): инструмент самопроверки реализации. Не «декоративная картина» бизнес-сценариев, а рабочий artifact для:
  - **самопроверки кода** — что планировали vs что сделали (отсюда нужны затрагиваемые файлы/контракты)
  - **самопроверки логов** — что новые log-points работают как задумано
  - **формирования пользовательских сценариев** — на чём верифицировать
- Корневые пункты (root items) = бизнес-сценарии задачи. Защищены, не меняются после создания.
- Подпункты (sub-items) = аспекты реализации (файлы, контракты, log-points). Каждый sub-flow дописывает свои.
- На финале — финальная валидация и отметка статусов.

## Структура master flow с checklist

```
1. task
2. scope_analysis
3. checklist_init        ← input: [task.output, scope_analysis.output]
                           создаёт root items + начальные sub-items по плану реализации
4. <run_subflows>        ← каждый sub-flow дописывает sub-items через checklist module
5. checklist_run         ← финальная валидация и отметка статусов
6. check                 ← lint + test + build
```

**Почему `checklist_init` ПОСЛЕ `scope_analysis`:** для самопроверки кода и логов нужно знать какие файлы/контракты/log-points будут затронуты. Это даёт scope_analysis. Из одного только брифа можно сформировать только бизнес-сценарии, но не план самопроверки реализации.

## Существующие компоненты модуля (используем как есть)

- **Шаг `checklist_init`** (`docs/forgeflow/modules/checklist/steps/checklist_init.md`) — создаёт `checklist.md` с root items из «спеки».
- **Шаг `checklist_run`** (`docs/forgeflow/modules/checklist/steps/checklist_run.md`) — проходит по чеклисту и проставляет статусы (`✅` / `[ ]` / `❌`).
- **Модуль checklist** (фаза `finalize:after`) — собирает sub-items из ответа агента и дописывает в `checklist.md`.
- **Snapshot/diff защита root items** — модуль не даёт случайно удалить корневые.
- **Strikethrough механика** — `~~старый текст~~` для устаревших sub-items, агент сам решает когда применять.

## Что нужно расширить в модуле checklist

Минимальная правка `docs/forgeflow/modules/checklist/prompt.md` псевдокода `finalize:after`:

```
// Где раньше было: items_section = extract_section(agent_response, "## checklist_items")
// Стало:

if step.type == "subflow":
  // Sub-flow не имеет «ответа агента» — есть summary.md
  if step.status == "feedback_required":
    return    // items не добавляем, sub-flow не завершён по делу
  summary_path = plan.dir + step.workspace + "summary.md"
  items_section = extract_section(read(summary_path), "## checklist_items")
else:
  items_section = extract_section(agent_response, "## checklist_items")
```

Две новые ветки:
1. **Subflow + done** → читать items из файла `summary.md`
2. **Subflow + feedback_required** → пропустить, items не добавляем

## Принципиальные решения и обоснования

### Решение 1: один checklist в master.dir, не в sub-flow workspace

**Почему:** Бизнес-сценарии — кросс-слоевые. Они не принадлежат одному sub-flow. Распределить — потерять единую картину.

**Альтернатива (не выбрана):** каждый sub-flow ведёт свой локальный checklist, master агрегирует в конце. Усложняет архитектуру без выгоды.

### Решение 2: subflow дописывает через summary, не напрямую в master.checklist

**Почему:**
- Сохраняет изоляцию sub-flow (он работает в своём workspace)
- Нет race condition при parallel UI ∥ Data (модуль fires на finalize:after sequential через wait_all)
- Использует существующий канал `summary.md` от sub-flow к master

**Альтернатива (не выбрана):** отдельный шаг после каждого subflow для извлечения items. Шумно в YAML, дублирует логику.

### Решение 3: feedback_required items не добавляются

**Почему:**
- `master.checklist` = «что сделано». Items от sub-flow в feedback_required состоянии вводят в заблуждение (sub-flow сам сказал что scope некорректен → работа не завершена по делу).
- Audit НЕ теряется: артефакты iter1 архивируются в `<workspace>_iter1/`, там лежит `summary.md` с `## checklist_items` секцией.
- На iter 2 (когда sub-flow реально закончится с `done`) items добавятся чисто, без strikethrough-каши.

**Альтернатива (не выбрана):** добавлять и зачёркивать через strikethrough при перезапуске. Получаем «забор зачёркнутых items» после 2-3 feedback итераций — нечитаемо.

### Решение 4: при feedback loop master.checklist не архивируется

**Почему:**
- Корневые (бизнес-сценарии задачи) не меняются между итерациями — `checklist_init` сработал один раз
- Sub-items от не перезапускаемых sub-flow остаются валидными
- Sub-items от перезапускаемых sub-flow либо не добавлены (feedback_required → skip), либо добавятся чисто на iter 2

Существующий механизм strikethrough в модуле работает для случаев когда iter 2 sub-flow видит противоречия с предыдущими items — агент сам зачёркивает.

### Решение 5: `checklist_init` input = `[task.output, scope_analysis.output]`, расположение ПОСЛЕ `scope_analysis`

**Почему:** Чеклист — инструмент самопроверки реализации (см. §«Принципы»). Для самопроверки кода и логов нужно знать какие файлы/контракты/log-points будут затронуты. Это даёт scope_analysis. Из одного только брифа сформируются только бизнес-сценарии (root items), но не sub-items для самопроверки.

Из `task.output` берутся бизнес-сценарии (root items). Из `scope_analysis.output` — начальный план самопроверки реализации (sub-items по затрагиваемым файлам / контрактам / лог-точкам).

**Альтернатива (отброшена):** `checklist_init` до `scope_analysis` с `input: task.output` only. Получали бы только бизнес-сценарии, а самопроверки реализации формировались бы каждым sub-flow с нуля. Дублирование работы и потеря единой картины планируемой реализации.

### Решение 6: при повторном запуске master flow `checklist_init` архивирует старый checklist

Если master flow запускается заново для той же фичи и `checklist.md` уже существует (от прошлого запуска) — `checklist_init` **архивирует** старый файл с суффиксом (например `checklist_run1.md`, `checklist_run2.md`) и создаёт новый.

**Почему:** Чеклист — важный artifact самопроверки. Терять прошлую работу нельзя. Архивация даёт audit trail между запусками.

**Альтернатива (отброшена):** перетереть старый — потеря работы; отказаться работать — раздражает; пропустить себя если файл есть — пользователь может хотеть свежий старт.

## Что нужно сделать (action items)

### 1. Обновить `flows/adaptive.yml` (overlay)

Добавить:
- Шаг `checklist_init` **после** `scope_analysis` с `input: [task.output, scope_analysis.output]`
- Шаг `checklist_run` перед `check` с `input: [infra.output?, business.output?, ui.output?, data.output?]` — все опциональные, агент читает summary'и завершённых sub-flow и проставляет статусы
- `with.checklist.phases: [finalize:after]` на каждый subflow-шаг (infra, business, ui, data)
- В `modules:` добавить `checklist`

### 2. Расширить `docs/forgeflow/modules/checklist/prompt.md`

В псевдокоде `finalize:after` добавить две ветки для `step.type == "subflow"` (см. выше).

### 3. Обновить `FORGEFLOW_design.md` §1.1

Master flow: 4 шага → 6 шагов (добавились `checklist_init` и `checklist_run`).

## Совместимость с `lexeme_bugfix`

Модуль checklist расширяется ветками для subflow. Для обычных шагов (которые есть в lexeme_bugfix) поведение не меняется. Backward compat сохранена.

`lexeme_bugfix.yml` не использует checklist module вообще — это не меняется.

---

## Открытые вопросы (вернуться позже)

### OQ-1. Механика модуля без утечки в step file

Контекст: `summary.md` sub-flow должен содержать секцию `## checklist_items` (или эквивалент) чтобы master.checklist module их забрал. Но:
- Если формат секции прописан в `steps/summary.md` step-файле — **утечка модуля в step**.
- По существующей конвенции ForgeFlow — module instructions **инжектятся** в промпт агента через активацию модуля (`with.<module>`).

Открытый вопрос: на каком уровне модуль активен и как agent summary шага получает инструкции про checklist:
- (a) В master, активация на subflow-шагах через `with.checklist`. Модуль fires в master context. Но мастер не может инжектить в child-агента — это разные процессы.
- (b) В child sub-flow, активация на summary шаге. Модуль инжектит в summary агента. Но куда writes — в child.checklist или master.checklist?
- (c) Расширить модуль чтобы он различал master/child контекст через `plan.parent`. Сложнее.

Также вариант: формат секции — это не «module info» в строгом смысле, а **естественная часть summary** (что нужно проверить). Если назвать секцию нейтрально (`## контрольные точки`), это **не утечка модуля**, а просто content. Модуль читает по конвенции.

Решение отложено — нужно более детальное обсуждение архитектуры injection в ForgeFlow.

### OQ-2. Sub-flow декларация checklist module

Если разработчик случайно добавит `checklist` в `modules:` дочернего sub-flow — child создаст СВОЙ `checklist.md` в `workspace/`. Появится **два чеклиста** параллельно.

Варианты защиты:
- (a) Gentlemen agreement — документация
- (b) Runtime-проверка в runner (нет ли дубля модулей у child)
- (c) Module enforce — checklist в child режиме (`plan.parent != null`) отказывается работать с warning

Связано с OQ-1 — выбор зависит от того как разрулим инjection.

### OQ-3. Edge cases (минорные, открытые)

- **`step.status == "error"` у subflow** — модуль checklist должен skip как `feedback_required`? Сейчас не описано.
- **Пустая секция `## checklist_items`** в summary — модуль трактует как «нечего добавлять» (default OK) или error?
- **`step.type` доступ из модуля** — новая конвенция (модули не использовали раньше). Описать в `dsl.md` модуль-API.

---

## Status: ПРОКЛЯТО

Накопившиеся нерешённые проблемы делают текущую интеграцию нежизнеспособной без переписывания.

### Нерешённое

- **OQ-1** — child sub-flow не получает inject_prompt модуля → формат `## checklist_items` не доходит до child summary-агента. Это блокер интеграции, а не «отложенный вопрос». Варианты решения (декларация модуля в child, two-context mode псевдокода) ведут к расширению механики модулей.
- **OQ-2** — sub-flow декларация checklist module: gentlemen agreement vs runtime enforce vs «отказ модуля в child режиме» — не выбрано.
- **OQ-3** — три edge case'а (error status у subflow, пустая секция, `step.type` доступ из модуля).
- **F1** — `checklist_init` при rerun (Решение 6) не сбрасывает `.snapshots/checklist.md` → первый же `finalize:after` сделает откат к старому snapshot. Решение 6 не консистентно с механикой модуля.
- **F4** — дубли sub-items в master.checklist при rerun subflow после feedback от соседа. Решение 4 («master.checklist не архивируется») не покрывает кейс «ui done на iter1 → data feedback → ui_touched=true на iter2 → ui дописывает повторно».

### Что это значит

Решения 1–6 закрывали отдельные кейсы локально. На второй и третий round ревью каждый раз вскрывались новые противоречия с механикой модуля (snapshot/diff, фильтрация по `with.<module>`, atomicity items vs feedback_required, race condition отброшен только из-за sequential conductor'а).

Корневая причина — модуль `checklist` спроектирован под **single-flow** (один plan, один checklist.md, один контекст агента). Адаптивный flow вводит **master + child** — это два контекста, два plan'а, изоляция workspace'ов. Без переосмысления модуля под двухконтекстную работу latanie дыр будет продолжаться.

### Решение

1. **Перепланировать модуль `checklist` с нуля** под адаптивный flow. Явно описать master-режим и child-режим псевдокода. Принципы:
   - master владеет файлом checklist.md и его мутациями
   - child работает с inject_prompt, но не пишет в файл
   - канал передачи items child → master = только summary.md
   - rerun и feedback loop встроены как первоклассные сценарии (не latanie)

2. **Возможно — пересмотреть архитектуру модулей в целом.** Открытые вопросы уровня всей подсистемы:
   - inherited modules между master и child (контракт наследования или явная декларация)
   - module-API для доступа к структуре `plan` и `step` (`step.type`, `step.workspace`, `plan.parent`)
   - two-context mode как общий паттерн или специфика checklist

Текущий документ замораживается. Возврат — после решения по перепланированию.

---

_Документ создан как дополнение к design'у адаптивного flow. Содержит решения по checklist-интеграции принятые в обсуждениях._
