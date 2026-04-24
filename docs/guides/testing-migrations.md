# Тестирование Room-миграций

## Каркас

Кастомный фреймворк для тестирования миграций. Расположение:

```
core/core-db-impl/src/androidTest/java/.../room/
├── base/
│   ├── BaseMigration.kt      — Базовый класс теста миграции
│   └── Schemable.kt          — Интерфейс версионированной схемы
├── schemable/
│   ├── WordV1.kt, WordV3.kt, WordV5.kt, WordV8.kt
│   ├── LexemeV9.kt, LexemeV10.kt
│   └── WriteQuizV1.kt, WriteQuizV5.kt, WriteQuizV10.kt
├── migrations/
│   ├── MigrationFrom01to02.kt
│   ├── ...
│   └── MigrationFrom09to10.kt
├── dataSource/
│   └── DataProvider.kt        — Тестовые данные
├── utils/
│   ├── CommonExtensions.kt    — hasTable, hasColumns, checkData, toDatabase
│   └── CommonMethods.kt       — selectAllFromTable
├── Schema.kt                  — Версионированные схемы для всех таблиц
└── AllMigrationTest.kt        — Тест полной цепочки миграций
```

---

## Архитектура

### BaseMigration

Абстрактный класс, от которого наследуется каждый тест миграции.

```kotlin
@RunWith(AndroidJUnit4::class)
abstract class BaseMigration {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        Database::class.java
    )

    abstract fun getMigrationClass(): Migration
    abstract fun getCurrentVersion(): Int

    fun runMigrateDbTest(
        onCreate: (SupportSQLiteDatabase) -> Unit,
        afterCreateCheck: (SupportSQLiteDatabase) -> Unit,
        afterMigrationCheck: (SupportSQLiteDatabase) -> Unit,
    )
}
```

`runMigrateDbTest` — ключевой метод. Три фазы:

1. **`onCreate`** — вставить тестовые данные в БД текущей версии
2. **`afterCreateCheck`** — проверить данные ДО миграции (верификация тестовой среды)
3. **`afterMigrationCheck`** — проверить данные ПОСЛЕ миграции (основная проверка)

Внутри:
```kotlin
var db = helper.createDatabase(databaseName, currentVersion).apply {
    onCreate.invoke(this)           // вставляем данные
    afterCreateCheck.invoke(this)   // проверяем что вставились
}
db.close()
db = helper.runMigrationsAndValidate(     // запускаем миграцию
    databaseName, currentVersion + 1, true, migration
)
afterMigrationCheck.invoke(db)    // проверяем результат
```

### Schemable<T>

Интерфейс для версионированных схем таблиц. Каждая версия таблицы — отдельный object.

```kotlin
interface Schemable<T> :
    TableName,            // val tableName: String
    ColumnId,             // val columnId: String (default "id")
    ColumnListable,       // val columnList: Array<String>
    ContentValue<T>,      // fun asContentValue(list: List<T>): List<ContentValues>
    FromDatabase<T>,      // fun getFromDatabase(db: SupportSQLiteDatabase): List<T>
    DataProvider<T>       // fun data(): List<T>
```

Что реализует каждый Schemable:
- `tableName` — имя таблицы в этой версии схемы
- `columnList` — список колонок для проверки `hasColumns()`
- `asContentValue()` — конвертация entity → ContentValues для INSERT
- `getFromDatabase()` — чтение из БД через cursor → entity
- `data()` — тестовые данные для вставки

---

## Как устроен тест миграции

### Пример: MigrationFrom09to10

```kotlin
class MigrationFrom09to10 : BaseMigration() {

    override fun getMigrationClass() = migration_9_10
    override fun getCurrentVersion() = 9

    @Test
    fun from09to10() {
        runMigrateDbTest(
            // 1. ВСТАВИТЬ ДАННЫЕ (версия 9)
            onCreate = { database ->
                WordV8
                    .asContentValue(WordV8.data())
                    .toDatabase(database = database, table = WordV8.tableName)
                LexemeV9
                    .asContentValue(LexemeV9.data())
                    .toDatabase(database = database, table = LexemeV9.tableName)
                WriteQuizV5
                    .asContentValue(WriteQuizV5.data())
                    .toDatabase(database = database, table = WriteQuizV5.tableName)
            },

            // 2. ПРОВЕРИТЬ ДО МИГРАЦИИ
            afterCreateCheck = { database ->
                // Таблица существует?
                database.hasTable(tableName = WordV8.tableName)
                // Колонки на месте?
                database.hasColumns(
                    tableName = WordV8.tableName,
                    columns = WordV8.columnList
                )
                // Данные корректны?
                WordV8.getFromDatabase(database)
                    .checkData(
                        afterMigrationState = false,
                        origin = WordV8.data(),
                        originMatcher = { wordDb ->
                            WordV8.data().firstOrNull { wordDb.id == it.id }
                        },
                        checkMatcher = { inDb, origin ->
                            inDb.id == origin.id
                                && inDb.langId == origin.langId
                                && inDb.value == origin.value
                        }
                    )
            },

            // 3. ПРОВЕРИТЬ ПОСЛЕ МИГРАЦИИ
            afterMigrationCheck = { database ->
                // Новая таблица/колонки?
                database.hasTable(tableName = LexemeV10.tableName)
                database.hasColumns(
                    tableName = LexemeV10.tableName,
                    columns = LexemeV10.columnList
                )
                // Данные мигрировались корректно?
                LexemeV10.getFromDatabase(database)
                    .checkData(
                        origin = LexemeV9.data(),
                        originMatcher = { lexemeDb ->
                            LexemeV9.data().firstOrNull { lexemeDb.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                && migrated.wordId == origin.wordId
                                && migrated.translation == origin.translation
                        }
                    )
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 9
    }
}
```

