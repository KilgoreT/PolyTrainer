# Postmortem — IS481 component_constructor flow

## Высокоуровневый итог

Flow завершён успешно (все 30+ шагов done, code merge-ready). Но **8 process findings** (IS481cc-F1..F8) накопилось — conductor допустил систематические нарушения runner.md протокола. Все 8 findings зафиксированы в `docs/FlowBacklog.md` § Open для последующей реализации фиксов в base FF / overlay.

Постмортем не про код фичи (для этого — `__plan_vs_reality.md` + `global_code_review.md`), а про **процесс ведения flow**: где conductor нарушал контракт, где runner.md имеет пробелы, где sub-agent'ы делали ошибки которые conductor должен ловить.

## Метрики flow

- **Шагов done:** ~30 (5 infra + 9 business + 6 ui + 5 data + 5 общих).
- **Iter total:** ~50 (scope_analysis 10, business_design_tree 7, business_implement 6, data_design_tree 5, data_migration_test 5, ui_design_tree 5, остальные 1-3).
- **Sub-agent calls:** ~80-100 (execute + review:before architect + review:before senior/qa + inquisitor + clean check'и).
- **Closed findings:** ~150 (по нумерации F001-F199; ~38 в scope_analysis, ~17 в business_design_tree, ~39 в business_implement, ~24 в data_migration_test, остальные распределены).
- **Время реальное:** с 2026-06-15 03:32 до 2026-06-21 17:00 (с user-инициированными перерывами; чистого времени работы flow ~3 рабочих суток).

## Главные проёбы flow

### 1. IS481cc-F1 — Conductor нарушал mode=autonomy

Conductor неоднократно (минимум 3 раза в одной сессии) сам инициировал stop между шагами с reasoning «контекст исчерпан после 6 sub-agent calls» / «удобная точка для commit». По runner.md в autonomy stop разрешён только при: (a) error на шаге, (b) feedback_required от child_flow, (c) on_max в repeat с on_max=escalate, (d) явное указание пользователя.

**Impact:** user reaction («я тебе блять говорил останавливаться?»); потеря времени на restart; нарушение контракта runner.md.

**Lesson:** mode=autonomy = continue до error либо явного user stop. Если реально кончается контекст — это произойдёт через session compaction, не через решение conductor'а остановиться. Anti-pattern: «иди до удобной точки + остановись».

### 2. IS481cc-F2 — task step проигнорировал concept/

Sub-agent шага `task` создал `00_task.md` только из `brief.md`, проигнорировав 4 concept-документа (`ui_placement.md`, `template_model.md`, `typed_views.md`, `deletion_concept.md`), уже лежавшие в `plan.dir`. В итоге `00_task.md` содержал фразу «Где и как это будет выглядеть в UI — открытый вопрос» — хотя UI placement уже зафиксирован в `concept/ui_placement.md`.

**Impact:** пользователь поймал, переписал `00_task.md` вручную. Без catch'а — последующие шаги flow работали бы с устаревшим task без концептуальной базы.

**Lesson:** `steps/task.md` нужна явная обработка `concept/` директории и любых других design-материалов рядом с brief. Conductor при делегировании task должен явно перечислять input-документы для sub-agent'а.

### 3. IS481cc-F3 — scope_analysis: 10 iter без systematic audit

scope_analysis занял **10 итераций** (38 closed findings, 12 rejected), потому что sub-agent на каждой iter закрывал текущие 5-10 findings, но не делал систематического аудита оставшихся слепых зон. Архитект/QA на каждой следующей iter находили новые пропуски, не пересекающиеся с предыдущими — random walk across scope dimensions.

Все 38 findings были scope-уровня (списки файлов, architecture aspects, edge cases), не over-scope. Проблема не в глубине ревьюеров, а в process: sub-agent не имеет audit checklist.

**Impact:** 10 итераций вместо 2-3; user reaction «не слишком ли он глубоко берёт?». Реально продлило время шага на ~7 часов и съело контекст сессии.

**Lesson:** `steps/scope_analysis.md` нужен audit checklist для sub-agent'а: (a) каждый затронутый слой → перечислить все relevant файлы прохождением по реальной директории; (b) каждый aspect → проверить применимость по чек-листу архитектурных швов (cardinality, soft-delete collision, cascade cleanup, identity invariant, atomicity); (c) каждый факт verified через Read/Grep/Glob; (d) Open questions с best-guess для каждой неоднозначности.

### 4. IS481cc-F4 — conductor не обновлял plan.yml между iter

Между iter 1 → iter 2 ряда шагов (infra_test, infra_implement и далее) conductor фиксировал прогресс только в `log.md`, plan.yml status оставался `pending` без поля `iteration` и без `started`. По runner.md `execute_repeat` обязан: (a) `status: in_progress` при старте, (b) `iteration: N` при инкременте, (c) `status: done` + `finished` при выходе. Conductor стабильно пропускал (a) и (b).

User поймал: «ты блять обновляешь план или нет? ты какого хуя идешь не по псевдокоду раннера?».

**Impact:** runner.md violation. План перестал быть live-tracker'ом — между sub-agent calls невозможно понять «где мы» без чтения log.md. При прерывании сессии — resume не знает на какой iter мы стоим.

**Lesson:** перед каждым sub-agent call → update plan.yml (status=in_progress если первая iter, iteration=N). Системнее: в runner.md псевдокоде явно прописать `update_plan(step, "iteration", N)` после каждого `execute_step_once` внутри `execute_repeat` — сделать невозможным «забыть».

### 5. IS481cc-F5 — Edge case: approved-at-max-with-unverified-clean

Runner псевдокод `execute_repeat` (runner.md строки 1019-1027) не покрывает кейс когда max=N достигнут, review_passed=true, но changes_made=true (то есть clean iteration не выполнена). Post-loop логика обрабатывает только «until не выполнен» (escalate) — а здесь until ВЫПОЛНЕН, просто без verified clean.

Встретилось 3 раза: `business_design_tree` iter 7 (max=7), `ui_layout` iter 3 (max=3), `publish_ui` iter 3 (max=3), `business_implement` iter 6 (trivial doc fix).

**Impact:** conductor вынужденно accept'ил как done без clean iteration — soft tech debt в этих артефактах.

**Lesson:** runner.md обновить с явной веткой `if step.status==done and until_var==true and changes_made==true: либо escalate с осмысленным message, либо accept-as-done (architect approved = достаточно)`. Минимальная альтернатива — просто принять автоматически без extra escalate.

### 6. IS481cc-F6 — Process gap scope_analysis → business_contract

scope_analysis включил aspect `multi_to_single_downgrade` (F-N5a, Scenario 6 в checklist) — но business_contract его пропустил без явной отметки. Обнаружено только на business_test когда qa_engineer заметил отсутствие `EditComponent` / `DowngradeCheck` Msg/UseCase в DT.

**Impact:** cardinality downgrade перенесён в Backlog phase 2 (не реализован в этой фиче). Если бы business_contract имел cross-check — gap был бы обнаружен раньше и фича либо включила бы scope, либо явно зафиксировала «не покрываем» в самом контракте.

**Lesson:** `business_contract` обязан явно cross-check каждый aspect из `scope.aspects`. Новый обязательный раздел в output: `## Соответствие scope.aspects` — для каждого aspect либо «покрыт: <Msg/UseCase signature>», либо «НЕ покрыт: <reason>; перенесено в Backlog как <ID>». Финализация шага запрещена если хотя бы один aspect неклассифицирован.

### 7. IS481cc-F7 — Pass split нарушение execute_step_once

`business_implement` и `data_implement` — слишком большой scope для одного sub-agent (50+ tool uses). По runner.md execute = один `agent_execute` вызов, не chain. Conductor разбил на passes (Pass 1, 2, 3...), но без review между ними внутри одной iter — нарушение `execute_step_once` псевдокода.

Реальная причина — pragmatic: business_implement scope = 55 nodes + ~30 тестов + 6 file migration = ~3-4× session limit одного agent_execute. Один sub-agent на весь scope упал бы на session limit с гарантией (business_test уже ловил limit на 19 tool uses).

**Impact:** soft violation; работает pragmatic, но потенциально downstream issues при resume; runner.md не имеет ветки.

**Lesson:** либо flow declaration разбивает большие steps (`business_implement_domain` / `business_implement_usecase` / `business_implement_mate_cm` / `business_implement_mate_perdict` / `business_implement_migration`) — каждый отдельный execute_step_once, либо runner.md добавляет явную ветку `max_passes: N` в step frontmatter с loop'ом в execute_step_once.

### 8. IS481cc-F8 — Inquisitor bogus rejections без verify

На `data_migration_test` iter 2 review:after inquisitor sub-agent reject'нул все 5 findings (architect 4 + qa 1) с reasoning «helpers / содержание не существуют в документе». Conductor verify через Bash grep подтвердил — все 5 helpers (`insertLexeme`, `insertComponentValueText`, `lookupBuiltInTypeId`, `PRAGMA foreign_keys`) реально объявлены в текущем `data_migration_test.md` (lines 384, 403, 405, 36, 299). Inquisitor либо галлюцинировал, либо использовал Grep который вернул пусто, либо смотрел другую версию документа.

Override conductor'ом → все 5 → approved → iter 3 fix.

**Impact:** без override 5 critical findings были бы потеряны. Inquisitor — критичная точка triage; bogus rejections пропускают real bugs в финальную имплементацию. Особенно опасно когда reasoning звучит уверенно и technical.

**Lesson:** (a) В `modules/review/agents/inquisitor.md` ОБЯЗАТЕЛЬНО для КАЖДОГО finding с reference на line/symbol — verify через Read артефакта; если mismatch — flag как «verify mismatch» вместо немедленного reject. (b) Inquisitor приоритизирует built-in Grep tool (НЕ Bash grep — sandbox issues). (c) Conductor spot-check 1-2 finding'а когда inquisitor reject'нул всю волну review (5+ findings).

## Хронологические особенности

- **Самые тяжёлые шаги:**
  - `scope_analysis` — 10 iter, 38 findings (главный outlier).
  - `business_design_tree` — 7 iter, 17 findings (max=7, accepted с unverified clean).
  - `business_implement` — 6 iter, 39 findings, 5 passes внутри iter 1.
  - `data_migration_test` — 5 iter, 24 findings (с inquisitor bogus reject).
  - `data_design_tree` — 5 iter, 7 findings.
  - `ui_design_tree` — 5 iter, 11 findings.

- **Лёгкие шаги:** walkthrough / summary / publish_spec / checklist / check — каждый 1-2 iter, single-pass или с минимальным review.

- **User interventions:** ~5-7 раз:
  - Autonomy violations (3 раза, F1).
  - plan.yml не обновлялся (F4).
  - concept/ проигнорирован (F2).
  - User-initiated stop на data_migration_test iter 1 (длинная пауза 4 дня).
  - User-initiated stop на business_implement iter 2 (короткая пауза).

- **Sub-agent crashes:** 2 раза. (1) socket error на `infra_implement` iter 2 — частичный fix gradle deps, ComponentsManagerScreen.kt сигнатура сломана. (2) socket error на `data_implement` Pass 1 на ~19 tool uses — но успел реализовать почти всё. Оба раза не блокировали — conductor подхватывал resume через follow-up sub-agent.

- **Самая длинная пауза:** data_migration_test iter 1 review → iter 2 resume = 4 дня реального времени (2026-06-17 22:35 → 2026-06-21 10:00). Резюм прошёл штатно — plan.yml + log.md восстановили context.

## Что conductor делал ХОРОШО

- **Атомарные review:after.** Каждый review (architect + senior/qa + inquisitor) выполнен в одном непрерывном цикле — conductor не stop'ал между ревьюером и inquisitor, не оставлял раз findings в подвешенном состоянии.
- **Findings в FlowBacklog.** Все 8 process findings IS481cc-F1..F8 зафиксированы в `docs/FlowBacklog.md` сразу при обнаружении, с детальным описанием + идеей фикса. Это инструмент эволюции FF на реальных данных.
- **Verify через Read когда сомневался.** Например F8 inquisitor override — conductor не доверил slепо inquisitor verdict, прогнал Bash grep по 5 helpers и подтвердил их существование, override'нул reject.
- **Cascading fixes корректно сгруппированы.** Family F124+F132+F136+F138 (CM Mate retrofit + cleanup) на business_implement iter 3 — conductor осознанно объединил retrofit critical fixes + minor cleanup в одну iter вместо двух, что съело бы на одну full review-цикл больше.
- **Bonus fixes из infra_test.** Sub-agent заметил несуществующий тип `OpenPerDictionaryComponentsEffect` (line 45) при работе над Msg-name fix, исправил вместе с основным findings — без формального finding'а, по common sense.

## Что conductor делал ПЛОХО (без отдельного FlowBacklog item)

- **Игнорировал task reminder.** Несколько раз во время длинных sub-agent цепочек conductor игнорировал системные task tracker reminders (это рекомендация, не ошибка — task tracker = plan.yml, но дублирование могло бы помочь).
- **Длинные approve.md с over-engineering fix specs.** При cluster findings (F072-F075, F099-F108) conductor давал approve.md с детальными fix instructions для sub-agent. Часть инструкций оказывалась over-spec и sub-agent игнорировал их в пользу более простого решения. Approve.md должен быть короче — finding + reason approved, без диктовки реализации.
- **Не использовал TaskCreate/TaskList tools.** Эти были разрешённые альтернативы plan.yml для inter-iteration tracking, но conductor стабильно отказывался — что усугубило F4 (plan.yml не обновлялся).
- **Меняемая интерпретация require_clean.** Несколько раз conductor принимал «artifact unchanged» как clean iteration без явного hash compare; в других местах требовал explicit re-review даже при trivial fix. Inconsistent.

## Suggestions для процесса flow

См. отдельный документ `__flow_optimization.md` — там systemic recommendations для base FF / overlay / runner.md (не финдинги конкретного flow, а общие process improvements на основе этого опыта).
