# Business design tree: IS481 Lexeme Component Constructor

Граф файлов для реализации **business слоя** IS481. Каждый узел — конкретный файл
с действием `[+]` create / `[~]` modify / `[-]` delete + зависимостями.

**Scope:** только business слой по `business_contract.md`. Data (DAO/Entity/Migration),
UI (chip visibility), tests — отдельные sub-flow.

**Verification:** все `[~]` / `[-]` узлы verified через `Read` (см. `business_walkthrough.md`).

---

## Часть 1: Граф

```yaml
# ============================================================================
# Layer 0 — Domain types (modules/domain/lexeme)
# Pure-JVM, fundament всего IS481.
# ============================================================================

- id: 1
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/BuiltInComponent.kt
  action: "+"
  depends: []

- id: 2
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTemplate.kt
  action: "+"
  depends: []

- id: 3
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt
  action: "+"
  depends: []

- id: 4
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentType.kt
  action: "+"
  depends: [1, 2]

- id: 5
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValue.kt
  action: "+"
  depends: [3, 4]

- id: 6
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTypeRef.kt
  action: "+"
  depends: [1]

- id: 7
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/QuizConfig.kt
  action: "+"
  depends: [6]

- id: 8
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/LexemeBuiltInExt.kt
  action: "+"
  depends: [1, 5, 9]

- id: 9
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt
  action: "~"
  depends: [5]

# ============================================================================
# Layer 1 — Gradle dep edge (MIN-2)
# core-db-api начинает depend on modules/domain/lexeme.
# ============================================================================

- id: 10
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/build.gradle.kts
  action: "~"
  depends: []

# ============================================================================
# Layer 2 — API DTOs (core-db-api)
# ============================================================================

- id: 11
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentTypeApiEntity.kt
  action: "+"
  depends: [1, 2, 10]

- id: 12
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentValueApiEntity.kt
  action: "+"
  depends: [3, 10, 11]

- id: 13
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/QuizConfigApiEntity.kt
  action: "+"
  depends: [6, 10]

- id: 14
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/LexemeApiEntity.kt
  action: "~"
  depends: [12]

- id: 15
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt
  action: "~"
  depends: [11, 13, 14]

# ============================================================================
# Layer 3 — Wordcard domain entity Term (dictionaryId field)
# ============================================================================

- id: 16
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Term.kt
  action: "~"
  depends: []

# ============================================================================
# Layer 4 — UseCase interfaces (wordcard / quiz-chat)
# ============================================================================

- id: 17
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt
  action: "~"
  depends: [1, 3, 4, 5, 9, 16]

- id: 18
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt
  action: "~"
  depends: [7]

# ============================================================================
# Layer 5 — Mappers in app/
# ============================================================================

- id: 19
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt
  action: "~"
  depends: [1, 3, 4, 5, 7, 9, 11, 12, 13, 14]

# ============================================================================
# Layer 6 — UseCase impls (app/)
# ============================================================================

- id: 20
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt
  action: "~"
  depends: [1, 3, 4, 5, 9, 15, 16, 17, 19]

- id: 21
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt
  action: "~"
  depends: [7, 15, 18, 19]

# ============================================================================
# Layer 7 — Mate state / msg / reducer / handler (wordcard)
# ============================================================================

- id: 22
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt
  action: "~"
  depends: [9]

- id: 23
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt
  action: "~"
  depends: [4, 16]

- id: 24
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/WordCardReducer.kt
  action: "~"
  depends: [22, 23]

- id: 25
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/DatasourceEffectHandler.kt
  action: "~"
  depends: [3, 17, 23]

# ============================================================================
# Layer 8 — Quiz session wire (QuizGameImpl)
# ============================================================================

- id: 26
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt
  action: "~"
  depends: [7, 18]
```

---

## Часть 2: Детали изменений

### Node 1 `[+]` — `BuiltInComponent.kt`

**Назначение:** domain enum для built-in component types. После AGG-1 содержит
только `TRANSLATION` (definition мигрирует в user-defined per-dictionary).

```kotlin
package me.apomazkin.lexeme

enum class BuiltInComponent(val key: String) {
    TRANSLATION("translation");

    companion object {
        fun fromKey(key: String): BuiltInComponent? = entries.firstOrNull { it.key == key }
    }
}
```

---

### Node 2 `[+]` — `ComponentTemplate.kt`

**Назначение:** domain enum шаблонов содержимого компонентов.

