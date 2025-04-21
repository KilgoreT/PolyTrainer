package me.apomazkin.settingstab.logic

import android.net.Uri
import androidx.compose.runtime.Immutable
import me.apomazkin.mate.EMPTY_STRING

/**
 * State
 */
@Immutable
data class SettingsTabState(
    val showExporting: Boolean = false,
    val showDbExportDialog: DbExportDialogState = DbExportDialogState.Hide,
    val snackbarState: SnackbarState = SnackbarState(),
)

fun SettingsTabState.showExporting(): SettingsTabState =
    copy(showExporting = true)

fun SettingsTabState.hideExporting(): SettingsTabState =
    copy(showExporting = false)

sealed interface DbExportDialogState {
    data object Hide : DbExportDialogState
    data class Show(val uri: Uri) : DbExportDialogState
}

fun SettingsTabState.showDbExportDialog(
    uri: Uri
): SettingsTabState =
    copy(showDbExportDialog = DbExportDialogState.Show(uri))

fun SettingsTabState.hideDbExportDialog(): SettingsTabState =
    copy(showDbExportDialog = DbExportDialogState.Hide)

@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)