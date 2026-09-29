# IS508 | План правок: код до → код после → зачем

Формат: каждая правка — триада «до / после / зачем» плюс короткое
объяснение конструкций. Решения — [brief.md](brief.md) Д1–Д7, разбор —
[analysis.md](analysis.md). Порядок этапов — §9 анализа.

---

## Э1. Пин распределения по грейдам (тесты, без правок кода)

**Файл:** `app/src/test/.../quizchat/QuizChatUseCaseImplTest.kt`

**До.** Кейсы `normal` / `few items` / `empty` проверяют только
`size <= limit` и «не пусто». Сколько записей даёт каждая корзина —
нигде не закреплено.

**После.** Хелпер получает `wordId`, стабы корзин — по грейдам раздельно:

```kotlin
private fun makeQuizEntity(
    id: Long,
    grade: Int,
    dictId: Long = 1L,
    wordId: Long = id,
) = WriteQuizComplexEntity(
    quizData = WriteQuizApiEntity(id = id, dictionaryId = dictId, lexemeId = id, grade = grade, addDate = Date()),
    lexemeData = LexemeApiEntity(id = id, addDate = Date()),
    wordData = WordApiEntity(id = wordId, dictionaryId = dictId, value = "word_$wordId", addDate = Date()),
)

@Test
fun `grade distribution - halves remaining per grade, fills from leftovers`() = runTest {
    stubPrefs()
    // грейд 0: ids 1..10, грейд 1: 11..20, грейд 2: 21..30
    coEvery { quizApi.getWriteQuizIds(grade = 0, dictionaryId = 1L) } returns (1L..10L).toList()
    coEvery { quizApi.getWriteQuizIds(grade = 1, dictionaryId = 1L) } returns (11L..20L).toList()
    coEvery { quizApi.getWriteQuizIds(grade = 2, dictionaryId = 1L) } returns (21L..30L).toList()
    coEvery { quizApi.getWriteQuizByIds(any()) } answers {
        firstArg<List<Long>>().map { makeQuizEntity(it, grade = (it - 1).toInt() / 10) }
    }

    val result = useCase.getRandomWriteQuizList(limit = 10, maxGrade = 2, dictionaryId = 1L)

    assertEquals(10, result.size)
    // корзина 0 даёт limit/2 = 5, корзина 1 — remaining/2 = 2, корзина 2 — 1,
    // добор до 10 — из остатков любых грейдов
    assertTrue(result.count { it.grade == 0 } >= 5)
    assertTrue(result.count { it.grade == 1 } >= 2)
    assertTrue(result.count { it.grade == 2 } >= 1)
}
```

**Зачем.** Д7 меняет контейнер порции и учёт `remaining`; без пина
регресс раздачи по грейдам прошёл бы молча (ревью). Тест пишется и
проходит на ТЕКУЩЕМ коде, потом остаётся зелёным.

**Конструкции.** `wordId` в хелпере — чтобы моделировать «две лексемы
одного слова» (пины Д1/Д4 ниже). Ассерты `>=` на нижние границы, а не
точные числа: добор из `leftovers` перемешан, точный состав недетерминирован.

---

## Э2. Данные

### П1. Unique-индекс на лексему

**Файл:** `core/core-db-impl/.../entity/WriteQuizDb.kt`, строка 22.

До:
```kotlin
    indices = [Index("lexeme_id")]
```
После:
```kotlin
    // IS508: одна лексема — одна квиз-строка; инвариант закреплён схемой.
    indices = [Index("lexeme_id", unique = true)]
```

**Зачем.** Весь дедуп стоит на «одна лексема — одна квиз-строка».
Сейчас это держится только построением (compound-вставки); индекс делает
нарушение невозможным на уровне БД (Д5).

**Конструкции.** `Index(value, unique = true)` — Room генерирует
`CREATE UNIQUE INDEX`; имя индекса Room оставляет прежним
(`index_write_quiz_lexeme_id`), поэтому миграция пересоздаёт индекс под
тем же именем.

### П2. Версия схемы

**Файл:** `core/core-db-impl/.../room/Database.kt`, строка 36.

До: `version = 13` → После: `version = 14`.

**Зачем.** Изменение индекса — изменение схемы; Room сверяет живую БД с
экспортом `14.json` (генерируется сборкой в `schemas/`).

### П3. Миграция 13→14 (новый файл)

**Файл:** `core/core-db-impl/.../room/migrations/Migration_013_to_014.kt`

