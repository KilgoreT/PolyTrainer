# Implement — IS461. QuizGameImpl IndexOutOfBoundsException on empty quiz list

## Реализован вариант C

Все изменения в одном файле: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt`

### 1. Guard в `nextStep()` (строка 343)

```kotlin
is Step.Pending -> if (quizList.isNotEmpty()) Step.Started(0) else Step.Pending
```

Корневой фикс. При пустом `quizList` шаг остаётся `Pending`, `hasNextStep()` вернёт `false`, downstream (`DatasourceEffectHandler`) перейдёт в ветку `SessionOver`.

### 2. Bounds check в `getQuiz()` (строки 281-288)

```kotlin
is Step.Started -> quizList.getOrElse(step.value) {
    throw IndexOutOfBoundsException(
        "Quiz index ${step.value} out of bounds, quizList.size=${quizList.size}"
    )
}
```

Страховка. При невалидном индексе — понятное сообщение с контекстом вместо стандартного `ArrayList.get()`.

### 3. Warning лог в `loadData()` (строки 49-51)

```kotlin
if (quizData.isEmpty()) {
    logger.w(tag = LogTags.CHAT, message = "loadData: fetchData returned empty list")
}
```

Наблюдаемость. Позволяет отслеживать частоту ситуации в продакшне.

## Тесты

Все 3 теста из TDD red phase прошли (green phase):

| Тест | Результат |
|------|-----------|
| `hasNextQuestion returns false when quizList is empty after loadData` | PASS |
| `loadData logs warning when fetchData returns empty list` | PASS |
| `hasNextQuestion returns false on reload with empty list` | PASS |

## Нетривиальные решения

Нет. Реализация строго по варианту C из `04_solutions.md`.

## log_messages
- Implement завершён: вариант C реализован, 3/3 тестов прошли

_model: claude-opus-4-6_
