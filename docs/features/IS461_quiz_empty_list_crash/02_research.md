# Research — IS461. QuizGameImpl IndexOutOfBoundsException on empty quiz list

## 1. Природа проблемы

**С точки зрения пользователя:** пользователь открывает квиз-чат, приложение начинает загрузку квизов. Если в текущем словаре нет слов для квиза (пустой словарь, или все слова достигли `maxGrade`), приложение крашится вместо того чтобы показать сообщение об отсутствии вопросов.

**С точки зрения системы:** `QuizGameImpl` не обрабатывает граничное условие — пустой `quizList` после `loadData()`. Метод `nextStep()` безусловно переводит шаг из `Pending` в `Started(0)`, не проверяя наличие элементов. Дальше по цепочке `getQuiz(Step.Started(0))` обращается к `quizList[0]` — `IndexOutOfBoundsException`.

**Почему это проблема:** fatal crash в продакшне (версия 0.1.1). Затронуты пользователи с пустыми словарями или словарями, где все слова полностью выучены (grade >= maxGrade). Также затронута повторная сессия (`Msg.QuizReLoaded` → `DatasourceEffect.NextQuestion`), когда `loadData()` возвращает пустой список после фильтрации.

## 2. Воспроизведение

**Шаги:**
1. Создать словарь без слов (или с единственным словом, у которого `grade >= 3`)
2. Перейти на экран квиз-чата
3. Приложение отправляет `DatasourceEffect.PrepareToStart` → `Msg.PrepareToStart` → UI показывает welcome message
4. Пользователь нажимает "Старт" → `Msg.Start` → `DatasourceEffect.LoadQuiz`
5. `quizGame.loadData()` → `fetchData()` → `getRandomWriteQuizList()` возвращает пустой список
6. `Msg.QuizLoaded` → reducer отправляет `DatasourceEffect.NextQuestion`
7. `quizGame.hasNextQuestion()` возвращает `true` (баг)
8. `quizGame.nextQuestion()` → crash

**Условия:** словарь пуст или все слова имеют `grade >= maxGrade` (по умолчанию 3).

## 3. Корневая причина

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt`

Метод `nextStep()` (строки 341-346):

```kotlin
private fun nextStep() {
    currentStep = when (val step = currentStep) {
        is Step.Pending -> Step.Started(0)  // ← БАГ: безусловный переход
        is Step.Started -> if (step.value < maxStep() - 1) Step.Started(step.value + 1) else Step.Pending
    }
}
```

`maxStep()` (строка 348) возвращает `quizList.size`. При пустом `quizList`:
- `maxStep()` = 0
- Ветка `Step.Pending` всё равно возвращает `Step.Started(0)` — проверки `quizList.isEmpty()` нет
- `hasNextStep()` вызывает `nextStep()`, видит `currentStep is Step.Started` → возвращает `true`
- Далее `getQuiz(Step.Started(0))` → `quizList[0]` → crash

Ветка `Step.Started` корректна: `step.value < maxStep() - 1` = `0 < -1` = `false` → `Step.Pending`. Но до неё дело не доходит, потому что `nextStep()` уже установил `Step.Started(0)` при первом вызове из `hasNextStep()`.

Дополнительно: `getQuiz()` (строка 281-286) не имеет bounds check:

```kotlin
private fun getQuiz(step: Step): QuizItem {
    return when (step) {
        is Step.Pending -> throw QuizNotLoadedException()
        is Step.Started -> quizList[step.value]  // ← нет проверки индекса
    }
}
```

## 4. Data flow

```
Msg.Start
    ↓
ChatReducer.reduce() → (state, setOf(DatasourceEffect.LoadQuiz))
    ↓
DatasourceEffectHandler.runEffect(DatasourceEffect.LoadQuiz)
    ↓
quizGame.loadData()
    ↓
clearData()  → quizList.clear(), userAnswers.clear(), currentStep = Step.Pending
    ↓
fetchData()  → quizChatUseCase.getRandomWriteQuizList(dictionaryId, limit=10, maxGrade=3)
    ↓
addQuizData(emptyList)  → quizList = []   ← quizList остаётся пустым
    ↓
return Msg.QuizLoaded(content = quizGame.getStat())
    ↓
