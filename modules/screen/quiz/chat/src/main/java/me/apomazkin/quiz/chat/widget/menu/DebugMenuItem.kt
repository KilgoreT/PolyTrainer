package me.apomazkin.quiz.chat.widget.menu

import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import me.apomazkin.quiz.chat.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun DebugMenuItem(
        isChecked: Boolean,
        onClick: (isChecked: Boolean) -> Unit,
) {
    DropdownMenuItem(
            leadingIcon = {
                Checkbox(
                        checked = isChecked,
                        onCheckedChange = onClick,
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurface
                        )
                )
            },
            text = {
                Text(
                        text = stringResource(id = R.string.chat_menu_item_show_debug),
                        style = LexemeStyle.BodyL,
                        color = MaterialTheme.colorScheme.onSurface
                )
            },
            onClick = { onClick.invoke(!isChecked)},
    )
}

@PreviewWidget
@Composable
private fun Preview(
        @PreviewParameter(BoolParam::class) enabled: Boolean,
) {
    DebugMenuItem(
            isChecked = enabled,
            onClick = {}
    )
}