```kotlin
/**
 * IS508 migration M13 → M14: unique-индекс write_quiz.lexeme_id.
 *
 * На легальных базах дублей нет (квиз-строка создаётся только атомарно
 * с лексемой) — шаг 1 no-op. Схлопывание нужно, чтобы CREATE UNIQUE
 * INDEX не упал на повреждённой/восстановленной базе: остаётся
 * старейшая строка (MIN(id)).
 */
object Migration_013_to_014 : Migration(13, 14) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            DELETE FROM write_quiz
            WHERE id NOT IN (SELECT MIN(id) FROM write_quiz GROUP BY lexeme_id)
            """.trimIndent(),
        )
        connection.execSQL("DROP INDEX IF EXISTS `index_write_quiz_lexeme_id`")
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_write_quiz_lexeme_id` ON `write_quiz` (`lexeme_id`)",
        )
    }
}
```

**Зачем.** Room не умеет «изменить индекс» — только DROP + CREATE.
Порядок: сначала дедуп, иначе `CREATE UNIQUE` упадёт на дублях.

**Конструкции.** `Migration(13, 14)` + `migrate(SQLiteConnection)` —
KMP-API Room, как в `Migration_012_to_013`. `IF EXISTS` / `IF NOT EXISTS`
— идемпотентность (defensive, прецедент 12→13). Имя индекса — строго как
в экспорте `13.json` (`index_write_quiz_lexeme_id`), иначе валидация схемы
не пройдёт.

### П4. Регистрация миграции

**Файл:** `core/core-db-impl/.../di/module/RoomModule.kt`, строка 62 (+ KDoc-строка о M13→M14).

До:
```kotlin
            .addMigrations(Migration_011_to_012, Migration_012_to_013)
```
После:
```kotlin
            .addMigrations(Migration_011_to_012, Migration_012_to_013, Migration_013_to_014)
