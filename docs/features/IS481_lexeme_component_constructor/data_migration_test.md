# Data migration test plan — IS481 M11→M12

Test план для миграционного теста + integration тестов data-слоя. Реализуется в `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/`.

`needs_migration_tests = true` (см. 02_scope.md). Восстанавливается с нуля — prereq фича удалила всю legacy migration test infrastructure (BaseMigration, Schemable, DataProvider, Schema, 12 snapshot entities).

---

## 1. Harness — `MigrationFrom11to12.kt`

**Path:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom11to12.kt`

**Skeleton:**

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationFrom11to12 {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        driver = BundledSQLiteDriver(),
        databaseClass = Database::class,
    )

    // Каждый @Test:
    //   1. val db = helper.createDatabase(DB_NAME, 11) — создаёт v11 БД.
    //   2. INSERT тестовых данных через connection.execSQL.
    //   3. db.close().
    //   4. val migratedDb = helper.runMigrationsAndValidate(DB_NAME, 12, true, Migration_011_to_012).
    //   5. Assertions через PRAGMA + SELECT по новым таблицам.

    companion object {
        private const val DB_NAME = "migration-test"
    }
}
```

**Key API points** (verified через library source во время prereq):
- `MigrationTestHelper(instrumentation, driver = BundledSQLiteDriver(), databaseClass = Database::class)` — KMP API Room 2.8.x.
- `helper.createDatabase(name, version)` — возвращает `SQLiteConnection`.
- `helper.runMigrationsAndValidate(name, version, validateDroppedTables, vararg migrations)` — запускает миграцию + validates против `12.json` schema snapshot.
- `connection.execSQL("...")` — через extension `androidx.sqlite.execSQL`.
- `connection.prepare("SELECT ...").use { stmt -> while(stmt.step()) { stmt.getText(0) ... } }` — для SELECT assertions (см. `BundledSqliteFeatureTest.kt` как reference).

---

## 2. Migration test cases

### A. Translation-only lexeme

**Setup (v11):**
- 1 dictionary (id=1, "EN").
- 1 word (id=1, dictionary_id=1, value="cat").
- 1 lexeme (id=1, word_id=1, translation="кошка", definition=NULL).

**Action:** migrate v11 → v12.

**Assert (v12):**
- `SELECT COUNT(*) FROM component_types WHERE system_key='translation'` → 1 (built-in seeded).
- `SELECT COUNT(*) FROM component_values WHERE lexeme_id=1` → 1.
- `SELECT value FROM component_values WHERE lexeme_id=1` → `{"v":1,"text":"кошка"}` (JSON).
- `SELECT component_type_id FROM component_values WHERE lexeme_id=1` равен `SELECT id FROM component_types WHERE system_key='translation'`.
- `SELECT COUNT(*) FROM pragma_table_info('lexemes') WHERE name IN ('translation','definition')` → 0 (колонки dropped).

### B. Translation + definition lexeme в одном словаре

**Setup (v11):**
- 1 dictionary (id=1).
- 2 words (id=1,2).
- 2 lexemes:
  - id=1, word_id=1, translation="кошка", definition="домашнее животное".
  - id=2, word_id=2, translation="собака", definition=NULL.

**Assert (v12):**
- 1 user-defined component_type `(dictionary_id=1, name='Definition', system_key=NULL, template_key='text')` создан.
- 3 component_values total: lexeme_id=1 имеет 2 (translation + Definition), lexeme_id=2 имеет 1 (translation).
- `SELECT value FROM component_values WHERE lexeme_id=1 AND component_type_id = (SELECT id FROM component_types WHERE system_key='translation')` → `{"v":1,"text":"кошка"}`.
- `SELECT value FROM component_values WHERE lexeme_id=1 AND component_type_id = (SELECT id FROM component_types WHERE name='Definition' AND dictionary_id=1)` → `{"v":1,"text":"домашнее животное"}`.

### C. Definition-only lexeme

**Setup (v11):**
- 1 dictionary (id=1).
- 1 word (id=1).
- 1 lexeme (id=1, translation=NULL, definition="что-то").

**Assert (v12):**
- 1 user-defined component_type `(dictionary_id=1, name='Definition')` создан.
- 1 component_value (lexeme_id=1, component_type_id=<Definition id>, value=`{"v":1,"text":"что-то"}`).
- НЕТ component_value для built-in translation у этой лексемы.
- `quiz_configs` для этого словаря содержит `[BuiltIn(TRANSLATION), UserDefined("Definition")]`.

### D. Empty dictionary (без лексем)

**Setup (v11):**
- 1 dictionary (id=1).
- 0 words, 0 lexemes.

**Assert (v12):** (F1 closed)
- `SELECT COUNT(*) FROM quiz_configs WHERE dictionary_id=1` → 1.
- `SELECT component_refs FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'` → `[{"type":"builtin","key":"translation"}]`.
- 0 component_values total.
- НЕ создаётся user-defined Definition type для этого словаря.

