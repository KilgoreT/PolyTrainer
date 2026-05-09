# Research: IS463 — Room @Relation NULL в getRandomWriteQuizList

## 1. Природа проблемы

**С точки зрения пользователя:** пользователь открывает квиз-чат, приложение крашится с `IllegalStateException`. Квиз полностью недоступен. Пользователь ожидает увидеть вопрос — вместо этого приложение закрывается.

**С точки зрения системы:** Room-запрос `getRandomWriteQuizList` возвращает `WriteQuizDbEntity`, содержащий NON-NULL поле `lexemeDbWithWordDbRelation: LexemeDbWithWordDbRelation`. Room автоматически загружает `@Relation` — ищет `LexemeDb` по `write_quiz.lexeme_id = lexemes.id`. Если лексема с таким `id` отсутствует в таблице `lexemes`, Room получает NULL для NON-NULL поля и бросает `IllegalStateException`.

**Почему это проблема:** краш блокирует основной use case приложения — квиз-сессию. Затрагивает всех пользователей, у которых в БД есть осиротевшие записи `write_quiz`. Воспроизводится стабильно — при каждом входе в квиз.

## 2. Воспроизведение

**Шаги воспроизведения (подтверждено пользователем):**

1. Установить версию приложения с БД версии 10
2. Создать слова, лексемы, квизы (или использовать экспортированную БД)
3. Удалить слово или лексему (создав осиротевшую запись в `write_quiz`)
4. Обновить приложение до версии с БД версии 11
5. Миграция 10→11 выполнится, перенесёт все данные включая осиротевшие записи
6. Открыть квиз-чат → краш

**Альтернативный путь (теоретический):**

1. В любой версии приложения — если FK constraints не enforce'ятся в какой-то момент, и удаление `lexeme` не каскадирует удаление `write_quiz` — запись осиротевает
2. Открыть квиз-чат → краш

**Окружение:** Android, Room DB version 11, затронутая версия приложения: 0.1.1

## 3. Корневая причина

Двухуровневая проблема:

### Уровень 1: отсутствие защиты от NULL в @Relation

Файл `WriteQuizDbEntity.kt` (строка 14):

```kotlin
val lexemeDbWithWordDbRelation: LexemeDbWithWordDbRelation,
```

Поле объявлено как NON-NULL Kotlin-тип. Room при маппинге результата SQL-запроса в объект обнаруживает, что связанная запись не найдена, и бросает `IllegalStateException` вместо возврата null.

Аналогично в `LexemeDbWithWordDbRelation.kt` (строка 12):

```kotlin
val wordDb: WordDb,
```

Если `LexemeDb.word_id` ссылается на несуществующий `WordDb.id` — та же ситуация.

### Уровень 2: источник осиротевших данных — миграция 10→11

Файл `Migration_010_to_011.kt`:

- Миграция пересоздаёт таблицы `write_quiz` и `words` через паттерн: CREATE TABLE new → INSERT ... SELECT → DROP TABLE old → RENAME
- Новая таблица `write_quiz_new` объявляет `FOREIGN KEY (lexeme_id) REFERENCES lexemes(id) ON DELETE CASCADE`

> 📎 guide: docs/guides/data-layer.md — "Foreign keys с CASCADE delete на всех связях"

- **НО:** SQLite по умолчанию не enforce'ит FK constraints (`PRAGMA foreign_keys = OFF`)
- Room включает `PRAGMA foreign_keys = ON` при открытии соединения, но миграция выполняется через `SupportSQLiteDatabase` **до** того, как Room применяет свой pragma
- Поэтому `INSERT INTO write_quiz_new ... SELECT ... FROM write_quiz` копирует ВСЕ записи, включая те, у которых `lexeme_id` ссылается на несуществующую лексему

Миграция **не содержит:**
- `PRAGMA foreign_keys = ON` перед вставкой
- `PRAGMA foreign_key_check` после вставки
- `DELETE FROM write_quiz_new WHERE lexeme_id NOT IN (SELECT id FROM lexemes)` — очистку осиротевших записей

### Дополнительный фактор: ручное удаление без гарантий cascade

Файл `CoreDbApiImpl.kt`, метод `deleteWordSuspend` (строки 173-186):

```kotlin
override suspend fun deleteWordSuspend(id: Long): Int {
    wordDao.getWordSuspend(id).also { word ->
        wordDao.removeSampleSuspend(...)
        wordDao.deleteDefinitionsSuspend(...)
        return wordDao.removeWordSuspend(id)
    }
}
```

Удаляет samples и lexemes вручную, **но не удаляет quiz**. Полагается на CASCADE от `lexemes` → `write_quiz`. Если CASCADE не сработал (FK constraints были выключены), quiz-записи остаются осиротевшими.

Метод `deleteLexeme` (строка 260-262):

```kotlin
override suspend fun deleteLexeme(id: Long): Int {
    return wordDao.deleteLexemeById(id)
}
```

Удаляет лексему, quiz должен удалиться через CASCADE. Если CASCADE не сработал — quiz осиротевает.

