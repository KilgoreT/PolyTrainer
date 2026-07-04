# Business contract: IS481 Lexeme Component Constructor

Дельта к существующему wordcard / quiz-chat контракту. Mate refactor (Msg / Effect /
Reducer для translation+definition) — backlog (B4/C2).

## State

### `WordCardState` (`modules/screen/wordcard/.../mate/State.kt`)

Дельта — один плоский флаг `hasDefinitionComponent: Boolean = false` в
`WordCardState`. Per-dictionary флаг наличия user-defined типа `name="Definition",
system_key=NULL`. Управляет видимостью chip «Определение». Explicit field,
заполняется один раз на load. Composable AND'ит с per-lexeme `canAddDefinition`.

Инвариант: `true` ⇔ при load `componentTypes` (`List<ComponentType>` словаря
лексемы) содержит запись `name="Definition", systemKey=null`. Вычисляется reducer'ом
на `Msg.WordLoaded`.

### Domain `Term` (`modules/screen/wordcard/.../entity/Term.kt`)

Дельта — одно новое поле `dictionaryId: Long` (F1 fix). Используется handler'ом
`LoadWord` для вызова `getComponentTypes(dictionaryId)`. Источник —
`WordApiEntity.dictionaryId` (поле уже существует, verified Read), меняется
только маппер `TermApiEntity.toDomainEntity()` (`WordCardUseCaseImpl.kt:199-211`).

### Domain `Lexeme` (`modules/domain/lexeme/.../Lexeme.kt`)

Расширение data class — `components` + shim (B4/C2):

```kotlin
data class Lexeme(
    val lexemeId: LexemeId,
    val components: List<ComponentValue>,                                 // ← IS481
    @Deprecated("Use components") val translation: Translation? = null,   // shim
    @Deprecated("Use components") val definition: Definition? = null,     // shim
    val addDate: Date,
    val changeDate: Date? = null,
)
```

`components` — source of truth, упорядочен по `ComponentType.position`.
`translation`/`definition` — shim, заполняются маппером `LexemeApiEntity.toDomain()`
через lookup (translation = built-in `TRANSLATION`; definition = user-defined
`name="Definition"`, AGG-1). Mate / dictionaryTab / quiz читают без правок.
Debug-only shim consistency assertion в маппере (не закрывает `copy(translation=X)`
mutation в mate — known trade-off → mate refactor).

[guide: 04_builtin_strategy.md § «Слойность»; 06_mapping_design.md § «API → Domain»]

## Msg

**Дельта: одна изменённая Msg, ноль новых вариантов.** `Msg.WordLoaded(val word:
Term)` → `Msg.WordLoaded(val word: Term, val componentTypes: List<ComponentType>)`.
Причина (F1 fix): `Term`/`TermApiEntity` не содержат `componentTypes` (verified
Read) — нужен явный pre-fetch types словаря в handler'е, transport через Msg.

Остальные Msg sig не меняются: `Msg.RefreshTranslation`, `Msg.RefreshDefinition`,
`Msg.TranslationDeleted`, `Msg.DefinitionDeleted`, `Msg.LexemeCascadeRemovedWithUndo`,
`Msg.RefreshLexemeList`, `Msg.LexemeRemoved`.

Reducer на `WordLoaded` вычисляет
`hasDefinitionComponent = componentTypes.any { it.systemKey == null && it.name == "Definition" }`
и пишет в State. Никакого `Msg.HasDefinitionComponentChanged` (read-only-on-load).

[guide: _alignment_decisions.md → B4/C2, AGG-6]

## Effect/IO

**Дельта: ноль новых Effect-вариантов, изменяется только impl handler'а `LoadWord`.**

Все existing `DatasourceEffect` sealed-варианты из
`modules/screen/wordcard/.../mate/DatasourceEffectHandler.kt` сохраняют сигнатуры —
`LoadWord`, `RemoveWord`, `UpdateWord`, `RemoveLexeme`, `UpdateLexemeTranslation`,
`RemoveTranslation`, `UpdateLexemeDefinition(wordId, lexemeId?, definition)`,
`RemoveDefinition(lexemeId, currentValue)`, `RestoreLexeme(wordId, translation?,
definition?)`.

Изменения в **теле `onEffect`** (impl-detail):

- **`LoadWord`** — F1 fix: handler делает два sequential UseCase-вызова:
  ```kotlin
  val term = wordCardUseCase.getTermById(effect.wordId) ?: return@... null
  val types = wordCardUseCase.getComponentTypes(term.dictionaryId)
  Msg.WordLoaded(term, types)
  ```
  `dictionaryId` берётся из domain `Term` — domain entity расширяется новым полем
  `dictionaryId: Long` (мини-патч: `WordApiEntity.dictionaryId` уже существует,
  меняется только `TermApiEntity.toDomainEntity()` маппер +
  `Term` data class в `modules/screen/wordcard/.../entity/Term.kt`). Sequential, не
  parallel — `getComponentTypes` зависит от `dictionaryId` term'а. Альтернатива
  «private helper внутри `getTermById`» (reviewer F4 option a) отвергнута: types
  нужны в reducer'е через Msg, а не внутри UseCase impl — public UseCase-метод
  необходим.