```kotlin
package me.apomazkin.lexeme

enum class ComponentTemplate(val key: String) {
    TEXT("text"),
    LONG_TEXT("long_text"),
    IMAGE("image");

    companion object {
        /** Unknown key → fallback TEXT (forward-compat). Non-null контракт. */
        fun fromKey(key: String): ComponentTemplate = entries.firstOrNull { it.key == key } ?: TEXT
    }
}
```

---

### Node 3 `[+]` — `ComponentValueData.kt`

**Назначение:** sealed shape данных компонента. Variant — по `ComponentType.template`
parent-сущности. Суффикс `Value` (избегаем конфликта с `androidx.compose.material3.Text`).

```kotlin
package me.apomazkin.lexeme

sealed interface ComponentValueData {
    data class TextValue(val text: String) : ComponentValueData
    data class LongTextValue(val text: String) : ComponentValueData
    data class ImageValue(val uri: String) : ComponentValueData
}
```

---

### Node 4 `[+]` — `ComponentType.kt`

**Назначение:** domain entity типа компонента + value-class ID.

```kotlin
package me.apomazkin.lexeme

import java.util.Date

@JvmInline value class ComponentTypeId(val id: Long)

data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,   // null → user-defined
    val dictionaryId: Long?,            // null → global
    val name: String?,                  // user-defined: required; built-in: opt override
    val template: ComponentTemplate,
    val position: Int,
    val removeDate: Date? = null,
)
```

---

### Node 5 `[+]` — `ComponentValue.kt`

**Назначение:** domain entity значения компонента + value-class ID. `type` — full
embedded `ComponentType` (multi-level @Relation).

```kotlin
package me.apomazkin.lexeme

@JvmInline value class ComponentValueId(val id: Long)

data class ComponentValue(
    val id: ComponentValueId,
    val lexemeId: LexemeId,
    val type: ComponentType,
    val data: ComponentValueData,
)
```

---

### Node 6 `[+]` — `ComponentTypeRef.kt`

**Назначение:** sealed для quiz config — ссылка на тип компонента по stable identity.

```kotlin
package me.apomazkin.lexeme

/**
 * TODO: вынести в `modules/domain/quiz` в рамках backlog-фичи «Quiz config UX».
 * Сейчас живёт в lexeme domain как trade-off (AGG-10).
 */
sealed interface ComponentTypeRef {
    @JvmInline value class BuiltIn(val key: BuiltInComponent) : ComponentTypeRef
    @JvmInline value class UserDefined(val name: String) : ComponentTypeRef
}
```

---

### Node 7 `[+]` — `QuizConfig.kt`

**Назначение:** domain entity конфига квиза. `componentRefs` — упорядоченный
список ссылок на типы компонентов используемые в quiz сессии.

```kotlin
package me.apomazkin.lexeme

/**
 * TODO: вынести в `modules/domain/quiz` (AGG-10, backlog «Quiz config UX»).
 */
data class QuizConfig(
    val dictionaryId: Long,
    val quizMode: String,
    val componentRefs: List<ComponentTypeRef>,
)
```

---

### Node 8 `[+]` — `LexemeBuiltInExt.kt`

**Назначение:** extension для built-in lookup. Type-safe через enum, единый источник.

```kotlin
package me.apomazkin.lexeme

fun Lexeme.builtIn(key: BuiltInComponent): ComponentValue? =
    components.firstOrNull { it.type.systemKey == key }
```

---

### Node 9 `[~]` — `Lexeme.kt`

**Было** (`modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt`,
verified Read):
```kotlin
data class Lexeme(
    val lexemeId: LexemeId,
    val translation: Translation?,
    val definition: Definition?,
    val addDate: Date,
    val changeDate: Date? = null,
)
@JvmInline value class Translation(val value: String)
@JvmInline value class Definition(val value: String)
```

**Стало** (B4/C2 shim):
```kotlin
data class Lexeme(
    val lexemeId: LexemeId,
    val components: List<ComponentValue>,                                  // ← new SoT
    @Deprecated("Use components") val translation: Translation? = null,    // shim
    @Deprecated("Use components") val definition: Definition? = null,      // shim
    val addDate: Date,
    val changeDate: Date? = null,
)
@Deprecated("Use ComponentValueData.TextValue") @JvmInline value class Translation(val value: String)
@Deprecated("Use ComponentValueData.TextValue") @JvmInline value class Definition(val value: String)
```

Псевдокод: `components` упорядочен по `ComponentType.position`. shim-поля
заполняются маппером `LexemeApiEntity.toDomain()` (node 19).

