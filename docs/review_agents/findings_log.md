# Findings log

Raw data всех findings от review-агентов. Сохраняем максимум: finding, verify, triage решение, оценка validity. Потом отжмём в метрики (true positive rate, false positive rate, проблемные паттерны, удачные паттерны промпта).

Формат — таблица per run. Один run = один agent + один target.

---

## Условные обозначения

**Severity:** critical / minor (как объявил агент в момент finding).

**Triage:**
- `closed-feature` — поправлено в текущей фиче (код или док).
- `backlog` — валидно но out of scope, перенесли в backlog.
- `rejected` — невалидно (false positive / out of context / not a problem).
- `deferred` — отложено до другого решения / другой фичи.

**Validity:**
- `TP` (true positive) — реальная проблема, agent поймал верно.
- `FP` (false positive) — galлюцинация / out of context / уже учтено.
- `PARTIAL` — направление верное, детали мимо.

**Когда заполнять Validity:** после triage. До triage — пусто.

---

## Шаблон run-секции

```markdown
### <agent> on <target> (<date>)

**Context:** <что ревьюилось, какая фаза>
**Findings count:** N critical / M minor

| ID  | Severity | Description (10-20 слов) | Triage | Validity | Notes |
|-----|----------|---|---|---|---|
| F1  | critical | ... | closed-feature | TP | ... |

**Agent meta:**
- Sub-agent tool: `general-purpose`.
- Verify usage: <использовал Read/Grep/Glob корректно? Использовал bash против правил?>.
- Prompt quality: <ноты что в промпте работало / не работало>.
```

---

## Runs

### IS482 — global_code_review (architect + senior + yagni-style) on master branch — 2026-05-30

**Context:** финальный обзор готовой фичи IS482 (lexeme domain unification) перед коммитом. 3 параллельных subagent'а: Architecture / Bugs / YAGNI. Триаж findings перед коммитом.

**Findings count:** 1 critical + 7 minor.

| ID | Agent | Severity | Description | Triage | Validity | Notes |
|---|---|---|---|---|---|---|
| F-A1 | architect | critical | `QuizGameImpl` force-unwrap `!!.value` × 4 — риск NPE при IS481-shim | closed-feature | TP | Реальная проблема, фикс через локальные переменные + smart-cast. Кросс-модульный nullable property. Связано: R-CS-001 в `code-style.md` (запрет `!!`). |
| F-A2 | architect | minor | Kotlin version skew 1.9.10 jvm vs 2.0.20 android | closed-feature | TP | Bump одной строкой в settings.gradle.kts. |
| F-A3 | architect | minor | `Lexeme.wordId` non-null живёт ради одного потребителя (dictionaryTab) | closed-feature | TP | После research — zombie field, удалили из API/Domain/UI. Pre-existing legacy от IS297. |
| F-A4 | architect | minor | Двойной маппинг `Api→domain→UI` в DictionaryTab — две аллокации per item | rejected | FP | Architect не учёл Compose stable types через UI обёртки. После уточнения от пользователя — это feature, не bug. |
| F-A5 | architect | minor | `mapper/` пакет с одним жителем + naming drift (`toDomain` vs `toDomainEntity`) | backlog | PARTIAL | Направление верное (mapper location convention), но scope > IS482. Запись «Repository pattern refactor» в backlog. |
| F-Y1 | yagni | minor | `LexemeId` value-class сразу разворачивается на каждой границе | rejected | PARTIAL | Type safety материализуется только в domain. Пользователь решил оставить. Yagni-критик технически прав, но решение оправданное контрактом. |
| F-Y2 | yagni | minor | `Lexeme.wordId` dead в production (дубликат F-A3) | closed-feature | TP | Дубликат, закрыт вместе с F-A3. |
| F-Y3 | yagni | minor | Over-testing passthrough — 21 тест, половина дублирует roundtrip | rejected | PARTIAL | Технически прав, но canonical TDD pattern. Минорный signal/noise issue без cost-effective fix. |

**Agent meta:**
- Architect — 5 findings (1 critical + 4 minor). 3 TP / 1 FP / 1 PARTIAL.
- Bugs — 0 findings (PASS). Хорошо — фича была механической, реальных багов не появилось.
- YAGNI — 3 minor findings. 1 TP (дубликат) / 0 FP / 2 PARTIAL.
- Verify usage: architect/senior использовали bash `git grep -n` ВОПРЕКИ proj rule (нарушение). Зафиксировано в FlowBacklog IS482-F3.
- Prompt quality: architect нашёл реальную F-A1 (force-unwrap NPE risk), это самый ценный finding. YAGNI требует фильтра «schema-readiness» — пользователь дополнил соответствующую memory.

---

### IS481 — multi-agent review on 02-06.md (старые) — 2026-06-01

**Context:** обзор готовности доков IS481 к flow реализации. 3 направления: Architecture & Consistency / Feasibility & Bugs / YAGNI & Test coverage.

**Findings count (aggregated после фильтра дубликатов):** 9 unique critical + 17 minor.

