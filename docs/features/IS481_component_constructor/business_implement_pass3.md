# Pass 3 (Tier 5 ComponentsManager Mate) — IS481 component_constructor

Scope: ComponentsManager Mate layer (State/Msg/Effect/UiMsg/Reducer/FlowHandler/DatasourceEffectHandler/UiEffectHandler/NavigationEffect+Handler/ViewModel) + изолированные unit-тесты Reducer + EffectHandler + FlowHandler.

## Создано

### Mate files (package `me.apomazkin.components_manager.mate`)

- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/State.kt` — `ComponentsManagerScreenState`, `UserDefinedRow`, `CreateDialogState`, `RenameDialogState`, `DeleteConfirmState`, computed `isEmpty`, `internal fun UserDefinedTypesSnapshot.toRows()` extension. (узел #29)
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/Msg.kt` — sealed `Msg` (20+ branches) + top-level `sealed interface UiMsg : Msg` (per F086). (узел #30)
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/UiEffect.kt` — sealed `UiEffect : Effect` с `Snackbar(text)`.
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/UiEffectHandler.kt` — маппит `UiEffect.Snackbar → UiMsg.Snackbar`. (узел #31)
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/DatasourceEffect.kt` — sealed: `CreateComponent`, `RenameComponent`, `LoadImpact`, `SoftDeleteComponent`. (узел #32)
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/DatasourceEffectHandler.kt` — Effect → UseCase → `Msg.*Result`/`ImpactPreviewLoaded/Failed` mapping; `withContext(Dispatchers.IO)` + try/catch fallback на typed Failure outcomes. (узел #33)
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/AllUserDefinedTypesFlowHandler.kt` — `MateFlowHandler` подписывается на `useCase.flowAllUserDefinedTypes()` на init Mate; `.catch{} → Msg.TypesLoadFailed`, `.collectLatest → Msg.TypesLoaded`. (узел #34)
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/NavigationEffect.kt` — `sealed interface ComponentsManagerNavigationEffect : NavigationEffect` без variants (Back из base). (узел #35)
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/NavigationEffectHandler.kt` — `@AssistedInject` от `ComponentsManagerNavigator`; `onScreenEffect` — no-op (Back в super). (узел #36)
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/Reducer.kt` — `ComponentsManagerReducer`, exhaustive `when` по Msg branches; реализует guards (double-tap, isLoadingImpact/F102), overwrite reset (F106), race close-during-flight (F101 — snackbar fallback когда dialog уже закрыт). (узел #37)

### Tests (package `me.apomazkin.components_manager.mate`)

- `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/mate/ComponentsManagerReducerTest.kt` — 55 тестов.
- `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/mate/DatasourceEffectHandlerTest.kt` — 10 тестов.
- `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/mate/AllUserDefinedTypesFlowHandlerTest.kt` — 4 теста.

## Модифицировано

- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerViewModel.kt` — placeholder заменён реальной Mate-сборкой: ctor inject `DatasourceEffectHandler`/`AllUserDefinedTypesFlowHandler`/`UiEffectHandler` + `NavigationEffectHandler.Factory`; `initEffects = ∅` (FlowHandler auto-starts via subscribe); добавлен `override fun onCleared()` → `stateHolder.dispose()` для unsubscribe job. (узел #38)
- `modules/screen/components_manager/build.gradle.kts` — добавлены `implementation(:modules:core:logger)` + `testImplementation(testLibs.junit/mockk/coroutinesTest)`.

## Tests pass / fail

| Suite | Tests | Pass | Fail |
|---|---|---|---|
| ComponentsManagerReducerTest | 55 | 55 | 0 |
| DatasourceEffectHandlerTest | 10 | 10 | 0 |
| AllUserDefinedTypesFlowHandlerTest | 4 | 4 | 0 |
| **Total** | **69** | **69** | **0** |

Команды (cc-build wrapper):
```
./scripts/cc-build.sh :modules:screen:components_manager:testDebugUnitTest --tests "*ComponentsManagerReducerTest*"
./scripts/cc-build.sh :modules:screen:components_manager:testDebugUnitTest --tests "*DatasourceEffectHandlerTest*"
./scripts/cc-build.sh :modules:screen:components_manager:testDebugUnitTest --tests "*AllUserDefinedTypesFlowHandlerTest*"
```

Все BUILD SUCCESSFUL. Test reports: `modules/screen/components_manager/build/reports/tests/testDebugUnitTest/index.html`.

## Нетривиальные решения

1. **`UiMsg` top-level (F086 parity)** — `sealed interface UiMsg : Msg` лежит вне `sealed interface Msg` (а не как nested `Msg.UiMsg`) ради consistency с design tree + existing wordcard/quizchat convention. Reducer обрабатывает `is UiMsg` единой no-op веткой.

2. **Race close-during-flight (F101) — snackbar fallback** — все `*Result` ветки (CreateResult/RenameResult/DeleteResult) проверяют `state.<dialog> == null`: если диалог был закрыт пока operation в полёте — error-ветки emit `UiEffect.Snackbar(...)` вместо silent state update; dialog state не воскрешается (`createDialog/renameDialog/deleteConfirm` остаётся `null`).

3. **`ConfirmDelete` тройной guard** — кроме `isDeleting=true` (двойной тап) и `deleteConfirm == null` (no dialog), добавлен guard на `dlg.isLoadingImpact=true` (F102): подтверждение запрещено пока preview грузится — UX-инвариант.

4. **`OpenCreateDialog` overwrite reset (F106)** — `Msg.OpenCreateDialog` ВСЕГДА копирует `createDialog = CreateDialogState()` (свежий default), без проверки на existing — это намеренный reset to clean state, не игнор (обоснование: пользователь явно открывает диалог, ожидая чистого state).

5. **`onCleared()` dispose** — добавлен `override fun onCleared() { super.onCleared(); stateHolder.dispose() }` в ViewModel для cancel'а `MateFlowHandler.job` (избегает leak подписки в memory).

6. **`CreateOutcome.NameTooLong` branch добавлен в reducer** — domain sealed (`CreateOutcome.kt`) включает `NameTooLong` (Pass 1 reserved), хотя UseCase impl этой ветки не emit'ит (Pass 2 решение: maxLen не определён). Reducer обязан handle exhaustive `when` — добавлена generic «Name too long» snackbar + `NameError.TooLong` mapping. RenameOutcome аналогичной ветки не имеет — там и не нужно.

7. **`DatasourceEffectHandler` обёрт в общий try/catch** — UseCase impl сам обёртывает internal exceptions в `*.Failure(cause)`, но defensive catch вокруг `when` обеспечивает graceful degradation для unexpected throws (e.g. injection failure, OOM); каждая ветка маппится в соответствующий typed Failure (`CreateOutcome.Failure` / `RenameOutcome.Failure` / `DeleteOutcome.Failure` / `Msg.ImpactPreviewFailed`).

## Известные follow-ups для Pass 4-5

- **Pass 4** (PerDictionary Mate): зеркальное Pass 3 (узлы #39-#48, #56). Отличия: init-параметр `dictionaryId` через `@Assisted` в FlowHandler + ViewModel, `Msg.ItemsLoaded(PerDictionarySnapshot)` вместо `TypesLoaded`, `OpenCreateDialog` preselect `scope = Scope.PerDictionaries(listOf(state.dictionaryId))`.
- **Pass 5** (migration): `LexemeMapper.kt` / `WordCardUseCaseImpl.kt` / `QuizGameImpl.kt` / `Lexeme.kt` + 6 existing test files mechanical rebind `ComponentValueData → TemplateValues`.
