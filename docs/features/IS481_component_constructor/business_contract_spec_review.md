# Review: business_contract_spec.md

(ID сквозной per-feature — F078+ продолжение после F077.)

## Итерация 1 (2026-06-16T13:30:00-06:00)

### F078 [architect] critical

**Description:** `AffectedQuizConfig.quizMode: QuizMode` ссылается на тип `QuizMode`, который в проекте не определён (в БД и API хранится как `String`). Spec — срез финального состояния, не может оставлять несуществующий тип без import/определения. Унаследовано из contract Open Q #5, не разрешено.

**Status:** approved

**Verdict:** Тип `QuizMode` в проекте отсутствует (grep не находит ни declaration ни usages кроме test name). Resolve: использовать `String` (по data layer convention) или определить enum `QuizMode` в `:modules:domain:lexeme`.

### F079 [architect] minor

**Description:** Domain-типы (`Scope`, `CreateOutcome`, `RenameOutcome`, `DeleteOutcome`, `UserDefinedTypesSnapshot`, `ComponentUsage`, `DeletionImpact`) объявлены в `me.apomazkin.components_manager.logic` и используются как контрактные типы UseCase для **обоих** экранов (включая `per_dictionary_components`). Screen-модуль зависит от логических типов другого screen-модуля → нарушение слойности.

**Status:** approved

**Verdict:** `PerDictionaryComponentsUseCase` использует types из `components_manager.logic` → cross-screen dependency. Fix: переместить domain-shared types в `:modules:domain:lexeme` (там уже `ComponentType` / `ComponentValueData` — естественное место).

### F080 [architect] minor

**Description:** `CreateOutcome` содержит и обобщающий `NameTaken(scope)`, и дискретные `SameScopeCollision` / `CrossScopeCollision`. В spec'е (срез) ADT-ветка либо `NameTaken`, либо pair discrete never reached. Mёртвая ветка.

**Status:** approved

**Verdict:** Sealed overlapping → dead branches. Drop `NameTaken(scope)`, оставить только discrete `SameScopeCollision` / `CrossScopeCollision`.

### F081 [architect] minor

**Description:** `NameError.ScopeCollision` объявлен как обобщение для UI feedback, но в reducer-таблице используется только при `CreateResult(NameTaken(scope))`. Если UseCase возвращает discrete cases — `ScopeCollision` dead variant.

**Status:** approved

**Verdict:** Dependent on F080. После drop `NameTaken` — drop `NameError.ScopeCollision` тоже; reducer mapping напрямую: `SameScopeCollision` / `CrossScopeCollision` → `NameError.SameScopeCollision` / `NameError.CrossScopeCollision` (или unify в `NameError.Collision(scope)`).

## Итоги итерации 1

- **Approved:** 1 critical (F078) + 3 minor (F079, F080, F081).
- **Rejected:** 0.
- **Решение:** есть approved critical → reset minor_only_streak, repeat iter 2.

---

## Итерация 2 (2026-06-16T14:30:00-06:00)

### PASS [architect]

(verify: quizMode=String, shared types в lexeme package, NameTaken removed, ScopeCollision removed)

## Итоги итерации 2

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → review_passed = true.
- `changes_made = true` → repeat iter 3 (clean check).

---

## Итерация 3 (2026-06-16T14:45:00-06:00)

### PASS [architect]

(clean check после iter 2)

## Итоги итерации 3

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → review_passed = true.
- `changes_made = false` → execute_repeat exit → шаг `business_contract_spec → done`.
