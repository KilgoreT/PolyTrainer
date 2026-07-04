# Business test plan: IS481 Quiz Component Picker

Test plan по step-spec `docs/forgeflow/steps/test.md`. Verified через `Read` real source: `ChatReducer.kt`, `DatasourceEffectHandler.kt`, `QuizGameImpl.kt`, `QuizChatUseCase.kt`, `ComponentTypeRef.kt`, `ComponentType.kt`, `BuiltInComponent.kt`, существующие тесты `QuizGameImplTest`, `QuizGameImplEmptyListTest`, `LexemeBuiltInExtTest`, `QuizChatUseCaseImplTest`.

## Решение: тесты нужны (yes)

`needs_tests: true` per scope. Меняется публичное поведение `QuizChatUseCase` (+3 метода), `QuizGameImpl.fetchData` (filter logic), domain extension `ComponentType.toRef()`, reducer (+2 branches), DatasourceEffectHandler (+2 effects), новый `QuizPickerFlowHandler`. Все изменения покрываются unit-тестами на JVM (без Android-зависимостей).

## Test categories

### 1. Domain — `ComponentType.toRef()` extension (node #1)

**File:** `modules/domain/lexeme/src/test/java/me/apomazkin/lexeme/ComponentTypeRefExtTest.kt` (new).

Pure-JVM, по образцу `LexemeBuiltInExtTest.kt`.

| Case | Input | Expected |
|---|---|---|
| Built-in mapping | `systemKey=TRANSLATION, name=null` | `BuiltIn(TRANSLATION)` |
| Built-in с name override | `systemKey=TRANSLATION, name="Перевод"` | `BuiltIn(TRANSLATION)` (name игнорируется) |
| User-defined mapping | `systemKey=null, name="Definition"` | `UserDefined("Definition")` |
| User-defined с unicode/spec в name | `systemKey=null, name="hé:llo"` | `UserDefined("hé:llo")` |
| Invariant violation | `systemKey=null, name=null` | throws `IllegalStateException` (per spec `error(...)`) |

### 2. UseCase impl — `QuizChatUseCaseImpl` новые методы (node #4)

**File:** `app/src/test/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImplTest.kt` (extend existing). Pattern уже есть — `mockk<CoreDbApi.LexemeApi>`, `mockk<PrefsProvider>`.

| Case | Action | Expected |
|---|---|---|
| `getAvailableTypes` proxies LexemeApi | `lexemeApi.getComponentTypes(1L)` returns 2 entities (translation pos=0, Definition pos=1) | result = 2 domain `ComponentType`; порядок сохранён |
| `getAvailableTypes` empty | LexemeApi returns `emptyList()` | result = `emptyList()` |
| `getQuizPickerSelection` decode built-in | pref `quiz_picker_dict_1` = `"builtin:translation"` | `BuiltIn(TRANSLATION)` |
| `getQuizPickerSelection` decode user-defined | pref = `"user:Definition"` | `UserDefined("Definition")` |
| `getQuizPickerSelection` user-defined name с `:` | pref = `"user:My:Type"` | `UserDefined("My:Type")` (split по first `:`) |
| `getQuizPickerSelection` unknown built-in key | pref = `"builtin:unknown_xyz"` | `null` (future-proof) |
| `getQuizPickerSelection` corrupted format | pref = `"garbage"` либо `"json{}"` | `null` |
| `getQuizPickerSelection` empty pref | pref = `null` | `null` |
| `getQuizPickerSelection` empty builtin key (F2) | pref = `"builtin:"` | `null` (empty key → unknown) |
| `getQuizPickerSelection` case-sensitivity (F2) | pref = `"USER:Definition"` | `null` (prefix case-sensitive, `user`/`builtin` only) |
| `getQuizPickerSelection` no colon (F2) | pref = `"user"` | `null` (malformed, нет разделителя) |
| `setQuizPickerSelection` encode built-in | call `set(1L, BuiltIn(TRANSLATION))` | `prefsProvider.setStringByRawKey("quiz_picker_dict_1", "builtin:translation")` invoked |
| `setQuizPickerSelection` encode user-defined | call `set(1L, UserDefined("Definition"))` | `setStringByRawKey("quiz_picker_dict_1", "user:Definition")` invoked |
| Round-trip `UserDefined("")` empty name (F2) | `set(1L, UserDefined(""))` затем `get(1L)` | written `"user:"`; `get` returns `UserDefined("")` (round-trip preserved, empty name valid) |
| Per-dictionary key isolation | call `set(7L, ...)` и `set(42L, ...)` | keys `quiz_picker_dict_7` и `quiz_picker_dict_42` различимы |
| Overwrite на тот же dict key (F9) | `set(1L, BuiltIn(TRANSLATION))`; `set(1L, UserDefined("Definition"))`; `get(1L)` | `get` returns `UserDefined("Definition")`; `setStringByRawKey("quiz_picker_dict_1", ...)` invoked 2 раза с тем же key (overwrite semantics) |