```

**Зачем.** Без регистрации Room на v13-базе уйдёт в destructive fallback
и уничтожит данные.

### П5. «Частые ошибки» — только с ошибками

**Файл:** `core/core-db-impl/.../room/WordDao.kt`, `getFrequentMistakes`, строка 335.

До:
```sql
        WHERE dictionary_id = :langId
            AND (:groupId IS NULL OR EXISTS (
```
После:
```sql
        WHERE dictionary_id = :langId
            AND error_count > 0
            AND (:groupId IS NULL OR EXISTS (
```

**Зачем.** Д6: при < `limit` строк с ошибками опция добирала слова с
нулём ошибок под видом «частых». После дедупа это стало бы заметнее.

**Конструкции.** Условие в WHERE — до `ORDER BY`/`LIMIT` (то же правило,
что для группового фильтра IS500: топ считается из отфильтрованных).

### П6. Тесты данных (androidTest, девайс)

- **`MigrationFrom13to14`** по образцу `MigrationFrom12to13`:
  `helper.runMigrationsAndValidate(14, listOf(Migration_013_to_014))`
  на v13-базе с данными → схема валидна, строки целы; v13-база с двумя
  строками на одну лексему → после миграции одна, с `MIN(id)`; chained
  `11→14` (`listOf(Migration_011_to_012, Migration_012_to_013, Migration_013_to_014)`).
- **DAO:** в `QuizGroupFilterDaoTest` — запись с `error_count = 0` не
  попадает в `getFrequentMistakes`; с `error_count = 1` — попадает.

---

## Э3. Сборка порции

**Файл:** `app/src/main/.../quizchat/QuizChatUseCaseImpl.kt`,
`getRandomWriteQuizList`. Строки 61–77 (групповой фильтр, лог) и 78–90
(корзины `allByGrades` / `sortedGrades`) — без изменений.

### П7. Контейнер и общий выбор

До (строки 92–114):
```kotlin
        val result = mutableSetOf<WriteQuiz>()
        var remaining = limit

        for ((grade, list) in sortedGrades) {
            if (remaining <= 0) break
            val expectedCount = if (result.isEmpty()) {
                limit / 2
            } else {
                remaining / 2
            }.coerceAtLeast(1)

            val available = list.take(expectedCount)
            result += available
            remaining -= available.size
        }

        if (result.size < limit) {
            val leftovers = sortedGrades.values
                .flatten()
                .filterNot { it in result }
                .shuffled()
            result += leftovers.take(limit - result.size)
        }
```
После:
```kotlin
        // IS508: порция ключуется лексемой — один вопрос один раз, это
        // свойство контейнера, а не отдельных фильтров (Д1, Д7).
        val portion = LinkedHashMap<LexemeId, WriteQuiz>()

        // Берёт в порцию до count кандидатов: лексема ещё не в порции;
        // первый проход — только новые слова, второй — остальные (Д4).
        fun pick(candidates: List<WriteQuiz>, count: Int): List<WriteQuiz> {
            val picked = mutableListOf<WriteQuiz>()
            fun tryTake(quiz: WriteQuiz, allowSameWord: Boolean) {
                if (picked.size >= count) return
                if (quiz.lexeme.lexemeId in portion) return
                if (!allowSameWord && portion.values.any { it.word.id == quiz.word.id }) return
                portion[quiz.lexeme.lexemeId] = quiz
                picked += quiz
            }
            candidates.forEach { tryTake(it, allowSameWord = false) }
            candidates.forEach { tryTake(it, allowSameWord = true) }
            return picked
        }

        var remaining = limit
        for ((_, list) in sortedGrades) {
            if (remaining <= 0) break
            val expectedCount = if (portion.isEmpty()) {
                limit / 2
            } else {
                remaining / 2
            }.coerceAtLeast(1)
            remaining -= pick(list, expectedCount).size
        }

        if (portion.size < limit) {
            pick(sortedGrades.values.flatten().shuffled(), limit - portion.size)
        }
        val gradesCount = portion.size
```

**Зачем.** `Set<WriteQuiz>` сравнивал записи целиком, включая `type`, —
отсюда дубли; `Map` по `LexemeId` делает повтор невозможным везде (Д7).
`pick` — единственное место, где решается «кого брать»: жёсткий фильтр
по лексеме и мягкое предпочтение слов (Д4) для корзин, добора и добавок
сразу. Счётчики (`expectedCount`, `remaining`, добор до `limit`) —
прежние (требование 3 брифа): меняется только выбор внутри кандидатов.

**Конструкции.**
- `LinkedHashMap` — сохраняет порядок вставки (детерминизм до финального
  `shuffled()`); ключ — value class `LexemeId`, `in portion` = `containsKey`.
- Локальная `fun pick` с замыканием на `portion` — не утекает из метода,
  состояние порции одно. Вложенная `tryTake` с флагом `allowSameWord` —
  два прохода по одному списку кандидатов без дублирования условий;
  проверка слова идёт по текущему `portion`, поэтому вторая лексема того
  же нового слова в первом проходе уже пропускается.
- `remaining -= pick(...).size` — учёт по реально добавленным.
- `for ((_, list) …)` — `grade` в теле не используется.
- `portion.values.any { … }` — O(n·m) при n, m ≤ 14; отдельное
  множество слов не заводим (одно состояние вместо двух — ревью).

### П8. Добавки

До (строки 117–136):
```kotlin
        val isEarliestOn = prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN)
                ?: false
        if (isEarliestOn) {
            val earliest = quizApi
                    .getEarliestWriteQuizList(limit, dictionaryId, groupId)
                    .shuffled()
                    .toDomainEntity(type = QuizType.EARLIEST)
                    .take(2)
            result += earliest
        }
        val isFrequentMistakesOn = prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN)
                ?: false
        if (isFrequentMistakesOn) {
            val frequentMistakes = quizApi
                .getFrequentMistakesWriteQuizList(limit, dictionaryId, groupId)
                .shuffled()
                .toDomainEntity(type = QuizType.ERRORS)
                .take(2)
            result += frequentMistakes
        }
```
После:
```kotlin
        var earliestAdded = 0
        var earliestCandidates = 0
        val isEarliestOn = prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN)
                ?: false
        if (isEarliestOn) {
            val candidates = quizApi
                    .getEarliestWriteQuizList(limit, dictionaryId, groupId)
                    .toDomainEntity(type = QuizType.EARLIEST)
            earliestCandidates = candidates.size
            earliestAdded = pick(candidates.shuffled(), ADDON_SIZE).size
        }
        var errorsAdded = 0
        var errorsCandidates = 0
        val isFrequentMistakesOn = prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN)
                ?: false
        if (isFrequentMistakesOn) {
            val candidates = quizApi
                .getFrequentMistakesWriteQuizList(limit, dictionaryId, groupId)
                .toDomainEntity(type = QuizType.ERRORS)
            errorsCandidates = candidates.size
            errorsAdded = pick(candidates.shuffled(), ADDON_SIZE).size
        }
```
плюс константа в файле:
```kotlin
/** IS508: размер добавки «Самые давние» / «Частые ошибки» (было магическое 2). */
private const val ADDON_SIZE = 2
```

**Зачем.** Добавка идёт через тот же `pick`: дубли с корзиной и между
добавками отсекаются ДО отбора двух (Д2), предпочтение слов действует и
здесь (Д4). Окно кандидатов — прежние `limit` строк (Д3), при полном
перекрытии добавка даёт 0. Вызовы `quizApi` с теми же аргументами —
существующие `coVerify` в тестах живы.

**Конструкции.** `shuffled()` до `pick` — «2 случайных из оставшихся
кандидатов», как раньше `shuffled().take(2)`. Счётчики `*Added` /
`*Candidates` нужны только логу ниже.

### П9. Лог-маркер и возврат

До (строки 138–139):
```kotlin
        return result
            .shuffled()
