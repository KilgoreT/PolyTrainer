# Review: 03_database_design.md

Параллельный обзор дизайна БД для IS481 — 5 subagent'ов по разным направлениям. Каждый finding содержит `Verify:` строку через встроенные Claude tools (`Grep` / `Glob` / `Read`), без shell-команд.

## ✅ Автозакрытые findings (через решения из `04_builtin_strategy.md`)

После полного triage `04_builtin_strategy_review.md` следующие findings закрываются производными решениями:

**Закрытые через дизайн 04:**
- Architecture #1 (column-naming `lexemeId / lexeme_id`) — → backlog (в `docs/Backlog.md`).
- Architecture #2 (built-in id хардкод) — заменён на `system_key`.
- Architecture #3 (dictionary_id nullable scope) — две независимые оси + partial UNIQUE.
- Architecture #4 (FK CASCADE race) — описано в § «Открытые вопросы» п.3.
- Architecture #5 (templateKey без TypeConverter) — теперь без TypeConverter (Room хранит String).
- Architecture #6 (soft-delete pattern) — теперь `remove_date: Date?` (HintDb/SampleDb-стиль).
- Naming #1 (`is_system` нарушение R-N-007) — заменён на `system_key`.
- Naming #2 (`lexemeId / lexeme_id`) — → backlog.
- Naming #8 (built-in name prefix collision) — закрыто `name = NULL` для built-in.
- Naming #10 (`sort_order` vs `position`) — `position` в чеклисте.
- Performance #1 (избыточный `INDEX(lexeme_id)`) — в чеклисте «удалить».
- Migration #6 (built-in id idempotency) — закрыто через `system_key` + INSERT OR IGNORE.
- Migration #7 (UNIQUE NULL collision) — закрыто partial UNIQUE индексом.
- Migration #10 (backward compat / downgrade) — → rejected (см. Edge #7 в `04_builtin_strategy_review.md`).
- YAGNI всё (soft-delete / JSON payload / sort_order / UNIQUE) — отклонено: остаётся в дизайне как необходимое для built-in защиты / расширяемости / partial UNIQUE.

Эти findings помечены 🔒 / ❌ в соответствующих секциях ниже без отдельного развёрнутого triage.

---

## Сводка по направлениям

| Направление | Critical | Minor |
|---|---|---|
| Architecture | 4 | 2 |
| Performance | 3 | 3 |
| Naming | 5 | 5 |
| Migration risk | 6 | 5 |
| YAGNI | 2 | 4 |
| **Итого** | **20** | **19** |

---

## Architecture Review

### 🔒 [critical] Несогласованный column-naming в @Relation между SampleDb и ComponentValueDb
**Где:** § 5, `LexemeDbEntity` — `@Relation(... entityColumn = "lexeme_id")` для componentValueDbList vs `entityColumn = "lexemeId"` для sampleDbList.
**Что не так:** SampleDb / HintDb / WriteQuizDb используют разные конвенции колонок (`lexemeId` camelCase у Sample/Hint, `lexeme_id` snake_case у WriteQuiz). Дизайн ComponentValueDb берёт snake_case (`lexeme_id`), но в `LexemeDbEntity` уже стоит `entityColumn = "lexemeId"` для samples. Полученный @Relation смешивает обе конвенции в одном классе.
**Почему важно:** При будущем рефакторинге Sample/Hint на snake_case надо помнить, что LexemeDbEntity ссылается на конкретное имя колонки.
**Предложение:** Оставить `lexeme_id` в ComponentValueDb. В backlog добавить «привести SampleDb/HintDb columns к snake_case». В § 5 явно отметить, что разнобой осознан и временный.
**Verify:** Read `core/core-db-impl/.../SampleDb.kt` → нет `@ColumnInfo`, Kotlin-имя `lexemeId`. Read `LexemeDbEntity.kt:13` → `entityColumn = "lexemeId"`. Read `WriteQuizDb.kt` → `@ColumnInfo(name = "lexeme_id")`.
**Triage:** → backlog. В IS481 оставляем разнобой с TODO-комментарием в `LexemeDbEntity`. Отдельная фича — миграция SampleDb / HintDb на snake_case (записано в `docs/Backlog.md`). Создан `docs/guides/naming.md` с R-N-002 (snake_case через `@ColumnInfo`).

