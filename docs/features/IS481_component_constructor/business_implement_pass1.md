# business_implement — Pass 1 (Tier 0-3)

Scope: domain types + data-API расширение + UseCase interfaces. Без impl, без ViewModel/Reducer/Handler, без тестов, без migration call-sites.

## Создано

### Tier 0 — domain core M13 (`modules/domain/lexeme`)
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Primitive.kt` — sealed interface (`Text` / `Image` / `Color`).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/PrimitiveType.kt` — enum (`TEXT` / `IMAGE` / `COLOR`).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Field.kt` — data class `(name, type)`.
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/TemplateValues.kt` — sealed interface + `TextValues` + `ImageValues` (MVP).

### Tier 1 — domain shared types (`me.apomazkin.lexeme`)
- `Scope.kt` — sealed (`Global` / `PerDictionaries(ids)`).
- `NameError.kt` — sealed (`Empty` / `TooLong` / `SameScopeCollision` / `CrossScopeCollision`).
- `AffectedQuizConfig.kt` — data class `(dictionaryId, quizMode)`.
- `DeletionImpact.kt` — data class `(valueCount, dictionariesWithValues, affectedQuizConfigs, affectedPrefs)`.
- `ComponentUsage.kt` — data class `(valueCountByType, dictionaryIdsByType, dictionaryNames)`.
- `UserDefinedTypesSnapshot.kt` — data class `(types, usage)`.
- `PerDictionarySnapshot.kt` — data class `(dictionaryId, dictionaryName, types, valueCountByType)`.
- `CreateOutcome.kt` — sealed (`Success(List<ComponentType>)` / `NameEmpty` / `NameTooLong` / `SameScopeCollision` / `CrossScopeCollision` / `Failure(Throwable)`).
- `RenameOutcome.kt` — sealed (`Success(ComponentType)` / `NameEmpty` / `SameScopeCollision` / `CrossScopeCollision` / `BuiltInProtected` / `Failure`).
- `DeleteOutcome.kt` — sealed (`Success(DeletionImpact)` / `BuiltInProtected` / `Failure`).

