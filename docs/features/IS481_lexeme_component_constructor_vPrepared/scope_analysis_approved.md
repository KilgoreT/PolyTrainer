# Approved findings — scope_analysis (для следующей итерации)

Применять на следующей итерации scope_analysis. Все findings ниже approved инквизитором — реальные проёбы, требующие правок 02_scope.md.

## F001 critical — BaseMigration legacy ctor

**Проблема:** Acceptance 6.1 «прогон AllMigrationTest под bundled driver» формально невыполним. Текущий `BaseMigration.kt:18-22` использует `MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), Database::class.java)` (legacy ctor с SupportSQLite, без bundled driver).

**Verdict:** BaseMigration/MigrationTestHelper использует legacy ctor — без правки helper'а bundled driver в AllMigrationTest не подставится, а файл не упомянут в scope.

**Что править в `02_scope.md`:**
- В таблицу «Затронутые файлы» добавить `BaseMigration.kt` с правкой: перегрузка `MigrationTestHelper(instrumentation, file, driver = BundledSQLiteDriver(), databaseClass = Database::class.java, ...)` либо аналогичный KMP-конструктор Room 2.7+.
- Зафиксировать в acceptance 6.1: «помимо подключения bundled driver в production коде, требуется перегрузить `MigrationTestHelper` в `BaseMigration` для прогона existing migrations под тем же driver».

---

## F002 critical — scope vs brief drift на `Database.Callback`

**Проблема:** Brief (`00_brief.md:60`) — «в prereq просто убедиться что callback переопределён правильно (даже если пустой)». Scope (`02_scope.md:33`) — «в текущем Database.kt callback'а нет, ничего трогать не нужно». Противоречие.

**Verdict:** scope и brief действительно противоречат друг другу по состоянию Database.Callback — нужно синхронизировать формулировку.

**Что править в `02_scope.md`:**
- Определиться: либо (а) добавлять пустой `override fun onCreate(connection: SQLiteConnection)` в prereq как страховку от случайного использования legacy override в будущем, либо (б) явно зафиксировать «callback в текущем коде отсутствует, override не добавляется; вернёмся к этому в IS481 main миграции когда callback станет нужен для seedBuiltIns».
- Синхронизировать формулировку с brief.

---

## F005 critical — `BundledSqliteFeatureTest` без harness / fixtures / minSdk

**Проблема:** Scope описывает новый androidTest файл одной строкой без harness (как открыть `SQLiteConnection` от bundled driver без полного Room-стека), без fixtures (пустая v11 / новая / in-memory), без конкретных SQL-statements и expected results. Также bundled driver требует подтверждения поддержки на minSdk 23 (см. `core-db-impl/build.gradle.kts:19`).

**Verdict:** scope описывает новый androidTest файл одной строкой без harness/fixtures/minSdk подтверждения — для acceptance 6.2 этого недостаточно.

**Что править в `02_scope.md`:**
- В отдельный раздел «Test harness BundledSqliteFeatureTest» добавить:
  - Как открыть `SQLiteConnection` от bundled driver: `BundledSQLiteDriver().open(":memory:")` либо аналогичный API.
  - Fixtures: in-memory empty DB (не зависит от состояния real DB).
  - Конкретные SQL + ассерты:
    - `CREATE TABLE t (a INT, b INT); ALTER TABLE t DROP COLUMN b;` → проверить через `SELECT * FROM pragma_table_info('t')` что колонки `b` нет.
    - `SELECT json_object('k', 'v') AS r;` → assert result = `'{"k":"v"}'`.
    - `SELECT json_insert(json_array(), '$[#]', json_object('x', 1)) AS r;` → assert result содержит `[{"x":1}]`.
    - `SELECT * FROM json_each(json_array(1,2,3));` → assert 3 rows.
    - `SELECT json_remove(json_array(1,2,3), '$[1]') AS r;` → assert `[1,3]`.
    - `SELECT sqlite_version() AS v;` → assert `v` начинается с `3.45` или выше.
- Подтвердить совместимость bundled driver с minSdk 23 (изучить документацию `androidx.sqlite:sqlite-bundled` или эмпирически в smoke тесте). Зафиксировать в scope как явный verify-шаг.