### 🔒 [critical] Built-in типы с хардкоженными id=1,2 — нарушение слоя
**Где:** § 7 шаг «INSERT built-in типов», § 9 риск 2; design_sketch § 3 `BuiltInTypes.TRANSLATION_ID = 1`.
**Что не так:** Захардкоженные id=1,2 в миграции SQL + константы `BuiltInTypes.TRANSLATION_ID` в domain-слое — связь по магическому числу между миграцией Room и domain-кодом. Domain знает, что translation = 1, потому что миграция так положила.
**Почему важно:** (1) При смене id придётся синхронно править domain. (2) Тестируемость: domain-тесты должны знать про id=1,2. (3) Документ сам пишет «risk-point», не закрывая.
**Предложение:** Заменить захардкоженные числовые id на **семантический ключ** — добавить в `component_types` колонку `system_key: TEXT NULLABLE UNIQUE` (для built-in = `"translation"` / `"definition"`, для user-defined = NULL). Domain ищет built-in по `system_key`, не по числовому id.
**Verify:** Read `03_database_design.md` § 7 строки 467-474 → захардкоженные `(1, NULL, 'translation', ...)`, `(2, NULL, 'definition', ...)`. Read `02_design_sketch.md:239` → `BuiltInTypes.TRANSLATION_ID` / `DEFINITION_ID` = `ComponentTypeId(1)` / `ComponentTypeId(2)`.
**Triage:** → закрыть в дизайне. Добавить колонку `system_key: TEXT UNIQUE NULLABLE` в `component_types`. Built-in: `system_key = "translation"` / `"definition"`. User-defined: `system_key = NULL`. id остаётся autoincrement (не хардкодится). Domain ищет built-in через `dao.getByKey(key: String)`, не по числовому id. Колонка `is_system` **убирается совсем** — становится computed `val ComponentType.isBuiltIn get() = systemKey != null`. UNIQUE на `system_key` даёт реальную защиту от дублей built-in (там либо строка либо NULL, без NULL-collision как у `(dictionary_id, name)`).

### [critical] `dictionary_id` nullable как scope-маркер замазывает архитектуру
**Где:** § 3 `component_types.dictionary_id INTEGER NULL = global`; research § 1 «Mixed scope».
**Что не так:** `dictionary_id IS NULL` означает «global scope». Semantic overload: одна колонка несёт две роли — FK на dictionary И индикатор scope'а. UNIQUE(dictionary_id, name) при NULL опасно — SQLite не считает NULL равными (§ 7 edge case 4 — документ это признаёт).
**Почему важно:** (1) UNIQUE constraint на (NULL, name) **не предотвращает дубли** — два global типа с именем `"translation"` пройдут. Защита «на уровне приложения» **обходит назначение constraint'а** и вводит race condition. (2) Расширение на user/group scope потребует миграции колонок.
**Предложение:** Ввести explicit колонку `scope: TEXT NOT NULL DEFAULT 'DICTIONARY'` (`'SYSTEM'` / `'DICTIONARY'` для старта, расширяемо до `'USER'`, `'GROUP'`). UNIQUE INDEX над `(scope, COALESCE(dictionary_id, -1), name)` — реально предотвращает дубли.
**Verify:** Read `03_database_design.md:531` (edge case 4). Read `01_research.md:26-34` → автор сам отмечает что nullable-подход «не финальное решение».

### [critical] FK CASCADE на удаление dictionary — race в каскадах
**Где:** § 5 `ComponentTypeDb` `ForeignKey(... onDelete = CASCADE)`.
**Что не так:** При удалении dictionary каскадом удалятся `component_types` этого словаря → каскадом удалятся `component_values` лексем словаря. Но `lexemes` каскадом удаляются ОТДЕЛЬНО по FK от `words.dictionary_id`. **Race в каскадах**: какой путь сработает первым? SQLite это обрабатывает, но семантическая запутанность приведёт к багам при отладке.
**Почему важно:** При удалении словаря component_values могут удаляться **дважды-каскадом** через разные пути.
**Предложение:** Явно описать в § 2 (ER-диаграмма) и § 9 (риски) сценарий удаления dictionary. Добавить миграционный тест на удаление dictionary с user-defined типами и значениями.
**Verify:** Read `WordDb.kt` → `dictionary_id` FK с CASCADE. Read `03_database_design.md:171-174, 207-218` → ComponentTypeDb / ComponentValueDb FK CASCADE.

