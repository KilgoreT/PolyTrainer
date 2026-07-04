# Business implement: IS481 Quiz Component Picker

Реализованы все 17 узлов из `business_design_tree.md`. Тесты пройдены.

## Изменённые / созданные файлы

### Domain (`modules/domain/lexeme`)
- `ComponentTypeRef.kt` [~] — добавлен top-level `fun ComponentType.toRef(): ComponentTypeRef`.
- `ComponentTypeRefExtTest.kt` [+] — 5 unit tests (built-in, name override, user-defined, unicode/colon name, invariant violation).

### Datasource (`modules/datasource/prefs`)
- `PrefsProvider.kt` [~] — добавлены `getStringByRawKey` / `getStringFlowByRawKey` / `setStringByRawKey` (raw-string dynamic-key API, `stringPreferencesKey` под капотом).
- `QuizPickerPrefKey.kt` [+] — top-level helper `fun quizPickerPrefKey(dictionaryId: Long): String`. Single source of truth, shared между UseCaseImpl и FlowHandler.

### Strings (`core/core-resources`)
- `values/strings.xml` [~] — `chat_menu_item_quiz_component` = "Quiz component", `chat_menu_item_component_translation` = "Translation".
- `values-ru-rRU/strings.xml` [~] — "Компонент квиза", "Перевод".

### UseCase iface (`modules/screen/quiz/chat/.../deps`)
- `QuizChatUseCase.kt` [~] — 3 новых метода: `getAvailableTypes(dictId)`, `getQuizPickerSelection(dictId)`, `setQuizPickerSelection(dictId, ref)`.

### UseCase impl (`app/.../quizchat`)
- `QuizChatUseCaseImpl.kt` [~] — 3 impl + private `encodeRef`/`decodeRef` (без JSON, формат `builtin:<key>` / `user:<name>`, decode через `substringAfter(':')`, unknown built-in → null, иной prefix → null).
- `QuizChatUseCaseImplTest.kt` [~] — +17 cases (decode/encode happy paths + 6 edge cases F2 + per-dict isolation + overwrite F9).

### Mate (`modules/screen/quiz/chat/.../logic`)
- `State.kt` [~] — `ItemsState.QuizComponent(availableTypes, selectedRef?)` + computed `isPickerEnabled` + `updateQuizComponent()` extension.
- `Message.kt` [~] — `Msg.SelectQuizComponent(ref)` + `Msg.QuizComponentTypesLoaded(types, restoredSelectedRef)`.
- `DatasourceEffectHandler.kt` [~] — `DatasourceEffect.LoadQuizComponentTypes` + `SaveQuizPickerSelection`; ctor расширен `useCase: QuizChatUseCase`; в `onEffect` — 2 ветки (dictId через `getCurrentDictionaryId()`).
- `ChatReducer.kt` [~] — `Msg.PrepareToStart` extended (emit `LoadQuizComponentTypes` к existing); +2 branch (`SelectQuizComponent` → `SaveQuizPickerSelection`, state unchanged; `QuizComponentTypesLoaded` → `updateQuizComponent` через private `resolveSelection(types, restored)` helper).
- `QuizPickerFlowHandler.kt` [+] — `MateFlowHandler<Msg, Effect>` подписывается на `getStringFlowByRawKey(quizPickerPrefKey(dictId))`; на каждый emit re-emit `QuizComponentTypesLoaded`. При null dict — handler dead.
- `ChatReducerTest.kt` [+] — 10 cases (Select, Loaded х restored variants, F3 single-type, F7 idempotence, F8 no guard, PrepareToStart).
- `DatasourceEffectHandlerTest.kt` [+] — 4 cases (Load happy + null dict, Save happy + null dict). Использует `FakeUseCase` вместо mockk — mockk fails с `@JvmInline value class ComponentTypeRef` (см. Проблемы).
- `QuizPickerFlowHandlerTest.kt` [+] — 4 cases (initial emit, re-emit, no-op без dict, re-fetch on each emit).

