package me.apomazkin.settingstab.logic

import android.net.Uri


sealed interface Msg {
    data class ExportData(val uri: Uri) : Msg
    data class ExportFile(val uri: Uri) : Msg
    data object FileExported : Msg
    data class ImportData(val uri: Uri) : Msg
    data object ImportSuccess : Msg
    data object ImportFailed : Msg
    data object Empty : Msg
}


sealed interface UiMsg : Msg {
    /**
     * Message for Snackbar
     * @param message text of Snackbar.
     * @param show variable to reset show status for state.
     */
    data class Snackbar(val message: String, val show: Boolean) : UiMsg
    data class LifeCycleEvent(val lifeCycle: LifeCycle) : UiMsg {
        enum class LifeCycle {
            ON_CREATE,
            ON_START,
            ON_RESUME,
            ON_PAUSE,
            ON_STOP,
            ON_DESTROY,
            ON_ANY,
        }
    }
}