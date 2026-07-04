# Review: ui_layout.md

## Итерация 1 (2026-06-17T15:40:00-06:00)

### F148 [architect] critical

**Description:** Msg names в callbacks секциях ui_layout.md не соответствуют реальному контракту в `Msg.kt`. UI spec использует устаревшие имена: `UpdateCreateName/Template/IsMulti/Scope`, `ConfirmCreate`, `UpdateRenameName`, `ConfirmRename`. Реальные: `CreateNameChange/TemplateChange/MultiToggle/ScopeChange`, `SubmitCreate`, `RenameTextChange`, `SubmitRename`.

**Status:** approved

**Verdict:** Mechanical rename в ui_layout.md (диалоги CM + PerDict).

### F149 [architect] critical

**Description:** Сигнатуры `OpenRenameDialog(typeId, originalName)` / `OpenDeleteConfirm(typeId, name)` в Row callbacks не соответствуют контракту — реальные `(typeId: ComponentTypeId)`. Name наполняется Reducer'ом.

**Status:** approved

**Verdict:** Убрать `originalName` / `name` параметр в callbacks. Reducer берёт name из state.rows.

## Итоги итерации 1

- **Approved:** 2 critical. 0 rejected.
- **Решение:** есть approved critical → reset streak, repeat iter 2.

---

## Итерация 2 (2026-06-17T16:10:00-06:00)

- F150 [minor]: CreateComponentDialog params vs structure mismatch (onScopeChange vs onScopeGlobal/onScopePerDict/onOpenDictionaryPicker). approved.
- F151 [minor]: PerDictRowWidget enabled-attr pseudo-code vs behavior. approved.

## Итоги итерации 2

- **Approved:** 0 critical + 2 minor. 0 rejected.
- **Решение:** minor-only, streak=1. Repeat iter 3 (max=3 — последняя).

---

## Итерация 3 (2026-06-17T16:30:00-06:00)

### PASS [architect]

(verify: F148/F149/F150/F151 все closed; callbacks/params/structure consistent.)

## Итоги итерации 3

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → review_passed = true.
- max=3 достигнут, changes_made=true (iter 3 fix), edge case IS481cc-F5 (approved-at-max-with-unverified-clean) → accept as done.
- ui_layout → done.
