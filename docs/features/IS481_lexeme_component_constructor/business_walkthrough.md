# IS481 — Business walkthrough (factual code state, pre-IS481)

Discovery текущего состояния кодовой базы — что реально лежит и как, чтобы business_contract писался не теоретически.

## 1. CoreDbApi.LexemeApi — текущие сигнатуры

[`core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt:77-120`](../../../core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt):

- `getLexemeById(id: Long): LexemeApiEntity?` (78).
- `addLexeme(wordId: Long): Long` (79) — пустая лексема, без translation/definition.
- `addLexeme(wordId, translation: TranslationApiEntity): Long` (80) — overload, БЕЗ write-quiz (не atomic).
- `addLexeme(wordId, definition: DefinitionApiEntity): Long` (85) — overload, БЕЗ write-quiz.
- `addLexemeWithTranslation(wordId, dictionaryId, translation): Long` (94) — atomic INSERT lexeme + write-quiz. KDoc 91-93: «Закрывает domain-инвариант "у каждой лексемы есть write-quiz"».
- `addLexemeWithDefinition(wordId, dictionaryId, definition): Long` (103) — симметрично, atomic.
- `updateLexemeTranslation(id, translation: TranslationApiEntity?): Long?` (109) — null = clear.
- `updateLexemeDefinition(id, definition: DefinitionApiEntity?): Long?` (114) — null = clear.
- `deleteLexeme(id: Long): Int` (119).

Все 6 «специализированных» методов (`add*Translation` / `add*Definition` / `update*Translation` / `update*Definition`) — кандидаты на @Deprecated (translation) либо удаление (definition) по AGG-6.

## 2. LexemeApiEntity — текущий shape

[`core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/LexemeApiEntity.kt`](../../../core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/LexemeApiEntity.kt):

```kotlin
@JvmInline value class TranslationApiEntity(val value: String)   // :5-6
@JvmInline value class DefinitionApiEntity(val value: String)    // :8-9

data class LexemeApiEntity(
    val id: Long,
    val translation: TranslationApiEntity? = null,
    val definition: DefinitionApiEntity? = null,
    val wordClass: String? = null,
    val options: Long = 0,
    val addDate: Date,
    val changeDate: Date? = null,
)

fun LexemeApiEntity.canRemoveTranslation() = definition != null  // :21
fun LexemeApiEntity.canRemoveDefinition() = translation != null  // :22
```

Поля `translation` / `definition` — два nullable nominal-value-class столбца. Никакого `components: List<…>` нет. `canRemove*` extensions используются wordcard UseCaseImpl для решения «удалить только subentity vs cascade lexeme».

## 3. Domain `Lexeme` — текущее содержимое модуля `modules/domain/lexeme` (post-IS482)

[`modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt`](../../../modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt) — единственный файл модуля:

```kotlin
@JvmInline value class LexemeId(val id: Long)
@JvmInline value class Translation(val value: String)
@JvmInline value class Definition(val value: String)

data class Lexeme(
    val lexemeId: LexemeId,
    val translation: Translation?,
    val definition: Definition?,
    val addDate: Date,
    val changeDate: Date? = null,
)
```

Модуль pure-JVM (`build.gradle.kts`: `id("org.jetbrains.kotlin.jvm")`, без Android). Никаких `ComponentType` / `ComponentValue` / `BuiltInComponent` пока нет — IS481 их добавляет.

## 4. WordCardUseCase — interface (domain port в feature-module)

[`modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt`](../../../modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt):

- `addLexemeTranslation(wordId, lexemeId: Long?, translation: String): Lexeme?` (:11) — null lexemeId = insert.
- `deleteLexemeTranslation(lexemeId): RemoveTranslationResult?` (:12).
- `addLexemeDefinition(wordId, lexemeId: Long?, definition: String): Lexeme?` (:13).
- `deleteLexemeDefinition(lexemeId): RemoveDefinitionResult?` (:14).
- `restoreLexeme(wordId, translation: String?, definition: String?): List<Lexeme>?` (:21-25).

Sealed результаты (:28-36):

```kotlin
sealed interface RemoveTranslationResult {
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    data object LexemeCascadeRemoved : RemoveTranslationResult
}
sealed interface RemoveDefinitionResult {
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}
```

Pattern: UseCase возвращает `null` на ошибку (логируется в Impl), `sealed` варианты для семантически разных success-исходов.

