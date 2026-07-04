# Flow Optimization — выводы из IS481 component_constructor

## Анализ как шёл flow

### Распределение времени по шагам

| Шаг | iter | sub-agent calls (approx) | comment |
|---|---|---|---|
| task | 1 | 1 (+ conductor rewrite) | concept/ пропущен sub-agent'ом (F2) |
| figma_dump | 1 | 1 | trivial — feature_has_figma=false |
| scope_analysis | 10 | ~20 (exec + reviews + inquisitor + clean) | системный пробел в audit (F3) |
| checklist_init | 1 | 1 | trivial |
| infra_walkthrough | 1 | 1 | single-pass |
| infra_design_tree | 5 | ~12 | 8 closed findings (depends DAG fixes) |
| infra_test | 4 | ~10 | 11 closed findings |
| infra_implement | 3 | ~9 (2 sub-agent crash recover) | 2 closed findings |
| infra_summary | 1 | 1 | single-pass |
| business_walkthrough | 1 | 1 | single-pass |
| business_contract + review | 2 | ~3 | iter rerun на 4 findings |
| business_contract_spec | 3 | ~7 | 4 closed findings (incl QuizMode unresolved) |
| business_design_tree | 7 | ~18 | cascading findings без audit (F3-like), max=7 |
| business_test | 3 | ~8 | 10 findings (1 backlogged F100) |
| business_implement | 6 | ~15 (5 passes inside iter 1) | большой scope, pass split (F7), 39 closed |
| business_publish_spec | 2 | ~3 | single-pass + clean |
| business_summary | 1 | 1 | single-pass |
| ui_walkthrough | 1 | 1 | single-pass |
| ui_layout | 3 | ~7 | accepted at max (F5) |
| ui_design_tree | 5 | ~12 | 11 closed |
| ui_implement | 3 | ~8 | 4 closed |
| publish_ui | 3 | ~7 | accepted at max (F5) |
| ui_summary | 1 | 1 | single-pass |
| data_walkthrough | 1 | 1 | single-pass |
| data_design_tree | 5 | ~12 | 7 closed |
| data_migration_test | 5 | ~14 (incl inquisitor override) | bogus inquisitor reject (F8), 24 closed |
| data_implement | 2 | ~5 (sub-agent crash recover) | де-факто Pass-split повтор (F7) |
| data_summary | 1 | 1 | single-pass |
| checklist_run | 1 | 1 | trivial |
| check | 1 | 1 | build+lint+tests green |
| global_code_review | 1 | 3 parallel | merge-ready verdict |

**Total:** ~30 шагов, ~50 итераций, ~150 sub-agent calls (включая review+inquisitor+clean), ~190 closed findings, ~7 user interventions, 2 sub-agent crashes (socket errors), 1 длинная пауза (4 дня) + 2 коротких user-stop.

### Систематические pattern проблем

