---
status: done
---

# Summary — business

## Что сделано

Реализованы все 17 узлов из `business_design_tree.md` (8 [+] / 9 [~]) в 5 параллельных кластерах. Business слой — picker компонента квиза в chat-меню — закрыт полностью; контракт `business_contract_spec.md` выполнен без изменений сигнатур (Msg / Effect / State surface).

**Mate слой (`modules/screen/quiz/chat/.../logic`):**
- `State.kt` [~] — nested `ItemsState.QuizComponent(availableTypes: List<ComponentType>, selectedRef: ComponentTypeRef?)` + computed `isPickerEnabled = availableTypes.size > 1` + extension `updateQuizComponent(types, selectedRef)`. Дефолт через no-arg конструктор по pattern existing `Earliest`/`FrequentMistakes`/`Debug`. Инварианты: `availableTypes` загружается one-shot на entry; `selectedRef = null` валиден только в transient-окне до первого `QuizComponentTypesLoaded`; после load — всегда `selectedRef ∈ availableTypes.map { it.toRef() }` либо `availableTypes.isEmpty()`.
- `Message.kt` [~] — `Msg.SelectQuizComponent(ref: ComponentTypeRef)` (click; state не меняется напрямую — pattern `Msg.EarliestOn`) + `Msg.QuizComponentTypesLoaded(types, restoredSelectedRef)` (bulk-load, single update path и для initial load, и для re-emit после write).
- `ChatReducer.kt` [~] — `Msg.PrepareToStart` расширен emit'ом `LoadQuizComponentTypes` (existing welcomeMessage + stopLoading сохранены); `is Msg.SelectQuizComponent → state to setOf(SaveQuizPickerSelection(ref))`; `is Msg.QuizComponentTypesLoaded → state.updateQuizComponent(...) to emptySet()` через private `resolveSelection(types, restored)` (membership через `toRef()`, fallback на `first()` после `isEmpty` guard — F1 senior fix).
- `DatasourceEffectHandler.kt` [~] — `DatasourceEffect.LoadQuizComponentTypes` (data object) + `SaveQuizPickerSelection(ref: ComponentTypeRef)` (data class); ctor расширен `useCase: QuizChatUseCase`; в обеих ветках `dictId` резолвится через `useCase.getCurrentDictionaryId()` (null → `Msg.Empty`, без throw — соответствует existing pattern `QuizGameImpl.fetchData:177`).
- `QuizPickerFlowHandler.kt` [+] — `MateFlowHandler<Msg, Effect>` subscribe на `getStringFlowByRawKey(quizPickerPrefKey(dictId))`; на каждый emit (initial DataStore value + последующие writes) re-emit `QuizComponentTypesLoaded` с re-fetched `types` + decoded `restoredSelectedRef`. При null dict на момент `subscribe` — handler dead до re-init ViewModel'а; повторные pref-writes не доходят (F1 test).
- `ChatViewModel.kt` [~] — `quizPickerFlowHandler: QuizPickerFlowHandler` в `@AssistedInject` ctor + `effectHandlerSet`. Без `quizPickerFlowHandler` в Set — subscribe не вызовется, picker не загрузится.

**Domain (`modules/domain/lexeme`):** [~] `fun ComponentType.toRef(): ComponentTypeRef` — pure-JVM маппинг (`systemKey != null` → `BuiltIn(systemKey)`, иначе `UserDefined(name!!)`). `IllegalStateException` при `systemKey=null && name=null` (invariant violation на DB-схеме). Stable identity без `id` — используется и в reducer `resolveSelection` (membership check), и в UI (`type.toRef() == selectedRef`).

