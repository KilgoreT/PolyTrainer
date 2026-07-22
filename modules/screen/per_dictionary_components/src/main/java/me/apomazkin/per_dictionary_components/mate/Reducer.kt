package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.OptionOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.SetEnabledOutcome
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
            // IS486: мульти для CHOICE запрещён (spec §7.5) — сброс при выборе шаблона.
            else state.copy(
                createDialog = dlg.copy(
                    template = message.template,
                    isMultiple = if (message.template == ComponentTemplate.CHOICE) false else dlg.isMultiple,
                ),
            ) to emptySet()
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

        // ===== IS486 (В1): пикер цели в Create =====
        is Msg.CreateTargetChange -> {
            val dlg = state.createDialog
            if (dlg == null) state to emptySet()
            // Ядро — свойство зависимых от лексемы (spec §7.7): при уводе цели
            // с лексемы галка форсится false.
            else state.copy(
                createDialog = dlg.copy(
                    target = message.target,
                    core = if (message.target is DependencyTarget.Lexeme) dlg.core else false,
                ),
            ) to emptySet()
        }

        is Msg.CreateCoreToggle -> {
            val dlg = state.createDialog
            when {
                dlg == null -> state to emptySet()
                dlg.target !is DependencyTarget.Lexeme -> state to emptySet()  // guard
                else -> state.copy(createDialog = dlg.copy(core = message.core)) to emptySet()
            }
        }

        // ===== IS486 (В2): черновики вариантов CHOICE в Create =====
        Msg.CreateOptionAdd -> {
            val dlg = state.createDialog
            if (dlg == null) state to emptySet()
            else state.copy(
                createDialog = dlg.copy(optionDrafts = dlg.optionDrafts + "", optionsError = false),
            ) to emptySet()
        }

        is Msg.CreateOptionChange -> {
            val dlg = state.createDialog
            if (dlg == null || message.index !in dlg.optionDrafts.indices) state to emptySet()
            else state.copy(
                createDialog = dlg.copy(
                    optionDrafts = dlg.optionDrafts.toMutableList()
                        .apply { set(message.index, message.value) },
                    optionsError = false,
                ),
            ) to emptySet()
        }

        is Msg.CreateOptionRemove -> {
            val dlg = state.createDialog
            if (dlg == null || message.index !in dlg.optionDrafts.indices) state to emptySet()
            else state.copy(
                createDialog = dlg.copy(
                    optionDrafts = dlg.optionDrafts.filterIndexed { i, _ -> i != message.index },
                ),
            ) to emptySet()
        }

        Msg.SubmitCreate -> {
            val dlg = state.createDialog
            val choiceLabels = dlg?.optionDrafts.orEmpty().map { it.trim() }.filter { it.isNotBlank() }
            when {
                dlg == null -> state to emptySet()
                state.isCreating -> state to emptySet()
                dlg.name.isBlank() ->
                    state.copy(
                        createDialog = dlg.copy(nameError = NameError.Empty)
                    ) to emptySet()
                // IS486: CHOICE без единого непустого варианта — UI-отказ (домен разрешает).
                dlg.template == ComponentTemplate.CHOICE && choiceLabels.isEmpty() ->
                    state.copy(
                        createDialog = dlg.copy(optionsError = true)
                    ) to emptySet()
                else ->
                    state.copy(isCreating = true) to setOf(
                        DatasourceEffect.CreateComponent(
                            epochId = dlg.epochId,
                            name = dlg.name,
                            template = dlg.template,
                            isMultiple = dlg.isMultiple,
                            scope = dlg.scope,
                            target = dlg.target,
                            core = dlg.core,
                            optionLabels = if (dlg.template == ComponentTemplate.CHOICE) choiceLabels else emptyList(),
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
                // IS486: нельзя удалить последнее включённое ядро словаря (spec §7.8).
                DeleteOutcome.LastEnabledCore ->
                    state.copy(isDeleting = false, deleteConfirm = null) to setOf(
                        UiEffect.Snackbar("Last enabled core component")
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
            when {
                row == null -> state to emptySet()
                // IS486: builtin в фазе 3 не редактируется (опции builtin — §21.2, фаза 4).
                row.systemKey != null -> state to emptySet()
                else -> {
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
                            originalTarget = row.dependsOn,
                            originalCore = row.core,
                            target = row.dependsOn,
                            core = row.core,
                            existingOptions = row.options.map { o ->
                                EditOptionRow(
                                    optionId = o.optionId,
                                    systemKey = o.systemKey,
                                    originalLabel = o.label,
                                    label = o.label.orEmpty(),
                                )
                            },
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

        // ===== IS486 (В1): пикер цели в Edit =====
        is Msg.EditTargetChange -> {
            val dlg = state.editDialog
            when {
                dlg == null -> state to emptySet()
                // Ранний цикл-чек (девайс-фидбек C2, решение 2026-07-21): выбор цели,
                // создающей цикл, отклоняется сразу в пикере — тост, target не меняется.
                // Data-проверка CycleDetected остаётся подстраховкой.
                createsCycle(state.items.orEmpty(), dlg.typeId, message.target) ->
                    state to setOf(UiEffect.Snackbar("Dependency cycle detected"))
                else -> state.copy(
                    editDialog = dlg.copy(
                        target = message.target,
                        core = if (message.target is DependencyTarget.Lexeme) dlg.core else false,
                    ),
                ) to emptySet()
            }
        }

        is Msg.EditCoreToggle -> {
            val dlg = state.editDialog
            when {
                dlg == null -> state to emptySet()
                dlg.target !is DependencyTarget.Lexeme -> state to emptySet()  // guard
                else -> state.copy(editDialog = dlg.copy(core = message.core)) to emptySet()
            }
        }

        // ===== IS486 (В2): варианты CHOICE в Edit =====
        is Msg.EditOptionLabelChange -> {
            val dlg = state.editDialog
            if (dlg == null) state to emptySet()
            else state.copy(
                editDialog = dlg.copy(
                    existingOptions = dlg.existingOptions.map { o ->
                        if (o.optionId == message.optionId) o.copy(label = message.value) else o
                    },
                ),
            ) to emptySet()
        }

        Msg.EditOptionDraftAdd -> {
            val dlg = state.editDialog
            if (dlg == null) state to emptySet()
            else state.copy(
                editDialog = dlg.copy(newOptionDrafts = dlg.newOptionDrafts + ""),
            ) to emptySet()
        }

        is Msg.EditOptionDraftChange -> {
            val dlg = state.editDialog
            if (dlg == null || message.index !in dlg.newOptionDrafts.indices) state to emptySet()
            else state.copy(
                editDialog = dlg.copy(
                    newOptionDrafts = dlg.newOptionDrafts.toMutableList()
                        .apply { set(message.index, message.value) },
                ),
            ) to emptySet()
        }

        is Msg.EditOptionDraftRemove -> {
            val dlg = state.editDialog
            if (dlg == null || message.index !in dlg.newOptionDrafts.indices) state to emptySet()
            else state.copy(
                editDialog = dlg.copy(
                    newOptionDrafts = dlg.newOptionDrafts.filterIndexed { i, _ -> i != message.index },
                ),
            ) to emptySet()
        }

        is Msg.EditOptionDeleteRequest -> {
            val dlg = state.editDialog
            val option = dlg?.existingOptions?.firstOrNull { it.optionId == message.optionId }
            when {
                dlg == null || option == null -> state to emptySet()
                dlg.isDeletingOption -> state to emptySet()          // guard
                dlg.optionDeleteConfirm?.optionId == message.optionId -> state to emptySet()
                else -> state.copy(
                    editDialog = dlg.copy(
                        optionDeleteConfirm = OptionDeleteConfirmState(
                            optionId = option.optionId,
                            label = option.label,
                            isLoadingImpact = true,
                        ),
                    ),
                ) to setOf(DatasourceEffect.LoadOptionImpact(option.optionId))
            }
        }

        Msg.CloseOptionDeleteConfirm -> {
            val dlg = state.editDialog
            if (dlg == null) state to emptySet()
            else state.copy(editDialog = dlg.copy(optionDeleteConfirm = null)) to emptySet()
        }

        is Msg.OptionImpactLoaded -> {
            val dlg = state.editDialog
            val confirm = dlg?.optionDeleteConfirm
            if (dlg == null || confirm == null || confirm.optionId != message.optionId) {
                state to emptySet()                                  // stale
            } else state.copy(
                editDialog = dlg.copy(
                    optionDeleteConfirm = confirm.copy(impact = message.impact, isLoadingImpact = false),
                ),
            ) to emptySet()
        }

        is Msg.OptionImpactFailed -> {
            val dlg = state.editDialog
            val confirm = dlg?.optionDeleteConfirm
            if (dlg == null || confirm == null || confirm.optionId != message.optionId) {
                state to emptySet()                                  // stale / closed — silent
            } else state.copy(
                editDialog = dlg.copy(
                    optionDeleteConfirm = confirm.copy(isLoadingImpact = false),
                ),
            ) to setOf(UiEffect.Snackbar("Failed to load impact"))
        }

        Msg.ConfirmOptionDelete -> {
            val dlg = state.editDialog
            val confirm = dlg?.optionDeleteConfirm
            when {
                dlg == null || confirm == null -> state to emptySet()
                dlg.isDeletingOption -> state to emptySet()          // double-tap guard
                confirm.isLoadingImpact -> state to emptySet()       // F102 parity
                else -> state.copy(
                    editDialog = dlg.copy(isDeletingOption = true),
                ) to setOf(
                    DatasourceEffect.DeleteOption(
                        epochId = dlg.epochId,
                        optionId = confirm.optionId,
                    )
                )
            }
        }

        is Msg.OptionDeleteResult -> {
            val dlg = state.editDialog
            if (dlg != null && dlg.epochId != message.epochId) {
                state to emptySet()                                  // F136 stale
            } else when (val o = message.outcome) {
                is OptionOutcome.Deleted ->
                    // Решение 2026-07-21 (девайс-фидбек O3): удаление опции — немедленная
                    // самостоятельная операция; Edit-диалог закрывается целиком (серая
                    // «Сохранить» после применённого удаления путала).
                    state.copy(editDialog = null, isEditing = false) to setOf(
                        UiEffect.Snackbar("${o.impact.valueCount + o.impact.descendantValueCount} values hidden")
                    )
                is OptionOutcome.Success ->
                    // Delete не возвращает Success — defensive.
                    state.copy(
                        editDialog = dlg?.copy(isDeletingOption = false, optionDeleteConfirm = null),
                    ) to emptySet()
                OptionOutcome.Removed ->
                    state.copy(
                        editDialog = dlg?.copy(isDeletingOption = false, optionDeleteConfirm = null),
                    ) to setOf(UiEffect.Snackbar("Option removed"))
                // Решение §21.2: недостижимо из UI (Edit builtin закрыт) — defensive.
                OptionOutcome.BuiltInProtected ->
                    state.copy(
                        editDialog = dlg?.copy(isDeletingOption = false, optionDeleteConfirm = null),
                    ) to setOf(UiEffect.Snackbar("Built-in protected"))
                is OptionOutcome.Failure ->
                    state.copy(
                        editDialog = dlg?.copy(isDeletingOption = false),
                    ) to setOf(UiEffect.Snackbar("Failed: ${o.cause.failureLabel()}"))
            }
        }

        Msg.SubmitEdit -> {
            val dlg = state.editDialog
            when {
                dlg == null -> state to emptySet()
                state.isEditing -> state to emptySet()       // F139 double-tap
                dlg.isDeletingOption -> state to emptySet()  // IS486: дождаться каскада опции
                dlg.name.trim().isBlank() ->
                    state.copy(
                        editDialog = dlg.copy(nameError = EditNameError.NameEmpty),
                    ) to emptySet()
                // IS486 умный сброс (решение 2026-07-21): смена цели/ядра — сначала
                // обязательный impact-конфирм («безопасно» / «будет скрыто N»).
                dlg.target != dlg.originalTarget || dlg.core != dlg.originalCore ->
                    state.copy(
                        editDialog = dlg.copy(
                            rebindConfirm = RebindConfirmState(isLoadingImpact = true),
                        ),
                    ) to setOf(
                        DatasourceEffect.LoadRebindImpact(
                            typeId = dlg.typeId,
                            target = dlg.target,
                            core = dlg.core,
                        ),
                    )
                else -> submitEditEffect(state, dlg)
            }
        }

        // ===== IS486 умный сброс: конфирм перепривязки =====
        is Msg.RebindImpactLoaded -> {
            val dlg = state.editDialog
            val confirm = dlg?.rebindConfirm
            if (dlg == null || confirm == null || dlg.typeId != message.typeId) {
                state to emptySet()                                  // stale
            } else state.copy(
                editDialog = dlg.copy(
                    rebindConfirm = confirm.copy(impact = message.impact, isLoadingImpact = false),
                ),
            ) to emptySet()
        }

        is Msg.RebindImpactFailed -> {
            val dlg = state.editDialog
            val confirm = dlg?.rebindConfirm
            if (dlg == null || confirm == null || dlg.typeId != message.typeId) {
                state to emptySet()                                  // stale / closed — silent
            } else state.copy(
                editDialog = dlg.copy(rebindConfirm = null),
            ) to setOf(UiEffect.Snackbar("Failed to load impact"))
        }

        Msg.CloseRebindConfirm -> {
            val dlg = state.editDialog
            if (dlg == null) state to emptySet()
            else state.copy(editDialog = dlg.copy(rebindConfirm = null)) to emptySet()
        }

        Msg.ConfirmRebind -> {
            val dlg = state.editDialog
            val confirm = dlg?.rebindConfirm
            when {
                dlg == null || confirm == null -> state to emptySet()
                state.isEditing -> state to emptySet()               // double-tap
                confirm.isLoadingImpact -> state to emptySet()       // F102 parity
                else -> {
                    val (next, effects) = submitEditEffect(
                        state.copy(editDialog = dlg.copy(rebindConfirm = null)),
                        dlg.copy(rebindConfirm = null),
                    )
                    next to effects
                }
            }
        }

        // (helper submitEditEffect — внизу файла)

        // ===== IS486: рубильник enabled (spec §6) =====
        is Msg.ToggleEnabled -> {
            val row = state.items?.firstOrNull { it.typeId == message.typeId }
            when {
                row == null -> state to emptySet()
                message.typeId in state.pendingEnabledToggles -> state to emptySet()  // guard
                row.enabled == message.enabled -> state to emptySet()                 // no-op
                else -> state.copy(
                    pendingEnabledToggles = state.pendingEnabledToggles + message.typeId,
                ) to setOf(DatasourceEffect.SetEnabled(message.typeId, message.enabled))
            }
        }

        is Msg.SetEnabledResult -> {
            val cleared = state.copy(
                pendingEnabledToggles = state.pendingEnabledToggles - message.typeId,
            )
            when (val o = message.outcome) {
                is SetEnabledOutcome.Success -> cleared to emptySet()  // flow отдаст новый снапшот
                SetEnabledOutcome.LastEnabledCore ->
                    cleared to setOf(UiEffect.Snackbar("Last enabled core component"))
                SetEnabledOutcome.Removed ->
                    cleared to setOf(UiEffect.Snackbar("Component removed"))
                is SetEnabledOutcome.Failure ->
                    cleared to setOf(UiEffect.Snackbar("Failed: ${o.cause.failureLabel()}"))
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
                // IS486 (spec §7.5, §7.8, §8): временно снеками — полноценная обработка
                // (in-dialog ошибки пикера цели) появится с UI иерархии в блоке B.
                EditOutcome.CycleDetected ->
                    state.copy(isEditing = false, editDialog = null) to setOf(
                        UiEffect.Snackbar("Dependency cycle detected"),
                    )
                EditOutcome.MultiForbiddenForChoice ->
                    state.copy(isEditing = false, editDialog = null) to setOf(
                        UiEffect.Snackbar("Multiple values forbidden for choice"),
                    )
                EditOutcome.LastEnabledCore ->
                    state.copy(isEditing = false, editDialog = null) to setOf(
                        UiEffect.Snackbar("Last enabled core component"),
                    )
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

    /**
     * IS486: общий submit-эффект Edit (прямой путь без смены цели и путь через
     * rebind-конфирм): EditComponent с целью/ядром + батч опций (В2).
     */
    private fun submitEditEffect(
        state: PerDictionaryComponentsScreenState,
        dlg: EditDialogState,
    ): Pair<PerDictionaryComponentsScreenState, Set<Effect>> =
        state.copy(isEditing = true) to setOf(
            DatasourceEffect.EditComponent(
                epochId = dlg.epochId,
                typeId = dlg.typeId,
                name = dlg.name,
                template = dlg.template,
                isMultiple = dlg.isMultiple,
                target = dlg.target,
                core = dlg.core,
                optionRenames = dlg.existingOptions
                    .filter { it.label.trim() != it.originalLabel.orEmpty() && it.label.isNotBlank() }
                    .map { it.optionId to it.label.trim() },
                optionAdds = dlg.newOptionDrafts.map { it.trim() }.filter { it.isNotBlank() },
            ),
        )
}
