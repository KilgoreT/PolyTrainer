# Infra walkthrough — IS481 Prepared (Bundled SQLite driver prereq)

Цель документа: собрать **факты** о реальной инфраструктуре (build / DI / migrations / ProGuard / CI), релевантные подключению `BundledSQLiteDriver` без модификации существующих миграций. Все утверждения — с `file:line` ссылками. Никаких дизайн-решений в этом документе.

## 1. DI-граф (Room provider)

### Факт 1.1 — `RoomModule` использует legacy ctor `Room.databaseBuilder(context, KClass, name)`

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt:37`:

```kotlin
return Room.databaseBuilder(context, Database::class.java, "name")
    .addMigrations(
        migration_1_2, migration_2_3, migration_3_4, migration_4_5,
        migration_5_6, migration_6_7, migration_7_8, migration_8_9,
        migration_9_10, migration_10_11,
    )
    .build()
```

- Это **legacy 3-arg builder** (KClass-form Room 2.0+).
- Включены ровно **10 миграций** (`RoomModule.kt:39-48`).
- Никаких `.setDriver(...)` / `.setQueryCoroutineContext(...)` нет.
- Имя БД захардкожено `"name"` (с TODO в `RoomModule.kt:33`, не относится к prereq).
- Класс `@Module` (`RoomModule.kt:30`), `@Singleton` + `@Provides` для `Database` (`RoomModule.kt:34-36`), отдельный `@Provides` для `WordDao` (`RoomModule.kt:53-56`).

### Факт 1.2 — RoomDatabase Callback / onCreate / onOpen в проекте отсутствует

`grep -rn "BundledSQLite\|SQLiteDriver\|sqlite-bundled\|setDriver"` по `core/`, `deps/`, `app/` — **0 совпадений** (exit code 1, `NO_MATCHES`).

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt:14-28` — голый `RoomDatabase` без `Callback`, без override `onCreate(...)` / `onOpen(...)`:

```kotlin
@Database(
    entities = [WordDb::class, LexemeDb::class, HintDb::class,
                SampleDb::class, WriteQuizDb::class, DictionaryDb::class],
    version = 11
)
@TypeConverters(DateTimeConverter::class)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
```

**Вывод по факту:** в текущем коде Callback'а нет → нечего адаптировать под `SQLiteConnection`-API. Пункт брифа 3 (override `onCreate(connection: SQLiteConnection)`) формально неприменим к текущему состоянию кода — это зафиксировано в `02_scope.md` строка 34 (decision F002).

