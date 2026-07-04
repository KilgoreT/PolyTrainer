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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import me.apomazkin.component_widgets.dialogs.RenameComponentDialog
import me.apomazkin.component_widgets.widgets.ComponentsEmptyStateWidget
import me.apomazkin.component_widgets.widgets.CreateComponentFab
import me.apomazkin.component_widgets.widgets.PerDictRowWidget
import me.apomazkin.core_resources.R
import me.apomazkin.di.viewModelFactory
import me.apomazkin.per_dictionary_components.mate.EditNameError
import me.apomazkin.per_dictionary_components.mate.ImpactedLexemesPreview
import me.apomazkin.per_dictionary_components.mate.Msg
import me.apomazkin.per_dictionary_components.mate.isEmpty
import me.apomazkin.ui.ErrorStateWidget

/**
 * IS481: `PerDictionaryComponentsScreen` — scoped CRUD view (global ∪ per-dict).
 *
 * Phase 2: dialogs/rows вынесены в `:modules:widget:component_widgets`. Mount'ит 4
 * диалога (Create + Rename + DeleteConfirm + Edit), CreateComponentDialog получает
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.dictionaryName
                            ?: stringResource(id = R.string.per_dict_components_title),
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
                                name = row.name,
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
        )
    }
    state.renameDialog?.let { dialog ->
        RenameComponentDialog(
            originalName = dialog.originalName,
            editedName = dialog.editedName,
            nameError = dialog.nameError,
            isSubmitting = state.isRenaming,
            onNameChange = { viewModel.accept(Msg.RenameTextChange(it)) },
            onSubmit = { viewModel.accept(Msg.SubmitRename) },
            onDismiss = { viewModel.accept(Msg.CloseRenameDialog) },
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
        )
    }
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
