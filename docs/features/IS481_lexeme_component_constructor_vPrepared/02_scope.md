# Scope analysis: IS481 Prepared (Bundled SQLite driver prereq)

## Резюме

Prereq-фича перед основной IS481: подключить `BundledSQLiteDriver` (Room 2.7+ KMP-builder + `androidx.sqlite:sqlite-bundled`) **без** изменения 10 существующих миграций. Скоуп строго инфра/data: Gradle-зависимость + pin версии в `deps/datastore.versions.toml`, переписка `RoomModule.kt` с legacy `Room.databaseBuilder(context, KClass, name)` на новый KMP-builder с `.setDriver(BundledSQLiteDriver())` + `.setQueryCoroutineContext(Dispatchers.IO)`, ProGuard keep-rules для нативных методов bundled SQLite, перегрузка `MigrationTestHelper` в `BaseMigration` под bundled driver. Verify: прогон существующего `AllMigrationTest` под bundled driver (regression на 10 миграций) + новый smoke androidTest `BundledSqliteFeatureTest` для `ALTER TABLE DROP COLUMN` / JSON1 / `sqlite_version() >= 3.45`. Без UI, без бизнес-логики, без новых таблиц/миграций — это pure platform/data-layer enabler для последующей IS481 main миграции v11→v12.

## Затронутые слои

- **Infrastructure** — да — изменения в `deps/datastore.versions.toml` (новый алиас + версия), `core/core-db-impl/build.gradle.kts` (новые `implementation` + `androidTestImplementation`), `app/proguard-rules.pro` (keep-rules для `androidx.sqlite.driver.bundled.**` и нативных методов). Это classic infra: build-сетап, DI-провайдер (`RoomModule`), ProGuard.
- **Business logic** — нет — нет изменений в State/Msg/Reducer/Effect/UseCase. Доменные сущности не затрагиваются. Никакой TEA-логики не появляется и не модифицируется.
- **UI** — нет — нет composables, layouts, ресурсов. Фича невидима для пользователя кроме потенциального +4-5 МБ к универсальному APK.
- **Data** — да — переписка `RoomModule.kt` (data DI-слой), перегрузка `MigrationTestHelper` в `BaseMigration.kt` (androidTest harness data-слоя). Изменяется driver, через который Room говорит с SQLite. **БЕЗ** изменения версии БД, без новой миграции, без правки 10 existing миграций (compat layer Room 2.8 покрывает legacy `migrate(db: SupportSQLiteDatabase)` API).

## Аспекты

