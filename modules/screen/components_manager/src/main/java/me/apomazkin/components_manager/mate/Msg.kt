package me.apomazkin.components_manager.mate

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

/**
 * Msg для `ComponentsManagerScreen`. См. business_contract_spec.md § UI Messages.
 *
 * Async result Msg'ы (`*Result`, `ImpactPreviewLoaded`) несут correlation token —
 * `epochId` / `typeId` — для отбрасывания stale results (F124/F136 retrofit).
 */
sealed interface Msg {

    // ===== Lifecycle / data =====
    /** Emitted by `AllUserDefinedTypesFlowHandler` на каждый snapshot. */
    data class TypesLoaded(val snapshot: UserDefinedTypesSnapshot) : Msg
    data class TypesLoadFailed(val cause: Throwable) : Msg

    // ===== Create dialog =====
    data object OpenCreateDialog : Msg
    data object CloseCreateDialog : Msg
    data class CreateNameChange(val value: String) : Msg
    data class CreateTemplateChange(val template: ComponentTemplate) : Msg
    data class CreateMultiToggle(val isMultiple: Boolean) : Msg
    data class CreateScopeChange(val scope: Scope) : Msg
    data object SubmitCreate : Msg
    /**
     * Result of in-flight create operation. `epochId` matches the dialog that initiated
     * the request — reducer discards stale results (F136).
     */
    data class CreateResult(val epochId: Long, val outcome: CreateOutcome) : Msg

    // ===== Edit dialog (phase 2) =====
    data class OpenEditDialog(val typeId: ComponentTypeId) : Msg
    data object CloseEditDialog : Msg
    data class EditNameChange(val name: String) : Msg
    data class EditTemplateChange(val template: ComponentTemplate) : Msg
    data class EditMultiToggle(val isMultiple: Boolean) : Msg
    data object SubmitEdit : Msg
    data class EditResult(val epochId: Long, val outcome: EditOutcome) : Msg

    // ===== Multi-dict scope picker (phase 2) =====
    /** Toggle one dictionary id in `createDialog.selectedDictionaryIds`. */
    data class CreateDictionaryToggle(val dictionaryId: Long) : Msg

    /**
     * Emit'ится `DictionariesFlowHandler` на каждый snapshot подписки.
     * Reducer: обновляет `availableDictionaries` + chip-staleness фильтрует
     * `createDialog.selectedDictionaryIds ∩ list.ids`. F030 invariant —
     * `editDialog` НЕ мутируется.
     */
    data class DictionariesLoaded(val dictionaries: List<DictionaryApiEntity>) : Msg

    // ===== Delete dialog =====
    data class OpenDeleteConfirm(val typeId: ComponentTypeId) : Msg
    data object CloseDeleteConfirm : Msg
    /**
     * Preview импact-а; `typeId` корреллируется с активным `deleteConfirm.typeId` —
     * stale previews discarded (F124).
     */
    data class ImpactPreviewLoaded(val typeId: ComponentTypeId, val impact: DeletionImpact) : Msg
    /**
     * Preview impact failure. F145: `cause == null` означает «useCase вернул null»
     * (distinct semantics — без synthetic exception); non-null — реальное исключение
     * из data-layer / useCase.
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
     * User кликнул Retry в error state (когда initial load упал и rows == null).
     * Reducer выставляет `isLoading=true` и эмитит [DatasourceEffect.LoadAllUserDefinedTypes]
     * для повторной подписки.
     */
    data object OnRetryClick : Msg

    // ===== No-op =====
    data object Empty : Msg
}

/**
 * UI feedback (snackbar). Top-level UiMsg : Msg (F086 parity с existing convention).
 *
 * F128 retrofit: dead `show: Boolean` field removed — snackbar visibility теперь
 * выводится из `state.snackbarState != null` (F123).
 */
sealed interface UiMsg : Msg {
    data class Snackbar(val text: String) : UiMsg
}
