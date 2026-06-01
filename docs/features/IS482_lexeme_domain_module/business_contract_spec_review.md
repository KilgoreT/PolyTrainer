# Review: business_contract_spec

## Итерация 1 (2026-05-30T19:03:24-0600)

### F001 [architect] minor

**Description:** Таблица WordCardUseCase указывает `deleteLexeme(lexemeId: Long): List<Lexeme>?` — пропущен параметр `wordId`. Реальная сигнатура `deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?`.

**Status:** approved

**Verdict:** Реальная сигнатура — параметр `wordId` пропущен в спеке.

### F002 [architect] minor

**Description:** В разделе QuizChatUseCase упомянут метод `getWriteQuiz(...): List<WriteQuiz>`, которого нет. Реальное имя `getRandomWriteQuizList(limit, maxGrade, dictionaryId): List<WriteQuiz>`.

**Status:** approved

**Verdict:** Метод называется `getRandomWriteQuizList`, имени `getWriteQuiz` в интерфейсе нет.

## Итерация 2 (2026-05-30T19:05:09-0600)

### PASS [architect]

F001 и F002 закрыты, точные сигнатуры подтверждены Read через `WordCardUseCase.kt:10` и `QuizChatUseCase.kt:9-13`. Новых проблем нет.

**Решение:** PASS → `raw_findings=[]` → петля закрыта без inquisitor. `review_passed=true`.