ChatReducer.reduce(Msg.QuizLoaded) → (state.startQuiz().userMessage(), setOf(DatasourceEffect.NextQuestion))
    ↓
DatasourceEffectHandler.runEffect(DatasourceEffect.NextQuestion)
    ↓
quizGame.hasNextQuestion()
    ↓
hasNextStep()  →  nextStep()
    ↓
nextStep():  currentStep = Step.Pending → Step.Started(0)   ← БЕЗ проверки quizList.size
    ↓
hasNextStep() returns: currentStep is Step.Started → true
    ↓
quizGame.nextQuestion()  →  getNextQuestion()  →  getQuiz(Step.Started(0))
    ↓
quizList[0]   ← quizList пуст → IndexOutOfBoundsException
```

**Ключевой момент:** reducer безусловно отправляет `DatasourceEffect.NextQuestion` после `Msg.QuizLoaded`. Reducer не знает (и не должен знать по TEA-принципам) пуст ли `quizList` — эта информация инкапсулирована в `QuizGameImpl`. Защита должна быть в `QuizGameImpl`, а не в reducer.

> 📎 guide: docs/guides/effect-handlers.md — "Без сложной логики в хендлерах — они выполняют эффект и конвертируют результат в сообщение. Принятие решений остаётся в редьюсере"

Однако тут нюанс: `DatasourceEffectHandler` уже содержит ветвление `if (quizGame.hasNextQuestion())` (строка 110). Это корректно — хендлер проверяет условие и конвертирует в нужный `Msg`. Проблема в том, что `hasNextQuestion()` врёт при пустом списке.

## 5. Затронутые компоненты

| Файл | Роль в проблеме |
|------|----------------|
| `QuizGameImpl.kt` (строки 341-346) | `nextStep()` — корневая причина. Безусловный переход `Pending → Started(0)` |
| `QuizGameImpl.kt` (строки 281-286) | `getQuiz()` — точка краша. Нет bounds check |
| `QuizGameImpl.kt` (строки 336-339) | `hasNextStep()` — возвращает `true` при пустом списке (следствие бага в `nextStep()`) |
| `QuizGameImpl.kt` (строки 52-54) | `hasNextQuestion()` — публичный API, делегирует в `hasNextStep()` |
| `DatasourceEffectHandler.kt` (строки 108-129) | Вызывающий код. Доверяет `hasNextQuestion()` |
| `ChatReducer.kt` (строка 68) | `Msg.QuizLoaded` безусловно порождает `DatasourceEffect.NextQuestion` |

## 6. Ограничения

- **Контракт `QuizGame` interface:** публичный API (`hasNextQuestion`, `nextQuestion`, `loadData`) используется только из `DatasourceEffectHandler`. Изменения в `QuizGameImpl` не ломают внешние модули.
- **Побочный эффект `hasNextStep()`:** метод вызывает `nextStep()` — он мутирует `currentStep`. Это значит `hasNextQuestion()` не идемпотентен (не чистая проверка, а check-and-advance). Это existing design, не баг текущего тикета, но ограничение для фикса: нельзя просто добавить ранний return в `hasNextStep()` без учёта того, что `currentStep` уже мог быть изменён.
- **Ветка `Step.Started` в `nextStep()`:** условие `step.value < maxStep() - 1` корректно для непустого списка. При `maxStep() = 0` → `0 < -1` → false → `Step.Pending`. Это означает что даже если баг из `Pending` пропустит в `Started(0)`, повторный вызов `hasNextStep()` уже вернёт `false`. Но crash происходит при первом вызове — до повторного дело не доходит.
- **`clearData()` в `loadData()`:** при повторной загрузке (CONTINUE) `quizList` очищается перед `fetchData()`. `currentStep` сбрасывается в `Pending`. Фикс должен работать и при повторных сессиях.
- **Без изменения `QuizGame` interface:** интерфейс не нуждается в изменении сигнатур. Фикс целиком внутри `QuizGameImpl`.

## log_messages
- Research завершён: проблема полностью охарактеризована — `nextStep()` не проверяет `quizList.isEmpty()`, побочный эффект `hasNextStep()` мутирует state
- Затронут 1 файл (`QuizGameImpl.kt`), 3 метода (`nextStep`, `hasNextStep`, `getQuiz`)

_model: claude-opus-4-6_