### [minor] `templateKey: String` вместо TypeConverter
**Где:** § 5 `ComponentTypeDb.templateKey: String`.
**Что не так:** ComponentType держит `templateKey: String`, в коде маппится через `ComponentTemplate.fromKey()`. Альтернатива — Room TypeConverter `ComponentTemplate ↔ String`.
**Почему важно:** Маппер должен делать `ComponentTemplate.fromKey(templateKey) ?: error/fallback`. Лишний failure mode в каждом чтении.
**Предложение:** Использовать @TypeConverter для ComponentTemplate. Unknown key обрабатывается в одном месте. Согласовано с `DateTimeConverter`.
**Verify:** Read `core/core-db-impl/.../converters/DateTimeConverter.kt` → проект использует TypeConverter для Date. Read `03_database_design.md:185` → `templateKey: String` без converter.

### [minor] Soft-delete pattern несогласован с HintDb/SampleDb
**Где:** § 5 ComponentTypeDb `deleted: Boolean + deleted_at: Date?`.
**Что не так:** SampleDb/HintDb используют `removeDate: Date? = null` (NULL = не удалён) — single field. ComponentType вводит два поля, что избыточно (`deletedAt != null` ⇔ `deleted = true`).
**Почему важно:** Расхождение паттернов. Лишнее поле — потенциальный источник рассинхрона (`deleted = true` + `deletedAt = null` — невалидное состояние).
**Предложение:** Привести к существующему паттерну: одна колонка `removed_date: Date?`. Запросы: `WHERE removed_date IS NULL`. Cleanup: `WHERE removed_date IS NOT NULL AND removed_date < :threshold`.
**Verify:** Read `HintDb.kt:14` → `removeDate: Date? = null`. Read `SampleDb.kt:18` → `removeDate: Date? = null`.

---

## Performance Review

### [critical] Избыточный `INDEX(lexeme_id)` дублирует UNIQUE(lexeme_id, component_type_id)
**Где:** § 3, § 5, § 7 — три индекса в component_values.
**Что не так:** Композитный UNIQUE по `(lexeme_id, component_type_id)` уже полностью покрывает запросы фильтра по leading column `lexeme_id`. Отдельный `INDEX(lexeme_id)` — мёртвый: занимает место, тормозит INSERT/UPDATE/DELETE.
**Почему важно:** Запись компонента — hot path. Лишний индекс = ~30-50% дополнительной работы на write.
**Предложение:** Удалить `Index("lexeme_id")` — оставить `Index("component_type_id")` (для `countValuesForType`) и `Index(unique = true)`. Leading-column prefix покрывает запросы.
**Verify:** Read `03_database_design.md:112-115, 220-224, 462-464`.

### 🔒 [critical] N+1 при чтении лексем: @Relation загружает ComponentValue, но не ComponentType
**Где:** § 5 `LexemeDbEntity`, § 9 mention о маппере.
**Что не так:** @Relation на `List<ComponentValueDb>`, но `ComponentTypeDb` (с templateKey) не загружается. Маппер `toApiEntity()` делает «дополнительный запрос или JOIN» — это **N+1**: для каждого ComponentValue — отдельный лукап ComponentType.
**Почему важно:** На DictionaryTab при 1000 слов × 1-2 лексемы × 2 компонента это **4000 дополнительных DB-запросов**.
**Предложение:** Один из вариантов: (1) кэш `Map<ComponentTypeId, ComponentTypeDb>` (типов мало). (2) Multi-level @Relation через Room. (3) Денормализация `template_key` в `component_values` (read-optimized, write-cheap т.к. типы редко меняются).
**Verify:** Read `TermDbEntity.kt` → lexemes уже грузятся через nested @Relation, добавление ещё одного уровня без решения — узкое место.
**Triage:** → закрыто. Используем **Multi-level @Relation в Room** (проверенный паттерн проекта — TermDbEntity → LexemeDbEntity → SampleDb уже 3 уровня). Промежуточный `ComponentValueWithType` (`@Embedded` ComponentValueDb + `@Relation` ComponentTypeDb). `LexemeDbEntity` ссылается через `entity = ComponentValueDb::class`. Room генерирует 3 batched SELECT'a (lexemes / component_values / component_types) независимо от N — N+1 не возникает. Никакого кэша / денормализации. Записать в чеклист 04 при следующем обновлении.

