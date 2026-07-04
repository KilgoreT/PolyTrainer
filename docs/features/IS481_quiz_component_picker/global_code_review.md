# Global Code Review — IS481 Quiz Component Picker

_Reviewer model: claude-opus-4-7[1m]. Дата: 2026-06-11. Branch: `IS481_lexeme_component_constructor`._

## Verdict

**APPROVED WITH FOLLOWUPS** — фича функционально полная, контракт scope.md выполнен, тесты PASS,
lint clean, build OK. Архитектурные решения соблюдены (Dependency Rule не нарушен,
storage = prefs raw-string, encoding `builtin:` / `user:`, Tier 1 в `core/ui/dropdown`,
domain extension в `lexeme`, dictionaryId резолвится in-handler).

Блокирующих дефектов нет. Зафиксирован один **scope-drift**, не относящийся к picker'у
(см. Major #1 — debug menu обёрнут в `BuildConfig.DEBUG`); рекомендую либо вынести в
отдельный коммит/задачу, либо удалить из IS481.

## Findings

### Critical

— нет.

### Major

**M1. Scope drift: debug menu hidden on release builds (`ActionsWidget.kt`).**
В `02_scope.md` интеграция нового picker'а описана как «integration в `ActionsWidget.kt`
между `MistakesMenuItem` и debug-блоком». Однако в diff'е debug-блок дополнительно
обёрнут в `if (BuildConfig.DEBUG) { ... }` (требует включить `buildFeatures.buildConfig = true`
в `modules/screen/quiz/chat/build.gradle.kts`). Это самостоятельное поведенческое
изменение (release-сборка теряет debug меню), которое не описано в business_summary /
implement / check артефактах. Само по себе изменение разумное, но оно:
- расширяет surface PR за пределы IS481 picker;
- не покрыто тестами (debug-видимость в release);
- меняет состав release-фичи без записи в backlog / changelog.

**Что сделать:** либо вынести в отдельный коммит с явным заголовком («hide debug menu
in release builds»), либо откатить и завести как отдельную задачу. Если оставлять —
зафиксировать в `business_implement.md` / `check.md` как корректировку vs spec.
Файлы: `modules/screen/quiz/chat/build.gradle.kts:21-24`,
`modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/widget/appbar/ActionsWidget.kt:6,43-52`.

### Minor

**m1. `disableUserInput()` pre-existing bug — IS481 коснулся файла, оставил TODO.**
`modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/State.kt:238-242` —
функция `disableUserInput()` устанавливает `isUserInputEnable = true` с пометкой `//TODO`.
Это pre-existing (не введён IS481), однако файл `State.kt` касался picker'ом —
формально есть «touch». Уже зафиксировано в backlog'е (F5 senior follow-up). Не блокирует
merge; pre-existing → backlog приемлем.

**m2. `ComponentChoiceItem` `when (ref.key)` — exhaustive over single enum entry.**
`modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/widget/appbar/menu/ComponentChoiceItem.kt:30-35`
— `when (ref.key) { TRANSLATION -> stringResource(...) }`. Exhaustive над `BuiltInComponent`
с одним entry. При добавлении нового entry (PRONUNCIATION / TRANSCRIPTION) — compile error,
loud fail. Это сознательное решение (см. backlog F3 — «UI loud-fail, quiz silent-fail»).
Корректность сохранена, симметрия с `QuizGameImpl.toQuizItem` (silent fallback на
definition header) уже зафиксирована в backlog. Минор остаётся как напоминание.

**m3. `LexemeSubmenuMenuItem.isExpanded` — состояние теряется при `enabled=false` mid-flight.**
`modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeSubmenuMenuItem.kt:35,48` —
`remember { mutableStateOf(false) }`, click игнорируется при `enabled=false`. Edge case:
если submenu раскрыт (`isExpanded=true`) и затем `availableTypes` уменьшилось до 1
(`enabled=false`), submenu останется раскрытым — внутри будет один disabled radio item,
визуально странновато. Acceptable trade-off для current scope (single-dictionary, динамика
`availableTypes` mid-session не происходит). Без действий.

**m4. `LexemeSubmenuMenuItem` placed в `core/ui/dropdown/` — package per Lexeme convention.**
Primitives названы `Lexeme*MenuItem` per convention, но это **первые** файлы в
`me.apomazkin.ui.dropdown` package — других потребителей пока нет. Не дефект, fix-forward
при добавлении новых dropdown primitives. Acceptable.

**m5. `QuizPickerFlowHandler` — dict resolution только один раз на subscribe.**
`modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/QuizPickerFlowHandler.kt:38` —
`val dictId = useCase.getCurrentDictionaryId() ?: return@launch`. Если на момент subscribe
`dictId == null` — handler в terminal state. Если пользователь позже создал словарь
без re-init ViewModel — picker не загрузится до следующего входа в chat screen.
В рамках IS476 (gracefully handle no dict) и текущей навигации chat screen всегда
recreate'ит ViewModel при возврате — фактически проблема не материализуется. Зафиксировать
в спеке (`F1 test` в business_summary это уже описывает). Без действий.

### Followups (зафиксированы в `docs/Backlog.md`)

F1. **Migration `widget/iconDropDowned/` → `core/ui/dropdown/`** (Lexeme prefix migration).
F2. **Encoding consolidation** — единый кодек `ComponentTypeRef` вместо JSON (DB) + string (prefs).
F3. **`BuiltInComponent.titleResId` / `quizHeaderResId`** — display resources на enum entries
(симметрия UI loud-fail vs quiz silent-fail).
F4. **`LoadQuizComponentTypes` effect** — убрать дубль с FlowHandler initial emission.
F5. **`State.disableUserInput()`** — инвертированный флаг (pre-existing bug).

Все 5 — out-of-scope IS481, корректно ушли в backlog.

## Coverage analysis

**Unit tests:**
- `:modules:domain:lexeme:test` → `ComponentTypeRefExtTest` 5/5 — built-in, name override
игнорируется, user-defined, unicode/colon name, invariant violation (`systemKey=null
&& name=null` → `IllegalStateException`).
- `:app:testDebugUnitTest` → `QuizChatUseCaseImplTest` 23/23 — encode/decode happy paths
+ 6 edge cases (empty key, case-sensitivity, no-colon, malformed, empty UserDefined
round-trip, builtin: с пустым ключом) + per-dict isolation + overwrite на тот же ключ.
**Покрытие encoding полное** — все edge cases из требований review.
- `:modules:screen:quiz:chat:testDebugUnitTest`:
  - `ChatReducerTest` 8 cases (PrepareToStart emit, Select без guard, Loaded variants
с restored valid / invalid / null / empty / single-type fallback / double-emit
idempotent).
  - `DatasourceEffectHandlerTest` 4 (Load × happy/null-dict, Save × happy/null-dict)
с workaround `FakeUseCase` из-за mockk + `@JvmInline value class` несовместимости.
  - `QuizPickerFlowHandlerTest` 4 (initial / re-emit / no-op без dict / re-fetch on
each emit).
  - `QuizGameImplFetchDataTest` 4 (selectedRef override, null fallback, F4 override
semantics — quizConfig ignored, graceful skip).
  - Existing `QuizGameImplTest` / `QuizGameImplEmptyListTest` не сломаны (+1 stub
`getQuizPickerSelection any returns null`).

**Critical paths покрыты:**
- Encoding edge cases — ✅ (12+ кейсов в `QuizChatUseCaseImplTest`).
- Reducer fallback при invalid restored ref — ✅ (`ChatReducerTest::restored invalid - fallback to first`).
- FlowHandler null dict at subscribe — ✅ (`QuizPickerFlowHandlerTest::no-op when no current dict`).
- QuizGameImpl null selectedRef fallback на quizConfig — ✅ (`QuizGameImplFetchDataTest::null - fallback`).
- ComponentType.toRef() null safety — ✅ (`ComponentTypeRefExtTest::invariant violation throws`).

**Не покрыто (приемлемо):**
- Integration test ViewModel-уровня (subscribe + emit + reduce + persist цикл) —
оставлено для manual smoke checklist; для unit-уровня sufficient coverage компонентов.
- PrefsProvider raw-string API integration test — категория 3 опущена per test plan
(DataStore library contract).
- UI compose tests — категория 8 — manual smoke.

**Bug / safety verification:**

1. **Encoding edge cases.** `decodeRef` использует `raw.substringAfter(':')` —
для `user:My:Type` корректно даёт `UserDefined("My:Type")` (хранит первое
`substringAfter`), encode обратно даст `user:My:Type` → roundtrip stable. Для
`builtin:my:fake` — `fromKey("my:fake")` вернёт null (unknown built-in). Для
пустой строки после префикса — `builtin:` → null, `user:` → `UserDefined("")`. Для
неизвестного префикса (`USER:`, `garbage`) → null. Edge cases test'ы покрывают.
**Корректно.**

2. **Reducer fallback при invalid restored ref.** `resolveSelection` сначала `isEmpty`
guard (returns null), затем membership check, затем `available.first()`. После guard
`first()` безопасен (non-null). Edge case: пустой список → null (тест `empty types
sets empty state`). **Корректно.**

3. **FlowHandler null dict at subscribe.** `dictId = useCase.getCurrentDictionaryId()
?: return@launch` — handler не запускает collect, terminal state. Если dict
появляется позже — handler dead до re-init ViewModel'а. См. Minor m5. **Acceptable**
(в рамках IS476 chat screen всегда требует non-null dict).

4. **QuizGameImpl fallback на quizConfig.componentRefs при null selectedRef.**
`val effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs`
— null → preserves существующую семантику (до первого выбора). Тест `null - fallback`
verifies. **Корректно.**

5. **ComponentType.toRef() null safety.** `systemKey != null → BuiltIn(sk)`;
`systemKey == null → UserDefined(name ?: error(...))`. Invariant violation — `error()`
throws IllegalStateException. Тест `invariant violation throws` покрывает. На DB-уровне
CHECK constraint гарантирует non-null name при systemKey null (см. business_summary).
**Корректно.**

## Architecture compliance

- **Dependency Rule.** ✅ Соблюдён.
  - `modules/domain/lexeme` (вершина) — `ComponentType`, `ComponentTypeRef`, `BuiltInComponent`,
extension `toRef()`. Pure-JVM, без зависимостей.
  - `modules/datasource/prefs` — depends на Android (DataStore), `quizPickerPrefKey` helper.
  - `modules/screen/quiz/chat` — depends на `domain/lexeme` + `datasource/prefs` + `core/ui`.
Импорты в порядке.
  - `app` (impl) — depends на all above.
  - `core/ui/dropdown` Tier 1 primitives — без зависимости на `widget/iconDropDowned`,
используют `String` вместо `StringSource` (acceptable per Tier 1).

- **Layer attribution.** Mate (Msg/State/Reducer/EffectHandler/FlowHandler) живёт в
`logic/`. UI composables в `widget/appbar/menu/` + `core/ui/dropdown/`. Domain extension
в `domain/lexeme`. Use case interface в `deps/`, impl в `app/`. Корректное распределение.

- **Persistent per-dictionary через prefs.** Решение из обсуждения (`PREFS-not-quiz_configs`)
применено. Ключ `quiz_picker_dict_<id>` через `quizPickerPrefKey(dictionaryId)` —
single source of truth в `:modules:datasource:prefs` (используется UseCaseImpl + FlowHandler).
Корректировка vs spec (изначально планировалось в UseCaseImpl) зафиксирована в
business_implement.md. ✅

- **Encoding `builtin:` / `user:`.** Применено. Encode/decode internal к
`QuizChatUseCaseImpl` (не в domain). Domain `ComponentTypeRef` остаётся pure без
serialization annotations. ✅

- **Tier 1 в `core/ui/dropdown/`.** Применено — `LexemeSubmenuMenuItem` + `LexemeRadioMenuItem`.
Iter 1 ошибочно положил в widget/iconDropDowned, iter 2 восстановил per convention. ✅

- **Domain extension в `lexeme`.** `ComponentType.toRef()` живёт в `modules/domain/lexeme`,
используется в reducer (membership) + UI (selected match). ✅

- **`dictionaryId` в handler (не в Msg/Effect).** `LoadQuizComponentTypes` / `SaveQuizPickerSelection`
без `dictionaryId` payload; handler резолвит через `useCase.getCurrentDictionaryId()`.
Match с existing pattern `QuizGameImpl.fetchData:177`. ✅

- **Existing IS481 main работа не сломана.** AGG-1/AGG-4/AGG-5/AGG-10/AGG-12 интегрированы.
`QuizConfig` / `ComponentTypeRef` / `ComponentType` используются как есть, новых
domain типов не введено. Existing migrations / tests (`Migration_011_to_012`,
`BundledSqliteFeatureTest`) не затронуты. ✅

- **Dead code / stubs / FIXME.**
  - `disableUserInput() //TODO` — pre-existing (m1).
  - `// IS481 (AGG-12)` / `// IS476` маркеры — корректные провенанс-комментарии.
  - Нет orphan кода. Нет закомментированных блоков.

## Pre-merge gates

| Gate | Status | Note |
|---|---|---|
| Lint (0 errors) | ✅ | 3 pre-existing warnings (vector icons / monochrome) |
| Unit tests | ✅ | Все категории PASS, exit 0 |
| Build assembleDebug | ✅ | APK собран |
| Spec compliance | ✅ | Контракт scope.md выполнен, decisions применены |
| Dependency Rule | ✅ | Без нарушений |
| Backlog для out-of-scope | ✅ | 5 followups зафиксированы |
| Scope drift документация | ⚠️ | M1 — debug-menu wrap не описан |
| Manual UI smoke | ⏳ | Пользовательский шаг по checklist.md |

## Рекомендации перед merge

1. **(Обязательно)** Решить судьбу `BuildConfig.DEBUG` wrap (Major M1) — либо вынести в
отдельный коммит/задачу, либо добавить в `business_implement.md §Корректировки vs spec`
строку: «debug menu обёрнут в `BuildConfig.DEBUG` — скрывает item на release builds.
Out-of-scope IS481 picker, but co-shipped».
2. **(Желательно)** Manual smoke по checklist.md перед merge (4 root scenarios + edge cases).
3. **(Опционально)** F4 (LoadQuizComponentTypes redundancy) можно сделать сразу при
ребейзе — net deletion ~30 строк, упрощает code path. Если этот PR уходит as-is —
backlog корректно отражает.

_model: claude-opus-4-7[1m]_

## log_messages

- Verdict: **APPROVED WITH FOLLOWUPS**. Блокирующих дефектов нет; scope.md выполнен; тесты / lint / build PASS; 5 senior followups корректно в backlog'е.
- **M1 scope drift**: `ActionsWidget.kt` дополнительно обёрнул debug-блок в `if (BuildConfig.DEBUG)` + включил `buildFeatures.buildConfig = true` — это **отдельное** behaviour-изменение вне scope IS481 picker; не задокументировано в business_implement / check.
- Encoding `builtin:` / `user:` через `substringAfter(':')` корректно покрывает edge cases (unicode, `:` в name, empty name, unknown built-in, case-sensitivity, no-colon) — 12+ тестов в `QuizChatUseCaseImplTest`.
