# Review: business_test.md

(ID сквозной per-feature — F099+ продолжение после F098.)

## Итерация 1 (2026-06-16T19:00:00-06:00)

### PASS [architect]

(spec coverage Msg/Effect/Outcome согласована с contract + design_tree)

### F099 [qa_engineer] critical

**Description:** Tier 7 migration call-sites вскрывают 50× ссылок на `ComponentValueData` в 6 тестовых файлах (WordCardUseCaseImplTest 15× / LexemeMapperTest 14× / wordcard DatasourceEffectHandlerTest 12× / QuizGameImplTest 3× / QuizGameImplFetchDataTest 3× / LexemeBuiltInExtTest 3×). Спека упоминает rebind одной фразой без перечня файлов и плана.

**Status:** approved

**Verdict:** Добавить в spec явную секцию «Существующие тесты для миграции» с **перечнем файлов** + **паттерном замены** (`ComponentValueData.TextValue(s)` → `TemplateValues.TextValues(Primitive.Text(s))`) + явное упоминание `LexemeBuiltInExtTest` (domain layer).

### F100 [qa_engineer] critical → backlog

**Description:** Scenario 6 checklist (cardinality downgrade `is_multi=true→false`) не покрыт ни тестом, ни Msg-ветками в design_tree. Cardinality downgrade в scope (aspect `multi_to_single_downgrade`, F-N5a), но business_contract/DT не содержат EditComponent/DowngradeCheck.

**Status:** approved (symptom of missing-from-design в contract/DT)

**Verdict:** Это **missing-from-design** на уровне business_contract / business_design_tree, не business_test. Применяю правило `feedback_no_scope_expansion`: НЕ переоткрываю business_contract / DT (уже done). Вместо этого:

1. В business_test.md — добавить явную секцию «Не покрываем (вне scope)»: «cardinality downgrade `is_multi=true→false` (aspect F-N5a) — design не покрывает edit component / downgrade операции. Перенесено в backlog как продолжение фичи».
2. Зафиксировать в FlowBacklog `IS481cc-F6` (process: scope_analysis включил, business_contract пропустил — gap процесса между фазами).
3. Добавить в `docs/Backlog.md` запись «IS481 phase 2: edit component + cardinality downgrade» как user-visible продолжение фичи.

### F101 [qa_engineer] critical

**Description:** Race-condition: пользователь закрывает диалог пока операция в полёте → Result приходит после закрытия → reducer `state.createDialog?.copy(nameError=...)` к null теряет error display, isCreating=false сбрасывается.

**Status:** approved

**Verdict:** Добавить в spec сценарии race-condition для Create/Rename/Delete:
- `given dialog closed (state.createDialog=null) + isCreating=true, when CreateResult(SameScopeCollision), then isCreating=false, state.createDialog stays null, UiEffect.Snackbar emitted (для error display через snackbar)`.

Это уточняет contract: error при closed dialog → snackbar fallback.

### F102 [qa_engineer] critical

**Description:** `Msg.ConfirmDelete` без guard `isLoadingImpact=true`. Soft-delete без preview данных.

**Status:** approved

**Verdict:** Добавить тест:
- `given deleteConfirm with isLoadingImpact=true, when ConfirmDelete, then NO SoftDelete effect emitted (guard)`.

Reducer должен иметь guard (impl-level constraint). Spec тест зафиксирует invariant.

### F103 [qa_engineer] critical

**Description:** `softDeleteComponent` orphan-risk: prefs reset throws после успешного DB commit → возвращает Failure → UI snackbar "failed" misleading.

**Status:** approved

**Verdict:** Добавить тест 1.5.6:
- `given DB soft-delete success + prefsProvider.setStringByRawKey throws, when softDeleteComponent, then returns DeleteOutcome.Success (best-effort prefs reset) OR Failure with detailed error message`.

Уточнение: UseCase impl должен либо обернуть prefs reset в try/catch (best-effort), либо возвращать `Outcome.PartialSuccess` (new variant). Решение — на implement; тест зафиксирует invariant.

### F104 [qa_engineer] minor

**Description:** Test isolation — спека не упоминает clearAllMocks convention.

**Status:** rejected

**Verdict:** Общая convention проекта (за пределами scope этой spec). Не блокирует coverage.

### F105 [qa_engineer] minor

**Description:** flow throws → passthrough в UseCase не покрыт.

**Status:** rejected

**Verdict:** Passthrough — корректный дизайн (FlowHandler catches). Бессмыслен на UseCase level; покрыт в FlowHandlerTest 6.1.

### F106 [qa_engineer] minor

**Description:** OpenCreateDialog while open — overwrite vs preserve неоднозначно.

**Status:** approved

**Verdict:** В тесте 3.x явно: «`given createDialog != null, when OnCreateClick, then createDialog reset to empty default state (template=Translation, scope=Global, nameError=null)`». Это уточняет invariant: reopen всегда reset.

### F107 [qa_engineer] minor

**Description:** rename to same name.

**Status:** rejected

**Verdict:** DAO/UseCase-level, не business test layer.

### F108 [qa_engineer] minor

**Description:** helper toRows() test placement.

**Status:** rejected

**Verdict:** Стилистика организации тестов.

## Итоги итерации 1

- **Approved:** 5 critical (F099, F100→backlog, F101, F102, F103) + 1 minor (F106). 0 rejected critical, 4 rejected minor.
- **Решение:** есть approved critical → reset streak, repeat iter 2. F100 → backlog (без переоткрытия business_contract/DT).

---

## Итерация 2 (2026-06-16T19:30:00-06:00)

### PASS [architect]
### PASS [qa_engineer]

(verify: migration table 6 файлов + race-condition тесты + ConfirmDelete guard + orphan prefs test + overwrite reset)

## Итоги итерации 2

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → review_passed = true.
- `changes_made = true` → repeat iter 3 (clean check).

---

## Итерация 3 (2026-06-16T19:45:00-06:00)

### PASS [architect]
### PASS [qa_engineer]

(clean check; артефакт unchanged)

## Итоги итерации 3

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → review_passed = true.
- `changes_made = false` → execute_repeat exit → шаг `business_test → done`.