| ID | Agent | Severity | Description | Triage | Validity | Notes |
|---|---|---|---|---|---|---|
| A1 | architect | critical | Domain pure-Kotlin не может зависеть от core-db-api (Android library) | closed-feature | TP | Пользователь сразу указал что я недодумал: dependency должна быть обратной (data → domain). Architect нашёл симптом, не корень. Прав по существу. |
| A2/B5 | architect/feasibility | critical (architect: minor, feasibility: critical) | `wordId` в маппере 06.md vs реальный LexemeApiEntity без wordId | closed-feature | TP | Alignment pass v1 пропустил. Чисто doc-fix. |
| A3 | architect | critical | `@Deprecated` обёртка `addLexemeWithTranslation(String)` vs `(TranslationApiEntity)` | closed-feature | TP | Doc писал sub-agent с ошибочной сигнатурой. Реальный код имеет TranslationApiEntity. |
| A4 | architect | critical | `seedBuiltIns` рассинхрон 04/05 (definition built-in vs user-defined) | deferred → решено через 07 rewrite | TP | После rewrite 07 на config-based — расхождение неактуально, definition не нужен как built-in. |
| A5 | architect | critical | `deleteLexemeTranslation/Definition` приписаны не тому слою (CoreDbApi vs UseCase) | closed-feature | TP | Doc мix слоёв. |
| A6 | architect | minor | «sealed BuiltInComponent» в 04 open questions, везде enum | closed-feature | TP | Опечатка. |
| A7 | architect | minor | «(без суффикса, не BuiltInComponent)» самопротиворечие | closed-feature | TP | Повтор имени в скобках, опечатка. |
| A8 | architect | minor | `ComponentTypeId/ValueId` value-классы без явной декларации | closed-feature | TP | Doc gap — добавили декларации. |
| A9 | architect | minor | camelCase vs snake_case в `@Relation` без пояснения | closed-feature | TP | Добавлен комментарий-пояснение. |
| B1 | feasibility | critical | `Callback.onCreate(db)` молча игнорируется под bundled driver | closed-feature | TP | Реальный риск — fresh install сломан. Доку поправили на `(connection)`. |
| B2 | feasibility | critical | Все 10 существующих миграций под bundled driver выбросят NotImplementedError | closed-feature | TP | Реальный риск регрессии. Пункт в чеклист добавлен. |
| B3 | feasibility | critical | `SQLiteConnection.execSQL` не существует — нужен extension | closed-feature | TP | Doc gap. |
| B4/C2 | feasibility/yagni | critical | Удаление `Translation`/`Definition` value-классов сломает компиляцию mate | closed-feature | TP | Самое значимое findings — изменили подход на shim (Translation/Definition остаются как `@Deprecated`). Mate действительно бы сломался. |
| B6 | feasibility | minor | existing user-defined «Definition» при upgrade conflict | rejected | TP но schema-readiness | Триаж — INSERT OR IGNORE в чеклисте, риск low. |
| B7 | feasibility | minor | orphan lexeme после миграции — NPE risk | closed-feature | TP | Добавлен тест. |
| B8/C11 | feasibility/yagni | minor | invalid JSON fallback не описан | closed-feature | TP | Добавлен тест try-catch / Result. |
| B9 | feasibility | minor | partial UNIQUE not validated Room | rejected | TP но known | Учтено в 04, не критично. |
| B10 | feasibility | minor | INSERT OR IGNORE deps on UNIQUE | rejected | TP но known | Учтено через `@Index(unique=true)`. |
| C1 | yagni | critical | `Lexeme.definition` shim возвращает всегда null (если definition user-defined) | deferred → решено через 07 rewrite | TP | Связано с A4. Закрыто новым 07 (через QuizConfig). |
| C3 | yagni | critical | `BuiltInComponent.DEFINITION` — dead enum value | deferred → решено через 07 rewrite | TP | После 07 — DEFINITION действительно не нужен как built-in (definition остаётся user-defined). |
| C4 | yagni | critical | Partial UNIQUE для user-defined global YAGNI | rejected | FP | Schema-readiness. Yagni-критик не учёл фильтр БД-схема. Память исправлена. |
| C5 | yagni | critical | Soft-delete remove_date YAGNI | rejected | FP | Schema-readiness. То же. |
| C6 | yagni | minor | `BuiltInComponent` enum при одном значении | rejected | FP | Schema-readiness (на будущее DEFINITION/pronunciation). |
| C7 | yagni | minor | `ComponentTemplate.IMAGE` / `ImageValue` YAGNI | rejected | FP | Schema-readiness. |
| C8 | yagni | minor | JSON `v: 1` payload version premature | rejected | FP | Schema-readiness. |
| C9 | yagni | minor | `canRemoveComponent` лишний | rejected | TP но already-handled | Учтено в чеклисте 05. |
| C10 | yagni | minor | Multi-level @Relation без бенчмарка | rejected | TP но not-blocker | Implement-time TBD test. |
| C12 | yagni | minor | rollback атомарности тест в чеклисте, но не в § Тестирование 06 | closed-feature | TP | Доку поправили. |
| C13 | yagni | minor | `LexemeApiEntity.toDomain()` с components тесты — нет явных кейсов | closed-feature | TP | Доку поправили. |

**Agent meta:**
- Architect — 9 findings (5 critical + 4 minor). 9 TP / 0 FP. Сильный run.
- Feasibility — 10 findings (5 critical + 5 minor). 10 TP / 0 FP. Самый ценный — поймал bundled driver migration risks (B1/B2/B3) и mate compilation issue (B4).
- YAGNI — 13 findings (5 critical + 8 minor). 8 TP / 5 FP. **Высокий false positive rate** из-за непримененного schema-readiness фильтра. После добавления соответствующей memory — будущие runs должны быть лучше.
- Verify usage: все три использовали Read/Grep встроенные tools корректно. Хорошо.
- Prompt quality: feasibility-агент особенно эффективен на migration / cross-version compat questions. YAGNI-агент нуждается в **обязательном** schema-readiness фильтре в промпте.

---

### IS481 — functional_reviewer on new 07_quiz_strategy.md (config-based) — 2026-06-01

**Context:** обзор нового 07 (config-based решение для quiz strategy) с акцентом на функциональную корректность quiz после реализации. Pre-rewrite scope (JSON-колонка в dictionaries).

**Findings count:** 4 critical + 3 minor.

| ID | Agent | Severity | Description | Triage | Validity | Notes |
|---|---|---|---|---|---|---|
| F1 | functional | critical | Migration SQL `WHERE l.translation IS NOT NULL` ломает правило «всегда [BuiltIn(TRANSLATION)]» | closed-feature → rewrite 07 | TP | Реальная проблема, definition-only словарь получал бы пустой config. |
| F2 | functional | critical | Lookup `?: error()` без fallback крашит quiz в 5 реалистичных сценариях | closed-feature → rewrite 07 | TP | Самое ценное finding — превращение defensive throw в живой crash path. Решено через graceful skip (`toQuizItem(): QuizItem?`). |
| F3 | functional | critical | existing quiz-записи могут крашиться после миграции на edge кейсах | closed-feature → rewrite 07 | TP | Связано с F1/F2. Закрылось вместе. |
| F4 | functional | critical | Multiple components order — скрытый UX (translation > definition implicit) | closed-feature → rewrite 07 | TP | Order semantics задокументирован как контракт. |
| F5 | functional | minor | `toQuizItem` сигнатура неполная — dictionary lookup не определён (N+1 risk) | closed-feature → rewrite 07 | TP | quizComponents передаётся параметром, fetch один раз per session. |
| F6 | functional | minor | Future quiz-режимы — single column не масштабируется | closed-feature → rewrite 07 | TP | Перешли с JSON-колонки на отдельную таблицу `quiz_configs` с `quiz_mode` колонкой. Закрыло F6 структурно. |
| F7 | functional | minor | Test coverage неполное — 6 кейсов | closed-feature → rewrite 07 | TP | Расширено до 12 кейсов с crash/skip path. |

**Agent meta:**
- Sub-agent tool: `general-purpose`.
- Verify usage: использовал Read для verify реального `QuizGameImpl.kt` поведения (с file:line). Корректно.
- Prompt quality: **главный критерий — функциональная корректность** в промпте дал результат — нашёл F2 (silent throw → crash в продакшене), который другие агенты не нашли. Functional reviewer оказался самым эффективным агентом для feature-level review.
- Open question: agent оставил один soft-hint (Map vs entity for quizConfigs) — нужно явно правило «open question OR decision, не soft-hint between».

---

### IS481 — multi-agent review on new 07 (table-based) + alignment v3 — 2026-06-01

**Context:** второй проход multi-agent review после rewrite 07 на отдельную таблицу `quiz_configs` + alignment pass v3 в 02-06.

**Findings count:** 16 critical / 16 minor (от 4 агентов до фильтра + dedup).

#### Pre-triage filter (отсеиваем «полное говно и не по делу»)

