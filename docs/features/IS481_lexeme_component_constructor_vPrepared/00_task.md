# IS481 Prepared: Bundled SQLite driver prereq

Маленькая prereq-фича, выделенная из IS481 (lexeme component constructor) для отдельной мерж'а ДО основной фичи.

**Полная документация основной фичи:** `docs/features/IS481_lexeme_component_constructor/plan/` (план, decisions, review).

---

## Цель

Подключить `BundledSQLiteDriver` (Room 2.7+ KMP-builder + `androidx.sqlite:sqlite-bundled`) в проект **без** изменения существующих 10 миграций. Verify что compat-layer Room 2.8 покрывает legacy `migrate(db: SupportSQLiteDatabase)` API + critical SQLite features (`ALTER TABLE DROP COLUMN`, JSON1) доступны под фактической bundled version.

**Зачем сейчас (отдельной фичей):** IS481 миграция M11→M12 требует SQLite 3.45+ для `ALTER TABLE DROP COLUMN` + `json_insert($, '$[#]', ...)`. Система SQLite на minSdk 23 = 3.8.10 — не поддерживает. Без bundled driver основная миграция падает. Атомарный prereq merge → отдельный PR с low risk → если 10 existing миграций упадут под bundled, поймаем раньше IS481 main.

---

## Скоуп (что делаем в этой фиче)

### 1. Gradle dependency

`deps/datastore.versions.toml`:
- Добавить `sqliteBundledVersion` (минимум — версия, гарантирующая SQLite **3.45+**; pin для защиты от gradle downgrade).
- Алиас `sqliteBundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "sqliteBundledVersion" }`.

`core/core-db-impl/build.gradle.kts`:
- `implementation(datastoreLibs.sqliteBundled)`.

### 2. RoomModule — переписка с legacy на KMP-builder Room 2.7+

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt:37`:

**Сейчас:**
```kotlin
return Room.databaseBuilder(context, Database::class.java, "name")
    .addMigrations(...)
    .build()
```

**Заменить на:**
```kotlin
return Room.databaseBuilder<Database>(context = context, name = "name")
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .addMigrations(...)
    .build()
```

**Важно:** legacy builder `Room.databaseBuilder(context, Database::class.java, "name")` **молча игнорирует** `.setDriver()` → bundled SQLite не активируется. Переписка обязательна.

### 3. Database.Callback — переопределить `onCreate(connection: SQLiteConnection)`

Если в `RoomModule` уже есть `Database.Callback` с `onCreate(db: SupportSQLiteDatabase)` — добавить параллельный override:

```kotlin
override fun onCreate(connection: SQLiteConnection) {
    // legacy seed/setup logic (если был в SupportSQLiteDatabase override) — перенести сюда
}
```

Legacy `onCreate(db: SupportSQLiteDatabase)` под bundled driver молча игнорируется. В IS481 main миграция перенесёт `seedBuiltIns(connection)` в callback. В prereq — просто убедиться что callback переопределён правильно (даже если пустой).

### 4. Import

Во всех файлах где используются миграции / Database.Callback / DAO — `import androidx.sqlite.execSQL` (extension `SQLiteConnection.execSQL(sql: String)`). Для prereq — НЕ переписываем 10 existing миграций. Только новый код (callback) использует extension.

### 5. ProGuard keep-rules

`app/proguard-rules.pro`:

```
-keep class androidx.sqlite.driver.bundled.** { *; }
-keep class androidx.sqlite.** { native <methods>; }
```

Без этого R8 может strip нативные методы bundled SQLite в release → `UnsatisfiedLinkError` на старте.

### 6. APK size note

Bundled SQLite добавляет ~1MB на abi × 4 abi = ~4-5MB универсальный APK. Для prod через AAB Play split per-abi → ~1MB на пользователя. Зафиксировать в release notes.

---

## Verify procedure (acceptance criteria)

### 6.1. Все 10 existing миграций под bundled driver — прогон

В `core/core-db-impl/src/androidTest/` существующий `AllMigrationTest` (либо аналог) — **прогнать на bundled driver**. Все 10 миграций (`Migration_001_to_002.kt` … `Migration_010_to_011.kt`) должны пройти **без изменений**.

**Если хоть одна миграция падает** — точечно править падающую миграцию на `override fun migrate(connection: SQLiteConnection)` + `connection.execSQL(...)` через extension. **НЕ переписывать все 10**.

**Если все 10 проходят** — Room 2.8 compat-layer покрывает legacy `SupportSQLiteDatabase` API.

### 6.2. SQLite features verify (symmetric M2 решение)

Добавить smoke test `core/core-db-impl/src/androidTest/BundledSqliteFeatureTest.kt`:

```kotlin
@Test fun alterTableDropColumn_isSupported() {
    // CREATE TABLE test_t (a INTEGER, b INTEGER)
    // ALTER TABLE test_t DROP COLUMN b
    // Assert: no exception, column gone
}

