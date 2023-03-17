package me.apomazkin.vocabulary.ui.widget.topBar

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.R

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