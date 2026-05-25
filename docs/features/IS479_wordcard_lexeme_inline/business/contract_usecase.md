# contract_usecase v1

> Финал `WordCardUseCase` interface для IS479. Источник изменений — `contract_io.md` v7, § «Расхождения spec ↔ code → Изменения в API `WordCardUseCase`». Новых решений в этом артефакте нет — только сборка финального контракта + per-method документация + явная фиксация diff vs текущий код.

## Режим работы

**Режим 3** — шаг 3 (`contract_io` v7) + существующий `WordCardUseCase.kt` interface (`modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt`).

## UseCase

```kotlin
package me.apomazkin.wordcard.deps

import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {

    // ─── Команды: Word level (Datasource Effects) ───

    /** Cold load слова + лексем. */
    suspend fun getTermById(wordId: Long): Term?            // Effect: LoadWord

    /** Удаляет слово; БД каскадно (FK) удаляет лексемы и суб-сущности. */
    suspend fun deleteWord(wordId: Long): Int               // Effect: RemoveWord

    /** Обновляет value слова. */
    suspend fun updateWord(wordId: Long, value: String): Boolean   // Effect: UpdateWord

    // ─── Команды: Lexeme level (Datasource Effects) ───

    /** Удаляет лексему; возвращает актуальный список лексем слова. */
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?   // Effect: RemoveLexeme

    /**
     * Создаёт лексему (если lexemeId == null) или обновляет translation существующей.
     * При lexemeId == null impl ОБЯЗАН выполнить insert лексемы + insert translation
     * в одной Room-транзакции (см. § Atomicity contract).
     */
    suspend fun addLexemeTranslation(
        wordId: Long,
        lexemeId: Long?,
        translation: String,
    ): Lexeme?                                              // Effect: UpdateLexemeTranslation

    /** Удаляет translation. Sealed result различает non-cascade и cascade-removal лексемы. */
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?   // Effect: RemoveTranslation

    /** Симметрично addLexemeTranslation; lexemeId == null ⇒ insert лексемы + definition в одной Room-транзакции. */
    suspend fun addLexemeDefinition(
        wordId: Long,
        lexemeId: Long?,
        definition: String,
    ): Lexeme?                                              // Effect: UpdateLexemeDefinition

    /** Симметрично deleteLexemeTranslation. */
    suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?   // Effect: RemoveDefinition
}

// ─── Sealed result types ───

sealed interface RemoveTranslationResult {
    /** Translation удалён в БД; лексема осталась (definition non-null). Содержит обновлённую лексему. */
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    /** Translation был последней суб-сущностью; лексема каскадно удалена data-слоем. */
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveDefinitionResult {
    /** Definition удалён в БД; лексема осталась (translation non-null). Содержит обновлённую лексему. */
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    /** Definition был последней суб-сущностью; лексема каскадно удалена data-слоем. */
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}
```

**Реактивных источников нет** — `WordCardUseCase` экспонирует только `suspend`-методы; subscriber'ы отсутствуют (см. `contract_io` v7 § Subscribers и § Проверка реактивности).

## Покрытие 1-к-1

8 методов interface ↔ 8 `DatasourceEffect`-вариантов (`contract_io` v7 § Effects):

| UseCase метод | Effect | Return Msg (success/failure) |
|---|---|---|
| `getTermById(wordId)` | `LoadWord(wordId)` | `WordLoaded(term)` / `WordNotFound` |
| `deleteWord(wordId)` | `RemoveWord(wordId)` | `NavigateBack` (shared) / `ShowNotification` |
| `updateWord(wordId, value)` | `UpdateWord(wordId, value)` | `RefreshWord(term)` / `ShowNotification` |
| `deleteLexeme(wordId, lexemeId)` | `RemoveLexeme(wordId, lexemeId)` | `RefreshLexemeList(lexemes)` / `ShowNotification` |
| `addLexemeTranslation(wordId, lexemeId?, translation)` | `UpdateLexemeTranslation(wordId, lexemeId?, translation)` | `RefreshTranslation(lexemeId, translation?)` / `ShowNotification` |
| `deleteLexemeTranslation(lexemeId)` | `RemoveTranslation(lexemeId)` | `RefreshTranslation(lexemeId, null)` / `LexemeCascadeRemoved(lexemeId)` / `ShowNotification` |
| `addLexemeDefinition(wordId, lexemeId?, definition)` | `UpdateLexemeDefinition(wordId, lexemeId?, definition)` | `RefreshDefinition(lexemeId, definition?)` / `ShowNotification` |
| `deleteLexemeDefinition(lexemeId)` | `RemoveDefinition(lexemeId)` | `RefreshDefinition(lexemeId, null)` / `LexemeCascadeRemoved(lexemeId)` / `ShowNotification` |

