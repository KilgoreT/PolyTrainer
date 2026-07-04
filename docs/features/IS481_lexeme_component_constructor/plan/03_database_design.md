# Database design: ComponentType + ComponentValue

Конкретика для разработчика data-слоя: схема таблиц, Room Entity, DAO, обзор миграции.

**Концептуальные обоснования** (зачем такая схема, scope-разделение built-in/user-defined, immutability, seed) — см. [`04_builtin_strategy.md`](04_builtin_strategy.md).
**Детали миграции** (SQL шаги, bundled SQLite, JSON через `json_object()`, тестовые сценарии) — см. [`05_migration_strategy.md`](05_migration_strategy.md).

---

## 1. Обзор изменений

**Версия БД:** v11 → v12.

| Таблица | Действие |
|---|---|
| `dictionaries` | без изменений |
| `words` | без изменений |
| `lexemes` | **изменена** — удалены колонки `translation`, `definition` |
| `samples` | без изменений |
| `hints` | без изменений |
| `write_quiz_*` | без изменений |
| `component_types` | **новая** |
| `component_values` | **новая** |
| `quiz_configs` | **новая** — конфиг компонентов для quiz по `(dictionary_id, quiz_mode)`. См. [`07_quiz_strategy.md`](07_quiz_strategy.md). |

---

## 2. ER-диаграмма

```
┌──────────────────┐
│  dictionaries    │
└────────┬─────────┘
         │ 1
         │ N
┌────────┴─────────┐         ┌──────────────────────┐
│      words       │         │  component_types     │
└────────┬─────────┘         │  ──────────────────  │
         │ 1                 │  id (PK)             │
         │ N                 │  system_key (NULL)   │ ─ nullable: NULL = user-defined; built-in: "translation" / "definition"
┌────────┴─────────┐         │  dictionary_id (FK)  │ ─ nullable: NULL = global
│     lexemes      │         │  name (NULL)         │ ─ literal display name; NULL = use systemKey from enum
└────────┬─────────┘         │  template_key        │ ─ "text" | "long_text" | "image"
         │ 1                 │  position            │
         │ N                 │  remove_date (NULL)  │ ─ soft-delete для user-defined
   ┌─────┴────────────────┐  └──────────┬───────────┘
   │  component_values    │             │ 1
   │  ──────────────────  │             │ N
   │  id (PK)             │             │
   │  lexeme_id (FK)      │ ────────────┼─────┐
   │  component_type_id   │ ◄───────────┘     │
   │  value (TEXT JSON)   │ ─ json_object('text', ...) для text/long_text; json_object('uri', ...) для image
   └──────────────────────┘                   │
                                              │
                  ┌───────────────────────────┘
                  │ FK ON DELETE CASCADE
                  ▼
         (cascade на удаление типа удалит все его values)
```

**Ключевые связи:**
- `component_types.dictionary_id` → `dictionaries.id` (nullable; NULL = global).
- `component_values.lexeme_id` → `lexemes.id` (CASCADE).
- `component_values.component_type_id` → `component_types.id` (CASCADE).
- `quiz_configs.dictionary_id` → `dictionaries.id` (NOT NULL, CASCADE). Один config на пару `(dictionary_id, quiz_mode)`.

**Double cascade pathway** (MIN-3): при `DELETE FROM dictionaries WHERE id = X` строки `component_values` удаляются по двум путям — через `component_types` (FK `dictionary_id → dictionaries.id` ON DELETE CASCADE → каскад на `component_type_id`) и через `words → lexemes` (FK `lexeme_id → lexemes.id` ON DELETE CASCADE). Оба пути намеренны: первый покрывает delete отдельного `component_type`, второй — delete отдельной lexeme. SQLite обрабатывает idempotent: повторное удаление одной и той же строки безопасно. Не удалять один из CASCADE как «дублирующий» — каждый путь покрывает свой сценарий.

---

## 3. Схема таблиц

### `component_types` (новая)

