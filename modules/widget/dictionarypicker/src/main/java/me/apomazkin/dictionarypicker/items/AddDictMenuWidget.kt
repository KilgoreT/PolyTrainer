package me.apomazkin.dictionarypicker.items

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import me.apomazkin.dictionarypicker.R
import me.apomazkin.icondropdowned.IconSource
import me.apomazkin.icondropdowned.MenuItem
import me.apomazkin.icondropdowned.StringSource
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AddDictMenuWidget(
    onClick: () -> Unit,
) {
    MenuItem
            .withIcon(
                    icon = IconSource.fromResId(
                            resId = R.drawable.ic_add_circled,
                            tint = MaterialTheme.colorScheme.onTertiary,
                    ),
                    title = StringSource.fromRes(
                            resId = R.string.menu_item_title_add_dict,
                            style = LexemeStyle.BodyL,
                    ),
                    onClick = onClick,
            )
            .Widget()
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Column {
            AddDictMenuWidget {}
        }
    }
}