## 5. WordCardUseCaseImpl — где лежит и patterns

[`app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt`](../../../app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt) — Impl в `app/`, не в модуле (AGG-2 паттерн).

Конструктор (:23-30): `wordApi, dictionaryApi, termApi, lexemeApi, prefsProvider, logger`.

Ключевые patterns:
- **try/catch + null on error + logger.e** (:44-50, :56-69, :71-84, и т.д.). Тэг `LogTags.WORDCARD`.
- **Insert-vs-update branch** (:58-64): если `lexemeId == null` — atomic `insertLexemeWithTranslation` (private helper :162-178) → внутри читает `PrefKey.CURRENT_DICTIONARY_ID_LONG` из prefs, валидирует через `dictionaryApi.getDictionaryById`, бросает `IllegalStateException` если нет dictionary, потом вызывает `lexemeApi.addLexemeWithTranslation(wordId, dictionaryId, translation)`.
- **Delete sub-entity vs cascade** (:71-84, :104-117): `lexemeApi.getLexemeById` → `canRemoveTranslation()` extension → `updateLexemeTranslation(id, null)` либо `deleteLexeme(id)` → возвращает соответствующий `Removed.*` вариант. Не атомарный (2 запроса: get+update/delete).
- **`restoreLexeme`** (:119-155) — самая «грязная» точка: если обе translation+definition → INSERT с translation через `insertLexemeWithTranslation`, потом `updateLexemeDefinition` отдельным запросом; при провале второго — manual rollback через `deleteLexeme`. Сейчас две операции, **не atomic**. MIN-9 предписывает переписку на одну atomic generic-INSERT с двумя компонентами.

## 6. DatasourceEffectHandler — реально в `modules/screen/wordcard/mate/`, не в `app/`

[`modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/DatasourceEffectHandler.kt`](../../../modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/DatasourceEffectHandler.kt):

Sealed `DatasourceEffect` (:15-46): включает `UpdateLexemeTranslation` (:23-28, lexemeId nullable = insert), `RemoveTranslation` (:30), `UpdateLexemeDefinition` (:32-37), `RemoveDefinition` (:39), `RestoreLexeme` (:41-45).

`onEffect` (:55-183) — большой `when` с try/catch fallback (:164-180) логирующий через `LexemeLogger`. Definition-точки:
- :127-137 (`UpdateLexemeDefinition` → `wordCardUseCase.addLexemeDefinition(...)` → `Msg.RefreshDefinition`).
- :139-154 (`RemoveDefinition` → `wordCardUseCase.deleteLexemeDefinition(...)` → когда `DefinitionRemoved` → `Msg.DefinitionDeleted`, когда `LexemeCascadeRemoved` → `Msg.LexemeCascadeRemovedWithUndo`).

Эти **2 точки** (AGG-6 чеклист F001) переписываются на generic в IS481.

## 7. LexemeMapper в `app/` — текущий маппер ApiEntity → Domain

[`app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt`](../../../app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt):

```kotlin
fun LexemeApiEntity.toDomain(): Lexeme = Lexeme(
    lexemeId = LexemeId(id),
    translation = translation?.let { Translation(it.value) },
    definition = definition?.let { Definition(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)
```

Тривиальный 1:1 mapping value-classes. Локация — `app/src/main/java/me/apomazkin/polytrainer/mapper/` (AGG-2 паттерн уже соблюдён). IS481 расширяет: `components = …`, shim полей `translation`/`definition` через lookup.

Импортируется в:
- `WordCardUseCaseImpl.kt:11` — `me.apomazkin.polytrainer.mapper.toDomain`.
- `QuizChatUseCaseImpl.kt:7`.

Другие мапперы:
- `WordCardUseCaseImpl.kt:199-211` — `TermApiEntity.toDomainEntity()` — лежит **рядом с UseCaseImpl** в одном файле (не выделен в `mapper/` пакет, IS482 паттерн неконсистентный).
- `QuizChatUseCaseImpl.kt:114-147` — `WordApiEntity.toDomainEntity`, `WriteQuizComplexEntity.toDomainEntity`, `WriteQuizUpsertEntity.toApiEntity` — также рядом с Impl. Конвенция: «мапперы могут лежать рядом с использующим их Impl ИЛИ в `app/.../mapper/`».

