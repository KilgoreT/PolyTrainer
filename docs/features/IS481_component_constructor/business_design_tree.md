# Business design tree: IS481 component_constructor

DAG-граф бизнес-слоя для IS481. Покрывает: M13 переход в `:modules:domain:lexeme` (`TemplateValues` / `Primitive` / `Field` / новый `ComponentTemplate`/`ComponentType`), доменные snapshot/outcome типы для двух новых экранов, расширение `CoreDbApi.LexemeApi` сигнатурами, два UseCase интерфейса/реализации, две Mate-обвязки (State/Msg/Effect/UiEffect/Reducer/FlowHandler/DatasourceEffectHandler/ViewModel) для `ComponentsManagerScreen` и `PerDictionaryComponentsScreen`, миграцию call-site'ов M12→M13 `ComponentValueData → TemplateValues` (wordcard / quiz chat / app DI / data mappers — только в части domain-shape, без UI).

## Часть 1: Граф

```yaml
# ============================================================
# Tier 0: Domain — primitives / fields / templates (`:modules:domain:lexeme`)
# ============================================================
- id: 1
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Primitive.kt
  action: "+"
  depends: []

- id: 2
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/PrimitiveType.kt
  action: "+"
  depends: []

- id: 3
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Field.kt
  action: "+"
  depends: [2]

- id: 4
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTemplate.kt
  action: "~"
  depends: [2, 3]

- id: 5
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/TemplateValues.kt
  action: "+"
  depends: [1]

# id: 6 (delete ComponentValueData.kt) перенесён в data_design_tree
# как финальный узел после всех data-side migrations. См. секцию
# "## Перенос в data_design_tree" ниже.

- id: 7
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentType.kt
  action: "~"
  depends: [4]

- id: 8
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValue.kt
  action: "~"
  depends: [5, 7]

# ============================================================
# Tier 1: Domain — shared types для двух экранов конструктора
# ============================================================
- id: 9
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Scope.kt
  action: "+"
  depends: []

- id: 10
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/NameError.kt
  action: "+"
  depends: []

- id: 11
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/AffectedQuizConfig.kt
  action: "+"
  depends: []

- id: 12
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/DeletionImpact.kt
  action: "+"
  depends: [11]

- id: 13
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentUsage.kt
  action: "+"
  depends: [7]

- id: 14
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/UserDefinedTypesSnapshot.kt
  action: "+"
  depends: [7, 13]

- id: 15
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/PerDictionarySnapshot.kt
  action: "+"
  depends: [7]

- id: 16
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/CreateOutcome.kt
  action: "+"
  depends: [7]

- id: 17
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/RenameOutcome.kt
  action: "+"
  depends: [7]

- id: 18
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/DeleteOutcome.kt
  action: "+"
  depends: [12]

# ============================================================
# Tier 2: data-API contract (`CoreDbApi.LexemeApi`) — расширение
# ============================================================
- id: 19
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentTypeApiEntity.kt
  action: "~"
  depends: [4]

- id: 20
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentValueApiEntity.kt
  action: "~"
  depends: [5, 19]

- id: 21
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/DictionaryTypesSnapshot.kt
  action: "+"
  depends: [19]

- id: 22
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/UserDefinedTypesUsageSnapshot.kt
  action: "+"
  depends: [19]

- id: 23
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentOutcomeApiEntity.kt
  action: "+"
  depends: [12, 19]

- id: 24
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt
  action: "~"
  depends: [4, 5, 9, 12, 19, 20, 21, 22, 23]

# ============================================================
# Tier 3: UseCase interfaces в screen-модулях (заполнение infra-placeholder)
# ============================================================
- id: 25
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/deps/ComponentsManagerUseCase.kt
  action: "~"
  depends: [4, 9, 12, 14, 16, 17, 18]

- id: 26
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/deps/PerDictionaryComponentsUseCase.kt
  action: "~"
  depends: [4, 9, 12, 15, 16, 17, 18]

# ============================================================
# Tier 4: UseCase impls (app/di/module/...)
# ============================================================
- id: 27
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt
  action: "~"
  depends: [4, 9, 12, 13, 14, 16, 17, 18, 23, 24, 25, 52, 55]

- id: 28
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt
  action: "~"
  depends: [4, 9, 12, 15, 16, 17, 18, 24, 26, 27, 52]

# ============================================================
# Tier 5: ComponentsManagerScreen — Mate (logic/)
# ============================================================
- id: 29
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/logic/State.kt
  action: "+"
  depends: [4, 7, 9, 10, 12, 14]

- id: 30
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/logic/Message.kt
  action: "+"
  depends: [4, 9, 12, 14, 16, 17, 18]

- id: 31
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/logic/UiEffectHandler.kt
  action: "+"
  depends: [30]

- id: 32
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/logic/DatasourceEffect.kt
  action: "+"
  depends: [4, 9]

- id: 33
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/logic/DatasourceEffectHandler.kt
  action: "+"
  depends: [16, 17, 18, 25, 30, 32, 55]

- id: 34
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/logic/AllUserDefinedTypesFlowHandler.kt
  action: "+"
  depends: [25, 30, 55]

- id: 35
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerNavigationEffect.kt
  action: "+"
  depends: []

- id: 36
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerNavigationEffectHandler.kt
  action: "+"
  depends: [30, 35]

- id: 37
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/logic/ComponentsManagerReducer.kt
  action: "+"
  depends: [10, 16, 17, 18, 29, 30, 31, 32]

- id: 38
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerViewModel.kt
  action: "+"
  depends: [29, 30, 31, 33, 34, 36, 37]

# ============================================================
# Tier 6: PerDictionaryComponentsScreen — Mate (logic/)
# ============================================================
- id: 39
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/logic/State.kt
  action: "+"
  depends: [4, 7, 9, 10, 12, 15]

- id: 40
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/logic/Message.kt
  action: "+"
  depends: [4, 9, 12, 15, 16, 17, 18]

- id: 41
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/logic/UiEffectHandler.kt
  action: "+"
  depends: [40]

- id: 42
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/logic/DatasourceEffect.kt
  action: "+"
  depends: [4, 9]

- id: 43
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/logic/DatasourceEffectHandler.kt
  action: "+"
  depends: [16, 17, 18, 26, 40, 42, 56]

- id: 44
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/logic/ComponentsForDictionaryFlowHandler.kt
  action: "+"
  depends: [26, 40, 56]

- id: 45
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsNavigationEffect.kt
  action: "+"
  depends: []

- id: 46
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsNavigationEffectHandler.kt
  action: "+"
  depends: [40, 45]

- id: 47
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/logic/PerDictionaryComponentsReducer.kt
  action: "+"
  depends: [9, 10, 16, 17, 18, 39, 40, 41, 42]

- id: 48
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsViewModel.kt
  action: "+"
  depends: [39, 40, 41, 43, 44, 46, 47]

# ============================================================
# Tier 7: Migration call-sites M12 → M13 (`ComponentValueData → TemplateValues`)
# ============================================================
- id: 49
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt
  action: "~"
  depends: [1, 5, 20, 24]

- id: 50
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt
  action: "~"
  depends: [5]

- id: 51
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/DatasourceEffectHandler.kt
  action: "~"
  depends: [1, 5, 50]

- id: 52
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt
  action: "~"
  depends: [1, 5, 7, 19, 20]

- id: 53
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt
  action: "~"
  depends: [5]

- id: 54
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt
  action: "~"
  depends: [5, 8]

# ============================================================
# Tier 0 (independent): LogTags per module (используются в Tier 4 + Tier 5/6)
# ============================================================
- id: 55
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/LogTags.kt
  action: "+"
  depends: []

- id: 56
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/LogTags.kt
  action: "+"
  depends: []
```

## Часть 2: Детали изменений

### Tier 0: Domain — primitives / fields / templates (M13 переход)

#### #1 Primitive.kt [+]

Sealed-иерархия для типизированных значений примитивов. Variants: `Text(value: String)`, `Image(uri: String)`, `Color(hex: String)`. См. `business_contract_spec.md` § Primitive.

```kotlin
package me.apomazkin.lexeme

sealed interface Primitive {
    data class Text(val value: String) : Primitive
    data class Image(val uri: String) : Primitive
    data class Color(val hex: String) : Primitive
}
```

#### #2 PrimitiveType.kt [+]

```kotlin
package me.apomazkin.lexeme

enum class PrimitiveType { TEXT, IMAGE, COLOR }
```

#### #3 Field.kt [+]

```kotlin
package me.apomazkin.lexeme

data class Field(
    val name: String,
    val type: PrimitiveType,
)
```

#### #4 ComponentTemplate.kt [~]

**Было** (`modules/domain/lexeme/.../ComponentTemplate.kt:7-16`):
```kotlin
enum class ComponentTemplate(val key: String) {
    TEXT("text"),
    LONG_TEXT("long_text"),
    IMAGE("image");

    companion object {
        fun fromKey(key: String): ComponentTemplate = entries.firstOrNull { it.key == key } ?: TEXT
    }
}
```

**Стало:**
```kotlin
enum class ComponentTemplate(val key: String) {
    TEXT("text"),
    IMAGE("image"),
    // composite — добавляются в будущих фичах
    ;

    val fields: List<Field>
        get() = when (this) {
            TEXT -> listOf(Field("value", PrimitiveType.TEXT))
            IMAGE -> listOf(Field("value", PrimitiveType.IMAGE))
        }

    companion object {
        /**
         * Fail-soft парсинг: unknown key → null + caller логирует в Crashlytics
         * (`forward_compat_unknown`).
         */
        fun fromKey(key: String): ComponentTemplate? =
            entries.firstOrNull { it.key == key }
    }
}
```

Изменения: drop `LONG_TEXT` (template-key consolidation `long_text → text` на M13 миграции данных, F046); `fromKey` → nullable; новый computed `fields`. Compile-time gate через exhaustive `when` на call-sites парсера (data) и template-resolver'а (UI sub-flow).

#### #5 TemplateValues.kt [+]

Sealed-замена `ComponentValueData`. Per-template variant; compile-time exhaustive `when` на read/write path. MVP — `TextValues` + `ImageValues`.

```kotlin
package me.apomazkin.lexeme

sealed interface TemplateValues

data class TextValues(
    val value: Primitive.Text,
) : TemplateValues

data class ImageValues(
    val value: Primitive.Image,
) : TemplateValues

// Composite values (QuoteWithSourceValues / ImageWithCaptionValues / etc.) —
// будущие фичи; добавляются как новые data class : TemplateValues.
```

#### #6 — перенесён в data_design_tree