### Quiz session (`modules/screen/quiz/chat/.../quiz`)
- `QuizGameImpl.kt` [~] — в `fetchData()` после load `quizConfig`: `selectedRef = useCase.getQuizPickerSelection(dictId)`; `effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs`; передаётся в `toQuizItem(componentRefs = ...)`.
- `QuizGameImplFetchDataTest.kt` [+] — 4 cases (selectedRef override, null fallback, F4 quizConfig ignored, graceful skip).
- `QuizGameImplEmptyListTest.kt` [~] — добавлен stub `getQuizPickerSelection(any()) returns null` (новая dep в QuizGameImpl).

### UI (`modules/core/ui/.../dropdown` + `modules/screen/quiz/chat/.../widget/appbar`)
- `core/ui/dropdown/LexemeSubmenuMenuItem.kt` [+] — Tier 1 generic submenu wrapper (`DropdownMenuItem` header + chevron + inline `Column` content; remembered `isExpanded` state). Использует `String` title вместо `StringSource` — core/ui не зависит от widget/iconDropDowned.
- `core/ui/dropdown/LexemeRadioMenuItem.kt` [+] — Tier 1 generic radio menu item (`DropdownMenuItem` + `RadioButton` leading icon).
- `widget/appbar/menu/ComponentChoiceItem.kt` [+] — per-type wrapper над `LexemeRadioMenuItem`; title resolution: built-in TRANSLATION → res; UserDefined → `name` raw.
- `widget/appbar/menu/QuizComponentMenuItem.kt` [+] — top-level picker; скрывает себя при `availableTypes.isEmpty()`; `enabled = isPickerEnabled`.
- `widget/appbar/ActionsWidget.kt` [~] — встроен `QuizComponentMenuItem` между `MistakesMenuItem` и debug-блоком.

### ViewModel (`modules/screen/quiz/chat`)
- `ChatViewModel.kt` [~] — `quizPickerFlowHandler: QuizPickerFlowHandler` в ctor + добавлен в `effectHandlerSet`.

## Корректировки vs spec / design_tree

1. **`quizPickerPrefKey` помещён в `:modules:datasource:prefs`** (не в UseCaseImpl) — single source of truth для обоих consumer'ов (`QuizChatUseCaseImpl` + `QuizPickerFlowHandler`). Оба модуля и так depend on `:modules:datasource:prefs`, циклов нет.
2. **Tier 1 primitives используют `String` вместо `StringSource`** — `me.apomazkin.ui.dropdown` package в core/ui модуле, который не depends на `widget/iconDropDowned` (где живёт `StringSource`). Caller вызывает `stringResource()` перед передачей. Это в духе Tier 1 (pure presentational, минимум абстракций).
3. **`SaveQuizPickerSelection` при null dict** — handler emit'ит `Msg.Empty` (set не вызывается, без throw). Соответствует existing pattern `LoadQuizComponentTypes` ветки.

## Проблемы

### mockk + `@JvmInline value class ComponentTypeRef`

При попытке использовать mockk matchers (`any()`, `capture()`, `coVerify`) для параметров типа `ComponentTypeRef` — mockk fails с `IllegalStateException: null packRef for class ComponentTypeRef signature BuiltIn(key=null)`. Причина: mockk не умеет распаковывать `@JvmInline value class` при auto-hinting matchers.

**Workaround:** `DatasourceEffectHandlerTest` использует `FakeUseCase` (вручную написанный stub `QuizChatUseCase`) вместо mockk. Альтернативный обход — `coEvery { ... } answers { ... }` с захватом через `firstArg()`/`secondArg()` — тоже работает но менее чисто (требует `relaxed = true` ИЛИ всё равно ловит при `any()` matcher).

В `QuizChatUseCaseImplTest` проблемы нет — там coVerify на `PrefsProvider.setStringByRawKey(string, string)` (примитивные типы, value class не задействован).

## Test results

