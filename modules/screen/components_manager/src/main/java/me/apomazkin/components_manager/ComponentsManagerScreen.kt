@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.components_manager

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
import me.apomazkin.component_widgets.dialogs.DictionaryRef
import me.apomazkin.component_widgets.dialogs.EditComponentDialog
import me.apomazkin.component_widgets.dialogs.HostVariant
import me.apomazkin.component_widgets.widgets.ComponentsEmptyStateWidget
import me.apomazkin.component_widgets.widgets.CreateComponentFab
import me.apomazkin.component_widgets.widgets.UserDefinedRowWidget
import me.apomazkin.components_manager.mate.EditNameError
import me.apomazkin.components_manager.mate.ImpactedLexemesPreview
import me.apomazkin.components_manager.mate.Msg
import me.apomazkin.components_manager.mate.isEmpty
import me.apomazkin.core_resources.R
import me.apomazkin.di.viewModelFactory
import me.apomazkin.lexeme.Scope
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.ui.ErrorStateWidget

/**
 * IS481: `ComponentsManagerScreen` — aggregated CRUD view всех user-defined компонентов.
 *
 * Phase 2: dialogs/rows вынесены в `:modules:widget:component_widgets`. Mount'ит 4
 * диалога (Create + DeleteConfirm + Edit), CreateComponentDialog получает
 * multi-dict scope picker (hostVariant=Manager).
 */
@Composable
fun ComponentsManagerScreen(
    factory: ComponentsManagerViewModel.Factory,
    navigator: ComponentsManagerNavigator,
    viewModel: ComponentsManagerViewModel = viewModel(
        factory = viewModelFactory { factory.create(navigator) },
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
                        text = stringResource(id = R.string.components_manager_title),
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
            val rows = state.userDefinedTypes
            when {
                state.isLoading && rows == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // F163: error state — initial load упал, rows == null и не идёт loading.
                !state.isLoading && rows == null -> {
                    ErrorStateWidget(
                        modifier = Modifier.align(Alignment.Center),
                        messageRes = R.string.components_manager_load_failed,
                        retryRes = R.string.components_error_retry,
                        onRetry = { viewModel.accept(Msg.OnRetryClick) },
                    )
                }

                state.isEmpty -> {
                    ComponentsEmptyStateWidget(
                        headlineRes = R.string.components_empty_headline_manager,
                        bodyRes = R.string.components_empty_body_manager,
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
                            UserDefinedRowWidget(
                                typeId = row.typeId,
                                name = row.name,
                                template = row.template,
                                isMultiple = row.isMultiple,
                                isGlobal = row.scope is Scope.Global,
                                usageCount = row.usageCount,
                                dictionaryNames = row.dictionaryNames,
                                onEdit = { viewModel.accept(Msg.OpenEditDialog(it)) },
                                onDelete = { viewModel.accept(Msg.OpenDeleteConfirm(it)) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog overlays
    state.createDialog?.let { dialog ->
        CreateComponentDialog(
            name = dialog.name,
            template = dialog.template,
            isMultiple = dialog.isMultiple,
            scope = dialog.scope,
            nameError = dialog.nameError,
            isSubmitting = state.isCreating,
            availableDictionaries = state.availableDictionaries.map {
                DictionaryRef(id = it.id, name = it.name)
            },
            selectedDictionaryIds = dialog.selectedDictionaryIds,
            hostVariant = HostVariant.Manager,
            onNameChange = { viewModel.accept(Msg.CreateNameChange(it)) },
            onTemplateSelect = { viewModel.accept(Msg.CreateTemplateChange(it)) },
            onMultiToggle = { viewModel.accept(Msg.CreateMultiToggle(it)) },
            onScopeChange = { viewModel.accept(Msg.CreateScopeChange(it)) },
            onDictionaryToggle = { viewModel.accept(Msg.CreateDictionaryToggle(it)) },
            onSubmit = { viewModel.accept(Msg.SubmitCreate) },
            onDismiss = { viewModel.accept(Msg.CloseCreateDialog) },
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
            onShowAllImpacted = { /* TBD: drill-in destination — backlog (bottom-sheet/screen) */ },
            onSubmit = { viewModel.accept(Msg.SubmitEdit) },
            onDismiss = { viewModel.accept(Msg.CloseEditDialog) },
        )
    }
}

/**
 * IS481 phase 2 — local маппинг [EditNameError] → StringRes. Shared widget принимает
 * `Int?` (resolved res), а не mate enum (избегаем coupling shared widget на screen-mate).
 */
@androidx.annotation.StringRes
private fun EditNameError.toLabelRes(): Int = when (this) {
    EditNameError.NameEmpty -> R.string.components_name_error_empty
    EditNameError.SameScopeCollision -> R.string.components_name_error_same_scope_collision
    EditNameError.CrossScopeCollision -> R.string.components_name_error_cross_scope_collision
}