**Datasource (`modules/datasource/prefs`):** [~] `getStringByRawKey(key)` / `getStringFlowByRawKey(key)` / `setStringByRawKey(key, value?)` — под капотом `stringPreferencesKey` + `dataStore.edit`/`.data.map`, симметрично существующим `Long/Boolean` методам. `value=null` → `remove(key)`. `PrefKey` enum НЕ трогается (dynamic per-dictionary keys живут вне enum). `QuizPickerPrefKey.kt` [+] — top-level `fun quizPickerPrefKey(dictionaryId: Long): String = "quiz_picker_dict_$id"`, single source of truth для UseCaseImpl + FlowHandler.

**UseCase (`QuizChatUseCase` iface + `QuizChatUseCaseImpl` в `app`):** [~] +3 метода:
- `getAvailableTypes(dictId)` — proxy `lexemeApi.getComponentTypes(dictId).map { it.toDomain() }` (existing маппер из WordCardUseCaseImpl).
- `getQuizPickerSelection(dictId)` — read raw pref + decode; null при любом невалидном формате.
- `setQuizPickerSelection(dictId, ref)` — encode + write через `setStringByRawKey`.

Encoding/parsing — internal, без `kotlinx.serialization` (новой dep не вводилось). `ComponentTypeRef` в domain остаётся pure.

**Quiz session (`QuizGameImpl.fetchData`):** [~] перед `mapNotNull { it.toQuizItem(...) }` — `selectedRef = useCase.getQuizPickerSelection(dictId)`; `effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs`; передаётся в `toQuizItem(componentRefs = effectiveRefs, ...)`. Null → fallback на `quizConfig.componentRefs` (preserves семантику до первого выбора). Mid-session change не применяется per-question — selection вступает в силу на следующем `loadData()` (per scope §4).

**UI (Tier 1 + Tier 3):**
- `core/ui/dropdown/LexemeSubmenuMenuItem.kt` [+] — generic submenu wrapper над `DropdownMenuItem`: header + chevron + раскрывающаяся inline `Column` content; remembered `isExpanded`. `enabled=false` → header показан но клики игнорируются.
- `core/ui/dropdown/LexemeRadioMenuItem.kt` [+] — generic radio variant `DropdownMenuItem` + `RadioButton` leading icon. По аналогии с `MenuItemWithCheckbox` (не модифицируем `MenuItem.kt`).
- `widget/appbar/menu/ComponentChoiceItem.kt` [+] — per-type wrapper над `LexemeRadioMenuItem`. Title resolution: built-in TRANSLATION → `R.string.chat_menu_item_component_translation`; UserDefined → `name` raw.
- `widget/appbar/menu/QuizComponentMenuItem.kt` [+] — top-level picker оборачивает `LexemeSubmenuMenuItem` + список `ComponentChoiceItem` per `availableTypes`. Скрывает себя при `availableTypes.isEmpty()` (invariant 3). `enabled = isPickerEnabled`.
- `widget/appbar/ActionsWidget.kt` [~] — встроен `QuizComponentMenuItem(state.quizComponent, onSelect = { sendMessage(Msg.SelectQuizComponent(it)) })` между `MistakesMenuItem` и debug-блоком.

**Strings (`core/core-resources`):** [~] `chat_menu_item_quiz_component` ("Quiz component" / "Компонент квиза") + `chat_menu_item_component_translation` ("Translation" / "Перевод") в `values/` и `values-ru-rRU/`. UserDefined пункт title не использует strings (raw `name`).

**Тесты (категории 1, 2, 4, 5, 6, 7 из `business_test.md`; 3 опущена per plan, 8 — manual smoke):**
- `:modules:domain:lexeme:test` — `ComponentTypeRefExtTest` 5 cases (built-in / name override / user-defined / unicode-colon name / invariant violation throws).
- `:app:testDebugUnitTest` → `QuizChatUseCaseImplTest` — 23 cases (было 6, +17): decode/encode happy paths + F2 edge cases (empty key / case-sensitivity / no-colon / empty UserDefined round-trip) + F9 overwrite на тот же dict key + per-dict isolation (`dict_7` vs `dict_42`).
- `:modules:screen:quiz:chat:testDebugUnitTest` — `ChatReducerTest` 10 (Select без guard F8 / Loaded variants / F3 single-type fallback / F7 double-emit idempotence / PrepareToStart emits Load); `DatasourceEffectHandlerTest` 4 (Load/Save × happy/null-dict; FakeUseCase workaround — mockk fails с `@JvmInline value class` auto-hint matcher); `QuizPickerFlowHandlerTest` 4 (initial / re-emit / no-op без dict / re-fetch на каждый emit); `QuizGameImplFetchDataTest` 4 (selectedRef override / null fallback / F4 quizConfig ignored override semantics / graceful skip); existing 11 (+1 stub `getQuizPickerSelection any returns null` в `QuizGameImplEmptyListTest`).

