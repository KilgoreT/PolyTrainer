# Постмортем — IS481 quiz_component_picker

Дата: 2026-06-11
Ветка: `IS481_lexeme_component_constructor`
Flow: ForgeFlow adaptive (33 шага: 18 done + 6 ui_* skipped + 5 data_* skipped + 5 infra_* skipped + 1 figma_dump skipped + 1 publish_spec без публикации)
Verdict: APPROVED WITH FOLLOWUPS (5 senior findings → backlog) + 1 UX-fix post-review.

---

## 1. Цели vs достижения

### Изначальные цели

(из `00_task.md` + `02_scope.md` после 3 итераций scope_analysis)

- Picker компонента квиза в chat-меню: динамический radio из `availableTypes` текущего словаря, без хардкода имён типов.
- Edge cases: один тип — `disabled+checked`; несколько — все enabled, дефолт = первый по `position`.
- Persistent per-dictionary, storage решение в design-фазе (prefs vs `quiz_configs`).
- Квиз отдаёт ТОЛЬКО выбранный компонент: `QuizGameImpl.fetchData` фильтрует `componentRefs` до single-element list перед `toQuizItem`.
- Применение выбора — на следующий `loadData()` (lifecycle не ломаем).
- Без новых enum/sealed; использовать существующие `ComponentTypeRef` / `ComponentType` из `modules/domain/lexeme`.
- БЕЗ DB schema / migration.

### Достигнуто

- Picker встроен в `ActionsWidget` между `MistakesMenuItem` и debug-блоком; submenu+radio через новые Tier 1 primitives `LexemeSubmenuMenuItem` / `LexemeRadioMenuItem` в `core/ui/dropdown/`.
- Полный MVI-цикл: `LoadQuizComponentTypes` (one-shot на `PrepareToStart`) + `QuizPickerFlowHandler.subscribe` (reactive re-emit на pref-writes); `SaveQuizPickerSelection` пишет через `useCase.setQuizPickerSelection`.
- Persistence — `PrefsProvider` raw-string API + composite key `quiz_picker_dict_<id>`; encoding `builtin:<key>` / `user:<name>` через `substringAfter(':')`. Cross-dictionary isolation и cold-start restore verified user 2026-06-11.
- `QuizGameImpl.fetchData` фильтрует: `effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs`; null → fallback на исходный config (preserves семантику до первого выбора).
- Тесты PASS: `ComponentTypeRefExtTest` (5), `QuizChatUseCaseImplTest` (23), `ChatReducerTest` (10), `DatasourceEffectHandlerTest` (4), `QuizPickerFlowHandlerTest` (4), `QuizGameImplFetchDataTest` (4). Lint clean, `assembleDebug` OK.

### Расхождения

- **Storage decision сузился** к prefs (вариант `quiz_configs.component_refs` отброшен, зафиксирован как backlog).
- **Scope-drift M1** (poymal global_code_review): `ActionsWidget` дополнительно обернул debug-блок в `if (BuildConfig.DEBUG)` + включил `buildFeatures.buildConfig = true` в `quiz/chat/build.gradle.kts`. Поведение release-сборки изменено, не описано в `business_implement` / `check`, не покрыто тестами. Pre-flow user-driven fix, попал в working tree.
- **UX-расхождение → backlog (manual smoke 2026-06-11):** `LexemeSubmenuMenuItem` остался inline accordion вместо ожидаемого cascaded popup (Material). Followup записан с двумя путями (`me.saket.cascade` либо manual nested `DropdownMenu`).
- **UX-fix post-review:** `size == 1` сначала был disabled header — юзер не видел что выбрано. Fix через Edit (убрать `enabled = isPickerEnabled` с `LexemeSubmenuMenuItem` в `QuizComponentMenuItem`). Verified на Mi A1 user-confirmed «работает».
- **Production логи `###QuizPicker###` отсутствуют** вопреки `business_summary` / `checklist_init` — ручная проверка опирается на `###MATE###` / `###CHAT###`.

### Verdict

Picker-фича закрыта целиком в рамках своего scope (4 acceptance scenarios verified, code review APPROVED WITH FOLLOWUPS), шипится с задокументированным долгом: 5 senior findings (F1-F5) + atomicity tests + honest return + cascaded popup в backlog. Один не задокументированный scope-drift (debug-menu wrap в `BuildConfig.DEBUG`).

---

## 2. Проблемы в исполнении flow

### Rerun'ы и feedback iterations

Из 18 выполненных шагов 6 потребовали iter ≥ 2:

