package me.apomazkin.dictionary.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.list.widget.ConfirmDeleteDictionaryWidget
import me.apomazkin.dictionary.list.widget.DictionaryListItemWidget
import me.apomazkin.dictionary.list.widget.EmptyDictionaryWidget
import me.apomazkin.dictionary.widget.DictionaryAppBar
import me.apomazkin.dictionary.widget.LoadingWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DictionaryListScreen(
    dictionaryUseCase: DictionaryUseCase,
    viewModel: DictionaryListViewModel = viewModel(
        factory = DictionaryListViewModel.Factory(dictionaryUseCase)
    ),
    onBackPress: (() -> Unit)? = null,
    onExit: () -> Unit = {},
    onOpenForm: (id: Long?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val effectiveBackPress: (() -> Unit)? = if (state.dictionaries.isEmpty()) {
        { onExit() }
    } else {
        onBackPress
    }
    DictionaryListScreen(
        state = state,
        onBackPress = effectiveBackPress,
        onExit = onExit,
        onOpenForm = onOpenForm,
    ) { viewModel.accept(it) }
}

@Composable
internal fun DictionaryListScreen(
    state: DictionaryListScreenState,
    onBackPress: (() -> Unit)? = null,
    onExit: () -> Unit = {},
    onOpenForm: (id: Long?) -> Unit,
    sendMsg: (DictionaryListMsg) -> Unit,
) {
    BackHandler {
        if (state.dictionaries.isEmpty()) {
            onExit()
        } else {
            onBackPress?.invoke()
        }
    }
    SystemBarsWidget(
        color = whiteColor,
    )
    Scaffold(
        topBar = {
            onBackPress?.let { DictionaryAppBar(onBackPress = it) }
        }
    ) { paddings ->
        Box(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .background(color = whiteColor)
        ) {
            if (state.isLoading) {
                LoadingWidget()
            } else {
                DictionaryListContent(
                    state = state,
                    onOpenForm = onOpenForm,
                    sendMsg = sendMsg,
                )
            }
        }
    }

    if (state.deleteDialogState.show) {
        ConfirmDeleteDictionaryWidget(
            dictionaryName = state.deleteDialogState.dictionaryName,
            onConfirm = { sendMsg(DictionaryListMsg.ConfirmDelete) },
            onDismiss = { sendMsg(DictionaryListMsg.DismissDelete) },
        )
    }
}

@Composable
private fun DictionaryListContent(
    state: DictionaryListScreenState,
    onOpenForm: (id: Long?) -> Unit,
    sendMsg: (DictionaryListMsg) -> Unit,
) {
    val dictionaries = state.dictionaries

    if (dictionaries.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EmptyDictionaryWidget(
                modifier = Modifier.weight(1f),
            )
            PrimaryFullButtonWidget(
                modifier = Modifier.padding(horizontal = 16.dp),
                titleRes = R.string.dictionary_new,
                onClick = { onOpenForm(null) },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(dictionaries) { item ->
                    DictionaryListItemWidget(
                        item = item,
                        onItemClick = { onOpenForm(item.id) },
                        onDeleteClick = {
                            sendMsg(DictionaryListMsg.RequestDelete(item.id, item.name))
                        },
                    )
                }
            }
            PrimaryFullButtonWidget(
                modifier = Modifier.padding(horizontal = 16.dp),
                titleRes = R.string.dictionary_new,
                onClick = { onOpenForm(null) },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
@PreviewWidget
private fun PreviewList() {
    AppTheme {
        DictionaryListScreen(
            state = DictionaryListScreenState(isLoading = false),
            onOpenForm = {},
        ) {}
    }
}