### Tier 2 — data-API (`core/core-db-api`)
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/DictionaryTypesSnapshot.kt`.
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/UserDefinedTypesUsageSnapshot.kt`.
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentOutcomeApiEntity.kt` — `CreateComponentOutcome` / `RenameComponentOutcome` / `SoftDeleteComponentOutcome` + `typealias PreviewDeletionOutcome = DeletionImpact?`.

## Модифицировано

### Tier 0
- `modules/domain/lexeme/.../ComponentTemplate.kt`
  - Drop `LONG_TEXT` entry (consolidation `long_text → text` на M13 data migration F046).
  - `fromKey(key): ComponentTemplate?` теперь nullable (fail-soft парсинг, unknown key → null + Crashlytics).
  - Добавлен computed `fields: List<Field>` (schema-in-code).
- `modules/domain/lexeme/.../ComponentType.kt`
  - Добавлены поля `isMulti: Boolean = false`, `createdAt: Date`, `updatedAt: Date`.
  - Rename `removeDate → removedAt`.

### Tier 2
- `core/core-db-api/.../entity/ComponentTypeApiEntity.kt`
  - Добавлены `isMulti`, `createdAt`, `updatedAt`; rename `removeDate → removedAt`.
- `core/core-db-api/.../entity/ComponentValueApiEntity.kt`
  - Rebind `data: ComponentValueData → data: TemplateValues`.
  - Добавлены `createdAt`, `updatedAt`, `removedAt`.
- `core/core-db-api/.../CoreDbApi.kt`
  - 5 BREAKING сигнатур меняют `ComponentValueData → TemplateValues`:
    `addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent` / `addLexemeWithComponents` / `addComponentValue` / `updateComponentValue`.
  - 6 NEW методов: `flowAllUserDefinedTypesWithUsage` / `flowUserDefinedTypesForDictionary` /
    `createUserDefinedComponent` / `renameComponentType` / `previewDeletionImpact` / `softDeleteComponentType`.
  - Imports: dropped `ComponentValueData`; added `TemplateValues`, `ComponentTemplate`, `Scope`, `DeletionImpact`, новые snapshot/outcome data-entity.

### Tier 3
- `modules/screen/components_manager/.../deps/ComponentsManagerUseCase.kt` — placeholder заменён на полный interface с 5 методами.
- `modules/screen/per_dictionary_components/.../deps/PerDictionaryComponentsUseCase.kt` — placeholder заменён на полный interface с 5 методами (один отличается — `flowComponentsForDictionary(dictionaryId)`).
- `modules/screen/components_manager/build.gradle.kts` — добавлена зависимость `implementation(project("path" to ":modules:domain:lexeme"))`.
- `modules/screen/per_dictionary_components/build.gradle.kts` — добавлена зависимость `implementation(project("path" to ":modules:domain:lexeme"))`.

## Нетривиальные решения

1. **`ComponentValueData.kt` оставлен существовать.** Per pass-rules: delete перенесён в data_design_tree как финальный узел; `ComponentValue.kt` (узел #8) НЕ перебинден на `TemplateValues` в Pass 1 — он останется на `ComponentValueData` до Pass 5 migration (там же будут перебинды call-sites). Иначе на этом pass ComponentValue не скомпилируется, т.к. ComponentValueData ещё используется в DAO/mapper.

2. **`CreateOutcome.NameTooLong` добавлен** — pass instructions явно перечислили его в списке variants, хотя контракт §16 показывал минимальный набор (без TooLong). Соответствует `NameError.TooLong`.

3. **`PreviewDeletionOutcome` оформлен как `typealias DeletionImpact?`** (не sealed). Read-only preview не имеет typed-ошибок — либо тип найден (DeletionImpact), либо `null`.

4. **Зависимость `:modules:domain:lexeme`** добавлена в build.gradle двух новых screen-модулей — без неё UseCase interfaces не скомпилируются. Это de-facto изменение infra-граней; infra_implement создал placeholder без lexeme dependency, т.к. placeholder был пустым interface.

## Известная нестыковка компиляции (ожидаемо)

После Pass 1 код **не собирается** — это by design:
- `core-db-impl` mapper (`ComponentTypeDb.kt`) использует `removeDate` (стало `removedAt`) и `ComponentValueData` (стало `TemplateValues`).
- `WordCardUseCaseImpl`, `LexemeMapper`, `QuizGameImpl` и тесты используют 5 переименованных сигнатур + `removeDate`.
- `ComponentValue.kt` всё ещё `data: ComponentValueData` — domain compiles, но wordcard / quiz call-sites нет.

Это покрывается следующими passes (см. follow-ups). build/lint в Pass 1 НЕ запускался — по правилам шага.

## Известные follow-ups для Pass 2-5

- **Pass 2:** UseCase impls (`ComponentsManagerUseCaseImpl.kt`, `PerDictionaryComponentsUseCaseImpl.kt`) + их тесты. Включает создание `LogTags.kt` для обоих screen-модулей (узлы #55/#56). Маппер `ComponentTypeApiEntity.toDomain()` финализируется (Pass 5 migration перебиндит и LexemeMapper).
- **Pass 3:** ComponentsManager Mate (State/Msg/Effect/UiMsg/Reducer/FlowHandler/DatasourceEffectHandler/ViewModel/NavigationEffect) + тесты.
- **Pass 4:** PerDictionaryComponents Mate (аналогично) + тесты.
- **Pass 5:** Migration call-sites M12→M13:
  - `ComponentValue.kt` rebind `data: ComponentValueData → TemplateValues` (узел #8 design tree).
  - `WordCardUseCaseImpl.kt`, `WordCardUseCase.kt`, `wordcard/DatasourceEffectHandler.kt` — 5 BREAKING signatures.
  - `LexemeMapper.kt` — toDomain маппинги (`removeDate → removedAt`, `ComponentValueData → TemplateValues`).
  - `QuizGameImpl.kt` — ComponentValueData usages.
  - `Lexeme.kt` — если требуется по design_tree #54.
  - Тесты: `WordCardUseCaseImplTest`, `QuizChatUseCaseImplTest`, `LexemeMapperTest`, `LexemeBuiltInExtTest`, `QuizGameImplFetchDataTest`, `QuizGameImplTest`, `wordcard/mate/DatasourceEffectHandlerTest`.
  - Core-db-impl: `ComponentTypeDb.kt`, `ComponentValueDb.kt`, `ComponentValueWithType.kt`, `ComponentValueDataJson.kt`, `CoreDbApiImpl.kt` — это data side, формально в data_implement (data_design_tree).
  - **Финальный узел:** delete `ComponentValueData.kt` — после того как все call-sites переехали.

(Этот pass1.md — временный tracking; финальный business_implement.md создаст Pass 5.)