**Encoding contract:** `builtin:<key>` → `BuiltInComponent.fromKey(key)?.let(::BuiltIn)` (unknown built-in → null, future-proof); `user:<name>` → `UserDefined(raw.substringAfter(':'))` (корректно для `:` внутри name, unicode, любого String, пустой строки); любой другой формат → `null` (caller fallback на default через `resolveSelection`).

**Лог-точки и tag:** все логи picker'а через `LexemeLogger` с tag `###QuizPicker###` (per `checklist_init`). Срабатывают в `DatasourceEffectHandler` (load/save outcomes), `QuizPickerFlowHandler` (subscribe/emit), `QuizChatUseCaseImpl` (decode failure → null).

**Lifecycle (data flow закрыт):**
- **Initial load на entry:** `Msg.PrepareToStart` → `LoadQuizComponentTypes` effect → handler резолвит `dictId` → `useCase.getAvailableTypes(dictId)` + `useCase.getQuizPickerSelection(dictId)` → `Msg.QuizComponentTypesLoaded(types, restored)` → reducer `resolveSelection` → `state.quizComponent` обновлён.
- **Reactive восстановление:** параллельно `QuizPickerFlowHandler.subscribe` подписывается на `getStringFlowByRawKey(quizPickerPrefKey(dictId))` — DataStore выдаёт current value → re-emit `QuizComponentTypesLoaded` (idempotent с initial load — F7 test).
- **Persist через click:** `Msg.SelectQuizComponent(ref)` → reducer не меняет state → `SaveQuizPickerSelection(ref)` effect → handler резолвит `dictId` → `useCase.setQuizPickerSelection(dictId, ref)` → `PrefsProvider.setStringByRawKey(key, encoded)` → FlowHandler подхватывает write → `Msg.QuizComponentTypesLoaded(types, restored=новый ref)` → state.
- **Apply в quiz session:** на каждый `loadData()` (start/continue) `QuizGameImpl.fetchData` вызывает `useCase.getQuizPickerSelection(dictId)` → `effectiveRefs = listOf(selectedRef)` либо fallback на `quizConfig.componentRefs` → `toQuizItem(componentRefs = effectiveRefs)` фильтрует список квизов до выбранного компонента.

UseCase — single source. Persist и restore идут через UseCase в двух точках (FlowHandler для UI / QuizGameImpl для quiz logic). PrefsProvider — implementation detail; raw-string API инкапсулирован в UseCaseImpl через encode/decode.

## Ключевые решения

