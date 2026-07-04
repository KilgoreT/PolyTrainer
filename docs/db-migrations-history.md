# DB Migrations History

История эволюции схемы БД проекта PolyTrainer. Документ заменяет удалённые migration objects (см. IS481_vPrepared scope drop).

**Current schema:** v11 (с tag 0.1.0). Internal testers все на 0.1.x → никто не имеет БД < v11 → миграции 1→2 ... 10→11 dead code, удалены.

Историю кода каждой миграции (полный source) можно восстановить через `git log --all -- core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_NNN_to_MMM.kt`.

---

## v1 → v2

**Commit:** `a0f4c5e` (2021-05-30) — IS199. Added date field to word and quiz.

**Что менялось:**
- Создана таблица `writeQuiz` (поля: `id`, `definitionId`, `grade`, `score`) — отдельное хранилище состояния квиза на запись.
- Бэкфилл записей `writeQuiz` из существующих `definitions` (по одной строке на каждую дефиницию, `grade=0`, `score=0`).

## v2 → v3

**Commit:** `a0f4c5e` (2021-05-30) — IS199. Added date field to word and quiz.

**Что менялось:**
- `words`: добавлены `addDate`, `changeDate`. Существующие записи бэкфилл'ятся текущим временем минус 1 сек.
- `writeQuiz`: добавлены `addDate`, `lastSelectDate`. `addDate` бэкфилл'ится аналогично.
- Цель — научиться сортировать слова и квизы по времени.

## v3 → v4

**Commit:** `af0dff7` (2021-05-31) — IS140. Add HINT and Quotation(SAMPLE) to database scheme.

**Что менялось:**
- Создана таблица `hint` (`id`, `definitionId`, `value`, `addDate`, `changeDate`) — подсказки к дефинициям.
- Создана таблица `sample` (`id`, `definitionId`, `value`, `source`, `addDate`, `changeDate`) — примеры использования (цитаты).

## v4 → v5

**Commit:** `c2044aa` (2022-01-12) — IS228. Added second language support.

**Что менялось:**
- Создана таблица `languages` (`id`, `code`, `name`, `addDate`, `changeDate`).
- Вставлена дефолтная запись `id=0, code='en', name='English'`.
- `words` и `writeQuiz`: добавлена FK-колонка `langId NOT NULL DEFAULT 0` — все ранее существовавшие слова и квизы автоматически привязываются к English.

## v5 → v6

**Commit:** `110bd0d` (2023-01-26) — IS250. Add select lang logic.

**Что менялось:**
- `languages`: добавлена колонка `numericCode INTEGER NOT NULL DEFAULT 1` — числовой идентификатор языка для логики выбора (вероятно, ICU/locale numeric).

## v6 → v7

**Commit:** `dc520c7` (2023-02-21) — IS254. Added top bar lang selection. Поправлена позже в `ca3d47e` (2025-04-16) — IS338. Fixed migration.

**Что менялось:**
- Резолв дубликатов `numericCode` в `languages`: для каждой группы строк с одинаковым `numericCode` оставляется первая, остальным присваивается `numericCode + (index+1) * 7`.
- Создан UNIQUE-индекс `index_languages_numericCode` на `languages.numericCode`.
- Содержательный комментарий в файле: «1. Получаем все дублирующиеся numericCode / 2. Проходимся по каждому дубликату / 3. Первую запись оставляем как есть, остальные — меняем».

## v7 → v8

**Commit:** `89a7377` (2024-12-04) — IS293. Added Lexeme Ui. Доуточнена в `ba3f1b2` (2024-12-09) — IS297. Migrate to new database scheme.

**Что менялось:**
- Большой schema-refactor с table-rebuild через `*_new` + `INSERT SELECT` + `DROP` + `RENAME`.
- `words`: колонка `word` переименована в `value`, добавлено `removeDate` (подготовка к soft delete).
- Таблица `definitions` переименована в `lexemes` (`id`, `wordId`, `translation`, `definition`, `wordClass`, `options`) — введена концепция лексемы; добавлена колонка `translation`.
- Таблица `sample` переименована в `samples` (FK `definitionId` → `lexemeId`, добавлен `removeDate`).
- Таблица `hint` переименована в `hints` (FK `definitionId` → `lexemeId`, добавлен `removeDate`).

## v8 → v9

**Commit:** `941a5ba` (2024-12-18) — IS299. Db integration for WordCard Screen.

**Что менялось:**
- `lexemes`: добавлены `addDate NOT NULL DEFAULT <currentTimeMillis>`, `changeDate`, `removeDate` — недостающие date-поля для WordCard CRUD.

## v9 → v10

**Commit:** `77304ae` (2025-02-25) — IS313. Added Chat Quiz.

**Что менялось:**
- Очередной schema-refactor через `*_new` table-rebuild — переход с camelCase на snake_case в именах колонок + foreign keys + indices.
- `words`: `langId` → `lang_id`, `addDate` → `add_date`, `changeDate` → `change_date`; `value NOT NULL` (через `COALESCE`), `add_date NOT NULL` (через `COALESCE strftime`); добавлена FK на `languages(id) ON DELETE CASCADE`; индекс `index_words_lang_id`.
- `lexemes`: аналогично, плюс FK `word_id → words(id) ON DELETE CASCADE`, индекс `index_lexemes_word_id`, `options NOT NULL DEFAULT 0`.
- `writeQuiz` → `write_quiz`: переименование таблицы, snake_case-колонки, новая колонка `error_count INTEGER NOT NULL`, FK `lexeme_id → lexemes(id) ON DELETE CASCADE`, индекс `index_write_quiz_lexeme_id`.

## v10 → v11

**Commit:** `b7e23d0` (2026-04-24) — IS441. Rename domain lang to dictionary with Room migration 11. Доуточнена в `c16e9c4` (2026-04-29) — IS441. Dictionary screens refactoring.

**Что менялось:**
- Domain-переименование: концепция «language» заменена на «dictionary».
- `languages` → `dictionaries`: новая таблица с теми же полями кроме `code` (колонка дропнута), `name NOT NULL` (через `COALESCE`).
- `words`: `lang_id` → `dictionary_id`, FK теперь на `dictionaries(id) ON DELETE CASCADE`; индекс пересоздан как `index_words_dictionary_id`.
- `write_quiz`: `lang_id` → `dictionary_id`, FK `lexeme_id` сохраняется, индекс `index_write_quiz_lexeme_id` пересоздан.
- Содержательные комментарии-разделители в файле: `=== languages → dictionaries (drop "code" column) ===`, `=== words: lang_id → dictionary_id ===`, `=== write_quiz: lang_id → dictionary_id ===`.

---

_model: claude-opus-4-7[1m]_