### 🔒 [critical] Миграция payload через SQL string concatenation — медленно и небезопасно на больших dataset
**Где:** § 7 миграция.
**Что не так:** SQL string concat ломается на `\n`, `\r`, control chars.
**Triage:** → раскрыто в [`05_migration_strategy.md`](05_migration_strategy.md). Скоуп миграции пересмотрен: translation → built-in, definition → user-defined per-dictionary. JSON-escape проблема — отдельный TBD в `05` с 3 вариантами решения (SQL REPLACE / Kotlin Cursor / без JSON-обёртки для text). Финальное решение — перед реализацией миграции. Транзакция (`db.beginTransaction`) обязательна в любом варианте.

### 🔒 [minor] Recreate lexemes на больших таблицах + риск имени индекса
**Где:** § 7.
**Что не так:** Recreate-таблица медленный, имя индекса должно совпасть.
**Triage:** → закрыто через подключение **bundled SQLite** (см. `05_migration_strategy.md` § «Bundled SQLite — почему»). С bundled SQLite 3.45+ доступен `ALTER TABLE DROP COLUMN` — recreate не нужен. Транзакция вокруг миграции + проверка имён индексов через `12.json` snapshot — остаются в чеклисте `05`.

### ❌ [minor] `countValuesForType` без LIMIT — full scan для UI confirm-диалога
**Где:** § 6 DAO.
**Triage:** → rejected. Сам метод `countValuesForType` лишний (built-in не удаляются, UI удаления user-defined вне скоупа). Метод удалить из DAO в `03_database_design.md` при обновлении.

### ❌ [minor] Cleanup soft-deleted — нет индекса на `deleted_at`
**Где:** § 3 индексы.
**Что не так:** `DELETE FROM component_types WHERE deleted = 1 AND deleted_at < :threshold` — full scan.
**Почему важно:** Таблица маленькая (десятки строк) — finding minor.
**Предложение:** Не добавлять индекс. Cleanup при app start, не каждый запуск.

---

## Naming Review

### [critical] `is_system` / `isSystem` — неточная семантика
**Где:** § 3, § 5, § 9 (открытый вопрос).
**Что не так:** Семантика по дизайну — «нельзя удалить / переименовать, мигрирован как built-in». «System» в Android-коде уже занято (SystemBars, System resources).
**Почему важно:** В research § 9 автор сам не уверен между `is_system` / `built_in` / `system`. По смыслу built-in типы — встроенные в код, не системные.
**Предложение:** `built_in` (column) / `builtIn: Boolean` (Kotlin).
**Verify:** Read `03_database_design.md` § 9 п.3 → автор сам использует «built-in» в речи, а в коде `is_system`.

### [critical] `payload` vs `value` — расходится с конвенцией проекта
**Где:** § 3, § 5.
**Что не так:** В проекте текстовые поля сущностей — `value`: `WordDb.value`, `SampleDb.value`, `HintDb.value`. `payload` — внешний термин.
**Почему важно:** Таблица `component_values` — колонка с самим значением должна быть `value`. JSON — деталь хранения.
**Предложение:** `value: String` (с `@ColumnInfo(name = "value")`).
**Verify:** Read `WordDb.kt:27`, `SampleDb.kt:14`, `HintDb.kt:12` → все используют `value`.

### [critical] `lexemeId` vs `lexeme_id` в `@Relation` (дубль architecture)
**Где:** § 5, `LexemeDbEntity`.
**Что не так:** Внутри одного класса два разных стиля FK на lexeme. См. architecture finding выше.
**Предложение:** Добавить TODO-комментарий о причине разнобоя, чтобы не «выровняли» в неправильную сторону.

### [critical] Стиль значений `template_key`: `"text"` vs `"TEXT"`
**Где:** § 4, § 7.
**Что не так:** Дизайн использует lowercase, не объясняя.
**Почему важно:** В проекте `word_class` хранит `"noun"` / `"verb"` — lowercase. Без явной ссылки на конвенцию кто-то на code-review предложит UPPER.
**Предложение:** Добавить комментарий «consistent with `lexemes.word_class` values».
**Verify:** Read `DefinitionMapper.kt:32-35` → `NOUN = "noun"; VERB = "verb"; ...`.

