# Approved findings — contract_io v4, итерация 4

**2 critical + 5 minor approved.** Streak reset.

## Critical (2)

### F045 — Cascade-delete лексемы в data-слое

**Решение conductor'а: опция 2 (cascade-aware Msg).**

UseCase API расширяется sealed-result типами:

```kotlin
sealed interface RemoveTranslationResult {
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveDefinitionResult {
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}

interface WordCardUseCase {
    // ...
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?
    suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?
}
```

Семантика возврата:
- `null` — БД-ошибка / failure.
- `TranslationRemoved(lexeme)` — перевод удалён, лексема осталась.
- `LexemeCascadeRemoved` — последняя суб-сущность удалена, лексема каскадно ушла.

**Новый Datasource Msg:**
```kotlin
data class LexemeCascadeRemoved(val lexemeId: Long) : Msg
```

**Reducer-логика `LexemeCascadeRemoved`:**
```kotlin
// Удалить лексему из списка
state.copy(lexemeList = state.lexemeList.filterNot { it.id == lexemeId })
```

**Handler паттерн для `RemoveTranslation`:**
```kotlin
DatasourceEffect.RemoveTranslation -> withContext(Dispatchers.IO) {
    try {
        when (val result = wordCardUseCase.deleteLexemeTranslation(effect.lexemeId)) {
            null -> consumer(Msg.ShowNotification("Не удалось удалить перевод"))
            is RemoveTranslationResult.TranslationRemoved ->
                consumer(Msg.RefreshTranslation(effect.lexemeId, result.lexeme.translation?.value))
            RemoveTranslationResult.LexemeCascadeRemoved ->
                consumer(Msg.LexemeCascadeRemoved(effect.lexemeId))
        }
    } catch (e: Exception) {
        consumer(Msg.ShowNotification("Не удалось удалить перевод"))
    }
}
```

Симметрично для `RemoveDefinition`.

**Pessimistic Remove path в `CommitTranslationEdit` ветвь 1:**
reducer (в contract_ui_msg v3) уже оставляет `translation` non-null до прихода Msg. Когда приходит:
- `RefreshTranslation(lexemeId, null)` → reducer обнулит `translation`.
- `LexemeCascadeRemoved(lexemeId)` → reducer удалит лексему из списка целиком.

Оба сценария корректны без изменения reducer-логики `CommitTranslationEdit`. **Feedback в contract_ui_msg не требуется** — UI Msg формы не меняются.

**Cascade применим только к `RemoveTranslation`/`RemoveDefinition`.** В `UpdateLexemeTranslation`/`UpdateLexemeDefinition` cascade не возникает (мы пишем непустое значение, не null).

### F046 — `deleteLexeme(wordId, lexemeId)` resync через full term fetch

UseCase impl не имеет dedicated `getLexemesByWord` endpoint. Resync — через `termApi.getTermById(wordId).lexemes`.

Решение:
```kotlin
override suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>? {
    return try {
        lexemeApi.deleteLexeme(lexemeId)
        termApi.getTermById(wordId)?.lexemes?.map { it.toDomainEntity() }
    } catch (e: Exception) { null }
}
```

В контракте `contract_io` явно зафиксировать в § «Расхождения spec ↔ code»:
> `deleteLexeme(wordId, lexemeId): List<Lexeme>?` использует `termApi.getTermById(wordId)` для resync списка (full term fetch). Точечного `getLexemesByWord` endpoint'а нет — добавление out-of-scope IS479. Trade-off: одна лишняя query, но переиспользует существующий API.

Снять формулировку «точечный resync без full re-fetch» в обосновании.

## Minor (5)

### F047 — Defensive null-после-success в Update

Симметрично F044. После success `addLexemeTranslation`, если `lexeme.translation == null` — это нарушение контракта impl, но из соображений согласованности state↔БД лучше шлать `RefreshTranslation(lexemeId, null)` (обнулить state в согласие с БД), чем `ShowNotification` (state остаётся optimistic-non-null).

Обновить guard:
```kotlin
when {
    lexeme == null -> consumer(Msg.ShowNotification("Не удалось сохранить перевод"))
    else -> consumer(Msg.RefreshTranslation(effect.lexemeId, lexeme.translation?.value))
}
```
И ремарка: «Если `lexeme.translation == null` после success Update (contract violation в impl) — reducer обнулит state. Это безопаснее, чем удержание устаревшего origin».

### F048 — Терминологическая неточность «FIFO»

Заменить «FIFO consumer-вызовов» на «sequential execution в одной coroutine handler'а — два `consumer(msg)` вызова выполняются последовательно в порядке кода».

### F049 — `addLexeme` failure misframing

В EC `CreateLexeme` уточнить:
> Failure: либо `null` возврат (БД insert упал), либо `IllegalStateException("Dictionary not found")` (нет `CURRENT_DICTIONARY_ID_LONG` в Prefs или dictionary в БД отсутствует). Handler не различает причины — единый `ShowNotification("Не удалось создать лексему")`. Trade-off: диагностическая бедность сообщения; out-of-scope IS479.

### F050 — `RefreshLexemeList` ordering

После `RemoveLexeme` reducer шлёт `RefreshLexemeList(lexemes)`. Зафиксировать ordering:
> `lexemeList` упорядочен по `addDate ASC` (порядок Room-query `getLexemesByTermId`). После `RemoveLexeme` оставшиеся лексемы сохраняют изначальный порядок. Если БД-query сменит ORDER BY — это будет breaking change UI, потребуется явная синхронизация.

### F051 — `UpdateWord` term=null edge case

Заменить «не шлём ничего — state остаётся optimistic; лечится следующим LoadWord» на:
> При `updateWord==true ∧ getTermById==null` — теоретически невозможно (БД consistency после успешного update), но defensive: handler шлёт `ShowNotification("Не удалось получить обновлённое слово")`. State остаётся optimistic — рассинхрон с БД до выхода и повторного захода на экран. Accepted trade-off (IS479 не вводит retry-механику).

---

## Задача итерации 5

Перепиши `contract_io.md` v4 → v5, закрыв все 7 findings.

**Особенно F045:**
- Ввести новые sealed-result типы `RemoveTranslationResult` / `RemoveDefinitionResult` в § «Расхождения spec ↔ code» (изменение UseCase API).
- Новый Datasource Msg `LexemeCascadeRemoved(lexemeId)` + reducer-логика.
- Handler-pseudocode для `RemoveTranslation` / `RemoveDefinition` обновлён под sealed result.
- В EC явно зафиксировать существующую cascade-семантику data-слоя как принятую.
- В таблице «Изменения в API WordCardUseCase» добавить:
  - `deleteLexemeTranslation(lexemeId): Unit` → `(lexemeId): RemoveTranslationResult?` (sealed).
  - `deleteLexemeDefinition(lexemeId): Unit` → `(lexemeId): RemoveDefinitionResult?` (sealed).

**Feedback в contract_ui_msg НЕ требуется** — UI Msg формы и reducer-логика веток `Commit*Edit`/`Remove*` остаются прежними.

После — кратко (3-5 строк): что закрыто, какие новые типы введены, есть ли feedback.