| Колонка | Тип | Constraint | Описание |
|---|---|---|---|
| `id` | INTEGER | PK, autoincrement | |
| `system_key` | TEXT | nullable, UNIQUE | NULL = user-defined; built-in: ключ enum (`"translation"`). См. `04` § «Слойность». |
| `dictionary_id` | INTEGER | FK → `dictionaries.id` nullable, ON DELETE CASCADE | NULL = global (для всех словарей). |
| `name` | TEXT | nullable | Литерал. NULL для built-in без override (display из enum через `nameRes()`). |
| `template_key` | TEXT | NOT NULL | `"text"` / `"long_text"` / `"image"` (lowercase snake_case по конвенции `word_class`). |
| `position` | INTEGER | NOT NULL, default 0 | Порядок отображения. Built-in идут раньше user-defined. |
| `remove_date` | INTEGER | nullable | Soft-delete (стиль `HintDb.removeDate`). NULL = активный. |

**CHECK constraint:** `name IS NOT NULL OR system_key IS NOT NULL`.

**Индексы:**
- `INDEX(dictionary_id)` — FK.
- `UNIQUE INDEX(system_key)` — защита от дублей built-in (через `@Index(unique=true)`).
- `UNIQUE INDEX(dictionary_id, name)` — защита от дублей user-defined per-dictionary.
- `UNIQUE INDEX(name) WHERE dictionary_id IS NULL AND system_key IS NULL` — **partial index**, защита 4-й комбинации (user-defined global). Создаётся вручную в migration через `connection.execSQL` (extension `androidx.sqlite.execSQL`; Room из аннотаций partial не генерирует). **Note для schema review:** partial UNIQUE не появится в Room `12.json` schema snapshot — Room не моделирует WHERE-clause в индексах. Это норма; не пытаться добавить в snapshot. Migration test должен явно проверить наличие индекса через `SELECT sql FROM sqlite_master WHERE name = 'index_component_types_global_userdef_name'`.

### `component_values` (новая)

| Колонка | Тип | Constraint | Описание |
|---|---|---|---|
| `id` | INTEGER | PK, autoincrement | |
| `lexeme_id` | INTEGER | NOT NULL, FK → `lexemes.id`, ON DELETE CASCADE | |
| `component_type_id` | INTEGER | NOT NULL, FK → `component_types.id`, ON DELETE CASCADE | |
| `value` | TEXT | NOT NULL | JSON. Для `text` / `long_text` — `{"text":"..."}`. Для `image` — `{"uri":"..."}`. JSON формируется через `json_object()` в SQL миграции (JSON1 в bundled SQLite). |

**Индексы:**
- `INDEX(component_type_id)` — FK.
- `UNIQUE INDEX(lexeme_id, component_type_id)` — одна лексема имеет не более одного значения каждого типа. Композитный — покрывает запросы по leading column `lexeme_id` (отдельный `INDEX(lexeme_id)` **не нужен**).

### `lexemes` (изменена)

| Колонка | Тип | Было | Стало |
|---|---|---|---|
| `id` | INTEGER PK | ✓ | ✓ |
| `word_id` | INTEGER FK | ✓ | ✓ |
| `translation` | TEXT? | ✓ | **удалена** (через `ALTER TABLE DROP COLUMN` — bundled SQLite 3.45+) |
| `definition` | TEXT? | ✓ | **удалена** |
| `word_class` | TEXT? | ✓ | ✓ |
| `options` | INTEGER | ✓ | ✓ |
| `add_date` | INTEGER | ✓ | ✓ |
| `change_date` | INTEGER? | ✓ | ✓ |

Никакого recreate-таблицы (см. `05` § «SQL миграция, шаг 6»).

### `quiz_configs` (новая)

Хранит конфиг **исходных компонентов** для quiz по паре `(dictionary_id, quiz_mode)`. Подробности — [`07_quiz_strategy.md`](07_quiz_strategy.md).

| Колонка | Тип | Constraint | Описание |
|---|---|---|---|
| `id` | INTEGER | PK, autoincrement | |
| `dictionary_id` | INTEGER | NOT NULL, FK → `dictionaries.id`, ON DELETE CASCADE | |
| `quiz_mode` | TEXT | NOT NULL | `"write"` (IS481). Future: `"card"`, `"recall"`, ... |
| `component_refs` | TEXT | NOT NULL | JSON array `List<ComponentTypeRef>` (sealed: `BuiltIn(key)` / `UserDefined(name)`). См. `06.md` § JSON serialization. |

