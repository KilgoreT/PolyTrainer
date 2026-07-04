# Implementation: IS481 vPrepared (Bundled SQLite driver prereq)

Реализация шага `infra_implement` flow `adaptive`. Source-of-truth: `02_scope.md` + `infra_design_tree.md` + `infra_test.md` + `infra_test_review.md` (tech debt F003/F005/F007).

## Применённые правки

| Узел | Файл | Действие | Результат |
|---|---|---|---|
| 0 | `deps/datastore.versions.toml` | [~] | Добавлены `sqliteBundledVersion = "2.6.2"` (последний stable, обеспечивает SQLite ≥ 3.45+) и алиас `sqliteBundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "sqliteBundledVersion" }`. |
| 1 | `core/core-db-impl/build.gradle.kts` | [~] | Добавлены `implementation(datastoreLibs.sqliteBundled)` (production) и `androidTestImplementation(datastoreLibs.sqliteBundled)` (androidTest source set). |
| 2 | `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt` | [~] | Переписан `provideDatabase` с legacy `Room.databaseBuilder(context, Database::class.java, "name")` на KMP `Room.databaseBuilder<Database>(context, name)` + `.setDriver(BundledSQLiteDriver())` + `.setQueryCoroutineContext(Dispatchers.IO)`. 10 миграций сохранены без изменений. Добавлен `logBundledSqliteVersion(database)` — F005/F009 closure (runtime sqlite_version log в production app process). |
| 3 | `core/core-db-impl/src/androidTest/.../base/BaseMigration.kt` | **НЕ изменён** | См. секцию «Нетривиальные решения» ниже (D1). Legacy ctor `MigrationTestHelper(instrumentation, Database::class.java)` сохранён, bundled driver под migration-тестами **не подключён** — acceptance 6.1 не покрыт в скоупе prereq, эскалирован в IS481 main. |
| 4 | `core/core-db-impl/src/androidTest/.../room/BundledSqliteFeatureTest.kt` | [+] | Создан новый файл с 6 test cases по test plan (`infra_test.md`): `alterTableDropColumn_isSupported`, `jsonObject_isSupported`, `jsonInsertAppend_isSupported` (F003 strict), `jsonEach_returns3Rows`, `jsonRemove_isSupported` (F003 strict), `sqliteVersion_isAtLeast3_45` (F009 log). Harness — `BundledSQLiteDriver().open(":memory:")`. |
| 5 | `app/proguard-rules.pro` | [~] | Добавлен блок `# Bundled SQLite driver — keep native methods and driver classes`: `-keep class androidx.sqlite.driver.bundled.** { *; }` + `-keep class androidx.sqlite.** { native <methods>; }`. Существующие keep-rules не тронуты. |

## Нетривиальные решения

### D1 — BaseMigration НЕ изменяется в prereq (acceptance 6.1 эскалирован)

**Контекст:** `infra_design_tree.md` (узел 3) и `02_scope.md` предполагали, что compat layer Room 2.8 оборачивает bundled connection в `SupportSQLiteDatabase` для legacy `migrate(db: SupportSQLiteDatabase)` API, и что в `BaseMigration.kt` можно просто добавить `driver = BundledSQLiteDriver()` к существующему ctor без изменения тела `runMigrateDbTest`.

**Реальный API Room 2.8.4** (проверено по sources `room-testing-android-2.8.4-sources.jar`, `androidx/room/testing/MigrationTestHelper.android.kt`):

`MigrationTestHelper` имеет **три** mutually-exclusive констуктора:

1. `MigrationTestHelper(instrumentation, assetsFolder, openFactory)` — deprecated, без `databaseClass`.
2. `MigrationTestHelper(instrumentation, databaseClass)` / `(instrumentation, databaseClass, specs, openFactory)` — **legacy support API**. Возвращает `SupportSQLiteDatabase`. Принимает `SupportSQLiteOpenHelper.Factory`, **не `SQLiteDriver`**. BundledSQLiteDriver сюда подложить **невозможно** — driver и openFactory — разные интерфейсы.
3. `MigrationTestHelper(instrumentation, file, driver, databaseClass, databaseFactory, autoMigrationSpecs)` — **новый KMP API**. Возвращает `SQLiteConnection`, а не `SupportSQLiteDatabase`. Требует явный `File` для БД (in-memory не поддерживается — driver «opens connections to a file database»).

Эти два API контракта **несовместимы**: `createDatabase`/`runMigrationsAndValidate` возвращают разные типы; `check(delegate is SupportSQLiteMigrationTestHelper)` / `check(delegate is SQLiteDriverMigrationTestHelper)` бросают исключение при попытке использовать «не свой» метод.

**Что значит для prereq:**