Subscriber'ов нет — Subscriber-блок отсутствует (см. `contract_io` v7).

## Документация per method

### `getTermById(wordId: Long): Term?`

- **Что делает:** одноразовый cold load слова из БД. `Term.lexemeList` уже содержит лексемы — отдельный запрос лексем не нужен.
- **Когда вызывается:** `initEffects` ViewModel'я (effect `LoadWord`), один раз при создании Mate.
- **Failure:** `null` — слово не найдено (ID не существует в БД либо БД-ошибка, поглощённая impl). Handler конвертирует в `WordNotFound`, reducer-ветка → `NavigationEffect.Back` (silent exit).

### `deleteWord(wordId: Long): Int`

- **Что делает:** удаляет слово. Лексемы и суб-сущности удаляются БД-каскадом (Room FK ON DELETE CASCADE).
- **Когда вызывается:** effect `RemoveWord` (после confirmation-диалога).
- **Возврат:** Int — число удалённых строк. Для существующего слова всегда `1`. Handler не интерпретирует значение — success path определяется отсутствием исключения.
- **Failure:** exception (handler ловит) ⇒ `ShowNotification("Не удалось удалить слово")`.

### `updateWord(wordId: Long, value: String): Boolean`

- **Что делает:** обновляет `value` слова.
- **Когда вызывается:** effect `UpdateWord` (после `CommitWordChanges` ветви Update).
- **Failure:**
  - `false` ⇒ `ShowNotification("Не удалось сохранить")`.
  - Exception ⇒ то же.
- **Семантика handler'а после success:** делает повторный `getTermById(wordId)` для resync (БД могла нормализовать value — trim/case-folding). Null после success — defensive `ShowNotification("Не удалось получить обновлённое слово")` (F051).

### `deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?`

- **Что делает:** удаляет лексему; БД каскадом (FK) удаляет translation/definition. Возвращает **актуальный** список лексем слова после удаления (resync через `termApi.getTermById(wordId).lexemeList`).
- **Когда вызывается:** effect `RemoveLexeme` (UI Msg `RemoveLexeme` для лексем с реальным id; для `NOT_IN_DB`-лексем effect не шлётся — локальный nullify в reducer).
- **Idempotent contract (F039):** для уже отсутствующей лексемы impl возвращает текущий список слова (`List<Lexeme>` без неё), **не `null`**.
- **Failure:** `null` — реальная БД-ошибка; handler конвертирует в `ShowNotification("Не удалось удалить значение")`.
- **Почему `wordId` параметром (а не выводится из lexeme):** F040 ит.4 — `Lexeme` entity не имеет поля `wordId`, явный параметр чище чем дополнительный lookup.
- **Почему `List<Lexeme>?` вместо `Boolean`:** F046 ит.5 — точечного `getLexemesByWord` в `CoreDbApi.LexemeApi` нет (добавление out-of-scope IS479); resync через full term fetch экономит API-расширение data-слоя. Merge-by-id в reducer даёт робастный resync UI.

### `addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?`

- **Что делает:**
  - `lexemeId == null` (первый Commit для `NOT_IN_DB`-лексемы) ⇒ impl делает `lexemeApi.addLexeme(wordId)` (internal-helper после удаления из public API) + `lexemeApi.updateLexemeTranslation(newId, translation)` **в одной Room-транзакции** + возвращает `lexemeApi.getLexemeById(newId)` как domain `Lexeme`.
  - `lexemeId != null` ⇒ upsert translation существующей лексемы; возвращает обновлённую лексему.
- **Когда вызывается:** effect `UpdateLexemeTranslation` (UI Msg `CommitTranslationEdit` ветвь 4).
- **Возврат:** `Lexeme` с реальным `lexemeId` (новый — для null-кейса; существующий — для non-null). Handler шлёт `RefreshTranslation(lexemeId = lexeme.lexemeId.id, translation = lexeme.translation?.value)`. Reducer заменяет `NOT_IN_DB → реальный id` в `lexemeList` при первом Commit (см. `contract_ui_msg` v3.2).
- **Failure:** `null` либо exception ⇒ handler шлёт `ShowNotification("Не удалось сохранить перевод")`. Особенно важно для NOT_IN_DB first-Commit failure (нет dictionary в Prefs — симметрично F049) — без try/catch UI остался бы навсегда заблокирован через `isPendingDbOp`.
- **Почему `lexemeId: Long?`** (а не non-null + отдельный `createLexemeWithTranslation`): F023 ит.6 отменён. Один метод закрывает оба пути (create + update); UI не нужно знать про два разных API. См. § Atomicity contract.

