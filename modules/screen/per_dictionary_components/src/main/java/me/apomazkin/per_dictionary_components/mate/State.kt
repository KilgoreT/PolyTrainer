package me.apomazkin.per_dictionary_components.mate

import androidx.compose.runtime.Stable
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.Scope

/**
 * State для `PerDictionaryComponentsScreen` (IS481 scoped CRUD view —
 * global ∪ per-dict для конкретного словаря). Зеркально `ComponentsManagerScreenState`
 * с двумя отличиями:
 *
 * - init-параметр `dictionaryId` (assisted-инжект в ViewModel) хранится в state
 *   для reducer'а (`OpenCreateDialog` использует его для preselect scope).
 * - `dictionaryName` приходит с первым snapshot'ом, отображается в header.
 *
 * Все [F123/F124/F127/F132/F136/F138/F140] инварианты с самого начала (iter 3 retrofit).
 *
 * Invariants:
 * - `[shape]` createDialog/deleteConfirm/editDialog: одновременно ≤1 диалог
 *   (enforced в Reducer mutual-exclusion в Open*Dialog ветках — F138).
 * - `[shape]` is*ing == true → соответствующий dialog != null (race fallback допускает временно null).
 * - `[transition]` ConfirmDelete пока isDeleting=true → ignored.
 * - `[correlation]` *Result Msg несут epochId; reducer применяет только если активный
 *   dialog.epochId совпадает (stale results discarded).
 */
@Stable
data class PerDictionaryComponentsScreenState(
    // ===== Init context =====
    val dictionaryId: Long,
    val dictionaryName: String? = null,

    // ===== Loaded data =====
    val items: List<PerDictRow>? = null,

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    /** Phase 2 (IS481): in-flight edit operation. */
    val isEditing: Boolean = false,

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
    /** Phase 2 (IS481): edit dialog state. */
    val editDialog: EditDialogState? = null,

    // ===== Snackbar (F123) =====
    val snackbarState: SnackbarState? = null,

    // ===== Epoch counter (F124/F136 correlation) =====
    val nextEpoch: Long = 0L,
)

@Stable
data class PerDictRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    /** `true` если type — global (dictionaryId == null), иначе per-dict для этого словаря. */
    val isGlobal: Boolean,
    val valueCount: Int,
)

@Stable
data class CreateDialogState(
    val epochId: Long,
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isMultiple: Boolean = false,
    /**
     * Init-инициализируется текущим dictId через `Scope.PerDictionaries(listOf(dictId))`
     * в Reducer ветке `Msg.OpenCreateDialog` (preselect; см. `business_design_tree.md` #47).
     */
    val scope: Scope,
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
 * Edit dialog state (IS481 phase 2). Parity с Manager-вариантом — структурно
 * дублируется (UI sub-flow позже выносит в shared widget module).
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

sealed interface EditNameError {
    data object NameEmpty : EditNameError
    data object SameScopeCollision : EditNameError
    data object CrossScopeCollision : EditNameError
}

sealed interface ImpactedLexemesPreview {
    val impactedLexemeIds: List<Long>

    data class InlineOnly(override val impactedLexemeIds: List<Long>) : ImpactedLexemesPreview

    data class InlineWithDrillIn(
        override val impactedLexemeIds: List<Long>,
        val inlineIds: List<Long>,
    ) : ImpactedLexemesPreview
}

/**
 * Snackbar payload (F123). Nullable on state — отсутствует когда нет активного
 * snackbar'а. UI отрисует SnackbarHost reading state.
 */
@Stable
data class SnackbarState(val text: String)

/** Computed selector. Loaded и пустой ⇒ показ empty state. */
val PerDictionaryComponentsScreenState.isEmpty: Boolean
    get() = items?.isEmpty() == true && !isLoading

/**
 * Маппер snapshot → UI rows. Используется в Reducer на `Msg.ItemsLoaded`.
 *
 * - `isGlobal` infer: dictionaryId == null → true, else false.
 * - `valueCount` берётся из `snapshot.valueCountByType` (fallback 0).
 */
internal fun PerDictionarySnapshot.toPerDictRows(): List<PerDictRow> =
    types.map { t ->
        PerDictRow(
            typeId = t.id,
            name = t.name.orEmpty(),
            template = t.template,
            isMultiple = t.isMultiple,
            isGlobal = t.dictionaryId == null,
            valueCount = valueCountByType[t.id] ?: 0,
        )
    }
