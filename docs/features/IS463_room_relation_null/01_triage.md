# Triage: IS463 — Room @Relation NULL в getRandomWriteQuizList

## 1. Корневая причина

### Data flow от триггера до ошибки

1. `QuizGameImpl` → `QuizChatUseCase.getRandomWriteQuizList()` → `CoreDbApi.QuizApi.getRandomWriteQuizList()`
2. `CoreDbApiImpl.QuizApiImpl.getRandomWriteQuizList()` → `WordDao.getRandomWriteQuizList(grade, limit, langId)`
3. `WordDao.getRandomWriteQuizList()` — запрос: `SELECT * from write_quiz WHERE grade = :grade AND dictionary_id = :langId ORDER BY RANDOM() LIMIT :limit`
4. Room автоматически загружает `@Relation`: для каждого `WriteQuizDb` ищет `LexemeDb` по `write_quiz.lexeme_id = lexemes.id`
5. Поле `lexemeDbWithWordDbRelation` в `WriteQuizDbEntity` — NON-NULL тип `LexemeDbWithWordDbRelation`
6. Если лексема с указанным `id` не существует → Room получает NULL → `IllegalStateException`

### Конкретные файлы

| Файл | Роль |
|---|---|
| `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/WriteQuizDbEntity.kt` | Определяет `@Relation` с NON-NULL `lexemeDbWithWordDbRelation` |
| `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDbWithWordDbRelation.kt` | Вложенная `@Relation` с NON-NULL `wordDb: WordDb` |
| `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt:164-169` | Запрос `getRandomWriteQuizList` — точка краша |
| `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_010_to_011.kt` | Миграция пересоздаёт `write_quiz` и `words` без проверки целостности |

### Почему возникают orphaned записи

**Основная гипотеза: несогласованные данные при миграции.**

Миграция 10→11 (`Migration_010_to_011.kt`) пересоздаёт таблицы `write_quiz` и `words`:
- Данные из старых таблиц копируются в новые через `INSERT ... SELECT`
- FK constraints в SQLite **выключены по умолчанию** (`PRAGMA foreign_keys = OFF`)
- Room включает `PRAGMA foreign_keys = ON` при открытии connection, но **миграция выполняется до этого момента** (миграция использует `SupportSQLiteDatabase` напрямую)
- Если до миграции в таблице `write_quiz` были записи с `lexeme_id`, ссылающимся на несуществующую лексему — они перенесутся в новую таблицу без ошибки
- Аналогично для цепочки `lexemes.word_id → words.id`

> 📎 guide: docs/guides/data-layer.md — "Foreign keys с CASCADE delete на всех связях"

**Воспроизведение от пользователя:** экспорт БД версии 10, импорт в приложение с версией 11.

**Анализ экспортированных БД (LexemeDb10.sqlite, LexemeDb11.sqlite):**
- Обе базы **консистентны** — нет orphaned записей ни в write_quiz→lexemes, ни в lexemes→words
- Словари: English(1618 слов), French(559), Spanish(566), Latin(0 слов, 0 quiz)
- Все FK связи валидны в обеих версиях

**Вывод:** orphaned данные — НЕ основная причина. Возможные сценарии:
1. **Race condition** — удаление слова/лексемы во время квиза (между SELECT и @Relation подзапросом)
2. **Room @Relation + RANDOM() + LIMIT** — известный баг Room (бэклог: ВекторныйПиздеж). Room 2.7.1 давал IllegalStateException при RANDOM()+LIMIT+@Relation (https://issuetracker.google.com/issues/413924560). Текущая версия Room 2.7.1 → обновлена до 2.8.4 в BoM, но проблема может сохраняться.
3. **Транзиентное состояние** — данные были невалидны после миграции, но до экспорта были исправлены

**Рекомендация:** фикс через nullable @Relation остаётся правильным независимо от причины — защищает от любого из трёх сценариев.

**Дополнительный фактор:**

`CoreDbApiImpl.WordApiImpl.deleteWordSuspend()` (строки 173-186) вручную удаляет samples и lexemes перед удалением word, полагаясь на то, что cascade удалит quiz при удалении lexeme. Если cascade не сработал (FK constraints были выключены в какой-то момент), quiz-записи остались orphaned.

Также `CoreDbApiImpl.LexemeApiImpl.deleteLexeme()` (строка 260) удаляет лексему через `deleteLexemeById`, не удаляя quiz явно — полностью полагается на CASCADE.

## 2. Тип бага

**simple**

Обоснование:
- Баг вызван несогласованностью данных (orphaned `write_quiz` записи), не ошибкой в спеке
- Фикс: добавить защиту от NULL в `@Relation` (сделать поле nullable + отфильтровать невалидные записи) или добавить LEFT JOIN / data cleanup
- Не меняет поведение, описанное в спеке — квиз должен показывать только валидные записи
- Затрагивает entity-файлы в одном модуле (`core-db-impl`)

`needs_spec_update = false`

## 3. Затронутые спеки

Директория `docs/features-spec/`. Прочитан `README.md`.

Спеки по теме не найдены. В списке спек нет документов, описывающих:
- Доменную модель quiz/lexeme/word relationships
- Контракты Room entity и DAO
- Поведение при удалении данных (cascade strategy)

Ближайшие по области — общие разделы "Доменная модель" и "Квиз-чат (QuizChat)" из README, но отдельных спек-файлов для них нет.

## log_messages
- Корневая причина: orphaned `write_quiz` записи ссылаются на несуществующие лексемы, Room крашится на NON-NULL `@Relation`
- Тип бага: simple (needs_spec_update = false), фикс в entity-слое core-db-impl
- Основная гипотеза: данные стали несогласованными при миграции 10→11, где FK constraints не enforce'ились

_model: claude-opus-4-6_