---

### Node 10 `[~]` — `core/core-db-api/build.gradle.kts`

**Было** (verified Read):
```kotlin
dependencies {
    implementation(kotlinLibs.coroutinesCore)
    implementation(datastoreLibs.paging)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
```

**Стало** (MIN-2):
```kotlin
dependencies {
    implementation(kotlinLibs.coroutinesCore)
    implementation(datastoreLibs.paging)
    implementation(project(":modules:domain:lexeme"))   // ← new dep edge
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
```

---

### Node 11 `[+]` — `ComponentTypeApiEntity.kt`

**Назначение:** API DTO для ComponentType. `systemKey` / `template` — enum'ы из
domain (data знает domain по A1).

```kotlin
package me.apomazkin.core_db_api.entity

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import java.util.Date

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

---

### Node 12 `[+]` — `ComponentValueApiEntity.kt`

**Назначение:** API DTO для ComponentValue. `type` — full embedded
`ComponentTypeApiEntity` (multi-level @Relation). `data` — sealed из domain.

```kotlin
package me.apomazkin.core_db_api.entity

import me.apomazkin.lexeme.ComponentValueData

data class ComponentValueApiEntity(
    val id: Long,
    val lexemeId: Long,
    val type: ComponentTypeApiEntity,
    val data: ComponentValueData,
)
```

---

### Node 13 `[+]` — `QuizConfigApiEntity.kt`

**Назначение:** API DTO для QuizConfig. `componentRefs` — domain sealed (`core-db-api`
зависит от domain по A1/MIN-2).

```kotlin
package me.apomazkin.core_db_api.entity

import me.apomazkin.lexeme.ComponentTypeRef

data class QuizConfigApiEntity(
    val id: Long,
    val dictionaryId: Long,
    val quizMode: String,
    val componentRefs: List<ComponentTypeRef>,
)
```

---

### Node 14 `[~]` — `LexemeApiEntity.kt`

**Было** (verified Read):
```kotlin
@JvmInline value class TranslationApiEntity(val value: String)
@JvmInline value class DefinitionApiEntity(val value: String)

data class LexemeApiEntity(
    val id: Long,
    val translation: TranslationApiEntity? = null,
    val definition: DefinitionApiEntity? = null,
    val wordClass: String? = null,
    val options: Long = 0,
    val addDate: Date,
    val changeDate: Date? = null,
)

fun LexemeApiEntity.canRemoveTranslation() = definition != null
fun LexemeApiEntity.canRemoveDefinition() = translation != null
```

**Стало:** добавлено `components`, удалены `translation` / `definition` fields +
extensions `canRemove*` (AGG-6 — translation/definition больше не специальные
поля DTO, доступ через `components` либо domain shim).
```kotlin
@JvmInline value class TranslationApiEntity(val value: String)   // оставлены для @Deprecated overloads
@JvmInline value class DefinitionApiEntity(val value: String)

data class LexemeApiEntity(
    val id: Long,
    val components: List<ComponentValueApiEntity>,        // ← new SoT
    val wordClass: String? = null,
    val options: Long = 0,
    val addDate: Date,
    val changeDate: Date? = null,
)
// canRemoveTranslation / canRemoveDefinition extensions удалены
// (callsite WordCardUseCaseImpl переписан на generic deleteComponentValue → RemoveComponentResult).
```

---

### Node 15 `[~]` — `CoreDbApi.kt` (`LexemeApi` interface)

**Было** (verified Read, lines 77-120):
```kotlin
interface LexemeApi {
    suspend fun getLexemeById(id: Long): LexemeApiEntity?
    suspend fun addLexeme(wordId: Long): Long
    suspend fun addLexeme(wordId: Long, translation: TranslationApiEntity): Long
    suspend fun addLexeme(wordId: Long, definition: DefinitionApiEntity): Long
    suspend fun addLexemeWithTranslation(wordId, dictionaryId, translation: TranslationApiEntity): Long
    suspend fun addLexemeWithDefinition(wordId, dictionaryId, definition: DefinitionApiEntity): Long
    suspend fun updateLexemeTranslation(id, translation: TranslationApiEntity?): Long?
    suspend fun updateLexemeDefinition(id, definition: DefinitionApiEntity?): Long?
    suspend fun deleteLexeme(id: Long): Int
}
```

**Стало** (AGG-6 — удалены definition-wrappers; добавлены generic; translation
wrappers оставлены как `@Deprecated` shim A3):
```kotlin
interface LexemeApi {
    suspend fun getLexemeById(id: Long): LexemeApiEntity?
    suspend fun addLexeme(wordId: Long): Long
    suspend fun deleteLexeme(id: Long): Int

