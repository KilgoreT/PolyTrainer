# Варианты решения — IS461. QuizGameImpl IndexOutOfBoundsException on empty quiz list

---

## A: Guard в `nextStep()` — проверка `quizList.isEmpty()`

> Добавить проверку размера `quizList` в `nextStep()` при переходе из `Pending` в `Started`. Если список пуст — оставить `Pending`.

Код:

```kotlin
private fun nextStep() {
    currentStep = when (val step = currentStep) {
        is Step.Pending -> if (quizList.isNotEmpty()) Step.Started(0) else Step.Pending
        is Step.Started -> if (step.value < maxStep() - 1) Step.Started(step.value + 1) else Step.Pending
    }
}
```

| | |
|---|---|
| Плюсы | Минимальное изменение (1 строка). Фикс в корневой причине — `nextStep()` сам решает, можно ли стартовать. `hasNextStep()` автоматически возвращает `false` для пустого списка. |
| Минусы | `getQuiz()` по-прежнему без bounds check — если кто-то вызовет `getQuiz(Step.Started(N))` напрямую с невалидным индексом, краш возможен. |
| Сложность | Низкая |
| Файлы | `QuizGameImpl.kt` (строка 343) |

> Это минимально достаточный фикс. Весь downstream (`hasNextQuestion`, `nextQuestion`, `DatasourceEffectHandler`) работает корректно — при `hasNextQuestion() = false` хендлер переходит в ветку `SessionOver`.

---

## B: Guard в `nextStep()` + bounds check в `getQuiz()`

> Вариант A + дополнительная защита в `getQuiz()`: проверка индекса перед обращением к `quizList`.

Код:

```kotlin
private fun nextStep() {
    currentStep = when (val step = currentStep) {
        is Step.Pending -> if (quizList.isNotEmpty()) Step.Started(0) else Step.Pending
        is Step.Started -> if (step.value < maxStep() - 1) Step.Started(step.value + 1) else Step.Pending
    }
}

private fun getQuiz(step: Step): QuizItem {
    return when (step) {
        is Step.Pending -> throw QuizNotLoadedException()
        is Step.Started -> quizList.getOrElse(step.value) {
            throw IndexOutOfBoundsException(
                "Quiz index ${step.value} out of bounds, quizList.size=${quizList.size}"
            )
        }
    }
}
```

| | |
|---|---|
| Плюсы | Двойная защита: `nextStep()` не пропускает пустой список, `getQuiz()` даёт понятное сообщение при невалидном индексе. Defensive programming — будущие изменения не вызовут такой же непонятный crash. |
| Минусы | Два изменения вместо одного. `getQuiz()` по сути дублирует защиту — при корректной работе `nextStep()` невалидный индекс невозможен. |
| Сложность | Низкая |
| Файлы | `QuizGameImpl.kt` (строки 343, 281-286) |

> Bounds check в `getQuiz()` — страховка, а не основной фикс. Exception message с контекстом (`quizList.size`) облегчит отладку если подобная ситуация возникнет в будущем по другой причине.

---

## C: Guard в `nextStep()` + bounds check в `getQuiz()` + логирование пустого списка в `loadData()`

> Варианты A + B + логирование в `loadData()` когда `fetchData()` возвращает пустой список. Позволяет отслеживать частоту ситуации в продакшне.

Код (дополнение к варианту B):

```kotlin
override suspend fun loadData() {
    clearData()
    val quizData = fetchData()
    if (quizData.isEmpty()) {
        logger.w(tag = LogTags.CHAT, message = "loadData: fetchData returned empty list")
    }
    addQuizData(quizData)
}
```

| | |
|---|---|
| Плюсы | Всё из варианта B + наблюдаемость: если пустой список — частое явление, логи покажут это. Помогает диагностировать ситуацию в продакшне без воспроизведения. |
| Минусы | Три изменения. Лог может быть шумным если пользователи часто открывают квиз с пустым словарём. |
| Сложность | Низкая |
| Файлы | `QuizGameImpl.kt` (строки 343, 281-286, 46-50) |

> `logger.w` (warning level) — подходящий уровень: ситуация не аварийная (crash предотвращён), но нетипичная. `LogTags.CHAT` уже используется в `addQuizData()`.

## log_messages
- Solutions завершён: 3 варианта — от минимального guard до guard + bounds check + логирование

_model: claude-opus-4-6_