Узел «delete `ComponentValueData.kt`» перенесён в `data_design_tree` как финальный узел (после всех data-side migrations). business_design_tree.md в Tier 7 содержит только business-side migrations (#49-#54); data-side обработает удаление символа.

#### #7 ComponentType.kt [~]

**Было** (`modules/domain/lexeme/.../ComponentType.kt:16-24`):
```kotlin
data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val removeDate: Date? = null,
)
```

**Стало:**
```kotlin
data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val isMulti: Boolean = false,         // NEW (M13)
    val createdAt: Date,                  // NEW (M13)
    val updatedAt: Date,                  // NEW (M13)
    val removedAt: Date? = null,          // RENAME removeDate → removedAt (M13)
)
```

Все call-sites доменной модели (`ComponentTypeApiEntity.toDomain` / `ComponentType.toRef` / тесты) обязаны компилироваться после rename. Существующий extension `ComponentType.toRef()` (`ComponentTypeRef.kt:32-35`) не трогается — он по `systemKey` ветвится.

#### #8 ComponentValue.kt [~]

**Было** (`modules/domain/lexeme/.../ComponentValue.kt:11-16`):
```kotlin
data class ComponentValue(
    val id: ComponentValueId,
    val lexemeId: LexemeId,
    val type: ComponentType,
    val data: ComponentValueData,
)
```

**Стало:** rebind `data: ComponentValueData → data: TemplateValues`. Сигнатура и все остальные поля без изменений.

### Tier 1: Domain — shared types для двух экранов

#### #9 Scope.kt [+]

```kotlin
package me.apomazkin.lexeme

sealed interface Scope {
    data object Global : Scope                                  // dictionaryId IS NULL
    data class PerDictionaries(val ids: List<Long>) : Scope     // 1+ rows
}
```

#### #10 NameError.kt [+]

```kotlin
package me.apomazkin.lexeme

sealed interface NameError {
    data object Empty : NameError                               // name.isBlank()
    data object TooLong : NameError                             // зарезерв; UI-policy
    data object SameScopeCollision : NameError                  // active row в том же scope
    data object CrossScopeCollision : NameError                 // global ⊥ per-dict invariant (F039)
}
```

#### #11 AffectedQuizConfig.kt [+]

```kotlin
package me.apomazkin.lexeme

/**
 * Подзапись `DeletionImpact.affectedQuizConfigs`. Каждый элемент — quiz_config row
 * у которого `component_refs` ссылается на удаляемый ComponentType.
 *
 * `quizMode` хранится как String (data-convention, F078); enum может появиться позже.
 */
data class AffectedQuizConfig(
    val dictionaryId: Long,
    val quizMode: String,
)
```

#### #12 DeletionImpact.kt [+]

```kotlin
package me.apomazkin.lexeme

/**
 * Preview каскадного soft-delete user-defined component_type.
 *
 * `affectedQuizConfigs` — `quiz_configs.component_refs` ссылается на удаляемый
 *  тип; будут вычищены в одной транзакции с soft-delete.
 * `affectedPrefs` — `quiz_picker_dict_<id>` pref ссылается на удаляемый ref;
 *  сбрасывается after soft-delete (UseCase composition, prefs в DataStore вне Room).
 */
data class DeletionImpact(
    val valueCount: Int,
    val dictionariesWithValues: List<Long>,
    val affectedQuizConfigs: List<AffectedQuizConfig>,
    val affectedPrefs: List<Long>,
)
```

#### #13 ComponentUsage.kt [+]

```kotlin
package me.apomazkin.lexeme

/**
 * Aggregated usage — `flowAllUserDefinedTypesWithUsage()` собирает в одном snapshot,
 * чтобы reducer не делал N+1 запросов по типу.
 */
data class ComponentUsage(
    val valueCountByType: Map<ComponentTypeId, Int>,
    val dictionaryIdsByType: Map<ComponentTypeId, Set<Long>>,
    val dictionaryNames: Map<Long, String>,
)
```

#### #14 UserDefinedTypesSnapshot.kt [+]

```kotlin
package me.apomazkin.lexeme

/**
 * Snapshot для aggregated view. Dedicated data class устраняет неоднозначность
 * `.first/.second` в reducer (F1 iter1 review).
 */
data class UserDefinedTypesSnapshot(
    val types: List<ComponentType>,
    val usage: ComponentUsage,
)
```

#### #15 PerDictionarySnapshot.kt [+]

```kotlin
package me.apomazkin.lexeme

data class PerDictionarySnapshot(
    val dictionaryId: Long,
    val dictionaryName: String,
    val types: List<ComponentType>,
    val valueCountByType: Map<ComponentTypeId, Int>,
)
```

#### #16 CreateOutcome.kt [+]

```kotlin
package me.apomazkin.lexeme

sealed interface CreateOutcome {
    /** N rows: 1 для Global/single per-dict, N для PerDictionaries(N). */
    data class Success(val created: List<ComponentType>) : CreateOutcome
    data object NameEmpty : CreateOutcome
    data object SameScopeCollision : CreateOutcome
    data object CrossScopeCollision : CreateOutcome
    data class Failure(val cause: Throwable) : CreateOutcome
}
```

#### #17 RenameOutcome.kt [+]

```kotlin
package me.apomazkin.lexeme

sealed interface RenameOutcome {
    data class Success(val type: ComponentType) : RenameOutcome
    data object NameEmpty : RenameOutcome
    data object SameScopeCollision : RenameOutcome
    data object CrossScopeCollision : RenameOutcome
    data object BuiltInProtected : RenameOutcome
    data class Failure(val cause: Throwable) : RenameOutcome
}
```

#### #18 DeleteOutcome.kt [+]

```kotlin
package me.apomazkin.lexeme

sealed interface DeleteOutcome {
    data class Success(val impact: DeletionImpact) : DeleteOutcome
    data object BuiltInProtected : DeleteOutcome
    data class Failure(val cause: Throwable) : DeleteOutcome
}
```

### Tier 2: data-API contract расширение

> Реализация (DAO, mappers, миграции) — `data_design_tree`. Здесь — только сигнатуры в API, чтобы UseCase impl мог компилироваться.

#### #19 ComponentTypeApiEntity.kt [~]

**Было** (`core-db-api/.../ComponentTypeApiEntity.kt:13-21`):
```kotlin
data class ComponentTypeApiEntity(
    val id: Long,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val removeDate: Date? = null,
)
```

**Стало:**
```kotlin
data class ComponentTypeApiEntity(
    val id: Long,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val isMulti: Boolean = false,
    val createdAt: Date,
    val updatedAt: Date,
    val removedAt: Date? = null,
)
```

Note (F019): nullability `template` после nullable `fromKey` финализируется в data: `toApiEntity()` skip row при unknown template + Crashlytics; api-entity остаётся non-nullable.

#### #20 ComponentValueApiEntity.kt [~]

**Было** (`core-db-api/.../ComponentValueApiEntity.kt:11-16`):
```kotlin
data class ComponentValueApiEntity(
    val id: Long,
    val lexemeId: Long,
    val type: ComponentTypeApiEntity,
    val data: ComponentValueData,
)
```

**Стало:**
```kotlin
data class ComponentValueApiEntity(
    val id: Long,
    val lexemeId: Long,
    val type: ComponentTypeApiEntity,
    val data: TemplateValues,
    val createdAt: Date,
    val updatedAt: Date,
    val removedAt: Date? = null,
)
```

Все call-sites рассматривают `data: TemplateValues`; soft-deleted rows скрываются на чтении (DAO convention или post-load filter, F031 — data side).

#### #21 DictionaryTypesSnapshot.kt [+]

Data-layer-shape snapshot, маппится в domain `PerDictionarySnapshot`. Содержит `ComponentTypeApiEntity` (API) и raw map.

```kotlin
package me.apomazkin.core_db_api.entity

data class DictionaryTypesSnapshot(
    val dictionaryId: Long,
    val dictionaryName: String,
    val types: List<ComponentTypeApiEntity>,
    val valueCountByType: Map<Long, Int>,    // raw typeId.value → count
)
```

#### #22 UserDefinedTypesUsageSnapshot.kt [+]

Аналог для aggregated view.

```kotlin
package me.apomazkin.core_db_api.entity

data class UserDefinedTypesUsageSnapshot(
    val types: List<ComponentTypeApiEntity>,
    val valueCountByType: Map<Long, Int>,
    val dictionaryIdsByType: Map<Long, Set<Long>>,
    val dictionaryNames: Map<Long, String>,
)
```

#### #23 ComponentOutcomeApiEntity.kt [+]

Data-layer typed outcomes для CRUD (маппятся в domain `CreateOutcome`/`RenameOutcome`/`DeleteOutcome`).

```kotlin
package me.apomazkin.core_db_api.entity

import me.apomazkin.lexeme.DeletionImpact

sealed interface CreateComponentOutcome {
    /** length = 1 для Global / single per-dict; length = N для PerDictionaries(N). */
    data class Success(val types: List<ComponentTypeApiEntity>) : CreateComponentOutcome
    data object SameScopeCollision : CreateComponentOutcome
    data object CrossScopeCollision : CreateComponentOutcome
}

sealed interface RenameComponentOutcome {
    data class Success(val type: ComponentTypeApiEntity) : RenameComponentOutcome
    data object SameScopeCollision : RenameComponentOutcome
    data object CrossScopeCollision : RenameComponentOutcome
    data object BuiltInProtected : RenameComponentOutcome
}

sealed interface SoftDeleteComponentOutcome {
    data class Success(val impact: DeletionImpact) : SoftDeleteComponentOutcome
    data object BuiltInProtected : SoftDeleteComponentOutcome
}
```

`DeletionImpact` берётся из domain (`me.apomazkin.lexeme.DeletionImpact`) — data знает domain по A1/MIN-2.

#### #24 CoreDbApi.kt [~]

Расширить `interface LexemeApi`:

**Было (релевантные методы):** `addLexemeWithBuiltInComponent(..., data: ComponentValueData)`, `addLexemeWithUserDefinedComponent(..., data: ComponentValueData)`, `addLexemeWithComponents(..., components: List<Pair<ComponentTypeRef, ComponentValueData>>)`, `addComponentValue(..., data: ComponentValueData)`, `updateComponentValue(..., data: ComponentValueData)` (см. `CoreDbApi.kt:93-132`).

**Стало:**

1. **BREAKING** — 5 сигнатур меняют `ComponentValueData → TemplateValues`:
   ```kotlin
   suspend fun addLexemeWithBuiltInComponent(
       wordId: Long,
       dictionaryId: Long,
       systemKey: BuiltInComponent,
       data: TemplateValues,                                                  // ← M13
   ): Long

   suspend fun addLexemeWithUserDefinedComponent(
       wordId: Long,
       dictionaryId: Long,
       name: String,
       data: TemplateValues,                                                  // ← M13
   ): Long?

   suspend fun addLexemeWithComponents(
       wordId: Long,
       dictionaryId: Long,
       components: List<Pair<ComponentTypeRef, TemplateValues>>,              // ← M13
   ): Long?

   suspend fun addComponentValue(
       lexemeId: Long,
       componentTypeId: Long,
       data: TemplateValues,                                                  // ← M13
   ): Long

   suspend fun updateComponentValue(
       componentValueId: Long,
       data: TemplateValues,                                                  // ← M13
   ): Int
   ```

2. **NEW** — 6 методов для конструктора:
   ```kotlin
   /**
    * Реактивная подписка на все user-defined active component_types
    * (`system_key IS NULL AND removed_at IS NULL`) + aggregated usage.
    * Source: ui_placement.md § Общий view.
    */
   fun flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesUsageSnapshot>

   /**
    * Реактивная подписка на active user-defined types применимые к словарю:
    * `(dictionary_id = :dictId OR dictionary_id IS NULL) AND system_key IS NULL
    *  AND removed_at IS NULL` + valueCount within dict.
    */
   fun flowUserDefinedTypesForDictionary(dictionaryId: Long): Flow<DictionaryTypesSnapshot>

   /**
    * Atomic create user-defined component_type:
    *  1) two-prong SELECT for name collision (aspect `userdefined_identity_invariant`);
    *  2) INSERT row(s) — для PerDictionaries(N) создаётся N rows в одной транзакции;
    *  3) Возврат typed outcome со ВСЕМИ созданными rows (F2 iter1 review).
    */
   suspend fun createUserDefinedComponent(
       name: String,
       template: ComponentTemplate,
       isMulti: Boolean,
       scope: Scope,
   ): CreateComponentOutcome

   /**
    * Atomic rename + cascade `quiz_configs.component_refs` (json_replace).
    * Built-in защищён `WHERE system_key IS NULL`.
    */
   suspend fun renameComponentType(
       typeId: Long,
       newName: String,
   ): RenameComponentOutcome

   /**
    * Read-only preview: valueCount + dictionariesWithValues + affectedQuizConfigs +
    * affectedPrefs. JOIN component_values ⋈ lexemes ⋈ words ⋈ dictionaries +
    * scan quiz_configs.component_refs JSON. Prefs iterate — на UseCase-уровне.
    */
   suspend fun previewDeletionImpact(typeId: Long): DeletionImpact?

   /**
    * Atomic soft-delete + cascade:
    *  1) UPDATE component_types SET removed_at = :now WHERE id = ? AND system_key IS NULL;
    *  2) Cascade `quiz_configs.component_refs` (json_remove либо собрать новый JSON);
    *  3) Возврат `DeletionImpact` для UI snackbar.
    * Affected prefs сбрасываются ВНЕ транзакции на UseCase-уровне (DataStore, не Room).
    */
   suspend fun softDeleteComponentType(typeId: Long): SoftDeleteComponentOutcome
   ```

3. **Дополнительные imports** в `CoreDbApi.kt`:
   ```kotlin
   import kotlinx.coroutines.flow.Flow
   import me.apomazkin.core_db_api.entity.DictionaryTypesSnapshot
   import me.apomazkin.core_db_api.entity.UserDefinedTypesUsageSnapshot
   import me.apomazkin.core_db_api.entity.CreateComponentOutcome
   import me.apomazkin.core_db_api.entity.RenameComponentOutcome
   import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
   import me.apomazkin.lexeme.ComponentTemplate
   import me.apomazkin.lexeme.DeletionImpact
   import me.apomazkin.lexeme.Scope
   import me.apomazkin.lexeme.TemplateValues
   ```

   Removed:
   ```kotlin
   import me.apomazkin.lexeme.ComponentValueData            // dropped in M13
   ```

### Tier 3: UseCase interfaces (deps/)

> Infra-фаза создала placeholder с TODO-комментом (`infra_design_tree.md` id 5/6). Здесь — финализация контрактных методов.

#### #25 ComponentsManagerUseCase.kt [~]

**Было** (placeholder из infra):
```kotlin
package me.apomazkin.components_manager.deps

interface ComponentsManagerUseCase {
    // методы — финализируются на business_contract / business_design_tree
}
```

**Стало:**
```kotlin
package me.apomazkin.components_manager.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

interface ComponentsManagerUseCase {

    /**
     * Subscribed by [AllUserDefinedTypesFlowHandler]. Один dedicated snapshot —
     * никаких N+1 запросов из reducer.
     */
    fun flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>

    /**
     * Triggered by [DatasourceEffect.CreateComponent].
     * @return [CreateOutcome.Success] с list созданных entities (length = N для multi-scope) либо typed error.
     */
    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
        scope: Scope,
    ): CreateOutcome

    /** Triggered by [DatasourceEffect.RenameComponent]. */
    suspend fun renameComponent(
        typeId: ComponentTypeId,
        newName: String,
    ): RenameOutcome

    /** Triggered by [DatasourceEffect.LoadImpact]. Read-only preview. */
    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    /** Triggered by [DatasourceEffect.SoftDeleteComponent]. */
    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome
}
```

#### #26 PerDictionaryComponentsUseCase.kt [~]

**Стало:**
```kotlin
package me.apomazkin.per_dictionary_components.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope

interface PerDictionaryComponentsUseCase {

    /** Subscribed by [ComponentsForDictionaryFlowHandler]. */
    fun flowComponentsForDictionary(dictionaryId: Long): Flow<PerDictionarySnapshot>

    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
        scope: Scope,
    ): CreateOutcome

    suspend fun renameComponent(typeId: ComponentTypeId, newName: String): RenameOutcome

    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome
}
```

**Решение Open Q #2 (business_contract):** оба UseCase — **отдельные интерфейсы с делегированием в общий impl**. `PerDictionaryComponentsUseCaseImpl` принимает `ComponentsManagerUseCaseImpl` (либо общую CRUD-фабрику) в ctor через DI и делегирует write-методы (`createUserDefinedComponent` / `renameComponent` / `previewDeletionImpact` / `softDeleteComponent`). Read-метод (`flowComponentsForDictionary`) — собственный, со scoped query. Обоснование: cross-module наследование нежелательно, но shared CRUD impl допустим через DI composition.

### Tier 4: UseCase impls (app/di/module/...)

> Infra-фаза создала placeholder (`infra_design_tree.md` id 23/25). Здесь — финализация.

#### #27 ComponentsManagerUseCaseImpl.kt [~]

**Было** (placeholder):
```kotlin
class ComponentsManagerUseCaseImpl @Inject constructor(
    // ...
) : ComponentsManagerUseCase {
    // implementations — business_design_tree
}
```

**Стало:**
```kotlin
package me.apomazkin.polytrainer.di.module.componentsmanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import me.apomazkin.components_manager.LogTags
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.CreateComponentOutcome
import me.apomazkin.core_db_api.entity.RenameComponentOutcome
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentUsage
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.polytrainer.mapper.toDomain     // existing api → domain mapper для ComponentType
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizPickerPrefKey
import javax.inject.Inject

class ComponentsManagerUseCaseImpl @Inject constructor(
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) : ComponentsManagerUseCase {

    override fun flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot> =
        lexemeApi.flowAllUserDefinedTypesWithUsage().map { api ->
            UserDefinedTypesSnapshot(
                types = api.types.map { it.toDomain() },
                usage = ComponentUsage(
                    valueCountByType = api.valueCountByType.mapKeys { ComponentTypeId(it.key) },
                    dictionaryIdsByType = api.dictionaryIdsByType.mapKeys { ComponentTypeId(it.key) },
                    dictionaryNames = api.dictionaryNames,
                ),
            )
        }

    override suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
        scope: Scope,
    ): CreateOutcome = try {
        if (name.isBlank()) return CreateOutcome.NameEmpty
        when (val r = lexemeApi.createUserDefinedComponent(name.trim(), template, isMulti, scope)) {
            is CreateComponentOutcome.Success ->
                CreateOutcome.Success(r.types.map { it.toDomain() })
            CreateComponentOutcome.SameScopeCollision -> CreateOutcome.SameScopeCollision
            CreateComponentOutcome.CrossScopeCollision -> CreateOutcome.CrossScopeCollision
        }
    } catch (e: Exception) {
        logger.e(tag = LogTags.COMPONENTS_MANAGER, message = "create failed: ${e.message}")
        CreateOutcome.Failure(e)
    }

    override suspend fun renameComponent(
        typeId: ComponentTypeId,
        newName: String,
    ): RenameOutcome = try {
        if (newName.isBlank()) return RenameOutcome.NameEmpty
        when (val r = lexemeApi.renameComponentType(typeId.id, newName.trim())) {
            is RenameComponentOutcome.Success -> RenameOutcome.Success(r.type.toDomain())
            RenameComponentOutcome.SameScopeCollision -> RenameOutcome.SameScopeCollision
            RenameComponentOutcome.CrossScopeCollision -> RenameOutcome.CrossScopeCollision
            RenameComponentOutcome.BuiltInProtected -> RenameOutcome.BuiltInProtected
        }
    } catch (e: Exception) {
        logger.e(tag = LogTags.COMPONENTS_MANAGER, message = "rename failed: ${e.message}")
        RenameOutcome.Failure(e)
    }

    override suspend fun previewDeletionImpact(
        typeId: ComponentTypeId,
    ): DeletionImpact? = try {
        lexemeApi.previewDeletionImpact(typeId.id)
    } catch (e: Exception) {
        logger.e(tag = LogTags.COMPONENTS_MANAGER, message = "previewImpact failed: ${e.message}")
        null
    }

    override suspend fun softDeleteComponent(
        typeId: ComponentTypeId,
    ): DeleteOutcome = try {
        when (val r = lexemeApi.softDeleteComponentType(typeId.id)) {
            is SoftDeleteComponentOutcome.Success -> {
                // Cleanup prefs ВНЕ Room транзакции (option B, F049):
                resetQuizPickerPrefsFor(typeId, r.impact)
                DeleteOutcome.Success(r.impact)
            }
            SoftDeleteComponentOutcome.BuiltInProtected -> DeleteOutcome.BuiltInProtected
        }
    } catch (e: Exception) {
        logger.e(tag = LogTags.COMPONENTS_MANAGER, message = "softDelete failed: ${e.message}")
        DeleteOutcome.Failure(e)
    }

    /**
     * F049 option B: для каждого dictId из `impact.affectedPrefs` — сбросить
     * `quiz_picker_dict_<id>` pref через rawKey API.
     */
    private suspend fun resetQuizPickerPrefsFor(
        typeId: ComponentTypeId,
        impact: DeletionImpact,
    ) {
        impact.affectedPrefs.forEach { dictId ->
            prefsProvider.setStringByRawKey(quizPickerPrefKey(dictId), null)
        }
    }
}
```

`me.apomazkin.components_manager.LogTags` — создаётся отдельным узлом #55 (см. Tier 0 independent ниже).

#### #28 PerDictionaryComponentsUseCaseImpl.kt [~]

**Было** (placeholder).

**Стало:**
```kotlin
package me.apomazkin.polytrainer.di.module.perdictionarycomponents

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import me.apomazkin.polytrainer.di.module.componentsmanager.ComponentsManagerUseCaseImpl
import me.apomazkin.polytrainer.mapper.toDomain
import javax.inject.Inject

class PerDictionaryComponentsUseCaseImpl @Inject constructor(
    private val lexemeApi: CoreDbApi.LexemeApi,
    // Делегирование write-методов на shared impl
    private val sharedCrud: ComponentsManagerUseCaseImpl,
) : PerDictionaryComponentsUseCase {

    override fun flowComponentsForDictionary(
        dictionaryId: Long,
    ): Flow<PerDictionarySnapshot> =
        lexemeApi.flowUserDefinedTypesForDictionary(dictionaryId).map { api ->
            PerDictionarySnapshot(
                dictionaryId = api.dictionaryId,
                dictionaryName = api.dictionaryName,
                types = api.types.map { it.toDomain() },
                valueCountByType = api.valueCountByType.mapKeys { ComponentTypeId(it.key) },
            )
        }

    override suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
        scope: Scope,
    ): CreateOutcome = sharedCrud.createUserDefinedComponent(name, template, isMulti, scope)

    override suspend fun renameComponent(
        typeId: ComponentTypeId,
        newName: String,
    ): RenameOutcome = sharedCrud.renameComponent(typeId, newName)

    override suspend fun previewDeletionImpact(
        typeId: ComponentTypeId,
    ): DeletionImpact? = sharedCrud.previewDeletionImpact(typeId)

    override suspend fun softDeleteComponent(
        typeId: ComponentTypeId,
    ): DeleteOutcome = sharedCrud.softDeleteComponent(typeId)
}
```

Note: DI module (`PerDictionaryComponentsModule`, infra id 26) уже `@Binds` `PerDictionaryComponentsUseCaseImpl → PerDictionaryComponentsUseCase`. `ComponentsManagerUseCaseImpl` доступен как `@Inject` constructor — Dagger разрешит. Если требуется expose как self-bind — добавить `@Binds @Singleton` либо использовать direct ctor injection (Dagger автоматически генерирует Provider).

### Tier 5: ComponentsManagerScreen — Mate (logic/)

#### #29 State.kt [+]

```kotlin
package me.apomazkin.components_manager.logic

import androidx.compose.runtime.Stable
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.Scope

@Stable
data class ComponentsManagerScreenState(
    // ===== Loaded data =====
    val userDefinedTypes: List<UserDefinedRow>? = null,

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
)

@Stable
data class UserDefinedRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMulti: Boolean,
    val scope: Scope,
    val usageCount: Int,
    val dictionaryNames: List<String>,
)

@Stable
data class CreateDialogState(
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isMulti: Boolean = false,
    val scope: Scope = Scope.Global,
    val nameError: NameError? = null,
)

@Stable
data class RenameDialogState(
    val typeId: ComponentTypeId,
    val originalName: String,
    val editedName: String,
    val nameError: NameError? = null,
)

@Stable
data class DeleteConfirmState(
    val typeId: ComponentTypeId,
    val name: String,
    val impact: DeletionImpact? = null,
    val isLoadingImpact: Boolean = false,
)

// Computed selectors
val ComponentsManagerScreenState.isEmpty: Boolean
    get() = userDefinedTypes?.isEmpty() == true && !isLoading

/**
 * Маппер snapshot → UI rows. Используется в Reducer #37 на `Msg.TypesLoaded`.
 * Signature — best-guess; финальные детали (group by scope, dictionaryNames lookup)
 * — в business_implement.
 */
internal fun UserDefinedTypesSnapshot.toRows(): List<UserDefinedRow> =
    types.map { t ->
        UserDefinedRow(
            typeId = t.id,
            name = t.name.orEmpty(),
            template = t.template,
            isMulti = t.isMulti,
            scope = if (t.dictionaryId == null) Scope.Global
                    else Scope.PerDictionaries(listOf(t.dictionaryId)),
            usageCount = usage.valueCountByType[t.id] ?: 0,
            dictionaryNames = (usage.dictionaryIdsByType[t.id] ?: emptySet())
                .mapNotNull { usage.dictionaryNames[it] },
        )
    }
```

Узел #29 описание: **State.kt + helper `toRows()`** (extension function в том же файле).

Инварианты `[shape]` / `[transition]` — см. `business_contract_spec.md` § Инварианты; код enforce'ится в Reducer через guard-условия (#37).

#### #30 Message.kt [+]

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

sealed interface Msg {

    // ===== Lifecycle / data =====
    data class TypesLoaded(val snapshot: UserDefinedTypesSnapshot) : Msg
    data class TypesLoadFailed(val cause: Throwable) : Msg

    // ===== Create dialog =====
    data object OpenCreateDialog : Msg
    data object CloseCreateDialog : Msg
    data class CreateNameChange(val value: String) : Msg
    data class CreateTemplateChange(val template: ComponentTemplate) : Msg
    data class CreateMultiToggle(val isMulti: Boolean) : Msg
    data class CreateScopeChange(val scope: Scope) : Msg
    data object SubmitCreate : Msg
    data class CreateResult(val outcome: CreateOutcome) : Msg

    // ===== Rename dialog =====
    data class OpenRenameDialog(val typeId: ComponentTypeId) : Msg
    data object CloseRenameDialog : Msg
    data class RenameTextChange(val value: String) : Msg
    data object SubmitRename : Msg
    data class RenameResult(val outcome: RenameOutcome) : Msg

    // ===== Delete dialog =====
    data class OpenDeleteConfirm(val typeId: ComponentTypeId) : Msg
    data object CloseDeleteConfirm : Msg
    data class ImpactPreviewLoaded(val impact: DeletionImpact) : Msg
    data class ImpactPreviewFailed(val cause: Throwable) : Msg
    data object ConfirmDelete : Msg
    data class DeleteResult(val outcome: DeleteOutcome) : Msg

    // ===== Navigation =====
    data object RequestBack : Msg

    // ===== No-op =====
    data object Empty : Msg
}

sealed interface UiMsg : Msg {
    data class Snackbar(val text: String, val show: Boolean) : UiMsg
}
```

#### #31 UiEffectHandler.kt [+]

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import javax.inject.Inject

sealed interface UiEffect : Effect {
    data class Snackbar(val text: String) : UiEffect
}

class UiEffectHandler @Inject constructor() : MateTypedEffectHandler<Msg, UiEffect>() {

    override fun filter(effect: Effect): UiEffect? = effect as? UiEffect

    override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        val msg = when (effect) {
            is UiEffect.Snackbar -> UiMsg.Snackbar(text = effect.text, show = true)
        }
        consumer(msg)
    }
}
```

#### #32 DatasourceEffect.kt [+]

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.mate.Effect

sealed interface DatasourceEffect : Effect {

    data class CreateComponent(
        val name: String,
        val template: ComponentTemplate,
        val isMulti: Boolean,
        val scope: Scope,
    ) : DatasourceEffect

    data class RenameComponent(
        val typeId: ComponentTypeId,
        val newName: String,
    ) : DatasourceEffect

    data class LoadImpact(val typeId: ComponentTypeId) : DatasourceEffect

    data class SoftDeleteComponent(val typeId: ComponentTypeId) : DatasourceEffect
}
```

`SubscribeAll` (см. contract) — реализован как **`init`-trigger в `MateFlowHandler.subscribe`** (см. #34); отдельный Effect не нужен — handler автоматически подписывается при init Mate, по аналогии с `QuizPickerFlowHandler`. Если позже понадобится re-subscribe (например, на ручной refresh) — добавить `data object SubscribeAll : DatasourceEffect`.

#### #33 DatasourceEffectHandler.kt [+]

```kotlin
package me.apomazkin.components_manager.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.components_manager.LogTags
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import javax.inject.Inject

class DatasourceEffectHandler @Inject constructor(
    private val useCase: ComponentsManagerUseCase,
    private val logger: LexemeLogger,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        val msg: Msg = withContext(Dispatchers.IO) {
            try {
                when (effect) {
                    is DatasourceEffect.CreateComponent ->
                        Msg.CreateResult(
                            useCase.createUserDefinedComponent(
                                effect.name, effect.template, effect.isMulti, effect.scope,
                            )
                        )
                    is DatasourceEffect.RenameComponent ->
                        Msg.RenameResult(useCase.renameComponent(effect.typeId, effect.newName))
                    is DatasourceEffect.LoadImpact -> {
                        val impact = useCase.previewDeletionImpact(effect.typeId)
                        if (impact != null) Msg.ImpactPreviewLoaded(impact)
                        else Msg.ImpactPreviewFailed(IllegalStateException("preview returned null"))
                    }
                    is DatasourceEffect.SoftDeleteComponent ->
                        Msg.DeleteResult(useCase.softDeleteComponent(effect.typeId))
                }
            } catch (e: Exception) {
                logger.e(tag = LogTags.COMPONENTS_MANAGER, message = "Effect failed: ${effect::class.simpleName} — ${e.message}")
                when (effect) {
                    is DatasourceEffect.CreateComponent -> Msg.CreateResult(CreateOutcome.Failure(e))
                    is DatasourceEffect.RenameComponent -> Msg.RenameResult(RenameOutcome.Failure(e))
                    is DatasourceEffect.LoadImpact -> Msg.ImpactPreviewFailed(e)
                    is DatasourceEffect.SoftDeleteComponent -> Msg.DeleteResult(DeleteOutcome.Failure(e))
                }
            }
        }
        consumer(msg)
    }
}
```

#### #34 AllUserDefinedTypesFlowHandler.kt [+]

Аналог `QuizPickerFlowHandler` (`modules/screen/quiz/chat/.../QuizPickerFlowHandler.kt`) — `MateFlowHandler` подписывается при init Mate.

```kotlin
package me.apomazkin.components_manager.logic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.components_manager.LogTags
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import javax.inject.Inject

