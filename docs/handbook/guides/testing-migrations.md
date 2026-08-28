# Тестирование Room-миграций

Актуализировано в IS493 (Э2, 2026-08-10). Прежний кастомный фреймворк
(`BaseMigration` / `Schemable` / `AllMigrationTest` / schemable-версии таблиц)
снесён в IS481 вместе со схлопыванием исторических миграций — этот гайд
описывает **фактический** паттерн: плоские тест-классы на
`MigrationTestHelper` + `BundledSQLiteDriver`.

## Расположение

```
core/core-db-impl/src/androidTest/java/.../room/
├── MigrationFrom11to12.kt                — collapsed 11→12 (Case A..W)
├── MigrationFrom11to12IdempotencyTest.kt — идемпотентность 11→12 (failAfterStep)
├── MigrationFrom12to13.kt                — IS493 группы (Case A..F)
└── GroupDaoTest.kt, Is486DataLayerTest.kt, ... — DAO-тесты (in-memory)
```

Схемы: `core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/N.json`
(экспорт `room { schemaDirectory }`); подключены в androidTest как assets.

## Паттерн теста миграции

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationFrom12to13 {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val dbFile: File = instrumentation.targetContext.getDatabasePath(DB_NAME)

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = dbFile,
        driver = BundledSQLiteDriver(),      // bundled SQLite — как в проде
        databaseClass = Database::class,
    )

    @After
    fun cleanUp() {   // чистим -shm/-wal/-journal
        listOf("", "-shm", "-wal", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).takeIf { it.exists() }?.delete()
        }
    }

    @Test
    fun case() {
        helper.createDatabase(12).use { v12 ->   // схема из 12.json
            v12.execSQL("INSERT INTO ...")        // данные старой версии — сырым SQL
        }
        val v13 = helper.runMigrationsAndValidate(13, listOf(Migration_012_to_013))
        // runMigrationsAndValidate сверяет фактическую схему с 13.json (TableInfo)
        // assert'ы — через prepare/step (см. хелперы countWhere/scalarText в тестах)
        v13.close()
    }
}
```

## Обязательные кейсы

1. **Изолированный шаг** `N-1 → N` с данными: схема валидна, данные живы.
2. **Chained-путь от последней РЕЛИЗНОЙ версии** (боевой путь пользователя):
   `createDatabase(последняя_релизная)` →
   `runMigrationsAndValidate(N, listOf(все миграции цепочки))`.
   Текущая релизная — v11 (деплой-тег 0.1.5); прецедент — Case B
   `MigrationFrom12to13`.
3. **FK cascade** для новых таблиц: в тестовом соединении **обязателен явный
   `PRAGMA foreign_keys=ON`** (helper по умолчанию выключен; в проде
   enforcement включает `Database_Impl.onOpen`). Прецеденты — Case F 11→12,
   Cases C–E 12→13.
4. **Idempotency** (`failAfterStep`-hook, тест-класс отдельно) — ТОЛЬКО для
   миграций с данными/сидами (11→12: 14 шагов, backfill, DROP COLUMN).
   Для чистых `CREATE TABLE/INDEX IF NOT EXISTS` не нужен: идемпотентно по
   построению + Room-транзакция (решение IS493 В4).

## Ловушки сверки с N.json

- `runMigrationsAndValidate` сравнивает TableInfo, не текст DDL: порядок
  колонок не важен, но важны NOT NULL, DEFAULT, **порядок колонок составного
  PK** и **точное множество индексов** (лишний рукописный индекс, который
  Room не экспортирует = провал валидации).
- Имена индексов — конвенция Room `index_<table>_<col>`.
- DDL в миграции пишется буквально по `createSql` из экспортированного
  N.json (сверять руками при новых DDL-формах — прецедент: составной PK
  `word_groups`).

## Запуск

CI androidTest НЕ гоняет (только lint + unit + assemble) — прогон руками
на девайсе/эмуляторе обязателен перед merge:

```bash
./scripts/cc-build.sh :core:core-db-impl:connectedDebugAndroidTest
```

Системное решение (emulator-job на CI) — в Backlog («ВекторныйПиздеж»).
