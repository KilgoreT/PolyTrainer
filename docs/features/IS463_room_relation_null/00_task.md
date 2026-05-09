# Task

## IS463. Crash: Room @Relation NULL в getRandomWriteQuizList

Краш в продакшне. Версия 0.1.1.

## Стектрейс

```
Fatal Exception: java.lang.IllegalStateException: Relationship item 'lexemeDbWithWordDbRelation' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'lexeme_id' and entityColumn named 'id'.
    at me.apomazkin.core_db_impl.room.WordDao_Impl.getRandomWriteQuizList$lambda$20(WordDao_Impl.kt:1196)
```

## Суть

`WordDao.getRandomWriteQuizList()` выполняет запрос с `@Relation` между лексемой и словом. Room ожидает NON-NULL значение для связи `lexemeDbWithWordDbRelation` (parent column `lexeme_id` → entity column `id`), но получает NULL.

Возможные причины:
- Осиротевшая лексема: запись в таблице лексем ссылается на несуществующее слово
- Несогласованность данных после удаления слова без каскадного удаления лексем
- Проблема миграции БД

## Подробности

Полный стектрейс: `docs/crashes/2026-05-08_room-relation-null.md`
