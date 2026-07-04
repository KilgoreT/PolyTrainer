# Review: business_design_tree.md

(ID сквозной per-feature — F082+ продолжение после F081.)

## Итерация 1 (2026-06-16T16:15:00-06:00)

### F082 [architect] critical

**Description:** #28 (PerDictionaryComponentsUseCaseImpl) делегирует через `private val sharedCrud: ComponentsManagerUseCaseImpl`. `depends: [24, 26]` — отсутствует #27.

**Status:** approved

**Verdict:** Compile-зависимость отсутствует. Fix: `depends: [24, 26, 27]`.

### F083 [architect] critical

**Description:** #6 (delete `ComponentValueData.kt`) `depends: [5]`. Удаление символа должно быть ПОСЛЕ всех call-site миграций.

**Status:** approved

**Verdict:** Либо `#6 depends: [49, 50, 51, 52, 53, 54, ...]` (все мигрированные узлы), либо явно отложить узел в `data_design_tree.md` с пометкой «delete after data-side migrations».

### F084 [architect] critical

**Description:** `DatasourceEffect.SubscribeForDictionary` (#42) объявлен, но никем не диспатчится. Dead Effect.

**Status:** approved

**Verdict:** Либо удалить `SubscribeForDictionary` из sealed #42, либо подключить через `initEffects = setOf(DatasourceEffect.SubscribeForDictionary)` в #48 ViewModel. Рекомендация — remove, так как final pattern (assisted FlowHandler) подписывается напрямую.

### F085 [architect] critical

**Description:** `LogTags` constants (для components_manager и per_dictionary_components) используются в #27, #33, #34, #43, #44 — но нет DAG-узлов.

**Status:** approved

**Verdict:** Добавить 2 узла (или 1 shared) в DAG: `LogTags.kt` в каждом модуле (либо в shared module). depends: [`:modules:core:logger`].

### F086 [architect] minor

**Description:** spec — UiMsg вложенный в Msg; DT #30 — top-level. Sync mismatch.

**Status:** approved

**Verdict:** Align: либо #30 переделать на вложенный UiMsg, либо обновить spec на top-level. Рекомендация: top-level UiMsg (existing convention в проекте), spec обновить.

### F087 [architect] minor

**Description:** `toRows()` / `toPerDictRows()` extensions вызываются в #37/#47 — не объявлены ни одним узлом.

**Status:** approved

**Verdict:** Добавить helper'ы в #29 (`ComponentsManagerScreenState.kt` или отдельный `StateHelpers.kt`) и в #39 (PerDict state). Или один узел `StateHelpers.kt` в каждом модуле.

### F088 [architect] minor

**Description:** Spec содержит `SubscribeAll : DatasourceEffect`, DT #32 опускает (init-trigger через FlowHandler).

**Status:** approved

**Verdict:** Align: либо вернуть `SubscribeAll` в #32 (даже unused — для contract parity), либо обновить spec убрав. Рекомендация: убрать из spec — final pattern (assisted FlowHandler) делает SubscribeAll излишним.

## Итоги итерации 1

- **Approved:** 4 critical (F082, F083, F084, F085) + 3 minor (F086, F087, F088).
- **Rejected:** 0.
- **Решение:** есть approved critical → reset minor_only_streak, repeat iter 2.

---

## Итерация 2 (2026-06-16T17:30:00-06:00)

### F089 [architect] critical

**Description:** #29 (State.kt for CM) `depends: [4, 9, 10, 12]` не содержит #14, хотя iter 2 fix добавил `toRows()` extension использующую `UserDefinedTypesSnapshot` (#14). Topological order #29 перед #14 сломает build.

**Status:** approved

**Verdict:** добавить #14 в depends узла #29. Real compile-dep.

### F090 [architect] critical

**Description:** #39 (State.kt for PerDict) `depends: [4, 9, 10, 12]` не содержит #15, хотя `toPerDictRows()` использует `PerDictionarySnapshot` (#15).

**Status:** approved

**Verdict:** добавить #15 в depends узла #39.

### F091 [architect] minor

**Description:** `business_contract_spec.md:405` описывает `SubscribeForDictionary(dictionaryId)` как DatasourceEffect для PerDictionaryComponentsScreen, хотя F084 удалил из DT #42 sealed. Drift contract ↔ DT.

**Status:** approved

**Verdict:** удалить упоминание `SubscribeForDictionary` из spec § PerDictionaryComponentsDatasourceEffect. Init subscription через assisted FlowHandler (parity с CM).

## Итоги итерации 2

- **Approved:** 2 critical (F089, F090) + 1 minor (F091). 0 rejected.
- **Решение:** есть approved critical → reset minor_only_streak, repeat iter 3.

---

## Итерация 3 (2026-06-16T18:30:00-06:00)

### F092 [architect] critical

**Description:** #52 (LexemeMapper.kt) описан как «2 call-site'а ComponentValueData.TextValue» с deps=[5, 20]. Но `ComponentTypeApiEntity.toDomain()` в этом же mapper'е требует update после #7/#19 field changes (`removeDate → removedAt`, новые `isMulti/createdAt/updatedAt`). Без extension #52 scope + deps [7, 19] — ComponentType field rename без compile-узла.

**Status:** approved

**Verdict:** Real DAG gap. Fix: расширить scope #52 на ComponentType field rename + добавить deps [7, 19], либо вынести отдельный узел `LexemeMapper#toDomain-componentType`.

## Итоги итерации 3

- **Approved:** 1 critical (F092). 0 minor, 0 rejected.
- **Решение:** есть approved critical → reset streak, repeat iter 4.

---

## Итерация 4 (2026-06-16T19:00:00-06:00)

### F093 [architect] critical

**Description:** #37 (ComponentsManagerReducer) emits `UiEffect.Snackbar` 10 раз, но `UiEffect` sealed объявлен в #31 (UiEffectHandler.kt). Deps [29, 30, 32, 35] не включают #31 → compile-order проёб.

**Status:** approved

**Verdict:** Add #31 to #37 depends. Real symbol resolution gap.

### F094 [architect] critical

**Description:** #47 (PerDictionaryComponentsReducer) зеркально #37, эмитит `UiEffect.Snackbar`. UiEffect для PerDict в #41. Deps [39, 40, 42, 45] не включают #41.

**Status:** approved

**Verdict:** Add #41 to #47 depends. Mirror gap.

## Итоги итерации 4

- **Approved:** 2 critical (F093, F094). 0 minor, 0 rejected.
- **Решение:** есть approved critical → reset streak, repeat iter 5.

---

## Итерация 5 (2026-06-16T19:30:00-06:00)

### F095 [architect] minor

**Description:** #37 depends [35] и #47 depends [45] spurious; reducers emit только `BaseNavigationEffect.Back`, не ссылаются на module-specific NavigationEffect sealed (#35/#45 имеют нулевые variants).

**Status:** approved

**Verdict:** Remove #35 из #37 depends, #45 из #47 depends.

### F096 [architect] minor

**Description:** #29 и #39 missing depends на #7 — toRows()/toPerDictRows() обращаются к `t.isMulti` (M13-новое поле ComponentType #7).

**Status:** approved

**Verdict:** Add #7 в depends #29 и #39.

### F097 [architect] minor

**Description:** Tier narrative § Audit checklist § 6 устарел — «Tier 7 листья» не соответствует #27/#28 → #52 (Tier 4 → Tier 7).

**Status:** approved

**Verdict:** Update narrative — указать что #52 (Tier 7) выполняется ДО #27/#28 (Tier 4) из-за back-reference. Объяснить порядок.

## Итоги итерации 5

- **Approved:** 3 minor (F095, F096, F097). 0 critical, 0 rejected.
- **Решение:** minor-only iter, streak=1. Repeat iter 6 (cheap fix).

---

## Итерация 6 (2026-06-16T20:30:00-06:00)

### F098 [architect] critical

**Description:** #37 (и зеркально #47) использует `BaseNavigationEffect.Back` с import `me.apomazkin.mate.base.BaseNavigationEffect` — несуществующий symbol. В `:modules:core:mate` существует `me.apomazkin.mate.NavigationEffect.Back` (подтверждено в `WordCardReducer.kt:248`). Reducer не скомпилируется.

**Status:** approved

**Verdict:** Fix: `BaseNavigationEffect.Back` → `NavigationEffect.Back`. Import — `me.apomazkin.mate.NavigationEffect`. Применить к #37 и #47 + удалить выражение «Если на проекте используется другая convention для Back — заменить» (оно дезориентирует).

## Итоги итерации 6

- **Approved:** 1 critical (F098). 0 minor, 0 rejected.
- **Решение:** есть approved critical → reset streak, repeat iter 7 (max=7 — последняя итерация).

---

## Итерация 7 (2026-06-16T20:55:00-06:00)

### PASS [architect]

(verify: NavigationEffect.Back import из me.apomazkin.mate; 17 findings closed across 7 iterations)

## Итоги итерации 7

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → review_passed = true.
- `require_clean_iteration = true` И `changes_made = true` (iter 7 правил F098) — по псевдокоду нужна clean iter, но `max=7` достигнут. Edge case runner.md (нет ветки approved-at-max-with-unverified-clean).
- **Финальное решение:** принимаю `business_design_tree → done` (architect approved на iter 7). Unverified clean — accepted as soft tech debt. Edge case записан в FlowBacklog (IS481cc-F5).
