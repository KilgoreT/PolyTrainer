# Triage — IS461. QuizGameImpl IndexOutOfBoundsException on empty quiz list

## 1. Корневая причина

**Файл:** `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt`

**Data flow от триггера до ошибки:**

1. `DatasourceEffectHandler.runEffect()` получает `DatasourceEffect.NextQuestion` (строка 108)
> 📎 guide: docs/guides/effect-handlers.md — "Без сложной логики в хендлерах — они выполняют эффект и конвертируют результат в сообщение. Принятие решений остаётся в редьюсере"
2. Вызывается `quizGame.hasNextQuestion()` → `hasNextStep()` → `nextStep()` (строка 341)
3. `nextStep()` при `currentStep = Step.Pending` **безусловно** переводит в `Step.Started(0)` (строка 343):
   ```kotlin
   is Step.Pending -> Step.Started(0)
   ```
4. `hasNextStep()` возвращает `true` — `currentStep is Step.Started` (строка 338)
5. `quizGame.nextQuestion()` → `getNextQuestion()` → `getQuiz(currentStep)` (строка 210)
6. `getQuiz(Step.Started(0))` → `quizList[0]` (строка 284) — **IndexOutOfBoundsException**, потому что `quizList` пуст

**Корневая причина:** `nextStep()` не проверяет размер `quizList` при переходе из `Pending` в `Started(0)`. Если `loadData()` вернул пустой список (словарь без слов, или все слова выше maxGrade), `quizList` остаётся пустым, но `hasNextQuestion()` всё равно возвращает `true`.

**Строки:**
- `QuizGameImpl.kt:341-344` — `nextStep()` не проверяет `quizList.size`
- `QuizGameImpl.kt:284` — `quizList[step.value]` без bounds check
- `DatasourceEffectHandler.kt:110-111` — вызов `hasNextQuestion()` + `nextQuestion()`, доверяет `hasNextQuestion()`
> 📎 guide: docs/guides/mate-framework.md — "EffectHandler выполняет сайд-эффекты, генерирует новые сообщения через consumer"

## 2. Тип бага

**simple**

Обоснование:
- Затрагивает 1 файл (`QuizGameImpl.kt`)
- Фикс очевиден: `nextStep()` должен проверять `quizList.isEmpty()` перед переходом из `Pending` в `Started`
- Спека на QuizChat отсутствует (в `docs/features-spec/` нет файла по QuizChat). Фикс не меняет описанное в спеке поведение — он добавляет проверку граничного условия (пустой список), которая логически следует из текущего кода

`needs_spec_update = false`

## 3. Затронутые спеки

Спеки по теме не найдены. В `docs/features-spec/README.md` раздел "Квиз-чат (QuizChat)" перечислен без ссылки на файл — спека не существует.

## log_messages
- Triage завершён: баг классифицирован как simple, корневая причина — `nextStep()` не проверяет пустой `quizList`
- Спека на QuizChat отсутствует, `needs_spec_update = false`

_model: claude-opus-4-6_
