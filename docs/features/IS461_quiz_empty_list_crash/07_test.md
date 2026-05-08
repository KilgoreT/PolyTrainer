# Test — IS461. QuizGameImpl IndexOutOfBoundsException on empty quiz list

## Решение: тесты нужны

**Обоснование:** фикс бага — нужен тест воспроизводящий баг (упадёт до фикса, пройдёт после). Меняется публичное поведение метода `hasNextQuestion()` при пустом `quizList`. Добавляется новый сценарий (логирование пустого списка в `loadData()`).

## Что сделано

### 1. Добавлены тестовые зависимости

Файл: `modules/screen/quiz/chat/build.gradle.kts`

Добавлены `testLibs.mockk` и `testLibs.coroutinesTest` — ранее модуль имел только `junit`.

### 2. Создан тестовый класс `QuizGameImplEmptyListTest`

Файл: `modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/quiz/QuizGameImplEmptyListTest.kt`

3 теста по варианту C:

| Тест | Что проверяет | Статус до фикса |
|------|--------------|-----------------|
| `hasNextQuestion returns false when quizList is empty after loadData` | Корневой баг: `hasNextQuestion()` должен вернуть `false` при пустом `quizList` | FAIL (AssertionError: true вместо false) |
| `loadData logs warning when fetchData returns empty list` | Вариант C: `logger.w` вызывается при пустом результате | FAIL (logger.w не вызван) |
| `hasNextQuestion returns false on reload with empty list` | Повторная сессия с пустым списком | FAIL (AssertionError: true вместо false) |

### 3. TDD red phase подтверждена

Все 3 теста компилируются и падают на текущем коде — корректно воспроизводят баг. После реализации варианта C все тесты должны пройти.

## log_messages
- Test завершён: 3 теста написаны (TDD red phase), все падают на текущем коде — баг воспроизведён

_model: claude-opus-4-6_