class AllUserDefinedTypesFlowHandler @Inject constructor(
    private val useCase: ComponentsManagerUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {}

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            useCase.flowAllUserDefinedTypes()
                .catch { e ->
                    logger.e(tag = LogTags.COMPONENTS_MANAGER, message = "flow failed: ${e.message}")
                    send(Msg.TypesLoadFailed(e))
                }
                .collectLatest { snapshot ->
                    send(Msg.TypesLoaded(snapshot))
                }
        }
    }
}
```

#### #35 ComponentsManagerNavigationEffect.kt [+]

```kotlin
package me.apomazkin.components_manager

import me.apomazkin.mate.NavigationEffect

sealed interface ComponentsManagerNavigationEffect : NavigationEffect {
    // только Back; конкретных переходов нет (drill-in в per-dict экран
    // не предусмотрен per ui_placement.md § Cross-flow)
}
```

#### #36 ComponentsManagerNavigationEffectHandler.kt [+]

```kotlin
package me.apomazkin.components_manager

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.components_manager.logic.Msg
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect

class ComponentsManagerNavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: ComponentsManagerNavigator,
) : MateNavigationEffectHandler<Msg>(navigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        // base Back уже обработан super; экран собственных переходов не имеет
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: ComponentsManagerNavigator): ComponentsManagerNavigationEffectHandler
    }
}
```

`ComponentsManagerNavigator` — interface создан в infra (`infra_design_tree.md` id 7) с одним методом `back()` из `Navigator` base.

#### #37 ComponentsManagerReducer.kt [+]

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.mate.NavigationEffect

class ComponentsManagerReducer : MateReducer<ComponentsManagerScreenState, Msg, Effect> {

    override fun reduce(
        state: ComponentsManagerScreenState,
        message: Msg,
    ): ReducerResult<ComponentsManagerScreenState, Effect> = when (message) {

        // ===== Lifecycle =====
        is Msg.TypesLoaded -> {
            val rows = message.snapshot.toRows()       // helper в State.kt (см. ниже)
            state.copy(userDefinedTypes = rows, isLoading = false) to emptySet()
        }
        is Msg.TypesLoadFailed -> state.copy(isLoading = false) to setOf(
            UiEffect.Snackbar("Failed to load: ${message.cause.message}")
        )

        // ===== Create dialog =====
        Msg.OpenCreateDialog ->
            state.copy(createDialog = CreateDialogState()) to emptySet()
        Msg.CloseCreateDialog ->
            state.copy(createDialog = null) to emptySet()
        is Msg.CreateNameChange ->
            state.copy(
                createDialog = state.createDialog?.copy(
                    name = message.value, nameError = null
                )
            ) to emptySet()
        is Msg.CreateTemplateChange ->
            state.copy(
                createDialog = state.createDialog?.copy(template = message.template)
            ) to emptySet()
        is Msg.CreateMultiToggle ->
            state.copy(
                createDialog = state.createDialog?.copy(isMulti = message.isMulti)
            ) to emptySet()
        is Msg.CreateScopeChange ->
            state.copy(
                createDialog = state.createDialog?.copy(scope = message.scope)
            ) to emptySet()
        Msg.SubmitCreate -> {
            val dlg = state.createDialog ?: return@reduce state to emptySet()
            if (state.isCreating) return@reduce state to emptySet()        // [transition] guard
            if (dlg.name.isBlank()) {
                state.copy(createDialog = dlg.copy(nameError = NameError.Empty)) to emptySet()
            } else {
                state.copy(isCreating = true) to setOf(
                    DatasourceEffect.CreateComponent(dlg.name, dlg.template, dlg.isMulti, dlg.scope)
                )
            }
        }
        is Msg.CreateResult -> when (val o = message.outcome) {
            is CreateOutcome.Success ->
                state.copy(isCreating = false, createDialog = null) to setOf(
                    UiEffect.Snackbar("Created ${o.created.size}")
                )
            CreateOutcome.NameEmpty ->
                state.copy(
                    isCreating = false,
                    createDialog = state.createDialog?.copy(nameError = NameError.Empty),
                ) to emptySet()
            CreateOutcome.SameScopeCollision ->
                state.copy(
                    isCreating = false,
                    createDialog = state.createDialog?.copy(nameError = NameError.SameScopeCollision),
                ) to emptySet()
            CreateOutcome.CrossScopeCollision ->
                state.copy(
                    isCreating = false,
                    createDialog = state.createDialog?.copy(nameError = NameError.CrossScopeCollision),
                ) to emptySet()
            is CreateOutcome.Failure ->
                state.copy(isCreating = false) to setOf(
                    UiEffect.Snackbar("Failed: ${o.cause.message}")
                )
        }

        // ===== Rename dialog =====
        is Msg.OpenRenameDialog -> {
            val row = state.userDefinedTypes?.firstOrNull { it.typeId == message.typeId }
                ?: return@reduce state to emptySet()
            state.copy(
                renameDialog = RenameDialogState(
                    typeId = row.typeId,
                    originalName = row.name,
                    editedName = row.name,
                )
            ) to emptySet()
        }
        Msg.CloseRenameDialog ->
            state.copy(renameDialog = null) to emptySet()
        is Msg.RenameTextChange ->
            state.copy(
                renameDialog = state.renameDialog?.copy(
                    editedName = message.value, nameError = null
                )
            ) to emptySet()
        Msg.SubmitRename -> {
            val dlg = state.renameDialog ?: return@reduce state to emptySet()
            if (state.isRenaming) return@reduce state to emptySet()
            if (dlg.editedName.isBlank()) {
                state.copy(renameDialog = dlg.copy(nameError = NameError.Empty)) to emptySet()
            } else {
                state.copy(isRenaming = true) to setOf(
                    DatasourceEffect.RenameComponent(dlg.typeId, dlg.editedName)
                )
            }
        }
        is Msg.RenameResult -> when (val o = message.outcome) {
            is RenameOutcome.Success ->
                state.copy(isRenaming = false, renameDialog = null) to setOf(
                    UiEffect.Snackbar("Renamed")
                )
            RenameOutcome.NameEmpty ->
                state.copy(
                    isRenaming = false,
                    renameDialog = state.renameDialog?.copy(nameError = NameError.Empty),
                ) to emptySet()
            RenameOutcome.SameScopeCollision ->
                state.copy(
                    isRenaming = false,
                    renameDialog = state.renameDialog?.copy(nameError = NameError.SameScopeCollision),
                ) to emptySet()
            RenameOutcome.CrossScopeCollision ->
                state.copy(
                    isRenaming = false,
                    renameDialog = state.renameDialog?.copy(nameError = NameError.CrossScopeCollision),
                ) to emptySet()
            RenameOutcome.BuiltInProtected ->
                state.copy(isRenaming = false, renameDialog = null) to setOf(
                    UiEffect.Snackbar("Built-in protected")
                )
            is RenameOutcome.Failure ->
                state.copy(isRenaming = false) to setOf(
                    UiEffect.Snackbar("Failed: ${o.cause.message}")
                )
        }

        // ===== Delete confirm =====
        is Msg.OpenDeleteConfirm -> {
            val row = state.userDefinedTypes?.firstOrNull { it.typeId == message.typeId }
                ?: return@reduce state to emptySet()
            state.copy(
                deleteConfirm = DeleteConfirmState(
                    typeId = row.typeId,
                    name = row.name,
                    isLoadingImpact = true,
                )
            ) to setOf(DatasourceEffect.LoadImpact(row.typeId))
        }
        Msg.CloseDeleteConfirm ->
            state.copy(deleteConfirm = null) to emptySet()
        is Msg.ImpactPreviewLoaded ->
            state.copy(
                deleteConfirm = state.deleteConfirm?.copy(
                    impact = message.impact, isLoadingImpact = false,
                )
            ) to emptySet()
        is Msg.ImpactPreviewFailed ->
            state.copy(
                deleteConfirm = state.deleteConfirm?.copy(isLoadingImpact = false),
            ) to setOf(UiEffect.Snackbar("Failed to load impact"))
        Msg.ConfirmDelete -> {
            val dlg = state.deleteConfirm ?: return@reduce state to emptySet()
            if (state.isDeleting) return@reduce state to emptySet()        // [transition] guard
            state.copy(isDeleting = true) to setOf(
                DatasourceEffect.SoftDeleteComponent(dlg.typeId)
            )
        }
        is Msg.DeleteResult -> when (val o = message.outcome) {
            is DeleteOutcome.Success ->
                state.copy(isDeleting = false, deleteConfirm = null) to setOf(
                    UiEffect.Snackbar("${o.impact.valueCount} values hidden")
                )
            DeleteOutcome.BuiltInProtected ->
                state.copy(isDeleting = false, deleteConfirm = null) to setOf(
                    UiEffect.Snackbar("Built-in protected")
                )
            is DeleteOutcome.Failure ->
                state.copy(isDeleting = false) to setOf(
                    UiEffect.Snackbar("Failed: ${o.cause.message}")
                )
        }

        // ===== Navigation =====
        Msg.RequestBack -> state to setOf(NavigationEffect.Back)

        // ===== No-op / UiMsg =====
        Msg.Empty -> state to emptySet()
        is UiMsg -> state to emptySet()
    }
}
```