### Факт 1.3 — Database version = 11

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt:23` — `version = 11`. Не инкрементируется в prereq.

### Факт 1.4 — App boot path и DI integration

`app/src/main/java/me/apomazkin/polytrainer/App.kt:17-32` — `Application.onCreate()`:
- Создаёт `DaggerAppComponent` (Dagger DI).
- Передаёт `CoreDbComponent.init(this, logger)` (`App.kt:28`) — это точка инициализации core-db.
- **Нет** dev-only init-блока с логированием БД (релевантно F009 — runtime sqlite_version verify).
- **Нет** аналога bundled driver setup в Application.

## 2. Build-конфигурация

### Факт 2.1 — Version catalog для datastore

`deps/datastore.versions.toml`:
- `roomVersion = "2.8.4"` (`datastore.versions.toml:3`) — Room 2.8.4 уже подключён. Это **поддерживает** KMP-builder + `setDriver()` + `setQueryCoroutineContext()` (введены в Room 2.7+).
- **Нет** `sqliteBundledVersion` (отсутствует в `[versions]` секции).
- **Нет** алиаса `sqliteBundled` в `[libraries]`.
- Версионники для других datastore-зависимостей: `datastore-version = "1.1.7"`, `documentfileVersion = "1.1.0"`, `pagingVersion = "3.3.6"`.

**Вывод:** новая запись `sqliteBundledVersion` + алиас `sqliteBundled` добавляются с нуля.

### Факт 2.2 — `core-db-impl` build.gradle: текущие зависимости

`core/core-db-impl/build.gradle.kts`:
- `minSdk = 23` (`build.gradle.kts:19`) — совместимо с `androidx.sqlite:sqlite-bundled` (декларирует `minSdk = 21`).
- `targetSdk = 35`, `compileSdk = 35` (`build.gradle.kts:16,20`).
- Room dependencies (`build.gradle.kts:44-48`):
  - `implementation(datastoreLibs.roomRuntime)`
  - `implementation(datastoreLibs.roomKtx)`
  - `ksp(datastoreLibs.roomCompiler)`
  - `implementation(datastoreLibs.roomPaging)`
- AndroidTest (`build.gradle.kts:54-55`):
  - `androidTestImplementation(project("path" to ":modules:core:ui"))`
  - `androidTestImplementation(datastoreLibs.roomTesting)`
- **Нет** `implementation(datastoreLibs.sqliteBundled)` (поскольку алиаса не существует).
- **Нет** `androidTestImplementation(datastoreLibs.sqliteBundled)`.

### Факт 2.3 — `app/build.gradle.kts`: ProGuard activation

`app/build.gradle.kts:95-107` — release block:
- `isMinifyEnabled = true` (`build.gradle.kts:97`) — **ProGuard/R8 активны в release**.
- `isShrinkResources = true` (`build.gradle.kts:98`).
- ProGuard files: `getDefaultProguardFile("proguard-android-optimize.txt")` + `proguard-rules.pro` (`build.gradle.kts:100-101`).
- Debug блок `build.gradle.kts:84-94` — `isMinifyEnabled = false` (явно).

**Вывод:** keep-rules в `proguard-rules.pro` будут активны в release. Это подтверждает риск release-only-bug из `02_scope.md` строка 21.

### Факт 2.4 — Root build.gradle.kts

`build.gradle.kts:1-6` — только plugins, никаких dependency-блоков. Релевантно: ничего не блокирует добавление новой зависимости в `datastore.versions.toml`.

## 3. AndroidManifest

`app/src/main/AndroidManifest.xml:1-33` — стандартный манифест:
- Один `<application>` с `android:name=".App"`.
- Один `<activity>` MainActivity (LAUNCHER).
- `meta-data firebase_crashlytics_collection_enabled=false`.

**Вывод:** манифест **не релевантен** изменениям bundled driver. Никаких изменений в манифесте не требуется. Подтверждено: bundled driver — это classpath/runtime-зависимость, не component-уровень.

## 4. ProGuard / R8

`app/proguard-rules.pro:1-60` — текущие правила:

Что уже есть:
- Room: `-keep class * extends androidx.room.RoomDatabase { *; }` (`proguard-rules.pro:7`), `@Entity` (`:8`), `@Dao` (`:9`), `RoomDatabase$Callback` (`:10`), `**_Impl` (`:11`), `@Embedded`/`@Relation` (`:14-17`).
- Project entities: `me.apomazkin.core_db_impl.entity.**` (`:20`), `me.apomazkin.core_db_impl.room.**` (`:21`), `me.apomazkin.core_db_api.entity.**` (`:24`).
- Dagger: `**_Factory`, `**_MembersInjector`, `dagger.**` (`:27-29`), `@Inject` (`:30-33`).
- DataStore: `androidx.datastore.**` (`:36`).
- Firebase Sessions kotlinx.serialization (`:39-46`).
- country_data (`:49`).
- Coroutines (`:52-53`).
- Compose (`:56-59`).

Чего **нет**:
- **Нет** `-keep class androidx.sqlite.driver.bundled.** { *; }`.
- **Нет** `-keep class androidx.sqlite.** { native <methods>; }`.

**Вывод:** keep-rules для bundled SQLite в текущем файле отсутствуют. Их нужно добавить (`02_scope.md` строка 36). Стиль добавления — точно такой же, как блоки Room (`:7-17`) или Compose (`:56-59`).

## 5. CI / CD

`.github/workflows/` — найдено:
- `on_feature_push.yml` — Lint → Unit Tests (`testDebugUnitTest`) → Build APK (`assembleDebug`). Триггер: `IS**`, `MT**`. **Не** запускает androidTests/instrumented tests.
- `on_pull_request_sample.yml`, `on_push_sample.yml`, `on_prerelease_sample.yml` — sample-варианты.
- `on_prerelease.yml` — Lint → Unit Tests (`testDebugUnitTest`) → Build (`bundleRelease` + `assembleRelease`) → Publish. **Не** запускает androidTests/instrumented tests.

`.github/workflows/on_feature_push.yml:32-54` — `test_job` запускает только `./gradlew testDebugUnitTest` (unit tests на JVM). **Никакого instrumented testing в CI нет.**

**Вывод:** CI **не покрывает** `AllMigrationTest` (это `androidTest`, не `test`). Поэтому acceptance criterion 6.1 (regression-прогон существующих миграций под bundled driver) выполняется **локально** через `connectedAndroidTest` либо на эмуляторе вручную. CI не блокирует, но и не гарантирует. Это **факт** (не дизайн-решение): инфраструктура CI на момент анализа не включает instrumented tests.

## 6. Существующие миграции (legacy API)

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/` — **10 файлов**:

- `Migration_001_to_002.kt`, `Migration_002_to_003.kt`, ..., `Migration_010_to_011.kt`.

Все используют **legacy API** `migrate(db: SupportSQLiteDatabase)`. Подтверждение:

`Migration_001_to_002.kt:1-23`:
```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(...)
    }
}
```

`Migration_010_to_011.kt:1-87` — самый сложный из 10, тоже на `SupportSQLiteDatabase` API (`migrate(db: SupportSQLiteDatabase)`, line 8). Содержит recreate-table pattern (CREATE NEW → INSERT SELECT → DROP OLD → ALTER RENAME) для смены имён колонок.

**Вывод:** все 10 миграций — legacy API. Compat layer Room 2.8.4 должен поддерживать их под bundled driver (это утверждение **из брифа**, не из кода — проверяется acceptance 6.1).

## 7. Существующие тесты миграций (androidTest)

### Факт 7.1 — `BaseMigration` использует legacy `MigrationTestHelper` ctor

`core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/base/BaseMigration.kt:18-22`:

```kotlin
@get:Rule
val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    Database::class.java
)
```

- Ctor — **2-arg** (instrumentation + KClass), **без** driver-параметра.
- Импорты (`BaseMigration.kt:1-13`) — `androidx.room.testing.MigrationTestHelper`, `androidx.sqlite.db.SupportSQLiteDatabase`, нет `BundledSQLiteDriver`.
- API хелпера `BaseMigration.kt:47-68`:
  - `createDatabase(databaseName, currentVersion)` → возвращает `SupportSQLiteDatabase`.
  - `runMigrationsAndValidate(databaseName, version, validateDroppedTables, migration)` → возвращает `SupportSQLiteDatabase`.
- Callback'и `onCreate`/`afterCreateCheck`/`afterMigrationCheck` (`BaseMigration.kt:52-54`) принимают `SupportSQLiteDatabase`.

**Вывод:** под bundled driver требуется **перегрузка ctor** на Room 2.7+ KMP-форму с явным driver-параметром. Без этого — driver не подставится в helper, и acceptance 6.1 формально не покроет regression под bundled (F001).

### Факт 7.2 — `AllMigrationTest` — Suite из 10 тест-классов

`core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/AllMigrationTest.kt:1-29`:

- `@RunWith(Suite::class)` + `@Suite.SuiteClasses(MigrationFrom01to02::class, ..., MigrationFrom10to11::class)`.
- Каждый класс наследуется от `BaseMigration` (см. `MigrationFrom10to11.kt:45`).
- Все используют `SupportSQLiteDatabase` API в callbacks (например `MigrationFrom10to11.kt:54,82,125`, `database.execSQL(...)`, `database.query(...)`).

**Вывод:** правка `BaseMigration` ctor — единственная точка изменения, чтобы переключить все 10 тест-классов на bundled driver. Сами тест-классы не трогаются (их callbacks работают через `SupportSQLiteDatabase`, который Room 2.8 compat layer оборачивает).

### Факт 7.3 — Test harness каталог

`core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/`:
- `AllMigrationTest.kt`, `ExampleInstrumentedTest.kt`, `Schema.kt` — корневые.
- `base/` — `BaseMigration.kt`, `Schemable.kt`.
- `migrations/` — 10 MigrationFromNNtoMM.kt.
- `dataSource/` — provider'ы тестовых данных.
- `schemable/` — версионированные схемы таблиц.
- `utils/` — хелперы (`checkData`, `hasColumn`, `hasTable`, `toDatabase`).

**Вывод:** новый тест `BundledSqliteFeatureTest` логично располагается в корне `core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/` (рядом с `AllMigrationTest`, как заявлено в `02_scope.md` строка 37). Он не зависит от существующего harness — открывает `BundledSQLiteDriver().open(":memory:")` напрямую.

### Факт 7.4 — Зависимость `roomTesting` подключена

`core/core-db-impl/build.gradle.kts:55` — `androidTestImplementation(datastoreLibs.roomTesting)` уже есть. То есть `MigrationTestHelper` доступен в androidTest. Под bundled driver новый ctor `MigrationTestHelper(instrumentation, databaseClass, driver)` придёт **из этой же** зависимости (Room 2.8.4 содержит KMP-форму).

## 8. Аналог bundled SQLite driver setup в проекте

**Поиск:** `grep -rn "BundledSQLite\|SQLiteDriver\|sqlite-bundled\|setDriver" core/ deps/ app/` — **0 совпадений** во всём проекте.