- `new_dependency` — добавляется `androidx.sqlite:sqlite-bundled` с pinned версией ≥ обеспечивающей SQLite 3.45+. Pin обязателен — без него gradle resolution может выбрать downgrade'нутую транзитивную версию, и миграция (когда она появится в основной IS481) упадёт молча.
- `public_contract_change` — нет. `CoreDbApi` не трогается. Внешние модули не видят разницы.
- `db_migration` — нет (в скоупе prereq). Версия БД остаётся 11. **Schema не меняется.** Но фича создаёт инфраструктуру, на которой будет жить будущая M11→M12.
- `cross_tab_subscription` — нет.
- `production_crash` — потенциально (mitigation): без ProGuard keep-rules R8 в release-сборке может strip нативные методы bundled SQLite → `UnsatisfiedLinkError` на старте. Поэтому keep-rules — обязательная часть скоупа, не «на потом».
- `release_only_bug` — связан с предыдущим. Без proguard-rules баг проявится только в release, debug пройдёт. Verify: убедиться что `app/build.gradle.kts` release block имеет `isMinifyEnabled = true` (на момент анализа — да, line 97), иначе keep-rules формально не активны.
- `apk_size_impact` — +~1 МБ на ABI × 4 ABI ≈ +4-5 МБ универсальный APK. Через AAB Play split per-abi → +~1 МБ на пользователя.
- `verify_feature_availability` — главный тех-аспект. `BundledSQLiteDriver` молча игнорируется на legacy builder. Нужен runtime-verify через `SELECT sqlite_version()` и feature-смок `ALTER TABLE DROP COLUMN` / `json_insert($, '$[#]', ...)` — без этого нельзя гарантировать что bundled реально включился.
- `regression_risk_existing_migrations` — главный риск фичи: 10 существующих миграций используют legacy `migrate(db: SupportSQLiteDatabase)` API. Room 2.8 compat layer должен их подхватить под bundled driver, но это **необходимо подтвердить прогоном** `AllMigrationTest` — это и есть acceptance criterion 6.1. Прогон требует предварительной перегрузки `MigrationTestHelper` в `BaseMigration.kt` (legacy ctor без driver-параметра не подставит bundled driver — без правки helper'а acceptance 6.1 формально невыполним).
- `test_harness_change` — `BaseMigration.kt` использует legacy ctor `MigrationTestHelper(instrumentation, Database::class.java)`. Под Room 2.7+ KMP с bundled driver требуется новый ctor с явным driver-параметром.

## Затронутые файлы

| Файл | Изменение | Обоснование |
|---|---|---|
| `deps/datastore.versions.toml` | новая version `sqliteBundledVersion`, новый алиас `sqliteBundled` | пункт скоупа 1; здесь живут все Room/datastore версии (`roomVersion = "2.8.4"` уже там) |
| `core/core-db-impl/build.gradle.kts` | (1) `implementation(datastoreLibs.sqliteBundled)`; (2) `androidTestImplementation(datastoreLibs.sqliteBundled)` | пункт 1; bundled SQLite нужен в production (RoomModule) и в androidTest (новый `BundledSqliteFeatureTest` + перегрузка `MigrationTestHelper` в `BaseMigration` под bundled driver — F006). Без `androidTestImplementation` androidTest source set не скомпилируется |
| `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt:37` | переписка с legacy `Room.databaseBuilder(context, Database::class.java, "name")` на KMP-builder с `.setDriver(BundledSQLiteDriver())` + `.setQueryCoroutineContext(Dispatchers.IO)` | пункт 2; legacy builder молча игнорирует `.setDriver()`. Текущий `RoomModule.kt` имеет ровно эту legacy-форму, плюс `.addMigrations(migration_1_2, …, migration_10_11)` (10 миграций сохраняются как есть) |
| `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt` | **НЕ изменяем** в prereq | F002-решение: callback в текущем коде отсутствует (verify через `grep -i "Callback\|onCreate\|onOpen"` в `Database.kt` и `RoomModule.kt` — пусто). Override `onCreate(connection: SQLiteConnection)` не добавляем — нечего переопределять. Brief-пункт 3 формально неприменим к текущему состоянию кода; вернёмся к нему в IS481 main, когда callback станет нужен под `seedBuiltIns(connection)`. **Бриф рассинхронизирован с фактом — нужно либо пометить пункт 3 как «moot for prereq», либо удалить.** |
| `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/base/BaseMigration.kt` | перегрузка `MigrationTestHelper` ctor на Room 2.7+ KMP-форму с явным driver-параметром (`BundledSQLiteDriver`) и `databaseClass = Database::class.java`. Конкретный сигнатур-вариант (ctor list: `instrumentation, databaseClass, driver` либо `(instrumentation, file, driver, databaseClass, …)`) подбирается из актуального API Room 2.8.4 при имплементации; принцип — bundled driver передаётся явно | F001; без правки `BaseMigration` AllMigrationTest продолжает прогоняться через legacy `SupportSQLiteDatabase` API helper'а, и bundled driver к 10 существующим миграциям не подставится → acceptance 6.1 формально невыполним. Файл текущий: `MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), Database::class.java)` (lines 18-22) — legacy ctor без driver-аргумента |
| `app/proguard-rules.pro` | добавить `-keep class androidx.sqlite.driver.bundled.** { *; }` + `-keep class androidx.sqlite.** { native <methods>; }` | пункт 5; защита от R8 strip нативных методов в release. Текущий proguard-rules уже keep'ает Room/Compose/Coroutines — добавление в том же стиле. Активность keep-rules зависит от `isMinifyEnabled = true` в release (на момент анализа — есть, `app/build.gradle.kts:97`) |
| `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/BundledSqliteFeatureTest.kt` | **новый файл** | acceptance criterion 6.2; smoke androidTest проверяет `ALTER TABLE DROP COLUMN`, `json_object` + `json_insert($, '$[#]', ...)`, `json_each` + `json_remove`, `sqlite_version() >= 3.45`. Детали harness/fixtures/SQL — в разделе ниже |

**Не трогаем (но рядом):**
- 10 файлов `Migration_NNN_to_MMM.kt` в `core/core-db-impl/.../room/migrations/` — out of scope (compat layer Room 2.8 покрывает). Только если 6.1 покажет регрессию — точечная правка одного файла.
- `schemas/me.apomazkin.core_db_impl.room.Database/*.json` — НЕ трогаем (нет change БД-версии).
- Schemable / DataProvider в androidTest — НЕ трогаем (нет новой версии таблицы).
- `core/core-db-api/build.gradle.kts` — out of scope (это IS481 main, не prereq).

## Test harness `BundledSqliteFeatureTest`

Новый androidTest, **не** опирается на Room-стек / `MigrationTestHelper` / реальную БД приложения. Цель — независимо подтвердить что bundled driver реально активен и предоставляет требуемые SQLite features под фактической runtime-версии.

### Harness — как открыть SQLiteConnection от bundled driver

Используем напрямую `BundledSQLiteDriver` (из `androidx.sqlite:sqlite-bundled`) — без Room, без `RoomDatabase`:

```kotlin
val driver = BundledSQLiteDriver()
val conn: SQLiteConnection = driver.open(":memory:")
try {
    // ... SQL + assertions ...
} finally {
    conn.close()
}
```

Каждый `@Test` открывает свежий in-memory соединение, выполняет SQL через `conn.prepare(sql).use { stmt -> stmt.step(); stmt.getText(0)/getLong(0) }` (либо `androidx.sqlite.execSQL(conn, sql)` extension для side-effect SQL), закрывает.

### Fixtures

In-memory empty DB (`":memory:"`). Не зависит от состояния продакшен-БД, не зависит от Room-генератора, не требует prepopulated assets. Каждый тест — изолирован.

### Конкретные SQL-проверки

| Тест | SQL | Ассерт |
|---|---|---|
| `alterTableDropColumn_isSupported` | `CREATE TABLE t (a INTEGER, b INTEGER);` `ALTER TABLE t DROP COLUMN b;` | `SELECT name FROM pragma_table_info('t')` — в результате только `a`, нет `b`. Без exception на `DROP COLUMN`. |
| `jsonObject_isSupported` | `SELECT json_object('k', 'v') AS r;` | `r == '{"k":"v"}'` |
| `jsonInsertAppend_isSupported` | `SELECT json_insert(json_array(), '$[#]', json_object('x', 1)) AS r;` | `r` — валидный JSON, парсится как массив из одного объекта `{"x":1}` |
| `jsonEach_returns3Rows` | `SELECT count(*) FROM json_each(json_array(1, 2, 3));` | count == 3 |
| `jsonRemove_isSupported` | `SELECT json_remove(json_array(1, 2, 3), '$[1]') AS r;` | `r == '[1,3]'` |
| `sqliteVersion_isAtLeast3_45` | `SELECT sqlite_version() AS v;` | `v >= "3.45"` (semver-сравнение / startsWith "3.45" / "3.46" / "3.4N" с N≥5 / "3.5x" / "4.x"). Также логируем `v` в logcat для F009 runtime-verify |

### MinSdk 23 verify

`core/core-db-impl/build.gradle.kts:19` — `minSdk = 23`. `androidx.sqlite:sqlite-bundled` декларирует `minSdk = 21` (см. документацию AndroidX SQLite), поэтому формально совместим с проектным minSdk 23. Verify-шаг — **прогнать `BundledSqliteFeatureTest` на эмуляторе/устройстве API 23**, не только на последнем API. Если хоть один тест упал именно на API 23 (а на API 34 — прошёл) — это сигнал что pinned bundled version не поддерживает minSdk 23 → нужно поднять `sqliteBundledVersion` либо в исключительном случае поднять project `minSdk` (последнее — out of scope prereq, требует отдельной фичи).

### Если хоть один тест упал

Bundled driver version необходимо pin до версии содержащей SQLite 3.45+; либо для конкретной упавшей операции применить fallback (recreate-table для DROP COLUMN, Kotlin string-building для json_insert) — но fallback'и материализуются в IS481 main, не в prereq. В prereq — pin версии правится, скоуп остаётся.

## Verify procedure (acceptance criteria)

### 6.1. Прогон AllMigrationTest под bundled driver

Перегрузить `MigrationTestHelper` в `BaseMigration.kt` (см. «Затронутые файлы») на Room 2.7+ KMP-форму с явным `BundledSQLiteDriver()`. Прогнать существующий `AllMigrationTest`. Все 10 миграций должны пройти **без изменений тел миграций**.

**Если хоть одна миграция падает** — точечная правка падающей миграции на `override fun migrate(connection: SQLiteConnection)` + `connection.execSQL(...)` через `androidx.sqlite.execSQL` extension. **НЕ переписывать все 10**. Если падает 5+ — переоценить scope (это уже не «infra prereq», а часть основной IS481).

### 6.2. SQLite features verify

Прогон нового `BundledSqliteFeatureTest` (см. раздел «Test harness» выше). Все 6 тестов проходят на эмуляторе minSdk 23 + последний API.

### 6.3. Application start — без crash (Manual smoke check)

**Решение:** manual smoke check (не automated). Обоснование — `app/`-модуль использует Dagger DI + full app boot + UI; instrumented `ApplicationStartSmokeTest` дублировал бы существующие UI-тесты модулей и не покрывал бы release-сборку с активным ProGuard (а именно release — главный риск bundled driver — `UnsatisfiedLinkError` после R8 strip).

**Чек-лист:**

1. Собрать debug APK: `./gradlew assembleDebug`. Установить на эмулятор **API 23** (minSdk). Запустить приложение → проверить отсутствие crash в `adb logcat` за первые 10 секунд (фильтр по pid процесса).
2. Открыть приложение → нажать «создать словарь» → добавить лексему. Никаких crashes, никаких `UnsatisfiedLinkError`.
3. Повторить п.1-2 на эмуляторе **API 34** (последний). Это покрывает «работает на старом и новом Android».
4. Убедиться что `app/build.gradle.kts` release block имеет `isMinifyEnabled = true` (на момент анализа: line 97 — есть). **Если внезапно изменено — F008 поднять флаг.**
5. Собрать release APK с применённым ProGuard: `./gradlew assembleRelease` (потребует keystore). Установить, повторить п.1-2. Это smoke-проверка что keep-rules сработали и нативные методы bundled SQLite не были strip'нуты R8.
6. **Runtime sqlite_version logging (F009):** в debug-сборке однократно при старте (например, через `applicationContext.let { db.openHelper.writableDatabase.query("SELECT sqlite_version()") }` либо через первый запрос к Room с логом, либо отдельным dev-only init-блоком в `Application.onCreate`) залогировать `sqlite_version()` в logcat. Подтвердить что значение ≥ `3.45` — это runtime-доказательство что под Room именно bundled driver, а не системный. **Альтернатива** (если callback / dev-init добавлять не хочется в prereq): instrumented тест `ProvidedDatabaseSqliteVersionTest` который через DI получает `Database` и выполняет тот же SELECT — fail если `< 3.45`. Решение между «логирование» и «instrumented тест» — на этапе имплементации; оба варианта закрывают F009.

## Релевантные спеки и гайды

| Файл | Релевантность |
|---|---|
| `docs/features/IS481_lexeme_component_constructor/plan/05_migration_strategy.md` (раздел «Bundled SQLite (предварительное)», строки 9-53) | source of truth: оттуда выделен скоуп этой prereq-фичи. Описывает зачем нужен bundled, как переписать builder, callback override, proguard, verify-процедура. |
| `docs/features/IS481_lexeme_component_constructor/plan/03_database_design.md` | контекст основной фичи (зачем bundled — для будущей M11→M12 с DROP COLUMN + JSON1). Не нужен для имплементации prereq, нужен для контекста. |
| `docs/guides/data-layer.md` | архитектурный гайд по data-слою: где живёт Room, как устроен `RoomModule`, конвенции маппинга. Раздел «Room Database» актуален. |
| `docs/guides/testing-migrations.md` (§ Каркас, § BaseMigration, § AllMigrationTest) | как устроен `AllMigrationTest` (Suite из MigrationFromNNtoMM) и `BaseMigration` (helper rule), который мы будем модифицировать (F001 — перегрузка `MigrationTestHelper` ctor). Новый `BundledSqliteFeatureTest` пишется по стилю androidTest, но **вне** этого каркаса (не миграционный тест, а feature smoke; harness — прямой `BundledSQLiteDriver.open(":memory:")`). |
| `docs/guides/dagger-di.md` | актуален для `RoomModule.kt` — это Dagger-модуль с `@Provides`. После переписки структура `@Module` / `@Singleton` / `@Provides` остаётся неизменной. |
| `docs/features-spec/` | спеки для этой фичи нет. `spec_filename: null`. |

## Sub-flow для запуска

| Sub-flow | Запускать? | Обоснование |
|---|---|---|
| Infrastructure | **да** | gradle deps + version pin + ProGuard keep-rules + DI provider правка (`RoomModule`). Это центральный sub-flow фичи. |
| Business | **нет** | нет ни State/Msg/Reducer/Effect, ни UseCase/доменных сущностей. Пропускается через `if: business_touched`. |
| UI | **нет** | нет composables / layouts / ресурсов. Пропускается через `if: ui_touched`. |
| Data | **да** | переписка `RoomModule.kt` + перегрузка `MigrationTestHelper` в `BaseMigration.kt` (test harness data-слоя) — DI/data-уровень; новый androidTest `BundledSqliteFeatureTest`; regression-прогон `AllMigrationTest` под bundled driver. Параллельно с Infra по porядку (Infra → Business → UI ∥ Data; Business skipped). |

```yaml
infra_touched: true
business_touched: false
ui_touched: false
data_touched: true
needs_tests: true
needs_migration_tests: false
feature_has_ui_contract: false
spec_filename: null
```

**Обоснование флагов:**
- `needs_tests: true` — нужен новый androidTest `BundledSqliteFeatureTest` (acceptance 6.2). Это не «тривиальный infra-фикс» уровня ProGuard rule — мы вводим новый driver, под которым нужно подтвердить наличие критичных SQLite features. Также regression-прогон `AllMigrationTest` обязателен и требует правки `BaseMigration.kt`.
- `needs_migration_tests: false` — `RoomDatabase.version` НЕ инкрементируется (остаётся 11), schema не меняется. Новой миграции не появляется. Поэтому формальные «миграционные тесты» по гайду `testing-migrations.md` не пишутся. Regression-прогон существующего `AllMigrationTest` + правка test-harness `BaseMigration` ≠ написание новых миграционных тестов.
- `feature_has_ui_contract: false` — нет UI-контракта, фича невидима для пользователя.
- `spec_filename: null` — в `docs/features-spec/` спеки для этой фичи нет (`bundled-sqlite.md` отсутствует). Истина — в `05_migration_strategy.md` основной фичи + этот `02_scope.md`.

_model: claude-opus-4-7[1m]_