```
:modules:domain:lexeme:test            ✅  5  ComponentTypeRefExtTest
:modules:screen:quiz:chat:testDebugUnitTest  ✅  34 (4 new test files + 3 existing)
  - ChatReducerTest:              10
  - DatasourceEffectHandlerTest:   4
  - QuizPickerFlowHandlerTest:     4
  - QuizGameImplFetchDataTest:     4
  - QuizGameImplTest:              8 (existing, untouched)
  - QuizGameImplEmptyListTest:     3 (+1 stub line)
  - ExampleUnitTest:               1
:app:testDebugUnitTest                 ✅  QuizChatUseCaseImplTest: 23 (was 6, +17)
:modules:datasource:prefs:testDebugUnitTest  ✅  (Example test only — PrefsProvider DataStore test skipped per test plan, integration scope)
```

Все категории 1, 2, 4, 5, 6, 7 из `business_test.md` покрыты. Категория 3 (PrefsProvider raw-string) — опущена per test plan (DataStore library contract). Категория 8 (Tier 3 UI) — manual smoke в чек-листе.

_model: claude-opus-4-7[1m]_

## log_messages

- 17 узлов реализованы; 4 новых test файла + extend существующих; все тесты пройдены.
- `quizPickerPrefKey` помещён в `:modules:datasource:prefs` (не impl) — single source of truth для UseCaseImpl + FlowHandler.
- mockk fails с `@JvmInline value class ComponentTypeRef` — DatasourceEffectHandlerTest использует FakeUseCase workaround.

## checklist_items

- **User открывает chat → видит picker с правильным состоянием** [root]
  - [x] `LexemeSubmenuMenuItem` + `LexemeRadioMenuItem` primitives созданы (core/ui/dropdown)
  - [x] `QuizComponentMenuItem` рендерит подменю, скрывает себя при empty availableTypes
  - [x] `ComponentChoiceItem` маркирует selected ref через `type.toRef() == selectedRef`
  - [x] `ActionsWidget` встраивает picker между Mistakes и debug-блоком
  - [x] `ItemsState.QuizComponent` + `isPickerEnabled` computed helper
  - [x] `LoadQuizComponentTypes` triggered из `Msg.PrepareToStart`; `QuizComponentTypesLoaded` обновляет state через `resolveSelection`
  - [x] Default fallback на первый из availableTypes если restored ref не in list
  - [x] Строки `chat_menu_item_quiz_component` + `chat_menu_item_component_translation` (+ ru) добавлены
- **User меняет выбор → выбор сохраняется в prefs per-dictionary** [root]
  - [x] `Msg.SelectQuizComponent(ref)` emit'ит Save effect, state не меняется
  - [x] DatasourceEffectHandler.SaveQuizPickerSelection через `useCase.setQuizPickerSelection(dictId, ref)`
  - [x] UseCaseImpl encode+write через PrefsProvider.setStringByRawKey
  - [x] PrefsProvider raw-string API + `quizPickerPrefKey(dictId)`
  - [x] QuizPickerFlowHandler re-emit `QuizComponentTypesLoaded` → state update
- **User запускает quiz session → componentRefs отфильтрован по selectedRef** [root]
  - [x] QuizGameImpl.fetchData вызывает `useCase.getQuizPickerSelection(dictionaryId)`
  - [x] non-null → `effectiveRefs = listOf(selectedRef)`
  - [x] null → fallback на `quizConfig.componentRefs`
  - [x] UseCase iface + impl расширены 3 методами
- **User возвращается в словарь → previous выбор восстановлен из prefs** [root]
  - [x] Per-dictionary key `quiz_picker_dict_<id>` гарантирует изоляцию
  - [x] Initial load через LoadQuizComponentTypes effect → emit с restored ref
  - [x] Encoding: `builtin:<key>` / `user:<name>` через `substringAfter(':')`
  - [x] `ComponentType.toRef()` extension в domain
  - [x] DataStore persistence (PrefsProvider unchanged для cold start surv)
