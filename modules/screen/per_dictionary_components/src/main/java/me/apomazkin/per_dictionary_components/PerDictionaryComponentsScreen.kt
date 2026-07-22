@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.per_dictionary_components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.component_widgets.dialogs.CreateComponentDialog
import me.apomazkin.component_widgets.dialogs.DeleteComponentConfirmDialog
import me.apomazkin.component_widgets.dialogs.DeletionImpactRef
import me.apomazkin.component_widgets.dialogs.EditComponentDialog
import me.apomazkin.component_widgets.dialogs.HostVariant
import me.apomazkin.component_widgets.dialogs.OptionDeleteConfirmDialog
import me.apomazkin.component_widgets.dialogs.RebindConfirmDialog
import me.apomazkin.component_widgets.dialogs.TargetPickerItem
import me.apomazkin.component_widgets.widgets.ComponentsEmptyStateWidget
import me.apomazkin.component_widgets.widgets.CreateComponentFab
import me.apomazkin.component_widgets.widgets.PerDictRowWidget
import me.apomazkin.component_widgets.widgets.componentDisplayName
import me.apomazkin.component_widgets.widgets.optionDisplayLabel
import me.apomazkin.core_resources.R
import me.apomazkin.di.viewModelFactory
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.per_dictionary_components.mate.EditNameError
import me.apomazkin.per_dictionary_components.mate.ImpactedLexemesPreview
import me.apomazkin.per_dictionary_components.mate.Msg
import me.apomazkin.per_dictionary_components.mate.PerDictRow
import me.apomazkin.per_dictionary_components.mate.createsCycle
import me.apomazkin.per_dictionary_components.mate.isEmpty
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.ui.ErrorStateWidget

/**
 * IS481: `PerDictionaryComponentsScreen` — scoped CRUD view (global ∪ per-dict).
 *
 * Phase 2: dialogs/rows вынесены в `:modules:widget:component_widgets`. Mount'ит 4
 * диалога (Create + DeleteConfirm + Edit), CreateComponentDialog получает
 * hostVariant=PerDict (scope picker скрыт; scope hardcoded в Reducer).
 */
