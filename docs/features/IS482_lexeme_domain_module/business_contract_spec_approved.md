# Approved findings (business_contract_spec, iter 1)

Все minor — точные правки сигнатур, исправляются прямо в `business_contract_spec.md` без design-решений.

## F001 [architect] minor

**Description:** `deleteLexeme(lexemeId: Long): List<Lexeme>?` в таблице WordCardUseCase — пропущен `wordId`.

**Что нужно:** В разделе `## UseCase / WordCardUseCase` строку с `deleteLexeme` исправить на `deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?`.

## F002 [architect] minor

**Description:** `getWriteQuiz(...): List<WriteQuiz>` в QuizChatUseCase — метод называется иначе.

**Что нужно:** В разделе `## UseCase / QuizChatUseCase` упоминание метода поправить на `getRandomWriteQuizList(limit: Int, maxGrade: Int, dictionaryId: Long): List<WriteQuiz>`.
