package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.OptionOutcome
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.SetEnabledOutcome

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

    // IS486 (В1): пикер цели + галка «Ядро».
    data class CreateTargetChange(val target: DependencyTarget) : Msg
    data class CreateCoreToggle(val core: Boolean) : Msg

    // IS486 (В2): черновики вариантов CHOICE.
    data object CreateOptionAdd : Msg
    data class CreateOptionChange(val index: Int, val value: String) : Msg
    data class CreateOptionRemove(val index: Int) : Msg

    data object SubmitCreate : Msg
    data class CreateResult(val epochId: Long, val outcome: CreateOutcome) : Msg

    // ===== Edit dialog (phase 2) =====
    data class OpenEditDialog(val typeId: ComponentTypeId) : Msg
    data object CloseEditDialog : Msg
    data class EditNameChange(val name: String) : Msg
    data class EditTemplateChange(val template: ComponentTemplate) : Msg
    data class EditMultiToggle(val isMultiple: Boolean) : Msg

    // IS486 (В1): пикер цели в Edit.
    data class EditTargetChange(val target: DependencyTarget) : Msg
    data class EditCoreToggle(val core: Boolean) : Msg

    // IS486 (В2): варианты CHOICE в Edit — rename существующих батчем на Submit,
    // новые черновики add на Submit, удаление существующей — немедленно (конфирм).
    data class EditOptionLabelChange(val optionId: Long, val value: String) : Msg
    data object EditOptionDraftAdd : Msg
    data class EditOptionDraftChange(val index: Int, val value: String) : Msg
    data class EditOptionDraftRemove(val index: Int) : Msg
    data class EditOptionDeleteRequest(val optionId: Long) : Msg
    data object CloseOptionDeleteConfirm : Msg
    data class OptionImpactLoaded(val optionId: Long, val impact: DeletionImpact) : Msg
    data class OptionImpactFailed(val optionId: Long, val cause: Throwable? = null) : Msg
    data object ConfirmOptionDelete : Msg
    data class OptionDeleteResult(val epochId: Long, val outcome: OptionOutcome) : Msg

    // IS486 умный сброс (решение 2026-07-21): конфирм перепривязки перед применением.
    data class RebindImpactLoaded(val typeId: ComponentTypeId, val impact: DeletionImpact) : Msg
    data class RebindImpactFailed(val typeId: ComponentTypeId, val cause: Throwable? = null) : Msg
    data object ConfirmRebind : Msg
    data object CloseRebindConfirm : Msg

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

    // ===== IS486: рубильник enabled (spec §6) =====
    data class ToggleEnabled(val typeId: ComponentTypeId, val enabled: Boolean) : Msg
    data class SetEnabledResult(val typeId: ComponentTypeId, val outcome: SetEnabledOutcome) : Msg

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
