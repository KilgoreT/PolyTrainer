package me.apomazkin.wordcard.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import me.apomazkin.core_resources.R
import me.apomazkin.icondropdowned.IconSource
import me.apomazkin.icondropdowned.MenuItem
import me.apomazkin.icondropdowned.StringSource
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun DeleteWordMenuItem(
        onDeleteClick: () -> Unit,
) {
    MenuItem
            .withIcon(
                    icon = IconSource.fromResId(
                            resId = R.drawable.ic_delete,
                            tint = MaterialTheme.colorScheme.onError,
                    ),
                    title = StringSource.fromRes(
                            resId = R.string.button_delete,
                            style = LexemeStyle.BodyL,
                            color = MaterialTheme.colorScheme.onError,
                    ),
                    onClick = onDeleteClick,
            )
            .Widget()
}

@Composable
@PreviewWidget
private fun DeleteWordMenuItemPreview() {
    AppTheme {
        DeleteWordMenuItem(
                onDeleteClick = {}
        )
    }
}