# Review: infra_implement.md

(ID сквозной per-feature — продолжение нумерации F076+ после infra_test_review.md F075. Deviation от per-file контракта review module, осознанная для cross-step references.)

## Итерация 1 (2026-06-16T07:45:00-06:00)

### PASS [architect]

### F076 [senior] minor

**Description:** В `modules/screen/components_manager/build.gradle.kts` и `modules/screen/per_dictionary_components/build.gradle.kts` объявлены 5 project-deps (`:modules:domain:lexeme`, `:modules:widget:component_widgets`, `:modules:core:tools`, `:modules:core:logger`, `:core:core-resources`), которые **ни одним import не используются** в текущих placeholder-исходниках. Нарушает YAGNI на infra-уровне.

**Status:** approved

**Verdict:** пять project-deps объявлены в обоих build.gradle.kts, но ни одного import не используется в текущих placeholder-исходниках — YAGNI-нарушение, не schema-readiness. Убрать неиспользуемые из gradle либо зафиксировать в `infra_implement.md` § Известные TODO с обоснованием pre-declare.

### F077 [senior] minor

**Description:** В `ComponentsManagerScreen.kt` и `PerDictionaryComponentsScreen.kt` используется костыль `@Suppress("UnusedPrivateMember") val vm = viewModel`, аналогично `private val navigator` в placeholder ViewModel'ях — placeholder-debt, который надо выпилить на `business_implement`, не оставлять «потому что компилируется».

**Status:** approved

**Verdict:** `@Suppress("UnusedPrivateMember") val vm = viewModel` присутствует в обоих Screen.kt + неиспользуемые `private val navigator`/`dictionaryId` в VM — явный placeholder-костыль. Дешёвый fix: убрать присваивание (composable не использует — ViewModel инстанцируется через DI; в placeholder Screen ViewModel и так не нужен) либо переписать как `val viewModel = viewModel<...>(factory)` без присвоения. Альтернатива: задокументировать TODO для business_implement.

## Итоги итерации 1

- **Approved:** 2 minor (F076, F077). 0 critical.
- **Rejected:** 0.
- **Решение:** minor-only iteration, streak=1. Repeat iter 2 (cheap fix).

---

## Итерация 2 (2026-06-16T09:00:00-06:00)

### PASS [architect]
### PASS [senior]

(оба ревьюера — без findings; verify-цепочки: gradle deps cleaned, @Suppress hack убран в Screen, targeted suppress + TODO в VM)

## Итоги итерации 2

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → `review_passed = true`.
- `require_clean_iteration = true` и `changes_made = true` → repeat iter 3 (clean check).

---

## Итерация 3 (2026-06-16T09:30:00-06:00)

### PASS [architect]
### PASS [senior]

(clean check после iter 2)

## Итоги итерации 3

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → `review_passed = true`.
- `changes_made = false` → execute_repeat exit → шаг `infra_implement → done`.