| ID | Agent | Severity | Description | Filter result | Notes |
|---|---|---|---|---|---|
| A_minor#5 | architect | minor | `QuizConfigApiEntity` идентичен domain QuizConfig без преобразования | **FP filtered** | Это convention проекта — `LexemeApiEntity` тоже = domain + id. Не уникальная для IS481. |
| F_C2 | feasibility | critical | shim consistency assertion локально в mapper, mate State через `LexemeState.copy(translation=)` обходит | **FP filtered** | Это known trade-off B4/C2: assertion ловит регрессии mapper'а, mate State **специально** обходит (через `LexemeState`, не domain Lexeme). Уже зафиксировано в alignment_decisions. |
| A_C2 | architect | critical→minor | Gap-4 в alignment_decisions устарел относительно B4/C2 | downgrade to minor | B4/C2 хронологически позже Gap-4 — implicit override. Minor doc-fix: добавить cross-reference в Gap-4. |
| F_F-naming | functional | minor | 07 содержит legacy `dictionary_quiz_configs` | merged into A_C4 | Дубликат — то же что A_C4. |
| A_minor#1 | architect | minor | `json_insert($, '$[#]', ...)` под bundled SQLite | merged into F_C3 | Дубликат. |
| A_minor#2 | architect | minor | empty quiz session UX | merged into F_F6 | Дубликат. |
| F_F1 | functional | critical | definition shim сломан после миграции | merged into F_C1 | Дубликат cross-doc inconsistency. |

#### Validated findings (для triage)

**Critical (10 unique):**

| ID | Agent(s) | Description | Validity (preliminary) |
|---|---|---|---|
| AGG-1 | feasibility + functional + yagni | **Definition built-in vs user-defined** cross-doc inconsistency (04/05/06/07 расходятся). After миграции shim `lexeme.definition` всегда null → existing definitions исчезают из UI/mate/quiz. Главный блокер. | TP |
| AGG-2 | architect | Маппер `LexemeApiEntity.toDomain()` декларируется в `modules/domain/lexeme` → циклическая Gradle dep при A1. Реальный паттерн IS482 — маппер в `app/`. | TP |
| AGG-3 | architect | 07.md не переписан под OQ-1: содержит legacy `dictionary_quiz_configs` имена. Alignment v3 не дотронул. | TP |
| AGG-4 | functional | Новый dictionary после миграции — `addDictionary` не INSERT'ит default quiz_configs row → quiz lookup = empty → пустая сессия. | TP |
| AGG-5 | functional | `QuizConfig` lookup not wired in `QuizChatUseCaseImpl` / `QuizGameImpl.fetchData`. План не указывает где pre-fetch. | TP |
| AGG-6 | functional | Add definition после миграции (`@Deprecated addLexemeWithDefinition`) не sync'ит `quiz_configs.component_refs`. Definition не попадает в quiz. | TP |
| AGG-7 | feasibility | Переписывание 10 миграций тянет 10 migration-тестов + BaseMigration + utils (~30 файлов). Scope creep. Кандидат на prereq-фичу. | TP |
| AGG-8 | feasibility | SQL `json_insert($, '$[#]', json_object(...))` синтаксис требует verify под фактической bundled SQLite version. F4 order semantics под угрозой. | TP |
| AGG-9 | feasibility | Порядок вызова `seedBuiltIns(connection)` ВНУТРИ `Migration_11_12` не зафиксирован — FK violation risk на шаге 4. | TP |
| AGG-10 | architect | `QuizConfig` в `modules/domain/lexeme` — SRP violation. Trade-off accepted но не записан в decisions log. | TP |

**Minor (12 unique):**

| ID | Agent(s) | Description |
|---|---|---|
| MIN-1 | architect | Gap-4 в alignment_decisions устарел vs B4/C2 — добавить cross-ref. |
| MIN-2 | architect | Чеклист отсутствует на «`implementation(project(":modules:domain:lexeme"))` в `core-db-api/build.gradle.kts`». |
| MIN-3 | architect | Double cascade pathway `dictionary → component_values` — стоит закомментировать в 03. |
| MIN-4 | feasibility | Cascade chain integration test после ALTER TABLE DROP COLUMN. |
| MIN-5 | feasibility | `component_values.component_type_id` CASCADE — verify в 03. |
| MIN-6 | feasibility | JSON парсинг quiz_configs thread policy (не overengineer). |
| MIN-7 | feasibility | F1 invariant для future quiz_modes — зафиксировать в 07. |
| MIN-8 | yagni | `QuizConfig.toApiEntity()` / `upsertQuizConfig` write-path без потребителя. |
| MIN-9 | yagni | `restoreLexeme` API не покрыт планом миграции. |
| MIN-10 | yagni | Test gap cascade `component_types → component_values`. |
| MIN-11 | functional | Remove definition stale config (eventual cleanup). |
| MIN-12 | functional | Empty quiz session UX (corrupt config vs no lexemes) + F4 contract clarification + migration idempotency `INSERT OR IGNORE`. |

**Filter summary:**
- 32 raw findings → 22 unique после dedup → 22 - 2 FP = **20 действительных**.
- FP rate after filter: 2/32 = 6%.

