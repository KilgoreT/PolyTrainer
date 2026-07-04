# business_implement.md — IS481 component_constructor

**Status: complete** (все 5 passes ✅ done после 5 review iterations — см. § Состояние и § История ревью). Build broken для `:app:testDebugUnitTest` — by design, ждёт `data_implement` sub-flow для impl новых LexemeApi методов в `:core:core-db-impl`.

**Process note:** Conductor нарушил `execute_step_once` псевдокод (multiple sub-agent passes внутри одной execute фазы вместо одного agent_execute) — зафиксировано в `docs/FlowBacklog.md → IS481cc-F7`. С iter 3 protocol corrected — каждый pass = одна iteration с review между.

## Состояние

Реализация разбита de-facto на 5 частей (passes) — превышает context лимит одного sub-agent.

| Pass | Tier | Scope | Status |
|---|---|---|---|
| 1 | 0-3 | domain types + data-API расширение + UseCase interfaces | ✅ done (см. `business_implement_pass1.md`) |
| 2 | 4 + LogTags | UseCase impls (Components/PerDictionary) + LogTags + UseCase impl тесты | ✅ done (см. `business_implement_pass2.md`) |
| 3 | 5 | ComponentsManager Mate (Msg, Effect, UiMsg, State, Reducer, Handlers, ViewModel) + тесты | ✅ done (см. `business_implement_pass3.md`) |
| 4 | 6 | PerDictionary Mate (зеркально #3) + тесты | ✅ done (см. `business_implement_pass4.md`) |
| 5 | 7 | Migration call-sites (LexemeMapper.kt, WordCardUseCaseImpl.kt, QuizGameImpl.kt, 8 existing test files; delete ComponentValueData в data_design_tree, не здесь) | ✅ done (см. `business_implement_pass5.md`) |

## Создано (Pass 1-2 cumulative)

### Domain types — `:modules:domain:lexeme`
- `Primitive.kt` (sealed: Text/Image/Color)
- `PrimitiveType.kt` (enum)
- `Field.kt`
- `TemplateValues.kt` (sealed: TextValues — MVP)
- `Scope.kt` (sealed: Global / PerDictionaries)
- `NameError.kt` (sealed: Empty/TooLong/SameScopeCollision/CrossScopeCollision)
- `AffectedQuizConfig.kt`
- `DeletionImpact.kt`
- `ComponentUsage.kt`
- `UserDefinedTypesSnapshot.kt`
- `PerDictionarySnapshot.kt`
- `CreateOutcome.kt` (sealed)
- `RenameOutcome.kt` (sealed)
- `DeleteOutcome.kt` (sealed)

### Data-API (`:core:core-db-api`)
- `DictionaryTypesSnapshot.kt`
- `UserDefinedTypesUsageSnapshot.kt`
- `ComponentOutcomeApiEntity.kt`

### UseCase impls
- `app/.../di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt` (узел #27)
- `app/.../di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt` (узел #28)
- Local `internal fun ComponentTypeApiEntity.toDomain()` extension в обоих файлах (обходит broken `LexemeMapper.kt` пока Pass 5 не migrate'нул).

### LogTags
- `modules/screen/components_manager/.../LogTags.kt` (узел #55)
- `modules/screen/per_dictionary_components/.../LogTags.kt` (узел #56)

### Pass 3 (CM Mate)
- `modules/screen/components_manager/.../mate/State.kt` (узел #29 + `toRows()` helper)
- `modules/screen/components_manager/.../mate/Msg.kt` (узел #30 + top-level UiMsg per F086)
- `modules/screen/components_manager/.../mate/UiEffect.kt` (sealed Snackbar)
- `modules/screen/components_manager/.../mate/UiEffectHandler.kt` (узел #31)
- `modules/screen/components_manager/.../mate/DatasourceEffect.kt` (узел #32)
- `modules/screen/components_manager/.../mate/DatasourceEffectHandler.kt` (узел #33)
- `modules/screen/components_manager/.../mate/AllUserDefinedTypesFlowHandler.kt` (узел #34)
- `modules/screen/components_manager/.../mate/NavigationEffect.kt` (узел #35)
- `modules/screen/components_manager/.../mate/NavigationEffectHandler.kt` (узел #36)
- `modules/screen/components_manager/.../mate/Reducer.kt` (узел #37 — `ComponentsManagerReducer`)

### Pass 3 — Modified
- `modules/screen/components_manager/.../ComponentsManagerViewModel.kt` (placeholder → real Mate wiring; +`onCleared()` dispose)
- `modules/screen/components_manager/build.gradle.kts` (+`:modules:core:logger`, testLibs.junit/mockk/coroutinesTest)

### Pass 4 (PerDictionary Mate)
- `modules/screen/per_dictionary_components/.../mate/State.kt` (узел #39 — `PerDictionaryComponentsScreenState` с `dictionaryId`/`dictionaryName` + `PerDictRow` + dialog states + `SnackbarState` + computed `isEmpty` + `internal fun PerDictionarySnapshot.toPerDictRows()`)
- `modules/screen/per_dictionary_components/.../mate/Msg.kt` (узел #40 — `ItemsLoaded/Failed` + Create/Rename/Delete семейства c epochId/typeId correlation + top-level `UiMsg : Msg` per F086)
- `modules/screen/per_dictionary_components/.../mate/UiEffect.kt` (sealed Snackbar)
- `modules/screen/per_dictionary_components/.../mate/UiEffectHandler.kt` (узел #41 — `UiEffect.Snackbar → UiMsg.Snackbar` без `show` per F128)
- `modules/screen/per_dictionary_components/.../mate/DatasourceEffect.kt` (узел #42 — Create/Rename/SoftDelete с `epochId`, `LoadImpact(typeId)`)
- `modules/screen/per_dictionary_components/.../mate/DatasourceEffectHandler.kt` (узел #43 — IO mapper + `CancellationException` re-throw F125 + epochId/typeId propagation)
- `modules/screen/per_dictionary_components/.../mate/ComponentsForDictionaryFlowHandler.kt` (узел #44 — `@AssistedInject(dictionaryId)`; подписывается на `flowComponentsForDictionary(dictionaryId)`)
- `modules/screen/per_dictionary_components/.../mate/NavigationEffect.kt` (узел #45 — sealed без variants, Back из base)
- `modules/screen/per_dictionary_components/.../mate/NavigationEffectHandler.kt` (узел #46 — `@AssistedInject`, `onScreenEffect` no-op)
- `modules/screen/per_dictionary_components/.../mate/Reducer.kt` (узел #47 — `PerDictionaryComponentsReducer`; preselect `Scope.PerDictionaries([state.dictionaryId])` в `OpenCreateDialog`; все critical fixes F123/F124/F127/F132/F136/F138/F140 включены с самого начала)

### Pass 4 — Modified
- `modules/screen/per_dictionary_components/.../PerDictionaryComponentsViewModel.kt` (узел #48 — placeholder → реальная Mate сборка с `@AssistedInject(dictionaryId)` + factory pattern для FlowHandler/NavigationEffectHandler + `onCleared() dispose`)

### Тесты (cumulative; counts уточнены iter 4 F120)
- `app/.../ComponentsManagerUseCaseImplTest.kt` (25 тестов)
- `app/.../PerDictionaryComponentsUseCaseImplTest.kt` (6 тестов)
- `modules/screen/components_manager/.../mate/ComponentsManagerReducerTest.kt` (68 тестов)
- `modules/screen/components_manager/.../mate/DatasourceEffectHandlerTest.kt` (14 тестов — 4 Effect branches × happy/error + CancellationException re-throw для CreateComponent/RenameComponent/LoadImpact/SoftDelete F125/F146)
- `modules/screen/components_manager/.../mate/AllUserDefinedTypesFlowHandlerTest.kt` (4 теста)
- `modules/screen/per_dictionary_components/.../mate/PerDictionaryComponentsReducerTest.kt` (62 теста — все critical fixes + scope preselect + toPerDictRows helper)
- `modules/screen/per_dictionary_components/.../mate/DatasourceEffectHandlerTest.kt` (14 тестов — 4 Effect branches × happy/error + CancellationException re-throw для CreateComponent/RenameComponent/LoadImpact/SoftDelete F125/F146)
- `modules/screen/per_dictionary_components/.../mate/ComponentsForDictionaryFlowHandlerTest.kt` (4 теста — initial emit / re-emit / throw / dictionaryId propagation)

### Pass 5 (migration call-sites M12 → M13)

**Migrated (business-side):**

- `app/.../polytrainer/mapper/LexemeMapper.kt` (узел #52) — `ComponentValueData.TextValue` casts → `TextValues`; `ComponentTypeApiEntity.toDomain()` пробрасывает `isMulti`/`createdAt`/`updatedAt`/`removedAt`.
- `app/.../di/module/wordCard/WordCardUseCaseImpl.kt` (узел #49) — 4× `ComponentValueData.TextValue(s)` → `TextValues(Primitive.Text(s))`; 5× `data: ComponentValueData` parameter → `TemplateValues`.
- `modules/screen/wordcard/.../deps/WordCardUseCase.kt` (узел #50) — interface contracts (5 методов) rebind.
- `modules/screen/wordcard/.../mate/DatasourceEffectHandler.kt` (узел #51) — 1× call-site.
- `modules/screen/quiz/chat/.../quiz/QuizGameImpl.kt` (узел #53) — 2× casts → `TextValues`; `LongTextValue` fallback dropped (F046).
- `modules/domain/lexeme/.../Lexeme.kt` (узел #54) — `@Deprecated` text refresh.
- `modules/domain/lexeme/.../ComponentValue.kt` — rebind `data: ComponentValueData` → `TemplateValues`.

**F117 cleanup:**

- `app/.../componentsmanager/ComponentsManagerUseCaseImpl.kt` — local `private fun toDomain()` extension удалён; canonical import `me.apomazkin.polytrainer.mapper.toDomain`.
- `app/.../perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt` — то же.

**F099 — test files mechanical rebind (8 files, не 6 как было в prompt — discovery during compile):**

- `app/.../WordCardUseCaseImplTest.kt` — `TextValues` + `createdAt`/`updatedAt` для api-entity helpers + `ComponentValueId` import cleanup.
- `app/.../LexemeMapperTest.kt` — `TextValues` + 1× `ImageValues` (was ImageValue test case C12) + test helpers update.
- `modules/screen/wordcard/.../mate/DatasourceEffectHandlerTest.kt` — `ComponentValueData → TemplateValues` в FakeUseCase signatures + `TextValues` assertion fix (`.value.value`).
- `modules/screen/wordcard/.../mate/WordLoadedTest.kt` — `createdAt`/`updatedAt` для 3 ComponentType helpers.
- `modules/screen/quiz/chat/.../QuizGameImplTest.kt` — `TextValues` + new fields.
- `modules/screen/quiz/chat/.../QuizGameImplFetchDataTest.kt` — то же.
- `modules/screen/quiz/chat/.../logic/ChatReducerTest.kt` — new fields + `import java.util.Date`.
- `modules/screen/quiz/chat/.../logic/DatasourceEffectHandlerTest.kt` — то же.
- `modules/screen/quiz/chat/.../logic/QuizPickerFlowHandlerTest.kt` — то же.
- `modules/domain/lexeme/.../LexemeBuiltInExtTest.kt` — `TextValues` + new fields.
- `modules/domain/lexeme/.../ComponentTypeRefExtTest.kt` — new fields + `import java.util.Date`.

## Модифицировано (Pass 1-2)

- `ComponentTemplate.kt`: drop LONG_TEXT, fromKey → nullable, +fields.
- `ComponentType.kt`: +isMulti/createdAt/updatedAt, removeDate → removedAt.
- `ComponentTypeApiEntity.kt`: +isMulti/createdAt/updatedAt, removeDate → removedAt.
- `ComponentValueApiEntity.kt`: rebind на TemplateValues.
- `CoreDbApi.kt`: 5 BREAKING сигнатур + 6 NEW методов в `LexemeApi`.
- `ComponentsManagerUseCase.kt` / `PerDictionaryComponentsUseCase.kt`: placeholder → 5 методов.
- `modules/screen/components_manager/build.gradle.kts` / `per_dictionary_components/build.gradle.kts`: +dependency `:modules:domain:lexeme`.

## Build/Test состояние

- `:modules:domain:lexeme:test` — **PASS** (Pass 5: domain tests after migration).
- `:modules:screen:components_manager:testDebugUnitTest` — **PASS** (Pass 3 + Pass 5 stability: 69 тестов).
- `:modules:screen:per_dictionary_components:testDebugUnitTest` — **PASS** (Pass 4 + Pass 5 stability: 77 тестов).
- `:modules:screen:wordcard:testDebugUnitTest` — **PASS** (after Pass 5 migration + helper fixes).
- `:modules:screen:quiz:chat:testDebugUnitTest` — **PASS** (after Pass 5 migration + helper fixes).
- `:app:testDebugUnitTest` — **не запускался** (зависит от `:core:core-db-impl` который сломан Pass 1 breaking changes).
- ComponentsManagerUseCaseImplTest / PerDictionaryComponentsUseCaseImplTest — **не запускались** (нет успешного build chain до них; находятся в `:app` source set).

Это **by design** — `:core:core-db-impl` будет восстановлен data_implement (data-layer impl новых API методов).

## Нетривиальные решения

1. **Pass split** — нарушение runner.md, признано в FlowBacklog (IS481cc-F7). Conductor решил продолжать passes для прогресса.
2. **Local `toDomain()` extension** в UseCase impls обходит broken `LexemeMapper.kt` (узел #52 будет fixed в Pass 5).
3. **`resetQuizPickerPrefsBestEffort()`** в `softDeleteComponent` — try/catch wrap для F103 orphan prefs (DeleteOutcome.Success даже если prefs reset бросил, с warning log).
4. **`CreateOutcome.NameTooLong`** validation НЕ имплементирован — `maxLen` policy не зафиксирован в contract (отложено в UI sub-flow).
5. **UseCase impl locations**: `app/.../di/module/...` (по design_tree + existing convention с 11 другими UseCaseImpl), не `modules/screen/.../deps/` как ошибочно предложила Pass 2 prompt инструкция.

## Известные TODO

- **data_implement** (отдельный sub-flow): implementation новых LexemeApi методов в `:core:core-db-impl` (CoreDbApiImpl, SeedBuiltIns, Migration_012_to_013, и т.д.), plus delete `ComponentValueData.kt` (domain узел #6 перенесён в data_design_tree как финальный).

## История ревью

### iter 4 cleanup

Применены следующие findings:

- **F146** — CancellationException test parity. Добавлены 2 теста в каждый `DatasourceEffectHandlerTest.kt` (CM Mate + PerDict Mate) — `LoadImpact` и `SoftDeleteComponent` ветки. Подтверждено что existing impls (catch CancellationException → re-throw) уже корректны; тесты закрывают coverage gap. Counts: 12 → 14 в обоих модулях.
- **F120** — test counts в § «Тесты» приведены к фактическим значениям (`@Test` annotations re-grep'нуты по 8 test files). Diff: CM UseCase 21→25, CM Reducer 55→68, CM Datasource 10→14, PerDict Reducer 61→62, PerDict Datasource 12→14.

Verify: `./scripts/cc-build.sh :modules:screen:components_manager:testDebugUnitTest` и `:modules:screen:per_dictionary_components:testDebugUnitTest` — оба зелёные.

После iter 4 раздел «Известные TODO» очищен — остаётся только **data_implement** sub-flow как единственный известный pending пункт.