## 4. Data flow

```
1. QuizGameImpl.loadData()
   ↓
2. QuizGameImpl.fetchData()
   ↓
3. quizChatUseCase.getRandomWriteQuizList(dictionaryId, limit, maxGrade)
   ↓
4. CoreDbApi.QuizApi.getRandomWriteQuizList() → CoreDbApiImpl.QuizApiImpl
   ↓
5. WordDao.getRandomWriteQuizList(grade, limit, langId)
   SQL: SELECT * FROM write_quiz WHERE grade = :grade AND dictionary_id = :langId ORDER BY RANDOM() LIMIT :limit
   ↓
6. Room @Transaction: для каждого WriteQuizDb загружает @Relation:
   SELECT * FROM lexemes WHERE id = :lexeme_id  (из WriteQuizDbEntity)
   ↓
7. Для найденного LexemeDb загружает вложенный @Relation:
   SELECT * FROM words WHERE id = :word_id  (из LexemeDbWithWordDbRelation)
   ↓
8. *** CRASH *** если на шаге 6 lexeme не найден → NULL для NON-NULL поля → IllegalStateException
   Или если на шаге 7 word не найден → тот же результат
```

Три метода DAO подвержены той же проблеме (все возвращают `List<WriteQuizDbEntity>`):
- `getRandomWriteQuizList` — точка краша из стектрейса
- `getEarliest` — тот же @Relation, та же уязвимость
- `getFrequentMistakes` — тот же @Relation, та же уязвимость

## 5. Затронутые компоненты

| Файл | Роль в проблеме |
|---|---|
| `core/core-db-impl/.../entity/WriteQuizDbEntity.kt` | NON-NULL `@Relation` поле — прямая причина краша |
| `core/core-db-impl/.../entity/LexemeDbWithWordDbRelation.kt` | Вложенный NON-NULL `@Relation` `wordDb: WordDb` — вторая точка краша |
| `core/core-db-impl/.../room/WordDao.kt:163-169` | Запрос `getRandomWriteQuizList` — точка входа |
| `core/core-db-impl/.../room/WordDao.kt:171-183` | Запрос `getEarliest` — аналогичная уязвимость |
| `core/core-db-impl/.../room/WordDao.kt:185-197` | Запрос `getFrequentMistakes` — аналогичная уязвимость |
| `core/core-db-impl/.../room/migrations/Migration_010_to_011.kt` | Миграция копирует данные без FK-проверки |
| `core/core-db-impl/.../CoreDbApiImpl.kt:173-186` | `deleteWordSuspend` — не удаляет quiz явно, полагается на CASCADE |
| `core/core-db-impl/.../CoreDbApiImpl.kt:260-262` | `deleteLexeme` — полагается на CASCADE для quiz |
| `core/core-db-impl/.../entity/WriteQuizDb.kt` | Entity с FK constraint — CASCADE объявлен, но не гарантирован при миграции |
| `modules/screen/quiz/chat/.../QuizGameImpl.kt:173-209` | `fetchData()` — вызывает crashable DAO, нет try/catch |

## 6. Ограничения

- **Нельзя терять валидные данные.** Любой фикс должен сохранить корректные quiz-записи с существующими лексемами
- **Нельзя ломать миграцию.** `Migration_010_to_011` уже в продакшне, её нельзя менять (SQLite не поддерживает ретроактивные миграции). Можно только добавить новую миграцию 11→12 или cleanUp при открытии БД

> 📎 guide: docs/guides/testing-migrations.md — "Один тест-класс на миграцию, Schemable на каждую версию таблицы, AllMigrationTest обязателен"
- **Три точки входа в DAO.** Фикс на уровне entity (`WriteQuizDbEntity`) покрывает все три метода (`getRandomWriteQuizList`, `getEarliest`, `getFrequentMistakes`). Фикс на уровне DAO требует изменения каждого метода отдельно
- **Цепочка @Relation двухуровневая:** `WriteQuizDb → LexemeDb → WordDb`. Фикс должен учитывать оба уровня — лексема может существовать, но ссылаться на несуществующее слово
- **Зависимость `WriteQuizDbEntity.toApiEntity()`:** функция маппинга обращается к `lexemeDbWithWordDbRelation.lexemeDb` и `lexemeDbWithWordDbRelation.wordDb` — если сделать поля nullable, нужно обновить маппинг
- **Контракт с `QuizChatUseCase` / `QuizGameImpl`:** ожидает `WriteQuiz` с заполненными `word`, `lexeme` — nullable результат потребует фильтрации на уровне DAO или use case

## log_messages
- Исследованы WriteQuizDbEntity, LexemeDbWithWordDbRelation — двухуровневый @Relation с NON-NULL полями, оба уязвимы к orphaned данным
- Миграция 10→11 копирует данные без FK enforcement и без cleanup orphaned записей — подтверждённый источник проблемы
- Три метода DAO (`getRandomWriteQuizList`, `getEarliest`, `getFrequentMistakes`) подвержены одной и той же уязвимости

_model: claude-opus-4-6_