- **Persistent per-dictionary через PrefsProvider raw-string + composite key** (`quiz_picker_dict_<id>`). Альтернативы (JSON-blob под одним ключом + kotlinx.serialization; запись в `quiz_configs.component_refs` через БД) отброшены: первая — лишняя dep ради одной фичи; вторая — нарушает scope «не трогаем quiz_configs». `PrefKey` enum не расширяется — dynamic keys живут вне enum (см. `business_contract.md` §PrefsProvider).
- **`ComponentTypeRef` encoding** `builtin:<key>` / `user:<name>` через `raw.substringAfter(':')` — выбран вместо JSON-сериализации (новой dep нет в проекте, см. `business_walkthrough.md` §PrefsProvider). Корректен для names с `:`, unicode, пустых строк. Encoder/decoder internal к `QuizChatUseCaseImpl`, domain `ComponentTypeRef` остаётся pure без serialization-аннотаций (см. `business_contract_spec.md` §ComponentTypeRef encoding).
- **Tier 1 primitives `Lexeme*MenuItem` в `modules/core/ui/dropdown/`** per зафиксированной convention (`core/ui` + `core/theme` с `Lexeme*` prefix). `widget/iconDropDowned/` (без Lexeme prefix) не трогаем — его migration → backlog. Primitives используют `String` вместо `StringSource` (core/ui не depends на widget/iconDropDowned) — caller вызывает `stringResource()` перед передачей; в духе Tier 1 (минимум абстракций). Iter 1 design_tree положил primitives в widget/iconDropDowned вопреки convention — iter 2 восстановил пути.
- **`dictionaryId` резолвится в `DatasourceEffectHandler` через `useCase.getCurrentDictionaryId()`**, не в `Msg`/`Effect` payload. Match pattern `QuizGameImpl.fetchData:177` (current dict резолвится in-place). Reducer без IO; Msg / Effect surface минимальный.
- **Mid-session change применяется на next `loadData()`** — `QuizGameImpl.fetchData` пересобирает фильтр на каждый `LoadQuiz` (start/continue), не per-question. Per-question re-fetch не вводится (out of scope per `02_scope.md` §4 — закрыто на scope-этапе F005 hedge).
- **DataStore Flow + `LoadQuizComponentTypes` effect — дублирующий path, idempotent.** `QuizPickerFlowHandler.subscribe` выдаёт initial DataStore value, `Msg.PrepareToStart` параллельно emit'ит `LoadQuizComponentTypes`. Оба apply через тот же `resolveSelection` — результат стабилен (F7 double-emit idempotence test). Effect оставлен как explicit signal на entry (читаемость); consolidation в single-path через FlowHandler — senior F4, ушёл в backlog.
- **`quizPickerPrefKey` помещён в `:modules:datasource:prefs`** (не в UseCaseImpl) — single source of truth для двух consumer'ов (`QuizChatUseCaseImpl` + `QuizPickerFlowHandler`); оба и так depend на `:modules:datasource:prefs`, циклов нет. Implementation-level корректировка vs spec (контракт не затронут — см. `business_publish_spec.md` §Корректировки 1).
- **`SaveQuizPickerSelection` при null dict → `Msg.Empty`** (без throw, set не вызывается) — соответствует existing pattern `LoadQuizComponentTypes` ветки. Поведение Effect (no-op при null dict) подразумевалось спекой, формализовано без новых Msg / Effect (см. `business_publish_spec.md` §Корректировки 3).
- **`selectedRef: ComponentTypeRef?`, не `Map<Ref, Boolean>`** — radio выражает invariant «ровно один» через тип. Multi-select → рефактор `selectedRef → Map` с reducer-инвариантом, backlog (когда придёт фича).
- **`resolveSelection` fallback на `first()` после `isEmpty` guard** (не `firstOrNull()`) — F1 senior fix iter 2: после `if (types.isEmpty()) return null` guard `available.first()` безопасен и yields stronger type (non-null). Был `firstOrNull()` — null-path unreachable.
- **mockk + `@JvmInline value class ComponentTypeRef`** — fails при auto-hinting matchers (`any()`, `coVerify` с value-class аргументом). `DatasourceEffectHandlerTest` использует `FakeUseCase` (ручной stub) вместо mockk. В `QuizChatUseCaseImplTest` проблемы нет — там coVerify на `setStringByRawKey(string, string)` (примитивные типы). См. `business_implement.md` §Проблемы.
- **Бэклог follow-ups** (зафиксировано в `log.md:00:19:25`, 0 critical, не блокируют IS481): F2 encoding consolidation (вынести encode/decode в `:modules:datasource:prefs` либо domain); F3 `BuiltInComponent.displayResId` (избавиться от hardcoded title-resolution в `ComponentChoiceItem`); F4 `LoadQuizComponentTypes` effect redundancy (consolidation в FlowHandler-only path); F5 `disableUserInput` pre-existing bug в `QuizGameImpl` (race condition, не связан с IS481). Плюс: migration `widget/iconDropDowned/` → `core/ui/dropdown/` с Lexeme prefix.

