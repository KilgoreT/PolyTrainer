# publish_spec

## Опубликовано
- spec_filename: null → публикация в `docs/features-spec/` не выполняется
- `business_contract_spec.md` остаётся в feature dir как canonical local-scope контракт

## Корректировки от implement

Без contract-level изменений: спека опубликована as-is относительно `business_contract_spec.md`.

Корректировки в `business_implement.md` (секция «Корректировки vs spec / design_tree») — implementation-level, не затрагивают контрактную поверхность:

1. **`quizPickerPrefKey` → `:modules:datasource:prefs`** (не в UseCaseImpl) — location helper'а, single source of truth для UseCaseImpl + FlowHandler. Msg / Effect / State surface не изменены.
2. **Tier 1 primitives `String` вместо `StringSource`** — `core/ui/dropdown` не зависит от `widget/iconDropDowned`. UI-internal детализация, не контракт.
3. **`SaveQuizPickerSelection` при null dict → `Msg.Empty`** — handler emit'ит `Msg.Empty` (set не вызывается, без throw). Соответствует existing pattern `LoadQuizComponentTypes`. Поведение Effect (no-op при null dict) и так подразумевалось спекой, формализовано в impl без новых Msg / Effect.

Все контрактные `Msg` (`SelectQuizComponent`, `QuizComponentTypesLoaded`), `DatasourceEffect` (`LoadQuizComponentTypes`, `SaveQuizPickerSelection`), `State.QuizComponent` (поля `availableTypes`, `selectedRef`), `isPickerEnabled` computed — реализованы без изменений сигнатур vs `business_contract_spec.md`.

## PUML
- .puml файлов в feature dir нет — пропуск

_model: claude-opus-4-7[1m]_