**Вердикт по поисковой задаче:** **не найден.** Аналога bundled driver setup в проекте нет. Это новая инфраструктура.

## 9. Сводка по релевантным файлам

| Файл | file:line | Состояние |
|---|---|---|
| `deps/datastore.versions.toml` | `:1-15` | `roomVersion = "2.8.4"` есть (`:3`); `sqliteBundled*` отсутствует |
| `core/core-db-impl/build.gradle.kts` | `:38-60` | Room deps есть (`:44-48`); `roomTesting` есть (`:55`); `sqliteBundled` нет; `minSdk = 23` (`:19`) |
| `core/core-db-impl/.../di/module/RoomModule.kt` | `:30-58` | legacy 3-arg builder (`:37`); 10 миграций (`:39-48`); нет `setDriver` |
| `core/core-db-impl/.../room/Database.kt` | `:14-28` | `version = 11` (`:23`); нет Callback / нет override `onCreate`/`onOpen` |
| `core/core-db-impl/.../room/migrations/Migration_NNN_to_MMM.kt` | 10 файлов | все используют `migrate(db: SupportSQLiteDatabase)` (legacy API) |
| `core/core-db-impl/.../androidTest/.../base/BaseMigration.kt` | `:18-22` | legacy 2-arg `MigrationTestHelper(instrumentation, KClass)`; нет driver-параметра |
| `core/core-db-impl/.../androidTest/.../AllMigrationTest.kt` | `:16-28` | JUnit Suite из 10 MigrationFromNNtoMM |
| `app/build.gradle.kts` | `:95-107` | `isMinifyEnabled = true` (`:97`); ProGuard активен в release; `minSdk = 23` (`:40`) |
| `app/proguard-rules.pro` | `:1-60` | Room/Dagger/Compose keep есть; `androidx.sqlite.driver.bundled.**` нет; `native <methods>` нет |
| `app/src/main/AndroidManifest.xml` | `:1-33` | не релевантен bundled driver |
| `app/src/main/java/.../App.kt` | `:17-32` | стандартный Application.onCreate; нет dev-only init для logging sqlite_version |
| `.github/workflows/on_feature_push.yml` | `:32-54` | unit tests only (`testDebugUnitTest`); androidTest **не запускается в CI** |
| `.github/workflows/on_prerelease.yml` | `:42-68` | unit tests only; androidTest **не запускается в CI** |
| `build.gradle.kts` (root) | `:1-6` | только plugins; нет dependency-блоков |

## Вердикт

1. **DI / Room builder** — legacy `Room.databaseBuilder(context, KClass, name)` в `RoomModule.kt:37`. Bundled driver setup **не найден** (нигде в проекте).
2. **Database Callback / `onCreate(connection)` override** — **не найден**. Пункт брифа 3 неприменим к текущему коду; решение F002 в `02_scope.md` зафиксировано.
3. **Version catalog алиас для bundled** — **не найден** в `deps/datastore.versions.toml`. Room 2.8.4 уже подключён → KMP-builder доступен.
4. **`androidx.sqlite:sqlite-bundled` зависимость** — **не подключена** ни в `core-db-impl/build.gradle.kts` (`implementation`), ни в androidTest (`androidTestImplementation`).
5. **ProGuard keep-rules для bundled SQLite** — **не найдены** в `app/proguard-rules.pro`. `isMinifyEnabled = true` в release (`app/build.gradle.kts:97`) → keep-rules будут активны после добавления.
6. **`MigrationTestHelper` с driver-параметром** — **не используется**; `BaseMigration.kt:19-22` имеет legacy 2-arg ctor.
7. **10 существующих миграций** — все на legacy `migrate(db: SupportSQLiteDatabase)`. Совместимость с bundled driver через Room 2.8 compat layer — **проверяется** acceptance 6.1, не подтверждена кодом.
8. **AndroidManifest** — не релевантен bundled driver, изменений не требует.
9. **CI** — instrumented tests **не запускаются** ни в `on_feature_push.yml`, ни в `on_prerelease.yml`. Acceptance 6.1 / 6.2 покрываются только локально или вручную (на момент анализа).
10. **`minSdk = 23`** в `core-db-impl/build.gradle.kts:19` и `app/build.gradle.kts:40` — формально совместим с `androidx.sqlite:sqlite-bundled` (`minSdk = 21` декларация). Verify-точка — прогон `BundledSqliteFeatureTest` на API 23 эмуляторе.

**Аналог bundled driver setup — не найден.** Это greenfield-инфра в рамках проекта.

_model: claude-opus-4-7[1m]_