**Индексы:**
- `INDEX(dictionary_id)` — FK.
- `UNIQUE INDEX(dictionary_id, quiz_mode)` — один config на пару (словарь, режим). Конфликт INSERT → upsert либо явная ошибка (контракт DAO).

**CASCADE:** удаление dictionary → все его configs удаляются автоматически.

---

## 4. Room Entity

### `ComponentTypeDb`

```kotlin
package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "component_types",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryDb::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dictionary_id"),
        Index(value = ["system_key"], unique = true),
        Index(value = ["dictionary_id", "name"], unique = true),
    ],
)
data class ComponentTypeDb(
    // system_key IMMUTABLE after creation — см. 04_builtin_strategy.md § «Эволюция».
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "system_key") val systemKey: String?,
    @ColumnInfo(name = "dictionary_id") val dictionaryId: Long?,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "template_key") val templateKey: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "remove_date") val removeDate: Date? = null,
)
```

### `ComponentValueDb`

```kotlin
package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "component_values",
    foreignKeys = [
        ForeignKey(
            entity = LexemeDb::class,
            parentColumns = ["id"],
            childColumns = ["lexeme_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ComponentTypeDb::class,
            parentColumns = ["id"],
            childColumns = ["component_type_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("component_type_id"),
        Index(value = ["lexeme_id", "component_type_id"], unique = true),
    ],
)
data class ComponentValueDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "lexeme_id") val lexemeId: Long,
    @ColumnInfo(name = "component_type_id") val componentTypeId: Long,
    @ColumnInfo(name = "value") val value: String,
)
```

### `QuizConfigDb`

```kotlin
package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_configs",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryDb::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dictionary_id"),
        Index(value = ["dictionary_id", "quiz_mode"], unique = true),
    ],
)
data class QuizConfigDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "dictionary_id") val dictionaryId: Long,
    @ColumnInfo(name = "quiz_mode") val quizMode: String,
    @ColumnInfo(name = "component_refs") val componentRefs: String,  // JSON array
)
```

`component_refs` хранит JSON-сериализованный `List<ComponentTypeRef>`. Helper `ComponentTypeRefJson.kt` в `core-db-impl` (Android-зависимый `org.json.JSONObject`). См. [`06_mapping_design.md`](06_mapping_design.md) § «ComponentTypeRef JSON».

### `ComponentValueWithType` (для Multi-level @Relation)

```kotlin
package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ComponentValueWithType(
    @Embedded val value: ComponentValueDb,
    @Relation(parentColumn = "component_type_id", entityColumn = "id")
    val type: ComponentTypeDb,
)
```

### Изменения `LexemeDb`

Удаляются:
```kotlin
@ColumnInfo(name = "translation") val translation: String? = null,
@ColumnInfo(name = "definition") val definition: String? = null,
```

Остальные поля и аннотации сохраняются.

### Изменения `LexemeDbEntity`

Добавляется `@Relation` на компоненты с типами (Multi-level — Room делает batched JOIN):

```kotlin
data class LexemeDbEntity(
    @Embedded val lexemeDb: LexemeDb,
    // camelCase: SampleDb legacy без @ColumnInfo; ComponentValueDb уже snake_case через @ColumnInfo
    @Relation(parentColumn = "id", entityColumn = "lexemeId")
    val sampleDbList: List<SampleDb>,
    @Relation(
        entity = ComponentValueDb::class,
        parentColumn = "id",
        entityColumn = "lexeme_id",
    )
    val componentValueListDb: List<ComponentValueWithType>,
)
```

Маппер `toApiEntity()` использует `componentValueListDb` (тип уже подгружен — N+1 не возникает). Детали маппинга (DB → API → Domain, JSON-сериализация `ComponentValueData`) — см. **[`06_mapping_design.md`](06_mapping_design.md)**.

### Изменения `Database`