**Agent meta:**
- architect — 9 findings (4 critical + 5 minor). 1 FP filtered (no-op ApiEntity). 8 TP. **89% TP rate**.
- feasibility — 9 findings (5 critical + 4 minor). 1 FP filtered (shim assert mate). 8 TP. **89% TP rate**.
- yagni — 5 findings (1 critical + 4 minor). Schema-readiness filter применён корректно (8 findings rejected sub-agent'ом ДО подачи). 5 TP. **100% TP rate** после фильтра. **Memory update сработала.**
- functional — 9 findings (6 critical + 3 minor). 1 FP filtered (dupe AGG-3). 8 TP. **89% TP rate**.
- Все 4 агента использовали встроенные Read/Grep корректно. Без bash нарушений.

**Главное наблюдение:** второй пересечённый блок (AGG-1, AGG-3, AGG-4, AGG-5, AGG-6) — все про definition status + quiz_config sync runtime. Одно фундаментальное решение (definition built-in vs user-defined) разрулит ~5 из 10 critical findings.

**Triage outcomes:**

| ID | Исход | Решение |
|---|---|---|
| AGG-1 | ✅ принято | Variant 2 — definition = user-defined per-dictionary. `BuiltInComponent.DEFINITION` удалить. Mapper заполняет shim через user-defined `name="Definition"` lookup. Mate/UI не трогаем. Детали: `_alignment_decisions.md` AGG-1. |
| AGG-6 | ✅ принято | Удалить @Deprecated definition wrappers (`addLexemeWithDefinition`, `updateLexemeDefinition`, `deleteLexemeDefinition`) из CoreDbApi + `addLexemeDefinition`/`deleteLexemeDefinition` из `WordCardUseCase`. Переписать `WordCardUseCaseImpl` / `DatasourceEffectHandler` / 2 теста на generic component API. UI блок: chip «Определение» через `WordCardState.hasDefinitionComponent`. Расширение scope IS481 на mate точечно. Детали: `_alignment_decisions.md` AGG-6. |
| AGG-2 | ✅ принято | Маппер в `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` (паттерн IS482). Циклической Gradle dep избегаем. Поправить 06.md. Детали: `_alignment_decisions.md` AGG-2. |
| AGG-3 | ✅ принято | Поправить 07.md — `dictionary_quiz_configs` → `quiz_configs`, `DictionaryQuizConfigDb/ApiEntity` → `QuizConfigDb/ApiEntity`. Убрать soft-hint про варианты A/B. Закроется в alignment v4. Детали: `_alignment_decisions.md` AGG-3. |
| AGG-4 | ✅ принято (реверс) | Возвращён в IS481: `addDictionary` auto-INSERT default `[BuiltIn(TRANSLATION)]`; Migration_11_12 для existing INSERT по факту (translation + definition если есть). В backlog остаётся только UI configurator. Предыдущая интерпретация триажа была ошибкой — слил wire и UI в одну кучу. Детали: `_alignment_decisions.md` AGG-4. |
| AGG-5 | ✅ принято (реверс) | Возвращён в IS481: `QuizConfig` lookup wire в `QuizChatUseCaseImpl` / `QuizGameImpl.fetchData`. Quiz session: для каждого `ComponentTypeRef` из конфига резолв в тип → подтянуть `component_values`. Graceful skip если требуемый компонент отсутствует у лексемы. Детали: `_alignment_decisions.md` AGG-5. |
| AGG-7 | ✅ принято | Bundled driver подключаем 2 строками (gradle dep + `setDriver` в RoomModule) как prereq-шаг IS481. 10 миграций не переписываем — Room 2.8 compat layer. Verify: прогон existing migration tests; падающие фиксим точечно. Детали: `_alignment_decisions.md` AGG-7. |
| AGG-8 | ✅ принято | Verify step в 05.md — smoke test `json_insert($, '$[#]', json_object(...))` после подключения bundled driver. Fallback на Kotlin string-building если не работает. Детали: `_alignment_decisions.md` AGG-8. |
| AGG-9 | ✅ принято | В 05.md зафиксировать порядок шагов Migration_11_12 нумерованным списком (1: create component_types, 2: create component_values, 3: seedBuiltIns TRANSLATION, 4: seed Definition user-defined, 5: transform old definitions, 6: DROP COLUMN). Шаги 3+4 до 5 — иначе FK violation. Детали: `_alignment_decisions.md` AGG-9. |
| AGG-10 | ✅ принято | `QuizConfig` / `ComponentTypeRef` остаются в `modules/domain/lexeme` (trade-off — не плодим второй domain модуль ради 2 типов). KDoc-TODO на каждом типе про вынос в `modules/domain/quiz` в backlog-фиче. Backlog entry дополнен. Детали: `_alignment_decisions.md` AGG-10. |
| MIN-1 | ✅ принято | Gap-4 в `_alignment_decisions.md` удалён целиком — полностью перечёркнут B4/C2 (translation остаётся value class @Deprecated, правка `LexemeUiItem.kt:24` не нужна). |
| MIN-2 | ✅ принято | В чеклист 05.md § «API совместимости» добавить пункт «`implementation(project(":modules:domain:lexeme"))` в `core/core-db-api/build.gradle.kts`». Без dep IS481 не соберётся. |
| MIN-3 | ✅ принято | В 03.md добавить блок «Double cascade pathway» с rationale почему два CASCADE-пути намеренны (защита от ошибочного cleanup). |
| MIN-4 | ✅ принято | В 05.md § «Тестовый план» добавить integration test cascade chain после DROP COLUMN / recreate-table: setup → delete dictionary → assert все связанные таблицы пусты. |
| MIN-5 | ❌ FP | CASCADE на `component_values.component_type_id` уже декларирован в 03.md:97. Verify показал finding некорректен — агент не дочитал DAO-секцию. Reclassify feasibility: TP 7/9 → 89% по этому run. |
| MIN-6 | ✅ принято | One-liner в 06.md около mapper `QuizConfigDb.toApiEntity()`: thread policy = inline без flowOn/cache. Защита от copy-paste с DictionaryTab. |
| MIN-7 | ✅ принято | В 07.md добавить раздел Invariants с явными F1 (полнота config для каждого quiz_mode) и F5 (no N+1 при lookup quizComponents). DDL UNIQUE не покрывает процедурный invariant. |
| MIN-8 | ✅ принято | Убрать write-mapper `QuizConfig.toApiEntity()` + DAO `upsertQuizConfig` через mapper. Оставить только: read-mapper для lookup; простой DAO `insertDefaultQuizConfig(dictId, mode)` с hardcoded JSON для `addDictionary`; direct SQL в Migration_11_12. Write-mapper появится в backlog UI configurator. |
| MIN-9 | ✅ принято | В 05.md § AGG-6 чеклист переписки добавить `WordCardUseCaseImpl.restoreLexeme` — переписать impl на generic INSERT (translation built-in + если definition != null — user-defined "Definition" тип). Сигнатура не меняется. Без точки IS481 не соберётся. |
| MIN-10 | ✅ принято | В 05.md § «Тестовый план» добавить test cascade `component_types → component_values`: DELETE component_type → assert component_values с этим type_id удалены. Прямой test, отдельно от MIN-4 (через dictionary). |
| MIN-11 | ✅ принято | Оставляем JSON-storage (normalized — over-engineering, evolution не предвидится). F6 invariant в 07.md + DAO `deleteComponentType` в backlog: атомарный cleanup `quiz_configs.component_refs` через `json_remove` + DELETE. В IS481 cleanup НЕ реализуем (нет UI триггера). |
| MIN-12 | ✅ принято частично | (a) Empty quiz session UX — оставляем как есть в IS481, backlog улучшит. (b) F4 invariant: позиция в JSON-массиве = display order — добавить в 07.md § Invariants. (c) `INSERT OR IGNORE` — отклонено как FP (Room migrations atomic by default). |

---

## Run 5: IS481 v3 review (после alignment v4) — 2026-06-03

После закрытия всех 22 findings v2 (AGG-1..10 + MIN-1..12) и применения alignment v4 к 02-07 — повторный прогон всех 4 агентов параллельно.

**Сырые числа:**

| Agent | Submitted | TP (новые валидные) | FP (фича в planning, не реализована) | DUP (повтор AGG-N / MIN-N) | PARTIAL |
|---|---|---|---|---|---|
| architect | 10 | 0 | 9 | 1 (F1 = MIN-2) | 0 |
| feasibility | 16 | 5 (F7, F8, F15, F16, F11) | 6 | 4 (F1, F3=MIN-2; F5=AGG-7; F12=MIN-10; F14=AGG-8) | 1 (F6) |
| yagni | 0 | 0 | 0 | 0 | 0 |
| functional | 12 | 2 (F8 canRemove, F11 soft-delete) | 8 | 1 (F10=AGG-8) | 1 (F7 self-FP) |

**Реально новые валидные findings:** ~7 из 38 = **18% TP rate**.

**Главный провал:** ~60% findings (architect + functional + часть feasibility) — пересказ скоупа IS481 как «недостатков текущего кода». Агенты не различили «фича в планировании» vs «фича реализована».

**Главное наблюдение yagni-агента (важно):** «План IS481 well-engineered. Все сложные решения явно задокументированы и обоснованы. Schema-readiness фильтр работает корректно.» — подтверждает что после triage v2 + alignment v4 план готов к implement.

## Prompt quality lessons (run 5)

### Что не сработало в per-run prompt

1. **Не явное разделение фазы фичи.** Не сказал явно «фича в планировании, отсутствие в коде domain/DAO/migration/wire — НЕ finding». Архитектор/functional агенты увидели отсутствие в коде → подали 10+ findings вида «X нет в коде».

2. **Не передал список уже triagged findings.** Сказал «не повторять AGG-1..10 / MIN-1..12», но не передал короткие названия. Агенты повторяли AGG-7, AGG-8, MIN-2, MIN-10 потому что не знали что они уже закрыты.

3. **Per-run prompt = 600-800 слов специфики.** Растил каждый раз. Часть была reusable (формат вывода, запреты bash). Эту часть переносим в stable `agents.md` — см. § «Общие правила».

### Что добавить в следующий run

1. **Stable правила вынесены в `agents.md` § «Общие правила для всех агентов»** (одноразово, файл обновлён).

2. **Per-run brief короткий (под 500 слов)** — см. шаблон в `agents.md` § «Per-run brief template». Содержит: фаза (planning/implementation), цель, скоуп, out-of-scope, пути документов / кода. Triagged findings — ссылкой на `_alignment_decisions.md` + `findings_log.md` § Triage outcomes (агент сам прочитает).

3. **Phase explicit в brief:** «**Фаза:** planning. Отсутствие реализации в коде НЕ является finding.»

### Agent-specific lessons

- **architect** (95% FP в этом run) — хуже всех справился с фазой. Промпт architect должен **первым делом** упоминать phase разделение.
- **functional** (75% FP) — та же проблема, но также находит ценные user-сценарий findings когда не путает фазу.
- **feasibility** (~50% FP) — менее affected, потому что критерий «runtime safety» легче применить к плану.
- **yagni** (0 findings, идеально) — schema-readiness filter работает, фильтр через memory `feedback_yagni_schema_readiness.md` сработал.

### Action items для следующего review run

- [x] Обновить `agents.md` с общими правилами (Phase + Triagged + Schema-readiness + Verify + Tools).
- [x] Добавить per-run brief template в `agents.md`.
- [ ] При следующем review (после реализации IS481, например) использовать короткий brief из template + ссылку на `agents.md`.

---

## Run 7: IS481 v3 review (с acid-test в agents.md) — 2026-06-03

После run 6 в `agents.md` добавлен **acid-test**: «Если фича успешно завершится по плану — finding всё ещё валиден?» + явные примеры что finding / что не finding в planning фазе. Brief идентичен run 6.

**Сырые числа:**

| Agent | Submitted | Self-FP (acid-test применил сам) | TP (новые валидные) | Phase confusion | DUP triagged |
|---|---|---|---|---|---|
| architect | 6 | 0 | 4 (F3, F4, F5, F6) | 2 (F1, F2) | 0 |
| feasibility | 7 | 1 (F8) | 2 (F1 atomicity DUP, F4 B2-AGG-7 conflict) | 3 (F2, F3 верификация DUP, F5 edge) | 1 (F3=AGG-8) |
| yagni | 0 (11 examined) | — | 0 | 0 | 0 |
| functional | 2 (13 examined) | 11 (acid-test применил к каждому candidate) | 2 (F5, F8) | 0 | 0 |

**Реально новые валидные:** 8 из 15 поданных = **53% TP rate** (utрое больше run 5).
**Self-FP:** 1 (feasibility F8).
**11 candidates** functional агент явно отклонил через acid-test ДО подачи.

## Run 8: повторный sanity check с передачей triaged списка — 2026-06-03

После run 7 запущен idemtical brief, но с **явной передачей списка triaged AGG-1..10, MIN-1..12, N1..N8** в brief каждого агента. Гипотеза: список снизит повторы. **Гипотеза опровергнута.**

**Сырые числа:**

| Agent | Submitted | TP (новые валидные) | DUPs цитированные | Phase confusion |
|---|---|---|---|---|
| architect | 5 | 0 | A2 (AGG-7), A3 (MIN-2) | A1, A4, A5 |
| feasibility | 12 | 3 (F5, F8, F12) | 7 цитированных triaged | F3, F6 |
| yagni | 3 (10 examined) | 0-1 marginal | — | — |
| functional | 10 | 3 (F5, F8, F9) | 7 цитированных triaged | F1, F3, F6 |
| **Total** | **30** | **~6** | **~16** | **~7** |

**TP rate подачи: ~23%** (vs run 7 — 53%).

**Парадокс:** передача triaged списка → agent видит ID → **цитирует в Verify** («я процитировал AGG-7») → **всё равно подаёт** finding под флагом «решено в плане, не в коде». Phase confusion усилился потому что agent теперь имеет explicit reference на triaged decision как доказательство «это запланировано но не реализовано = TP».

**Yagni агент тоже регрессировал:** из стабильных 0 findings (runs 5/6/7) → 3 marginal в run 8. Передача triaged списка дала ему контент для повторной обработки (хотя сам agent отметил 7 из 10 candidate'ов как FP).

## Закрытие findings run 8

Финальные правки в план: 3 валидных (M1-M3) применены + 1 minor (feasibility F5) закрыт; 5 minor reject'нуты с обоснованием.

| ID | Source | Статус | Резолюция |
|---|---|---|---|
| M1 | feasibility F8 (повтор runs 5/7) | ✅ исправлено | В `06.md` `String.toComponentTypeRefList()` обёрнут в try-catch с graceful fallback на `emptyList()`; контракт quiz UseCase для пустого config — empty session, не crash |
| M2 | feasibility F12 | ✅ исправлено | В `05.md` § Bundled SQLite — pin минимальной версии `sqlite-bundled` + symmetric verify smoke test для `ALTER TABLE DROP COLUMN` + JSON1 функций; fallback на recreate-table если DROP COLUMN не поддерживается |
| M3 | functional F9 | ✅ исправлено | В `05.md` § Integration tests добавлен test case «interrupted migration restart» — verify что Room rollback оставляет БД в v11 и restart проходит чисто (защита от батареи / OOM) |
| — | feasibility F5 (partial UNIQUE в snapshot) | ✅ исправлено | В `03.md` добавлена note для schema review: partial UNIQUE не в Room `12.json` snapshot — норма; migration test проверяет наличие через `sqlite_master` |
| — | functional F5 (orphan lexeme) | ❌ reject | Edge case impossible state в production (UI validation требует хотя бы одно поле); MIN-4 частично покрывает; UI gracefully ничего не показывает, не crash |
| — | functional F8 (component order) | ❌ reject | JSON1 array сохраняет order по RFC 8259 спецификации; `ComponentTypeRefJsonTest` round-trip implicitly покрывает |
| — | yagni F1 (Multi-level @Relation perf) | ❌ reject | Pattern verified в проекте (TermDb → LexemeDb → SampleDb); план явно ссылается |
| — | yagni F2 (JSON v:1 forward-compat fallback) | ❌ reject | Agent сам PARTIAL — schema-readiness, не over-engineering; fallback появится при реальной evolution payload (out of scope IS481) |
| — | yagni F4 (canRemoveComponent bounded context) | ❌ reject | TODO в плане уже как marker для revisit при добавлении non-meaning built-in типов |

## Сравнение run 7 vs run 8 — ключевой инсайт

| Метрика | Run 7 (без triaged списка) | Run 8 (с triaged списком) |
|---|---|---|
| Total submitted | 15 | 30 |
| TP rate | 53% | 23% |
| Yagni findings | 0 | 3 |
| Architect TP | 4 | 0 |
| Cited triaged в Verify, но всё равно подано | 0 | ~16 |

**Вывод для будущих review:**

1. **НЕ передавать список triaged AGG-N / MIN-N в brief.** Лучший формат — run 7: stable rules в `agents.md` + acid-test + agent сам читает `_alignment_decisions.md` через правило #3.
2. **Передача списка создаёт content для повтора, а не запрет.** Phase confusion усиливается когда agent имеет explicit reference на triaged как доказательство.
3. **Yagni — fragile**. Стабильно идеален при пустом контексте triaged; при передаче списка регрессирует.

## Финальное состояние IS481 plan

После всех 8 runs review + alignment v4 + 22 finding closures (AGG-1..10, MIN-1..12) + 8 N-findings (run 7) + 3 M-findings (run 8) + 1 partial UNIQUE note:

- **План well-engineered** (подтверждено independent runs).
- **Все critical findings closed.**
- **Acid-test проверен** (run 7 = 53% TP rate).
- **Limit of value** для дальнейших review-проходов достигнут — run 8 дал ~3 новых TP при 30 findings (vs 8 TP при 15 в run 7).
- **Ready to implement.**

---

## Сравнение run 5 vs run 6 vs run 7 vs run 8

| Метрика | Run 5 | Run 6 | Run 7 | Run 8 |
|---|---|---|---|---|
| Total submitted | 38 | 31 | **15** | 30 |
| Реально новых TP | ~7 | ~5 | **8** | ~6 |
| TP rate | 18% | 16% | **53%** | 23% |
| Self-FP | 0 | 5 | 1 (+11 examined-rejected) | 0 (+7 examined-rejected yagni) |
| Architect submitted | 10 | 7 | 6 | 5 |
| Architect TP | 0 | 0 | **4** | 0 |
| Feasibility submitted | 16 | 12 | 7 | 12 |
| Feasibility TP | 5 | 3 | 2 | 3 |
| Functional submitted | 12 | 12 | 2 | 10 |
| Functional TP | 2 | 1 | 2 | 3 |
| Yagni submitted | 0 | 0 | 0 | 3 (regression) |
| Triaged cited but submitted | n/a | n/a | 0 | **16** |

**Промт изменения по run'ам:**
- Run 5 → 6: добавлены stable rules в `agents.md`.
- Run 6 → 7: добавлен **acid-test** + явные примеры что finding / что не finding.
- Run 7 → 8: добавлена **передача triaged списка** в brief. **Регрессия.**

**Best practice (по результатам 8 runs):** Run 7 формат. Stable rules + acid-test, БЕЗ передачи triaged списка. Agent сам читает `_alignment_decisions.md` через правило #3.

**Главный выигрыш run 7:**
- **Шум упал в 2.5×** (38 → 15 findings).
- **TP rate утроился** (18% → 53%).
- **Architect TP rate 0% → 67%** — даже при phase confusion в F1/F2, F3-F6 нашли реальные cross-doc inconsistencies (включая критичный gap `BuiltInComponent.DEFINITION` leftovers в 06.md mapper code).
- **Functional агент** применил acid-test к 13 candidates, явно отклонил 11. Подал только 2 minor finding'а — оба валидны (F5 F1 maintenance path, F8 rename component_type).
- **Yagni** стабильно 0 findings — schema-readiness фильтр + acid-test работают идеально для этого агента.

## Закрытие findings run 7

| ID | Source | Статус | Резолюция |
|---|---|---|---|
| N1 | architect F3 | ✅ исправлено | `BuiltInComponent.DEFINITION` leftovers в 06.md (стр. 312 mapper, стр. 587 test cases) — переписаны на user-defined lookup |
| N2 | architect F4 | ✅ исправлено | 07.md строки 109-115 soft-hints A/B — заголовок и текст переписаны под принятый OQ-1 |
| N3 | architect F5 | ❌ FP | Path-комментарий `// core-db-impl/...` уже на 06.md:127 над блоком кода |
| N4 | architect F6 | ❌ FP | MIN-2 dep пункт уже в § Bundled SQLite prereq (05.md:16) |
| N5 | feasibility F4 | ✅ исправлено | B2 (decisions.md:119) помечен `[obsolete — см. AGG-7]` с cross-ref и зачёркнутым оригинальным текстом для аудита |
| N6 | feasibility F1 | ✅ исправлено | В 05.md добавлен пункт «Атомарность `addDictionary` + `insertDefaultQuizConfig`» с явным `@Transaction` контрактом и test'ом на FK violation |
| N7 | functional F5 | ✅ исправлено | В Backlog «Quiz config UX» добавлен пункт «F1 invariant maintenance: при добавлении нового `quiz_mode` миграция INSERT default для всех existing dictionaries» |
| N8 | functional F8 | ✅ исправлено | В Backlog «Quiz config UX» + 07.md F6 invariant добавлено: F6 покрывает DELETE, rename — отдельная атомарная операция через `json_replace`; rename в IS481 не поддерживается (immutable component_types) |

## Acid-test lessons (run 7)

### Что сработало

1. **Functional агент** — образцовое применение acid-test. Из 13 candidates подал только 2 минорных valid finding'а, остальные 11 явно отклонил с конкретным cross-ref на план. TP rate подачи **100%**.

2. **Yagni** — стабильно 0 findings. Schema-readiness фильтр + acid-test работают идеально, агент сам сослался на C5-C10 и MIN-8 как уже triagged.

3. **Architect** — нашёл реальные cross-doc inconsistencies, которые я пропустил в alignment v4 (`BuiltInComponent.DEFINITION` в 06.md mapper code — compile error при implement). Без acid-test agent утонул бы в phase confusion findings (как в run 5).

### Что не сработало

1. **Architect F1/F2** — phase confusion остался для самых базовых cases («поле X нет в коде, план говорит добавить»). Agent написал «acid-test пройден» но интерпретировал тест как «is it in plan?» вместо «will feature close it?». Правило #1 в agents.md недостаточно явно для этих cases.

2. **Feasibility** TP rate просел (5 → 3 → 2). Часть findings — повторы из runs 5/6 (atomicity, JSON verify) — что **подтверждает их валидность** через independent re-discovery, но также показывает что **этот агент не читал _alignment_decisions.md** триaged findings (потому что мы намеренно их не передавали).

### Что улучшить для run 8+ (если будет)

1. **Усилить правило #1** в agents.md ещё одним acid-test примером для architect: «`X.kt` не содержит поле `Y`, план говорит добавить → НЕ finding, это и есть scope».

2. **Передавать триaged AGG-N / MIN-N список** в brief — снимет повторы (feasibility F3, F4 etc.). Чистый тест «без видимости triaged» был полезен один раз чтобы откалибровать; теперь можно вернуть для эффективности.

3. **Architect промпт** — явно сказать что найти inconsistency в плане **ценнее** чем найти отсутствие реализации.

## Action items run 7

- [x] Все 8 findings закрыты (5 corrections + 2 FP + 1 documentation).
- [x] B2 в decisions помечен obsolete с cross-ref на AGG-7.
- [x] `BuiltInComponent.DEFINITION` leftovers в 06.md устранены.
- [x] Backlog «Quiz config UX» дополнен F1 maintenance + rename operation contract.
- [x] Решение про run 8: **НЕ передавать** triaged списка — run 8 показал регрессию (53% → 23% TP rate, yagni 0 → 3 marginal). Best practice — run 7 формат.

---

## Сводная статистика (по 8 runs)

| Agent | Total | TP | FP | PARTIAL | TP rate |
|---|---|---|---|---|---|
| architect | 44 | 24 | 18 | 2 | 55% (run 7 — 67% peak; run 8 — 0% после передачи triaged) |
| feasibility | 54 | 28 | 23 | 3 | 52% (run 7 — best, run 8 регрессия) |
| yagni | 24 | 14 | 8 | 2 | 58% (runs 5/6/7 — 0 findings; run 8 — 3 marginal после передачи triaged) |
| functional | 40 | 22 | 16 | 2 | 55% (run 7 — best 100% подача, run 8 регрессия) |

**Эволюция TP rate подачи по runs (после агрегации):**

| Run | Total submitted | TP rate подачи | Главное изменение |
|---|---|---|---|
| 1-4 | 79 | ~83% | baseline до alignment |
| 5 | 38 | 18% | повторный review без `agents.md` rules + без triaged context |
| 6 | 31 | 16% | `agents.md` rules добавлены |
| **7** | **15** | **53%** | **acid-test добавлен** — best result |
| 8 | 30 | 23% | передача triaged списка → **регрессия** |

**Главный вывод:** Comprehensive review **до** triage даёт ~83% TP rate (агенты находят реальные issues). **После** triage без передачи списка triaged — TP rate резко падает (повторы + phase confusion). Решения:
1. **Acid-test** — частично решает phase confusion (run 7 vs run 5: +35 пунктов).
2. **Передача triaged списка** — снимет повторы (будет проверено в потенциальном run 8).
3. **Yagni агент** — стабильно идеален во всех runs благодаря schema-readiness фильтру в memory.

**Наблюдения:**
- **functional** + **feasibility** + **architect** — стабильно высокие (87-95%). Хорошие узко-focused промпты.
- **yagni** — после применения `feedback_yagni_schema_readiness.md` memory во 2-м run TP rate взлетел до 100%. Memory работает.
- **Самые ценные findings** — runtime crash (B1/B2/F1/AGG-1) и user-flow gaps (AGG-4/5/6). Должны быть приоритет в промптах.
- **Pattern:** при cross-doc rewrite (07 переписано, 02-06 v3 alignment) образуется terminology drift — 5+ findings про definition built-in/user-defined inconsistency. **Recommendation для промптов:** в architect/feasibility добавить explicit «cross-doc terminology check — для каждого ключевого термина (BuiltInComponent values, table names, system_key) проверь identical interpretation across all docs».
- **Dedup в финале** — 32 raw → 22 unique. Дубликаты между агентами полезны как cross-validation, но требуют merge перед triage.

---

### IS481 component_constructor — design review template_model.md (architect + qa + senior + inquisitor) — 2026-06-13

**Context:** обзор готовности `template_model.md` к flow реализации. 3 параллельных subagent'а: architect / qa_engineer / senior. После — inquisitor поверх их findings (merge + filter).

**Findings count (raw):** 34 (10 architect + 14 qa + 10 senior).
**После inquisitor merge:** 8 unique (F-N1..F-N8).

| ID | Agent | Severity | Description | Triage | Validity | Notes |
|---|---|---|---|---|---|---|
| F-N1 | architect+qa+senior | critical | Race / duplicates на `is_multi=false` без БД-constraint — concurrent INSERT проскользнут | closed-feature | TP | Решено через `@Transaction` DAO + SELECT COUNT + UI блокирует save во время операции. Partial UNIQUE отвергнут (Room не видит partial index). |
| F-N2 | architect+senior | critical | Type-safety регрессия с `Map<String, Primitive>` — string-keys, runtime `as?`, опечатки compile-OK | closed-feature | TP | Решено через typed views per template (sealed `TemplateValues` + parser сразу выдаёт typed, Map не существует в коде). Создан `typed_views.md`. |
| F-N3 | architect+senior+qa | critical | M13 слон — composite rewrite + drop UNIQUE + is_multi + timestamps everywhere в одной миграции | closed-feature | TP | Решено split: M13 (composite + cardinality + timestamps `component_*`), M14 (repository-wide rename). Backlog запись добавлена. |
| F-N4 | architect+qa | critical | Soft-delete query-pattern не зафиксирован — `WHERE removed_at IS NULL` обязателен? cascade паттерн? | closed-feature | TP | Решено через аудит таблиц (`deletion_concept.md` § Текущее состояние) + JOIN-based cascade hiding + convention `WHERE removed_at IS NULL` в DAO. БЛОКЕР flow снят. |
| F-N5a | qa | major | Multi→single downgrade при count>1 — поведение не специфицировано | closed-feature | TP | Решено: жёсткий запрет downgrade при наличии лексем с count>1. UI показывает count + preview ~3. Bulk-actions запрещены. |
| F-N5b | qa | major | Field rename в template-schema без migration plan → silent breakage | closed-feature | TP (переформулирован) | Изначально предлагалась migration-при-rename; после уточнения пользователя — template immutable после релиза, golden fixture round-trip защищает. |
| F-N6 | architect+senior | major | Resolver `ComponentTemplate → Composable` location, fromKey nullability — implementation detail | rejected | FP | Пользователь возразил: location уже сказан в documenti (widget module), API resolver — implementation, fromKey nullable — естественное следствие. Inquisitor пропустил implementation detail. |
| F-N7 | architect+senior+qa | major | JSON-парсер не выбран, schema mismatch шум при расширении template | closed-feature | TP (частично) | (a) парсер `org.json.JSONObject` (текущий) — оставляем; (b) schema mismatch noise исчезает потому что template immutable. |
| F-N8 | qa | major | Атомарность save composite-лексемы — partial state при ошибке | closed-feature | TP | Решено через `@Transaction` repository-метод + unit test rollback на FK violation. |

**Filtered (12 из 34 → 8 unique, исключения):**

| ID | Agent | Cause | Notes |
|---|---|---|---|
| F-A1 | architect | dедуп | Schema module location — решение в документе (widget module). |
| F-A9 | architect | гипотетика | `@RewriteQueriesToDropUnusedColumns` — не используется в проекте. |
| F-A10 | architect | гипотетика | JSON schema versioning — без триггера. |
| F-Q6 | qa | театр | Forward-compat при downgrade — однопользовательская app, sync нет, destructive fallback уже стоит. |
| F-Q7 | qa | дубль F-N3 | Repository-wide rename аудит — вошёл в F-N3 split. |
| F-Q10 | qa | implementation | TTL race vs recovery — тривиально решается single check, не design. |
| F-Q11 | qa | minor | «Empty + multi» = 0 rows, не violation; покрыт правилом «всё пустое = не пишется». |
| F-Q12 | qa | косметика | `removed_at` nullable явно следует из § Timestamps. |
| F-Q14 | qa | проверяемый факт | SQLite ≥ 3.38 — bundled SQLite уже подключён. |
| F-S7 | senior | meta | Test strategy в документе — оформление doc, не дефект архитектуры. |
| F-S8 | senior | out-of-scope | MVP-only-TEXT vs composite-unified-сейчас уже решено в Open Q #6. |
| F-S10 | senior | рутина | `addDate`/`changeDate` rename — implementation работа. |

**Agent meta:**
- architect — 10 findings (3 critical + 4 major + 3 minor). Inquisitor приял 5 как источники (50%).
- qa — 14 findings. Inquisitor приял 6 как источники (43%). Много дублей с architect.
- senior — 10 findings (3 critical + 4 major + 3 minor). Inquisitor приял 5 (50%).
- inquisitor — корректно дедуплицировал, отсеял большую часть театра и implementation detail. F-N6 пропустил implementation level.
- Verify usage: все агенты использовали Read/Grep правильно, без bash grep против правил.
- Prompt quality: explicit раздел «не дублировать architect-уровень» в qa промпте помог снизить дубли (но не до нуля).

---

### IS481 component_constructor — финальный аудит документов (consistency + gap + inquisitor) — 2026-06-15

**Context:** финальный аудит всех документов фичи на белые пятна и противоречия перед запуском flow. 2 параллельных subagent'а: consistency / gap-analysis. Inquisitor поверх их findings.

**Findings count (raw):** 27 (12 consistency + 15 gap).
**После inquisitor merge:** 9 unique (F1..F9).
**После пользовательского design-фильтра:** **3 real** (F6, F7, F8). 6 из 9 inquisitor-finalized оказались implementation-уровнем.

| ID | Agent | Severity | Description | Triage | Validity | Notes |
|---|---|---|---|---|---|---|
| F6 (C4+C5) | consistency | major | `typed_views.md` цитирует и инструктирует править разделы которых в template_model уже нет | closed-feature | TP | Document drift. После rewrite template_model — typed_views остался с устаревшим self-instruction. |
| F7 (C2+C6+C12) | consistency | major | Target-state (post-M13) vs current-state (M12) не маркированы — читатель не отличает | open | TP | Документы описывают финальный дизайн как уже-сделанное. Нужен disclaimer/marker. |
| F8 (C8+C9) | consistency | minor | `deletion_concept.md` self-reference на закрытый Open Q #4 + формулировка «values потеряется» при soft-delete вводит в заблуждение | open | TP | Внутренние противоречия после серии правок. |
| F1 | gap | critical | UseCase + DAO API контракт не описан | rejected | FP (phase) | Implementation phase concern, не design. Inquisitor не отфильтровал. |
| F2 | gap | critical | State / Msg / Reducer / Effect двух экранов | rejected | FP (phase) | Implementation phase (business_contract step). |
| F3 | gap | major | Uniqueness rules при создании (trim, max length, case) | rejected | FP (phase) | Implementation validation rules. |
| F4 | gap | major | Module structure + navigation routes + DI wiring | rejected | FP (phase) | Implementation. UI placement уже зафиксирован. |
| F5 | gap | major | Tests scope (fixtures, migration test, downgrade test) | rejected | FP (phase) | Test step flow. |
| F9 | consistency | minor | `fromKey` nullable не отдельный item в plan | rejected | FP (phase) | Implementation detail. |
| C1 | consistency | minor | `Primitive.Color` декларирован но MVP=TEXT — нет «scope: future» | rejected | косметика | Disclaimer-label, MVP=TEXT уже сказан. |
| C3 | consistency | minor | Migration M12→M13 JSON формат — иллюстративный, нужна сверка с реальным | rejected | implementation | Implementation detail миграции. |
| C10 | consistency | minor | «Storage выбран в IS481 main» — ссылка нерезолвируется | rejected | косметика | Контекст предыдущего flow. |
| C11 | consistency | minor | Backlog ссылки без якоря | rejected | косметика | Минорная неточность. |
| G6 | gap | major | UI компоненты API (диалоги, badges, rows) | rejected | implementation | Implementation phase. |
| G8 | gap | major | Strings / Resources список | rejected | implementation | Resource files на implementation. |
| G9 | gap | major | Accessibility (contentDescription, screen reader) | rejected | implementation | A11y — implementation requirement. |
| G15 | gap | minor | i18n locale-specific сравнение (ROOT locale) | rejected | implementation | Coding standard, не design. |

**Agent meta:**
- consistency — 12 findings. После inquisitor 8 unique. После design-фильтра пользователя — **3 real** (signal rate 25%). Хорошо для cross-file drift detection.
- gap — 15 findings. После inquisitor 11 (слиты в F1-F5). После design-фильтра — **0 real** (signal rate 0%). Все findings — implementation phase (UseCase/DAO API, MVI контракт, tests, modules, DI, validation rules, strings, a11y, locale).
- inquisitor — корректно дедуплицировал и отсеял часть (косметика, явный impl-checklist), **но не отделил phase для core findings** (UseCase API, MVI, validation rules — слил как «Critical design» вместо «implementation»). Главный проёб этой сессии. Misjudge rate: 6 из 9 financial findings = 67%.

**Главный проёб inquisitor'а:** не отделил «design-phase concerns» от «implementation phase concerns». На вход inquisitor получил implementation-level gap findings, отсеял только явный checklist (strings, a11y), но UseCase сигнатуры / MVI / validation слепил как design-critical. После пользовательского ревью — все они вылетели.

**Прoёб gap-промпта:** «найди белые пятна» без явного отделения phase даёт спам implementation concerns. **Lesson:** gap-агент должен иметь явное определение что есть design-phase в проекте (= где экран, что показывает, основные UI flow, технические детали критичные для понимания) vs что не design (UseCase сигнатуры, MVI контракт, validation rules, DI, tests, strings, a11y, locale).

**Verify usage:** все агенты использовали Read/Grep правильно.
**Prompt quality:** consistency-промпт работает (25% signal rate). Gap-промпт требует переписать с explicit phase boundary.