@Test fun jsonObject_andJsonInsertAppend_areSupported() {
    // SELECT json_object('k', 'v') — assert valid JSON
    // SELECT json_insert(json_array(), '$[#]', json_object('x', 1)) — assert array has 1 element
}

@Test fun jsonEach_andJsonRemove_areSupported() {
    // SELECT * FROM json_each(json_array(1, 2, 3)) — assert 3 rows
    // SELECT json_remove(json_array(1, 2, 3), '$[1]') — assert [1, 3]
}

@Test fun sqliteVersion_isAtLeast3_45() {
    // SELECT sqlite_version() — log + assert >= "3.45"
}
```

**Если хоть один тест падает** → bundled driver version необходимо pin до версии содержащей SQLite 3.45+; либо для конкретной упавшей операции применить fallback (recreate-table для DROP COLUMN, Kotlin string-building для json_insert).

### 6.3. Application start — без crash

После всех правок: запустить debug-сборку на эмуляторе minSdk 23 + последний API. Открыть приложение, нажать «создать словарь», добавить лексему. Никаких crashes, никаких `UnsatisfiedLinkError`. Это smoke check что Room actually use bundled driver и ProGuard keep-rules работают.

---

## Out-of-scope (НЕ делаем в этой prereq-фиче)

- Миграция Migration_11_12 (= IS481 main).
- Новые таблицы `component_types`, `component_values`, `quiz_configs`.
- Domain types `BuiltInComponent` / `ComponentType` / `ComponentValue` / etc.
- API DTO новые.
- mate переписка definition wrappers.
- Quiz wire.
- `addDictionary` атомарность с `insertDefaultQuizConfig`.

Всё это — IS481 main, после merge prereq.

---

## Cross-refs (источники)

- `docs/features/IS481_lexeme_component_constructor/plan/05_migration_strategy.md` § «Bundled SQLite (предварительное)» — детальный чеклист, основа этого брифа.
- `docs/features/IS481_lexeme_component_constructor/plan/_alignment_decisions.md` § AGG-7 — решение «10 миграций не переписываем, verify через прогон тестов».
- `docs/features/IS481_lexeme_component_constructor/plan/_alignment_decisions.md` § AGG-8 — smoke test для `json_insert` синтаксиса.
- `docs/features/IS481_lexeme_component_constructor/plan/_alignment_decisions.md` § B1 — `Database.Callback.onCreate(connection: SQLiteConnection)`.
- `docs/features/IS481_lexeme_component_constructor/plan/_alignment_decisions.md` § B3 — `SQLiteConnection.execSQL` через `androidx.sqlite.execSQL` extension.
- `docs/features/IS481_lexeme_component_constructor/plan/_alignment_decisions.md` § B2 — **OBSOLETE** (старое решение «переписать все 10», отменено AGG-7 после verify).
- `docs/review_agents/findings_log.md` Run 8 M2 — pin минимальной версии bundled driver + symmetric verify smoke test для `ALTER TABLE DROP COLUMN`.

---

## Размер фичи и риски

**Размер:** очень маленький. 2-3 строки gradle + переписка 1 RoomModule.kts + override callback + ProGuard keep-rules + 1 androidTest файл с feature verify. ~ 1 день имплементации.

**Главный риск:** какая-то из 10 existing миграций упадёт под bundled. Mitigation — verify procedure (6.1) ловит это в isolation; точечная правка падающей миграции (один файл) — не блокер на основную IS481 фичу.

**Готовность к flow:** scope чётко очерчен, acceptance criteria измеримы, all decisions triaged в plan/_alignment_decisions.md.