- **`UpdateLexemeDefinition`** → `wordCardUseCase.addLexemeWithUserDefinedComponent(...)`
  вместо удалённого `addLexemeDefinition`. → тот же `Msg.RefreshDefinition` через
  shim.
- **`RemoveDefinition`** → `wordCardUseCase.deleteComponentValue(...)`. Результат
  через `RemoveComponentResult` маппится в существующие `Msg.DefinitionDeleted` /
  `Msg.LexemeCascadeRemovedWithUndo`.
- **`RestoreLexeme`** → sig сохранена, impl переписан на atomic compound INSERT
  (см. UseCase § MIN-9).

`getQuizConfig` — quiz/chat имеет свой data-flow в `QuizGameImpl.fetchData`, не
через mate Effect.

[guide: _alignment_decisions.md → AGG-6; business_walkthrough.md § 6, § 18]

## UseCase

### `WordCardUseCase` (`modules/screen/wordcard/.../deps/WordCardUseCase.kt`)

**Удалены** (AGG-6):

```kotlin
suspend fun addLexemeDefinition(wordId, lexemeId: Long?, definition: String): Lexeme?   // DELETE
suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?             // DELETE
// sealed RemoveDefinitionResult                                                        // DELETE
```

**Сохранены без изменений сигнатур** (translation остаётся built-in; MIN-9 — у
`restoreLexeme` sig сохраняется, impl переписан на atomic compound INSERT):
`getTermById`, `deleteWord`, `updateWord`, `deleteLexeme`, `addLexemeTranslation`,
`deleteLexemeTranslation`, `restoreLexeme`. Sealed `RemoveTranslationResult`
(`TranslationRemoved(lexeme)` / `LexemeCascadeRemoved`) — без изменений.

**Добавлены generic** (AGG-6):

```kotlin
/** Atomic INSERT lexeme + write_quiz + первый built-in component_value. */
suspend fun addLexemeWithBuiltInComponent(
    wordId: Long,
    lexemeId: Long?,
    systemKey: BuiltInComponent,
    data: ComponentValueData,
): Lexeme?

/** Atomic INSERT lexeme + write_quiz + первый user-defined component_value.
 *  Lookup type по (dictionary_id, name, system_key=NULL). */
suspend fun addLexemeWithUserDefinedComponent(
    wordId: Long,
    lexemeId: Long?,
    name: String,
    data: ComponentValueData,
): Lexeme?

/** Add / update / delete component_value (delete: если последний — cascade lexeme). */
suspend fun addComponentValue(lexemeId: Long, componentTypeId: ComponentTypeId, data: ComponentValueData): Lexeme?
suspend fun updateComponentValue(componentValueId: ComponentValueId, data: ComponentValueData): Lexeme?
suspend fun deleteComponentValue(componentValueId: ComponentValueId): RemoveComponentResult?

/** Lookup component types словаря. Public — вызывается из mate handler `LoadWord`
 *  для вычисления `hasDefinitionComponent` (F1 fix). */
suspend fun getComponentTypes(dictionaryId: Long): List<ComponentType>

sealed interface RemoveComponentResult {
    data class ComponentRemoved(val lexeme: Lexeme) : RemoveComponentResult
    data object LexemeCascadeRemoved : RemoveComponentResult
}
```

**Атомарность generic add-методов:** `@Transaction` на default-method DAO (тот же
pattern `WordDao.addLexemeWithQuiz`, walkthrough § 9). Не `RoomDatabase.withTransaction
{ }` — проект bundled SQLite использует только `@Transaction`.

#### MIN-9: `restoreLexeme` — atomic compound INSERT (F3 fix)

`restoreLexeme(wordId, translation: String?, definition: String?): List<Lexeme>?` —
**сигнатура сохраняется** (B4/C2 shim). **Impl** делает atomic compound INSERT
**всех components одной транзакцией** через новый DAO default-method
`WordDao.addLexemeWithComponents(lexemeDb, dictionaryId, components: List<ComponentValueDb>)`
(`@Transaction`, generic вариант существующего `addLexemeWithQuiz`, walkthrough § 9):
INSERT `lexemes` → INSERT `write_quiz` → INSERT N `component_values` (translation
built-in + опционально definition user-defined). FK violation на любом шаге → rollback
всей транзакции (рассинхрон при провале второго INSERT'а исключён — это и закрывает
MIN-9 vs «две транзакции» в walkthrough § 5).