### E. Dictionary с definition → quiz_config содержит `[BuiltIn(TRANSLATION), UserDefined("Definition")]`

**Setup (v11):**
- 1 dictionary (id=1).
- 1 word, 1 lexeme с definition (non-null).

**Assert (v12):**
- `SELECT component_refs FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'` → должно содержать оба элемента в правильном порядке.
- Парсинг через `JSONArray`: `length() == 2`, `[0]` = `{"type":"builtin","key":"translation"}`, `[1]` = `{"type":"user","name":"Definition"}`.

### F. FK cascade chain (MIN-4) — delete dictionary → words → lexemes → component_values + quiz_configs

**Setup (v12 после миграции):**
- 1 dictionary (id=1), 1 word, 1 lexeme с translation + definition (2 component_values + 1 quiz_config + user-defined Definition type).
- Migrate v11 → v12 (помещает данные через миграцию).

**Action:** `connection.execSQL("DELETE FROM dictionaries WHERE id=1")` (после миграции через MigrationTestHelper.runMigrationsAndValidate, или открытие БД через Room после migration).

**Assert (cascade):**
- `SELECT COUNT(*) FROM words WHERE dictionary_id=1` → 0.
- `SELECT COUNT(*) FROM lexemes` → 0 (cascade через words.dictionary_id → lexemes.word_id).
- `SELECT COUNT(*) FROM component_values` → 0 (cascade через lexemes.id или через component_types.dictionary_id).
- `SELECT COUNT(*) FROM component_types WHERE dictionary_id=1` → 0 (cascade через dictionary_id FK; built-in с dictionary_id=NULL остаётся).
- `SELECT COUNT(*) FROM component_types WHERE system_key='translation'` → 1 (built-in не затронут).
- `SELECT COUNT(*) FROM quiz_configs WHERE dictionary_id=1` → 0.
- `SELECT COUNT(*) FROM write_quiz WHERE dictionary_id=1` → 0 (existing cascade через lexeme_id).

### G. Direct cascade `component_types → component_values` (MIN-10)

**Setup (v12 post-migration):**
- 1 dictionary, 1 lexeme с translation (1 component_value на built-in translation type).
- Дополнительно через прямой `connection.execSQL` создать user-defined `component_type(dictionary_id=1, name='TestType')` + 2 `component_values` ссылающихся на этот type (для разных lexemes).

**Action:** `DELETE FROM component_types WHERE id = <TestType id>`.

**Assert:**
- `SELECT COUNT(*) FROM component_values WHERE component_type_id = <TestType id>` → 0.
- Translation component_values остаются (другой component_type_id).

### H. AGG-8 verify: `json_insert($, '$[#]', json_object(...))` синтаксис

**Setup:** 1 dictionary с lexeme содержащим definition (стандартный кейс E).

**Assert (post-migration):**
- После migration step 7.3 — `SELECT component_refs FROM quiz_configs WHERE dictionary_id=1` парсится как valid JSON array с 2 элементами.
- Порядок элементов: `[0]` = builtin translation, `[1]` = user Definition. F4 invariant.

Эквивалентно с BundledSqliteFeatureTest.jsonInsertAppend_isSupported (smoke на синтаксис). Здесь — verify что фактическая Migration_011_to_012 step 7.3 даёт правильный output.

### I. Partial UNIQUE index exists

**Action:** post-migration `SELECT sql FROM sqlite_master WHERE name = 'index_component_types_global_userdef_name'`.

**Assert:**
- Returned non-null SQL содержит `WHERE dictionary_id IS NULL AND system_key IS NULL`.

### J. Orphan lexeme (translation IS NULL AND definition IS NULL в v11)

**Setup (v11):** 1 lexeme с обоими NULL колонками.

**Assert (v12):**
- `SELECT COUNT(*) FROM component_values WHERE lexeme_id=<id>` → 0.
- Lexeme сам по себе НЕ удалён — остался без component_values.

### K. Special chars в definition / translation (через json_object())

**Setup (v11):** lexeme с translation `she said "hello"`, definition `многострочная\nс эмодзи 😀`.

**Assert (v12):**
- `SELECT value FROM component_values WHERE lexeme_id=<id> AND component_type_id=<translation>` → JSON содержит правильно escaped string. Парсинг через `JSONObject(value).getString("text")` возвращает исходную строку `she said "hello"`.
- Аналогично для definition с переносом строки и эмодзи.

### L. UNIQUE constraint на (dictionary_id, quiz_mode)

**Setup:** post-migration попытка повторного INSERT `quiz_configs(dictionary_id=1, quiz_mode='write', ...)`.

**Assert:**
- Бросает SQLite UNIQUE constraint exception (через try/catch в тесте: `assertThrows<...> { connection.execSQL("INSERT INTO quiz_configs ...") }`).