### `deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?`

- **Что делает:** удаляет translation. Семантика возврата:
  - `TranslationRemoved(lexeme)` — translation удалён в БД, лексема осталась (definition non-null). `lexeme` содержит обновлённое БД-состояние (`translation = null`, `definition` non-null).
  - `LexemeCascadeRemoved` — translation был единственной суб-сущностью; БД-инвариант F052 («лексема имеет ≥ 1 суб-сущность») заставил data-слой каскадно удалить саму лексему. Cascade — внутренний механизм impl (`WordCardUseCaseImpl.canRemoveTranslation` + `lexemeApi.deleteLexeme(id)`), не БД-FK.
  - `null` — реальная БД-ошибка.
- **Когда вызывается:** effect `RemoveTranslation` — два UI-источника: явный пункт меню `RemoveTranslation(lexemeId)` либо pessimistic Remove ветка `CommitTranslationEdit` (при `edited.isBlank() ∧ origin.isNotEmpty()`). Effect шлётся ТОЛЬКО для реальных id; для `NOT_IN_DB` — локальный nullify в reducer.
- **Idempotent contract (F033, F061):**
  - Для лексемы у которой translation уже null — impl возвращает `TranslationRemoved(lexeme)` с текущей лексемой (idempotent no-op, не `null`).
  - Для уже каскадно удалённой лексемы (`getLexemeById(lexemeId) == null` в impl) — поведение допустимо двойственное: `LexemeCascadeRemoved` (silent reducer no-op) либо `null` (snackbar). Обе ветки безопасны на UI-стороне (reducer `filterNot` идемпотентен). Конкретное решение — на impl-шаге, не блокер IS479.
- **Почему sealed result, а не `Lexeme?`:** F045 ит.5 — handler должен явно знать, исчезла ли лексема целиком. Возврат `Lexeme?` со значением `null` неоднозначен (failure vs cascade). Sealed различает три исхода без неявных кодов.

### `addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?`

- **Что делает / failure / atomicity:** структурно симметрично `addLexemeTranslation` (см. выше). Effect `UpdateLexemeDefinition`. Failure ⇒ `ShowNotification("Не удалось сохранить определение")`.

### `deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?`

- **Что делает / failure / idempotent:** структурно симметрично `deleteLexemeTranslation` (см. выше). Effect `RemoveDefinition`. Failure ⇒ `ShowNotification("Не удалось удалить определение")`.

## Расхождения spec ↔ code

**Режим 3** — спека отсутствует. Сверка — с текущим `WordCardUseCase.kt`:

```kotlin
// БЫЛО (текущий код)
interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(lexemeId: Long): Boolean
    suspend fun addLexeme(wordId: Long): Lexeme?
    suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long)
    suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?
    suspend fun deleteLexemeDefinition(lexemeId: Long)
}
```

### Diff vs текущий код

| Метод | Было | Стало | Обоснование |
|---|---|---|---|
| `getTermById` | `suspend fun getTermById(wordId: Long): Term?` | **без изменений** | — |
| `deleteWord` | `suspend fun deleteWord(wordId: Long): Int` | **без изменений** | — |
| `updateWord` | `suspend fun updateWord(wordId: Long, value: String): Boolean` | **без изменений** | — |
| `deleteLexeme` | `suspend fun deleteLexeme(lexemeId: Long): Boolean` | `suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?` | **F040 ит.4:** `wordId` явным параметром (`Lexeme` entity не несёт `wordId`). **F046 ит.5:** resync через `termApi.getTermById(wordId).lexemeList` — экономит расширение `LexemeApi`. **F039:** idempotent no-op возвращает актуальный список, не `null`. Boolean ⇒ `List<Lexeme>?` устраняет дополнительный re-fetch в handler'е и даёт merge-by-id resync. |
| `addLexeme` | `suspend fun addLexeme(wordId: Long): Lexeme?` | **УДАЛЁН** | См. § «Удалённый метод» ниже. |
| `addLexemeTranslation` | `suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?` | **без изменений** (сигнатура) | Семантика расширена: при `lexemeId == null` impl ОБЯЗАН создать лексему + translation атомарно. **F023 отменён ит.6:** nullable `lexemeId` — не dead code, валидный путь под NOT_IN_DB-архитектуру. См. § Atomicity contract. |
| `deleteLexemeTranslation` | `suspend fun deleteLexemeTranslation(lexemeId: Long)` (Unit) | `suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?` | **F045 ит.5:** cascade-aware sealed result (`TranslationRemoved` / `LexemeCascadeRemoved`). Unit ⇒ Result? чтобы handler различил два success-исхода (translation удалён vs лексема каскадно ушла) + failure. **F033/F061:** idempotent contract документирован. |
| `addLexemeDefinition` | `suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?` | **без изменений** (сигнатура) | Симметрично `addLexemeTranslation`. |
| `deleteLexemeDefinition` | `suspend fun deleteLexemeDefinition(lexemeId: Long)` (Unit) | `suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?` | Симметрично `deleteLexemeTranslation`. |

