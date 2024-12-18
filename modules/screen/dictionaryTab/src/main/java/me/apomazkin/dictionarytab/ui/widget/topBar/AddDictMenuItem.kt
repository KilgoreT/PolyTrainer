package me.apomazkin.dictionarytab.ui.widget.topBar

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.apomazkin.dictionarytab.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AddDictMenuItem(
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_circled),
                contentDescription = stringResource(id = R.string.menu_item_title_add_dict)
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.menu_item_title_add_dict),
                style = LexemeStyle.BodyL,
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
            AddDictMenuItem {}
        }
    }
}