## 8. CoreDbApi.LexemeApi.Impl — где собственно atomic выполнен

[`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt:198-297`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt) — `LexemeApiImpl`.

`addLexemeWithTranslation` (:244-258) и `addLexemeWithDefinition` (:260-274) — оба делегируют в `wordDao.addLexemeWithQuiz(lexemeDb, dictionaryId)`. Atomic-механизм — на DAO level.

## 9. WordDao — atomicity patterns в проекте

[`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt):

Атомарная транзакция inline `@Transaction` на default-methods interface (:164-169):

```kotlin
/**
 * Atomic INSERT лексемы + write-quiz записи в одной транзакции.
 * Гарантирует domain-инвариант «у каждой лексемы есть write-quiz».
 */
@Transaction
suspend fun addLexemeWithQuiz(lexemeDb: LexemeDb, dictionaryId: Long): Long {
    val newLexemeId = addLexeme(lexemeDb)
    addWriteQuiz(WriteQuizDb.create(dictionaryId = dictionaryId, lexemeId = newLexemeId))
    return newLexemeId
}
```

Это **единственный «compound INSERT»** в проекте сейчас. Pattern для IS481 `addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent` тот же: `@Transaction` на default-method DAO + sequential calls.

Поиск `withTransaction` / `db.withTransaction` по проекту — **нет ни одного результата** (`grep -rn "withTransaction" /Users/kilg/AndroidStudioProjects/PolyTrainer/core` пусто). Проект использует **только** Room-аннотацию `@Transaction`, не `RoomDatabase.withTransaction { … }` API.

Прочие `@Transaction` в WordDao: 11 случаев — все это `@Query` SELECT-методы с @Relation проекциями (:65, :70, :74, :85, :104, :117, :143, :180, :186, :200, :221, :225). Это паттерн «SELECT через @Relation → @Transaction для consistency». Не «compound write».

## 10. WordDao.addDictionary — где реально живёт

`addDictionary` лежит в **WordDao** (не отдельный DictionaryDao):
- [`WordDao.kt:28-29`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt): `@Insert(onConflict = OnConflictStrategy.ABORT) suspend fun addDictionary(dictionaryDb: DictionaryDb): Long`.

Текущая реализация — однократный `@Insert`, без транзакции. Никакого auto-INSERT в смежные таблицы.

Callsite: [`CoreDbApiImpl.kt:81-90`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt) — `DictionaryApiImpl.addDictionary` → `wordDao.addDictionary(DictionaryDb(...))`. IS481 AGG-4 расширяет в `@Transaction`-метод с auto-INSERT `quiz_configs` row для каждого `quiz_mode`.

## 11. LexemeDbEntity и LexemeDb — текущий shape

[`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDb.kt`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDb.kt):

```kotlin
@Entity(tableName = "lexemes", foreignKeys = [...], indices = [Index("word_id")])
data class LexemeDb(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    @ColumnInfo(name = "translation") val translation: String? = null,  // :26 — удаляется в M11→M12
    @ColumnInfo(name = "definition") val definition: String? = null,    // :27 — удаляется в M11→M12
    @ColumnInfo(name = "word_class") val wordClass: String? = null,
    @ColumnInfo(name = "options") val options: Long = 0,
    @ColumnInfo(name = "add_date") val addDate: Date,
    @ColumnInfo(name = "change_date") val changeDate: Date? = null,
)
```

[`LexemeDbEntity.kt`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDbEntity.kt) — @Relation wrapper. Сейчас содержит `lexemeDb: LexemeDb` + `sampleDbList: List<SampleDb>`. **Multi-level @Relation в проекте уже используется** (`SampleDb` через `entityColumn = "lexemeId"`). Маппер `.toApiEntity()` (:18-26) делает 1:1 translation/definition → ApiEntity. IS481 добавляет multi-level @Relation для `component_values` + nested `component_types`.

Пример существующего multi-level @Relation цепочки: `TermDbEntity → LexemeDbEntity → SampleDb` ([`TermDbEntity.kt:8-15`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/TermDbEntity.kt) — top-level `wordDb` + `lexemeListDb: List<LexemeDbEntity>` с `entity = LexemeDb::class`).

## 12. Database.kt — текущая версия

[`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt):