## Артефакты

Sub-flow артефакты:
- [`business_walkthrough.md`](./business_walkthrough.md) — discovery в реальном коде + аналоги pattern'ов + 6 gaps + 5 decision points.
- [`business_contract.md`](./business_contract.md) — State / Msg / Effect/IO / UseCase contract (iter 1 → 2 после review; payload mismatch fix).
- [`business_contract_review.md`](./business_contract_review.md) — verdict approved (iter 2).
- [`business_contract_spec.md`](./business_contract_spec.md) — canonical local spec (iter 2 после architect critical: добавлен `ComponentType.toRef()` в UseCase раздел).
- [`business_design_tree.md`](./business_design_tree.md) — 17 узлов + 5 параллельных кластеров + checklist_items (iter 2 после Tier 1 path correction Lexeme*MenuItem → core/ui/dropdown/).
- [`business_test.md`](./business_test.md) — 7 категорий + manual smoke + checklist_items (iter 2 после 9 QA findings F1-F9).
- [`business_implement.md`](./business_implement.md) — implementation report (17 узлов реализованы, тесты PASS, 4 senior follow-ups → backlog).
- [`business_publish_spec.md`](./business_publish_spec.md) — публикация в `docs/features-spec/` пропущена (`spec_filename: null`, фича без записи в каталоге спек).

Код (новые / изменённые файлы):
- Domain: `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTypeRef.kt` [~]; `modules/domain/lexeme/src/test/java/me/apomazkin/lexeme/ComponentTypeRefExtTest.kt` [+].
- Prefs: `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt` [~]; `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/QuizPickerPrefKey.kt` [+].
- Resources: `core/core-resources/src/main/res/values/strings.xml` [~]; `core/core-resources/src/main/res/values-ru-rRU/strings.xml` [~].
- UseCase: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt` [~]; `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt` [~]; `app/src/test/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImplTest.kt` [~].
- Mate (`modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/`): `State.kt` [~], `Message.kt` [~], `DatasourceEffectHandler.kt` [~], `ChatReducer.kt` [~], `QuizPickerFlowHandler.kt` [+]; ViewModel: `ChatViewModel.kt` [~].
- Тесты Mate (`modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/logic/`): `ChatReducerTest.kt` [+], `DatasourceEffectHandlerTest.kt` [+], `QuizPickerFlowHandlerTest.kt` [+].
- Quiz session: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt` [~]; `modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/quiz/QuizGameImplFetchDataTest.kt` [+]; `QuizGameImplEmptyListTest.kt` [~] (+1 stub).
- UI Tier 1: `modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeSubmenuMenuItem.kt` [+]; `LexemeRadioMenuItem.kt` [+].
- UI Tier 3: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/widget/appbar/menu/QuizComponentMenuItem.kt` [+]; `ComponentChoiceItem.kt` [+]; `widget/appbar/ActionsWidget.kt` [~].

_model: claude-opus-4-7[1m]_

## log_messages

- business sub-flow закрыт: 17 узлов реализованы, тесты 6 категорий PASS (категория 3 опущена per plan, категория 8 — manual smoke), контракт `business_contract_spec.md` выполнен без изменений сигнатур.
- Persistent per-dictionary через PrefsProvider raw-string + composite key `quiz_picker_dict_<id>`; encoding `builtin:<key>` / `user:<name>` через `substringAfter(':')`, без JSON/kotlinx.serialization.
- Tier 1 primitives в `core/ui/dropdown/` с `Lexeme*` prefix; iconDropDowned migration + 4 senior follow-ups → Backlog.