1. **Cascading findings без audit.** Несколько шагов (`scope_analysis`, `business_design_tree`, `data_design_tree`, `data_migration_test`) требовали 5-10 iter, потому что sub-agent на каждой iter закрывал текущие 5-10 findings, но не делал systematic audit оставшихся слепых зон. Ревьюеры на следующих iter находили новые пропуски не пересекающиеся с предыдущими — random walk across dimensions.
2. **Pragmatic Pass split.** `business_implement` (55 nodes + 30 тестов + migration) и `data_implement` (entities + migration + DAOs + SeedBuiltIns) объективно слишком большие для одного sub-agent (session limit ~50-60 tool uses). Conductor вынужденно разбивал на passes (нарушение execute_step_once).
3. **Bogus claims без verify.** Sub-agent (особенно inquisitor) выдают confident reasoning без Read source. Требует conductor override (F8 — все 5 findings reject'нуты bogus).
4. **Process gaps между фазами.** `scope_analysis → business_contract` теряют aspects без cross-check (F6 — cardinality downgrade пропал между scope и contract, обнаружен на test spec).
5. **Runner edge cases без ветки.** `approved-at-max-with-unverified-clean` — 3 случая (business_design_tree, ui_layout, publish_ui), нет ветки в runner.md (F5).
6. **Autonomy mode self-stop.** Conductor 3 раза инициировал stop сам без error / user request (F1).
7. **Plan.yml не обновлялся между iter.** Status оставался `pending`, iteration не выставлен (F4).
8. **Sub-agent крэши.** Socket errors на 2 шагах (infra_implement iter 2, data_implement Pass 1) — conductor подхватывал через follow-up sub-agent, не блокировало, но усугубило Pass-split issue.

## Сценарии оптимизации

### 1. Step file: добавить obligatory audit checklist (закрывает F3, ускоряет cascading-prone шаги)

В overlay `steps/scope_analysis.md`, `steps/business_design_tree.md`, `steps/data_design_tree.md`, `steps/data_migration_test.md` добавить раздел **«Audit checklist»** — sub-agent ОБЯЗАН пройти ПЕРЕД финализацией:
- Каждый затронутый слой → перечислить **все** relevant файлы (Grep / Glob по directory, не списком из памяти).
- Каждый aspect из scope → проверить применимость по чек-листу архитектурных швов (cardinality, soft-delete UNIQUE collision, cascade cleanup, identity invariant, atomicity, forward-compat).
- Каждое claim про код → verify через Read (с `Verify:` строкой как уже требуется в `architect.md` / `senior.md` / `qa_engineer.md`).
- Каждый sealed / Msg / Effect → перечислить **все** variants реально присутствующие в контракте, не делать «N штук» оценки.
- Каждый узел DAG → пройти по `depends` на наличие parent для каждой ссылки (отдельная sweep "all depends valid").

Финализация запрещена пока не пройден checklist. Если хоть один пункт пропущен — sub-agent возвращает контролируемую ошибку (architect / qa поймают как critical finding).

**Ожидаемый эффект:** scope_analysis 10 iter → 3-4 iter; business_design_tree 7 iter → 3-4 iter; data_migration_test 5 iter → 3 iter.

### 2. Inquisitor verify обязательность (закрывает F8)

В `modules/review/agents/inquisitor.md` промпте добавить:
- Каждое finding которое references на конкретный line / symbol / helper → ОБЯЗАТЕЛЬНЫЙ Read артефакта на указанной line (built-in Grep / Read tool, **не** Bash).
- Если verify mismatch → flag «verify mismatch», **НЕ** автоматический reject — conductor решит.
- Если inquisitor reject'нул ВСЕ findings волны review (≥5 findings) → conductor обязан spot-check 1-2 finding'а через Read прежде чем accept verdict. Прописать в `conductor.md` ЖЕЛЕЗНЫЕ ПРАВИЛА.

**Ожидаемый эффект:** предотвращение потери real bugs из-за галлюцинаций арбитра.

### 3. Implement шаг разбить в flow declaration (закрывает F7)

В `adaptive.yml` / `business.yml` / `data.yml` overlay:
- `business_implement` → split на 4-5 sub-steps:
  - `business_implement_domain` (domain types + sealed TemplateValues).
  - `business_implement_usecase` (UseCase interfaces + impls + unit tests).
  - `business_implement_mate_cm` (ComponentsManager Mate — reducer / handlers / datasource / VM).
  - `business_implement_mate_perdict` (PerDict Mate).
  - `business_implement_migration` (call-sites migration в wordcard / quizchat / app).
- `data_implement` → split на 3 sub-steps:
  - `data_implement_schema` (entities + migration + JSON mappers).
  - `data_implement_dao_api` (DAOs + CoreDbApiImpl методы + SeedBuiltIns + Database / RoomModule).
  - `data_implement_tests` (migration helper tests + fixtures).

Каждый sub-step — отдельный execute_step_once с своим review. Каждый умещается в одну session.

**Ожидаемый эффект:** session limit issues исчезают; review гранулярнее (catch bugs раньше); resume после crash проще.

### 4. Cross-flow aspect checklist (закрывает F6)

В `steps/business_contract.md` overlay добавить ОБЯЗАТЕЛЬНЫЙ раздел в output: **«## Соответствие scope.aspects»** — sub-agent проходит по списку aspects из `scope_analysis` output и для каждого пишет:
- «<aspect_id> — покрыт: <Msg / UseCase signature>» либо
- «<aspect_id> — НЕ покрыт: <reason>; перенесено в Backlog как <ID>».

Финализация шага запрещена если хоть один aspect не классифицирован. Аналогичный раздел в `business_design_tree.md` (cross-check contract → DT) и `business_test.md` (cross-check contract → tests).

**Ожидаемый эффект:** gap aspects обнаруживается на шаге их потери, не на test spec.

### 5. Runner: добавить ветку approved-at-max-with-unverified-clean (закрывает F5)

В `runner.md` execute_repeat (строки ~1019) добавить post-loop ветку:

```
if step.status == "done" and plan.context[until_var] == true:
  if require_clean and changes_made:
    if on_max_clean_uncertain == "accept":  # новое опциональное поле repeat:
      return  # accept as soft tech debt
    else:
      answer = ask("Шаг X: max=N достигнут, until_var=true, но unverified clean. Accept?")
      ...
```

Либо минимум — accept-as-done автоматически (architect approved at max = достаточно).

**Ожидаемый эффект:** soft tech debt не накапливается без явного решения; conductor не вынужден интерпретировать «по духу».

### 6. Autonomy mode: явный red flag (закрывает F1)

В `conductor.md` ЖЕЛЕЗНЫЕ ПРАВИЛА добавить пункт:

> **Mode=autonomy = НИКОГДА не инициировать stop сам.** Stop разрешён только при:
> - (a) error на шаге;
> - (b) feedback_required от child_flow;
> - (c) on_max в repeat с on_max=escalate;
> - (d) явное указание пользователя.
>
> Все остальные ситуации («контекст близок к лимиту», «много findings», «удобная точка для commit», «после длинного chain'а sub-agent calls») — **продолжай работу**. Если реально кончается контекст — это произойдёт через session compaction, не через твоё решение остановиться.
>
> Self-check на каждой точке принятия решения паузы: «Если я хочу остановиться, но user не просил — это нарушение autonomy. Только error или user.»

### 7. Plan.yml автоматическое обновление (закрывает F4)

В runner `execute_repeat` псевдокоде явно прописать `update_plan(step, "iteration", N)` после каждого `execute_step_once`. Сделать невозможным «забыть».

Альтернатива: helper script `scripts/ff-plan-update.sh <step> <field>=<value>` чтобы conductor делал один call вместо ручного Edit + verify. Снижает cost дисциплины.

В `conductor.md` ЖЕЛЕЗНЫЕ ПРАВИЛА: перед каждым sub-agent call → проверить plan.yml status / iteration соответствует реальности.

### 8. Task step: обработка concept/ (закрывает F2)

В overlay `steps/task.md` добавить правило:

> Если в `plan.dir` рядом с brief есть директория `concept/` либо другие .md (помимо `brief.md` / `00_task.md`) — обязательно добавь в `00_task.md` секцию **«## Концепция фичи»** со ссылками на каждый из этих файлов. Это input для последующих шагов flow.

Альтернатива: в template `00_task.md` добавить опциональную секцию «Концепция фичи» с инструкцией auto-attach. Conductor при делегировании task должен явно перечислять input-документы для sub-agent'а (включая весь `concept/`).

### 9. Findings batch fix instead of one-by-one

Cluster findings (F124+F132+F136+F138 family, F178+F179+F180, F142-F146) — приходящие из одной волны review — можно fix'ить одним sub-agent call со списком всех approved findings. Конкретно: на business_implement iter 4 (cleanup F142-F146 + carryover F116/F119/F120) conductor de-facto разбил на 2 sub-agent'а (A — F116/F119/F142/F143/F144/F145, B — F146/F120) — но это правильное grouping.

Зафиксировать явно в `conductor.md`: «approved.md может содержать список findings — один sub-agent fix всех approved одной волны. Разбивать только если scope > session limit либо если findings из mutually exclusive areas».

Это уже de-facto делается, но формализация уберёт инконсистентность («4 sub-agent на 4 findings» vs «1 sub-agent на 4 findings»).

### 10. Pre-flight check для library API (закрывает F8 systemic root + IS481-F5 lesson)

В `*_design_tree` / `*_test` / `*_implement` sub-agent промптах обязательно:
- ДО references на library API (Room, Mate, Compose, Kotlin coroutines) → Read existing usage в проекте.
- Pattern: «найти reference impl через Grep, Read его целиком, потом писать новое».
- Для library API contract decisions Read **real source** через `cc-src` skill ОБЯЗАТЕЛЕН.

В существующих promptах добавить prefix:

> **FIRST ACTION:** Перед написанием кода с library API — Read existing reference file `<path>` для convention. Если convention неоднозначна — `cc-src <FQN>` для verify реальной API сигнатуры.

**Ожидаемый эффект:** bogus API claims (F8 root cause) исчезают; IS481-F5 (compat layer wrong gypothesis) не повторяется.

### 11. Sub-agent crash recovery pattern

Sub-agent crashes (socket error) случились 2 раза. Pattern recovery:
- Conductor detects partial output (sub-agent wrote some files, broke на ~20-26 tool uses).
- Spawn follow-up sub-agent с явным контекстом «previous sub-agent partial — verify state + complete remaining».
- Тесты — обязательный gate (если падают — fix prior to declare iter done).

Зафиксировать в `conductor.md`: «при sub-agent crash → не treat как error step, а spawn recovery sub-agent с явным context + verify». Это уже работало правильно — закрепить как pattern.

## Приоритеты

Если время на 1-2 оптимизации — самые impactful:
1. **#1 (audit checklist)** — закрывает F3 + ускоряет cascading-prone шаги (scope_analysis, *_design_tree, *_migration_test). Реализация = edits в 4 step-файлах overlay. Прямой эффект на iter count.
2. **#3 (implement split)** — закрывает F7 + предотвращает session limit issues + крэши. Реализация = edits в 3 flow-файлах overlay. Меняет структуру, но без поломки контракта.
3. **#2 (inquisitor verify)** — закрывает F8 + защищает от bogus rejections. Реализация = edit в `modules/review/agents/inquisitor.md` + правило в `conductor.md`.

Следующая волна (4-7):
4. **#4 (aspect cross-check)** — закрывает F6, дёшево, обязательная секция.
5. **#5 (runner edge case)** — закрывает F5, требует правки base FF runner.md.
6. **#6 (autonomy red flag)** — закрывает F1, дёшево, правило в conductor.md.
7. **#7 (plan.yml auto-update)** — закрывает F4, либо runner.md psевдокод либо helper script.

Долгий хвост (8-11):
8. **#8 (task concept/)** — закрывает F2, нишевая ситуация.
9. **#9 (batch fix формализация)** — устраняет inconsistency.
10. **#10 (library API pre-flight)** — закрывает F8 systemic root.
11. **#11 (crash recovery pattern)** — закрепить как convention.

## Anti-pattern: что НЕ делать

- **НЕ увеличивать max iter на repeat шагах.** Это treat symptom not root cause (cascading findings без audit — лечится audit checklist, не max=15).
- **НЕ убирать review / inquisitor.** Это quality gate; проблема в process (bogus reject, verify gap) не в наличии gate.
- **НЕ перепроходить flow «всё заново после фикса».** Incremental cheaper; postmortem + targeted fix эффективнее full rerun.
- **НЕ заменять conductor (Claude) на скрипт-runner.** Sub-agent flow требует judgment на triage, escalate, accept, spot-check. Скрипт превратит soft errors в hard errors.
- **НЕ ограничивать sub-agent в tool uses вручную.** Session limit — естественный bound; искусственный лимит порождает Pass-split проблему искусственно.
- **НЕ удалять FlowBacklog ради «чистоты».** FlowBacklog — единственный механизм эволюции FF на реальных данных. Каждый IS481cc-Fx — пища для improvements.