`UserDefinedTypesSnapshot.toRows()` — helper в `State.kt` ниже (объединяет `types` + `usage` в `List<UserDefinedRow>`); финальная реализация (group by scope, dictionaryNames lookup) — в `business_implement`.

#### #38 ComponentsManagerViewModel.kt [+]

```kotlin
package me.apomazkin.components_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.components_manager.logic.AllUserDefinedTypesFlowHandler
import me.apomazkin.components_manager.logic.ComponentsManagerReducer
import me.apomazkin.components_manager.logic.ComponentsManagerScreenState
import me.apomazkin.components_manager.logic.DatasourceEffectHandler
import me.apomazkin.components_manager.logic.Msg
import me.apomazkin.components_manager.logic.UiEffectHandler
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

class ComponentsManagerViewModel @AssistedInject constructor(
    @Assisted navigator: ComponentsManagerNavigator,
    datasourceHandler: DatasourceEffectHandler,
    flowHandler: AllUserDefinedTypesFlowHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: ComponentsManagerNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<ComponentsManagerScreenState, Msg> {

    private val stateHolder = Mate(
        initState = ComponentsManagerScreenState(isLoading = true),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = ComponentsManagerReducer(),
        effectHandlerSet = setOf(
            datasourceHandler,
            flowHandler,
            uiHandler,
            navHandlerFactory.create(navigator),
        ),
    )

    override val state: StateFlow<ComponentsManagerScreenState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: ComponentsManagerNavigator): ComponentsManagerViewModel
    }
}
```

