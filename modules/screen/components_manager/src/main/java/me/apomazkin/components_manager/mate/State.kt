package me.apomazkin.components_manager.mate

import androidx.compose.runtime.Stable
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

/**
 * State для `ComponentsManagerScreen` (IS481 aggregated CRUD view).
 *
 * Invariants (см. business_contract_spec.md § Инварианты + iter 3 retrofit + phase 2):
 * - `[shape]` createDialog / renameDialog / deleteConfirm / editDialog: одновременно
 *   открыт не более одного диалога (enforced в Reducer через mutual-exclusion в
 *   Open*Dialog ветках — F138; 4-way в phase 2).
 * - `[shape]` isCreating == true → createDialog != null (race race-fallback допускает временно null).
 * - `[shape]` isRenaming == true → renameDialog != null (race fallback допускает временно null).
 * - `[shape]` isDeleting == true → deleteConfirm != null (race fallback допускает временно null).
 * - `[shape]` isEditing == true → editDialog != null (race fallback допускает временно null).
 * - `[transition]` ConfirmDelete пока isDeleting=true → ignored (защита от двойного тапа).
 * - `[correlation]` async Result Msg несут `epochId`; reducer применяет только если
 *   совпадает с актуальным dialog.epochId — F124/F136 (stale results discarded).
 * - `[F030]` `Msg.DictionariesLoaded` НЕ мутирует `editDialog` поля — только
 *   `availableDictionaries` + chip-staleness filter в `createDialog`.
 */
@Stable
data class ComponentsManagerScreenState(
    // ===== Loaded data =====
    val userDefinedTypes: List<UserDefinedRow>? = null,

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,
    /** Phase 2: in-flight edit operation. */
    val isEditing: Boolean = false,

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
    /** Phase 2: edit dialog state. */
    val editDialog: EditDialogState? = null,

    // ===== Phase 2: multi-dict scope picker support =====
    /** Список словарей для chip-picker. Push'ится `DictionariesFlowHandler`. */
    val availableDictionaries: List<DictionaryApiEntity> = emptyList(),

    // ===== Snackbar (F123) =====
    val snackbarState: SnackbarState? = null,

    // ===== Epoch counter (F124/F136 correlation) =====
    /** Монотонно растущий счётчик; присваивается dialog'у при открытии. */
    val nextEpoch: Long = 0L,
)

@Stable
data class UserDefinedRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val scope: Scope,
    val usageCount: Int,
    val dictionaryNames: List<String>,
)

@Stable
data class CreateDialogState(
    val epochId: Long,
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isMultiple: Boolean = false,
    val scope: Scope = Scope.Global,
    val nameError: NameError? = null,
    /**
     * Phase 2: multi-select chip-list state. Set'овая модель (index intersection
     * через Set). При switch'е scope на Global — Reducer обнуляет.
     */
    val selectedDictionaryIds: Set<Long> = emptySet(),
)

/**
 * Computed flag для submit-кнопки. Phase 2: учитывает `PerDictionaries` scope
 * требующий хотя бы одной выбранной dictionary chip.
 */
internal val CreateDialogState.canSubmit: Boolean
    get() = name.trim().isNotEmpty() && when (scope) {
        Scope.Global -> true
        is Scope.PerDictionaries -> selectedDictionaryIds.isNotEmpty()
    }

/**
 * Edit dialog state (IS481 phase 2). Хранит snapshot оригинала (для diff)
 * + текущие input'ы + inline error / preview.
 */
@Stable
data class EditDialogState(
    val epochId: Long,
    val typeId: ComponentTypeId,
    val originalName: String,
    val originalTemplate: ComponentTemplate,
    val originalIsMultiple: Boolean,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val nameError: EditNameError? = null,
    val impactedLexemesPreview: ImpactedLexemesPreview? = null,
)

/**
 * Inline-name errors для Edit dialog. Mirror [NameError] для Create/Rename.
 *
 * `TemplateImmutable` / `BuiltInProtected` / `Removed` / `Failure` / `CardinalityDowngradeBlocked`
 * — НЕ через nameError; обрабатываются top-level reactions (snackbar / close
 * dialog / preview ветка).
 */