---

## F006 critical — пропущен `androidTestImplementation(sqliteBundled)`

**Проблема:** В таблице «Затронутые файлы» указан только `implementation(datastoreLibs.sqliteBundled)` в `core/core-db-impl/build.gradle.kts`. Для нового androidTest `BundledSqliteFeatureTest` (а также перегрузки `MigrationTestHelper` с bundled driver — см. F001) требуется `androidTestImplementation(datastoreLibs.sqliteBundled)` — без неё компиляция androidTest source set упадёт.

**Verdict:** для androidTest BundledSqliteFeatureTest нужна androidTestImplementation зависимости, scope фиксирует только implementation — компиляция упадёт.

**Что править в `02_scope.md`:**
- В таблицу «Затронутые файлы» строка `core/core-db-impl/build.gradle.kts` — добавить второе изменение `androidTestImplementation(datastoreLibs.sqliteBundled)` помимо `implementation(...)`.

---

## F007 critical — smoke check 6.3 не материализован

**Проблема:** Brief acceptance 6.3 — «application start без crash в debug-сборке на эмуляторе minSdk 23 + последний API». В scope этот критерий нигде не привязан к конкретной процедуре: manual (ручной запуск)? instrumented test? CI job? Если manual — нет описания пред-условий (release build с применённым ProGuard, иначе keep-rules не проверены).

**Verdict:** acceptance 6.3 определён в brief, но в scope не материализован (нет файла/процедуры/типа теста) — gap по покрытию acceptance criterion.

**Что править в `02_scope.md`:**
- Определиться: 6.3 — manual или automated.
  - **Manual** — добавить в scope раздел «Manual smoke check»: шаги (1) собрать debug-вариант, (2) запустить на эмуляторе minSdk 23, (3) проверить отсутствие crash при старте через `adb logcat`, (4) собрать release-вариант с включённым ProGuard, (5) повторить smoke на release. Зафиксировать как чек-лист в `02_scope.md`.
  - **Automated** — указать новый файл `app/src/androidTest/.../ApplicationStartSmokeTest.kt` либо аналог; добавить в таблицу «Затронутые файлы».

---

## F008 minor — ProGuard `isMinifyEnabled` не проверен

**Проблема:** ProGuard keep-rules добавляются в `app/proguard-rules.pro`, но без `isMinifyEnabled = true` в release build type R8 не активируется → keep-rules не применяются → `release_only_bug` (R8 strip нативных методов bundled SQLite) формально mitigated, но реально непроверен.

**Verdict:** ProGuard keep-rules без проверки isMinifyEnabled = true в release не дают гарантии что баг воспроизведётся — verify-шаг отсутствует.

**Что править в `02_scope.md`:**
- В раздел «Verify procedure» добавить шаг: «убедиться что в `app/build.gradle.kts` release block имеет `isMinifyEnabled = true` (или эквивалент). Если нет — добавить в скоуп (или зафиксировать ограничение что R8 strip не проверяется без отдельной фичи на включение R8)».

---

## F009 minor — KMP builder runtime verify отсутствует

**Проблема:** `BundledSQLiteDriver` молча игнорируется при использовании legacy `Room.databaseBuilder(context, Class, name)`. Scope упоминает аспект `verify_feature_availability`, но runtime-verify «builder именно KMP-вариант, не legacy» (например runtime-логирование `sqlite_version()` в `Database.Callback.onOpen`) как отдельный шаг не зафиксирован.

**Verdict:** главный аспект verify_feature_availability сам же scope упоминает, но runtime-verify через sqlite_version logging как отдельный verify-шаг не зафиксирован.

**Что править в `02_scope.md`:**
- В раздел «Verify procedure» добавить шаг: «runtime-логирование `SELECT sqlite_version()` в debug сборке (через `Database.Callback.onOpen` либо через query на первый запрос) для подтверждения что под Room именно bundled driver, а не системный».
- Альтернативно (если callback не добавляем по F002 решению): unit-тест либо instrumented-тест проверяющий что `RoomModule.provideDatabase()` возвращает Database с активным `BundledSQLiteDriver`.
