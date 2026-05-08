# Task

## IS461. Crash: QuizGameImpl IndexOutOfBoundsException on empty quiz list

Краш в продакшне. Версия 0.1.1.

## Стектрейс

```
Fatal Exception: java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
    at java.util.ArrayList.get(ArrayList.java:437)
    at me.apomazkin.quiz.chat.quiz.QuizGameImpl.getQuiz(QuizGameImpl.kt:284)
    at me.apomazkin.quiz.chat.quiz.QuizGameImpl.getNextQuestion(QuizGameImpl.kt:210)
    at me.apomazkin.quiz.chat.quiz.QuizGameImpl.nextQuestion(QuizGameImpl.kt:57)
    at me.apomazkin.quiz.chat.logic.DatasourceEffectHandler$runEffect$10.invokeSuspend(DatasourceEffectHandler.kt:111)
```

## Суть

`QuizGameImpl.getQuiz()` обращается к `ArrayList.get(0)` при пустом списке. Вызывается из цепочки `nextQuestion()` → `getNextQuestion()` → `getQuiz()`.

## Подробности

Полный стектрейс: `docs/crashes/2026-05-07_quiz-empty-list.md`