@Composable
fun PerDictionaryComponentsScreen(
    dictionaryId: Long,
    factory: PerDictionaryComponentsViewModel.Factory,
    navigator: PerDictionaryComponentsNavigator,
    viewModel: PerDictionaryComponentsViewModel = viewModel(
        factory = viewModelFactory { factory.create(dictionaryId, navigator) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler { viewModel.accept(Msg.RequestBack) }

    val snackbar = state.snackbarState
    LaunchedEffect(snackbar) {
        if (snackbar != null) {
            snackbarHostState.showSnackbar(snackbar.text)
            viewModel.accept(Msg.DismissSnackbar)
        }
    }

    Scaffold(
        containerColor = formBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        text = state.dictionaryName
                            ?: stringResource(id = R.string.per_dict_components_title),
                        style = LexemeStyle.H5,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.accept(Msg.RequestBack) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            CreateComponentFab(onClick = { viewModel.accept(Msg.OpenCreateDialog) })
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val rows = state.items
            when {
                state.isLoading && rows == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // F163: error state — initial load упал, items == null и не идёт loading.
                !state.isLoading && rows == null -> {
                    ErrorStateWidget(
                        modifier = Modifier.align(Alignment.Center),
                        messageRes = R.string.components_per_dict_load_failed,
                        retryRes = R.string.components_error_retry,
                        onRetry = { viewModel.accept(Msg.OnRetryClick) },
                    )
                }

                state.isEmpty -> {
                    ComponentsEmptyStateWidget(
                        headlineRes = R.string.components_empty_headline_per_dict,
                        bodyRes = R.string.components_empty_body_per_dict,
                        onCreate = { viewModel.accept(Msg.OpenCreateDialog) },
                    )
                }

                rows != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = rows, key = { it.typeId.id }) { row ->
                            PerDictRowWidget(
                                typeId = row.typeId,
                                name = componentDisplayName(systemKey = row.systemKey, name = row.name),
                                template = row.template,
                                isMultiple = row.isMultiple,
                                isGlobal = row.isGlobal,
                                valueCount = row.valueCount,
                                dictionaryNames = if (row.isGlobal) {
                                    emptyList()
                                } else {
                                    listOfNotNull(state.dictionaryName)
                                },
                                onEdit = { viewModel.accept(Msg.OpenEditDialog(it)) },
                                onDelete = { viewModel.accept(Msg.OpenDeleteConfirm(it)) },
                                // IS486 (В3): builtin — только свитч; чипы состояния.
                                isBuiltIn = row.systemKey != null,
                                enabled = row.enabled,
                                degraded = row.degraded,
                                enabledTogglePending = row.typeId in state.pendingEnabledToggles,
                                onToggleEnabled = { typeId, enabled ->
                                    viewModel.accept(Msg.ToggleEnabled(typeId, enabled))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    state.createDialog?.let { dialog ->
        CreateComponentDialog(
            name = dialog.name,
            template = dialog.template,
            isMultiple = dialog.isMultiple,
            scope = dialog.scope,
            nameError = dialog.nameError,
            isSubmitting = state.isCreating,
            availableDictionaries = emptyList(),
            selectedDictionaryIds = setOf(dictionaryId),
            hostVariant = HostVariant.PerDict,
            onNameChange = { viewModel.accept(Msg.CreateNameChange(it)) },
            onTemplateSelect = { viewModel.accept(Msg.CreateTemplateChange(it)) },
            onMultiToggle = { viewModel.accept(Msg.CreateMultiToggle(it)) },
            onScopeChange = { /* no-op — scope hardcoded in Reducer */ },
            onDictionaryToggle = { /* no-op */ },
            onSubmit = { viewModel.accept(Msg.SubmitCreate) },
            onDismiss = { viewModel.accept(Msg.CloseCreateDialog) },
            // IS486 (В1): пикер цели — все живые компоненты словаря + опции CHOICE.
            targetItems = targetPickerItems(rows = state.items, excludeTypeId = null),
            selectedTarget = dialog.target,
            core = dialog.core,
            onTargetSelect = { viewModel.accept(Msg.CreateTargetChange(it)) },
            onCoreToggle = { viewModel.accept(Msg.CreateCoreToggle(it)) },
            // IS486 (В2): варианты CHOICE.
            optionDrafts = dialog.optionDrafts,
            optionsError = dialog.optionsError,
            onOptionAdd = { viewModel.accept(Msg.CreateOptionAdd) },
            onOptionChange = { index, value -> viewModel.accept(Msg.CreateOptionChange(index, value)) },
            onOptionRemove = { viewModel.accept(Msg.CreateOptionRemove(it)) },
        )
    }
    state.deleteConfirm?.let { dialog ->
        val impactRef = dialog.impact?.let { i ->
            DeletionImpactRef(
                valueCount = i.valueCount,
                dictCount = i.dictionariesWithValues.size,
                quizCount = i.affectedQuizConfigs.size,
                prefsCount = i.affectedPrefs.size,
            )
        }
        DeleteComponentConfirmDialog(
            name = dialog.name,
            impact = impactRef,
            isLoadingImpact = dialog.isLoadingImpact,
            isSubmitting = state.isDeleting,
            onConfirm = { viewModel.accept(Msg.ConfirmDelete) },
            onDismiss = { viewModel.accept(Msg.CloseDeleteConfirm) },
        )
    }
    state.editDialog?.let { dialog ->
        val preview = dialog.impactedLexemesPreview
        val inlineIds: List<Long>?
        val totalCount: Int
        val showAllVisible: Boolean
        when (preview) {
            null -> {
                inlineIds = null
                totalCount = 0
                showAllVisible = false
            }
            is ImpactedLexemesPreview.InlineOnly -> {
                inlineIds = preview.impactedLexemeIds
                totalCount = preview.impactedLexemeIds.size
                showAllVisible = false
            }
            is ImpactedLexemesPreview.InlineWithDrillIn -> {
                inlineIds = preview.inlineIds
                totalCount = preview.impactedLexemeIds.size
                showAllVisible = true
            }
        }
        val context = androidx.compose.ui.platform.LocalContext.current
        // IS486: dirty от иерархии/опций — цель, ядро, rename существующих, новые черновики.
        val optionsDirty = dialog.existingOptions.any { it.label != it.originalLabel.orEmpty() } ||
            dialog.newOptionDrafts.any { it.isNotBlank() }
        EditComponentDialog(
            name = dialog.name,
            template = dialog.template,
            isMultiple = dialog.isMultiple,
            originalName = dialog.originalName,
            originalTemplate = dialog.originalTemplate,
            originalIsMultiple = dialog.originalIsMultiple,
            nameErrorRes = dialog.nameError?.toLabelRes(),
            previewInlineIds = inlineIds,
            previewTotalCount = totalCount,
            previewShowAllVisible = showAllVisible,
            lexemeLabel = { id ->
                context.getString(R.string.components_edit_lexeme_label, id)
            },
            isSubmitting = state.isEditing,
            onNameChange = { viewModel.accept(Msg.EditNameChange(it)) },
            onTemplateSelect = { viewModel.accept(Msg.EditTemplateChange(it)) },
            onMultiToggle = { viewModel.accept(Msg.EditMultiToggle(it)) },
            onShowAllImpacted = { /* TBD: drill-in destination — backlog */ },
            onSubmit = { viewModel.accept(Msg.SubmitEdit) },
            onDismiss = { viewModel.accept(Msg.CloseEditDialog) },
            // IS486 (В1): пикер цели — без самого редактируемого компонента и его опций.
            targetItems = targetPickerItems(rows = state.items, excludeTypeId = dialog.typeId),
            selectedTarget = dialog.target,
            core = dialog.core,
            onTargetSelect = { viewModel.accept(Msg.EditTargetChange(it)) },
            onCoreToggle = { viewModel.accept(Msg.EditCoreToggle(it)) },
            // IS486 (В2): варианты CHOICE.
            existingOptions = dialog.existingOptions.map { it.optionId to it.label },
            newOptionDrafts = dialog.newOptionDrafts,
            onOptionLabelChange = { optionId, value ->
                viewModel.accept(Msg.EditOptionLabelChange(optionId, value))
            },
            onOptionDeleteClick = { viewModel.accept(Msg.EditOptionDeleteRequest(it)) },
            onOptionDraftAdd = { viewModel.accept(Msg.EditOptionDraftAdd) },
            onOptionDraftChange = { index, value ->
                viewModel.accept(Msg.EditOptionDraftChange(index, value))
            },
            onOptionDraftRemove = { viewModel.accept(Msg.EditOptionDraftRemove(it)) },
            extraDirty = dialog.target != dialog.originalTarget ||
                dialog.core != dialog.originalCore ||
                optionsDirty,
        )

        // IS486 (В2): вложенный конфирм удаления опции — поверх Edit-диалога.
        dialog.optionDeleteConfirm?.let { confirm ->
            OptionDeleteConfirmDialog(
                label = confirm.label,
                valueCount = confirm.impact?.valueCount,
                descendantValueCount = confirm.impact?.descendantValueCount,
                degradedCount = confirm.impact?.degradedComponents?.size,
                isLoadingImpact = confirm.isLoadingImpact,
                isSubmitting = dialog.isDeletingOption,
                onConfirm = { viewModel.accept(Msg.ConfirmOptionDelete) },
                onDismiss = { viewModel.accept(Msg.CloseOptionDeleteConfirm) },
            )
        }

        // IS486 умный сброс: обязательный конфирм перепривязки — поверх Edit-диалога.
        dialog.rebindConfirm?.let { confirm ->
            RebindConfirmDialog(
                valueCount = confirm.impact?.valueCount,
                descendantValueCount = confirm.impact?.descendantValueCount,
                isLoadingImpact = confirm.isLoadingImpact,
                isSubmitting = state.isEditing,
                onConfirm = { viewModel.accept(Msg.ConfirmRebind) },
                onDismiss = { viewModel.accept(Msg.CloseRebindConfirm) },
            )
        }
    }
}

/**
 * IS486 (В1): кандидаты цели зависимости — все живые компоненты словаря
 * (builtin включая; зависимость от «Части речи» и её опций — ключевой сценарий)
 * + вложенные опции CHOICE. [excludeTypeId] — сам редактируемый компонент и его
 * опции (самозависимость бессмысленна).
 *
 * Решение 2026-07-21 (C2): цели, создающие цикл, — задизейблены с подписью
 * «нельзя — цикл» (компонент-ряд; его опции просто приглушены).
 */
@Composable
private fun targetPickerItems(
    rows: List<PerDictRow>?,
    excludeTypeId: ComponentTypeId?,
): List<TargetPickerItem> = rows.orEmpty()
    .filter { it.typeId != excludeTypeId }
    .flatMap { row ->
        val cyclic = excludeTypeId != null &&
            createsCycle(rows.orEmpty(), excludeTypeId, DependencyTarget.Component(row.typeId))
        val componentItem = TargetPickerItem(
            target = DependencyTarget.Component(row.typeId),
            label = componentDisplayName(systemKey = row.systemKey, name = row.name),
            enabled = !cyclic,
            showCycleHint = cyclic,
        )
        val optionItems = row.options.map { option ->
            TargetPickerItem(
                target = DependencyTarget.Option(option.optionId),
                label = optionDisplayLabel(systemKey = option.systemKey, label = option.label),
                indent = true,
                enabled = !cyclic,
            )
        }
        listOf(componentItem) + optionItems
    }

/**
 * IS481 phase 2 — local маппинг [EditNameError] → StringRes.
 */
@androidx.annotation.StringRes
private fun EditNameError.toLabelRes(): Int = when (this) {
    EditNameError.NameEmpty -> R.string.components_name_error_empty
    EditNameError.SameScopeCollision -> R.string.components_name_error_same_scope_collision
    EditNameError.CrossScopeCollision -> R.string.components_name_error_cross_scope_collision
}