```kotlin
@Database(
    entities = [
        WordDb::class,
        LexemeDb::class,
        HintDb::class,
        SampleDb::class,
        WriteQuizDb::class,
        DictionaryDb::class,
        ComponentTypeDb::class,         // ⬅ новое
        ComponentValueDb::class,         // ⬅ новое
        QuizConfigDb::class,             // ⬅ новое
    ],
    version = 12,                        // ⬅ было 11
)
@TypeConverters(DateTimeConverter::class)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun componentTypeDao(): ComponentTypeDao
    abstract fun componentValueDao(): ComponentValueDao
    abstract fun quizConfigDao(): QuizConfigDao
}
```

Никаких новых TypeConverter'ов — `system_key` / `template_key` хранятся как `String`, маппинг в enum происходит в API-маппере (см. `04` § «Слойность»).

### Подключение в RoomModule

```kotlin
Room.databaseBuilder(context, Database::class.java, dbName)
    .setDriver(BundledSQLiteDriver())  // bundled SQLite 3.45+ (см. 05)
    .addMigrations(migration_11_12)    // включает шаг 6 — quiz_configs (см. 05)
    .addCallback(seedCallback)         // seed built-in на fresh install
    .build()
```

---

## 5. DAO

### `ComponentTypeDao`

```kotlin
package me.apomazkin.core_db_impl.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import java.util.Date

@Dao
interface ComponentTypeDao {

    /**
     * Все актуальные типы для словаря: built-in (global) + per-dictionary user-defined.
     * Built-in идут первыми (system_key NOT NULL), внутри — по position.
     */
    @Query("""
        SELECT * FROM component_types
        WHERE (dictionary_id = :dictionaryId OR dictionary_id IS NULL)
          AND remove_date IS NULL
        ORDER BY (system_key IS NULL) ASC, position ASC
    """)
    fun flowTypesForDictionary(dictionaryId: Long): Flow<List<ComponentTypeDb>>

    @Query("""
        SELECT * FROM component_types
        WHERE (dictionary_id = :dictionaryId OR dictionary_id IS NULL)
          AND remove_date IS NULL
        ORDER BY (system_key IS NULL) ASC, position ASC
    """)
    suspend fun getTypesForDictionary(dictionaryId: Long): List<ComponentTypeDb>

    @Query("SELECT * FROM component_types WHERE system_key IS NOT NULL AND remove_date IS NULL")
    suspend fun getBuiltInTypes(): List<ComponentTypeDb>

    /** Built-in lookup по системному ключу. См. 04 § «Резолв имени». */
    @Query("SELECT * FROM component_types WHERE system_key = :key AND remove_date IS NULL")
    suspend fun getBySystemKey(key: String): ComponentTypeDb?

    @Query("SELECT * FROM component_types WHERE id = :id")
    suspend fun getById(id: Long): ComponentTypeDb?

    @Insert
    suspend fun insert(type: ComponentTypeDb): Long

    @Update
    suspend fun update(type: ComponentTypeDb)

    /**
     * Soft-delete только для user-defined. Built-in (`system_key NOT NULL`) защищены
     * на уровне SQL — UPDATE не сработает.
     * См. 04 § «Built-in не удаляется».
     */
    @Query("UPDATE component_types SET remove_date = :now WHERE id = :id AND system_key IS NULL")
    suspend fun softDelete(id: Long, now: Date): Int
}
```

### `ComponentValueDao`

```kotlin
package me.apomazkin.core_db_impl.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.apomazkin.core_db_impl.entity.ComponentValueDb

@Dao
interface ComponentValueDao {

    @Query("SELECT * FROM component_values WHERE lexeme_id = :lexemeId")
    suspend fun getForLexeme(lexemeId: Long): List<ComponentValueDb>

    @Query("SELECT * FROM component_values WHERE id = :id")
    suspend fun getById(id: Long): ComponentValueDb?

    @Query("""
        SELECT * FROM component_values
        WHERE lexeme_id = :lexemeId AND component_type_id = :typeId
    """)
    suspend fun getForLexemeAndType(lexemeId: Long, typeId: Long): ComponentValueDb?

    @Insert
    suspend fun insert(value: ComponentValueDb): Long

    @Update
    suspend fun update(value: ComponentValueDb)

    @Query("DELETE FROM component_values WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM component_values WHERE lexeme_id = :lexemeId AND component_type_id = :typeId")
    suspend fun deleteByLexemeAndType(lexemeId: Long, typeId: Long): Int
}
```