### 3. PrefsProvider raw-string API (node #3)

**File:** `modules/datasource/prefs/src/test/java/me/apomazkin/prefs/PrefsProviderRawStringTest.kt` (new — нет существующих тестов модуля).

DataStore JVM-test через `PreferenceDataStoreFactory` + tmp file, либо `runTest` + in-memory fake. Минимум:

| Case | Action | Expected |
|---|---|---|
| set + get round-trip | `setStringByRawKey("k", "v")`; `getStringByRawKey("k")` | `"v"` |
| set null removes | `set("k", "v")`; `set("k", null)`; `get("k")` | `null` |
| get на unset key | `get("missing")` | `null` |
| Flow emits initial + writes | subscribe flow; `set("k", "a")`; `set("k", "b")` | emissions: `null, "a", "b"` (DataStore конвенция) |

**Опускаем** если интеграционный тест DataStore вне scope unit-test слоя — тогда полагаемся на DataStore contract (verified library) + покрываем encode/decode на UseCase слое (#2).

### 4. Reducer — `ChatReducer` новые branches (node #8)

**File:** `modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/logic/ChatReducerTest.kt` (new — отсутствует, в модуле нет ReducerTest).

Pattern по `DictionaryAppBarReducerTest`. `LexemeLogger` + `ResourceManager` mockk relaxed.

| Case | Pre-state | Msg | Post-state | Effects |
|---|---|---|---|---|
| Select emits Save effect, state unchanged | any | `SelectQuizComponent(UserDefined("Definition"))` | identical to pre | `{SaveQuizPickerSelection(UserDefined("Definition"))}` |
| Select без guard на availableTypes (F8) | `availableTypes=[translation, def]` | `SelectQuizComponent(UserDefined("Removed"))` (ref ∉ availableTypes) | identical to pre (no guard) | `{SaveQuizPickerSelection(UserDefined("Removed"))}` emitted (intentional, eventual consistency через fallback на restore) |
| Types loaded — translation-only restored | empty | `QuizComponentTypesLoaded([translation], BuiltIn(TRANSLATION))` | `quizComponent=QuizComponent([translation], BuiltIn(TRANSLATION))` | `{}` |
| Types loaded — restored valid | empty | `Loaded([translation, def], UserDefined("Definition"))` | `selectedRef = UserDefined("Definition")` | `{}` |
| Types loaded — restored invalid → fallback | empty | `Loaded([translation, def], UserDefined("Removed"))` | `selectedRef = BuiltIn(TRANSLATION)` (first по position) | `{}` |
| Types loaded — restored invalid + single-type → fallback (F3) | empty | `Loaded([translation], UserDefined("Removed"))` | `selectedRef = BuiltIn(TRANSLATION)` (fallback на единственный), `isPickerEnabled=false` | `{}` |
| Types loaded — restored null → default first | empty | `Loaded([translation, def], null)` | `selectedRef = BuiltIn(TRANSLATION)` | `{}` |
| Types loaded — empty types | empty | `Loaded([], BuiltIn(TRANSLATION))` | `quizComponent=QuizComponent([], null)` | `{}` |
| Double-emit idempotence (F7) | empty | `Loaded([t, d], BuiltIn(TRANSLATION))` x2 подряд (PrepareToStart→LoadQuizComponentTypes effect + FlowHandler.subscribe initial DataStore emit) | state stable: одинаковый `resolveSelection` result после обоих apply; selectedRef совпадает | `{}` каждый раз; no flicker |
| PrepareToStart emits LoadQuizComponentTypes | initial | `PrepareToStart` | stopLoading + welcome systemMessage | `{LoadQuizComponentTypes}` (extends existing branch) |

Default — первый по `position` (types preserve порядок из `getAvailableTypes`).

### 5. DatasourceEffectHandler — новые branches (node #7)

**File:** `modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/logic/DatasourceEffectHandlerTest.kt` (new — отсутствует, аналог `wordcard/DatasourceEffectHandlerTest`).

`mockk<QuizChatUseCase>` + `mockk<QuizGame>` + `runTest`. `useCase: QuizChatUseCase` добавлен в ctor (per design node #7).

| Case | Effect | Stubs | Expected emitted Msg |
|---|---|---|---|
| Load resolves dictId + emits Loaded | `LoadQuizComponentTypes` | `getCurrentDictionaryId()=1L`; `getAvailableTypes(1L)=[t]`; `getQuizPickerSelection(1L)=BuiltIn(TRANSLATION)` | `QuizComponentTypesLoaded([t], BuiltIn(TRANSLATION))` |
| Load — no current dict | `LoadQuizComponentTypes` | `getCurrentDictionaryId()=null` | `Msg.Empty` (per design `return@withContext Msg.Empty`) |
| Save calls UseCase + emits Empty | `SaveQuizPickerSelection(BuiltIn(TRANSLATION))` | `getCurrentDictionaryId()=1L` | `Msg.Empty`; `coVerify { useCase.setQuizPickerSelection(1L, BuiltIn(TRANSLATION)) }` |
| Save — no current dict | `SaveQuizPickerSelection(...)` | `getCurrentDictionaryId()=null` | `Msg.Empty`; `setQuizPickerSelection` НЕ вызван |

### 6. QuizPickerFlowHandler (node #14)

**File:** `modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/logic/QuizPickerFlowHandlerTest.kt` (new).

`runTest { TestScope }` + `MutableSharedFlow<String?>` как fake для `prefsProvider.getStringFlowByRawKey`. Минимум:

| Case | Setup | Action | Expected sent Msgs |
|---|---|---|---|
| Subscribe + initial emit | dict=1L; flow emits `null` | `subscribe(scope, send)` | `send(QuizComponentTypesLoaded(types, null))` (initial DataStore current value) |
| Re-emit on write | dict=1L; flow emits `null` then `"user:Definition"` | как выше | 2 emissions; second has `restoredSelectedRef=UserDefined("Definition")` |
| No-op без current dict | `getCurrentDictionaryId()=null` | subscribe | flow НЕ subscribed; send никогда не вызван |
| Null-dict terminal — повторные pref-writes не доходят (F1) | `getCurrentDictionaryId()=null` at subscribe; затем сторонний код вызывает `setStringByRawKey(...)` 2 раза | subscribe; затем pref writes | handler dead: send никогда не вызван (terminal state до re-init ViewModel'а); никаких side-effects |
| Each emit re-fetches available types | flow emit twice | как выше | `useCase.getAvailableTypes(dictId)` вызван 2 раза |

### 7. QuizGameImpl.fetchData — filter integration (node #16)

**File:** `modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/quiz/QuizGameImplFetchDataTest.kt` (new), параллельно существующему `QuizGameImplTest`. Существующий тест покрывает `toQuizItem` сам по себе; новый покрывает session-time filter перед mapping.

| Case | Setup | Expected |
|---|---|---|
| selectedRef non-null → filter to single ref | `getQuizPickerSelection(1L)=UserDefined("Definition")`; quizConfig.componentRefs=[translation, Definition] | `toQuizItem` (внутри) вызван с `componentRefs=[UserDefined("Definition")]` (effectiveRefs) |
| selectedRef null → fallback на quizConfig.componentRefs | `getQuizPickerSelection(1L)=null` | `toQuizItem` с `componentRefs=[translation, Definition]` (full quizConfig) |
| selectedRef override semantics — quizConfig игнорируется (F4) | `selectedRef=UserDefined("Foo")`; `quizConfig.componentRefs=[BuiltIn(TRANSLATION)]` (selectedRef ∉ quizConfig) | `effectiveRefs=[UserDefined("Foo")]`; `toQuizItem` вызван с `[UserDefined("Foo")]`; quizConfig игнорируется (override, не пересечение — intentional) |
| Lexeme без выбранного компонента → graceful skip | selectedRef=UserDefined("Definition"); lexeme содержит только translation | `toQuizItem` returns null → loadData/quizList пустой; `hasNextQuestion=false` (no crash, IS461 pattern) |

Реализация: проверять через `quizGame.hasNextQuestion()` после `loadData()` либо через stub на `WriteQuiz.toQuizItem` (если возможно — extension, mockk).

### 8. UI (Tier 3) — manual smoke

Reducer-test (#4) полностью покрывает state derivation `isPickerEnabled = availableTypes.size > 1` через invariants. Composable rendering — manual smoke per `checklist.md` («Ручное тестирование» секция). Compose-instrumented тесты не вводятся.

Дополнительные manual smoke сценарии:
- **Empty availableTypes (F5).** Создать словарь, удалить все `ComponentType` rows (через SQL inspector или fresh schema). Открыть chat → picker полностью отсутствует в actions menu (не disabled, а скрыт), нет crash, quiz session не запускается (graceful empty state).
- **Persist через cold start (F6).** Выбрать `UserDefined("Definition")` в picker. **Force stop** через системные настройки (Settings → Apps → PolyTrainer → Force stop) — **не swipe из recent** (recent swipe не гарантирует kill процесса; force stop даёт full cold start с inflated DataStore). Открыть app → выбор восстановлен.
- **Dictionary-switch / null-dict re-init (F1).** Открыть chat при `getCurrentDictionaryId()=null` (нет словарей) → picker отсутствует, handler в terminal state. Создать dictionary через UI → re-init via tab navigation (выйти из quiz tab, зайти обратно) → ViewModel пересоздаётся, picker загрузился с availableTypes текущего словаря.

## Out of scope

- **Atomicity test** для `setQuizPickerSelection` — не нужен. Single-key DataStore write, no FK, no compound INSERT (verified: design node #4 — один `setStringByRawKey` call). Race conditions DataStore покрываются library.
- **Migration tests** — `needs_migration_tests: false` per scope (data слой не трогается).
- **Encoding round-trip property test** — каждая ветка encode/decode тестируется явно (#2), kotlinx.serialization не вводится.

## Test execution

```bash
./gradlew :modules:domain:lexeme:testDebugUnitTest --tests "*.ComponentTypeRefExtTest"
./gradlew :modules:screen:quiz:chat:testDebugUnitTest --tests "*.ChatReducerTest"
./gradlew :modules:screen:quiz:chat:testDebugUnitTest --tests "*.DatasourceEffectHandlerTest"
./gradlew :modules:screen:quiz:chat:testDebugUnitTest --tests "*.QuizPickerFlowHandlerTest"
./gradlew :modules:screen:quiz:chat:testDebugUnitTest --tests "*.QuizGameImplFetchDataTest"
./gradlew :app:testDebugUnitTest --tests "*.QuizChatUseCaseImplTest"
./gradlew :modules:datasource:prefs:testDebugUnitTest  # если #3 включён
```

_model: claude-opus-4-7[1m]_

## log_messages

- Iter 2: 9 QA findings закрыты добавлением edge case rows в существующие таблицы (#2 +4 cases, #4 +3 cases, #6 +1 case, #7 +1 case) + 3 manual smoke сценария (F1/F5/F6). Структура (7 категорий + manual smoke + checklist_items) не менялась.
- Encoding edge cases (F2): empty `UserDefined("")` round-trip, `"builtin:"` empty key→null, `"USER:..."` case-sensitivity→null, `"user"` no-colon→null. Overwrite semantics (F9): 2 `setStringByRawKey` calls с тем же key.
- Reducer additions: single-type invalid restore→fallback (F3), `SelectQuizComponent` без guard на availableTypes (F8), initial double-emit idempotence (F7); override semantics в QuizGameImpl (F4): quizConfig игнорируется если selectedRef задан.

## checklist_items

Привязка test cases к sub-item'ам checklist.md.

- **User открывает chat → видит picker с правильным состоянием** [root]
  - [ ] Reducer: `QuizComponentTypesLoaded(types, restored=valid)` → state.quizComponent корректно установлен (#4 case "restored valid")
  - [ ] Reducer: `Loaded([], _)` → `quizComponent=QuizComponent([], null)` (UI скрывает) (#4 case "empty types")
  - [ ] Reducer: `PrepareToStart` emits `LoadQuizComponentTypes` (#4 case "PrepareToStart")
  - [ ] DatasourceEffectHandler: `LoadQuizComponentTypes` → emit `QuizComponentTypesLoaded` с types + restored (#5 case "Load resolves")
  - [ ] DatasourceEffectHandler: `LoadQuizComponentTypes` без current dict → `Msg.Empty` (#5 case "no current dict")
- **User меняет выбор → выбор сохраняется в prefs per-dictionary** [root]
  - [ ] Reducer: `SelectQuizComponent(ref)` → effect `SaveQuizPickerSelection`, state unchanged (#4 case "Select emits")
  - [ ] DatasourceEffectHandler: `SaveQuizPickerSelection` calls `useCase.setQuizPickerSelection(dictId, ref)` (#5 case "Save calls UseCase")
  - [ ] UseCaseImpl: `setQuizPickerSelection(BuiltIn(TRANSLATION))` writes `"builtin:translation"` (#2 case "encode built-in")
  - [ ] UseCaseImpl: `setQuizPickerSelection(UserDefined("Definition"))` writes `"user:Definition"` (#2 case "encode user-defined")
  - [ ] UseCaseImpl: per-dictionary key isolation (`quiz_picker_dict_<id>`) (#2 case "Per-dictionary key isolation")
  - [ ] QuizPickerFlowHandler: re-emit `QuizComponentTypesLoaded` on pref write (#6 case "Re-emit on write")
- **User запускает quiz session → componentRefs отфильтрован по selectedRef** [root]
  - [ ] QuizGameImpl: non-null selectedRef → `effectiveRefs = [selectedRef]` (#7 case "selectedRef non-null")
  - [ ] QuizGameImpl: null selectedRef → fallback на `quizConfig.componentRefs` (#7 case "selectedRef null fallback")
  - [ ] QuizGameImpl: lexeme без выбранного компонента → graceful skip (no crash) (#7 case "graceful skip")
- **User возвращается в словарь → previous выбор восстановлен из prefs** [root]
  - [ ] UseCaseImpl: decode `"builtin:translation"` → `BuiltIn(TRANSLATION)` (#2 case "decode built-in")
  - [ ] UseCaseImpl: decode `"user:Definition"` → `UserDefined("Definition")` (#2 case "decode user-defined")
  - [ ] UseCaseImpl: decode user-defined name с `:` (split по first `:`) (#2 case "name с :")
  - [ ] UseCaseImpl: unknown built-in key → null (#2 case "unknown built-in")
  - [ ] UseCaseImpl: corrupted format → null (#2 case "corrupted format")
  - [ ] UseCaseImpl: empty pref → null (#2 case "empty pref")
  - [ ] Reducer fallback: restored invalid → первый по position (#4 case "restored invalid → fallback")
  - [ ] Reducer fallback: restored null → первый (#4 case "restored null → default first")
  - [ ] Domain `toRef()`: built-in / user-defined / invariant violation (#1 cases all)