### [critical] `ComponentValueData.Text` — конфликт с `androidx.compose.material3.Text`
**Где:** § 4 / `02_design_sketch.md`.
**Что не так:** Класс `Text` с полем `text: String`. Reader видит `value.text.text` — два уровня. Конфликт импорта с Compose.
**Предложение:** Переименовать → `TextValue` / `LongTextValue` / `ImageValue`. Поле — `text: String` / `uri: String`.
**Verify:** Read `02_design_sketch.md:286` → State уже использует `TextValueState` (с суффиксом `Value`). Inconsistency между API entity и State.

### [minor] Имя таблицы — отсутствует namespace
**Где:** § 1, § 3.
**Что не так:** `component_types` / `component_values` — слишком общие. Не указывают что речь о лексеме.
**Предложение:** `lexeme_component_types` / `lexeme_component_values`.

### [minor] `template_key` — избыточный суффикс
**Где:** § 3, § 5.
**Что не так:** В проекте `word_class` хранит ключ enum'а без суффикса `_key`.
**Предложение:** `template: String` (колонка `template`).
**Verify:** Read `LexemeDb.kt:28` → `word_class`, не `word_class_key`.

### [minor] Built-in name `'translation'` без префикса — риск коллизии
**Где:** § 7, § 9 п.3.
**Что не так:** Built-in name = `"translation"` / `"definition"` — те же что user-defined могут задать.
**Предложение:** Reserved prefix: `"@translation"` (как Android resources `@string/...`). Или отдельная колонка `name_resource_key: String?`.

### [minor] DAO method naming — `observe*` vs `flow*`
**Где:** § 6.
**Что не так:** В проекте Flow-методы именуются `flow*` (`flowDictionaries`, `flowWordCount`).
**Предложение:** `flowTypesForDictionary` вместо `observeTypesForDictionary`.
**Verify:** Read `WordDao.kt:46-47, 222-238` → префикс `flow*` установлен.

### [minor] `sort_order` vs `position`
**Где:** § 3.
**Что не так:** Длиннее аналогов, пахнет SQL-терминологией.
**Предложение:** `position: Int`.

---

## Migration Risk Review