```kotlin
@Database(
    entities = [WordDb::class, LexemeDb::class, HintDb::class, SampleDb::class,
                WriteQuizDb::class, DictionaryDb::class],
    version = 11
)
@TypeConverters(DateTimeConverter::class)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
```

IS481: добавятся `ComponentTypeDb`, `ComponentValueDb`, `QuizConfigDb` в `entities`; `version = 12`.

## 13. RoomModule.provideDatabase — текущее состояние post-prereq (IS481_vPrepared)

[`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt:42-61`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt):

- `.setDriver(BundledSQLiteDriver())` (:47) — bundled SQLite уже подключён.
- `.setQueryCoroutineContext(Dispatchers.IO)` (:48).
- `.fallbackToDestructiveMigration(dropAllTables = true)` (:49) — fallback для pre-0.1.0 install.
- `.addCallback(...)` с `onDestructiveMigration(connection: SQLiteConnection)` (:50-59) — логирует через `LexemeLogger.e(tag = LogTags.DB, ...)` → автоматически в Crashlytics (paste про CrashlyticsSink :22-27).

`.addMigrations(...)` блок убран в prereq (KDoc :19 — «Все исторические миграции 1→2 ... 10→11 удалены»). KDoc :28-35 даёт чеклист «когда понадобится новая миграция»:
1. Создать `Migration(N, N+1) { override fun migrate(connection: SQLiteConnection) {...} }`.
2. Добавить в `.addMigrations(...)`.
3. Инкрементировать `version` в Database.
4. Edit entity classes.
5. Написать migration test под bundled driver.

IS481 main делает ровно это — одна строка `.addMigrations(Migration_011_to_012)`, один файл миграции + один test.

## 14. Existing component-like абстракции — нет

Поиск аналогов «полиморфный component с template» в проекте:
- **HintDb** ([`HintDb.kt`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/HintDb.kt)) — отдельная таблица с FK на lexeme, но это **специализированный subentity** (один тип = одна таблица), не generic компонент. Schema: `id, lexemeId, value, addDate, changeDate, removeDate`.
- **SampleDb** ([`SampleDb.kt`](../../../core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/SampleDb.kt)) — тоже специализированный subentity (`value, source, addDate, ...`), не generic.
- **TranslationApiEntity / DefinitionApiEntity** — value-classes (одно поле `value: String`). Не sealed.
- **WordClass / `options`** — два plain-поля `lexemes.word_class` / `lexemes.options` (см. LexemeDb выше). Никакой полиморфии.

**Вывод:** в проекте **нет аналога generic «component с template»** — IS481 это первая «generic-полиморфная» абстракция на уровне data. Это **зелёное поле** — нет существующего паттерна который надо консистентно расширять. Контракт пишется с нуля.

## 15. QuizConfig / QuizMode — текущее отсутствие

Поиск `QuizConfig`, `QuizMode`, `quiz_configs` по проекту — **нет ни одного определения**. Существует только `QuizType` enum внутри quiz/chat ([`QuizChatUseCaseImpl.kt:11`](../../../app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt) импорт `me.apomazkin.quiz.chat.entity.QuizType`) — это runtime-enum для UI debug-меток (`GRADES` / `EARLIEST` / `ERRORS`), не «конфиг квиза».

IS481 вводит абсолютно новые типы `QuizConfig` / `ComponentTypeRef` (sealed BuiltIn / UserDefined) — никаких existing analogs.

## 16. QuizChatUseCase / QuizChatUseCaseImpl — где interface, где impl

- **Interface:** [`modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt`](../../../modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt):
  ```kotlin
  interface QuizChatUseCase {
      suspend fun getCurrentDictionaryId(): Long?
      suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
      suspend fun getRandomWriteQuizList(limit: Int, maxGrade: Int, dictionaryId: Long): List<WriteQuiz>
  }
  ```
  3 метода. IS481 добавляет `getQuizConfig(dictionaryId, quizMode): QuizConfig?` (AGG-5 wire).

- **Impl:** [`app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt`](../../../app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt) — действительно в `app/` (F002 подтверждён). Конструктор (:18-21): `dictionaryApi, quizApi, prefsProvider`. Не использует `lexemeApi` напрямую — Lexeme данные приходят через `WriteQuizComplexEntity` (содержит `lexemeData: LexemeApiEntity`).

