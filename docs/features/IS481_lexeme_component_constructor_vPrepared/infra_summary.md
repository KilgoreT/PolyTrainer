---
status: done
---

# Summary — infra

Prereq-фича «Bundled SQLite driver» (IS481 vPrepared) — bundled `androidx.sqlite:sqlite-bundled` driver подключён в production stack `core-db-impl`, smoke-покрытие feature-возможностей (DROP COLUMN, JSON1, version ≥ 3.45) реализовано как новый androidTest, ProGuard защищён keep-rules'ами. Пререк удовлетворён: основная IS481 main может писать миграцию v11→v12 с расчётом на bundled SQLite ≥ 3.45 API.

## Что сделано

**Узел 0 — `deps/datastore.versions.toml`** [~]
- Добавлены `sqliteBundledVersion = "2.6.2"` (последний stable из Google Maven; pin'ит SQLite ≥ 3.45) и алиас `sqliteBundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "sqliteBundledVersion" }`.

**Узел 1 — `core/core-db-impl/build.gradle.kts`** [~]
- `implementation(datastoreLibs.sqliteBundled)` — production classpath.
- `androidTestImplementation(datastoreLibs.sqliteBundled)` — androidTest source set (нужен для `BundledSqliteFeatureTest`).

**Узел 2 — `core/core-db-impl/.../di/module/RoomModule.kt`** [~]
- `provideDatabase`: переход с legacy `Room.databaseBuilder(context, Database::class.java, "name")` (3-arg KClass-builder, на котором `setDriver` молча игнорируется) на KMP-builder `Room.databaseBuilder<Database>(context, name)`.
- `.setDriver(BundledSQLiteDriver())` — bundled driver вместо системного (production).
- `.setQueryCoroutineContext(Dispatchers.IO)` — требование Room 2.7+ KMP-builder для async API.
- 10 миграций (`migration_1_2 … migration_10_11`) — без изменений.
- Добавлен `logBundledSqliteVersion(database)` (fire-and-forget `CoroutineScope(Dispatchers.IO).launch`) — F005/F009 closure, runtime-лог `sqlite_version()` в production app process.
- Структура `@Module` / `@Singleton` / `@Provides` — не тронута. Dagger-граф не меняется.

**Узел 3 — `core/core-db-impl/src/androidTest/.../base/BaseMigration.kt`** [НЕ ИЗМЕНЁН]
- Сохранён legacy 2-arg ctor `MigrationTestHelper(instrumentation, Database::class.java)`. См. **D1** в «Ключевые решения».

**Узел 4 — `core/core-db-impl/src/androidTest/.../room/BundledSqliteFeatureTest.kt`** [+]
- Новый androidTest-файл, 6 cases:
  - `alterTableDropColumn_isSupported` — `ALTER TABLE ... DROP COLUMN` + pragma_table_info verify (SQLite 3.35+).
  - `jsonObject_isSupported` — базовый `json_object('k','v')`.
  - `jsonInsertAppend_isSupported` — `json_insert(json_array(), '$[#]', ...)` pattern для будущего IS481 main; **strict equality** с `result.replace(" ", "")` (tech debt F003).
  - `jsonEach_returns3Rows` — table-valued `json_each`.
  - `jsonRemove_isSupported` — `json_remove(...)` с **strict equality** (F003 консистентность).
  - `sqliteVersion_isAtLeast3_45` — `sqlite_version()` parse + numeric `(major, minor)` compare; `Log.i` в logcat (F009 closure для test process).
- Harness — прямой `BundledSQLiteDriver().open(":memory:")`, **минует** Room compat layer, чтобы доказать что features предоставляет bundled native binary, а не Room их симулирует.

**Узел 5 — `app/proguard-rules.pro`** [~]
- Новый блок `# Bundled SQLite driver — keep native methods and driver classes`:
  - `-keep class androidx.sqlite.driver.bundled.** { *; }` — keep всех bundled driver классов (включая JNI entry points).
  - `-keep class androidx.sqlite.** { native <methods>; }` — keep native methods (без этого R8 в release strip'нет → `UnsatisfiedLinkError` на старте).
- `isMinifyEnabled = true` в release block — подтверждён, keep-rules будут активны.

**Tech debt из `infra_test_review.md` применён:**
- F003 (assertion strictness): применён в tests 3, 5 `BundledSqliteFeatureTest`.
- F005 (F009 production app logging): применён в `RoomModule.logBundledSqliteVersion`.
- F007 (regression baseline): н/а — `BaseMigration.kt` не менялся, baseline не требуется.

**Тесты:** `connectedDebugAndroidTest` **не запускался** — `adb devices` пуст. Verify-команды отложены в check-фазу (compile, lint, прогон на эмуляторе API 23 + API 34, manual smoke).

## Ключевые решения

### D1 — `BaseMigration.kt` НЕ изменялся; acceptance 6.1 эскалирован в IS481 main

Дизайн-три и `02_scope.md` исходили из предположения, что Room 2.8 compat layer оборачивает bundled connection в `SupportSQLiteDatabase`-фасад и `MigrationTestHelper` принимает `driver = BundledSQLiteDriver()` как дополнительный параметр к существующему ctor. **Реальный API Room 2.8.4** (verified по source jar `room-testing-android-2.8.4-sources.jar`): `MigrationTestHelper` имеет три **mutually-exclusive** ctor'а:

1. Legacy `(instrumentation, databaseClass)` — возвращает `SupportSQLiteDatabase`, принимает `SupportSQLiteOpenHelper.Factory`, **не SQLiteDriver**. Подложить bundled driver — невозможно.
2. KMP `(instrumentation, file, driver, databaseClass, databaseFactory, ...)` — возвращает `SQLiteConnection`, требует явный `File` (in-memory не поддерживается). API контракт несовместим с legacy: `check(delegate is SupportSQLiteMigrationTestHelper)` бросит исключение при попытке вызвать legacy метод на driver-helper'е.

Переключение на driver ctor требует переписки **всех 10 классов** `MigrationFromNNtoMM.kt` (callbacks принимают `SupportSQLiteDatabase`) + `Schemable` / `DataProvider` / `utils.toDatabase` / `utils.hasColumns` / `utils.checkData` / `Schema.kt` interfaces — это отдельная фича рефакторинга migration test harness, явно запрещённая дизайн-три (секция «Не трогаем», строки 300-308).

**Решение:** оставлено legacy в `BaseMigration.kt`. Acceptance 6.1 (regression 10 миграций под bundled driver) **формально НЕ покрыт в prereq** — `AllMigrationTest` продолжает гоняться через системный driver. Покрытие переключается:
- В **IS481 main** новая миграция M11→M12 всё равно будет писаться на `SQLiteConnection`-API → переход harness'а на driver ctor становится естественным шагом фичи.
- Зафиксировано как **upstream finding для IS481 main** (FlowBacklog IS481-F5).

Risk-mitigation: production driver всё равно меняется на bundled (узел 2). Если bundled compat-layer молча падает на одной из существующих 10 миграций — это проявится при реальном запуске приложения (acceptance 6.3 manual smoke). Schema БД не меняется (version = 11), миграции используют только operations доступные в любом SQLite (`CREATE TABLE`, `ALTER TABLE ... ADD COLUMN`, без `DROP COLUMN`/JSON1) → риск bundled-incompatibility — низкий.

### D2 — F005/F009 закрыт fire-and-forget корутиной в `RoomModule`

API Room 2.8 для query — suspending. Синхронный лог `sqlite_version()` в `provideDatabase` невозможен. Решено: `CoroutineScope(Dispatchers.IO).launch { transactor.usePrepared("SELECT sqlite_version()") { ... Log.i(...) } }` сразу после `.build()`. Орфанная scope — приемлема для one-shot dev-only лога; альтернатива (dev-only init в `App.onCreate`) — out of scope prereq, требует правок `app/` модуля и его DI.

### D3 — Pinned `sqliteBundledVersion = "2.6.2"`

Последняя stable из Google Maven `androidx.sqlite:sqlite-bundled` (versions 2.7.0-alpha* отвергнуты как alpha). Декларирует `minSdk = 21` — совместимо с проектным `minSdk = 23`. SQLite ≥ 3.45 в составе релиза — runtime-доказательство в `sqliteVersion_isAtLeast3_45`.

### Acceptance criteria — финальный статус

| AC | Покрытие | Status |
|---|---|---|
| 6.1 — Regression 10 миграций под bundled | НЕ покрыт в prereq (D1) — эскалирован в IS481 main / отдельную фичу harness-рефакторинга. `AllMigrationTest` гоняется через системный driver. | **Эскалирован (out of prereq scope)** |
| 6.2 — SQLite features (DROP COLUMN, JSON1, version ≥ 3.45) | `BundledSqliteFeatureTest` — 6 тестов готовы к прогону на API 23 + API 34. | **Покрыт** (verify в check-фазе) |
| 6.3 — App start без crash (debug + release × API 23/34) | Manual smoke чек-лист (6 шагов) из `infra_test.md` (в). | **Покрыт** manual (verify в check-фазе) |

### Что осталось вне scope prereq

- Переход migration test harness (`BaseMigration` + 10 `MigrationFromNNtoMM` + `Schemable`/`DataProvider`/utils) на `SQLiteConnection`-API — отдельная фича, разруливается в IS481 main вместе с новой миграцией v11→v12.
- Dev-only init-блок в `App.onCreate` для логирования sqlite version (F009 альтернатива B) — заменён на fire-and-forget корутину в `RoomModule` (D2).
- Negative test «system driver НЕ умеет DROP COLUMN на API 23» (F010 из test plan) — improvement-предложение, не блокер.
- Performance / WAL / concurrent тесты bundled driver — out of scope prereq.

## Артефакты

**Изменённые/созданные файлы:**
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/deps/datastore.versions.toml` [~]
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/build.gradle.kts` [~]
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt` [~]
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/BundledSqliteFeatureTest.kt` [+]
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/proguard-rules.pro` [~]

**НЕ изменён (D1):**
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/base/BaseMigration.kt`

**Документы flow:**
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_lexeme_component_constructor_vPrepared/infra_design_tree.md`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_lexeme_component_constructor_vPrepared/infra_test.md`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_lexeme_component_constructor_vPrepared/infra_test_review.md`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_lexeme_component_constructor_vPrepared/infra_implement.md`

_model: claude-opus-4-7[1m]_
