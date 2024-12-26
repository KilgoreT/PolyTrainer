package me.apomazkin.quiztab.logic

import androidx.compose.material3.DropdownMenu
import me.apomazkin.dictionarypicker.entity.DictUiEntity


sealed interface Msg {
    
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
    data class CurrentDict(val lang: DictUiEntity) : TopBarActionMsg
    
    /**
     * Message to change current language.
     */
    data class ChangeDict(val lang: DictUiEntity) : TopBarActionMsg
    
    /**
     * Message to expand or collapse Language [DropdownMenu].
     */
    data class ExpandDictMenu(val expand: Boolean) : TopBarActionMsg
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