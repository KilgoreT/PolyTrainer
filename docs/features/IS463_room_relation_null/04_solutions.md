# Варианты решения: IS463 — Room @Relation NULL

---

## A: Nullable @Relation + фильтрация на уровне маппинга

> Сделать поля `@Relation` nullable в entity, фильтровать невалидные записи в функции маппинга `toApiEntity()`.

### Изменения

`WriteQuizDbEntity.kt`:

```kotlin
data class WriteQuizDbEntity(
    @Embedded val writeQuizDb: WriteQuizDb,
    @Relation(
        entity = LexemeDb::class,
        parentColumn = "lexeme_id",
        entityColumn = "id"
    )
    val lexemeDbWithWordDbRelation: LexemeDbWithWordDbRelation?,  // nullable
)
```

`LexemeDbWithWordDbRelation.kt`:

```kotlin
data class LexemeDbWithWordDbRelation(
    @Embedded val lexemeDb: LexemeDbEntity,
    @Relation(
        parentColumn = "word_id",
        entityColumn = "id"
    )
    val wordDb: WordDb?,  // nullable
)
```

`CoreDbApiImpl.kt` — фильтрация при маппинге:

```kotlin
override suspend fun getRandomWriteQuizList(grade: Int, limit: Int, langId: Long): List<WriteQuizComplexEntity> {
    return wordDao.getRandomWriteQuizList(grade, limit, langId)
        .filter { it.lexemeDbWithWordDbRelation != null 
                  && it.lexemeDbWithWordDbRelation.wordDb != null }
        .map { it.toApiEntity() }
}
```

| | |
|---|---|
| Плюсы | Покрывает все три метода DAO одним изменением entity. Room не крашится. Осиротевшие записи тихо отфильтровываются. |

> 📎 guide: docs/guides/data-layer.md — "Три слоя маппинга: DB → API → Domain. Каждый через extension-функцию"
| Минусы | Маскирует проблему данных — orphaned записи остаются в БД. Код маппинга `toApiEntity()` усложняется null-проверками. Нужно менять все call sites. |
| Сложность | Низкая |
| Файлы | `WriteQuizDbEntity.kt`, `LexemeDbWithWordDbRelation.kt`, `CoreDbApiImpl.kt` |

> Не решает корневую причину (orphaned данные), но делает приложение устойчивым к ним. Пользователь может получить меньше квизов, чем запросил (отфильтрованные не заменяются).

---

## B: Миграция 11→12 с cleanup orphaned записей

> Добавить новую миграцию, которая удаляет осиротевшие записи `write_quiz` и `lexemes` при обновлении БД.

### Изменения

Новый файл `Migration_011_to_012.kt`:

```kotlin
val migration_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Удалить write_quiz с несуществующими lexeme_id
        db.execSQL("""
            DELETE FROM write_quiz 
            WHERE lexeme_id NOT IN (SELECT id FROM lexemes)
        """)
        // Удалить lexemes с несуществующими word_id
        db.execSQL("""
            DELETE FROM lexemes 
            WHERE word_id NOT IN (SELECT id FROM words)
        """)
    }
}
```

`Database.kt` — обновить версию до 12, зарегистрировать миграцию.

| | |
|---|---|
| Плюсы | Решает корневую причину — очищает orphaned данные. После миграции БД полностью консистентна. |
| Минусы | Не защищает от будущих несогласованностей (если новый источник orphaned данных появится). Нужен тест миграции (Schemable, AllMigrationTest). Пользователи потеряют quiz-прогресс для orphaned записей (допустимо — данные и так невалидны). |

> 📎 guide: docs/guides/testing-migrations.md — "Один тест-класс на миграцию, Schemable на каждую версию таблицы, обновить AllMigrationTest"
| Сложность | Средняя |
| Файлы | Новый `Migration_011_to_012.kt`, `Database.kt`, новые тест-файлы |

> Одноразовая очистка. Если пользователь уже на версии 11 — сработает при обновлении. Если пользователь ставит приложение с нуля — миграция не нужна (Room создаст чистую БД).

---

## ~~C: Комбинированный — nullable entity + миграция cleanup~~ ОТКЛОНЁН

> Анализ экспортированных БД (DB10, DB11) показал: orphaned записей нет. Обе базы консистентны. Миграция cleanup бессмысленна — чистить нечего. Причина краша — скорее всего Room баг с RANDOM()+@Relation (known issue: https://issuetracker.google.com/issues/413924560).
>
> **Рекомендация: вариант A** — nullable @Relation + фильтрация. Без миграции.

---

## D: Raw SQL запрос с JOIN вместо @Relation

> Заменить `@Relation`-автомаппинг на ручной SQL с LEFT JOIN, который изначально не вернёт orphaned записи.

### Изменения

`WordDao.kt`:

```kotlin
@Query("""
    SELECT wq.*, l.*, w.* 
    FROM write_quiz wq
    INNER JOIN lexemes l ON wq.lexeme_id = l.id
    INNER JOIN words w ON l.word_id = w.id
    WHERE wq.grade = :grade AND wq.dictionary_id = :langId
    ORDER BY RANDOM() 
    LIMIT :limit
""")
suspend fun getRandomWriteQuizList(grade: Int, limit: Int, langId: Long): List<WriteQuizFlatEntity>
```

Новый flat entity (без @Relation):

```kotlin
data class WriteQuizFlatEntity(
    // все поля из write_quiz, lexemes, words — в одном flat объекте
)
```

| | |
|---|---|
| Плюсы | Orphaned записи не попадают в результат в принципе (INNER JOIN). Один SQL запрос вместо N+1 от Room @Relation. Производительнее. |
| Минусы | Нужно написать маппинг для flat entity. Room @Relation-magic теряется — ручное управление. Нужно менять три метода DAO отдельно. Вложенная структура `LexemeDbEntity` с samples потребует дополнительного запроса. |
| Сложность | Высокая |
| Файлы | `WordDao.kt`, новый `WriteQuizFlatEntity.kt`, `CoreDbApiImpl.kt` |

> Архитектурно чище в долгосрочной перспективе, но объём работы несоразмерен баг-фиксу. Фактически переписывает весь quiz data layer.

## log_messages
- Предложены 4 варианта: A (nullable entity), B (миграция cleanup), C (A+B комбо), D (raw SQL JOIN)
- Все варианты решают краш, различаются глубиной защиты и объёмом изменений

_model: claude-opus-4-6_
