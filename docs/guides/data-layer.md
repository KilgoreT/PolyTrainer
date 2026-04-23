# Слой данных

## Архитектура

```
UseCase (интерфейс в feature модуле)
    |
    | CoreDbApi (контракт)
    v
CoreDbApiImpl (реализация)
    |
    | WordDao (Room DAO)
    v
Room Database (SQLite)
```

## CoreDbApi — контракт базы данных

Единый интерфейс с вложенными API по доменам:

```kotlin
interface CoreDbApi {
    interface LangApi {
        suspend fun addLang(numericCode: Int, code: String, name: String)
        suspend fun getLangList(): List<LanguageApiEntity>
        fun flowLangList(): Flow<List<LanguageApiEntity>>
    }

    interface TermApi {
        suspend fun getTermById(termId: Long): TermApiEntity?
        fun searchTermsPaging(langId: Long, query: String): Flow<PagingData<TermApiEntity>>
    }

    interface WordApi {
        suspend fun addWordSuspend(langId: Long, value: String): Long
        suspend fun deleteWordSuspend(wordId: Long)
        suspend fun updateWordSuspend(wordId: Long, value: String)
    }

    interface LexemeApi {
        suspend fun addLexeme(wordId: Long): LexemeApiEntity?
        suspend fun updateLexemeTranslation(lexemeId: Long, translation: String)
        suspend fun deleteLexeme(lexemeId: Long)
    }

    interface QuizApi {
        suspend fun getRandomWriteQuizList(limit: Int, maxGrade: Int, langId: Long): List<WriteQuizComplexEntity>
        suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
    }

    interface StatisticApi {
        fun flowWordCount(langId: Long): Flow<Int>
        fun flowLexemeCount(langId: Long): Flow<Int>
    }
}
```

## Room Database

```kotlin
@Database(
    entities = [WordDb::class, LexemeDb::class, HintDb::class,
                SampleDb::class, WriteQuizDb::class, LanguageDb::class],
    version = 10,
)
@TypeConverters(DateTimeConverter::class)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
```

- Текущая версия: **10**
- Миграции: 1→2, 2→3, ..., 9→10 (все зарегистрированы)
- Схемы экспортируются в `core/core-db-impl/schemas/`
- Один DAO (`WordDao`) на всю базу

## Маппинг сущностей

Три слоя сущностей:

```
Room Entity (WordDb, LexemeDb)        — БД-уровень
    ↓ .toApiEntity()
API Entity (WordApiEntity, TermApiEntity) — контракт
    ↓ .toDomainEntity()
Domain Entity (Word, Term, Lexeme)    — бизнес-логика
```

Пример маппинга:

```kotlin
// DB → API (в файле entity)
fun TermDbEntity.toApiEntity() = TermApiEntity(
    wordApiEntity = word.toApiEntity(),
    lexemeList = lexemes.map { it.toApiEntity() }
)

// API → Domain (в UseCase модуле)
fun TermApiEntity.toDomainEntity() = Term(
    wordId = WordId(wordApiEntity.id),
    word = Word(wordApiEntity.value),
    addedDate = wordApiEntity.addDate,
    lexemeList = lexemeList.map { it.toDomainEntity() }
)
```

## UseCase

Интерфейс определяется в feature-модуле, реализация — в app-модуле:

```kotlin
// modules/screen/wordcard/deps/WordCardUseCase.kt
interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(wordId: Long, value: String)
    suspend fun addLexeme(wordId: Long): Lexeme?
}

// app/.../WordCardUseCaseImpl.kt
class WordCardUseCaseImpl @Inject constructor(
    private val wordApi: CoreDbApi.WordApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
) : WordCardUseCase {

    override suspend fun getTermById(wordId: Long): Term? {
        return termApi.getTermById(wordId)?.toDomainEntity()
    }

    override suspend fun deleteWord(wordId: Long) {
        wordApi.deleteWordSuspend(wordId)
    }
}
```

## DataStore Preferences

```kotlin
// modules/datasource/prefs/PrefsProvider.kt
class PrefsProvider(context: Context) {
    private val dataStore = context.createDataStore(name = "lexemePrefStore")

    suspend fun getInt(key: PrefKey): Int? { ... }
    suspend fun setInt(key: PrefKey, value: Int) { ... }
    fun getIntFlow(key: PrefKey): Flow<Int> { ... }
    suspend fun getBoolean(key: PrefKey): Boolean? { ... }
    suspend fun setBoolean(key: PrefKey, value: Boolean) { ... }
    fun getBooleanFlow(key: PrefKey): Flow<Boolean> { ... }
}

enum class PrefKey {
    CURRENT_LANG_NUMERIC_CODE_INT,
    CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN,
    CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN,
    CHAT_DEBUG_STATUS_BOOLEAN,
}
```

Используется в `MateFlowHandler` для реактивного наблюдения:

```kotlin
class AppBarFlowHandler(private val prefsProvider: PrefsProvider) : MateFlowHandler<Msg, Effect> {
    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            combine(
                prefsProvider.getBooleanFlow(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN),
                prefsProvider.getBooleanFlow(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN),
                prefsProvider.getBooleanFlow(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN),
            ) { earliest, mistakes, debug ->
                Msg.UpdateMenu(earliest, mistakes, debug)
            }.collect { send(it) }
        }
    }
}
```

## Конвенции

1. **Один DAO** на всю базу (исторически, может измениться).
2. **Три слоя маппинга**: DB → API → Domain. Каждый через extension-функцию.
3. **UseCase интерфейс** в feature модуле, реализация в app модуле.
4. **CoreDbProvider** — service locator для доступа к API из DI.
5. **Все DB-операции** через `withContext(Dispatchers.IO)`.
6. **Пагинация** через `PagingData` + Room Paging (pageSize=50).
7. **Foreign keys** с CASCADE delete на всех связях.
