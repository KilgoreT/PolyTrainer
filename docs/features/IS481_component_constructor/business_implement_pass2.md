# Pass 2 (Tier 4 UseCase impls + LogTags + tests)

Scope: UseCase impls (#27 ComponentsManagerUseCaseImpl + #28 PerDictionaryComponentsUseCaseImpl) + LogTags (#55/#56) + их unit-тесты (~27 тестов по business_test.md § 1-2).

## Возражение по path'ам

Pass 2 spec в инструкциях указал `modules/screen/<feature>/src/main/.../deps/<Feature>UseCaseImpl.kt`, что противоречит:

1. `business_design_tree.md` узлы #27, #28 — указывают `app/src/main/java/me/apomazkin/polytrainer/di/module/<feature>/<Feature>UseCaseImpl.kt`.
2. Существующей конвенции: все 11 `*UseCaseImpl` живут в `app/.../di/module/...`.
3. Clean dependency rule: UseCaseImpl зависит от `CoreDbApi` + mapper'ов из `app/.../mapper/...` — screen-модуль не должен видеть data-API напрямую.

**Решение:** использовать пути из design_tree (`app/.../di/module/...`). Infra-placeholder там уже создан (`infra_implement`).

## Создано

### Tier 0 independent — LogTags
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/LogTags.kt` — `object LogTags { const val COMPONENTS_MANAGER = "ComponentsManager" }`.
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/LogTags.kt` — `object LogTags { const val PER_DICT_COMPONENTS = "PerDictComponents" }`.

### Tests
- `app/src/test/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImplTest.kt` — 21 теста (по спеке business_test § 1.1-1.5).
- `app/src/test/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImplTest.kt` — 6 тестов (по спеке § 2.1-2.2).

## Модифицировано

### Tier 4 — UseCase impls (placeholder → full)

- `app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt`
  - Ctor deps: `lexemeApi: CoreDbApi.LexemeApi`, `dictionaryApi: CoreDbApi.DictionaryApi`, `prefsProvider: PrefsProvider`, `logger: LexemeLogger` — точно как в design tree #27.
  - 5 методов реализованы: `flowAllUserDefinedTypes` (map data API → domain snapshot), `createUserDefinedComponent` (pre-validation NameEmpty → API call → outcome map), `renameComponent` (analogous), `previewDeletionImpact` (pass-through с try/catch → null), `softDeleteComponent` (DB-soft-delete + best-effort prefs cleanup).
  - Локальный internal helper `ComponentTypeApiEntity.toDomain()` — НЕ из глобального `LexemeMapper.kt` (avoid Pass 5 dependency, см. § «Нетривиальные решения» ниже).

- `app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt`
  - Ctor deps: `lexemeApi: CoreDbApi.LexemeApi`, `sharedCrud: ComponentsManagerUseCaseImpl` — точно как в design tree #28.
  - `flowComponentsForDictionary` — собственный (scoped query), импортирует `toDomain()` из componentsmanager package.
  - 4 write-метода — delegate на `sharedCrud.<method>` (Open Q #2 решение).

## Тесты pass / fail

**Не запускались** — блокировано Pass 5 + data_implement.

**Причина:** `:app:testDebugUnitTest` транзитивно требует компиляции `:core:core-db-impl`, который в текущем состоянии не собирается:
- `ComponentTypeDb.kt:58` — `removeDate` parameter renamed → `removedAt`, `createdAt/updatedAt` missing.
- `ComponentValueWithType.kt:27` — `data: ComponentValueData` ≠ `data: TemplateValues`, missing `createdAt/updatedAt`.
- `ComponentValueDataJson.kt:41` — `ComponentTemplate.LONG_TEXT` removed in M13.
- `CoreDbApiImpl.kt:209,237,253,276,308,322` — 5 BREAKING сигнатур `LexemeApi` (Pass 1 Tier 2) не имеют реализации в data-impl.

Все эти ошибки — work для следующих passes (Pass 5 для wordcard/quiz/mapper migration + data_design_tree / data_implement для core-db-impl). Pass 2 ожидаемо не пересекается с data side per design tree.

**Что собирается:**
- `:modules:screen:components_manager:compileDebugKotlin` — PASS (UseCase interface from Pass 1 + LogTags from Pass 2).
- `:modules:screen:per_dictionary_components:compileDebugKotlin` — PASS.
- `:app:compileDebugKotlin` — FAIL only because of core-db-impl transitive (impls сами синтаксически корректны).

**Тесты будут запущены:** на Pass 5 завершении (либо отдельно если data_implement пройдёт раньше). Pass 5 owns wiring up wordcard/quiz call-sites + LexemeMapper переход + удаление `ComponentValueData.kt`. После этого `:app:testDebugUnitTest` корректно соберётся.

## Нетривиальные решения

1. **Локальный `ComponentTypeApiEntity.toDomain()` в componentsmanager package** (вместо global `app/.../mapper/LexemeMapper.kt`).
   `LexemeMapper.kt` сейчас broken (line 31: `removeDate = removeDate` против переименованного `removedAt`; line 34-39: `ComponentValueApiEntity.data: TemplateValues` ≠ `ComponentValue.data: ComponentValueData`). Pass 5 owns LexemeMapper migration. Локальный `internal fun ComponentTypeApiEntity.toDomain()` в `ComponentsManagerUseCaseImpl.kt` развязывает Pass 2 от LexemeMapper migration.
   `PerDictionaryComponentsUseCaseImpl` импортирует `toDomain` из componentsmanager package — это допустимо в рамках одного app/di/module/ слоя; альтернатива — выделить в shared mapper-файл, но это over-engineering на 1-функцию.

2. **F103 best-effort prefs reset** (узел #27).
   `resetQuizPickerPrefsBestEffort()` оборачивает каждый `prefsProvider.setStringByRawKey()` в try/catch — failure логируется через `logger.w()` но НЕ propagate. Outcome остаётся `DeleteOutcome.Success(impact)` даже если все prefs writes упали. Обоснование: DB soft-delete уже committed, orphan pref recoverable (next picker open перерендерит state).

3. **`PerDictionaryComponentsUseCaseImpl(sharedCrud: ComponentsManagerUseCaseImpl)`** (а не interface).
   Design tree #28 явно указывает concrete тип; Dagger автогенерирует Provider для concrete @Inject constructor. Этого достаточно — нет необходимости в self-binding `@Binds`. Если же expose required, можно добавить позже в `ComponentsManagerModule`.

4. **`CreateOutcome.NameTooLong` НЕ обрабатывается** в реализации `createUserDefinedComponent` — спека Pass 2 указала на проверку `name.isBlank() → NameEmpty` и `> maxLen → NameTooLong`, но business_contract нигде не определяет `maxLen` policy. `NameError.TooLong` существует в Pass 1 как «UI-policy reserve» (см. business_design_tree #10). Без определённого `maxLen` impl бы вынужден был guess (например, 64), что нарушает single-source-of-truth. Оставлено для будущей итерации (UI sub-flow зафиксирует policy).

## Известные follow-ups для Pass 3-5

- **Pass 3:** ComponentsManager Mate (State/Msg/Effect/UiMsg/Reducer/FlowHandler/DatasourceEffectHandler/ViewModel) + тесты (узлы #29-#38, #55).
- **Pass 4:** PerDictionaryComponents Mate (узлы #39-#48, #56).
- **Pass 5:** Migration call-sites M12 → M13 (Tier 7, узлы #49-#54) — после этого app собирается + Pass 2 тесты runnable.
- **Data sub-flow** (`data_design_tree` + `data_implement`) — DAO/mappers/migrations для M13 + impl of 6 NEW LexemeApi методов; параллельно с Pass 5.