- Опция A — оставить legacy ctor → bundled driver под `MigrationTestHelper` **не подключается** → acceptance 6.1 формально невыполним (existing 10 миграций гоняются через системный driver, не bundled).
- Опция B — переключиться на driver ctor → надо переписать тело `runMigrateDbTest` под `SQLiteConnection` API, **переписать все 10 классов `MigrationFromNNtoMM`** (их callback'и принимают `SupportSQLiteDatabase`), переписать `Schemable` / `DataProvider` / `utils.toDatabase` / `utils.hasColumns` / `utils.checkData` / `Schema.kt` interfaces (все они работают через `SupportSQLiteDatabase`-фасад). Это уже не «infra prereq», а отдельная фича рефакторинга test harness.

**Решение:** Опция A (оставить legacy). Обоснование:
- Дизайн-тури (узел 3) явно запрещал трогать 10 файлов `MigrationFromNNtoMM.kt` и `Schemable`/`DataProvider` — «не трогаем» секция, `infra_design_tree.md` строки 300-308.
- Production-цель prereq — bundled driver в `RoomModule.kt` для будущей IS481 main M11→M12 — **выполняется** (узел 2 применён полностью).
- Smoke-доказательство того что bundled driver предоставляет нужные фичи (`ALTER TABLE DROP COLUMN`, JSON1, sqlite_version ≥ 3.45) даёт **новый** `BundledSqliteFeatureTest` (узел 4) — он использует `BundledSQLiteDriver().open(":memory:")` напрямую, не через Room, поэтому independent от migration harness и acceptance 6.2 покрывается **в скоупе prereq**.
- Verify «production app не падает с UnsatisfiedLinkError при старте» (acceptance 6.3) выполняется manual smoke check'ом, который НЕ требует правки `BaseMigration` — он гоняет production stack.
- Acceptance 6.1 (regression 10 миграций под bundled) — **эскалирован**: либо отдельная фича рефакторинга migration harness (переход всех 10 на `SQLiteConnection`-API), либо включена в IS481 main, где новая M11→M12 миграция всё равно будет писаться на `SQLiteConnection`-API (тогда переход harness'а на driver ctor становится естественным).

**Что это меняет для брифа prereq:**

- Acceptance 6.2 (feature smoke) — **покрыт** (`BundledSqliteFeatureTest`).
- Acceptance 6.3 (app start без crash, debug + release × API 23/34) — **manual** по чек-листу из `infra_test.md` (б), не automated.
- Acceptance 6.1 (regression 10 миграций под bundled) — **НЕ покрыт в prereq**, документирован как known gap, добавлен в backlog к основной IS481 main.

**Risk-assessment:**

- Production driver всё равно меняется на bundled (узел 2). Если bundled compat-layer молча падает на одной из 10 миграций — это проявится при **реальном** запуске приложения (acceptance 6.3 manual smoke), не только в тестах. Это менее строгая проверка чем automated regression, но не пустая.
- Schema БД не меняется (version = 11), 10 миграций используют только operations доступные в любом SQLite (`CREATE TABLE`, `INSERT`, `ALTER TABLE ... ADD COLUMN`, без `DROP COLUMN` / JSON1) → риск что bundled compat-layer падёт на одной из них — низкий.

### D2 — F005/F009 закрыты через корутинный launch в RoomModule

**Контекст:** F005 (review) требует runtime-лог `sqlite_version()` в production app process, не только в test process. Поскольку API Room 2.8 для query — suspending (`useReaderConnection` принимает `suspend (Transactor) -> R`), синхронный лог в `provideDatabase` невозможен.

**Решение:** Запустить fire-and-forget `CoroutineScope(Dispatchers.IO).launch` сразу после `.build()`. Корутина однократно выполнит `transactor.usePrepared("SELECT sqlite_version()") { stmt -> Log.i(...) }`. Орфанная scope — приемлема для one-shot dev-only лога; не utilites app-scope coroutine (нет dependency injection app-уровня в `RoomModule`). Альтернатива (dev-only init-блок в `App.onCreate`) — out of scope prereq, требует правок `app/` модуля и его DI.

### D3 — Pinned `sqliteBundledVersion = "2.6.2"`

**Контекст:** проверка Maven Central / Google Maven (`https://dl.google.com/dl/android/maven2/androidx/sqlite/group-index.xml`) показала, что последняя stable версия `androidx.sqlite:sqlite-bundled` — `2.6.2`. Версии `2.7.0-alpha*` доступны, но alpha — не подходит для production.

Версия `2.6.2` содержит SQLite ≥ 3.45 (упаковано вместе с релизом). `BundledSqliteFeatureTest.sqliteVersion_isAtLeast3_45` — runtime-доказательство соответствия требованию.

`sqlite-bundled` декларирует `minSdk = 21`, проектный `minSdk = 23` — совместимо.

## Tech debt применён

- **F003 (test 3/5 strict equality):** **да**. В `BundledSqliteFeatureTest`:
  - `jsonInsertAppend_isSupported` (test 3) — `assertEquals("""[{"x":1}]""", result.replace(" ", ""))`.
  - `jsonRemove_isSupported` (test 5) — `assertEquals("[1,3]", result.replace(" ", ""))` для консистентности.
- **F005 (F009 production app logging):** **да**. В `RoomModule.kt` добавлен `logBundledSqliteVersion(database)` — fire-and-forget корутина пишет `Log.i("RoomModule", "Bundled SQLite version (production): ...")` в logcat при создании Database. Verify в logcat — version должна совпасть с тем, что `BundledSqliteFeatureTest.sqliteVersion_isAtLeast3_45` пишет в test process (одинаковый bundled artifact в обоих).
- **F007 (regression baseline):** **н/а**. Решено НЕ менять `BaseMigration.kt` (D1) → существующий `AllMigrationTest` продолжает гоняться через legacy driver без правок → baseline регрессии не требуется. Если в будущем будет принято решение переводить migration harness на bundled driver (отдельная фича), baseline можно будет записать тогда.

## Тесты

`connectedDebugAndroidTest` **не запускался** — нет подключённого Android device/emulator (`adb devices` вернул пустой список). Согласно инструкции implement step: «Тесты запускай ... если есть Android device/emulator, иначе пропусти с note».

Verify steps на следующем шаге (`check`):
1. **Compile:** `./gradlew :core:core-db-impl:compileDebugAndroidTestKotlin` — проверить компиляцию нового `BundledSqliteFeatureTest.kt` + изменённого `RoomModule.kt` + build.gradle.kts.
2. **Lint:** `./gradlew :app:lintDebug` + `:core:core-db-impl:lintDebug`.
3. **AndroidTest** (на следующем этапе, при наличии эмулятора API 23 + API 34):
   - `./gradlew :core:core-db-impl:connectedDebugAndroidTest --tests "me.apomazkin.core_db_impl.room.BundledSqliteFeatureTest"` — все 6 тестов должны быть зелёными на обоих API.
   - `./gradlew :core:core-db-impl:connectedDebugAndroidTest --tests "me.apomazkin.core_db_impl.room.AllMigrationTest"` — все 10 миграций (regression через legacy harness, не через bundled — D1).
4. **Manual smoke** (acceptance 6.3, 6-шаговый чек-лист из `infra_test.md` (в)) — на эмуляторе API 23 + API 34, debug + release APK.

## Verify acceptance criteria (status)

| AC | Покрытие | Status |
|---|---|---|
| 6.1 | Regression 10 миграций под bundled driver | **НЕ покрыт в prereq** (D1) — эскалирован в IS481 main / отдельную фичу рефакторинга harness. Legacy `AllMigrationTest` продолжает гоняться, но через системный driver. |
| 6.2 | SQLite features verify (DROP COLUMN, JSON1, version ≥ 3.45) | покрыт — `BundledSqliteFeatureTest` (узел 4), 6 тестов готовы к запуску на эмуляторе. |
| 6.3 | Application start без crash (debug + release × API 23/34) | покрыт manual чек-листом из `infra_test.md` (в) — выполняется в check-фазе. |

## Релевантные гайды

> 📎 guide: `docs/guides/data-layer.md` — "Room Database провайдер живёт в RoomModule с @Singleton @Provides". Сохранено: структура `@Module` / `@Singleton` / `@Provides` не менялась, только тело `provideDatabase`.
>
> 📎 guide: `docs/guides/dagger-di.md` — "RoomComponent использует RoomModule + ApiModule; провайдер @Singleton возвращает Database". Dagger-граф не затронут.

_model: claude-opus-4-7[1m]_

## log_messages

- Применены правки 5 из 6 узлов DAG: версия catalog + build.gradle + RoomModule (bundled driver + F005 runtime log) + новый BundledSqliteFeatureTest (с F003 strict assertions) + ProGuard keep-rules. Pin: `sqliteBundled = 2.6.2` (последний stable, SQLite ≥ 3.45).
- D1 (нетривиальное решение): `BaseMigration.kt` НЕ менялся. Реальный API Room 2.8 `MigrationTestHelper` — driver ctor возвращает `SQLiteConnection` (не `SupportSQLiteDatabase`); переключение требует переписки всех 10 `MigrationFromNNtoMM` + `Schemable`/`DataProvider`/utils — out of prereq scope. Acceptance 6.1 эскалирован в IS481 main; acceptance 6.2/6.3 покрыты.
- Тесты `connectedDebugAndroidTest` не запускались — нет device/emulator (`adb devices` пуст), отложено в check-фазу.
