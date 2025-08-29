package me.apomazkin.dictionarytab.logic

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo


sealed interface Msg {

    /**
     * Message to select and activate a dictionary for word operations.
     * @param current The dictionary entity to be selected as active.
     */
    data class SelectDictionary(val current: DictUiEntity) : Msg

    /**
     * Message indicating that terms data has been loaded from the repository.
     * @param pattern The search pattern used to filter terms.
     * @param termList Flow of paginated term items to be displayed.
     */
    data class TermsLoaded(
            val pattern: String,
            val termList: Flow<PagingData<TermUiItem>>,
    ) : Msg

    /**
     * Message to open dialog for creating a new word.
     * @param wordValue Optional pre-filled text value for the new word.
     */
    data class OpenAddWordDialog(
            val wordValue: String? = null,
    ) : Msg

    /**
     * Message to close the add word dialog.
     */
    data object CloseAddWordDialog : Msg

    /**
     * Message to open dialog for editing an existing word.
     * @param wordId The unique identifier of the word to be edited.
     * @param wordValue The current text value of the word to be edited.
     */
    data class OpenEditWordDialog(
            val wordId: Long,
            val wordValue: String,
    ) : Msg

    /**
     * Message to update the input field value when user types in word creation/editing dialog.
     * @param value The current text value entered by the user.
     */
    data class UpdateWordInput(val value: String) : Msg

    /**
     * Message to create new word in the dictionary.
     * @param value The text value of the word to be created.
     */
    data class CreateWord(val value: String) : Msg

    /**
     * Message to update existing word in the dictionary.
     * @param wordId The unique identifier of the word to be updated.
     * @param value The new text value for the word.
     */
    data class UpdateWord(val wordId: Long, val value: String) : Msg

    /**
     * Message to open confirmation dialog for word deletion.
     * @param wordIds Set of words selected for deletion.
     */
    data class OpenDeleteConfirmation(val wordIds: Set<WordInfo>) : Msg

    /**
     * Message to close the word deletion confirmation dialog.
     */
    data object CloseDeleteConfirmation : Msg

    /**
     * Message to remove words from the dictionary.
     * @param wordIds Set of words to be removed.
     */
    data class RemoveWords(val wordIds: Set<WordInfo>) : Msg

    /**
     * Message to enter selection mode for multiple word operations.
     * @param targetWord The word that triggered the selection mode.
     */
    data class EnterSelectionMode(val targetWord: WordInfo) : Msg

    /**
     * Message to exit selection mode.
     */
    data object ExitSelectionMode : Msg

    /**
     * Message to toggle word selection in selection mode.
     * @param targetWord The word to toggle selection for.
     */
    data class ToggleSelection(val targetWord: WordInfo) : Msg

    /**
     * Message indicating no operation is needed after an effect.
     */
    data object NoOperation : Msg
}

sealed interface UiMsg : Msg {
    /**
     * Message to show or hide notification snackbar.
     * @param message The text content of the notification.
     * @param show Flag to control notification visibility.
     */
    data class ShowNotification(val message: String, val show: Boolean) : UiMsg
    /**
     * Message to handle application lifecycle events.
     * @param lifecycle The current lifecycle state of the application.
     */
    data class LifecycleEvent(val lifecycle: Lifecycle) : UiMsg {
        enum class Lifecycle {
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