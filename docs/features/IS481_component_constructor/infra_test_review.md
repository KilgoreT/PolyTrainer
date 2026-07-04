# Review: infra_test.md

## Итерация 1 (2026-06-16T03:50:00-06:00)

### architect → PASS

Спека согласована с design_tree, scope из 02_scope.md покрыт (Reducer extensions id 13, 17), cross-reference на другие test-шаги корректен. Обоснованно отказано от тестов NavigatorImpl / DI Component / NavGraphBuilder / gradle / CompositionRoot.

### F065 [qa HIGH → minor (normalized)]
**Description:** edge-case payload `dictId=0L` не покрыт — спека предлагает удалить второй кейс `DictionaryAppBarReducerTest`. Это упускает «payload-passthrough» контракт (защита от future `require(dictionaryId > 0)` рефакторинга).
**Status:** approved
**Verdict:** Снять «опционально» в § 2 — второй кейс с `dictionaryId=0L` (или `42L`) обязателен. Документирует «reducer не валидирует payload as-is».

### F066 [qa HIGH] re-dispatch idempotency
**Description:** Два последовательных Msg.OpenPerDictionaryComponents — что Reducer должен делать?
**Status:** rejected
**Verdict:** Reducer — чистая функция, состояния между вызовами нет. Дубль-клик = runtime/UI concern (debounce / navigator no-op). Out-of-scope reducer test.

### F067 [qa MEDIUM] back-fill 4 existing branches SettingsTabReducerTest
**Description:** существующие ветки `OpenLangManagement` и пр. без unit-coverage; добавить back-fill либо backlog item.
**Status:** rejected
**Verdict:** Scope creep, нарушает `feedback_no_scope_expansion`. Спека уже явно говорит «back-fill вне scope IS481». Опциональный Backlog item — отдельной задачей, не частью этой фичи.

### F068 [qa MEDIUM → minor] regression check 6 existing tests
**Description:** Нет явного pin что после расширения sealed `Msg`/`NavigationEffect` 6 существующих тестов остаются green.
**Status:** approved
**Verdict:** Add one bullet в § 2 — «Regression: existing 6 cases must remain green without modification».

### F069 [qa MEDIUM] icon visibility (currentDict=null) — explicit_state_flags violation?
**Description:** Чеклист scenario 2 step 3 — icon скрыт если currentDict=null; memory rule про explicit flags.
**Status:** rejected
**Verdict:** `currentDict: Long?` — primary state field; `state.currentDict != null` — derived guard от primary state, не отдельный boolean флаг. Memory rule не запрещает inline `if (state.currentDict != null)`. К тому же это ui_design_tree / ui_layout concern, не infra_test scope.

### F070 [qa LOW → minor] helper name for state immutability
**Description:** assertion для state immutability не имеет конкретного имени в спеке.
**Status:** approved
**Verdict:** В § 1 и § 2 явно: `assertEquals(initialState, result.state())` (inline; не вводить новый helper). Если в `:modules:core:mate.test` есть `assertStateUnchanged` — использовать его (sub-agent должен проверить).

### F071 [qa LOW] cancellation comment
**Description:** Явный note что cancellation effect = handler concern.
**Status:** rejected
**Verdict:** Over-spec. Reducer return-only-Set — базовый Mate pattern, не требует disclaimer'а в каждой spec.

## Итоги итерации 1

- **Approved:** 3 minor (F065, F068, F070). 0 critical.
- **Rejected:** 4.
- **Решение:** minor-only iteration, streak=1. Repeat iter 2 (cheap fix).

---

## Итерация 2 (2026-06-16T04:50:00-06:00)

### F072 [architect] critical

**Description:** Msg-name mismatch между infra_test и design_tree (id 15/17). infra_test.md § 2 использует `Msg.OnPerDictionaryComponentsClick(dictionaryId=...)`. design_tree id 15 (Message.kt) объявляет `data class OpenPerDictionaryComponents(val dictionaryId: Long) : Msg`; id 17 (Reducer) обрабатывает `is Msg.OpenPerDictionaryComponents`. Тест ссылается на несуществующий идентификатор → compile fail при выполнении infra_implement.