`fetchData` логика **в `QuizGameImpl`**, не в `QuizChatUseCaseImpl`:
- [`QuizGameImpl.kt:173-217`](../../../modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt) — `fetchData` вызывает `quizChatUseCase.getCurrentDictionaryId()` + `getRandomWriteQuizList(...)`, потом `.map { it.toQuizItem(...) }` для каждой `WriteQuiz`. IS481 wire (AGG-5): pre-fetch `QuizConfig` здесь же, передать в `toQuizItem`.

## 17. QuizGameImpl.toQuizItem — текущий контракт

[`QuizGameImpl.kt:429-521`](../../../modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt):

```kotlin
fun WriteQuiz.toQuizItem(resourceManager, isDebugOn: Boolean): QuizItem {
    ...
    val translation = lexeme.translation   // :439
    val definition = lexeme.definition     // :440
    return QuizItem(
        answer = word.value,
        fullQuestion = when {
            translation != null -> buildAnnotatedString { ...append(translation.value)... }   // :443-468
            definition != null -> buildAnnotatedString { ...append(definition.value)... }     // :470-497
            else -> throw IllegalArgumentException("No translation or definition")            // :499
        },
        question = buildAnnotatedString {
            if (translation != null) append(translation.value)
            else if (definition != null) append(definition.value)
            else append("No translation or definition")                                       // :507
        },
        info = QuizItem.QuizInfo(...)
    )
}
```

**Текущее поведение:** translation приоритетнее definition (when-cascade); если оба null — `throw IllegalArgumentException` (:499). IS481 AGG-5 переписывает: lookup через `QuizConfig.componentRefs`, для каждой ref резолв `ComponentValue` из `lexeme.components`, если нет — **graceful skip (null)**, не throw. **Возвращаемый тип меняется** на `QuizItem?` (либо filter-out на уровне `.map`).

Существующий тест на сценарий «empty list» — [`QuizGameImplEmptyListTest.kt`](../../../modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/quiz/QuizGameImplEmptyListTest.kt) (есть в проекте). Нет тестов на `toQuizItem` с обоими null (упирается в `throw`).

## 18. WordCardState — текущая структура State

[`modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt`](../../../modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt):

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

`LexemeState` (:51-59):
```kotlin
data class LexemeState(
    val id: Long = NOT_IN_DB,
    val translation: TextValueState? = null,
    val definition: TextValueState? = null,
) {
    val canAddTranslation: Boolean get() = translation == null
    val canAddDefinition: Boolean get() = definition == null
}
```

`canAddTranslation` / `canAddDefinition` — **computed properties в State** (нарушает project memory «UI flags must be explicit fields»). IS481 AGG-6 добавляет в **WordCardState** новый flag `hasDefinitionComponent: Boolean` (плоский, не в LexemeState — это per-dictionary настройка, не per-lexeme).

`Lexeme.toLexemeState()` (:245-261) — маппер domain → State, использует `this.translation` / `this.definition` (shim-поля).

## 19. AddLexemeMeaningRow — текущий UI с chip

[`modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddLexemeMeaningRow.kt:29-68`](../../../modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddLexemeMeaningRow.kt):

```kotlin
@Composable
internal fun AddLexemeMeaningRow(
    canAddTranslation: Boolean,
    canAddDefinition: Boolean,
    enabled: Boolean,
    onCreateTranslation: () -> Unit,
    onCreateDefinition: () -> Unit,
) {
    Column { ...
        FlowRow {
            if (canAddTranslation) SubentityChip(labelRes = R.string.word_card_bottom_translation, ...)
            if (canAddDefinition) SubentityChip(labelRes = R.string.word_card_bottom_definition, ...)
        }
    }
}
```

IS481 AGG-6 (UI блок): chip «Определение» прячется если `state.hasDefinitionComponent == false`. На уровне Composable — добавится параметр (или composition-time AND `canAddDefinition && hasDefinitionComponent`).

## 20. dictionaryTab `LexemeUiItem` — shim-зависимость

[`modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt:22-28`](../../../modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt):

```kotlin
fun Lexeme.toUiItem(): LexemeUiItem = LexemeUiItem(
    id = lexemeId.id,
    translation = translation?.let { TranslationUiEntity(it.value) },
    definition = definition?.let { DefinitionUiEntity(it.value) },
    ...
)
```

