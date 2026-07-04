# Review: business_contract

## Findings

(пусто — все iter 1 findings закрыты, новых блокеров нет)

### Closure verification (iter 1 findings)

- **F1 (`term.dictionary.componentTypes` non-existent)** — закрыт. Контракт:
  - расширяет domain `Term` новым полем `dictionaryId: Long` (line 22), источник
    `WordApiEntity.dictionaryId` (verified Read `core/core-db-api/.../WordApiEntity.kt:7` — поле существует);
  - меняет `Msg.WordLoaded(word: Term)` → `Msg.WordLoaded(word: Term, componentTypes: List<ComponentType>)`
    (line 55);
  - добавляет public UseCase `getComponentTypes(dictionaryId: Long): List<ComponentType>` (line 154);
  - handler `LoadWord` делает sequential pre-fetch `getTermById` → `getComponentTypes(term.dictionaryId)`
    → `Msg.WordLoaded(term, types)` (lines 81-85);
  - reducer вычисляет `hasDefinitionComponent = componentTypes.any { it.systemKey == null && it.name == "Definition" }`
    (line 62).
  - Цепочка реальная, опирается на verified ApiEntity-поле.

- **F2 (`getQuizConfig` returned `QuizConfigApiEntity?`)** — закрыт. Контракт line 199-203:
  ```kotlin
  suspend fun getQuizConfig(dictionaryId: Long, quizMode: String = "write"): QuizConfig?
  ```
  KDoc явно фиксирует «Возвращает **domain** QuizConfig (AGG-10) — UseCase port не пробрасывает
  ApiEntity наружу. Impl делает `apiEntity.toDomain()`». Соответствует walkthrough §16 и
  конвенции `getRandomWriteQuizList: List<WriteQuiz>`.

- **F3 (MIN-9 `restoreLexeme` atomic compound INSERT)** — закрыт. Контракт lines 167-184:
  - добавляет DAO default-method `WordDao.addLexemeWithComponents(lexemeDb, dictionaryId,
    components: List<ComponentValueDb>)` под `@Transaction` (generic вариант существующего
    `addLexemeWithQuiz`);
  - одна транзакция: INSERT lexemes + INSERT write_quiz + INSERT N component_values
    (translation built-in + опционально definition user-defined);
  - FK violation на любом шаге → rollback всей транзакции — рассинхрон «двух транзакций»
    из walkthrough §5 исключён;
  - compound DAO-метод используется **только** из `restoreLexeme` impl (один callsite),
    наружу как UseCase не выставлен — корректное узкое скопирование под undo flow.
  - sig `restoreLexeme` сохранена (B4/C2 shim).

- **F4 (rejected/clarified)** — контракт явно отвечает (lines 92-93): types нужны в reducer'е
  через `Msg`, не внутри UseCase impl → public `getComponentTypes` обоснован (option a отвергнута).
  Согласовано.

- **F5 (walkthrough opacity на `dictionaryId` source)** — закрыт. Контракт line 22-24
  явно ссылается на `WordApiEntity.dictionaryId` (verified Read) и меняемый маппер
  `TermApiEntity.toDomainEntity()` (`WordCardUseCaseImpl.kt:199-211`, verified Read —
  маппер реально на этих строках).

## Verdict

verdict: approved

_model: claude-opus-4-7[1m]_