### Новые типы

```kotlin
sealed interface RemoveTranslationResult {
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveDefinitionResult {
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}
```

Размещение пакетом — `me.apomazkin.wordcard.deps` (вместе с `WordCardUseCase`). Domain-типы, не feature-обёртки — соответствует п. 2 правил моделирования.

### Удалённый метод

`addLexeme(wordId: Long): Lexeme?` — **удалён из public API** UseCase.

- **Почему:** **F052 ит.6** — БД-инвариант «лексема в БД имеет ≥ 1 суб-сущность (translation либо definition non-null)». Прямой вызов `addLexeme` нарушает инвариант: создаёт пустую лексему без суб-сущностей. Это валидно только как **внутренний шаг** перед `updateLexemeTranslation` / `updateLexemeDefinition` под транзакцией.
- **Куда переехал:** становится `private`/`internal` helper-методом UseCase impl (либо вызов `lexemeApi.addLexeme(wordId)` инлайнится в impl `addLexemeTranslation`/`addLexemeDefinition`). Побочка `quizApi.addWriteQuiz(...)`, которая сейчас сидит в `addLexeme`, тоже остаётся в impl (на impl-шаге).
- **Архитектурное последствие:** `DatasourceEffect.CreateLexeme` снят (см. `contract_io` v7). UI Msg `CreateLexeme` теперь только локально расширяет state с `NOT_IN_DB`-лексемой, без БД-эффекта. БД-запись создаётся при первом `CommitTranslationEdit` / `CommitDefinitionEdit` с `lexemeId == null`.

## Atomicity contract

**Применимо к** `addLexemeTranslation(wordId, lexemeId = null, translation)` и `addLexemeDefinition(wordId, lexemeId = null, definition)`.

**Требование (F064 ит.7):** impl ОБЯЗАН выполнять `addLexeme(wordId)` + `updateLexeme*<newId, value>` в одной Room-транзакции (`@Transaction`-метод DAO или явная `withTransaction { ... }` обёртка).

**Почему:**
- Без транзакции: failure после успешного `addLexeme(wordId)`, но до `updateLexeme*` оставит **пустую лексему в БД** (translation = null ∧ definition = null) — нарушение БД-инварианта F052. Эта лексема будет видна другим readers (`getTermById`), сломает UI на следующем cold load.
- С транзакцией: либо обе строки в БД (success), либо ни одной (failure rollback). Дополнительно — никакой полу-готовый snapshot не утечёт к concurrent reader'ам до commit транзакции.

**Гарантия для UI:** после `consumer(Msg.RefreshTranslation(...))` БД-инвариант F052 удерживается. Если impl ловит exception до commit — handler получает `null` / exception → шлёт `ShowNotification(...)` → state не меняется (NOT_IN_DB-лексема остаётся локально, пользователь может повторить Commit). См. EC8 в `contract_io` v7.

**Ответственность:** impl-шаг (data-слой / `WordCardUseCaseImpl` + DAO). Контракт UI ничего не предполагает о деталях транзакции — только наблюдает success либо failure.

## Лог итераций

### ит.1 (2026-05-19T21:40:00-0600) — финал по contract_io v7

- Финальный interface зафиксирован: 8 методов (без `addLexeme`), nullable `lexemeId` в `addLexemeTranslation` / `addLexemeDefinition`, sealed result для `delete*` суб-сущностей.
- Новые типы: `RemoveTranslationResult` / `RemoveDefinitionResult` в пакете `me.apomazkin.wordcard.deps`.
- Удалённый метод: `addLexeme` (становится internal helper impl-уровня).
- Atomicity contract явно зафиксирован для null-кейсов `addLexemeTranslation` / `addLexemeDefinition`.
- Subscriber'ов нет (см. `contract_io` v7); реактивные источники в interface отсутствуют.

---

_model: claude opus 4.7 (1M context)_
