# Approved findings — business_contract_spec.md, iteration 1

4 findings (1 critical + 3 minor).

## F078 [critical] — unresolved тип `QuizMode`

`AffectedQuizConfig.quizMode: QuizMode` ссылается на несуществующий тип. Spec = срез финального состояния → блокер.

**Что исправить:** в **business_contract_spec.md** заменить `quizMode: QuizMode` на `quizMode: String` (matches data layer — БД хранит string). Также синхронизировать business_contract.md (если содержит `QuizMode`).

Альтернатива: определить enum `QuizMode` в `:modules:domain:lexeme` со значениями matching existing string values. Это **больший scope** — выбираем `String` как minimal fix.

## F079 [minor] — types в screen-package → cross-screen leak

Domain-shared types живут в `me.apomazkin.components_manager.logic`. PerDictionary UseCase зависит → нарушение слойности.

**Что исправить:** переместить types в `:modules:domain:lexeme` (там уже `ComponentType`, `ComponentValueData` — естественное место). Конкретно:

- `Scope` (sealed)
- `CreateOutcome` (sealed)
- `RenameOutcome` (sealed)
- `DeleteOutcome` (sealed)
- `UserDefinedTypesSnapshot` (data class)
- `ComponentUsage` (data class)
- `DeletionImpact` (data class)
- `AffectedQuizConfig` (data class)
- `PerDictionarySnapshot` (data class)

Package: `me.apomazkin.lexeme` (либо `me.apomazkin.lexeme.components` если нужна группировка).

В spec — обновить imports / package декларации. Конкретные file:line при design_tree.

## F080 [minor] — overlapping variants в `CreateOutcome`

`NameTaken(scope)` дублирует `SameScopeCollision` / `CrossScopeCollision` → dead branch.

**Что исправить:** удалить `NameTaken(scope) : CreateOutcome` из sealed. Оставить:

```kotlin
sealed interface CreateOutcome {
    data class Success(val created: List<ComponentType>) : CreateOutcome
    data object SameScopeCollision : CreateOutcome
    data object CrossScopeCollision : CreateOutcome
    data object NameEmpty : CreateOutcome
    data object NameTooLong : CreateOutcome
    data object Failure : CreateOutcome
}
```

В reducer-таблице — удалить ветку `Msg.CreateResult(NameTaken(scope))`, оставить отдельные ветки `Msg.CreateResult(SameScopeCollision)` и `Msg.CreateResult(CrossScopeCollision)`.

## F081 [minor] — dead variant `NameError.ScopeCollision`

После fix F080 — variant unused.

**Что исправить:** удалить `NameError.ScopeCollision` из sealed. Reducer mapping:

```kotlin
sealed interface NameError {
    data object Empty : NameError
    data object TooLong : NameError
    data object SameScopeCollision : NameError
    data object CrossScopeCollision : NameError
}
```

Reducer: `Msg.CreateResult(CreateOutcome.SameScopeCollision)` → `NameError.SameScopeCollision`. Аналогично для cross.

Альтернатива (для UI simplicity): unified `NameError.Collision(scope: Scope)`. Решение по choice — на business_design_tree, для текущего fix используем discrete cases (parity с outcome sealed).
