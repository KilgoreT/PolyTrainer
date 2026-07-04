# Pass 4 (Tier 6 PerDictionary Mate) — IS481 component_constructor

Scope: PerDictionary Mate layer (State/Msg/Effect/UiMsg/Reducer/FlowHandler/DatasourceEffectHandler/UiEffectHandler/NavigationEffect+Handler/ViewModel) + изолированные unit-тесты Reducer + EffectHandler + FlowHandler.

Зеркально Pass 3 (CM Mate) — два отличия:

1. **assisted `dictionaryId: Long`** — приходит через navigation, хранится в `State.dictionaryId`. ViewModel принимает через `@Assisted`-инжект; `ComponentsForDictionaryFlowHandler` тоже assisted (factory pattern).
2. **`OpenCreateDialog` preselect scope** — `Scope.PerDictionaries(listOf(state.dictionaryId))` вместо `Scope.Global`. User может override на Global через `CreateScopeChange` если business contract разрешит (Msg оставлен для совместимости с CM).

Все critical fixes (F123/F124/F127/F132/F136/F138/F140) реализованы **с самого начала** (без retrofit'а — как требуется prompt'ом).

## Создано

### Mate files (package `me.apomazkin.per_dictionary_components.mate`)

- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/State.kt` — `PerDictionaryComponentsScreenState` (с `dictionaryId`/`dictionaryName`), `PerDictRow`, `CreateDialogState`, `RenameDialogState`, `DeleteConfirmState`, `SnackbarState`, computed `isEmpty`, `internal fun PerDictionarySnapshot.toPerDictRows()` extension. (узел #39)
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/Msg.kt` — sealed `Msg` (20+ branches): `ItemsLoaded/Failed`, Create/Rename/Delete семейства c `epochId`/`typeId` correlation tokens + top-level `sealed interface UiMsg : Msg` per F086. (узел #40)
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/UiEffect.kt` — sealed `UiEffect : Effect` с `Snackbar(text)`.
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/UiEffectHandler.kt` — маппит `UiEffect.Snackbar → UiMsg.Snackbar` (без `show` — F128). (узел #41)
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/DatasourceEffect.kt` — sealed: `CreateComponent(epochId, ...)`, `RenameComponent(epochId, ...)`, `LoadImpact(typeId)`, `SoftDeleteComponent(epochId, typeId)`. (узел #42)
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/DatasourceEffectHandler.kt` — Effect → UseCase → `Msg.*Result`/`ImpactPreviewLoaded/Failed`; `withContext(Dispatchers.IO)` + `CancellationException` re-throw (F125) + try/catch fallback на typed Failure outcomes; epochId/typeId пробрасываются обратно в Result. (узел #43)
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/ComponentsForDictionaryFlowHandler.kt` — `MateFlowHandler` с `@AssistedInject(dictionaryId)`; подписывается на `useCase.flowComponentsForDictionary(dictionaryId)` на init Mate; `.catch{} → Msg.ItemsLoadFailed`, `.collectLatest → Msg.ItemsLoaded`. (узел #44)
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/NavigationEffect.kt` — `sealed interface PerDictionaryComponentsNavigationEffect : NavigationEffect` без variants (Back из base). (узел #45)
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/NavigationEffectHandler.kt` — `@AssistedInject` от `PerDictionaryComponentsNavigator`; `onScreenEffect` — no-op (Back в super). (узел #46)
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/Reducer.kt` — `PerDictionaryComponentsReducer`, exhaustive `when` по Msg branches; все guards (double-tap, isLoadingImpact/F102), overwrite reset (F106), race close-during-flight (F101 — snackbar fallback), mutual-exclusion диалогов (F138), epoch correlation guard (F136), typeId stale guard (F124), snackbarState через UiMsg (F123), `Throwable.failureLabel()` fallback (F129). (узел #47)

### Tests (package `me.apomazkin.per_dictionary_components.mate`)

- `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/mate/PerDictionaryComponentsReducerTest.kt` — **61 тест**.
  - 4.1 Lifecycle: ItemsLoaded (с dictName propagation) / empty snapshot → isEmpty / ItemsLoadFailed
  - 4.2 Create: open + scope preselect / open closes others (F138) / overwrite reset (F106) + preselect persists / name|template|multi|scope change / submit happy + blank + guard / each CreateOutcome result + race (F101) + stale epochId (F136)
  - 4.3 Rename: open found/missing/closes-others / textChange / submit happy+blank+guard / each RenameOutcome result + race + stale epochId
  - 4.4 Delete: open found/missing/same-typeId-no-retrigger/closes-others / preview Loaded/Failed + stale typeId (F124) / Confirm happy + isDeleting guard + isLoadingImpact guard (F102) / each DeleteOutcome + race + stale epochId
  - 4.5 Snackbar (F123): UiMsg.Snackbar writes snackbarState / DismissSnackbar
  - 4.6 Navigation/no-op: RequestBack → NavigationEffect.Back / Msg.Empty
  - 4.7 toPerDictRows helper: empty / isGlobal inference / valueCount fallback
- `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/mate/DatasourceEffectHandlerTest.kt` — **12 тестов**: 4 Effect branches × happy / error + CancellationException re-throw (F125) для Create/Rename + LoadImpact null/throw + DeleteSuccess/Failure.
- `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/mate/ComponentsForDictionaryFlowHandlerTest.kt` — **4 теста**: initial emit / re-emit on each snapshot / throw → ItemsLoadFailed + logged / assisted dictionaryId propagation verify.

## Модифицировано

- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsViewModel.kt` — placeholder заменён реальной Mate-сборкой:
  - `@AssistedInject` с `dictionaryId: Long` + `PerDictionaryComponentsNavigator`
  - ctor inject `DatasourceEffectHandler` / `ComponentsForDictionaryFlowHandler.Factory` / `UiEffectHandler` / `NavigationEffectHandler.Factory`
  - `initEffects = ∅` (FlowHandler auto-starts via subscribe)
  - init state c `dictionaryId`/`isLoading = true`
  - `override fun onCleared()` → `stateHolder.dispose()`. (узел #48)

## Tests pass / fail

| Suite | Tests | Pass | Fail |
|---|---|---|---|
| PerDictionaryComponentsReducerTest | 61 | 61 | 0 |
| DatasourceEffectHandlerTest | 12 | 12 | 0 |
| ComponentsForDictionaryFlowHandlerTest | 4 | 4 | 0 |
| **Total** | **77** | **77** | **0** |

Команда (cc-build wrapper):
```
./scripts/cc-build.sh :modules:screen:per_dictionary_components:testDebugUnitTest
```

BUILD SUCCESSFUL. Test reports: `modules/screen/per_dictionary_components/build/reports/tests/testDebugUnitTest/index.html`.

## Нетривиальные решения

1. **`toPerDictRows()` без явного `dictionaryId` параметра** — снапшот сам несёт `dictionaryId`, mapper не нуждается в дополнительном контексте. Если в будущем потребуется (cross-dict consolidation), сигнатуру можно расширить.
2. **`Scope.PerDictionaries(listOf(state.dictionaryId))` инициализируется только в `OpenCreateDialog`** — не в data class default (где `dictionaryId` ещё не известен, потому что `CreateDialogState` instantiated в state init только при открытии диалога). Reducer переинициализирует scope на каждое открытие диалога — preselect persistent across re-open.
3. **`CreateScopeChange` Msg сохранён** — даёт пользователю возможность переопределить на `Scope.Global`, если business contract разрешит в UI sub-flow. Само по себе не нарушает PerDict invariants — текущий словарь остаётся в state.
4. **Critical fixes (F123/F124/F127/F132/F136/F138/F140) применены сразу** — без retrofit-итерации как в Pass 3. CM Mate уже отработал эти fixes — реплицированы зеркально.

## Известные TODO

- **Pass 5** (migration call-sites): not started — `LexemeMapper.kt` / `WordCardUseCaseImpl.kt` / `QuizGameImpl.kt` / `LexemeBuiltInExt.kt` / `Lexeme.kt` + 6 existing test files mechanical rebind.
- **data_implement** sub-flow: impl новых LexemeApi методов в `:core:core-db-impl`.

_model: claude-opus-4-7[1m]_