| Шаг                    | Iter | Причина rerun'а                                                                                  |
|------------------------|------|--------------------------------------------------------------------------------------------------|
| scope_analysis         | 3    | qa 3 critical iter 1; iter 2 закрыл findings; iter 3 — обобщение под любой ComponentType + LOCK prefs |
| business_contract_review | 2  | Reducer-таблица собирала `SaveQuizPickerSelection(dictionaryId, ref)` против Effect declaration  |
| business_contract_spec | 2    | architect critical: `ComponentType.toRef()` dead ref → добавили declaration в UseCase раздел     |
| business_design_tree   | 2    | **Sub-agent deviation:** положил Tier 1 в `widget/iconDropDowned/` вопреки convention            |
| business_test          | 2    | QA 9 findings (F1-F9 edge cases/coverage gaps); iter 2 закрыл все                                |
| business_implement     | 2    | senior 8 findings (0 critical); F1 fixed, F2-F5 → Backlog                                        |

### Scope leak (главный дефект флоу) — IS481-F16

6 шагов UI sub-flow (`ui_walkthrough`, `ui_layout`, `ui_design_tree`, `ui_implement`, `publish_ui`, `ui_summary`) skipped как «duplicate work». Причина: `business_design_tree` sub-agent проспавнен с инструкцией покрыть все 17 узлов фичи в одном графе — включая Tier 1 UI primitives, Tier 3 wrappers, ActionsWidget integration. Sub-agent реализовал их в `business_implement`. Architect review графа PASS'нул — проверял согласованность, не layer boundary. Код в правильных модулях, тесты PASS — артефакты по решению пользователя не fix'ались. Layer-boundary check в `business_design_tree` ревью отсутствует — это дефект flow процесса, не sub-agent'а.

### Известные FF improvements (FlowBacklog)

- **IS481-F11** (главный systemic) — verify library API contract через Read real source ОБЯЗАТЕЛЕН; 2 повтор вопроса пользователя = немедленный Read source.
- **IS481-F12** — BOOTSTRAP.md: prose-шаги 1-4 → pseudo-code блок + печатать `READ: <path>` как proof trail.
- **IS481-F13** — DSL refactor: разделить `name:` (identity) и `prompt:` (step-file reference). **Реализовано** в base FF + 16 шагов overlay по ходу триажа.
- **IS481-F14** — F13 deployment known limitation: in-flight paused plans без `prompt:` поля. Для PolyTrainer не материализуется.
- **IS481-F16** — layer-boundary check отсутствует в `business_design_tree` review (см. выше).

### Session feedback issues (вне формального флоу)

После `global_code_review.md` (APPROVED) пользователь нашёл 2 UX-проблемы вне области ревью: (1) при `size == 1` picker disabled — нечего выбирать; (2) accordion-стиль раскрытия против cascaded popup. Обработано без перезапуска flow: один Edit в `QuizComponentMenuItem.kt` для (1) + запись в `docs/Backlog.md` для (2).

### Operational mishaps в session

- **Gradle build pipeline.** Прямой `./gradlew assembleDebug` через Bash tool выдавал пустой stdout / неинтерпретируемый stack trace. Пришлось писать `scripts/cc-build.sh` — 4 итерации скрипта (`>` redirect → `tee` → `tee -i` → `--no-daemon` → финал с explicit verdict по EXIT). Корневая причина: gradle daemon в этой harness шлёт UP-TO-DATE output минимально / через сокет, теряется в pipe.
- **Logcat misinterpretation.** Agent утверждал что видел restored state при cold start; пользователь поймал — agent смотрел не тот фрейм лога, реально cold start не проверялся до force-stop. Повторная проверка с force-stop confirmed restore.
- **Sub-agent path deviation.** `business_design_tree` iter 1: Tier 1 primitives в `widget/iconDropDowned/` вместо `modules/core/ui/dropdown/`. Восстановлено через Edit в iter 2 + senior F1 (`firstOrNull → first` после `isEmpty` guard) через Edit поверх sub-agent работы.

---

## 3. Решения и долги

### Ключевые архитектурные решения

1. **Persistence через `PrefsProvider` raw-string API, не новая DB-таблица.** Альтернативы: JSON-blob под одним ключом (требовал `kotlinx.serialization`, новая dep) и запись в `quiz_configs.component_refs` (нарушал scope). Минимальный путь — `getStringByRawKey` / `getStringFlowByRawKey` / `setStringByRawKey` симметрично существующим Long/Boolean. `PrefKey` enum не трогали.

2. **Composite prefix-encoded key `quiz_picker_dict_<dictionaryId>`, не Map/JSON.** Single value per ключ — DataStore Flow подписывается на конкретного словаря, нет «read-modify-write» цикла, нет concurrent-write проблем. Helper `quizPickerPrefKey(dictionaryId)` в `:modules:datasource:prefs` как single source of truth.

3. **`dictionaryId` резолвится в `DatasourceEffectHandler`, не в Msg/Effect payload.** Match pattern `QuizGameImpl.fetchData:177`. Reducer без IO; Msg/Effect surface минимальный; null dict → `Msg.Empty`.

4. **Tier 1/2/3 widget convention.** Tier 1 primitives — `core/ui/<subdir>/` с `Lexeme*` prefix (atomic). Tier 2 — `modules/widget/<name>/` (межфичевые с domain naming). Tier 3 — feature-local wrappers. Compose может использовать Tier 1 напрямую (не через Tier 2).