Если `definition != null` — impl делает `getComponentTypes(dictionaryId)` + lookup
user-defined `name="Definition", systemKey=null` для словаря (тип уже создан
миграцией M11→M12 для словарей с existing definitions, AGG-9 шаги 3-4). Compound
DAO-метод используется **только** из `restoreLexeme` impl (один callsite), наружу
как UseCase-метод не выставлен — атомарный INSERT двух компонентов нужен только в
undo flow. Для single-component INSERT'а остаются `addLexemeWithBuiltInComponent` /
`addLexemeWithUserDefinedComponent`.

[guide: _alignment_decisions.md → MIN-9, AGG-9; walkthrough § 5, § 9]

### `QuizChatUseCase` (`modules/screen/quiz/chat/.../deps/QuizChatUseCase.kt`)

**Добавлен один метод** (AGG-5, F2 fix):

```kotlin
/** Lookup quiz config. null → row отсутствует (F1 нарушение, не crash).
 *  Возвращает **domain** QuizConfig (AGG-10) — UseCase port не пробрасывает
 *  ApiEntity наружу. Impl делает `apiEntity.toDomain()` через
 *  `QuizConfigApiEntity.toDomain()` маппер (06.md § Quiz config).
 *  Вызывается один раз на quiz session start в QuizGameImpl.fetchData
 *  (F5 — no N+1). */
suspend fun getQuizConfig(
    dictionaryId: Long,
    quizMode: String = "write",
): QuizConfig?
```

Existing 3 метода (`getCurrentDictionaryId`, `updateWriteQuiz`, `getRandomWriteQuizList`)
не меняются. Конвенция возврата domain (`getRandomWriteQuizList: List<WriteQuiz>`)
сохранена — `QuizConfig` в `modules/domain/lexeme` (AGG-10 с TODO на вынос в
`modules/domain/quiz`).

`QuizGameImpl.fetchData` pre-fetch'ит `QuizConfig` (domain), передаёт в
`toQuizItem(quizConfig, ...)` для каждой лексемы. Возвращаемый тип `toQuizItem`
становится `QuizItem?` (graceful skip null) — existing `throw IllegalArgumentException`
(walkthrough § 17, line :499) удаляется.

### Domain types (`modules/domain/lexeme/`)

Pure-JVM модуль, без Android-зависимостей.

```kotlin
enum class BuiltInComponent(val key: String) { TRANSLATION("translation"); ... }   // AGG-1
enum class ComponentTemplate(val key: String) { TEXT("text"), LONG_TEXT("long_text"), IMAGE("image"); ... }

sealed interface ComponentValueData {
    data class TextValue(val text: String) : ComponentValueData
    data class LongTextValue(val text: String) : ComponentValueData
    data class ImageValue(val uri: String) : ComponentValueData
}

@JvmInline value class ComponentTypeId(val id: Long)
data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,    // null → user-defined
    val dictionaryId: Long?,             // null → global
    val name: String?,                   // user-defined: required; built-in: optional override
    val template: ComponentTemplate,
    val position: Int,
    val removeDate: Date? = null,
)

@JvmInline value class ComponentValueId(val id: Long)
data class ComponentValue(
    val id: ComponentValueId,
    val lexemeId: LexemeId,
    val type: ComponentType,             // full embedded (multi-level @Relation)
    val data: ComponentValueData,
)

// AGG-10 — TODO KDoc на вынос в modules/domain/quiz (backlog «Quiz config UX»):
sealed interface ComponentTypeRef {
    @JvmInline value class BuiltIn(val key: BuiltInComponent) : ComponentTypeRef
    @JvmInline value class UserDefined(val name: String) : ComponentTypeRef
}
data class QuizConfig(val dictionaryId: Long, val quizMode: String, val componentRefs: List<ComponentTypeRef>)

fun Lexeme.builtIn(key: BuiltInComponent): ComponentValue? =
    components.firstOrNull { it.type.systemKey == key }

@Deprecated(...) @JvmInline value class Translation(val value: String)   // shim B4/C2
@Deprecated(...) @JvmInline value class Definition(val value: String)    // shim B4/C2
```

[guide: 04_builtin_strategy.md § «Слойность»; 06_mapping_design.md § «Quiz config»;
_alignment_decisions.md → A1, AGG-10]

---

**Связность.** State `hasDefinitionComponent` ← reducer ← `Msg.WordLoaded(term,
componentTypes)` ← handler `LoadWord` (sequential: term → types) ←
`WordCardUseCase.getTermById` + `getComponentTypes(term.dictionaryId)`. Definition effects
(`UpdateLexemeDefinition` / `RemoveDefinition`) ← handler reroute ← generic UseCase
(`addLexemeWithUserDefinedComponent` / `deleteComponentValue`). `RestoreLexeme` ← impl
atomic compound DAO (`addLexemeWithComponents`, MIN-9). Quiz `QuizConfig` (domain) ←
`QuizChatUseCase.getQuizConfig` ← `QuizGameImpl.fetchData` pre-fetch. Domain types —
single source `modules/domain/lexeme`, мапперы в `app/.../mapper/LexemeMapper.kt`
(AGG-2).

_model: claude-opus-4-7[1m]_
