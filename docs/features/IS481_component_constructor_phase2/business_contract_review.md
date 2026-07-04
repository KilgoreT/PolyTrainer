# Review: business_contract

## Findings

1. **`EditOutcome.Success` payload не финализирован.**
   В контракте `data class Success(val typeId: Long) : EditOutcome` сопровождается комментарием `// либо ComponentType — финализирует design`. Это незакрытая decision-точка прямо в текущем артефакте. Baseline `RenameOutcome.Success(ComponentType)` (`RenameOutcome.kt:9-16`) задаёт parity-выбор. Контракт обязан зафиксировать payload — `typeId: Long` либо `ComponentType` — иначе downstream EffectHandler / Reducer / UseCaseImpl не могут шиться. Предложить: выбрать `ComponentType` для parity с `RenameOutcome.Success`, либо явно обосновать почему `typeId` достаточно (например, реактивная подписка `flowAllUserDefinedTypes` перерисует список и payload не нужен — но тогда `RenameOutcome.Success` тоже подлежит унификации, и это уже out-of-scope).

2. **`CardinalityDowngradeBlocked.impactedLexemeIds` — семантика списка не определена между API и State.**
   На API уровне `EditComponentOutcome.CardinalityDowngradeBlocked(impactedLexemeIds: List<Long>)` — список без явных ограничений (full / top-3 / какой sort). На State уровне `ImpactedLexemesPreview.InlineWithDrillIn` разделяет `impactedLexemeIds` (full) и `inlineIds` (top-3, явно отсортированные `ORDER BY updated_at DESC, lexeme_id ASC`). Контракт не указывает: (a) API возвращает full list и Reducer/UseCase делает top-3 split, либо (b) API возвращает уже top-3 + full отдельно, либо (c) API возвращает только top-N и full получается отдельным методом. От этого зависит сигнатура data API и API outcome shape. Предложить: явно зафиксировать что API возвращает full sorted list, top-3 split происходит в Reducer при построении `ImpactedLexemesPreview` из `EditOutcome.CardinalityDowngradeBlocked`.

## Verdict

verdict: approved

**Note:** original verdict iter 1 = `changes_requested` (2 findings). User-shortcut «хватит ревью петель» — conductor применил inline fix вместо full `trigger_step_rerun(business_contract)`:
- **F-BCR1 closed inline:** `business_contract.md` обновлён — `EditOutcome.Success(val updated: ComponentType)` (parity с `RenameOutcome.Success(ComponentType)`).
- **F-BCR2 closed inline:** `business_contract.md` обновлён — `CardinalityDowngradeBlocked.impactedLexemeIds = full sorted list`; top-3 split происходит в Reducer на UI-mapping (комментарий в sealed добавлен).

verdict: approved (after inline fix; trigger_step_rerun пропущен по user request).

_model: claude-opus-4-7[1m]_
