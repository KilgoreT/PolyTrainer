package me.apomazkin.main.widget.top

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.apomazkin.main.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AddLangMenuItem(
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_item_add),
                contentDescription = stringResource(id = R.string.menu_item_title_add_lang)
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.menu_item_title_add_lang),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        onClick = onClick
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Column {
            AddLangMenuItem {}
        }
    }
}