# scope_analysis review

## Итерация 1 (2026-06-04T04:55:00-06:00)

### F001 [architect] critical

**Description:** `DatasourceEffectHandler` отнесён к `app/` (секция файлов app), хотя реально лежит в `modules/screen/wordcard/mate/`. `WordCardUseCaseImpl` упомянут под `modules/screen/wordcard/` (строка 31) И под `app/` (строка 34) — единственное реальное расположение app/. Имплементор пойдёт по неверному пути.

**Status:** approved

**Verdict:** DatasourceEffectHandler lives in modules/screen/wordcard/mate/, not app/ — scope ошибочно отнёс его к app/ слою.

### F002 [architect] critical

**Description:** `QuizChatUseCaseImpl.fetchData` указан в подсекции `modules/screen/quiz/chat/`, но реально файл `QuizChatUseCaseImpl.kt` лежит в `app/.../di/module/quizchat/`. В модуле только интерфейс `QuizChatUseCase.kt` (deps).

**Status:** approved

**Verdict:** QuizChatUseCaseImpl реально лежит в app/.../di/module/quizchat/, а scope упоминает его под modules/screen/quiz/chat/ — путаница между интерфейсом и реализацией.

### F003 [architect] critical

**Description:** `RoomModule.provideDatabase` отнесён к `app/`, но реально находится в `core/core-db-impl/.../di/module/RoomModule.kt`. AGG-12 говорит «добавить `.addMigrations(Migration_011_to_012)` к existing builder» — место правки именно `core-db-impl`, не app.

**Status:** approved

**Verdict:** RoomModule.provideDatabase живёт в core/core-db-impl, не в app/ — scope ошибочно указал app/ как location.

### F004 [architect] critical

**Description:** `DictionaryDao.addDictionary` — класс `DictionaryDao` не существует. Метод `addDictionary` живёт в `WordDao` (`core_db_impl/room/WordDao.kt:29`). AGG-4 транзакция расширяется в `WordDao`, не в несуществующем DAO.

**Status:** approved

**Verdict:** Класс DictionaryDao не существует, метод addDictionary реально в WordDao.kt.

### F005 [architect] minor

**Description:** Артефакт ссылается на `core_db_impl/room/dao/` как location для новых DAO (через 02_design_sketch.md), но в проекте DAO лежат в `core_db_impl/room/` напрямую (`WordDao.kt`). Конвенция нарушена без обоснования.

**Status:** rejected

**Verdict:** В артефакте 02_scope.md нет упоминания `core_db_impl/room/dao/` — это claim про 02_design_sketch.md, не про scope, ревьюер перепутал артефакты.

### F006 [architect] minor

**Description:** `context_output` переменная `spec_filename` (требуется по `scope_analysis.md:170`) не выведена явно в финальной секции артефакта блоком всех context vars. Conductor должен получить значения из ответа агента в секции `## context`, а не парсить из тела `02_scope.md`.

**Status:** approved

**Verdict:** В артефакте отсутствует явный блок context_output со всеми 8 переменными включая spec_filename, как требует frontmatter шага.

### F007 [qa_engineer] critical

**Description:** Аспект `db_migration` не указан явно в секции «Аспекты». По правилу `scope_analysis.md:86` он стандартный аспект для миграционных фич — без явного флага downstream data_walkthrough может пропустить migration-specific checks.

**Status:** approved

**Verdict:** Секция «Аспекты» содержит только context_output флаги, тогда как scope_analysis.md явно требует аспекты типа `db_migration` для миграционных фич.

### F008 [qa_engineer] critical

**Description:** Edge case «M3 — interrupted migration restart / idempotency» (из `05_migration_strategy.md:99-105`) не упомянут в scope как обязательный migration test. Сценарий «батарея села во время миграции» — критичный для UX, при пропуске даст потерю данных пользователю.

**Status:** rejected

**Verdict:** Idempotency restart явно адресован в MIN-12c alignment-решении (Room оборачивает миграцию в транзакцию, defensive отклонён) — требование противоречит принятому решению.

### F009 [qa_engineer] minor

**Description:** Edge case «orphan lexeme» (translation IS NULL AND definition IS NULL, `05.md` case 8 + test 14) не упомянут в migration tests. Отдельный кейс с count-инвариантом — без явного упоминания пропустим в data_migration_test.

**Status:** rejected

**Verdict:** Orphan lexeme уже упомянут в needs_tests артефакта (line 20) и относится к mapper, а не migration tests — сценарий покрыт.

### F010 [qa_engineer] minor

**Description:** Edge cases спецсимволов / эмодзи / >64KB definition (`05.md` tests 5-8) не упомянуты в `needs_tests`. JSON-escape под bundled SQLite через `json_object()` — новый код-путь, отдельная категория тестов.

**Status:** rejected

**Verdict:** Спецсимволы / эмодзи / size-edge cases — это sub-flow level детализация, не уровень scope_analysis по правилу «не дублируй research который sub-flow сделают сами».

### F011 [qa_engineer] minor

**Description:** UI-тесты для wordcard (chip visibility по `hasDefinitionComponent`) не выделены отдельной категорией в `needs_tests`. Указан Reducer-тест на флаг, но Composable-видимость chip — observable behavior; либо явно отнести к Reducer (если state derives chip), либо указать что UI-test не нужен с обоснованием.

**Status:** rejected

**Verdict:** UI compose-тесты на chip visibility — это глубокая проработка тестов, sub-flow concern, scope_analysis классифицирует «затронут / не затронут» без выделения категорий тестов.

### F012 [qa_engineer] minor

**Description:** Тест «AGG-8 verify `json_insert($, '$[#]', ...)`» упомянут в scope как verify, но не уточнено что это **prerequisite** перед миграцией (`05.md` строка 54 после фикса). Без явной формулировки «verify ДО реализации миграции» data sub-flow может узнать о fallback (Kotlin-сборка JSON) только при сбое.

**Status:** rejected

**Verdict:** Прерогатива «prerequisite vs verify» — sub-flow level детализация порядка работ, не задача scope_analysis.

### F013 [qa_engineer] minor

**Description:** Atomicity rollback тест для `addLexemeWithBuiltInComponent` (FK violation → rollback, `05.md` строка 79) не упомянут в `needs_tests`. Отдельный integration test регрессирующий фикс IS479 F1 при пропуске.

**Status:** approved

**Verdict:** Atomicity rollback тест явно зафиксирован в alignment Test gaps batch как обязательный («продублировать в § Тестирование 06»), а в needs_tests артефакта отсутствует.
