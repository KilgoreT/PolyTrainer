# Infra design tree — IS481 Prepared (Bundled SQLite driver prereq)

DAG файлов для реализации prereq-фичи «Bundled SQLite driver». Каждый узел — файл. Зависимости определяют порядок: узел с пустым `depends` — стартовый, узлы с одинаковым `depends` могут идти параллельно.

Пометки: `[+]` создание, `[~]` изменение, `[-]` удаление.

## Часть 1: Граф

```yaml
- id: 0
  file: deps/datastore.versions.toml
  action: "~"
  depends: []

- id: 1
  file: core/core-db-impl/build.gradle.kts
  action: "~"
  depends: [0]

- id: 2
  file: core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt
  action: "~"
  depends: [1]

- id: 3
  file: core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/base/BaseMigration.kt
  action: "~"
  depends: [1]

- id: 4
  file: core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/BundledSqliteFeatureTest.kt
  action: "+"
  depends: [1]

- id: 5
  file: app/proguard-rules.pro
  action: "~"
  depends: [2]
```

**Топология:**

- `0` (version catalog) — корень DAG, не зависит ни от чего.
- `1` (`core-db-impl/build.gradle.kts`) — зависит от `0` (нужен алиас в catalog'е чтобы `datastoreLibs.sqliteBundled` резолвился).
- `2` (`RoomModule.kt`), `3` (`BaseMigration.kt`), `4` (`BundledSqliteFeatureTest.kt`) — все зависят только от `1` (как только зависимость в classpath — каждый из трёх правится независимо). **Идут параллельно.**
- `5` (`proguard-rules.pro`) — зависит от `2`: keep-rules имеет смысл активировать после того как `RoomModule` начал реально создавать `BundledSQLiteDriver` в production-сборке. Формально файлы независимы, но логически 5 защищает то, что делает 2 — поэтому 5 идёт после 2.

**Циклов нет** (граф — линеаризуется в `0 → 1 → {2 ∥ 3 ∥ 4} → 5`).

**Параллелизм:** узлы 2, 3, 4 после узла 1 идут параллельно.

## Часть 2: Детали

### Узел 0 — `deps/datastore.versions.toml` [~]

**Было:**

```toml
[versions]
datastore-version = "1.1.7"
roomVersion = "2.8.4"
documentfileVersion = "1.1.0"
pagingVersion = "3.3.6"

[libraries]
preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore-version" }
roomRuntime = { group = "androidx.room", name = "room-runtime", version.ref = "roomVersion" }
# ...
roomTesting = { group = "androidx.room", name = "room-testing", version.ref = "roomVersion" }
```

**Стало:**

```toml
[versions]
datastore-version = "1.1.7"
roomVersion = "2.8.4"
sqliteBundledVersion = "<pinned>"   # ≥ обеспечивающая SQLite 3.45+
documentfileVersion = "1.1.0"
pagingVersion = "3.3.6"

[libraries]
# ... existing aliases ...
sqliteBundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "sqliteBundledVersion" }
```

Конкретное значение `<pinned>` подбирается на этапе имплементации из актуальных релизов `androidx.sqlite:sqlite-bundled`, упаковывающих SQLite ≥ 3.45 (требование acceptance 6.2 — `sqlite_version() >= 3.45`). Pin обязателен — без него gradle resolution может выбрать downgrade'нутую транзитивную версию (`02_scope.md` строка 16).

### Узел 1 — `core/core-db-impl/build.gradle.kts` [~]

**Было** (фрагмент `dependencies` блока, lines 44-55):

```kotlin
//Room
implementation(datastoreLibs.roomRuntime)
implementation(datastoreLibs.roomKtx)
ksp(datastoreLibs.roomCompiler)
implementation(datastoreLibs.roomPaging)

//Dagger2
implementation(diLibs.dagger)
ksp(diLibs.daggerCompiler)

androidTestImplementation(project("path" to ":modules:core:ui"))
androidTestImplementation(datastoreLibs.roomTesting)
```

**Стало:**

```kotlin
//Room
implementation(datastoreLibs.roomRuntime)
implementation(datastoreLibs.roomKtx)
ksp(datastoreLibs.roomCompiler)
implementation(datastoreLibs.roomPaging)
implementation(datastoreLibs.sqliteBundled)          // ← bundled SQLite driver (production)

//Dagger2
implementation(diLibs.dagger)
ksp(diLibs.daggerCompiler)

androidTestImplementation(project("path" to ":modules:core:ui"))
androidTestImplementation(datastoreLibs.roomTesting)
androidTestImplementation(datastoreLibs.sqliteBundled) // ← bundled SQLite driver (androidTest)
```

Без `androidTestImplementation` androidTest source set не скомпилируется (узел 3 и узел 4 импортируют `BundledSQLiteDriver` из `androidx.sqlite.driver.bundled`).

### Узел 2 — `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt` [~]

**Было** (lines 34-51, тело `provideDatabase`):

```kotlin
@Singleton
@Provides
fun provideDatabase(context: Context): Database {
    return Room.databaseBuilder(context, Database::class.java, "name")
        .addMigrations(
            migration_1_2, migration_2_3, migration_3_4, migration_4_5,
            migration_5_6, migration_6_7, migration_7_8, migration_8_9,
            migration_9_10, migration_10_11,
        )
        .build()
}
```

**Стало** (псевдокод):

```kotlin
@Singleton
@Provides
fun provideDatabase(context: Context): Database {
    return Room.databaseBuilder<Database>(
            context = context,
            name = context.getDatabasePath("name").absolutePath,
        )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(
            migration_1_2, migration_2_3, migration_3_4, migration_4_5,
            migration_5_6, migration_6_7, migration_7_8, migration_8_9,
            migration_9_10, migration_10_11,
        )
        .build()
}
```

**Ключевые изменения:**

- Legacy 3-arg KClass-builder `Room.databaseBuilder(context, Database::class.java, "name")` → KMP-builder `Room.databaseBuilder<Database>(context, name)` (Room 2.7+).
- `.setDriver(BundledSQLiteDriver())` — bundled driver вместо системного. **Критично:** на legacy builder `setDriver` молча игнорируется (`02_scope.md` строка 23), поэтому переписка builder'а и `setDriver` — атомарны, ровно одним коммитом узла 2.
- `.setQueryCoroutineContext(Dispatchers.IO)` — корутинный контекст для query-операций (Room 2.7+ KMP-builder требование для async API).
- Имя БД: legacy builder принимал имя файла `"name"` и сам резолвил под `context.getDatabasePath()`; в KMP-builder путь к файлу резолвится явно (точная сигнатура и поведение по `name` уточняются по актуальной Room 2.8.4 docs при имплементации — псевдокод выше показывает принцип, не финальную форму).
- **10 миграций** (`migration_1_2 … migration_10_11`) — **остаются без изменений**. Compat layer Room 2.8 покрывает legacy `migrate(db: SupportSQLiteDatabase)` API (verify через узел 3 + прогон `AllMigrationTest`).
- **Новые импорты:** `androidx.sqlite.driver.bundled.BundledSQLiteDriver`, `kotlinx.coroutines.Dispatchers`.
- **Структура `@Module` / `@Singleton` / `@Provides` — НЕ меняется.** `provideWordDao` остаётся как есть. Dagger-граф не меняется.

### Узел 3 — `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/base/BaseMigration.kt` [~]

**Было** (lines 18-22):

```kotlin
@get:Rule
val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    Database::class.java
)
```

**Стало** (псевдокод):

```kotlin
@get:Rule
val helper: MigrationTestHelper = MigrationTestHelper(
    instrumentation = InstrumentationRegistry.getInstrumentation(),
    databaseClass = Database::class.java,
    driver = BundledSQLiteDriver(),
    // дополнительные параметры (file, databaseFactory) — по актуальной Room 2.8.4 сигнатуре
)
```

**Ключевые изменения:**

- Legacy 2-arg ctor `MigrationTestHelper(instrumentation, Database::class.java)` → Room 2.7+ KMP-форма с явным driver-параметром.
- Конкретная сигнатура ctor (порядок аргументов, дополнительные параметры `file`/`databaseFactory`) — подбирается из актуального Room 2.8.4 API при имплементации. Принцип — `BundledSQLiteDriver()` передаётся явно, чтобы helper создавал тестовую БД через bundled driver, а не системный (без этого acceptance 6.1 формально не покрывает regression под bundled — `02_scope.md` строка 24, F001).
- **Новый импорт:** `androidx.sqlite.driver.bundled.BundledSQLiteDriver`.
- Тело `runMigrateDbTest` (lines 47-68) — **НЕ меняется**. Callbacks остаются на `SupportSQLiteDatabase` (compat layer Room 2.8 оборачивает bundled-connection в `SupportSQLiteDatabase` для legacy API). 10 тест-классов `MigrationFromNNtoMM` не трогаются.
- `setUp`/`tearDown` (lines 24-30), `getMigrationClass`/`getCurrentVersion` (lines 32-33) — без изменений.

### Узел 4 — `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/BundledSqliteFeatureTest.kt` [+]

**Назначение:** smoke androidTest, независимо подтверждающий что bundled driver активен и предоставляет SQLite features ≥ 3.45 (acceptance 6.2). Не зависит от Room-стека, `MigrationTestHelper`, реальной БД приложения. Harness — прямой `BundledSQLiteDriver().open(":memory:")`.

**Ключевые сигнатуры (псевдокод):**

```kotlin
package me.apomazkin.core_db_impl.room

@RunWith(AndroidJUnit4::class)
class BundledSqliteFeatureTest {

    private lateinit var conn: SQLiteConnection

    @Before
    fun setUp() {
        conn = BundledSQLiteDriver().open(":memory:")
    }

    @After
    fun tearDown() {
        conn.close()
    }

    @Test fun alterTableDropColumn_isSupported() { /* CREATE → ALTER DROP COLUMN → pragma_table_info */ }

    @Test fun jsonObject_isSupported() { /* SELECT json_object('k','v') == '{"k":"v"}' */ }

    @Test fun jsonInsertAppend_isSupported() { /* SELECT json_insert(json_array(), '$[#]', ...) */ }

    @Test fun jsonEach_returns3Rows() { /* SELECT count(*) FROM json_each(json_array(1,2,3)) == 3 */ }

    @Test fun jsonRemove_isSupported() { /* SELECT json_remove(json_array(1,2,3), '$[1]') == '[1,3]' */ }

    @Test fun sqliteVersion_isAtLeast3_45() { /* SELECT sqlite_version() ≥ "3.45"; log в logcat (F009) */ }
}
```

**Конкретные SQL-проверки** — см. таблицу в `02_scope.md` § «Test harness `BundledSqliteFeatureTest`» (`02_scope.md` строки 71-78).

**Импорты:**

- `androidx.sqlite.SQLiteConnection`
- `androidx.sqlite.driver.bundled.BundledSQLiteDriver`
- `androidx.sqlite.execSQL` (extension для side-effect SQL)
- `androidx.test.ext.junit.runners.AndroidJUnit4`
- `org.junit.{Test, Before, After, runner.RunWith}`

**Расположение:** `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/BundledSqliteFeatureTest.kt` — корень `room/` рядом с `AllMigrationTest.kt` (`infra_walkthrough.md` факт 7.3).

**MinSdk 23 verify:** прогон на эмуляторе API 23 + последний API (`02_scope.md` строки 80-82). Если упало именно на API 23 — поднять `sqliteBundledVersion` в узле 0.

### Узел 5 — `app/proguard-rules.pro` [~]

**Было** (фрагмент, lines 6-21 — Room/Project блок):

```proguard
# Room — keep all entities, DAOs, and database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
# ...
# Keep all DB entities in core-db-impl
-keep class me.apomazkin.core_db_impl.entity.** { *; }
-keep class me.apomazkin.core_db_impl.room.** { *; }
```

**Стало** (добавление нового блока после Room-блока, стиль повторяет существующие keep-блоки):

```proguard
# Room — keep all entities, DAOs, and database classes
# ... existing rules unchanged ...

# Bundled SQLite driver — keep native methods and driver classes
-keep class androidx.sqlite.driver.bundled.** { *; }
-keep class androidx.sqlite.** { native <methods>; }

# Keep all DB entities in core-db-impl
# ... existing rules unchanged ...
```

**Ключевые изменения:**

- Добавляются ровно две новые директивы (`02_scope.md` строка 36).
- `-keep class androidx.sqlite.driver.bundled.** { *; }` — keep всех классов bundled driver'а (включая `BundledSQLiteDriver` который ссылается на JNI-точки).
- `-keep class androidx.sqlite.** { native <methods>; }` — keep всех native-методов в `androidx.sqlite.*`. Без этого R8 в release strip'нет нативные методы → `UnsatisfiedLinkError` на старте (`02_scope.md` строка 20, release-only-bug).
- `isMinifyEnabled = true` в release block (`app/build.gradle.kts:97`) подтверждён — keep-rules будут активны (`infra_walkthrough.md` факт 2.3).
- Существующие keep-rules (Room, Dagger, DataStore, Firebase, Compose, Coroutines) — **НЕ трогаются**.

**Зависимость от узла 2:** keep-rules защищают bundled driver, который вводится в production-сборку именно в узле 2 (`RoomModule.setDriver(BundledSQLiteDriver())`). До узла 2 keep-rules не имеют смысла — нечего keep'ить в classpath, доступном release-сборке. После узла 2 — обязательны.

## Не трогаем (в скоупе prereq)

- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt` — F002 (`02_scope.md` строка 34): нет существующего `Callback` / `onCreate(connection)` override, версия БД = 11 остаётся неизменной.
- 10 файлов `Migration_NNN_to_MMM.kt` — compat layer Room 2.8 покрывает legacy API, верификация — acceptance 6.1.
- `schemas/me.apomazkin.core_db_impl.room.Database/*.json` — нет change БД-версии.
- `Schemable` / `DataProvider` / 10 `MigrationFromNNtoMM` androidTest-классов — F001: правка `BaseMigration` (узел 3) — единственная точка изменения, чтобы переключить все 10 тестов на bundled driver.
- `core/core-db-api/build.gradle.kts` — out of scope (IS481 main, не prereq).
- `app/src/main/AndroidManifest.xml` — bundled driver — classpath/runtime-зависимость, не component-уровень (`infra_walkthrough.md` факт 3).
- `app/src/main/java/.../App.kt` — F009 (runtime sqlite_version verify) реализуется через `sqliteVersion_isAtLeast3_45` тест в узле 4 + опционально через manual smoke check (`02_scope.md` строка 111); dev-only init-блок в `App.onCreate` не вводится в prereq.

> 📎 guide: docs/guides/data-layer.md — "Room Database провайдер живёт в RoomModule с @Singleton @Provides"
>
> 📎 guide: docs/guides/dagger-di.md — "RoomComponent использует RoomModule + ApiModule; провайдер @Singleton возвращает Database"
>
> 📎 guide: docs/guides/testing-migrations.md — "BaseMigration с @get:Rule MigrationTestHelper — единственная точка hookup'а для всех 10 тест-классов миграций"

## log_messages

- Прочитан `02_scope.md` + `infra_walkthrough.md` + реальный код 6 целевых файлов; greenfield-инфра bundled driver подтверждена (0 совпадений `BundledSQLite|setDriver` во всём проекте).
- Граф из 6 узлов, линеаризуется как `0 → 1 → {2 ∥ 3 ∥ 4} → 5`; узлы 2/3/4 параллелятся после установки зависимости в build.gradle, узел 5 (proguard) логически следует за узлом 2.
- `Database.kt` НЕ включён в граф (F002): нет существующего Callback'а — нечего адаптировать под `SQLiteConnection`-API, пункт брифа 3 неприменим к текущему коду.

_model: claude-opus-4-7[1m]_
