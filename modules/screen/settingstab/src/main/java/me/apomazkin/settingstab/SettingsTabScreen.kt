package me.apomazkin.settingstab

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.settingstab.contract.FileShareContract
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import me.apomazkin.settingstab.logic.DbExportDialogState
import me.apomazkin.settingstab.logic.Msg
import me.apomazkin.settingstab.logic.SettingsTabState
import me.apomazkin.settingstab.widgets.settings.SettingsAppBar
import me.apomazkin.settingstab.widgets.settings.SettingsSectionWidget
import me.apomazkin.settingstab.widgets.settings.items.AboutAppWidget
import me.apomazkin.settingstab.widgets.settings.items.AppShareWidget
import me.apomazkin.settingstab.widgets.settings.items.ExportDataWidget
import me.apomazkin.settingstab.widgets.settings.items.FeedBackWidget
import me.apomazkin.settingstab.widgets.settings.items.ImportDataWidget
import me.apomazkin.settingstab.widgets.settings.items.LangManageWidget
import me.apomazkin.settingstab.widgets.settings.items.RateWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.preview.PreviewScreen

@Composable
fun SettingsTabScreen(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    settingsTabUseCase: SettingsTabUseCase,
    logger: LexemeLogger,
    viewModel: SettingsTabViewModel = viewModel(
        factory = SettingsTabViewModel.Factory(
            settingsTabUseCase = settingsTabUseCase,
            logger = logger,
        )
    ),
) {
    val state: SettingsTabState by viewModel.state.collectAsStateWithLifecycle()
    SettingsTabScreen(
        onLangManagementClick = onLangManagementClick,
        onAboutAppClick = onAboutAppClick,
        state = state,
        sendMessage = { viewModel.accept(it) }
    )
    
}

@Composable
internal fun SettingsTabScreen(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    state: SettingsTabState,
    sendMessage: (Msg) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = FileShareContract(),
        onResult = {
            sendMessage(Msg.FileExported)
        }
    )
    
    LaunchedEffect(state.showDbExportDialog) {
        if (state.showDbExportDialog is DbExportDialogState.Show) {
            val uri = state.showDbExportDialog.uri
            launcher.launch(uri)
        }
    }
    Scaffold(
        topBar = { SettingsAppBar() },
    ) { paddings: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {
            LazyColumn(
                modifier = Modifier,
                contentPadding = PaddingValues(
                    horizontal = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SettingsSectionWidget {
                        LangManageWidget(onClick = onLangManagementClick)
                        AboutAppWidget(onClick = onAboutAppClick)
                    }
                }
                item {
                    SettingsSectionWidget {
                        ExportDataWidget(
                            onClick = { sendMessage(Msg.ExportData(uri = it)) },
                        )
                        ImportDataWidget(
                            onClick = { sendMessage(Msg.ImportData(uri = it)) },
                        )
                    }
                }
                item {
                    SettingsSectionWidget {
                        FeedBackWidget()
                        RateWidget(onClick = null)
                        AppShareWidget()
                    }
                }
                
            }
            if (state.showExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 44.dp)
                        .background(color = Color.Red)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
@PreviewScreen
private fun Preview() {
    AppTheme {
        SettingsTabScreen(
            onLangManagementClick = {},
            onAboutAppClick = {},
            state = SettingsTabState(),
            sendMessage = {}
        )
    }
}