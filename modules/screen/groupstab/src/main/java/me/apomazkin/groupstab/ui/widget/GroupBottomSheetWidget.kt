package me.apomazkin.groupstab.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.groupstab.R
import me.apomazkin.groupstab.logic.GroupSheetError
import me.apomazkin.groupstab.logic.GroupSheetState
import me.apomazkin.groupstab.logic.Msg
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.ui.sheet.InputBottomSheetWidget

/**
 * IS493 Э3 (D15.1 v5): шторка создания/переименования группы — общий
 * [InputBottomSheetWidget] (единая вёрстка со шторкой слова words).
 * Ошибка валидации — строкой под полем (снекбар под шторкой/клавиатурой
 * не виден — итог ручного прогона).
 */
@Composable
internal fun GroupBottomSheetWidget(
    state: GroupSheetState,
    sendMessage: (Msg) -> Unit,
) {
    InputBottomSheetWidget(
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
                }
            )
        },
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        GroupBottomSheetWidget(
            state = GroupSheetState(
                mode = me.apomazkin.groupstab.logic.GroupSheetMode.Create,
                input = "Дом",
                error = GroupSheetError.DUPLICATE,
            ),
        ) {}
    }
}
