package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.Scope
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.tools.failureLabel

/**
 * Pure reducer для `PerDictionaryComponentsScreen`. Зеркально CM Reducer с двумя
 * отличиями:
 *
 * - `Msg.ItemsLoaded(snapshot)` маппит `snapshot.toPerDictRows()` + сохраняет
 *   `dictionaryName` из snapshot'а.
 * - `Msg.OpenCreateDialog` инициализирует `scope = Scope.PerDictionaries(listOf(state.dictionaryId))`
 *   (preselect текущий словарь — см. `business_contract_spec.md` § PerDictionaryComponentsScreenState).
 *
 * Все [F123/F124/F127/F132/F136/F138/F140] инварианты включены с самого начала.
 *
 * Invariants:
 * - SubmitCreate / ConfirmDelete — игнорируются если соответствующий
 *   `is{Creating,Deleting}=true`.
 * - ConfirmDelete также guard на `isLoadingImpact=true` (F102).
 * - Open*Dialog при уже открытом — overwrite reset (F106) + новый epochId + закрытие
 *   других диалогов (F138).
 * - *Result когда диалог закрыт (race close-during-flight, F101) — snackbar fallback +
 *   reset is*=false; dialog state не воскрешается.
 * - *Result с устаревшим epochId (F136) → silently discarded.
 * - ImpactPreviewLoaded/Failed с устаревшим typeId (F124) → silently discarded.
 *
 * F129 retrofit: `Throwable.message` fallback → class name → "unknown".
 */
