# Infra test plan — IS481 Prepared (Bundled SQLite driver prereq)

Test plan по TDD: тесты пишутся **ДО** реализации. Source of truth — `02_scope.md` (acceptance 6.1/6.2/6.3) + `infra_design_tree.md` (узлы #3 BaseMigration, #4 BundledSqliteFeatureTest).

## Резюме

| Acceptance | Что | Тип | Файл | Состояние |
|---|---|---|---|---|
| 6.1 | Regression: 10 existing миграций под bundled driver | androidTest (instrumented) | `BaseMigration.kt` (перегрузка ctor `MigrationTestHelper`) + прогон существующего `AllMigrationTest.kt` | существующий harness, правка узла #3 |
| 6.2 | SQLite features verify (DROP COLUMN / JSON1 / version ≥ 3.45) | androidTest (instrumented) | `BundledSqliteFeatureTest.kt` (новый) | новый файл, узел #4 |
| 6.3 | Application start без crash (debug + release × API 23 + 34) | manual smoke check | — | чек-лист, не automated |

**Unit tests** (JVM `testDebugUnitTest`) — **нет**. Бизнес-логика не появляется; всё что нужно протестировать требует фактического SQLite engine → androidTest only.

**CI оговорка:** `on_feature_push.yml` и `on_prerelease.yml` запускают только `testDebugUnitTest` — instrumented (`connectedAndroidTest`) **в CI не выполняется** (`infra_walkthrough.md` § 5). Поэтому acceptance 6.1/6.2 покрываются **локально**:
```bash
./gradlew :core:core-db-impl:connectedDebugAndroidTest
```
Acceptance 6.3 — manual чек-лист по факту имплементации.

---

## (а) `BundledSqliteFeatureTest.kt` — новый androidTest (узел #4)

### Назначение

Независимо подтвердить что bundled driver реально активен и предоставляет требуемые SQLite features под фактической runtime-версии. **Не** зависит от Room-стека / `MigrationTestHelper` / реальной БД приложения. Harness — прямой `BundledSQLiteDriver().open(":memory:")`.

### Расположение

```
core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/BundledSqliteFeatureTest.kt
```

Корень `room/` рядом с `AllMigrationTest.kt` (`infra_walkthrough.md` факт 7.3). **Не** под `room/base/` (это не миграционный harness) и **не** под `room/migrations/` (это не миграционный тест).

### Harness

In-memory connection через bundled driver — никакой реальной БД, никаких prepopulated assets, никаких миграций. Каждый `@Test` изолирован: `@Before` открывает свежий `:memory:` connection, `@After` закрывает.

```kotlin
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

    // ... тесты ...
}
```

**Обоснование выбора harness:**
- `:memory:` — нет I/O, нет file cleanup, изоляция между тестами автоматическая.
- Прямой `BundledSQLiteDriver().open(...)` — **исключает** Room compat layer из проверки. Цель теста — подтвердить что bundled native binary поставляет нужные feature'ы, а не что Room их прокинул.
- Если бы тестировали через `Room.databaseBuilder<Database>(...).setDriver(BundledSQLiteDriver()).build()` — успех мог бы значить «Room compat layer симулирует DROP COLUMN через recreate-table fallback», а не «bundled SQLite 3.45+ реально активен».

### Импорты

```kotlin
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL                            // extension для side-effect SQL
import androidx.sqlite.use                                // extension для prepared statement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log
```

(Точные имена extension'ов — `androidx.sqlite.execSQL` / `androidx.sqlite.SQLiteStatement.use` — уточняются по фактической Room 2.8.4 / sqlite-bundled API на этапе имплементации.)

### Test cases (6 шт)

#### Test 1 — `alterTableDropColumn_isSupported`

**Цель:** подтвердить `ALTER TABLE ... DROP COLUMN` (SQLite 3.35+). Это критично для будущей IS481 main миграции v11→v12.

```kotlin
@Test
fun alterTableDropColumn_isSupported() {
    // Arrange — таблица с двумя колонками
    conn.execSQL("CREATE TABLE t (a INTEGER, b INTEGER)")
    conn.execSQL("INSERT INTO t (a, b) VALUES (1, 2)")

    // Act — DROP COLUMN b
    conn.execSQL("ALTER TABLE t DROP COLUMN b")

    // Assert — в pragma_table_info только колонка 'a', нет 'b'
    val columns = mutableListOf<String>()
    conn.prepare("SELECT name FROM pragma_table_info('t')").use { stmt ->
        while (stmt.step()) {
            columns += stmt.getText(0)
        }
    }
    assertEquals(listOf("a"), columns)
}
```

**Что проверяем:** SQL `ALTER TABLE t DROP COLUMN b` не бросает exception (главное), и pragma подтверждает что колонка ушла. На системном SQLite Android API ≤ 30 эта операция упала бы с `SQL error or missing database (near "DROP")`.

#### Test 2 — `jsonObject_isSupported`

**Цель:** базовый JSON1 — `json_object('k','v')`. Это минимальная проверка что JSON1 extension вообще скомпилирована в bundled binary.

```kotlin
@Test
fun jsonObject_isSupported() {
    conn.prepare("SELECT json_object('k', 'v') AS r").use { stmt ->
        assertTrue("expected at least one row", stmt.step())
        val result = stmt.getText(0)
        assertEquals("""{"k":"v"}""", result)
    }
}
```

#### Test 3 — `jsonInsertAppend_isSupported`

**Цель:** `json_insert(json_array(), '$[#]', json_object('x', 1))` — pattern для append'а в массив. Это конкретный pattern, который будет использоваться в будущей IS481 main для построения lexeme.components.

```kotlin
@Test
fun jsonInsertAppend_isSupported() {
    conn.prepare("SELECT json_insert(json_array(), '$[#]', json_object('x', 1)) AS r").use { stmt ->
        assertTrue(stmt.step())
        val result = stmt.getText(0)
        // result — валидный JSON; точное форматирование пробелов между токенами
        // может отличаться по версиям, поэтому проверяем семантику:
        // - содержит ключ "x"
        // - содержит значение 1
        // - является массивом из одного объекта
        assertTrue("result must contain object {\"x\":1}", result.contains(""""x":1"""))
        assertTrue("result must be a JSON array", result.startsWith("[") && result.endsWith("]"))
    }
}
```

**Замечание:** проверяем семантически (содержит `"x":1`, форма `[...]`), а не точное равенство — бандл SQLite может варьировать пробельное форматирование json output. Если упадёт на semver-edge — strict-вариант `assertEquals("""[{"x":1}]""", result)` остаётся fallback'ом.

#### Test 4 — `jsonEach_returns3Rows`

**Цель:** table-valued function `json_each` (JSON1). Используется для итерации по JSON-массиву в SQL.

```kotlin
@Test
fun jsonEach_returns3Rows() {
    conn.prepare("SELECT count(*) FROM json_each(json_array(1, 2, 3))").use { stmt ->
        assertTrue(stmt.step())
        val count = stmt.getLong(0)
        assertEquals(3L, count)
    }
}
```

#### Test 5 — `jsonRemove_isSupported`

**Цель:** `json_remove(json_array(1,2,3), '$[1]')` — удаление элемента по индексу. Pattern для будущего удаления component из lexeme.

```kotlin
@Test
fun jsonRemove_isSupported() {
    conn.prepare("SELECT json_remove(json_array(1, 2, 3), '$[1]') AS r").use { stmt ->
        assertTrue(stmt.step())
        val result = stmt.getText(0)
        assertEquals("[1,3]", result)
    }
}
```

#### Test 6 — `sqliteVersion_isAtLeast3_45`

**Цель:** runtime-проверка что под driver'ом SQLite ≥ 3.45 (требование acceptance 6.2 + F009).

```kotlin
@Test
fun sqliteVersion_isAtLeast3_45() {
    conn.prepare("SELECT sqlite_version()").use { stmt ->
        assertTrue(stmt.step())
        val version = stmt.getText(0)
        Log.i("BundledSqliteFeatureTest", "Bundled SQLite version: $version")

        assertTrue(
            "Expected SQLite >= 3.45, got $version",
            isVersionAtLeast(version, major = 3, minor = 45)
        )
    }
}

/**
 * Парсит "3.45.0" / "3.46.1" / "4.0.0" и сравнивает с (major, minor).
 * Принимает "3.45", "3.45.0", "3.46", "4.0", и т.п.
 * Возвращает true если actual >= требуемого.
 */
private fun isVersionAtLeast(version: String, major: Int, minor: Int): Boolean {
    val parts = version.split(".")
    val actualMajor = parts.getOrNull(0)?.toIntOrNull() ?: return false
    val actualMinor = parts.getOrNull(1)?.toIntOrNull() ?: return false
    return when {
        actualMajor > major -> true
        actualMajor < major -> false
        else -> actualMinor >= minor
    }
}
```

**Замечание:** semver-сравнение через split + toInt — устойчиво к "3.45.0" / "3.46" / "4.0.1". Lexicographic `version >= "3.45"` ломается на "3.5" (5 < 4 в строковом сравнении ложно даст true). Поэтому именно числовое сравнение `(major, minor)`.

`Log.i` в logcat — закрывает F009 (runtime sqlite_version verify); при прогоне теста в logcat можно глазом видеть «Bundled SQLite version: 3.46.1».

### Edge cases / negative coverage

В скоупе prereq явно **не покрываем**:
- Concurrent access — bundled driver thread-safety тестируем имплицитно в acceptance 6.3 (нормальная работа приложения).
- WAL mode behavior — нет в брифе, не требуется для prereq.
- Performance — vs системного SQLite. Out of scope.
- Negative case «system driver НЕ умеет DROP COLUMN на API 23» — теоретически полезно как контраст, но требует параллельного теста через `FrameworkSQLiteOpenHelperFactory`, что удваивает сложность; F010 — оставляем как «возможное улучшение» для будущего, не блокер acceptance.

### Запуск локально

```bash
# Эмулятор API 23 (minSdk)
./gradlew :core:core-db-impl:connectedDebugAndroidTest \
    --tests "me.apomazkin.core_db_impl.room.BundledSqliteFeatureTest"

# Эмулятор последний API (34)
# (повторить то же на втором эмуляторе)
```

**Pass criteria:** все 6 тестов зелёные на API 23 **и** API 34. Если упало на API 23, но прошло на API 34 → поднять `sqliteBundledVersion` в `deps/datastore.versions.toml` (узел #0); это сигнал что pinned bundled version не поддерживает minSdk 23.

> 📎 guide: `docs/guides/testing-migrations.md` — "Запуск: `./gradlew :core:core-db-impl:connectedDebugAndroidTest` (нужен эмулятор/устройство)"

---

## (б) Regression: `AllMigrationTest` под bundled driver (узел #3)

### Назначение

Подтвердить acceptance 6.1: 10 существующих миграций (legacy `migrate(db: SupportSQLiteDatabase)` API) проходят под bundled driver **без изменений тел миграций**. Compat layer Room 2.8 должен обернуть bundled connection в `SupportSQLiteDatabase`-фасад.

### Требуемое изменение test harness (узел #3)

**Файл:** `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/base/BaseMigration.kt`

**Было** (lines 18-22):
```kotlin
@get:Rule
val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    Database::class.java
)
```

**Стало** (псевдокод; точная сигнатура ctor — по Room 2.8.4 API на этапе имплементации):
```kotlin
@get:Rule
val helper: MigrationTestHelper = MigrationTestHelper(
    instrumentation = InstrumentationRegistry.getInstrumentation(),
    databaseClass = Database::class.java,
    driver = BundledSQLiteDriver(),
    // дополнительные параметры (file, databaseFactory, openFactory) — по фактическому API
)
```

Новый импорт: `androidx.sqlite.driver.bundled.BundledSQLiteDriver`.

**Что НЕ меняется:**
- `runMigrateDbTest` body (lines 47-68) — `createDatabase` / `runMigrationsAndValidate` сигнатуры не меняются на pre-Room-2.8 API (возвращают `SupportSQLiteDatabase`); compat layer Room 2.8 оборачивает bundled connection.
- `setUp` / `tearDown` (lines 24-30) — пустые, без изменений.
- `getMigrationClass` / `getCurrentVersion` (lines 32-33) — без изменений.
- 10 файлов `MigrationFromNNtoMM.kt` — **не трогаются** (их callback'и работают через `SupportSQLiteDatabase`, который compat layer им и поставит).
- `AllMigrationTest.kt` (Suite) — **не трогается** (имена тест-классов те же, состав Suite тот же).

### Test plan

**Прогон:**
```bash
./gradlew :core:core-db-impl:connectedDebugAndroidTest \
    --tests "me.apomazkin.core_db_impl.room.AllMigrationTest"
```

**Pass criteria (acceptance 6.1):**
- Все 10 миграций проходят: `MigrationFrom01to02` … `MigrationFrom10to11` — все @Test зелёные.
- В logcat нет `SupportSQLiteDatabase` deprecation warnings ниже уровня INFO (опционально — `--info` для verbose Room logs).
- Время прогона не выросло катастрофически (если регрессия >3× — сигнал что compat layer работает медленно, поднимать вопрос отдельно — F011, не блокер acceptance).

**Fail scenarios и реакция:**

| Сценарий | Реакция |
|---|---|
| Падает 1 миграция | Точечная правка падающей миграции на `override fun migrate(connection: SQLiteConnection)` + `androidx.sqlite.execSQL(connection, ...)`. **НЕ переписывать остальные 9.** |
| Падает 2-4 миграции | То же — точечно, по одной. Зафиксировать в `infra_implementation.md` какие именно и почему. |
| Падает ≥5 миграций | **Stop the line.** Переоценить scope — это уже не «infra prereq», а часть основной IS481. Эскалировать оркестратору. |
| Падает на ошибке в самом `MigrationTestHelper` (ctor / lifecycle) | Уточнить сигнатуру ctor по фактической Room 2.8.4 docs / sources; не fall back на legacy 2-arg ctor (это закрывает acceptance 6.1 формально через старый API, а не bundled). |

**Что мы НЕ пишем как тест:** новые `MigrationFromNNtoMM` классы — `needs_migration_tests: false` (БД версия не инкрементируется в prereq, schema не меняется, новой миграции нет).

> 📎 guide: `docs/guides/testing-migrations.md` — "BaseMigration с `@get:Rule MigrationTestHelper` — единственная точка hookup'а для всех 10 тест-классов миграций"

---

## (в) Manual smoke check (acceptance 6.3)

### Назначение

Подтвердить что приложение **стартует без crash** на debug+release × API 23+34. Это покрывает release-only-bug риск (R8 strip нативных методов bundled SQLite → `UnsatisfiedLinkError`).

### Pre-requisites

- ProGuard keep-rules добавлены в `app/proguard-rules.pro` (узел #5).
- `app/build.gradle.kts:97` — `isMinifyEnabled = true` (verify glance — если изменено на false, F008 поднять флаг).
- Доступны два эмулятора: **API 23** (Android 6, minSdk) и **API 34** (Android 14, последний).

### Чек-лист (6 шагов, из `02_scope.md` § 6.3)

1. **Build debug APK:** `./gradlew assembleDebug`. Установить на эмулятор **API 23**. Запустить приложение. Проверить отсутствие crash в `adb logcat` за первые 10 секунд (фильтр по pid процесса). **Pass:** нет `AndroidRuntime` FATAL, нет `UnsatisfiedLinkError`.

2. **Smoke flow:** Открыть приложение → создать словарь → добавить слово → добавить лексему. **Pass:** все CRUD операции отрабатывают, нет crash, нет ANR.

3. **Repeat API 34:** Повторить шаги 1-2 на эмуляторе API 34. **Pass:** покрывает «работает на старом и новом Android».

4. **Verify minify flag:** Открыть `app/build.gradle.kts` line 97 — убедиться `isMinifyEnabled = true` в release block. **Pass:** флаг есть. **Fail (F008 trigger):** если флаг false — keep-rules формально не активны → release-only-bug риск остался непроверенным; эскалировать.

5. **Release APK smoke:** Собрать release APK: `./gradlew assembleRelease` (потребует keystore). Установить на API 23 (или API 34, если keystore single-target). Повторить шаги 1-2. **Pass:** нет crash, нет `UnsatisfiedLinkError`. **Это главная проверка** что R8 не strip'нул нативные методы.

6. **Runtime sqlite_version log (F009):** В debug-сборке однократно при старте залогировать `sqlite_version()` в logcat. Способ — на выбор имплементатора:
   - **Вариант A (рекомендуется):** Через тест `sqliteVersion_isAtLeast3_45` из (а) — он уже логирует `Log.i("BundledSqliteFeatureTest", "Bundled SQLite version: ...")`. Локальный прогон теста = подтверждение runtime version. **F009 закрывается через (а).**
   - **Вариант B:** Dev-only init-блок в `App.onCreate` (если хочется видеть version в logcat при каждом запуске debug-сборки) — out of scope prereq, можно отдельной задачей.
   
   В скоупе prereq — выбираем вариант A (тест уже логирует), F009 закрывается через acceptance 6.2.

### Документирование результата

Имплементатор фиксирует результаты чек-листа в `infra_implementation.md` (или соответствующем артефакте имплементации):
- Дата прогона.
- Эмуляторы (API + образ).
- Pass/fail каждого из 6 шагов.
- logcat-сниппет с `Bundled SQLite version: X.Y.Z`.

---

## Сводная таблица acceptance criteria

| AC | Тест | Файл | Тип | Где | Pass criterion |
|---|---|---|---|---|---|
| 6.1 | `AllMigrationTest` (Suite из 10) | `core-db-impl/.../room/AllMigrationTest.kt` + `base/BaseMigration.kt` (правка ctor) | androidTest | local `connectedDebugAndroidTest` | все 10 миграций зелёные |
| 6.2 | 6 тестов в `BundledSqliteFeatureTest` | `core-db-impl/.../room/BundledSqliteFeatureTest.kt` (новый) | androidTest | local `connectedDebugAndroidTest` API 23 + 34 | все 6 зелёные на обоих API |
| 6.3 | 6-шаговый manual чек-лист | — | manual smoke | local emulator API 23 + 34, debug + release | нет crash, нет `UnsatisfiedLinkError`, logcat показывает SQLite ≥ 3.45 |

---

## Не пишем (явное обоснование)

- **Unit tests (JVM `testDebugUnitTest`)** — нет бизнес-логики (TEA / Reducer / UseCase) в скоупе. Всё что есть — Gradle / DI provider / ProGuard / test harness. JVM-тестов под это не пишется.
- **Новые `MigrationFromNNtoMM` тесты** — `needs_migration_tests: false`. БД версия не инкрементируется, schema не меняется, новой миграции в prereq нет.
- **Тест на `RoomModule.provideDatabase`** — провайдер не имеет логики кроме передачи параметров в builder; integration covered косвенно через acceptance 6.3 (приложение стартует — значит провайдер вернул валидный `Database`).
- **Negative test «system driver НЕ умеет DROP COLUMN»** — теоретически полезный контраст к тесту 1 из (а), но удваивает harness (нужен `FrameworkSQLiteOpenHelperFactory`). F010 — оставляем как improvement-предложение, не блокер.
- **Concurrent / WAL / performance тесты bundled driver** — out of scope prereq.

## log_messages

- Test plan покрывает 3 контура: новый `BundledSqliteFeatureTest` (6 cases с конкретным Kotlin кодом + assert'ами по taken-from-scope SQL pattern'ам), regression-прогон `AllMigrationTest` через перегрузку `BaseMigration` ctor, и manual smoke check 6 шагов из scope-документа.
- Harness обоснование: для `BundledSqliteFeatureTest` — прямой `BundledSQLiteDriver().open(":memory:")` (исключает Room compat layer из проверки, чтобы доказать что bundled binary поставляет features, а не Room их симулирует); для regression — модификация `BaseMigration` ctor — единственная точка hookup'а 10 тест-классов.
- Unit-тестов не пишем (нет бизнес-логики); CI не покрывает androidTest (`on_feature_push.yml` запускает только `testDebugUnitTest`), все acceptance выполняются локально.

_model: claude-opus-4-7[1m]_
