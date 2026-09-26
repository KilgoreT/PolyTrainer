package me.apomazkin.groupstab.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.groupstab.R
import me.apomazkin.groupstab.logic.GroupSheetError
import me.apomazkin.groupstab.logic.GroupSheetState
import me.apomazkin.groupstab.logic.Msg
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.panel.InputDockedPanelWidget
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS493 Э3 (D15.1 v5) → IS496: панель создания/переименования группы —
 * общий [InputDockedPanelWidget] (немодальная, фон живой — Р1/Р2 брифа
 * IS496; единая вёрстка с панелью слова words). Ошибка валидации —
 * строкой под полем (снекбар под клавиатурой не виден — итог ручного
 * прогона Э3).
 */
@Composable
internal fun GroupInputPanelWidget(
    state: GroupSheetState,
    sendMessage: (Msg) -> Unit,
    modifier: Modifier = Modifier,
) {
    InputDockedPanelWidget(
        modifier = modifier,
        value = state.input,
        isSendEnabled = state.input.isNotBlank() && !state.isSubmitting,
        onValueChange = { sendMessage(Msg.SheetInputChanged(it)) },
        onSendAction = { sendMessage(Msg.SubmitSheet) },
        onDismissRequest = { sendMessage(Msg.DismissSheet) },
        errorText = state.error?.let { error ->
            stringResource(
                id = when (error) {
                    GroupSheetError.EMPTY -> R.string.group_error_empty_name
                    GroupSheetError.DUPLICATE -> R.string.group_error_duplicate_name
                    GroupSheetError.RESERVED -> R.string.group_error_reserved_name
                    // Inline в шторке сбои операций не показываются (reducer
                    // шлёт их только снекбаром), тексты — на случай появления.
                    GroupSheetError.CREATE_FAILED -> R.string.group_error_create_failed
                    GroupSheetError.RENAME_FAILED -> R.string.group_error_rename_failed
                    GroupSheetError.DELETE_FAILED -> R.string.group_error_delete_failed
                },
            )
        },
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        GroupInputPanelWidget(
            state = GroupSheetState(
                mode = me.apomazkin.groupstab.logic.GroupSheetMode.Create,
                input = "Дом",
                error = GroupSheetError.DUPLICATE,
            ),
            sendMessage = {},
        )
    }
}