Использует shim-поля `Lexeme.translation` / `.definition`. IS481 B4/C2: типы остаются как `@Deprecated` value-classes + nullable поля в `Lexeme` data class, **никаких computed extensions** (B4/C2 решение). dictionaryTab продолжит работать без правок.

## 21. core-db-api build.gradle — текущий граф зависимостей

[`core/core-db-api/build.gradle.kts`](../../../core/core-db-api/build.gradle.kts) — module deps:
- `kotlinLibs.coroutinesCore`
- `datastoreLibs.paging`
- **НЕТ** `implementation(project(":modules:domain:lexeme"))`.

`core-db-api` сейчас не зависит от domain. IS481 MIN-2 добавляет dep edge (`ComponentValueApiEntity.data: ComponentValueData` — тип из domain → api должен знать domain).

`modules/domain/lexeme/build.gradle.kts` — pure-kotlin (`org.jetbrains.kotlin.jvm`), без Android. Если api начнёт зависеть от lexeme — lexeme должен остаться JVM-only (Android `library` не нужен; api это Android library, может depend on JVM module).

## 22. Test conventions

Существующие test-файлы относительные к фиче:
- [`modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DatasourceEffectHandlerTest.kt`](../../../modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DatasourceEffectHandlerTest.kt) — тест на effect handler (paths через `wordCardUseCase`).
- [`modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/TranslationManagementTest.kt`](../../../modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/TranslationManagementTest.kt) и `DefinitionManagementTest.kt` — Reducer tests на translation/definition flows.
- [`modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/SpecializedLexemeExtTest.kt`](../../../modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/SpecializedLexemeExtTest.kt) — extension tests на State mutations.
- Существующий [`core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/BundledSqliteFeatureTest.kt`](../../../core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/BundledSqliteFeatureTest.kt) — добавлен в prereq для verify bundled driver feature (smoke на `json_insert($, '$[#]', ...)` — релевантен для AGG-8 в IS481 migration).

Test infrastructure для миграций (`BaseMigration.kt`, `Schemable.kt`, `MigrationTestHelper`) — **удалён в prereq** (см. AGG-12), IS481 пишет новый migration test с нуля.

---

## Вердикт

**Аналог: не найден.**

Generic «component с template» — это **первая полиморфная абстракция** в data-слое проекта. Существующие subentity (`Hint`, `Sample`, `Translation`, `Definition`) — все **специализированные** (one type = one table или one nullable column). Никаких sealed-полиморфных шаблонов значений (`text` / `long-text` / `image`), никаких lookup по `system_key`, никаких `ComponentTypeRef`-абстракций.

Из релевантных patterns, которые **можно переиспользовать в IS481 без изобретения**:
1. **Atomic compound INSERT** через `@Transaction` на default-method DAO — паттерн `WordDao.addLexemeWithQuiz` (`WordDao.kt:164-169`). Применяется к `addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent` / расширению `addDictionary` под auto-INSERT default `quiz_configs`.
2. **Multi-level @Relation** через wrapper entity — паттерн `TermDbEntity → LexemeDbEntity → SampleDb`. Применяется к `LexemeDbEntity → ComponentValueWithType → ComponentTypeDb`.
3. **UseCase Impl в `app/`** — паттерн `WordCardUseCaseImpl` / `QuizChatUseCaseImpl`. Применяется AGG-2 (LexemeMapper.toDomain), AGG-6 (WordCardUseCaseImpl переписка).
4. **Sealed result + null-on-error** — паттерн `RemoveTranslationResult` / `RemoveDefinitionResult`. Применим к новым generic операциям если хочется semantic-разные success-исходы (например `RemoveComponentResult.{ComponentRemoved, LexemeCascadeRemoved}` — но в скоупе IS481 это не требуется, definition-обёртки удаляются по AGG-6).
5. **Mapper в `app/.../mapper/` либо рядом с UseCaseImpl** — обе конвенции существуют (LexemeMapper.kt отдельным файлом vs inline в QuizChatUseCaseImpl); IS481 расширяет уже-выделенный `LexemeMapper.kt`.

`QuizConfig` / `ComponentTypeRef` / `BuiltInComponent` / `ComponentTemplate` / `ComponentValueData` — **полностью новые типы**, никаких predecessors. Контракт пишется с нуля по дизайну plan/04.

_model: claude-opus-4-7[1m]_