    // --- Generic API ---

    /** Atomic INSERT lexeme + write_quiz + первый built-in component_value. */
    suspend fun addLexemeWithBuiltInComponent(
        wordId: Long, dictionaryId: Long,
        systemKey: BuiltInComponent, data: ComponentValueData,
    ): Long

    /** Atomic INSERT lexeme + write_quiz + первый user-defined component_value.
     *  Lookup type по (dictionary_id, name, system_key=NULL). null if type not found. */
    suspend fun addLexemeWithUserDefinedComponent(
        wordId: Long, dictionaryId: Long,
        name: String, data: ComponentValueData,
    ): Long?

    /** Atomic compound INSERT lexeme + write_quiz + N component_values (MIN-9). */
    suspend fun addLexemeWithComponents(
        wordId: Long, dictionaryId: Long,
        components: List<Pair<ComponentTypeRef, ComponentValueData>>,
    ): Long?

    suspend fun addComponentValue(lexemeId: Long, componentTypeId: Long, data: ComponentValueData): Long
    suspend fun updateComponentValue(componentValueId: Long, data: ComponentValueData): Int
    /** Returns count(remaining components after delete) — caller cascades lexeme delete if 0. */
    suspend fun deleteComponentValue(componentValueId: Long): Int

    suspend fun getComponentTypes(dictionaryId: Long): List<ComponentTypeApiEntity>
    suspend fun getQuizConfig(dictionaryId: Long, quizMode: String): QuizConfigApiEntity?

    // --- Transitional @Deprecated shim (A3) ---

    @Deprecated("Use addLexemeWithBuiltInComponent")
    suspend fun addLexemeWithTranslation(
        wordId: Long, dictionaryId: Long, translation: TranslationApiEntity,
    ): Long
    @Deprecated("Use updateComponentValue via generic path")
    suspend fun updateLexemeTranslation(id: Long, translation: TranslationApiEntity?): Long?
    // addLexemeWithDefinition / updateLexemeDefinition — УДАЛЕНЫ (AGG-6)
    // addLexeme(wordId, translation) / addLexeme(wordId, definition) overloads — УДАЛЕНЫ
    // (не atomic, заменяются generic addLexemeWith*Component).
}
```

Псевдокод (не реализация): generic методы делегируют в DAO `@Transaction` default-method
(паттерн `WordDao.addLexemeWithQuiz`). `@Deprecated`-обёртки делегируют на generic
с `BuiltInComponent.TRANSLATION` + `ComponentValueData.TextValue`.

---

### Node 16 `[~]` — wordcard `Term.kt`

**Было** (verified Read):
```kotlin
data class Term(
    val wordId: WordId,
    val word: Word,
    val addedDate: Date,
    val changedDate: Date?,
    val removedDate: Date?,
    val lexemeList: List<Lexeme>
)
```

**Стало** (F1 fix — handler LoadWord нуждается в dictionaryId для
`getComponentTypes(dictionaryId)` pre-fetch):
```kotlin
data class Term(
    val wordId: WordId,
    val word: Word,
    val dictionaryId: Long,        // ← new (источник: WordApiEntity.dictionaryId, verified Read)
    val addedDate: Date,
    val changedDate: Date?,
    val removedDate: Date?,
    val lexemeList: List<Lexeme>
)
```

---

### Node 17 `[~]` — `WordCardUseCase.kt` interface

**Было** (verified Read):
```kotlin
interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?
    suspend fun addLexemeTranslation(wordId, lexemeId: Long?, translation: String): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?
    suspend fun addLexemeDefinition(wordId, lexemeId: Long?, definition: String): Lexeme?
    suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?
    suspend fun restoreLexeme(wordId, translation: String?, definition: String?): List<Lexeme>?
}
sealed interface RemoveTranslationResult { TranslationRemoved(Lexeme); LexemeCascadeRemoved }
sealed interface RemoveDefinitionResult  { DefinitionRemoved(Lexeme);  LexemeCascadeRemoved }
```

**Стало** (AGG-6: удалены definition-методы + `RemoveDefinitionResult`; добавлены
generic + `getComponentTypes` + `RemoveComponentResult`; translation-методы сохранены
как transitional shim):
```kotlin
interface WordCardUseCase {
    // --- Word / lexeme ---
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?
    suspend fun restoreLexeme(wordId, translation: String?, definition: String?): List<Lexeme>?

