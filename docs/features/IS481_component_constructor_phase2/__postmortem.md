# Постмортем: IS481 component_constructor phase 2 (adaptive flow)

Период: 2026-06-22T16:12:30 → 2026-06-22T22:31:48 (≈ 6 ч 19 мин wall-clock).
Mode: `autonomy`. Flow: `adaptive` (overlay `docs/forgeflow-overlay`).
Branch: `IS481_lexeme_component_constructor`.
Артефакты: `docs/features/IS481_component_constructor_phase2/` (см. `log.md`, `plan.yml`, `REVIEW.md`).

Цель phase 2: 5 функциональных пунктов поверх phase 1 baseline (Edit CRUD; Removed semantics для Rename/Delete/Edit; multi-dict scope picker; cardinality downgrade с preview; чистка дублирования между Manager / PerDict mate).

---

## 1. Таймлайн (high-level)

| Фаза | Start | End | Δ | Notable |
|---|---|---|---|---|
| task | 16:12:30 | 16:16:40 | 4m | brief.md удалён после переноса в `task.md` |
| figma_dump | 16:16:40 | 16:26:10 | 10m | Case A; **stub-обход IS481p2-F1** (условные output_criteria) |
| **scope_analysis** | 16:30:41 | 17:58:46 | **88m** | **5 итераций; 25 approved / 7 rejected; user-stop перед review iter 5** |
| checklist_init | 17:58:46 | 17:58:46 | <1m | 5 root + 14 manual scenarios |
| **infra sub-flow** | 17:58:46 | 18:41:12 | **43m** | walkthrough → design_tree (iter 2, clean iter 3 skipped) → test (PASS, тесты не нужны) → implement (PASS, user shortcut) → summary; 4 узла |
| **business sub-flow** | 18:41:12 | 20:32:00 | **111m** | walkthrough → contract → contract_review (changes_requested → user-inline-fix; rerun skipped) → contract_spec (drift accepted) → design_tree (inline-fix; rerun skipped) → test (review skipped) → **implement (1 проход; assemble + test PASS; 75 tests)** → publish_spec → summary |
| **ui sub-flow** | 20:32:13 | 21:32:26 | **60m** | walkthrough → layout (Case A) → design_tree (32 узла в 5 tier'ах) → implement (14 NEW widgets + 16 deletions) → publish_ui → summary |
| **data sub-flow** | 21:32:26 | 21:55:24 | **23m** | walkthrough (нашёл 3 bug + 2 logging gap) → design_tree (6 узлов) → migration_test skipped → implement (real per-lexeme SELECT) → summary |
| checklist_run | 21:55:24 | 21:59:59 | 5m | 5 root [ ] manual; 10 sub ✅ |
| check | 21:59:59 | 22:09:08 | 9m | lint / test / build все PASS (sequential per memory `feedback_sequential_gradle_tests`) |
| **global_code_review** | 22:09:08 | 22:31:48 | **23m** | 3 reviewers parallel; **28 findings (10 critical + 18 minor)**; A1/B1 blocker найден независимо двумя reviewers |
| **TOTAL** | — | — | **≈ 379m / 6h19m** | — |

Заметно: scope_analysis (88m, 5 iter) + business (111m) занимают ~53% всего wall-clock.

---

## 2. Что прошло хорошо

1. **business_implement за 1 sub-agent call.** В phase 1 (IS481cc-F7) `business_implement` требовал 2 passes (52 + 58 tool uses) из-за context limit. В phase 2 — один проход (~30 минут, по словам реализации 22 узла + 4 файла тестов + 1 existing test fix), `assembleDebug` + `testDebugUnitTest` PASS, 75 tests pass. Возможные причины успеха: scope чисто incremental поверх phase 1 (не из scratch); contract / design_tree уже зафиксировали все 22 узла гранулярно; tests были предзаложены skeleton'ом на business_test шаге, реализация работала «по тестам». IS481cc-F7 fix-ideas (разбиение на sub-steps) **не понадобились** — workload сам поместился.
2. **data sub-flow нашёл 3 bug'а phase 1 + 2 logging gap.** `data_walkthrough` отработал как «второй проход» по data слою после business_implement (с conservative cardinality approximation, см. § 3) — закрыл задолженность чисто, узко, без scope creep. Это правильный паттерн «шерсть data слоя после business».
3. **3 reviewers global_code_review независимо нашли A1/B1.** Architect + bugs reviewers оба независимо обнаружили блокер DictionariesFlowHandler — strongest signal что reviewer-pool работает (cross-validation real bugs vs random walk). YAGNI reviewer ушёл в другую сторону (dead code) — комплементарно.
4. **Logging per-event через Edit (IS481-F8 соблюдалось).** `log.md` 193 строки, каждая запись отдельной строкой с пустой строкой между событиями, формат `<br>[timestamp] step: X → status` + опциональные note блоки. Без bash `printf >>` и без batched append.
5. **sequential gradle tests (новое memory).** На ui_implement и data_implement тесты прогонялись по одному модулю (`./scripts/cc-build.sh testDebugUnitTest` на `:core-db-impl` отдельно от `:app`) — оптимизация подтвердилась через memory `feedback_sequential_gradle_tests`.
6. **Layer-attribution boundary держалась.** В отличие от IS481-F16 (IS481cc; business съел UI), здесь business_design_tree (24 узла) и ui_design_tree (32 узла) разведены чисто; UI integration появилась только в ui_implement.
7. **infra_test «тесты не нужны» — корректный verdict.** 4 узла (LogTags const + 2 build.gradle + Migration logging-only) — architect + qa оба PASS. Раньше существовала тенденция «прогонять test шаг ради галочки».

---

## 3. Что прошло плохо

### 3.1. DictionariesFlowHandler blocker несмотря на 75 tests pass

**Факт:** 75 phase 2 tests pass, `assembleDebug` + `testDebugUnitTest` зелёные — но multi-dict scope picker (1 из 5 phase-2 пунктов) **полностью нерабочий**. `DictionariesFlowHandler` создан как класс, инжектится в `DatasourceEffectHandler`, но не зарегистрирован в `effectHandlerSet` `ComponentsManagerViewModel` (`Mate.subscribeToLongRunningFlows()` его не подписывает).

Verify: Read `modules/screen/components_manager/.../ComponentsManagerViewModel.kt:42-47` → `effectHandlerSet = setOf(datasourceHandler, flowHandler, uiHandler, navHandlerFactory.create(navigator))` — нет `dictionariesFlowHandler`.

**Систематика проёба:**

1. **Tests на ViewModel-wiring не существовали и не были запрошены.** Reducer tests мокают handler'ы (изолированно). UseCaseImpl tests мокают API. DatasourceEffectHandler test (phase 1) был только обновлён добавлением ctor-mock. **Никто не тестировал что Mate.effectHandlerSet содержит правильный набор** — потому что ViewModel в этом проекте не тестируется (mate-pattern полагается на reducer/handler unit tests как достаточные).
2. **`SubscribeDictionaries` effect объявлен но не emit'ится reducer'ом.** Это было бы поймано grep'ом — но architect/senior reviewer на business_implement пропустили (3 minor findings от senior, ни один не про emit-coverage).
3. **business_walkthrough → contract → design_tree цепочка задизайнила handler как «новый узел Manager mate #12», но не задизайнила ни (a) где он регистрируется в ViewModel, ни (b) где emit'ится initial SubscribeDictionaries effect.** Узел #12 описывает handler как класс — а wiring (effectHandlerSet + initEffect) — это infra-level узел который остался невидим.

**Lesson:** В Mate-pattern проектах любой новый `FlowHandler` обязан иметь чек-лист wiring'а:
- регистрация в `effectHandlerSet` ViewModel,
- либо emit subscribe-effect в Reducer (initEffects или явный Msg.OnInit ветка),
- либо обе.

Design_tree должен **явно** включать узел «ViewModel.effectHandlerSet += newHandler» при появлении нового FlowHandler. Architect review должен пройти грep'ом «new Handler → registered in effectHandlerSet».

### 3.2. 5 итераций scope_analysis с user-interrupt вместо escalate

**Факт:** scope_analysis шёл 5 iter (16:30 → 17:58, 88 минут). Iter 1: 9 approved / 3 rejected; iter 2: 7/2; iter 3: 2/1; iter 4: 5/1; iter 5 — user-interrupt «хватит уже ревью scope_analysis» **до того как** sub-agent запустил review iter 5. Conductor выставил `review_passed=true` и перешёл к checklist_init.

**Сравнение с IS481cc-F3 (recurrence):** в phase 1 scope_analysis требовал 10 iter (38 closed / 12 rejected) — тот же класс проблемы, random walk reviewers находят новые слепые зоны на каждой итерации. Phase 2 — 5 iter вместо 10, но проблема тот же: sub-agent на iter N+1 не делает audit ВСЕХ оставшихся dimensions, он только закрывает findings iter N. **Фикс IS481cc-F3 (audit checklist в `scope_analysis.md`) не реализован** — проблема воспроизвелась.

**User-interrupt vs escalate:** в plan.yml `scope_analysis.repeat.max=7, on_max=escalate`. Реально пользователь интерраптнул на iter 5 — runner не дошёл до on_max. Это правильный manual-override, но он скрывает что **система не имеет критерия «достаточно глубокого scope»**. Без эвристики «approved findings на iter N стали marginal (≤ 1 critical и все minor)» — repeat будет крутиться до on_max либо до user-interrupt каждый раз.

**Возражение к собственному поведению:** на iter 4 architect перешёл на PASS, qa дал 1 critical + 2 minor (F022/F023/F024). Iter 5 закрыл 5 approved. Sub-agent с большой вероятностью на iter 5 review нашёл бы ещё 2-3 marginal finding, но это уже не давало бы реальной ценности для downstream. User-interrupt был корректен по существу — но conductor должен был **сам** предложить «marginal finding rate, остановим» а не ждать `хватит блять`.

### 3.3. Inline-fixes вместо trigger_step_rerun — на multiple шагах

Применено user-shortcut «правлю inline, rerun не делаю» в:
- `business_contract_review` (verdict=changes_requested, 2 finding'а: EditOutcome.Success(ComponentType) + CardinalityDowngradeBlocked split logic);
- `business_contract_spec` review (3 critical: typeId Long vs ComponentTypeId, state-class name, epochId omitted в spec — все «accepted as non-blocking drift спека↔контракт»);
- `business_design_tree` review (F001 critical missing DAG edge #11→#12, F002 minor labeling — закрыты inline);
- `infra_implement` review (2 minor от senior, user-accepted без rerun);
- `business_test` review — целиком **пропущен** по user shortcut «хватит ревью» (tests прошли validation только через actual compile+run на check шаге).

**Корректность:** для review шага с **малым числом findings, низкой severity** и **clearly mechanical patch** — inline-fix дешевле rerun (rerun = новый sub-agent call ~5-10 минут на каждый шаг). Это reasonable optimization.

**Где это сбоит:** `business_contract_spec` accepted **3 critical** findings как «non-blocking drift». «typeId Long vs ComponentTypeId» — это load-bearing API contract. Если бы был mismatch — implement сломался бы. Здесь implement его поправил (`нетривиальное решение #1`: typeId → ComponentTypeId), но это работает только потому что implement-sub-agent сам решил parity с codebase. Если бы он принял спеку буквально — был бы баг. **Critical findings нельзя downgrade'ить inline-фиксом без правки артефакта.** В этом случае фикс артефакта (5 минут Edit на спеку) был дешевле любого риска.

**Lesson:** user-shortcut «хватит ревью» применим к **minor** findings и к **подтверждённой mechanical inline-правке**. Critical findings обязательны к rerun **либо** к явной правке артефакта conductor'ом перед mark done.

### 3.4. Skip guides post-process на iter 3+ scope_analysis и на multiple шагах

**Факт:** в `plan.yml` шаги `scope_analysis`, `infra_design_tree`, `business_design_tree`, etc. имеют `with.guides.enabled: true`. Это означает что в post-process после каждой итерации должен пройти guides-check (R-правила из подложенных гайдов). По логу — на iter 3-5 scope_analysis нет упоминаний guides post-process; на iter 1 business_test review целиком skipped.

**Wasted shortcut vs legitimate optimization:** guides post-process — это R-правила, machine-checkable (R-RP / R-N / R-SM). Для **scope_analysis** (high-level аналитический документ) guides добавляют мало — почти все R-правила про код (reducer / state / naming), не про аналитику. Skip — legitimate. Для **design_tree** (низкоуровневый граф узлов с file paths) — guides критичны (R-N для naming, R-SM для state-modeling узлов). Skip там был бы потеря качества.

В фактическом flow — design_tree guides выполнялись (R-N упоминался в architect review F031 «hardcoded TAG → LogTags ref» — это R-N-NNN naming правило, значит guides loaded). Скип скорее на scope_analysis и на business_test post-process — там это нормально.

### 3.5. Документация спеки vs реализация: F017 template-immutability gate

**Факт:** `business_contract_spec.md` + `business_contract.md` + `02_scope.md` фиксируют F017: «UseCaseImpl сравнивает template параметра с current → TemplateImmutable без обращения к data API». Реальный `ComponentsManagerUseCaseImpl.kt:144-172` имеет только `trim+isBlank` check, сразу делегирует API. Defense-in-depth есть на data (`CoreDbApiImpl:582`), но **основной gate (UseCase) отсутствует**.

Verify: см. REVIEW.md § A2 (architect), § Y9 (yagni, дубликат). Найдено независимо двумя reviewers.

**Систематика:** контракт говорит одно, код другое. Test `whenSubmitEditWithChangedTemplate_thenTemplateImmutable_andDataApiNotCalled` отсутствует — а должен был быть **первым** тестом в business_test для F017 (TDD-flavor). business_test sub-agent его не написал, business_implement sub-agent не реализовал. Reviewer на business_implement (architect + senior) не заметил расхождение спека↔код потому что не сравнивал явно.

**Root cause:** между **спекой** и **тестами** нет cross-validation. Spec-driven testing требует чтобы каждый «инвариант / gate» из spec имел соответствующий test method. Сейчас этот mapping держится «в голове» sub-agent'а business_test, без явной audit checklist.

**Lesson:** business_test prompt должен включать обязательную секцию «соответствие тестов спецификации» — таблица spec.feature_id → test method name. Спеки phase 1/2 уже используют ID для invariants (F017, F101, F124, F136, F138, F140) — нужно требование `assert каждый F-NNN покрыт ≥ 1 тестом`.

### 3.6. Conservative cardinality approximation в business_implement

**Факт:** business_implement в data API (`CoreDbApiImpl.editComponentType`) реализовал cardinality downgrade approximation: «если existing.isMulti && !isMulti && countActiveByTypeId > 0 && dictIds.isNotEmpty() → blocked с empty ids list». Точный per-lexeme SELECT (`HAVING count(*) > 1` + deterministic ORDER) — задача data sub-flow.

Это было задокументировано как «нетривиальное решение #5» в business_implement.md.

data_implement потом реализовал real `findLexemesWithMultipleValuesForType` (NEW DAO) + real per-lexeme SELECT.

**Почему business_implement не сделал реальный SELECT:** scope разделения слоёв — data API (CoreDbApiImpl) считается «data слой», business_implement должен **не трогать** его (по правилу IS481-F16 layer boundary). Но компиляция business требует чтобы `LexemeApi.editComponentType` существовал и возвращал что-то осмысленное → minimal data impl был **compile-shim**. Это IS479-F12 (UI/business дрейф при compile-shim) — здесь применён к business → data: «business ставит rudimentary data impl чтобы скомпилироваться, data sub-flow доделает».

**Корректно ли:** да — это работающий паттерн «business compile-shim, data sub-flow доводит». Альтернатива «business не трогает data вообще» сломала бы compile. Альтернатива «data sub-flow идёт первым» — нарушит порядок зависимостей (data зависит от business contract).

**Проблема:** approximation **не был помечен** TODO-flag'ом в коде или явным `IllegalStateException("not yet implemented properly")`. Если бы data sub-flow «забыл» этот узел — approximation осталась бы в продакшене как silently-wrong поведение (blocked с empty ids list → UI показывает «нельзя downgrade, но не показывает какие лексемы» → user stuck). Стандарт — `// TODO data-subflow: real per-lexeme SELECT` маркер в коде.

### 3.7. Несколько FF/runner-finding'ов накоплены без системного фикса

- **IS481p2-F1** (figma_dump условные output_criteria, Case A) — workaround stub-файл.
- **IS481p2-F2** (рассинхрон `task.md` vs `00_task.md`) — workaround локальная правка `scope_analysis.md` input_criteria.

Оба finding'а — runner/DSL уровень (`is_mechanical` / `check_mechanical` не понимают условные критерии; `input_criteria` хардкодит filename вместо `{step.output}` interpolation). Оба зарегистрированы в FlowBacklog как **open**, фикс только локальный.

**Lesson:** quick-win'ы накапливаются. Системный fix (например `{step.output}` interpolation в `input_criteria`) дешевле чем 5 future workaround'ов. Но он требует правки base FF (`runner.md`) — out-of-scope текущей фичи, попадает в FF backlog.

---

## 4. Systemic patterns (с reference на FlowBacklog ID)

| ID | Pattern | Recurrence в phase 2 | Статус |
|---|---|---|---|
| **IS481cc-F1** (autonomy stop) | conductor self-initiates pause | **НЕ повторилось** в этой сессии. Все паузы — либо `pause: true`, либо user-interrupt. Conductor НЕ предлагал «давай остановимся». ✅ | open |
| **IS481cc-F3** (random walk reviewers scope_analysis) | sub-agent не делает audit ВСЕХ dimensions | **Повторилось** — scope_analysis 5 iter (vs 10 в phase 1, частичное улучшение по объёму но та же природа). audit checklist в overlay `steps/scope_analysis.md` НЕ реализован. ⚠ | open |
| **IS481cc-F7** (business_implement multiple passes) | один sub-agent на большой scope упирается в session limit | **НЕ повторилось** — phase 2 implement прошёл за 1 проход. Возможно потому что (a) incremental scope vs from-scratch; (b) test skeleton предзаложен — implement идёт «по тестам». Фикс IS481cc-F7 (split flow) **не понадобился**. ✅ | open (но не materialized) |
| **IS481cc-F8** (inquisitor bogus) | inquisitor отклоняет approved findings без verify | **НЕ повторилось** — нет упоминаний inquisitor bogus в log. ✅ | open |
| **IS481-F8** (per-event Edit logging) | bash printf vs Edit | **Соблюдалось** — log.md написан через Edit, per-event, без bash. ✅ | open (фикс применён) |
| **IS481-F11** (verify library API через source) | conductor полагается на documentation для library contract | **Не применимо** в phase 2 — нет library-API-contract decisions. Phase 1 prereq уже закрыл Room 2.8 / bundled driver через source-verification. Memory `verify_library_api_through_source` помечено как «главный systemic lesson». ✅ | open |
| **IS481-F15** (pause-перед-reviewer) | `pause: true` на target вместо reviewer | **Повторилось частично** — `business_contract: pause: true` в plan.yml + `business_contract_review` без pause. Но пользователь не возмутился — потому что результат сразу был обработан inline-fix без rerun (см. § 3.3). ⚠ | open |
| **IS481-F16** (layer boundary в design_tree) | business design_tree поглотил UI | **НЕ повторилось** — business 24 узла / ui 32 узла разведены чисто. ✅ | open |
| **IS482-F7** (alias-form для design_tree) | `step: <layer>_design_tree` без alias | **НЕ повторилось** — все *_design_tree используют alias-form (`prompt: design_tree, output: <layer>_design_tree.md`). ✅ | closed |
| **IS481p2-F1** (условные output_criteria) | runner.check_mechanical безусловно проверяет «существует» | **Новый finding из этой фичи.** Workaround применён. ⚠ | open |
| **IS481p2-F2** (filename vs step.output) | `input_criteria` хардкодит `00_task.md` vs реальный `task.md` | **Новый finding из этой фичи.** Workaround в overlay. ⚠ | open |

---

## 5. Quick wins для FF / overlay

1. **`input_criteria` interpolation `{step_name.output}`.** Решает IS481p2-F2 и весь класс «filename hardcoded в input_criteria». Правка `runner.md → check_criteria` + `dsl.md`. Effort: medium. Импакт: каждый шаг с input от другого шага становится robust к переименованию output.

2. **Условный output_criteria `если <var>=<value> — <rest>`.** Решает IS481p2-F1. Правка `runner.md → is_mechanical / check_mechanical` — распарсить prefix «если X=Y —» через `plan.context[X]`. Effort: small. Импакт: figma_dump перестаёт требовать stub в Case A; любой step с условным артефактом работает.

3. **`name + prompt` DSL (IS481-F13).** Уже задизайнено в FlowBacklog. Решит boilerplate alias-form. Effort: medium (правка `dsl.md` + `runner.md.resolve_steps` + миграция всех flow.yml + backward-compat). Импакт: 18+ alias-step'ов в overlay упрощаются.

4. **scope_analysis audit checklist (IS481cc-F3).** Самый дорогой не-реализованный фикс. Effort: small (правка `overlay/steps/scope_analysis.md` body — добавить obligatory секцию «audit checklist» с 4 пунктами: layer-by-layer file list / aspect chip-list / verified facts / open questions defaulted). Импакт: scope_analysis с 5-10 iter → 2-3 iter; экономия 30-60 минут на каждой фиче с многослойным scope.

5. **Mate FlowHandler wiring checklist.** Решает классу A1/B1 на будущее. Effort: small (правка `overlay/steps/business_design_tree.md` либо `architect.md` review prompt — добавить mandatory check «новый FlowHandler → узел «register в ViewModel.effectHandlerSet» в design_tree И узел «emit Subscribe Msg/initEffect» в Reducer»). Импакт: aналогичный bug не пройдёт через design_tree review.

6. **Spec-driven testing audit.** Решает § 3.5. Effort: small (правка `overlay/steps/business_test.md` body — добавить obligatory таблицу «F-NNN из спека → test method»). Импакт: invariants из спеки не пропадают.

7. **«хватит ревью» semantics.** Сейчас user-interrupt «хватит ревью» обрабатывается conductor'ом ad-hoc. Стоит ввести явный механизм: пользователь команда `/skip-review` → conductor выставляет `review_passed=true` И logs reason. Differentiating от обычного pause-continue. Effort: low. Импакт: explicit aудитирование того что review был пропущен (не теряется в логе).

---

## 6. Метрики

**Sub-agent calls (estimated):**

| Шаг | iter | sub-agent calls (gen + review) | Заметка |
|---|---|---|---|
| task | 1 | 1 | |
| figma_dump | 1 | 1 | sub-agent вернул Case A без MCP вызова |
| scope_analysis | 5 | ≈ 14 (5 gen + 5 architect + 4 qa + inquisitor отдельно) | самый дорогой шаг |
| infra_walkthrough | 1 | 1 | |
| infra_design_tree | 2 | 4 (2 gen + 2 architect) | iter 3 clean skipped |
| infra_test | 1 | 3 (1 gen + 1 architect + 1 qa) | verdict «тесты не нужны» |
| infra_implement | 1 | 3 (1 gen + 1 architect + 1 senior) | |
| infra_summary | 1 | 1 | |
| business_walkthrough | 1 | 1 | |
| business_contract | 1 | 1 | |
| business_contract_review | 1 | 1 | inline-fix вместо rerun |
| business_contract_spec | 1 | 2 (1 gen + 1 architect) | drift accepted |
| business_design_tree | 1 | 2 (1 gen + 1 architect) | inline-fix |
| business_test | 1 | 1 | review skipped |
| **business_implement** | 1 | 3 (1 gen + 1 architect + 1 senior) | **один проход 22 узла + 75 tests** |
| business_publish_spec | 1 | 1 | |
| business_summary | 1 | 1 | |
| ui_walkthrough | 1 | 1 | |
| ui_layout | 1 | 2 (1 gen + 1 architect) | |
| ui_design_tree | 1 | 2 | |
| ui_implement | 1 | 3 | 14 NEW widgets |
| publish_ui | 1 | 2 | |
| ui_summary | 1 | 1 | |
| data_walkthrough | 1 | 1 | нашёл 3 bug + 2 logging gap |
| data_design_tree | 1 | 2 | |
| data_implement | 1 | 3 | real per-lexeme SELECT |
| data_summary | 1 | 1 | |
| checklist_run | 1 | 1 | |
| check | 1 | 0 (Bash gradle) | sequential per memory |
| **global_code_review** | 1 | 3 (architect + bugs + yagni параллельно) | 28 findings |
| **TOTAL** | — | **≈ 63 sub-agent calls** | scope_analysis = 22% всех вызовов |

**Wall-clock breakdown:**
- scope_analysis: 88m (23%)
- business: 111m (29%)
- infra: 43m (11%)
- ui: 60m (16%)
- data: 23m (6%)
- review/check/finalize: 32m (8%)
- task/figma_dump/checklist: 25m (7%)

**Критические шаги (по wall-clock):**
1. business_implement (~34m, 1 проход, 22 узла)
2. ui_implement (~33m, 14 NEW widgets)
3. scope_analysis iter 1 (~22m)
4. business sub-flow в целом (111m)

**Артефактов:** 40+ файлов в `docs/features/IS481_component_constructor_phase2/` + 1 spec обновление (`docs/features-spec/component-constructor.md` 1359 → 1470 строк).

**Тестов phase 2:** 75 unit tests (32 ManagerReducer + 24 PerDictReducer + 15 ManagerUseCase + 4 PerDictUseCase) — все pass. **Не покрыто:** ViewModel-wiring (см. § 3.1 / A1/B1).

**Findings:** 28 в global_code_review (10 critical + 18 minor); 13 → закрыть в фиче, 14 → backlog, 1 → rejected.

---

## 7. Сводные выводы

**Что работает в adaptive flow:**
- Sub-agent pool (architect / qa / senior / yagni / bugs) ловит реальные баги, особенно на global_code_review (3 reviewers parallel).
- Per-event Edit logging (IS481-F8) даёт real-time audit trail без UX-overhead bash permission prompts.
- Sequential gradle tests (новое memory) реально быстрее parallel.
- Layer boundary (IS481-F16 lesson) держится в design_tree разделении business/ui/data.
- One-pass business_implement возможен когда test skeleton предзаложен — IS481cc-F7 не materialized.

**Что требует фикса в FF/overlay (priority):**
1. **HIGH** — Mate FlowHandler wiring checklist (§ 3.1) — иначе будем повторять A1/B1.
2. **HIGH** — scope_analysis audit checklist (IS481cc-F3) — экономия 30-60m на каждой фиче.
3. **MEDIUM** — `input_criteria` interpolation `{step.output}` (IS481p2-F2) и условные output_criteria (IS481p2-F1) — устраняют workaround'ы.
4. **MEDIUM** — Spec-driven testing audit table (§ 3.5) — invariants F-NNN не пропадают между spec и tests.
5. **LOW** — формализовать «хватит ревью» user-shortcut (§ 3.3) с явной семантикой critical vs minor.

**Что повторилось из IS481cc:**
- IS481cc-F3 (scope_analysis random walk) — частичное (5 iter вместо 10, но та же природа).
- IS481-F15 (pause-перед-reviewer на business_contract) — частичное.

**Что НЕ повторилось из IS481cc (либо потому что фикс был, либо случайно повезло):**
- IS481cc-F1 (autonomy stop) — соблюдалось.
- IS481cc-F7 (multiple passes business_implement) — workload поместился.
- IS481cc-F8 (inquisitor bogus) — не материализовалось.
- IS481-F16 (layer boundary) — соблюдалось.
- IS481-F8 (logging) — соблюдалось.

**Новые finding'и из phase 2:**
- IS481p2-F1 (условные output_criteria) — open.
- IS481p2-F2 (filename vs step.output) — open.
- A1/B1 DictionariesFlowHandler blocker — закрывается в фиче (REVIEW triage).

_model: claude-opus-4-7[1m]_