sealed interface EditNameError {
    data object NameEmpty : EditNameError
    data object SameScopeCollision : EditNameError
    data object CrossScopeCollision : EditNameError
}

/**
 * Explicit state-flag для cardinality downgrade preview (F023 edge-cases).
 *
 * - [InlineOnly] (size ≤ 3) — все ids inline, drill-in скрыт.
 * - [InlineWithDrillIn] (size > 3) — top-3 в `inlineIds`, full в `impactedLexemeIds`,
 *   drill-in видим.
 * - `size == 0` НЕ моделируется (downgrade проходит → `EditOutcome.Success`).
 */
sealed interface ImpactedLexemesPreview {
    val impactedLexemeIds: List<Long>

    data class InlineOnly(override val impactedLexemeIds: List<Long>) : ImpactedLexemesPreview

    data class InlineWithDrillIn(
        override val impactedLexemeIds: List<Long>,
        val inlineIds: List<Long>,
    ) : ImpactedLexemesPreview
}

@Stable
data class RenameDialogState(
    val epochId: Long,
    val typeId: ComponentTypeId,
    val originalName: String,
    val editedName: String,
    val nameError: NameError? = null,
)

@Stable
data class DeleteConfirmState(
    val epochId: Long,
    val typeId: ComponentTypeId,
    val name: String,
    val impact: DeletionImpact? = null,
    val isLoadingImpact: Boolean = false,
)

/**
 * Snackbar payload (F123). Nullable on state — отсутствует когда нет активного
 * snackbar'а. Composable отрисовывает SnackbarHost reading [ComponentsManagerScreenState.snackbarState];
 * `Msg.DismissSnackbar` сбрасывает.
 */
@Stable
data class SnackbarState(val text: String)

/** Computed selector. Loaded и пустой ⇒ показ empty state. */
val ComponentsManagerScreenState.isEmpty: Boolean
    get() = userDefinedTypes?.isEmpty() == true && !isLoading

/**
 * Маппер snapshot → UI rows. Используется в Reducer на `Msg.TypesLoaded`.
 *
 * - `scope` infer: dictionaryId == null → Global; иначе PerDictionaries(listOf(dictId)).
 * - `usageCount` берётся из usage.valueCountByType (fallback 0).
 * - `dictionaryNames` — lookup через usage.dictionaryIdsByType + usage.dictionaryNames.
 *
 * Built-in типы исключаются на data слое (system_key IS NULL фильтр). Если sneak'нулся —
 * row всё равно построится (name.orEmpty()), no crash.
 */
internal fun UserDefinedTypesSnapshot.toRows(): List<UserDefinedRow> =
    types
        .groupBy { t ->
            // Логический ключ компонента: per-dict записи с одинаковым name/template/isMultiple
            // считаются одной строкой; global всегда одна запись и группа тривиальная.
            ComponentLogicalKey(
                name = t.name.orEmpty(),
                template = t.template,
                isMultiple = t.isMultiple,
                isGlobal = t.dictionaryId == null,
            )
        }
        .map { (key, group) ->
            val head = group.first()
            val allDictIds = group.mapNotNull { it.dictionaryId }.toSet()
            UserDefinedRow(
                // TODO(IS481-multi-record-edit): typeId — head записи группы. Edit/Delete
                //  применятся только к ней; для per-dict групп нужен групповой API.
                typeId = head.id,
                name = key.name,
                template = key.template,
                isMultiple = key.isMultiple,
                scope = if (key.isGlobal) {
                    Scope.Global
                } else {
                    Scope.PerDictionaries(allDictIds.toList())
                },
                usageCount = group.sumOf { usage.valueCountByType[it.id] ?: 0 },
                dictionaryNames = group
                    .flatMap { usage.dictionaryIdsByType[it.id].orEmpty() }
                    .toSet()
                    .mapNotNull { usage.dictionaryNames[it] },
            )
        }

private data class ComponentLogicalKey(
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val isGlobal: Boolean,
)