    // --- Translation shim (transitional, delegates to generic) ---
    suspend fun addLexemeTranslation(wordId, lexemeId: Long?, translation: String): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?

    // --- Generic component API ---
    suspend fun addLexemeWithBuiltInComponent(
        wordId: Long, lexemeId: Long?, systemKey: BuiltInComponent, data: ComponentValueData,
    ): Lexeme?
    suspend fun addLexemeWithUserDefinedComponent(
        wordId: Long, lexemeId: Long?, name: String, data: ComponentValueData,
    ): Lexeme?
    suspend fun addComponentValue(lexemeId: Long, componentTypeId: ComponentTypeId, data: ComponentValueData): Lexeme?
    suspend fun updateComponentValue(componentValueId: ComponentValueId, data: ComponentValueData): Lexeme?
    suspend fun deleteComponentValue(componentValueId: ComponentValueId): RemoveComponentResult?

    /** Public — вызывается из mate handler `LoadWord` для вычисления
     *  `hasDefinitionComponent` (F1 fix). */
    suspend fun getComponentTypes(dictionaryId: Long): List<ComponentType>
}

sealed interface RemoveTranslationResult {
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveComponentResult {
    data class ComponentRemoved(val lexeme: Lexeme) : RemoveComponentResult
    data object LexemeCascadeRemoved : RemoveComponentResult
}

// sealed interface RemoveDefinitionResult — УДАЛЕНО (AGG-6)
// addLexemeDefinition / deleteLexemeDefinition — УДАЛЕНЫ (AGG-6)
```

---

### Node 18 `[~]` — `QuizChatUseCase.kt` interface

**Было** (verified Read):
```kotlin
interface QuizChatUseCase {
    suspend fun getCurrentDictionaryId(): Long?
    suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
    suspend fun getRandomWriteQuizList(limit: Int, maxGrade: Int, dictionaryId: Long): List<WriteQuiz>
}
```

**Стало** (AGG-5 — добавлен `getQuizConfig`, возвращает domain `QuizConfig`,
не ApiEntity):
```kotlin
interface QuizChatUseCase {
    suspend fun getCurrentDictionaryId(): Long?
    suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
    suspend fun getRandomWriteQuizList(limit: Int, maxGrade: Int, dictionaryId: Long): List<WriteQuiz>

