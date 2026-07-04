# Review: scope_analysis

## Итерация 1 (2026-06-03T20:55:00Z)

### F001 [architect] critical

**Description:** Acceptance 6.1 «прогон AllMigrationTest под bundled driver» требует модификации `BaseMigration.kt` — текущий `MigrationTestHelper(InstrumentationRegistry, Database::class.java)` использует legacy системный SQLite, не bundled. Но `BaseMigration.kt` отсутствует в «Затронутые файлы» и попадает под «не трогаем» → 6.1 формально невыполним без правки вне зафиксированного scope.

**Status:** approved

**Verdict:** BaseMigration/MigrationTestHelper использует legacy ctor — без правки helper'а bundled driver в AllMigrationTest не подставится, а файл не упомянут в scope

### F002 [architect] critical

**Description:** Рассинхрон scope vs brief по `Database.Callback`. Brief фиксирует «в prereq — просто убедиться что callback переопределён правильно (даже если пустой)». Scope утверждает обратное: «в текущем Database.kt callback'а нет, поэтому возможно ничего трогать не нужно». Противоречие между артефактами одной фичи — исполнитель не знает что делать.

**Status:** approved

**Verdict:** scope и brief действительно противоречат друг другу по состоянию Database.Callback — нужно синхронизировать формулировку

### F003 [architect] minor

**Description:** Scope упоминает `.setQueryCoroutineContext(Dispatchers.IO)` в RoomModule, но `core/core-db-impl/build.gradle.kts` не имеет явной зависимости от `kotlinx-coroutines-core` — coroutines подтягиваются транзитивно через `roomKtx`. Хрупко.

**Status:** rejected

**Verdict:** kotlinx-coroutines-core транзитивно через roomKtx — это стандартная практика Gradle, не хрупкость требующая фиксации в scope

### F004 [qa_engineer] critical

**Description:** Acceptance 6.1 не достижим без правки `BaseMigration`/`MigrationTestHelper` — helper создаётся через legacy ctor, bundled driver не подставляется. Дублирует F001.

**Status:** rejected

**Verdict:** дублирует F001 по существу — одна и та же проблема не должна засчитываться дважды

### F005 [qa_engineer] critical

**Description:** `BundledSqliteFeatureTest` объявлен новым файлом, но не описан ни test harness (как открыть `SQLiteConnection` от bundled driver в androidTest без полного Room-стека), ни fixtures (на какой версии БД), ни конкретные SQL-statements/expected results. Bundled driver требует подтверждения поддержки на minSdk 23 — в scope про minSdk нет ни слова.

**Status:** approved

**Verdict:** scope описывает новый androidTest файл одной строкой без harness/fixtures/minSdk подтверждения — для acceptance 6.2 этого недостаточно

### F006 [qa_engineer] critical

**Description:** Scope упускает обязательную зависимость `androidTestImplementation(datastoreLibs.sqliteBundled)` для `core-db-impl` — без неё ни `BundledSqliteFeatureTest`, ни перегруженный `MigrationTestHelper(driver=...)` не скомпилируются в androidTest source set.

**Status:** approved

**Verdict:** для androidTest BundledSqliteFeatureTest нужна androidTestImplementation зависимости, scope фиксирует только implementation — компиляция упадёт

### F007 [qa_engineer] critical

**Description:** «App start smoke check» (acceptance 6.3) в scope не материализован — нет ни файла теста, ни процедуры (manual/instrumented/CI?). Если manual — нужна явная процедура с пред-условиями.

**Status:** approved

**Verdict:** acceptance 6.3 определён в brief, но в scope не материализован (нет файла/процедуры/типа теста) — gap по покрытию acceptance criterion

### F008 [qa_engineer] minor

**Description:** ProGuard keep-rules заявлены, но не проверено что в release ProGuard включён (`isMinifyEnabled = true`). Без этого R8-strip баг не воспроизводится.

**Status:** approved

**Verdict:** ProGuard keep-rules без проверки isMinifyEnabled = true в release не дают гарантии что баг воспроизведётся — verify-шаг отсутствует

### F009 [qa_engineer] minor

**Description:** `BundledSQLiteDriver` молча игнорируется при использовании legacy `Room.databaseBuilder(context, Class, name)`. В scope не зафиксирован verify-шаг «после переписки убедиться что builder именно KMP-вариант» (runtime-логирование `sqlite_version()`).

**Status:** approved

**Verdict:** главный аспект verify_feature_availability сам же scope упоминает, но runtime-verify через sqlite_version logging как отдельный verify-шаг не зафиксирован

### F010 [qa_engineer] minor

**Description:** Acceptance 6.1 не определяет success criterion при частичном падении. Scope противоречит сам себе: либо миграции трогаются (точечная правка), либо нет.

**Status:** rejected

**Verdict:** brief явно даёт success criterion 6.1 — «все 10 проходят без изменений; если падает — точечная правка одного файла»; противоречия нет, scope ретранслирует то же правило
