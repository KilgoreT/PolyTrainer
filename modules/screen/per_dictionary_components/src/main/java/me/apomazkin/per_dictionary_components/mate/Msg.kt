package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope

/**
 * Msg для `PerDictionaryComponentsScreen`. Зеркально CM Msg за исключением
 * `ItemsLoaded(snapshot)` вместо `TypesLoaded`. Все async-result Msg несут
 * correlation token (epochId / typeId) — F124/F136.
 */
sealed interface Msg {

    // ===== Lifecycle / data =====
    /** Emitted by `ComponentsForDictionaryFlowHandler` на каждый snapshot. */
    data class ItemsLoaded(val snapshot: PerDictionarySnapshot) : Msg
    data class ItemsLoadFailed(val cause: Throwable) : Msg

    // ===== Create dialog =====
    data object OpenCreateDialog : Msg
    data object CloseCreateDialog : Msg
    data class CreateNameChange(val value: String) : Msg
    data class CreateTemplateChange(val template: ComponentTemplate) : Msg
    data class CreateMultiToggle(val isMultiple: Boolean) : Msg
    data class CreateScopeChange(val scope: Scope) : Msg
    data object SubmitCreate : Msg
    data class CreateResult(val epochId: Long, val outcome: CreateOutcome) : Msg

    // ===== Rename dialog =====
    data class OpenRenameDialog(val typeId: ComponentTypeId) : Msg
    data object CloseRenameDialog : Msg
    data class RenameTextChange(val value: String) : Msg
    data object SubmitRename : Msg
    data class RenameResult(val epochId: Long, val outcome: RenameOutcome) : Msg

    // ===== Edit dialog (phase 2) =====
    data class OpenEditDialog(val typeId: ComponentTypeId) : Msg
    data object CloseEditDialog : Msg
    data class EditNameChange(val name: String) : Msg
    data class EditTemplateChange(val template: ComponentTemplate) : Msg
    data class EditMultiToggle(val isMultiple: Boolean) : Msg
    data object SubmitEdit : Msg
    data class EditResult(val epochId: Long, val outcome: EditOutcome) : Msg

    // ===== Delete dialog =====
    data class OpenDeleteConfirm(val typeId: ComponentTypeId) : Msg
    data object CloseDeleteConfirm : Msg
    data class ImpactPreviewLoaded(val typeId: ComponentTypeId, val impact: DeletionImpact) : Msg
    /**
     * Preview impact failure. F145: `cause == null` означает «useCase вернул null»
     * (distinct semantics — без synthetic exception); non-null — реальное исключение.
     */
    data class ImpactPreviewFailed(val typeId: ComponentTypeId, val cause: Throwable? = null) : Msg
    data object ConfirmDelete : Msg
    data class DeleteResult(val epochId: Long, val outcome: DeleteOutcome) : Msg

    // ===== Navigation =====
    data object RequestBack : Msg

    // ===== Snackbar dismiss (F123) =====
    data object DismissSnackbar : Msg

    // ===== Error state retry (F163) =====
    /**
     * User кликнул Retry в error state (когда initial load упал и items == null).
     * Reducer выставляет `isLoading=true` и эмитит [DatasourceEffect.LoadComponentsForDictionary]
     * для повторной подписки.
     */
    data object OnRetryClick : Msg

    // ===== No-op =====
    data object Empty : Msg
}

/**
 * UI feedback (snackbar). Top-level UiMsg : Msg (F086 parity).
 */
sealed interface UiMsg : Msg {
    data class Snackbar(val text: String) : UiMsg
}