```
После:
```kotlin
        logger.d(
            tag = LogTags.CHAT,
            message = "getRandomWriteQuizList: portion grades=$gradesCount " +
                "earliest=+$earliestAdded/$earliestCandidates " +
                "errors=+$errorsAdded/$errorsCandidates " +
                "total=${portion.size} words=${portion.values.distinctBy { it.word.id }.size}",
        )
        return portion.values.shuffled()
```
Импорт: `me.apomazkin.lexeme.LexemeId`.

**Зачем.** Ручник и разбор жалоб получают детерминированный след:
`total` (без дублей по построению), `+a/cand` — сколько добавка дала из
скольких кандидатов (видно «перекрытие → 0»), `words` — работает ли
предпочтение (`words == total`, пока пул не исчерпан). Пишется рядом с
`groupFilter=` (IS500 §11).

### П10. Unit-тесты дедупа и предпочтения

**Файл:** `QuizChatUseCaseImplTest.kt`, секция `// ===== IS508 no duplicates =====`.
Стабы: оба pref → `true` после `stubPrefs()`; addon-методы —
`getEarliestWriteQuizList(any(), 1L, null)` / `getFrequentMistakesWriteQuizList(any(), 1L, null)`;
ассерты — только множества `it.lexeme.lexemeId.id` и размеры (финальный
`shuffled()`).

| # | Корзина | Давние | Ошибки | Ожидание |
|---|---|---|---|---|
| 1 | `[1,2,3]` | `[1,4,5]` | `[1,5,6]` | `{1..6}`, размер 6 — на текущем коде падает по размеру (7–8) |
| 2 | `[1,2,3]` | `[4,5,6,7]` | off | размер 5; `(set − {1,2,3}).size == 2`, `⊂ {4,5,6,7}` |
| 3 | `[1,2]` | off | `[1,3,4]` | `{1,2,3,4}` |
| 4 | `[1,2]` | `[1,2]` | off | ровно `{1,2}`, размер 2 (Д3) |
| 5 (Д1) | `[1(w1), 2(w1)]`, `limit=10` | off | off | размер 2 — второй проход добрал ту же лексему слова |
| 6 (Д4) | `[1(w1), 2(w1), 3(w3)]`, `limit=2` | off | off | `{1,3}` или `{2,3}`, никогда `{1,2}` |
| 7 (Д4 в добавке) | `[1(w1)]` | `[4(w1), 5(w5)]`, `ADDON_SIZE=2` | off | размер 3: 5 взята первым проходом, 4 — вторым |

Образец кейса 1:
```kotlin
@Test
fun `add-ons - lexeme never taken twice, next candidates taken`() = runTest {
    stubPrefs()
    coEvery { prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN) } returns true
    coEvery { prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN) } returns true
    coEvery { quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L) } returns listOf(1L, 2L, 3L)
    coEvery { quizApi.getWriteQuizByIds(any()) } answers {
        firstArg<List<Long>>().map { makeQuizEntity(it, grade = 0) }
    }
    coEvery { quizApi.getEarliestWriteQuizList(any(), 1L, null) } returns
        listOf(1L, 4L, 5L).map { makeQuizEntity(it, grade = 0) }
    coEvery { quizApi.getFrequentMistakesWriteQuizList(any(), 1L, null) } returns
        listOf(1L, 5L, 6L).map { makeQuizEntity(it, grade = 0) }

    val result = useCase.getRandomWriteQuizList(limit = 10, maxGrade = 0, dictionaryId = 1L)

    val lexemeIds = result.map { it.lexeme.lexemeId.id }
    assertEquals(lexemeIds.size, lexemeIds.toSet().size)
    assertEquals(setOf(1L, 2L, 3L, 4L, 5L, 6L), lexemeIds.toSet())
}
```

**Зачем.** Кейсы 1–4 — дедуп по всем трём пересечениям и кап добавки;
5–7 — пины решений Д1 и Д4, чтобы будущая «оптимизация» до дедупа по
слову или отказ от предпочтения не прошли молча.

---

## Э4. Ручники и канон

- `manual_test.md` — словарь логов (`portion …` под `###CHAT###`,
  `### Total: N` в debug-статистике) + M1–M5 из analysis §7.
- `quiz-group-filter/spec.md` §7 — блок «Инвариант порции (IS508)»,
  §11 — маркер `portion`; README «Известные пробелы» — bullet.
- Коммит один, после зелёных ручников.