**Status:** approved

**Verdict:** Литеральный mismatch identifier'а Msg между test (`OnPerDictionaryComponentsClick`) и design_tree id 15/17 (`OpenPerDictionaryComponents`) → compile fail при infra_implement; конкретный pointer на line, real fix. Синхронизировать имя — см. F074 (scope тоже подтянуть).

### F073 [qa_engineer] minor

**Description:** Спека инструктирует raw `assertEquals(setOf<Effect>(OpenPerDictionaryComponents(...)), result.effects())` для positive check. В `:modules:core:mate.test/MateTestHelper.kt:18-23` есть helper `ReducerResult.assertEffects(expectedEffects: Set<EFFECTS>)` — convention. Inline assertEquals дублирует helper'а.

**Status:** approved

**Verdict:** Helper существует и является convention; inline `assertEquals(setOf(...), result.effects())` дублирует helper'а, content impact на consistency со стилем существующих тестов. Заменить на `result.assertEffects(setOf(...))` в § 1 и § 2.

### F074 [qa_engineer] minor

**Description:** `02_scope.md:121` декларирует `Msg.OpenComponentConstructor` для DictionaryAppBar. `infra_test.md` § 2 использует `Msg.OnPerDictionaryComponentsClick`. `infra_design_tree.md` id 15 объявляет `Msg.OpenPerDictionaryComponents`. Три разных имени Msg между тремя артефактами одной фичи.

**Status:** approved

**Verdict:** Не duplicate F072 — расширяет fix на третий артефакт (`scope.md:121` тоже должен быть синхронизирован). Выбрать одно имя (рекомендация: `OpenPerDictionaryComponents` — consistent с effect-naming и уже закрытым design_tree) и применить во всех трёх местах: `scope.md:121` rename + `infra_test.md` § 2 rename + design_tree без изменений (уже правильно).

### F075 [qa_engineer] minor

**Description:** § 2 говорит «добавить новую секцию `=== Msg.OpenPerDictionaryComponents ===` в нумерованный список (после `Msg.DictMenu*`)», но не уточняет, новые кейсы продолжают сквозную нумерацию (7, 8) или начинают новую (1, 2). Существующий KDoc нумерует test-кейсы 1-6 сквозно через секции. Без указания implementer создаст inconsistent KDoc.

**Status:** approved

**Verdict:** Существующий KDoc нумерует кейсы сквозно 1-6 через секции (`DictionaryAppBarReducerTest.kt:19-29`); спека не уточняет (7, 8) vs (1, 2). Зафиксировать: «новые кейсы продолжают сквозную нумерацию — 7 и 8».

## Итоги итерации 2

- **Approved:** 1 critical (F072) + 3 minor (F073, F074, F075).
- **Rejected:** 0.
- **Решение:** есть approved critical → reset minor_only_streak, repeat iter 3.

---

## Итерация 3 (2026-06-16T05:30:00-06:00)

### PASS [architect]
### PASS [qa_engineer]

(оба ревьюера — без findings; verify-цепочки выполнены: MateTestHelper.kt, DictionaryAppBarReducerTest.kt KDoc 1-6, design_tree id 11/15, settingstab/src/test пустой)

## Итоги итерации 3

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → `review_passed = true`.
- `require_clean_iteration = true` (default) и `changes_made = true` (iter 3 правил артефакт + 02_scope.md) → execute_repeat не выходит. Запускается iter 4 для clean check.

---

## Итерация 4 (2026-06-16T05:50:00-06:00)

### PASS [architect]
### PASS [qa_engineer]

(оба ревьюера — без findings; clean check после iter 3)

## Итоги итерации 4

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → `review_passed = true`.
- `changes_made = false` (iter 4 не правил артефакт) → execute_repeat выходит → шаг `infra_test → done`.