5. **Encoding `ComponentTypeRef` как `builtin:<key>` / `user:<name>` через `substringAfter(':')`, не JSON.** `kotlinx.serialization` в проекте отсутствует — не оправдано ради одного поля. Decoder корректен для names с `:`, unicode, пустых строк.

### Backlog долги, добавленные этой фичей (тег IS481)

- **F1.** `widget/iconDropDowned/` → `core/ui/dropdown/` с `Lexeme*` prefix. Триггер: следующий заход в `iconDropDowned/` либо плановая уборка.
- **F2. Encoding consolidation `ComponentTypeRef`.** Сейчас два кодека (JSON в `ComponentTypeRefJson.kt` для DB + `builtin:`/`user:` в `QuizChatUseCaseImpl` для prefs). Триггер: новый variant либо payload.
- **F3. `BuiltInComponent.titleResId` / `quizHeaderResId` на enum-entries.** UI loud-fail (exhaustive `when`), quiz silent-fail (`when else`). Триггер: добавление второго built-in.
- **F4. `LoadQuizComponentTypes` effect redundancy.** Дублирует initial-emit `QuizPickerFlowHandler`; 2× I/O на cold start (idempotent через `resolveSelection`). Триггер: следующий заход в quiz/chat.
- **F5. `State.disableUserInput()` инвертирован** (pre-existing, surfaced при touch). `isUserInputEnable = true` вместо `false`. Триггер: следующий заход в state-логику.
- **Atomicity rollback androidTest для F013/F015.** Сейчас только mockk unit на exception; реальный rollback SQLite-транзакции не проверен. Триггер: следующий заход в `core-db-impl` либо регрессия в проде.
- **`updateComponentValue` / `deleteComponentValue` honest return.** Сейчас возвращают `null` на success — нет DAO метода `getLexemeIdByComponentValueId`. Триггер: UI configurator для component values.
- **`LexemeSubmenuMenuItem` — cascaded popup вместо accordion.** (Новый из session smoke.) Триггер: второй submenu в `core/ui/dropdown/` либо UX-полировка перед публикацией.

### Скрытые риски (не в backlog)

- **Composite key привязан к `DictionaryId = Long`.** Если кто-то изменит `Long → Int` либо обернёт в value class — `"quiz_picker_dict_$dictionaryId"` сохранит другой `toString()`. Существующие записи станут orphan. Migration не предусмотрена.
- **Encode/decode без version field.** Если формат сменится (F2 consolidation на JSON), legacy записи `builtin:translation` / `user:Definition` нужно либо распознавать в новом кодеке, либо мигрировать. Single-field format не оставляет места для version discriminator.
- **Удаление словаря не очищает prefs-запись.** `quiz_picker_dict_<id>` остаётся в DataStore после `DELETE FROM dictionaries`. При reuse id (теоретически возможен после truncate/restore) — restored selectedRef от чужого словаря. Cleanup не реализован.
- **Mid-session change selectedRef не применяется per-question** — зафиксировано в scope §4, но не в backlog. UX expectation mismatch не покрыт manual smoke явно.

---

## 4. Action items — что взять в следующие фичи

### Для FF base / overlay

1. **`business_design_tree` ревью должно валидировать layer boundary** — если узел имеет UI-extension (`*.kt` в `core/ui/`, `widget/`, `screen/*/widget/`), кластер обязан попасть в UI sub-flow, не business. Прямой следствие IS481-F16. Иначе ui_* шаги превращаются в null-op.
2. **F11 — Read real source library API** до того как писать procedure. 2 повтор вопроса пользователя = немедленный Read.
3. **F12 — BOOTSTRAP read-trail** в conversation: `READ: <path>` printout per шаг.

### Для следующих фич с picker / preference

1. Если value-per-key и реактивная подписка нужны — prefer composite key `<feature>_<entity>_<id>` над JSON-blob/Map. Простая Flow-подписка, нет concurrent-write race.
2. Encoding без version field — кандидат на rework заранее, если планируется evolve формата. Иначе planning hatch: «legacy parse → fallback» в decoder.
3. Удаление родительской сущности (словаря) — cleanup связанных prefs-записей в одной транзакции с DELETE. Иначе orphan на годы.

### Для session execution

1. **Build script `scripts/cc-build.sh` обязателен** для gradle через Bash tool. Прямой `./gradlew` теряет output на UP-TO-DATE задачах.
2. **Cold start logcat verification** — force-stop + relaunch + PID change check ОБЯЗАТЕЛЬНЫ для подтверждения restore from disk; нельзя интерпретировать «увидел state в логах» без верификации PID.
3. **Sub-agent path validation** — после `business_design_tree` / `business_implement` оркестратор должен сделать quick `Glob` по new файлам vs convention paths. Path deviation ловится за 1 шаг.

_model: claude-opus-4-7[1m]_