### `QuizConfigDao`

```kotlin
package me.apomazkin.core_db_impl.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.apomazkin.core_db_impl.entity.QuizConfigDb

@Dao
interface QuizConfigDao {

    /** Lookup конфига одного режима одного словаря (главный read-путь quiz session). */
    @Query("SELECT * FROM quiz_configs WHERE dictionary_id = :dictId AND quiz_mode = :mode")
    suspend fun getByDictionaryAndMode(dictId: Long, mode: String): QuizConfigDb?

    @Insert
    suspend fun insert(config: QuizConfigDb): Long

    @Update
    suspend fun update(config: QuizConfigDb)
}
```

CASCADE по FK гарантирует чистку при удалении dictionary — отдельный `deleteByDictionary` не нужен на уровне Kotlin.

---

## 6. Миграция v11 → v12

Краткий обзор. Полные SQL шаги, тестовые сценарии, обоснование bundled SQLite — см. **[`05_migration_strategy.md`](05_migration_strategy.md)**.

**Скоуп:**
- `translation` → built-in `component_types(system_key='translation')` + `component_values` с ссылкой на этот тип.
- `definition` → **per-dictionary user-defined** `component_types(system_key=NULL, dictionary_id=X, name='Definition')` + `component_values`. Создаётся только для словарей где есть data.
- Удаление колонок `translation` / `definition` из `lexemes` через `ALTER TABLE DROP COLUMN` (bundled SQLite 3.45+).
- **Шаг 6** — CREATE TABLE `quiz_configs` + индексы + INSERT default config `[BuiltIn(TRANSLATION)]` для всех словарей + UPDATE добавление `UserDefined("Definition")` для словарей с definition. См. `05.md` § «Шаг 6: Quiz configs».

**Ключевые технические решения:**
- **Bundled SQLite** через `androidx.sqlite:sqlite-bundled` + `BundledSQLiteDriver` — даёт ALTER TABLE DROP COLUMN, JSON1 (`json_object`), partial / expression indexes.
- **Миграция в транзакции** — Room автоматически оборачивает `migrate(connection)` в транзакцию (см. `05.md` § 0); явных BEGIN/COMMIT не нужно.
- **Built-in seed через общую функцию** `seedBuiltIns(connection: SQLiteConnection)` — вызывается из Migration (для upgrade) и из `RoomDatabase.Callback.onCreate(connection)` (для fresh install). Идемпотентна через `INSERT OR IGNORE`. Использует `connection.execSQL(...)` через extension `androidx.sqlite.execSQL`.

---

## 7. Тестирование

Минимальный список assert'ов миграционного теста. Полный список — `05` § «Тестовые сценарии».

- После миграции `getBySystemKey("translation")` возвращает row.
- Для каждой `lexemes.translation IS NOT NULL` существует `component_values(component_type_id = built-in translation id)`.
- Для каждого словаря с `definition IS NOT NULL` существует user-defined `component_types(name='Definition')` + соответствующие `component_values`.
- В таблице `lexemes` нет колонок `translation` / `definition`.
- Удаление словаря не задевает built-in типы (`getBySystemKey("translation")` всё ещё возвращает row).
- На fresh install (без миграции) `getBySystemKey("translation")` возвращает row из Callback seed.
- После миграции для каждого словаря существует row в `quiz_configs` с `quiz_mode='write'` и `component_refs` содержит как минимум `BuiltIn(translation)`.
- Для словарей с `definition IS NOT NULL` хотя бы у одной лексемы — `component_refs` дополнительно содержит `UserDefined("Definition")` (порядок: translation, потом definition).
- Удаление dictionary каскадно удаляет все его строки из `quiz_configs` (FK CASCADE).
- `UNIQUE(dictionary_id, quiz_mode)` — повторный INSERT для той же пары падает.

Спецсимволы в text-data (эмодзи, переносы строк, кавычки) — отдельные параметризованные тесты, см. `05`.