### Tier 6: PerDictionaryComponentsScreen — Mate (logic/)

Структурно зеркально Tier 5. Различия только в init-параметре `dictionaryId` и lifecycle-сообщении `ItemsLoaded` вместо `TypesLoaded`. Узлы #39-#48 имеют те же signatures pattern.

#### #39 State.kt [+]

```kotlin
package me.apomazkin.per_dictionary_components.logic

import androidx.compose.runtime.Stable
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.Scope

@Stable
data class PerDictionaryComponentsScreenState(
    // ===== Init context =====
    val dictionaryId: Long,
    val dictionaryName: String? = null,

    // ===== Loaded data =====
    val items: List<PerDictRow>? = null,

    // ===== UI flags =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
)

@Stable
data class PerDictRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMulti: Boolean,
    val isGlobal: Boolean,
    val valueCount: Int,
)

// CreateDialogState / RenameDialogState / DeleteConfirmState — дублируются
// (Open Q #1, business_contract: дублирование минимизирует cross-module зависимости).
@Stable
data class CreateDialogState(
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isMulti: Boolean = false,
    val scope: Scope,                          // ctor-инициализируется текущим dictId (см. Reducer Open)
    val nameError: NameError? = null,
)

@Stable
data class RenameDialogState(
    val typeId: ComponentTypeId,
    val originalName: String,
    val editedName: String,
    val nameError: NameError? = null,
)

@Stable
data class DeleteConfirmState(
    val typeId: ComponentTypeId,
    val name: String,
    val impact: DeletionImpact? = null,
    val isLoadingImpact: Boolean = false,
)

val PerDictionaryComponentsScreenState.isEmpty: Boolean
    get() = items?.isEmpty() == true && !isLoading

/**
 * Маппер snapshot → UI rows. Используется в Reducer #47 на `Msg.ItemsLoaded`.
 * Signature — best-guess; финальные детали — в business_implement.
 */
internal fun PerDictionarySnapshot.toPerDictRows(dictionaryId: Long): List<PerDictRow> =
    types.map { t ->
        PerDictRow(
            typeId = t.id,
            name = t.name.orEmpty(),
            template = t.template,
            isMulti = t.isMulti,
            isGlobal = t.dictionaryId == null,
            valueCount = valueCountByType[t.id] ?: 0,
        )
    }
```

Узел #39 описание: **State.kt + helper `toPerDictRows()`** (extension function в том же файле).

#### #40 Message.kt [+]

```kotlin
package me.apomazkin.per_dictionary_components.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope

sealed interface Msg {

    // ===== Lifecycle / data =====
    data class ItemsLoaded(val snapshot: PerDictionarySnapshot) : Msg
    data class ItemsLoadFailed(val cause: Throwable) : Msg

    // ===== Create dialog ===== (зеркально ComponentsManagerScreen)
    data object OpenCreateDialog : Msg
    data object CloseCreateDialog : Msg
    data class CreateNameChange(val value: String) : Msg
    data class CreateTemplateChange(val template: ComponentTemplate) : Msg
    data class CreateMultiToggle(val isMulti: Boolean) : Msg
    data class CreateScopeChange(val scope: Scope) : Msg
    data object SubmitCreate : Msg
    data class CreateResult(val outcome: CreateOutcome) : Msg

    // ===== Rename dialog =====
    data class OpenRenameDialog(val typeId: ComponentTypeId) : Msg
    data object CloseRenameDialog : Msg
    data class RenameTextChange(val value: String) : Msg
    data object SubmitRename : Msg
    data class RenameResult(val outcome: RenameOutcome) : Msg

    // ===== Delete dialog =====
    data class OpenDeleteConfirm(val typeId: ComponentTypeId) : Msg
    data object CloseDeleteConfirm : Msg
    data class ImpactPreviewLoaded(val impact: DeletionImpact) : Msg
    data class ImpactPreviewFailed(val cause: Throwable) : Msg
    data object ConfirmDelete : Msg
    data class DeleteResult(val outcome: DeleteOutcome) : Msg

    // ===== Navigation =====
    data object RequestBack : Msg

    data object Empty : Msg
}

sealed interface UiMsg : Msg {
    data class Snackbar(val text: String, val show: Boolean) : UiMsg
}
```

#### #41 UiEffectHandler.kt [+]

Зеркально #31, package `me.apomazkin.per_dictionary_components.logic`.

#### #42 DatasourceEffect.kt [+]

```kotlin
package me.apomazkin.per_dictionary_components.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.mate.Effect

sealed interface DatasourceEffect : Effect {

    data class CreateComponent(
        val name: String,
        val template: ComponentTemplate,
        val isMulti: Boolean,
        val scope: Scope,
    ) : DatasourceEffect

    data class RenameComponent(
        val typeId: ComponentTypeId,
        val newName: String,
    ) : DatasourceEffect

    data class LoadImpact(val typeId: ComponentTypeId) : DatasourceEffect

    data class SoftDeleteComponent(val typeId: ComponentTypeId) : DatasourceEffect
}
```