---

## Утилиты

### Проверка структуры БД

```kotlin
// Проверить что таблица существует
database.hasTable(tableName = "words")

// Проверить что колонки существуют
database.hasColumns(
    tableName = "words",
    columns = arrayOf("id", "lang_id", "value", "add_date")
)
```

### Вставка тестовых данных

```kotlin
// Schemable → ContentValues → INSERT
WriteQuizV10
    .asContentValue(WriteQuizV10.data())
    .toDatabase(database = database, table = WriteQuizV10.tableName)
```

### Проверка данных

```kotlin
// Прочитать из БД → сравнить с origin
LexemeV10.getFromDatabase(database)
    .checkData(
        origin = LexemeV9.data(),                   // данные до миграции
        originMatcher = { migrated: LexemeDb ->      // как найти origin по migrated
            LexemeV9.data().firstOrNull { migrated.id == it.id }
        },
        checkMatcher = { migrated, origin ->          // сравнение полей
            migrated.id == origin.id
                && migrated.wordId == origin.wordId
        }
    )
```

`checkData` проверяет:
1. Количество записей совпадает
2. Для каждой записи находит origin через `originMatcher`
3. Сравнивает поля через `checkMatcher`
4. Логирует каждое сравнение

`afterMigrationState = false` — используется в `afterCreateCheck` для правильного заголовка лога ("Creating Test" vs "Migration Test").

---

## Как добавить новую миграцию

### Шаг 1: Создать Schemable для новой версии

Если таблица меняет структуру — создать файл в `schemable/`:

```kotlin
// schemable/WriteQuizV11.kt
object WriteQuizV11 : Schemable<WriteQuizDb> {

    private const val COLUMN_DICTIONARY_ID = "dictionary_id"  // переименовано
    private const val COLUMN_LEXEME_ID = "lexeme_id"
    // ...

    override val tableName = "write_quiz"

    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_DICTIONARY_ID,    // было COLUMN_LANG_ID
        COLUMN_LEXEME_ID,
        // ...
    )

    override fun asContentValue(list: List<WriteQuizDb>): List<ContentValues> =
        list.map { writeQuizDb ->
            ContentValues().apply {
                put(columnId, writeQuizDb.id)
                put(COLUMN_DICTIONARY_ID, writeQuizDb.dictionaryId)
                // ...
            }
        }

    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WriteQuizDb> {
        // cursor → entity маппинг с новыми именами колонок
    }

    override fun data(): List<WriteQuizDb> {
        // тестовые данные
    }
}
```

### Шаг 2: Создать тест миграции

```kotlin
// migrations/MigrationFrom10to11.kt
class MigrationFrom10to11 : BaseMigration() {

    override fun getMigrationClass() = migration_10_11
    override fun getCurrentVersion() = 10

    @Test
    fun from10to11() {
        runMigrateDbTest(
            onCreate = { database ->
                // вставить данные через V10 Schemable
            },
            afterCreateCheck = { database ->
                // проверить через V10 Schemable
            },
            afterMigrationCheck = { database ->
                // проверить через V11 Schemable
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 10
    }
}
```

### Шаг 3: Обновить AllMigrationTest

**ОБЯЗАТЕЛЬНО.** Добавить новый тест-класс в `AllMigrationTest.kt` — это Suite, который позволяет запустить все миграции одной кнопкой.

```kotlin
// AllMigrationTest.kt
@RunWith(Suite::class)
@Suite.SuiteClasses(
    MigrationFrom01to02::class,
    // ...
    MigrationFrom09to10::class,
    MigrationFrom10to11::class,  // ← ДОБАВИТЬ
)
class AllMigrationTest
```

Без этого новый тест запускается только отдельно, а полный прогон его пропустит.

---

## Schema JSON

Room автоматически генерирует JSON-файл схемы при первой сборке модуля (`kspDebugKotlin`). Файл появляется в `core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/<version>.json`.

Чтобы сгенерировать схему для новой версии:
1. Обновить `version` в `Database.kt`
2. Обновить entity (переименовать поля/таблицы)
3. Запустить `./gradlew :core:core-db-impl:kspDebugKotlin`
4. JSON появится автоматически

Не создавать JSON вручную — Room генерирует его из аннотаций entity и сверяет при запуске миграционных тестов.

## Конвенции

1. **Один тест-класс на миграцию.** `MigrationFrom09to10`, `MigrationFrom10to11`.
2. **Schemable на каждую версию таблицы.** `WriteQuizV5`, `WriteQuizV10`, `WriteQuizV11`.
3. **`data()`** возвращает минимум 2-3 записи с разными значениями (edge cases: nulls, zeros, dates).
4. **`afterCreateCheck` обязателен** — не пропускать. Верифицирует что тестовая среда корректна.
5. **`checkMatcher` проверяет каждое поле** — не только id, но и все мигрирующие данные.
6. **Тестовые данные** — для general-purpose данных использовать `DataProvider`, для версионированных — `Schemable.data()`.
7. **Запуск:** `./gradlew :core:core-db-impl:connectedDebugAndroidTest` (нужен эмулятор/устройство).