class PerDictionaryComponentsReducer(
    private val logger: LexemeLogger,
) : MateReducer<PerDictionaryComponentsScreenState, Msg, Effect> {

    override fun reduce(
        state: PerDictionaryComponentsScreenState,
        message: Msg,
    ): Pair<PerDictionaryComponentsScreenState, Set<Effect>> {
        logger.log(tag = LogTags.DICT_COMPONENTS, message = "Reduce --prevState--: $state ")
        logger.log(tag = LogTags.DICT_COMPONENTS, message = "Reduce ---message---: $message ")
        val result = reduceInner(state, message)
        logger.log(tag = LogTags.DICT_COMPONENTS, message = "Reduce --newState--: ${result.first} ")
        if (result.second.isNotEmpty()) {
            logger.log(tag = LogTags.DICT_COMPONENTS, message = "Reduce --toEffect--: ${result.second} ")
        }
        return result
    }

    private fun reduceInner(
        state: PerDictionaryComponentsScreenState,
        message: Msg,
    ): Pair<PerDictionaryComponentsScreenState, Set<Effect>> = when (message) {

        // ===== Lifecycle =====
        is Msg.ItemsLoaded ->
            state.copy(
                items = message.snapshot.toPerDictRows(),
                dictionaryName = message.snapshot.dictionaryName,
                isLoading = false,
            ) to emptySet()

        is Msg.ItemsLoadFailed ->
            state.copy(isLoading = false) to setOf(
                UiEffect.Snackbar("Failed to load: ${message.cause.failureLabel()}")
            )

        // ===== Create dialog =====
        Msg.OpenCreateDialog -> {
            // F106/F138/F140: overwrite reset, close other dialogs (4-way phase 2),
            // reset is*-flags + preselect scope = текущий словарь.
            val newEpoch = state.nextEpoch + 1
            state.copy(
                createDialog = CreateDialogState(
                    epochId = newEpoch,
                    scope = Scope.PerDictionaries(listOf(state.dictionaryId)),
                ),
                deleteConfirm = null,
                editDialog = null,
                isCreating = false,
                isDeleting = false,
                isEditing = false,
                nextEpoch = newEpoch,
            ) to emptySet()
        }

        Msg.CloseCreateDialog ->
            state.copy(createDialog = null) to emptySet()

        is Msg.CreateNameChange -> {
            val dlg = state.createDialog
            if (dlg == null) state to emptySet()
            else state.copy(
                createDialog = dlg.copy(name = message.value, nameError = null)
            ) to emptySet()
        }

        is Msg.CreateTemplateChange -> {
            val dlg = state.createDialog
            if (dlg == null) state to emptySet()
            else state.copy(createDialog = dlg.copy(template = message.template)) to emptySet()
        }

        is Msg.CreateMultiToggle -> {
            val dlg = state.createDialog
            if (dlg == null) state to emptySet()
            else state.copy(createDialog = dlg.copy(isMultiple = message.isMultiple)) to emptySet()
        }

        is Msg.CreateScopeChange -> {
            val dlg = state.createDialog
            if (dlg == null) state to emptySet()
            else state.copy(createDialog = dlg.copy(scope = message.scope)) to emptySet()
        }

        Msg.SubmitCreate -> {
            val dlg = state.createDialog
            when {
                dlg == null -> state to emptySet()
                state.isCreating -> state to emptySet()
                dlg.name.isBlank() ->
                    state.copy(
                        createDialog = dlg.copy(nameError = NameError.Empty)
                    ) to emptySet()
                else ->
                    state.copy(isCreating = true) to setOf(
                        DatasourceEffect.CreateComponent(
                            epochId = dlg.epochId,
                            name = dlg.name,
                            template = dlg.template,
                            isMultiple = dlg.isMultiple,
                            scope = dlg.scope,
                        )
                    )
            }
        }

        is Msg.CreateResult -> {
            val dlg = state.createDialog
            if (dlg != null && dlg.epochId != message.epochId) {
                state to emptySet()                                  // F136 stale
            } else when (val o = message.outcome) {
                is CreateOutcome.Success ->
                    state.copy(isCreating = false, createDialog = null) to setOf(
                        UiEffect.Snackbar("Created ${o.created.size}")
                    )
                CreateOutcome.NameEmpty ->
                    if (dlg == null) {
                        state.copy(isCreating = false) to setOf(
                            UiEffect.Snackbar("Name cannot be empty")
                        )
                    } else {
                        state.copy(
                            isCreating = false,
                            createDialog = dlg.copy(nameError = NameError.Empty),
                        ) to emptySet()
                    }
                CreateOutcome.SameScopeCollision ->
                    if (dlg == null) {
                        state.copy(isCreating = false) to setOf(
                            UiEffect.Snackbar("Name already taken in this scope")
                        )
                    } else {
                        state.copy(
                            isCreating = false,
                            createDialog = dlg.copy(nameError = NameError.SameScopeCollision),
                        ) to emptySet()
                    }
                CreateOutcome.CrossScopeCollision ->
                    if (dlg == null) {
                        state.copy(isCreating = false) to setOf(
                            UiEffect.Snackbar("Name conflicts across scopes")
                        )
                    } else {
                        state.copy(
                            isCreating = false,
                            createDialog = dlg.copy(nameError = NameError.CrossScopeCollision),
                        ) to emptySet()
                    }
                is CreateOutcome.Failure ->
                    state.copy(isCreating = false) to setOf(
                        UiEffect.Snackbar("Failed: ${o.cause.failureLabel()}")
                    )
            }
        }

        // ===== Delete confirm =====
        is Msg.OpenDeleteConfirm -> {
            val row = state.items?.firstOrNull { it.typeId == message.typeId }
            when {
                row == null -> state to emptySet()                   // guard: row not found
                state.deleteConfirm?.typeId == message.typeId ->
                    // F132 narrow: тот же typeId — не пересоздаём.
                    state to emptySet()
                else -> {
                    val newEpoch = state.nextEpoch + 1
                    state.copy(
                        deleteConfirm = DeleteConfirmState(
                            epochId = newEpoch,
                            typeId = row.typeId,
                            name = row.name,
                            isLoadingImpact = true,
                        ),
                        createDialog = null,
                        editDialog = null,
                        isCreating = false,
                        isDeleting = false,
                        isEditing = false,
                        nextEpoch = newEpoch,
                    ) to setOf(DatasourceEffect.LoadImpact(row.typeId))
                }
            }
        }

        Msg.CloseDeleteConfirm ->
            state.copy(deleteConfirm = null) to emptySet()

        is Msg.ImpactPreviewLoaded -> {
            val dlg = state.deleteConfirm
            if (dlg == null || dlg.typeId != message.typeId) {
                state to emptySet()                                  // F124 stale
            } else state.copy(
                deleteConfirm = dlg.copy(impact = message.impact, isLoadingImpact = false),
            ) to emptySet()
        }

        is Msg.ImpactPreviewFailed -> {
            // F144: если dialog закрыт user'ом in-flight — silent (no snackbar),
            // т.к. UX-овой релевантности больше нет. Stale typeId (F124) тоже silent.
            val dlg = state.deleteConfirm
            if (dlg == null) {
                state to emptySet()
            } else if (dlg.typeId != message.typeId) {
                state to emptySet()                                  // F124 stale
            } else {
                state.copy(
                    deleteConfirm = dlg.copy(isLoadingImpact = false),
                ) to setOf(UiEffect.Snackbar("Failed to load impact"))
            }
        }

        Msg.ConfirmDelete -> {
            val dlg = state.deleteConfirm
            when {
                dlg == null -> state to emptySet()
                state.isDeleting -> state to emptySet()
                dlg.isLoadingImpact -> state to emptySet()           // F102
                else ->
                    state.copy(isDeleting = true) to setOf(
                        DatasourceEffect.SoftDeleteComponent(
                            epochId = dlg.epochId,
                            typeId = dlg.typeId,
                        )
                    )
            }
        }

        is Msg.DeleteResult -> {
            val dlg = state.deleteConfirm
            if (dlg != null && dlg.epochId != message.epochId) {
                state to emptySet()                                  // F136 stale
            } else when (val o = message.outcome) {
                is DeleteOutcome.Success ->
                    state.copy(isDeleting = false, deleteConfirm = null) to setOf(
                        UiEffect.Snackbar("${o.impact.valueCount} values hidden")
                    )
                DeleteOutcome.BuiltInProtected ->
                    state.copy(isDeleting = false, deleteConfirm = null) to setOf(
                        UiEffect.Snackbar("Built-in protected")
                    )
                DeleteOutcome.Removed ->
                    state.copy(isDeleting = false, deleteConfirm = null) to setOf(
                        UiEffect.Snackbar("Component removed")
                    )
                is DeleteOutcome.Failure ->
                    state.copy(isDeleting = false) to setOf(
                        UiEffect.Snackbar("Failed: ${o.cause.failureLabel()}")
                    )
            }
        }

        // ===== Edit dialog (phase 2) =====
        is Msg.OpenEditDialog -> {
            val row = state.items?.firstOrNull { it.typeId == message.typeId }
            if (row == null) {
                state to emptySet()
            } else {
                val newEpoch = state.nextEpoch + 1
                state.copy(
                    editDialog = EditDialogState(
                        epochId = newEpoch,
                        typeId = row.typeId,
                        originalName = row.name,
                        originalTemplate = row.template,
                        originalIsMultiple = row.isMultiple,
                        name = row.name,
                        template = row.template,
                        isMultiple = row.isMultiple,
                    ),
                    createDialog = null,
                    deleteConfirm = null,
                    isCreating = false,
                    isDeleting = false,
                    isEditing = false,
                    nextEpoch = newEpoch,
                ) to emptySet()
            }
        }

        Msg.CloseEditDialog ->
            state.copy(editDialog = null, isEditing = false) to emptySet()

        is Msg.EditNameChange -> {
            val dlg = state.editDialog
            if (dlg == null) state to emptySet()
            else state.copy(
                editDialog = dlg.copy(name = message.name, nameError = null),
            ) to emptySet()
        }

        is Msg.EditTemplateChange -> {
            val dlg = state.editDialog
            if (dlg == null) state to emptySet()
            else state.copy(editDialog = dlg.copy(template = message.template)) to emptySet()
        }

        is Msg.EditMultiToggle -> {
            val dlg = state.editDialog
            if (dlg == null) state to emptySet()
            else state.copy(
                editDialog = dlg.copy(
                    isMultiple = message.isMultiple,
                    impactedLexemesPreview = null,
                ),
            ) to emptySet()
        }

        Msg.SubmitEdit -> {
            val dlg = state.editDialog
            when {
                dlg == null -> state to emptySet()
                state.isEditing -> state to emptySet()       // F139 double-tap
                dlg.name.trim().isBlank() ->
                    state.copy(
                        editDialog = dlg.copy(nameError = EditNameError.NameEmpty),
                    ) to emptySet()
                else ->
                    state.copy(isEditing = true) to setOf(
                        DatasourceEffect.EditComponent(
                            epochId = dlg.epochId,
                            typeId = dlg.typeId,
                            name = dlg.name,
                            template = dlg.template,
                            isMultiple = dlg.isMultiple,
                        ),
                    )
            }
        }

        is Msg.EditResult -> {
            val dlg = state.editDialog
            if (dlg != null && dlg.epochId != message.epochId) {
                state to emptySet()                          // F136 stale
            } else when (val o = message.outcome) {
                is EditOutcome.Success ->
                    state.copy(isEditing = false, editDialog = null) to setOf(
                        UiEffect.Snackbar("Updated"),
                    )
                EditOutcome.NameEmpty ->
                    if (dlg == null) {
                        state.copy(isEditing = false) to setOf(
                            UiEffect.Snackbar("Name cannot be empty"),
                        )
                    } else {
                        state.copy(
                            isEditing = false,
                            editDialog = dlg.copy(nameError = EditNameError.NameEmpty),
                        ) to emptySet()
                    }
                EditOutcome.SameScopeCollision ->
                    if (dlg == null) {
                        state.copy(isEditing = false) to setOf(
                            UiEffect.Snackbar("Name already taken in this scope"),
                        )
                    } else {
                        state.copy(
                            isEditing = false,
                            editDialog = dlg.copy(nameError = EditNameError.SameScopeCollision),
                        ) to emptySet()
                    }
                EditOutcome.CrossScopeCollision ->
                    if (dlg == null) {
                        state.copy(isEditing = false) to setOf(
                            UiEffect.Snackbar("Name conflicts across scopes"),
                        )
                    } else {
                        state.copy(
                            isEditing = false,
                            editDialog = dlg.copy(nameError = EditNameError.CrossScopeCollision),
                        ) to emptySet()
                    }
                is EditOutcome.CardinalityDowngradeBlocked -> {
                    if (dlg == null) {
                        state.copy(isEditing = false) to setOf(
                            UiEffect.Snackbar("Cardinality downgrade blocked"),
                        )
                    } else {
                        val preview = if (o.impactedLexemeIds.size <= 3) {
                            ImpactedLexemesPreview.InlineOnly(o.impactedLexemeIds)
                        } else {
                            ImpactedLexemesPreview.InlineWithDrillIn(
                                impactedLexemeIds = o.impactedLexemeIds,
                                inlineIds = o.impactedLexemeIds.take(3),
                            )
                        }
                        state.copy(
                            isEditing = false,
                            editDialog = dlg.copy(impactedLexemesPreview = preview),
                        ) to emptySet()
                    }
                }
                EditOutcome.TemplateImmutable ->
                    state.copy(isEditing = false, editDialog = null) to setOf(
                        UiEffect.Snackbar("Template cannot be changed"),
                    )
                EditOutcome.BuiltInProtected ->
                    state.copy(isEditing = false, editDialog = null) to setOf(
                        UiEffect.Snackbar("Built-in protected"),
                    )
                EditOutcome.Removed ->
                    state.copy(isEditing = false, editDialog = null) to setOf(
                        UiEffect.Snackbar("Component removed"),
                    )
                is EditOutcome.Failure ->
                    state.copy(isEditing = false, editDialog = null) to setOf(
                        UiEffect.Snackbar("Failed: ${o.cause.failureLabel()}"),
                    )
            }
        }

        // ===== Navigation =====
        Msg.RequestBack -> state to setOf(NavigationEffect.Back)

        // ===== Snackbar (F123) =====
        is UiMsg.Snackbar -> state.copy(snackbarState = SnackbarState(message.text)) to emptySet()
        Msg.DismissSnackbar -> state.copy(snackbarState = null) to emptySet()

        // ===== Retry on error state (F163) =====
        Msg.OnRetryClick ->
            state.copy(isLoading = true) to setOf(DatasourceEffect.LoadComponentsForDictionary)

        // ===== No-op =====
        Msg.Empty -> state to emptySet()
    }
}
