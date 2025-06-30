package me.apomazkin.dictionarytab.logic

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo


sealed interface Msg {

    data class ChangeDict(val current: DictUiEntity) : Msg

    /**
     * Message to show Term Data.
     */
    data class TermDataLoaded(
            val pattern: String,
            val termList: Flow<PagingData<TermUiItem>>,
    ) : Msg

    /**
     * Message to control AddWord dialog visibility.
     */
    data class ShowAddWordDialog(
            val wordValue: String? = null,
    ) : Msg

    data object HideAddWordDialog : Msg

    /**
     * Message to Start AddWord dialog to modify word.
     * @param wordId id of word to modify.
     */
    data class StartChangeWord(
            val wordId: Long,
            val wordValue: String,
    ) : Msg

    /**
     * Message to handle AddWord's TextField.onValueChange.
     */
    data class WordValueChange(val value: String) : Msg

    /**
     * Message to add new word.
     */
    data class AddWord(val value: String) : Msg

    /**
     * Message to change word.
     * @param wordId id of word to change.
     * @param value new value of word.
     */
    data class ChangeWord(val wordId: Long, val value: String) : Msg

    /**
     * Message to control ConfirmDeleteWord dialog visibility.
     */
    data class ShowConfirmDeleteWordDialog(val wordIds: Set<WordInfo>) : Msg
    data object HideConfirmDeleteWordDialog : Msg

    /**
     * Message to delete word.
     */
    data class DeleteWord(val wordIds: Set<WordInfo>) : Msg

    /**
     * Message to show or hide ActionBar with action buttons.
     * Also can add new word to action mode.
     */

    data class ShowActionMode(val targetWord: WordInfo) : Msg
    data object HideActionMode : Msg
    data class ModifySelectedInActionMode(val targetWord: WordInfo) : Msg

    /**
     * When no need action after Effect.
     */
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