### [critical] JSON-escape SQL не покрывает все обязательные для JSON символы
**Где:** § 7 шаги 4-5, миграция строки 477-490.
**Что не так:** RFC 8259 требует экранировать `"`, `\`, и все control characters U+0000..U+001F. Текущие REPLACE покрывают только `"` и `\`. Любая лексема с `\n` в definition сгенерирует невалидный JSON → kotlinx.serialization падает при чтении → краш UI.
**Почему важно:** **Тихая потеря данных + краш UI после установки v12**. Definition исторически свободный текст — пользователи туда вставляли многострочный.
**Предложение:** Миграция через Cursor + kotlinx.serialization (вариант из § 9 п.1). Или минимум — REPLACE для `\b`, `\f`, `\n`, `\r`, `\t` в правильном порядке (сначала `\` → `\\`).
**Verify:** Read `03_database_design.md:477-490` → REPLACE покрывает только `\` и `"`.

### [critical] Edge case формулировка «могут понадобиться» — занижает риск
**Где:** § 7 edge case 2, § 9 п.1.
**Что не так:** «Могут понадобиться» — это блокер, а не open question. Definition в существующей БД с очень высокой вероятностью содержит `\n`.
**Предложение:** Переименовать в «решение требуется до реализации». Добавить комментарий «order matters: \\ MUST be first».

### [critical] Несовпадение схемы lexemes с identityHash Room (options DEFAULT 0)
**Где:** § 7 строки 494-503.
**Что не так:** `lexemes_new` создаётся с `options INTEGER NOT NULL DEFAULT 0`. Schemas/11.json и Entity LexemeDb — БЕЗ `DEFAULT 0`. После RENAME реальная схема содержит `DEFAULT 0`, Room валидация падает с `Migration didn't properly handle: lexemes`.
**Почему важно:** Падение на старте у всех пользователей после миграции.
**Предложение:** Убрать `DEFAULT 0` из `lexemes_new`. Или добавить `defaultValue = "0"` в `@ColumnInfo` LexemeDb (изменение модели, out of scope).
**Verify:** Read `schemas/.../11.json:247` → `"options" INTEGER NOT NULL` без default. Read `LexemeDb.kt:29` → Kotlin default `= 0` не транслируется в SQL DEFAULT без явного `defaultValue`.

### [critical] write_quiz имеет реальный FK на lexemes — DROP TABLE lexemes
**Где:** § 7 шаги 11-12.
**Что не так:** `WriteQuizDb` объявляет FK на lexemes CASCADE. Room по умолчанию выключает `foreign_keys=OFF` в migrate(), но в документе нет явного assertion'а. Если миграция запустится вне Room (тестовый контекст через raw SupportSQLiteDatabase), DROP TABLE упадёт.
**Предложение:** В начало `migrate()` добавить `db.execSQL("PRAGMA foreign_keys = OFF")` для defensive defense. Зафиксировать в комментарии.
**Verify:** Read `WriteQuizDb.kt:18-22` → FK с CASCADE на lexemes. В коде миграций проекта нет явного `PRAGMA foreign_keys`.

### [critical] AUTOINCREMENT sequence для lexemes теряется после RENAME
**Где:** § 7 шаги 8-12.
**Что не так:** `sqlite_sequence` хранит счётчик AUTOINCREMENT. После DROP + INSERT с явными id + RENAME — sequence сдвинется до max(id) в lexemes_new. Если в исходной таблице max использованный id был 1000, а реально max(id) = 800 (200 удалены) — после миграции 800. Следующий INSERT получит id=801, которое раньше использовалось → orphan ссылки могут «воскреснуть».
**Почему важно:** Edge case с восстановлением «удалённых» примеров/хинтов на новой лексеме. SampleDb/HintDb **не имеют FK CASCADE** — orphan-записи возможны.
**Предложение:** Сохранить max(id) ДО DROP, в конце `UPDATE sqlite_sequence SET seq = ? WHERE name = 'lexemes'`.
**Verify:** Read `SampleDb.kt:9-19` → нет foreignKeys. Read `HintDb.kt:7-16` → то же.

### [critical] Built-in id=1,2 хардкод — INSERT не идемпотентен
**Где:** § 7 шаг 5.
**Что не так:** `INSERT INTO component_types (id, ...) VALUES (1, ...)`. Если миграция запустится повторно — `UNIQUE constraint failed: component_types.id` (PK collision).
**Предложение:** `INSERT OR IGNORE` или `INSERT ... WHERE NOT EXISTS`.

### [minor] UNIQUE(dictionary_id, name) с NULL — защита только на уровне приложения
**Где:** § 3, § 7 edge case 4, § 9 п.3.
**Предложение:** Принять как known limitation. В тесте миграции добавить кейс: «после миграции существует ровно одна row с (NULL, 'translation') и одна с (NULL, 'definition')».

### [minor] MigrationTestHelper coverage — недостаточно edge cases
**Где:** § 8.
**Что пропущено:** Очень длинные тексты, surrogate pairs/emoji вне BMP, текст с подстрокой ` ` как литерал, начинающийся пробелом/таб/CR, lexemes без word'а (orphan), idempotency, большое количество лексем.
**Предложение:** Расширить § 8 явным списком 5-6 параметризованных кейсов.

### [minor] Стилистическая несовместимость с существующими миграциями
**Где:** § 7 `object Migration11To12 : Migration(11, 12)`.
**Что не так:** В проекте — `val migration_X_Y = object : Migration(X, Y)` (snake_case + lowercase val).
**Предложение:** `val migration_11_12 = object : Migration(11, 12) { ... }`. Файл `Migration_011_to_012.kt`.
**Verify:** Файлы `Migration_001_to_002.kt` ... `Migration_010_to_011.kt`. Read `RoomModule.kt:39-48` → `migration_1_2, ..., migration_10_11`.

### [minor] Backward compatibility — необратимая миграция, тестовые сборки
**Где:** § 9 п.5.
**Что не так:** При тестировании v12, откат на v11 → Room падает.
**Предложение:** Добавить предупреждение QA «wipe app data перед откатом на v11». Убедиться что нет `fallbackToDestructiveMigrationOnDowngrade()`.
**Verify:** Read `RoomModule.kt:36-50` → нет fallback. Данные сохранятся, но fail-fast на downgrade.

### [minor] Room verification — schema mismatch на FK ON UPDATE
**Где:** § 7.
**Что не так:** schemas/11.json имеет `"onUpdate": "NO ACTION"` явно. Код миграции — только `ON DELETE CASCADE`. SQLite default — NO ACTION, должно пройти, но без гарантии для future Room versions.
**Предложение:** Добавить `ON UPDATE NO ACTION` в DDL миграции.

---

## YAGNI Review

### [critical] Soft-delete фабрика (`deleted`, `deleted_at`, `softDelete`, `cleanupSoftDeleted`)
**Где:** § 3, § 5, § 6, § 7.
**Что излишне:** Колонки `deleted`, `deleted_at`; DAO `softDelete()` / `cleanupSoftDeleted()`; фильтр `AND deleted = 0`; retention period (§ 9.4 — даже не определён).
**Почему сейчас не нужно:** UI создания / редактирования / удаления user-defined типов не реализуется (§ 8 research). Built-in нельзя удалять. Soft-delete механика не вызывается ни одной строкой production-кода.
**Предложение:** Убрать всё. Добавлять вместе с UI удаления в отдельной итерации (миграция «добавить nullable колонку» тривиальна).
**Verify:** Read `01_research.md:174-179` → soft-delete описан для user-defined типов которых на старте нет.

### [critical] JSON `payload` для `text` шаблона
**Где:** § 3, § 4, § 7.
**Что излишне:** Хранение `{"text":"слово"}` вместо просто `слово`. SQL-escaping `REPLACE(REPLACE(...))` — risk-point.
**Почему сейчас не нужно:** На старте payload = одна String. JSON-обёртка добавляет parse-overhead + миграционный риск со spec-символами. LongText/Image не активны.
**Предложение:** Хранить `payload: String` raw text. Sealed `ComponentValueData` остаётся в Kotlin, `Text.text` ↔ `payload` — identity-маппинг. Миграция тривиальна: `INSERT ... SELECT id, 1, translation FROM lexemes WHERE translation IS NOT NULL`.
**Verify:** Read `03_database_design.md:477-490` → проблемный `REPLACE` с TODO.

### [minor] `sort_order` колонка
**Где:** § 3, § 5.
**Что излишне:** Колонка `sort_order: Int` + ORDER BY.
**Почему сейчас не нужно:** 2 built-in с фиксированным порядком по id (1=translation, 2=definition).
**Предложение:** Убрать. ORDER BY id ASC. Вернуть с user-defined типами.

### [minor] `UNIQUE INDEX(dictionary_id, name)`
**Где:** § 3, § 7.
**Что излишне:** UNIQUE constraint.
**Почему сейчас не нужно:** На старте 2 системных записи, никаких INSERT'ов кроме миграции. UNIQUE для NULL **не работает** (§ 9 риск 3 — сам автор отмечает).
**Предложение:** Убрать. Оставить только `INDEX(dictionary_id)`.

### [minor] `countValuesForType` DAO
**Где:** § 6.
**Что излишне:** Метод + отдельный `INDEX(component_type_id)`.
**Почему сейчас не нужно:** Удаления нет → confirm-диалог не существует.
**Предложение:** Убрать. Композитный UNIQUE остаётся (для `getForLexemeAndType`).

### [minor] `getTypesForDictionary` / `observeTypesForDictionary`
**Где:** § 6.
**Что излишне:** Оба метода + параметр `dictionary_id` в SQL.
**Почему сейчас не нужно:** Все типы — built-in global. `getSystemTypes()` даёт ровно то что нужно.
**Предложение:** Оставить только `getSystemTypes()` + `observeSystemTypes()` (для Flow в UI). Параметр `dictionaryId` прячет факт что в первой итерации он игнорируется.

### [keep] `dictionary_id` колонка nullable
**Почему оставить:** Корневая часть фичи (scope-объявление). Overhead копеечный.

### [keep] `is_system` колонка
**Почему оставить:** Корневой инвариант модели — отличить built-in от будущего user-defined.

### [keep] Sealed `ComponentTemplate` со всеми 3 вариантами
**Почему оставить:** Sealed-расширяемость = база фичи. Цена объявления = ноль.

### [keep] `ComponentTypeDao` как отдельный DAO
**Почему оставить:** Нужен `getSystemTypes()` для загрузки в State. Таблица обязательна.

---

## ✅ Массовое закрытие оставшихся минорных / автоматических

Помимо findings помеченных индивидуально, следующие закрываются массово:

### Naming (массовое)

- ❌ **Имя таблицы — namespace** (`lexeme_component_types`) — стилистика; `component_types` достаточно: проект не имеет других «components». Rejected.
- ❌ **`template_key` суффикс** — оставляем `template_key` (явно показывает что это ключ-строка для маппинга в enum). Rejected.
- ✅ **Built-in name prefix `'@translation'`** — автозакрыто (`name = NULL` для built-in без override; коллизий нет).
- ✅ **DAO `observe*` vs `flow*`** — приняли `flowTypesForDictionary` (есть в чеклисте 04 / соответствует конвенции `WordDao`).
- ✅ **`sort_order` vs `position`** — приняли `position` (в чеклисте 04).
- ✅ **`is_system`** — заменён на `system_key`.
- ✅ **`lexemeId` vs `lexeme_id`** — backlog.

### Migration risk (массовое — почти всё закрыто bundled SQLite)

- ✅ **JSON-escape** — `json_object()` через JSON1 (в `05_migration_strategy.md`).
- ✅ **Edge case формулировка** — то же.
- ✅ **`options DEFAULT 0` mismatch** — нет recreate (ALTER DROP COLUMN), проблема исчезает.
- ✅ **`write_quiz` FK DROP TABLE** — нет DROP TABLE, проблема исчезает.
- ✅ **AUTOINCREMENT после RENAME** — нет RENAME, проблема исчезает.
- ✅ **Built-in id хардкод idempotency** — закрыто `system_key` + `INSERT OR IGNORE`.
- ✅ **UNIQUE(dictionary_id, name) NULL** — закрыто partial UNIQUE.
- ✅ **MigrationTestHelper coverage** — расширенный список тест-сценариев в `05_migration_strategy.md` § «Тестовые сценарии».
- ❌ **Стиль миграций** (имя класса `Migration11To12` vs `migration_11_12`) — стилистика; в чеклист `05` («именовать как существующие миграции проекта»).
- ✅ **Backward compat downgrade** — rejected ранее.
- ❌ **`ON UPDATE NO ACTION` в DDL** — стилистика; SQLite default = NO ACTION, добавим только если Room verification упадёт. Добавить в чеклист `05`.

### YAGNI (массовое — все основные уже разрешены)

- ❌ **YAGNI soft-delete** — rejected ранее (нужна защита built-in + future UI).
- ❌ **YAGNI JSON payload** — rejected ранее (sealed расширяемость; JSON-escape проблема решена `json_object()` через bundled).
- ❌ **YAGNI sort_order** — rejected (нужен порядок типов).
- ❌ **YAGNI UNIQUE** — rejected (partial UNIQUE добавлен).
- ✅ **YAGNI countValuesForType** — rejected, метод удалён.
- ❌ **YAGNI getTypesForDictionary** — rejected (`flowTypesForDictionary` нужен для UI типов словаря, на старте через built-in global).

---

## Оставшиеся для обсуждения

1. ✅ **Naming `payload` → `value`** — приняли, переименовать во всех документах. Конвенция проекта (`WordDb.value`, `SampleDb.value`, `HintDb.value`). Записать в чеклист 04 / применить при обновлении 03 / 02 / 05.
2. ✅ **Naming `ComponentValueData.Text` → `TextValue`** — приняли. Sealed-варианты: `TextValue` / `LongTextValue` / `ImageValue`. Поля внутри (`text: String` / `uri: String`) остаются. Записать в чеклист 04 / применить при обновлении 03 / 02 / 05.

---

## Meta-возражение YAGNI-агента

> Документ явно проектирует под user-defined feature (soft-delete, UNIQUE на name, sort_order, dictionary_id фильтр), при этом сам же в § 8 research фиксирует «UI создания/редактирования/удаления user-defined типов не реализуем сейчас». Это классический «schema-первый», build-it-and-they-will-come. Чище — выпустить v12 как минимальный рефакторинг (только `name`, `template_key`, `is_system`, `dictionary_id`), а v13 при появлении user-defined UI донесёт `deleted`, `deleted_at`, `sort_order`, UNIQUE — миграцией нескольких ADD COLUMN.

---

## Triage

Заполняется conductor'ом вместе с пользователем. Для каждого finding: `→ закрыть в дизайне сейчас` / `→ backlog` / `→ rejected`.