    /** Lookup quiz config. null → row отсутствует (F1 нарушение, не crash).
     *  Domain `QuizConfig` (AGG-10). Вызывается раз на quiz session start
     *  в QuizGameImpl.fetchData (F5 — no N+1). */
    suspend fun getQuizConfig(dictionaryId: Long, quizMode: String = "write"): QuizConfig?
}
```

---

### Node 19 `[~]` — `app/.../mapper/LexemeMapper.kt`

**Было** (verified Read):
```kotlin
fun LexemeApiEntity.toDomain(): Lexeme = Lexeme(
    lexemeId = LexemeId(id),
    translation = translation?.let { Translation(it.value) },
    definition = definition?.let { Definition(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)
```

**Стало** (AGG-2 — все API→Domain мапперы IS481 в одном файле; AGG-1 — definition
shim через user-defined lookup; shim consistency debug-assertion):
```kotlin
// === ComponentTypeApiEntity → ComponentType ===
fun ComponentTypeApiEntity.toDomain(): ComponentType = ComponentType(
    id = ComponentTypeId(id),
    systemKey = systemKey,
    dictionaryId = dictionaryId,
    name = name,
    template = template,
    position = position,
    removeDate = removeDate,
)

// === ComponentValueApiEntity → ComponentValue ===
fun ComponentValueApiEntity.toDomain(): ComponentValue = ComponentValue(
    id = ComponentValueId(id),
    lexemeId = LexemeId(lexemeId),
    type = type.toDomain(),
    data = data,
)

// === QuizConfigApiEntity → QuizConfig ===
fun QuizConfigApiEntity.toDomain(): QuizConfig = QuizConfig(
    dictionaryId = dictionaryId,
    quizMode = quizMode,
    componentRefs = componentRefs,
)

// === LexemeApiEntity → Lexeme (with shim) ===
fun LexemeApiEntity.toDomain(): Lexeme {
    val mapped = components.map { it.toDomain() }
    val translationShim = mapped
        .firstOrNull { it.type.systemKey == BuiltInComponent.TRANSLATION }
        ?.data?.let { (it as? ComponentValueData.TextValue)?.text }
        ?.let { Translation(it) }
    // AGG-1: definition через user-defined lookup, НЕ built-in.
    val definitionShim = mapped
        .firstOrNull { it.type.systemKey == null && it.type.name == "Definition" }
        ?.data?.let { (it as? ComponentValueData.TextValue)?.text }
        ?.let { Definition(it) }
    if (BuildConfig.DEBUG) {
        // Shim consistency invariant — рассинхрон детектируется до prod.
        check(translationShim?.value == /* recompute */) { "..." }
        check(definitionShim?.value == /* recompute */) { "..." }
    }
    return Lexeme(
        lexemeId = LexemeId(id),
        components = mapped,
        translation = translationShim,
        definition = definitionShim,
        addDate = addDate,
        changeDate = changeDate,
    )
}
```

---

### Node 20 `[~]` — `WordCardUseCaseImpl.kt`

**Было** (verified Read): 5 переопределений на specialized API — `addLexemeTranslation`,
`deleteLexemeTranslation`, `addLexemeDefinition`, `deleteLexemeDefinition`,
`restoreLexeme` (две операции, не atomic). `insertLexemeWith*` private helpers
читают `PrefKey.CURRENT_DICTIONARY_ID_LONG`, валидируют через `dictionaryApi`,
вызывают `lexemeApi.addLexemeWith*`.

**Стало:**
- Удалены `addLexemeDefinition`, `deleteLexemeDefinition`, `insertLexemeWithDefinition`
  (AGG-6).
- `addLexemeTranslation` остаётся, impl делегирует на новый
  `addLexemeWithBuiltInComponent(wordId, lexemeId, BuiltInComponent.TRANSLATION,
  ComponentValueData.TextValue(trimmed))`.
- `deleteLexemeTranslation` остаётся, impl делегирует на новый `deleteComponentValue`
  с lookup translation component_value по `(lexemeId, type.systemKey=TRANSLATION)`;
  маппит `RemoveComponentResult` → `RemoveTranslationResult`.
- Добавлены generic переопределения: `addLexemeWithBuiltInComponent`,
  `addLexemeWithUserDefinedComponent`, `addComponentValue`, `updateComponentValue`,
  `deleteComponentValue`, `getComponentTypes`. Все через try/catch + null-on-error
  + `logger.e` (паттерн проекта).
- `restoreLexeme` impl — переписан на atomic compound INSERT (MIN-9) через
  новый `lexemeApi.addLexemeWithComponents(wordId, dictionaryId,
  components = listOfNotNull(translation builtin pair, definition user-defined pair))`.
  Сигнатура сохранена.
- Маппер `TermApiEntity.toDomainEntity()` (в этом же файле, :199-211) +
  `Term.dictionaryId = word.dictionaryId`.

Псевдокод impl `addLexemeWithUserDefinedComponent`:
```kotlin
override suspend fun addLexemeWithUserDefinedComponent(...): Lexeme? = try {
    val id = if (lexemeId == null) {
        val dictId = resolveCurrentDictionaryId()  // existing helper pattern
        lexemeApi.addLexemeWithUserDefinedComponent(wordId, dictId, name, data)
            ?: return null  // type "Definition" не зарегистрирован в словаре
    } else {
        val types = lexemeApi.getComponentTypes(dictIdForLexeme).firstOrNull { it.name == name && it.systemKey == null }
            ?: return null
        lexemeApi.addComponentValue(lexemeId, types.id, data)
        lexemeId
    }
    lexemeApi.getLexemeById(id)?.toDomain()
} catch (e: Exception) {
    logger.e(tag = LogTags.WORDCARD, message = "addLexemeWithUserDefinedComponent failed: ${e.message}")
    null
}
```

---

### Node 21 `[~]` — `QuizChatUseCaseImpl.kt`

**Было** (verified Read): три override (`getCurrentDictionaryId`, `updateWriteQuiz`,
`getRandomWriteQuizList`).

**Стало:** добавлен `override suspend fun getQuizConfig(dictionaryId, quizMode):
QuizConfig?`. Impl делегирует на `lexemeApi.getQuizConfig(dictionaryId, quizMode)?.toDomain()`
(API метод на `CoreDbApi.LexemeApi` per Node 15; контракт business: возврат domain `QuizConfig?`).
**Dep на Node 18** (interface UseCase) + Node 19 (mapper `QuizConfigApiEntity.toDomain`) + Node 15 (LexemeApi с `getQuizConfig`). Injectable `lexemeApi: CoreDbApi.LexemeApi` уже доступна в Impl (см. existing constructor injects).

Псевдокод:
```kotlin
override suspend fun getQuizConfig(dictionaryId: Long, quizMode: String): QuizConfig? = try {
    lexemeApi.getQuizConfig(dictionaryId, quizMode)?.toDomain()
} catch (e: Exception) {
    logger.e(...)  // null-on-error
    null
}
```

---

### Node 22 `[~]` — wordcard `State.kt`

**Было** (verified Read):
```kotlin
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val lexemeIdPendingDelete: Long? = null,
)
```

**Стало** (AGG-6 UI блок + memory project_explicit_state_flags):
```kotlin
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val lexemeIdPendingDelete: Long? = null,
    /** Per-dictionary флаг наличия user-defined `name="Definition", systemKey=null`.
     *  Read-only-on-load, вычисляется reducer'ом на Msg.WordLoaded. */
    val hasDefinitionComponent: Boolean = false,    // ← new
)
```

`LexemeState.canAddTranslation` / `canAddDefinition` computed properties — без
изменений (per-lexeme флаги, остаются). Composable AND'ит `canAddDefinition &&
hasDefinitionComponent`.

`Lexeme.toLexemeState()` (:245) — без изменений сигнатуры. Внутри читает
`this.translation` / `this.definition` — теперь shim-поля, заполненные маппером.

---

### Node 23 `[~]` — wordcard `Message.kt`

**Было** (verified Read):
```kotlin
data class WordLoaded(val word: Term) : Msg
```

**Стало** (F1 fix — добавлен `componentTypes` payload для вычисления `hasDefinitionComponent`):
```kotlin
data class WordLoaded(
    val word: Term,
    val componentTypes: List<ComponentType>,
) : Msg
```

Остальные Msg sigs не меняются.

---

### Node 24 `[~]` — `WordCardReducer.kt`

**Было** (verified Read, lines 253-265):
```kotlin
is Msg.WordLoaded -> {
    val w = message.word
    state.copy(
        isLoading = false, isPendingDbOp = false,
        wordState = WordState.Loaded(...),
        lexemeList = w.lexemeList.map { it.toLexemeState() },
    ) to emptySet()
}
```

**Стало:** reducer вычисляет `hasDefinitionComponent` из `message.componentTypes`
одновременно с заполнением `wordState`. Других веток reducer не задевает.

```kotlin
is Msg.WordLoaded -> {
    val w = message.word
    val hasDef = message.componentTypes.any { it.systemKey == null && it.name == "Definition" }
    state.copy(
        isLoading = false, isPendingDbOp = false,
        wordState = WordState.Loaded(id = w.wordId.id, added = w.addedDate, value = w.word.value),
        lexemeList = w.lexemeList.map { it.toLexemeState() },
        hasDefinitionComponent = hasDef,    // ← new
    ) to emptySet()
}
```

Никакого `Msg.HasDefinitionComponentChanged` — read-only-on-load.

---

### Node 25 `[~]` — `DatasourceEffectHandler.kt`

**Было** (verified Read, lines 59-62, 127-154): `LoadWord` делает один вызов
`getTermById`. `UpdateLexemeDefinition` вызывает `addLexemeDefinition`,
`RemoveDefinition` вызывает `deleteLexemeDefinition` (sealed
`RemoveDefinitionResult`).

**Стало** (тело `onEffect` reroute на generic; sigs sealed `DatasourceEffect`
не меняются):

- **`LoadWord`** — sequential pre-fetch:
  ```kotlin
  is DatasourceEffect.LoadWord -> {
      val term = wordCardUseCase.getTermById(effect.wordId) ?: return@... Msg.WordNotFound
      val types = wordCardUseCase.getComponentTypes(term.dictionaryId)
      Msg.WordLoaded(term, types)
  }
  ```

- **`UpdateLexemeDefinition`** — вместо `addLexemeDefinition(...)`:
  ```kotlin
  is DatasourceEffect.UpdateLexemeDefinition ->
      wordCardUseCase.addLexemeWithUserDefinedComponent(
          wordId = effect.wordId,
          lexemeId = effect.lexemeId,
          name = "Definition",
          data = ComponentValueData.TextValue(effect.definition),
      )?.let { Msg.RefreshDefinition(it.lexemeId.id, it.definition?.value) }
          ?: Msg.ShowError(R.string.word_card_error_save_definition)
  ```

- **`RemoveDefinition`** — вместо `deleteLexemeDefinition(...)`:
  ```kotlin
  is DatasourceEffect.RemoveDefinition ->
      // Resolve componentValueId по (lexemeId, type.name="Definition") внутри UseCase impl,
      // не в handler. Handler передаёт lexemeId, UseCase делает lookup.
      when (val r = wordCardUseCase.deleteDefinitionComponent(effect.lexemeId)) {  // shim wrapper в UseCase либо явный resolve
          is RemoveComponentResult.ComponentRemoved ->
              Msg.DefinitionDeleted(r.lexeme.lexemeId.id, effect.currentValue)
          RemoveComponentResult.LexemeCascadeRemoved ->
              Msg.LexemeCascadeRemovedWithUndo(effect.lexemeId, null, effect.currentValue)
          null -> Msg.ShowError(R.string.word_card_error_remove_definition)
      }
  ```
  (Реальный path — UseCase impl делает lookup `componentTypeId` + `componentValueId`
  по `lexemeId`, вызывает `deleteComponentValue(componentValueId)`. Handler не
  знает про componentValueId — это implementation detail.)

- **`RestoreLexeme`** — sig сохранена, impl UseCase переписан (node 20).

Translation-эффекты (`UpdateLexemeTranslation`, `RemoveTranslation`) — не задевает,
работают через transitional shim `addLexemeTranslation` / `deleteLexemeTranslation`
(node 17).

---

### Node 26 `[~]` — `QuizGameImpl.kt`

**Было** (verified Read, lines 173-217 `fetchData`, 429-521 `toQuizItem`):
- `fetchData`: вызывает `getCurrentDictionaryId` + `getRandomWriteQuizList`,
  потом `.map { it.toQuizItem(resourceManager, isDebugOn) }`.
- `toQuizItem`: when-cascade translation → definition → `throw IllegalArgumentException("No translation or definition")` (:499).

**Стало** (AGG-5 wire):

- `fetchData`: pre-fetch `QuizConfig` (один раз на session, F5 no N+1):
  ```kotlin
  val dictionaryId = quizChatUseCase.getCurrentDictionaryId() ?: return emptyList()
  val quizConfig = quizChatUseCase.getQuizConfig(dictionaryId, "write")
      ?: return emptyList().also { logger.w(... "no quiz config for dict $dictionaryId") }
  // ... existing getRandomWriteQuizList + .map
  .mapNotNull { it.toQuizItem(quizConfig.componentRefs, resourceManager, isDebugOn) }
  ```

- `toQuizItem(componentRefs: List<ComponentTypeRef>, ...): QuizItem?` — graceful
  skip null:
  ```kotlin
  fun WriteQuiz.toQuizItem(
      componentRefs: List<ComponentTypeRef>,
      resourceManager: ResourceManager,
      isDebugOn: Boolean,
  ): QuizItem? {
      // Для каждого ref резолв ComponentValue в lexeme.components;
      // если все требуемые ref отсутствуют в lexeme — null (skip).
      val resolved = componentRefs.mapNotNull { ref ->
          when (ref) {
              is ComponentTypeRef.BuiltIn ->
                  lexeme.components.firstOrNull { it.type.systemKey == ref.key }
              is ComponentTypeRef.UserDefined ->
                  lexeme.components.firstOrNull { it.type.systemKey == null && it.type.name == ref.name }
          }
      }
      if (resolved.isEmpty()) return null     // graceful skip, НЕ throw
      // Сборка fullQuestion / question из resolved values в порядке componentRefs (F4).
      // ... existing buildAnnotatedString логика, переписанная на resolved.
  }
  ```

Удаляется `throw IllegalArgumentException("No translation or definition")` (:499).
Существующий empty-list test (`QuizGameImplEmptyListTest`) остаётся релевантен.

---

## log_messages

- design tree собран, 26 узлов (9 new + 1 gradle modify + 5 new API DTO/method + 11 modifications)
- AGG-12 / AGG-6 / AGG-2 / MIN-2 / MIN-9 / F1 fix отражены в графе
- все `[~]` верифицированы через Read (Lexeme.kt, build.gradle.kts, LexemeApiEntity.kt, CoreDbApi.kt, Term.kt, WordCardUseCase.kt, WordCardUseCaseImpl.kt, QuizChatUseCase.kt, QuizChatUseCaseImpl.kt, State.kt, Message.kt, WordCardReducer.kt, DatasourceEffectHandler.kt, QuizGameImpl.kt, LexemeMapper.kt)

_model: claude-opus-4-7[1m]_
