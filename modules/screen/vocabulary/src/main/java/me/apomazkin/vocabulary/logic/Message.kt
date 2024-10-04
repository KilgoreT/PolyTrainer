package me.apomazkin.vocabulary.logic

import androidx.compose.material3.DropdownMenu
import me.apomazkin.vocabulary.entity.DictUiEntity
import me.apomazkin.vocabulary.entity.LexemeLabel
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.entity.WordInfo


sealed interface Msg {

    /**
     * Message to Load or Reload Data.
     */
    data object TermDataLoad : Msg

    /**
     * Message to show Term Data.
     */
    data class TermDataLoaded(val termList: List<TermUiItem>) : Msg

    /**
     * Message to control AddWord dialog visibility.
     */
    data class StartAddWord(
        val show: Boolean,
        val wordValue: String? = null,
    ) : Msg

    /**
     * Message to Start AddWord dialog to modify word.
     * @param wordId id of word to modify.
     */
    data class StartChangeWord(
        val wordId: Long,
        val wordValue: String
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
    data class ConfirmDeleteWordDialog(val isOpen: Boolean, val wordIds: Set<WordInfo>) : Msg

    /**
     * Message to delete word.
     */
    data class DeleteWord(val wordIds: Set<WordInfo>) : Msg

    /**
     * Message to show or hide ActionBar with action buttons.
     * Also can add new word to action mode.
     */
    data class ChangeActionMode(val isActionMode: Boolean, val targetWord: WordInfo? = null) : Msg

    /**
     * When no need action after Effect.
     */
    data object Empty : Msg
}

sealed interface TopBarActionMsg : Msg {

    /**
     * Message to show languages.
     */
    data class AvailableDict(val list: List<DictUiEntity>) : TopBarActionMsg

    /**
     * Message to show current language.
     */
    data class CurrentDict(val numericCode: Int) : TopBarActionMsg

    /**
     * Message to change current language.
     */
    data class ChangeDict(val numericCode: Int) : TopBarActionMsg

    /**
     * Message to expand or collapse Language [DropdownMenu].
     */
    data class ExpandDictMenu(val expand: Boolean) : TopBarActionMsg
}

sealed interface WordDetailMsg : Msg {

    /**
     * Message to show or hide word detail dialog.
     */
    data class Show(val wordId: Long, val word: String = "") : WordDetailMsg

    /**
     * Message to hide word detail dialog.
     */
    object Hide : WordDetailMsg

    /**
     * Message to reset LexemeCategory to [me.apomazkin.vocabulary.entity.LexemeLabel.UNDEFINED]
     */
    data class ResetLexemeCategory(val lexemeId: Long?) : WordDetailMsg

    /**
     * Message to select LexemeCategory.
     */
    data class LexicalCategory(
        val lexemeId: Long?,
        val category: LexemeLabel
    ) : WordDetailMsg

    data class DefinitionEditStart(val lexemeId: Long?) : WordDetailMsg
    data class DefinitionEditFinish(val lexemeId: Long?, val value: String) : WordDetailMsg

    /**
     * Message to update definition.
     */
    data class DefinitionUpdate(val lexemeId: Long?, val value: String) : WordDetailMsg

    /**
     * Message to update translation.
     */
    data class TranslationUpdate(val lexemeId: Long?, val value: String) : WordDetailMsg

    /**
     * Message to update Sample.
     */
    data class ExampleUpdate(val value: String) : WordDetailMsg

    /**
     * Message to save Lexeme.
     */
    data class Save(
        val wordId: Long,
        val lexemeList: List<LexemeState>,
    ) : WordDetailMsg

    data class AddLexeme(val wordId: Long) : WordDetailMsg

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