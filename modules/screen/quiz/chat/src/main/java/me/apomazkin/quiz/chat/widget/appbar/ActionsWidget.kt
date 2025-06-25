package me.apomazkin.quiz.chat.widget.appbar

import androidx.compose.runtime.Composable
import me.apomazkin.icondropdowned.DividerMenuItem
import me.apomazkin.icondropdowned.IconDropdownWidget
import me.apomazkin.quiz.chat.logic.ItemsState
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.widget.appbar.menu.DebugMenuItem
import me.apomazkin.quiz.chat.widget.appbar.menu.EarliestReviewedMenuItem
import me.apomazkin.quiz.chat.widget.appbar.menu.MistakesMenuItem

@Composable
fun ActionsWidget(
        isActionsOpen: Boolean,
        state: ItemsState,
        sendMessage: (Msg) -> Unit,
) {
    IconDropdownWidget(
            isDropDownOpen = isActionsOpen,
            onClickDropDown = { sendMessage(Msg.ShowMenu) },
            onDismissRequest = { sendMessage(Msg.HideMenu) },
    ) {
        EarliestReviewedMenuItem(
                isChecked = state.earliest.isOn,
                onClick = { isChecked ->
                    val message = if (isChecked) Msg.EarliestOn else Msg.EarliestOff
                    sendMessage(message)
                }
        )
        MistakesMenuItem(
                isChecked = state.frequentMistakes.isOn,
                onClick = { isChecked ->
                    val message = if (isChecked) Msg.FrequentMistakesOn else Msg.FrequentMistakesOff
                    sendMessage(message)
                }
        )
        DividerMenuItem()
        DebugMenuItem(
                isChecked = state.debug.isOn,
                onClick = { isChecked ->
                    val message = if (isChecked) Msg.DebugOn else Msg.DebugOff
                    sendMessage(message)
                }
        )
    }
}