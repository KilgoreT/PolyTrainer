package me.apomazkin.wordcard.widget

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.IconBoxed

@Composable
internal fun DeleteWordMenuItem(
    onDeleteClick: () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = {
            IconBoxed(
                iconRes = R.drawable.ic_delete,
                contentDescriptionRes = R.string.button_delete,
                size = 24,
                enabled = true,
                colorEnabled = MaterialTheme.colorScheme.onError,
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.button_delete),
                style = LexemeStyle.BodyL,
                color = MaterialTheme.colorScheme.onError
            )
        },
        onClick = onDeleteClick,
    )
}