Note (Open Q resolution): per-dict screen имеет dynamic `dictionaryId` (init-параметр). Подписка стартует через `ComponentsForDictionaryFlowHandler` с `@AssistedInject @Assisted dictionaryId: Long` (#44 финальная версия), который вызывает `subscribe(scope, send)` на init Mate — никакого `SubscribeForDictionary` Effect не нужно.

#### #43 DatasourceEffectHandler.kt [+]

Зеркально #33. Подписка на flow стартует не через Effect, а через `ComponentsForDictionaryFlowHandler.subscribe()` на init Mate (см. #44).

```kotlin
package me.apomazkin.per_dictionary_components.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import javax.inject.Inject

class DatasourceEffectHandler @Inject constructor(
    private val useCase: PerDictionaryComponentsUseCase,
    private val logger: LexemeLogger,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        val msg: Msg = withContext(Dispatchers.IO) {
            try {
                when (effect) {
                    is DatasourceEffect.CreateComponent ->
                        Msg.CreateResult(
                            useCase.createUserDefinedComponent(
                                effect.name, effect.template, effect.isMulti, effect.scope,
                            )
                        )
                    is DatasourceEffect.RenameComponent ->
                        Msg.RenameResult(useCase.renameComponent(effect.typeId, effect.newName))
                    is DatasourceEffect.LoadImpact -> {
                        val impact = useCase.previewDeletionImpact(effect.typeId)
                        if (impact != null) Msg.ImpactPreviewLoaded(impact)
                        else Msg.ImpactPreviewFailed(IllegalStateException("preview null"))
                    }
                    is DatasourceEffect.SoftDeleteComponent ->
                        Msg.DeleteResult(useCase.softDeleteComponent(effect.typeId))
                }
            } catch (e: Exception) {
                logger.e(tag = LogTags.PER_DICT_COMPONENTS, message = "Effect failed: ${e.message}")
                when (effect) {
                    is DatasourceEffect.CreateComponent -> Msg.CreateResult(CreateOutcome.Failure(e))
                    is DatasourceEffect.RenameComponent -> Msg.RenameResult(RenameOutcome.Failure(e))
                    is DatasourceEffect.LoadImpact -> Msg.ImpactPreviewFailed(e)
                    is DatasourceEffect.SoftDeleteComponent -> Msg.DeleteResult(DeleteOutcome.Failure(e))
                }
            }
        }
        consumer(msg)
    }
}
```

#### #44 ComponentsForDictionaryFlowHandler.kt [+]

`MateFlowHandler` с динамическим `dictionaryId` (assisted-inject) — стартует подписку через `subscribe(scope, send)` при init Mate, без отдельного Effect.

```kotlin
package me.apomazkin.per_dictionary_components.logic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import javax.inject.Inject

class ComponentsForDictionaryFlowHandler @Inject constructor(
    private val useCase: PerDictionaryComponentsUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    /** runEffect не используется — подписка стартует через subscribe(scope, send) на init Mate. */
    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        // no-op: подписка инициируется в subscribe(scope, send) с assisted dictionaryId
    }

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        // Подписка стартует на init Mate; `dictionaryId` приходит через assisted-inject
        // (финальная форма — см. блок "Решение по умолчанию" ниже). Отдельный Effect не требуется.
    }

    /**
     * Активация через явный effect-input. Конкретный API `MateFlowHandler`
     * фиксируется в business_implement; здесь — псевдокод подписки на UseCase.
     */
    fun startCollect(scope: CoroutineScope, dictionaryId: Long, send: (Msg) -> Unit) {
        job?.cancel()
        job = scope.launch {
            useCase.flowComponentsForDictionary(dictionaryId)
                .catch { e ->
                    logger.e(tag = LogTags.PER_DICT_COMPONENTS, message = "flow failed: ${e.message}")
                    send(Msg.ItemsLoadFailed(e))
                }
                .collectLatest { snapshot ->
                    send(Msg.ItemsLoaded(snapshot))
                }
        }
    }
}
```

Note: `MateFlowHandler` (`:modules:core:mate/.../MateFlowHandler.kt`) определяет `subscribe(scope, send)` без параметров — `dictionaryId` нужно прокинуть либо через ctor (assisted-инжект ViewModel'ом), либо через явный init-call (`startCollect`). Финальный стиль (assisted vs explicit init effect handling) — нюансировка `business_implement`. Узел графа фиксирует поведение, не конкретный API contract `MateFlowHandler`.

**Решение по умолчанию (business_implement):** `@AssistedInject` constructor с `@Assisted dictionaryId: Long`; ViewModel создаёт handler через factory и передаёт его в `effectHandlerSet`; `subscribe(scope, send)` запускает collect с заранее известным `dictId`.

```kotlin
class ComponentsForDictionaryFlowHandler @AssistedInject constructor(
    @Assisted private val dictionaryId: Long,
    private val useCase: PerDictionaryComponentsUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {
    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {}
    override var job: Job? = null
    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            useCase.flowComponentsForDictionary(dictionaryId)
                .catch { e -> send(Msg.ItemsLoadFailed(e)) }
                .collectLatest { send(Msg.ItemsLoaded(it)) }
        }
    }
    @AssistedFactory
    interface Factory { fun create(dictionaryId: Long): ComponentsForDictionaryFlowHandler }
}
```

#### #45 PerDictionaryComponentsNavigationEffect.kt [+]

Зеркально #35.

#### #46 PerDictionaryComponentsNavigationEffectHandler.kt [+]

Зеркально #36, но navigator — `PerDictionaryComponentsNavigator` (infra id 8).

#### #47 PerDictionaryComponentsReducer.kt [+]

Зеркально #37 со следующими отличиями:
- `Msg.ItemsLoaded(snapshot)` → `state.copy(items = snapshot.toPerDictRows(state.dictionaryId), dictionaryName = snapshot.dictionaryName, isLoading = false)`.
- `Msg.OpenCreateDialog` инициализирует `scope = Scope.PerDictionaries(listOf(state.dictionaryId))` (preselect текущий словарь, см. `business_contract_spec.md` § PerDictionaryComponentsScreenState).
- В остальном reducer-ветки совпадают.

```kotlin
// фрагмент:
Msg.OpenCreateDialog ->
    state.copy(
        createDialog = CreateDialogState(
            scope = Scope.PerDictionaries(listOf(state.dictionaryId)),
        )
    ) to emptySet()
```

#### #48 PerDictionaryComponentsViewModel.kt [+]

```kotlin
package me.apomazkin.per_dictionary_components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.per_dictionary_components.logic.ComponentsForDictionaryFlowHandler
import me.apomazkin.per_dictionary_components.logic.DatasourceEffectHandler
import me.apomazkin.per_dictionary_components.logic.Msg
import me.apomazkin.per_dictionary_components.logic.PerDictionaryComponentsReducer
import me.apomazkin.per_dictionary_components.logic.PerDictionaryComponentsScreenState
import me.apomazkin.per_dictionary_components.logic.UiEffectHandler

class PerDictionaryComponentsViewModel @AssistedInject constructor(
    @Assisted dictionaryId: Long,
    @Assisted navigator: PerDictionaryComponentsNavigator,
    datasourceHandler: DatasourceEffectHandler,
    flowHandlerFactory: ComponentsForDictionaryFlowHandler.Factory,
    uiHandler: UiEffectHandler,
    navHandlerFactory: PerDictionaryComponentsNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<PerDictionaryComponentsScreenState, Msg> {

    private val stateHolder = Mate(
        initState = PerDictionaryComponentsScreenState(
            dictionaryId = dictionaryId,
            isLoading = true,
        ),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = PerDictionaryComponentsReducer(),
        effectHandlerSet = setOf(
            datasourceHandler,
            flowHandlerFactory.create(dictionaryId),
            uiHandler,
            navHandlerFactory.create(navigator),
        ),
    )

    override val state: StateFlow<PerDictionaryComponentsScreenState> = stateHolder.state
    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(
            dictionaryId: Long,
            navigator: PerDictionaryComponentsNavigator,
        ): PerDictionaryComponentsViewModel
    }
}
```

### Tier 7: Migration call-sites M12 → M13 (`ComponentValueData → TemplateValues`)

> Compile-time gates после удаления `ComponentValueData.kt` (#6). Каждый узел — конвертация конкретных call-site'ов на `TemplateValues`.
>
> Полный список call-sites (`grep -rln ComponentValueData --include="*.kt"`, exclude build/): app/mapper/LexemeMapper.kt (#52), app/di/module/wordCard/WordCardUseCaseImpl.kt (#49), modules/screen/wordcard/.../deps/WordCardUseCase.kt (#50), modules/screen/wordcard/.../mate/DatasourceEffectHandler.kt (#51), modules/screen/quiz/chat/.../quiz/QuizGameImpl.kt (#53), modules/domain/lexeme/Lexeme.kt (#54 — только `@Deprecated` doc text, не compile fail, но обновить ради чистоты). Data layer (`core/core-db-impl/CoreDbApiImpl.kt`, `entity/ComponentValueDb.kt`, `entity/ComponentValueWithType.kt`, `mapper/ComponentValueDataJson.kt`) — обрабатывает `data_design_tree`. Тесты (`*Test.kt`) — `business_test` фаза.

#### #49 WordCardUseCaseImpl.kt [~]

`app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt` — несколько call-site'ов `ComponentValueData.TextValue(...)`:

**Было:**
```kotlin
import me.apomazkin.lexeme.ComponentValueData
// ...
data = ComponentValueData.TextValue(translation.trim()),
```

**Стало:**
```kotlin
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
// ...
data = TextValues(value = Primitive.Text(translation.trim())),
```

Все call-site'ы `ComponentValueData.TextValue(s)` → `TextValues(Primitive.Text(s))`. Сигнатуры delegate-методов автоматически меняются после rebind `addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent` / `addComponentValue` / `updateComponentValue` в `CoreDbApi.LexemeApi` (#24).

#### #50 WordCardUseCase.kt [~]

`modules/screen/wordcard/.../deps/WordCardUseCase.kt` — interface contract публикуется screen-модулем; меняет тип `data: ComponentValueData → data: TemplateValues` во всех методах:
- `addLexemeWithBuiltInComponent`
- `addLexemeWithUserDefinedComponent`
- `addComponentValue`
- `updateComponentValue`

(см. `WordCardUseCase.kt:38-62`).

#### #51 WordCardUseCase DatasourceEffectHandler.kt [~]

`modules/screen/wordcard/.../mate/DatasourceEffectHandler.kt` — один call-site `ComponentValueData.TextValue(effect.definition)` (`:141`).

**Было:**
```kotlin
import me.apomazkin.lexeme.ComponentValueData
// ...
data = ComponentValueData.TextValue(effect.definition),
```

**Стало:**
```kotlin
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
// ...
data = TextValues(value = Primitive.Text(effect.definition)),
```

#### #52 LexemeMapper.kt [~]

`app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` — verified содержит 2 call-site'а `ComponentValueData.TextValue` (lines 70, 75) → переписать на `Primitive.Text` / `TemplateValues.TextValues`. ДОПОЛНИТЕЛЬНО: обновить `ComponentTypeApiEntity.toDomain()` (lines ~24-32) под field rename `removeDate → removedAt` + новые поля `isMulti/createdAt/updatedAt` (синхронно с #7 domain + #19 api-entity).

**Было:**
```kotlin
import me.apomazkin.lexeme.ComponentValueData
// ...
?.let { (it.data as? ComponentValueData.TextValue)?.text }
```

**Стало:**
```kotlin
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
// ...
?.let { (it.data as? TextValues)?.value?.value }
```

Замена обеих веток (translation lookup line 70, definition lookup line 75) на pattern `(it.data as? TextValues)?.value?.value`. После #6 (delete `ComponentValueData.kt`) символ перестаёт существовать — compile gate ловит любые пропущенные call-sites через весь проект.

#### #53 QuizGameImpl.kt [~]

`modules/screen/quiz/chat/.../quiz/QuizGameImpl.kt` — verified содержит 2 call-site'а (lines 472-473):

**Было:**
```kotlin
import me.apomazkin.lexeme.ComponentValueData
// ...
val text = (source.data as? ComponentValueData.TextValue)?.text
    ?: (source.data as? ComponentValueData.LongTextValue)?.text
```

**Стало:**
```kotlin
import me.apomazkin.lexeme.TextValues
// ...
val text = (source.data as? TextValues)?.value?.value
// LongTextValue полностью упразднён (M13 template consolidation `long_text → text`, F046);
// fallback-ветка удаляется.
```

#### #54 Lexeme.kt [~]

`modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt` — содержит только `@Deprecated("Use ComponentValueData.TextValue via components")` text-references (lines 8, 12) на классах `Translation` / `Definition` value class.

**Было:**
```kotlin
@Deprecated("Use ComponentValueData.TextValue via components")
@JvmInline
value class Translation(val value: String)

@Deprecated("Use ComponentValueData.TextValue via components")
@JvmInline
value class Definition(val value: String)
```

**Стало:**
```kotlin
@Deprecated("Use TextValues via components")
@JvmInline
value class Translation(val value: String)

@Deprecated("Use TextValues via components")
@JvmInline
value class Definition(val value: String)
```

Чисто косметика; `@Deprecated` text не compile-fail при удалении референса, но обновление поддерживает консистентность doc'ов. После полного отказа от shim-полей `translation/definition` в `Lexeme` (backlog) узел станет [-]; сейчас фиксируется только string update.

### Tier 0 (independent): LogTags per module

> Один `LogTags.kt` на screen-модуль (convention аналог `wordcard/LogTags.kt`). Используется в Tier 4 UseCase impls и Tier 5/6 effect/flow handlers.

#### #55 LogTags.kt (components_manager) [+]

```kotlin
package me.apomazkin.components_manager

internal object LogTags {
    const val COMPONENTS_MANAGER = "ComponentsManager"
}
```

#### #56 LogTags.kt (per_dictionary_components) [+]

```kotlin
package me.apomazkin.per_dictionary_components

internal object LogTags {
    const val PER_DICT_COMPONENTS = "PerDictComponents"
}
```

## Часть 3: UI dependencies

UI-задачи для ui sub-flow. Реализация — в `ui_design_tree` / `ui_walkthrough` / `ui_layout`. Здесь — только контракт + локация.

- **ComponentsManagerScreen** (новый Composable, Tier 3 screen):
  - params: `state: ComponentsManagerScreenState`, `onMsg: (Msg) -> Unit`
  - location: `modules/screen/components_manager/src/main/.../ComponentsManagerScreen.kt`
  - rationale: главный экран глобального менеджера компонентов; рисует list + dialogs.

- **PerDictionaryComponentsScreen** (новый Composable, Tier 3 screen):
  - params: `state: PerDictionaryComponentsScreenState`, `onMsg: (Msg) -> Unit`
  - location: `modules/screen/per_dictionary_components/src/main/.../PerDictionaryComponentsScreen.kt`
  - rationale: per-dictionary view; такой же CRUD сюжет с preselect scope.

- **UserDefinedRowWidget** (новый Composable / list-item):
  - params: `row: UserDefinedRow`, `onRename: (ComponentTypeId) -> Unit`, `onDelete: (ComponentTypeId) -> Unit`
  - location: `modules/screen/components_manager/src/main/.../widget/`
  - rationale: рендер row с usage badge + dictionaryNames.

- **PerDictRowWidget** (новый Composable / list-item):
  - params: `row: PerDictRow`, `onRename`, `onDelete`
  - location: `modules/screen/per_dictionary_components/src/main/.../widget/`
  - rationale: рендер row с `isGlobal` badge + valueCount.

- **CreateComponentDialog** (новый Composable, dialog):
  - params: `state: CreateDialogState`, `availableDictionaries: List<Dictionary>`, `isCreating: Boolean`, `onMsg: (Msg) -> Unit`
  - location: разделить дублированно в каждом screen-модуле (Open Q #1 resolution: дублирование) либо общий widget в `modules/widget/component_widgets/` — финал на `ui_design_tree`.
  - rationale: name input + template selector + multi toggle + scope picker (Global / multiselect dicts) + submit с nameError display.

- **RenameComponentDialog** (новый Composable, dialog):
  - params: `state: RenameDialogState`, `isRenaming: Boolean`, `onMsg`
  - location: симметрично CreateComponentDialog.
  - rationale: text input + nameError display.

- **DeleteConfirmDialog** (новый Composable, dialog):
  - params: `state: DeleteConfirmState`, `isDeleting: Boolean`, `onMsg`
  - location: симметрично.
  - rationale: impact preview (valueCount + dictionariesWithValues + affectedQuizConfigs + affectedPrefs) + confirm button (disabled при `isLoadingImpact`).

- **ComponentManageWidget** (settings tab entry):
  - params: `onClick: () -> Unit`
  - location: `modules/screen/settingstab/.../widgets/settings/items/` (existing pattern `LangManageWidget`).
  - rationale: новый settings-row с иконкой/title, dispatch `Msg.OpenComponentsManager` (settings tab Msg уже расширен на infra-шаге, infra id 11).

- **HammerIconButton** (`DictionaryAppBar` actions slot):
  - params: `currentDictionaryId: Long?`, `onClick: (Long) -> Unit`
  - location: `modules/widget/dictionaryappbar/.../widget/`
  - rationale: новый icon-button «молоток», visible только при `currentDictionaryId != null`; dispatch `Msg.OpenPerDictionaryComponents(dictId)` (infra id 15).

- **TemplatePreview composables** (Tier 2 widget):
  - params: `template: ComponentTemplate`, `values: TemplateValues`
  - location: `modules/widget/component_widgets/.../preview/` (infra id 2 module).
  - rationale: дефолтные preview-composables per `ComponentTemplate` (TEXT preview / IMAGE preview). Используются в CreateComponentDialog и (в будущих фичах) в quiz items.

---

## Audit checklist

### 1. Каждый ContractDomain тип имеет узел

| ContractDomain тип (из business_contract_spec) | Узел DAG | Status |
|---|---|---|
| `Primitive` | #1 | ok |
| `PrimitiveType` | #2 | ok |
| `Field` | #3 | ok |
| `ComponentTemplate` (M13) | #4 | ok (modify) |
| `TemplateValues` | #5 | ok |
| `ComponentValueData` (delete) | — | moved to data_design_tree (см. § Перенос) |
| `ComponentType` (M13) | #7 | ok (modify) |
| `ComponentValue` (rebind data) | #8 | ok (modify) |
| `Scope` | #9 | ok |
| `NameError` | #10 | ok |
| `AffectedQuizConfig` | #11 | ok |
| `DeletionImpact` | #12 | ok |
| `ComponentUsage` | #13 | ok |
| `UserDefinedTypesSnapshot` | #14 | ok |
| `PerDictionarySnapshot` | #15 | ok |
| `CreateOutcome` | #16 | ok |
| `RenameOutcome` | #17 | ok |
| `DeleteOutcome` | #18 | ok |

### 2. Каждый UseCase метод имеет соответствие в DAG

`ComponentsManagerUseCase`:

| Method | Узел interface | Узел impl | Status |
|---|---|---|---|
| `flowAllUserDefinedTypes()` | #25 | #27 | ok |
| `createUserDefinedComponent(name, template, isMulti, scope)` | #25 | #27 | ok |
| `renameComponent(typeId, newName)` | #25 | #27 | ok |
| `previewDeletionImpact(typeId)` | #25 | #27 | ok |
| `softDeleteComponent(typeId)` | #25 | #27 (+ prefs reset) | ok |

`PerDictionaryComponentsUseCase`:

| Method | Узел interface | Узел impl | Status |
|---|---|---|---|
| `flowComponentsForDictionary(dictId)` | #26 | #28 | ok |
| `createUserDefinedComponent(...)` | #26 | #28 (delegation to #27) | ok |
| `renameComponent(...)` | #26 | #28 (delegation) | ok |
| `previewDeletionImpact(...)` | #26 | #28 (delegation) | ok |
| `softDeleteComponent(...)` | #26 | #28 (delegation) | ok |

`CoreDbApi.LexemeApi` новые методы:

| Method | Узел | Status |
|---|---|---|
| `flowAllUserDefinedTypesWithUsage()` | #24 | ok |
| `flowUserDefinedTypesForDictionary(dictId)` | #24 | ok |
| `createUserDefinedComponent(...)` | #24 | ok |
| `renameComponentType(typeId, newName)` | #24 | ok |
| `previewDeletionImpact(typeId)` | #24 | ok |
| `softDeleteComponentType(typeId)` | #24 | ok |

`CoreDbApi.LexemeApi` сигнатуры (BREAKING): #24 покрывает `addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent` / `addLexemeWithComponents` / `addComponentValue` / `updateComponentValue` — все 5 переведены на `TemplateValues`. Call-sites: #49, #50, #51 (wordcard), #52 (app mapper), #53 (quiz/chat), #54 (Lexeme.kt deprecation text). Data-side call-sites (`CoreDbApiImpl`, `ComponentValueDb`, `ComponentValueWithType`, `ComponentValueDataJson`) — `data_design_tree`. Тесты — `business_test`.

### 3. Каждая Msg sealed-ветка имеет Reducer-узел

`ComponentsManagerScreen` Msg → Reducer #37: все 20+ веток (Lifecycle / Create / Rename / Delete / Navigation / Empty / UiMsg) разобраны exhaustive `when`. Compile-time gate: kotlinc fails если ветка пропущена (sealed interface).

`PerDictionaryComponentsScreen` Msg → Reducer #47: зеркально, плюс init-`Scope.PerDictionaries(listOf(state.dictionaryId))` на `OpenCreateDialog`.

### 4. Cascade и инварианты отражены

- **`userdefined_identity_invariant`** (cross-scope) → `CoreDbApi.createUserDefinedComponent` two-prong SELECT (#24 docstring + impl на data side); domain `CreateOutcome.CrossScopeCollision` (#16); reducer reaction в #37/#47.
- **`soft_delete_unique_collision`** (same-scope) → `SameScopeCollision` branch в #16/#17/#23; reducer #37/#47.
- **`quiz_configs_cleanup` (rename + soft-delete cascade)** → docstring `renameComponentType` / `softDeleteComponentType` (#24); impl в data sub-flow.
- **`prefs_cleanup_on_soft_delete`** → `ComponentsManagerUseCaseImpl.resetQuizPickerPrefsFor` (#27, option B); `DeletionImpact.affectedPrefs` (#12).
- **`[transition] double-tap guard`** → `state.isDeleting/isCreating/isRenaming` early-return в #37/#47.
- **`[shape] one dialog at a time`** → инвариант enforce'ится через `Open*Dialog` логику reducer'а: `OpenCreateDialog` сбрасывает `createDialog` to non-null, остальные dialogs остаются `null` (см. #37). Дополнительный assertion-helper можно добавить в `business_implement`.
- **`forward_compat_unknown`** (`ComponentTemplate.fromKey: nullable`) → #4 docstring; call-sites обрабатывают `null` (data sub-flow финализирует поведение mapper'а).
- **`UNIQUE removed from DB → enforced in UseCase`** → docstring `createUserDefinedComponent` (#24); two-prong SELECT в data sub-flow реализации.

### 5. Layer boundary check

Все 52 узла графа — бизнес-слой (`modules/domain/lexeme/`, `core/core-db-api/`, `modules/screen/.../logic/`, `modules/screen/.../deps/`, `modules/screen/.../mate/`, `app/.../di/module/...UseCaseImpl.kt`, `app/.../mapper/`). Ни одного узла на `@Composable` / `widget/` / `composables/` / `core/ui/` / ResIds. UI-задачи задекларированы в § Часть 3 `## UI dependencies` для ui sub-flow.

### 6. Циклы / порядок

- Tier 0 листья без зависимостей (#1, #2, #9, #10, #11) + цепочка domain types (#3 → #4 → #5, #4 → #7 → #8, #6 после #5).
- Tier 1 (domain shared) — зависит от Tier 0 domain types (#7, #12, #15).
- Tier 2 (data API) — зависит от domain (#5, #19, #20).
- Tier 3 (UseCase interfaces) — зависит от Tier 1 (#25 от #14/#16/#17/#18; #26 от #15/#16/#17/#18).
- Tier 4 (impls) — зависит от Tier 2 API (#24) + Tier 3 interfaces.
- Tier 5/6 (Mate) — независимые ветки, обе зависят от domain (#4, #9, #12) + UseCase interfaces (#25/#26).
- Tier 7 (migration) — листья компиляции, зависят только от Tier 0 (#5) + Tier 2 (#20).

Циклов нет. Tier 5 и Tier 6 могут выполняться параллельно после Tier 4.

> **Внимание (после iter 5 audit):** Tier-метки используются для **концептуальной группировки**, не для определения порядка выполнения. Фактический DAG-порядок диктуется явными `depends`. Пример: #52 (Tier 7 — LexemeMapper update) — dep для #27/#28 (Tier 4 — UseCase impls). Это означает что #52 должен выполняться **до** #27/#28. Узлы Tier 7 — не строго «листья», некоторые из них являются предками для Tier 4.

---

## Перенос в data_design_tree

Узел «delete `ComponentValueData.kt`» (бывший #6) перенесён в `data_design_tree` как финальный узел (после всех data-side migrations). business_design_tree.md в Tier 7 содержит только business-side migrations (#49-#54); data-side обработает удаление символа.

Обоснование: символ `ComponentValueData` используется на data-стороне (`core/core-db-impl/CoreDbApiImpl.kt`, `entity/ComponentValueDb.kt`, `entity/ComponentValueWithType.kt`, `mapper/ComponentValueDataJson.kt`) — удаление файла невозможно до конвертации всех data-side call-sites на `TemplateValues`. Поэтому delete-action принадлежит data_design_tree и должен быть финальным узлом во всём IS481 DAG.

---

## Открытые вопросы (для business_implement)

1. **`MateFlowHandler` API для dynamic `dictionaryId`** (Tier 6 #44) — `subscribe(scope, send)` без параметров vs `@AssistedInject` ctor; решено `@AssistedInject` + factory в ViewModel. Финал — на business_implement если упрётся в Dagger nuance.
2. **Dialog-state дублирование vs `:modules:widget:component_widgets/` shared** — Open Q #1 (`business_contract`) решён в пользу дублирования в screen-модулях. Если на implement-фазе обнаружится явный winner shared (UI cohesion), backlog запись.
3. **`UserDefinedTypesSnapshot.toRows()` / `PerDictionarySnapshot.toPerDictRows()` helper-расширения** — финализация в State.kt либо в отдельном `*Mapper.kt` (screen-package). business_implement выбирает по convention.

---

## История ревью

### iter 1 (2026-06-16) — 7 findings

- F082 (critical): #28 missing dep on #27 → added.
- F083 (critical): #6 (delete ComponentValueData) перенесён в data_design_tree.
- F084 (critical): SubscribeForDictionary удалён из #42 sealed (dead Effect).
- F085 (critical): добавлены 2 узла LogTags.kt (CM + PD).
- F086 (minor): UiMsg top-level подтверждён; spec обновлён убрав вложенность.
- F087 (minor): #29 и #39 расширены extension helpers toRows() / toPerDictRows().
- F088 (minor): SubscribeAll удалён из spec.

### iter 2 (2026-06-16): 7 findings fixed.

### iter 3 (2026-06-16): F089/F090/F091 fixed

- F089: #29 depends + 14
- F090: #39 depends + 15
- F091: spec — удалён SubscribeForDictionary из PerDictionaryComponentsDatasourceEffect

### iter 4 (2026-06-16): F092 fixed

- #52 (LexemeMapper.kt): scope расширен — включает ComponentTypeApiEntity.toDomain() field rename; depends + [7, 19].

### iter 5 (2026-06-16): F093/F094 + systematic depends audit

#### Точечные fix
- #37 depends + 31 (F093 — Reducer emits `UiEffect.Snackbar`, sealed declared в #31).
- #47 depends + 41 (F094 — зеркальный паттерн PerDictionary).

#### Systematic audit results

Прошёл по всем 55 узлам, проверил depends на полноту символов которые узел использует (sealed/data class/interface/extension/object/typealias). Pre-existing types (`ComponentTypeId`, `BuiltInComponent`, `LexemeId`, `ComponentTypeRef`, `Date`, `MateReducer`/`Effect`/`MateFlowHandler`/`MateTypedEffectHandler`/`NavigationEffect`/`Mate`/`MateStateHolder`, `LexemeLogger`, `PrefsProvider`, `Composable`/`Stable`, `Navigator`-stubs) — вне DAG, не учитывались.

**Missing deps обнаружены и исправлены:**

- **#4** depends [3] → [2, 3]. Reason: `fields` getter использует `PrimitiveType.TEXT/IMAGE` (node #2) напрямую, transitive через #3→#2 недостаточно для explicit symbol audit.
- **#24** depends [5, 9, 19, 20, 21, 22, 23] → [4, 5, 9, 12, 19, 20, 21, 22, 23]. Reason: сигнатура `createUserDefinedComponent(...template: ComponentTemplate...)` → #4; return `previewDeletionImpact(...): DeletionImpact?` → #12.
- **#25** depends [14, 16, 17, 18] → [4, 9, 12, 14, 16, 17, 18]. Reason: imports `ComponentTemplate` (#4), `Scope` (#9), `DeletionImpact` (#12) в сигнатурах методов интерфейса.
- **#26** depends [15, 16, 17, 18] → [4, 9, 12, 15, 16, 17, 18]. Reason: симметрично #25 — `ComponentTemplate`/`Scope`/`DeletionImpact` в сигнатурах.
- **#27** depends [24, 25, 55] → [4, 9, 12, 13, 14, 16, 17, 18, 23, 24, 25, 52, 55]. Reason (impl с rich body): использует `ComponentTemplate`/`Scope` в overrides (#4/#9); конструирует `DeletionImpact`/`ComponentUsage`/`UserDefinedTypesSnapshot` (#12/#13/#14); branch на `CreateOutcome`/`RenameOutcome`/`DeleteOutcome` (#16/#17/#18); branch на `CreateComponentOutcome`/`RenameComponentOutcome`/`SoftDeleteComponentOutcome` (#23); вызывает обновлённый `ComponentTypeApiEntity.toDomain()` (#52 — это modified mapper).
- **#28** depends [24, 26, 27] → [4, 9, 12, 15, 16, 17, 18, 24, 26, 27, 52]. Reason: симметрично #27 — direct symbols `ComponentTemplate`/`Scope`/`DeletionImpact`/`PerDictionarySnapshot`/`Create|Rename|DeleteOutcome` + обновлённый `toDomain()` (#52).
- **#33** depends [25, 30, 32, 55] → [16, 17, 18, 25, 30, 32, 55]. Reason: catch-блок конструирует `CreateOutcome.Failure(e)` / `RenameOutcome.Failure(e)` / `DeleteOutcome.Failure(e)` — direct symbol use.
- **#37** depends [29, 30, 31, 32, 35] → [10, 16, 17, 18, 29, 30, 31, 32, 35]. Reason: branch на `CreateOutcome`/`RenameOutcome`/`DeleteOutcome` sealed (#16/#17/#18); ссылается `NameError.Empty/SameScopeCollision/CrossScopeCollision` (#10) при error mapping в dialog state. (Базовый F093 fix уже учтён — +31.)
- **#43** depends [26, 40, 42, 56] → [16, 17, 18, 26, 40, 42, 56]. Reason: симметрично #33.
- **#47** depends [39, 40, 41, 42, 45] → [9, 10, 16, 17, 18, 39, 40, 41, 42, 45]. Reason: симметрично #37 + дополнительно использует `Scope.PerDictionaries(listOf(state.dictionaryId))` при `OpenCreateDialog` (#9). (Базовый F094 fix уже учтён — +41.)
- **#49** depends [5, 20, 24] → [1, 5, 20, 24]. Reason: call-site `TextValues(value = Primitive.Text(translation.trim()))` — direct symbol `Primitive` из #1.
- **#51** depends [5, 50] → [1, 5, 50]. Reason: симметрично #49 — `Primitive.Text(effect.definition)`.
- **#52** depends [5, 7, 19, 20] → [1, 5, 7, 19, 20]. Reason: использует `Primitive` (либо явно `Primitive.Text(...)`, либо через `TextValues(Primitive.Text)` reconstruction в обоих веток translation/definition lookup).

**Missing nodes:** не обнаружены. Все референсы либо pre-existing (вне graph scope), либо покрыты существующими узлами.

**Все остальные узлы (#1, #2, #3, #5, #7, #8, #9, #10, #11, #12, #13, #14, #15, #16, #17, #18, #19, #20, #21, #22, #23, #29, #30, #31, #32, #34, #35, #36, #38, #39, #40, #41, #42, #44, #45, #46, #48, #50, #53, #54, #55, #56)** — depends корректны (verified).

### iter 6 (2026-06-16): F095/F096/F097 fixed

- F095: #37 depends -35; #47 depends -45 (spurious — sealed без variants).
- F096: #29 depends +7; #39 depends +7 (toRows/toPerDictRows используют ComponentType.isMulti).
- F097: tier narrative дополнен — Tier-метки = концептуальные, порядок = depends.

### iter 7 (2026-06-16): F098 fixed

- #37, #47: BaseNavigationEffect.Back → NavigationEffect.Back; import me.apomazkin.mate.NavigationEffect (canonical из MateNavigationEffectHandler.kt:15, WordCardReducer.kt:248). Дезориентирующие fallback-заметки удалены.

## log_messages

- iter 1: создан DAG из 54 узлов, 7 tiers; покрытие contract spec проверено по audit checklist
- iter 2: 7 findings fixed; узел count: 54 - 1 (#6 → data_design_tree) + 2 (#55/#56 LogTags) = 55 узлов
- 5 узлов на domain core (M13: Primitive/Field/PrimitiveType/ComponentTemplate/TemplateValues), 10 на domain shared shapes для конструктора, 6 на расширение CoreDbApi (включая 5 BREAKING сигнатур), 2 на UseCase interfaces + 2 impls (с делегированием), 10×2 на Mate-обвязку двух экранов, 6 на migration call-sites (wordcard + app mapper + quiz/chat + Lexeme.kt deprecation text)
- 38 [+] новых файлов, 15 [~] modify, 1 [-] delete (`ComponentValueData.kt`); циклов нет
- Open Q #1 (Dialog dup) и #2 (UseCase delegation) closed в DAG (#39 dup + #28 delegation)
- UI dependencies задекларированы в § Часть 3 (9 composables); ни один UI-узел в графе
- iter 1 followup: добавлены #53 (QuizGameImpl) и #54 (Lexeme.kt deprecation text) после `grep ComponentValueData --include="*.kt"` audit — пропуски ловятся compile gate после удаления `ComponentValueData.kt`

_model: claude-opus-4-7[1m]_