### M. M3 — interrupted migration (idempotency)

Опциональный, harder-to-write. См. 05.md § Integration tests M3. Может быть отложен в post-implementation hardening, не блокирует IS481.

---

## 3. Integration tests (runtime DAO, post-migration)

Эти **НЕ migration tests** — отдельный test class `WordDaoQuizConfigIntegrationTest.kt` либо подобный. Запускаются на in-memory БД v12 (через Room builder, не через MigrationTestHelper).

### N. `addDictionary` auto-INSERT default quiz_config (F014)

**Path:** новый тест `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/AddDictionaryQuizConfigTest.kt`.

**Setup:** in-memory Room БД v12 (`Room.inMemoryDatabaseBuilder<Database>(...).setDriver(BundledSQLiteDriver()).build()`).

**Action:** `wordDao.addDictionary(DictionaryDb(name="Test", addDate=Date()))` — новый default-method с расширенной транзакцией.

**Assert:**
- `quizConfigDao.getByDictionaryAndMode(newId, "write")` non-null.
- Парсинг `component_refs` через `String.toComponentTypeRefList()` → `[ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)]`.

### O. `addDictionary` atomicity rollback (F015)

**Setup:** in-memory v12 + симуляция corrupt JSON / FK violation в `insertDefaultQuizConfig`.

**Action:** вызвать `wordDao.addDictionary(...)` где quiz_config INSERT падает (через mock либо инжектированную ошибку).

**Assert:**
- `wordDao.getDictionaries().isEmpty()` (либо count == 0 — никаких side effects).
- `quizConfigDao.getByDictionaryAndMode(...)` для несуществующего id — null.
- Транзакция Rollback'нула всё.

**Hard part:** надо как-то инжектировать ошибку. Опции:
1. Мокать `QuizConfigDao.insert` через test double и заставить throw.
2. Создать тест-double `Database` где `quizConfigDao()` возвращает faulty implementation.
3. Использовать reflection / spy на real DAO.

Решение по реализации — на этапе implement.

### P. `addLexemeWithComponents` atomicity rollback (existing IS479 F1 + MIN-9 regression test)

**Setup:** v12 БД с 1 dictionary, 1 word.

**Action:** `wordDao.addLexemeWithComponents(wordId=1, dictionaryId=1, components=listOf(99999L to "{...}"))` — где `99999L` — несуществующий componentTypeId.

**Assert (FK violation rollback):**
- `wordDao.getLexemeById(...)` — никаких новых строк в `lexemes`.
- `SELECT COUNT(*) FROM write_quiz` — без изменений.
- `SELECT COUNT(*) FROM component_values` — без изменений.

Регрессия IS479 F1 + MIN-9 atomicity.

---

## 4. Не покрыто в IS481 / отложено

- **M3 interrupted migration** (05.md § Integration tests) — сложный для реализации (нужен механизм injection failure mid-migration). Опционально, не блокирует.
- **Large dataset (10k+ lexemes)** — performance тест миграции. Не критичен — internal testers с малым dataset.
- **F6 cleanup `quiz_configs.component_refs` при DELETE component_type** — в IS481 операция `deleteComponentType` отсутствует (нет UI триггера). Тест на cleanup — backlog «Quiz config UX».
- **Concurrent INSERT на UNIQUE (dictionary_id, quiz_mode)** — race condition; SQLite ABORT on conflict. Defensive тест — не нужен (single-thread mutations через DAO).

---

## 5. Test execution

**Command:**
```bash
./gradlew :core:core-db-impl:connectedDebugAndroidTest
```

**Acceptance:** все кейсы A-L PASS. Кейсы M (M3) опционально. Integration N-P PASS — отдельный test class.

**Verify через MigrationTestHelper:**
- При каждом `runMigrationsAndValidate(...12, true, Migration_011_to_012)` — Room validation проверяет что схема после миграции **совпадает** с `12.json` snapshot. Если миграция создаёт таблицу с другим именем индекса / другим типом колонки — validate падает с явным diff.
- Имена индексов в Migration_011_to_012 ДОЛЖНЫ совпадать с тем что Room сгенерирует в `12.json` (см. data_walkthrough.md § 5).

---

## log_messages

- 12 migration test cases A-L + 3 integration runtime cases N-P; M3 (interrupted migration) опционально, не блокирует IS481
- harness: MigrationTestHelper(driver = BundledSQLiteDriver(), databaseClass = Database::class) — Room 2.8.x KMP API verified в prereq; runMigrationsAndValidate validates against 12.json snapshot
- F014/F015 переcategorized как runtime integration (не migration) — addDictionary auto-INSERT default quiz_config + atomicity rollback тестируются на in-memory v12 БД, не через MigrationTestHelper

_model: claude-opus-4-7[1m]_
