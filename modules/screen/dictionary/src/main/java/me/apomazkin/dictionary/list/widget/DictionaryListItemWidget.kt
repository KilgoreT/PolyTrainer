package me.apomazkin.dictionary.list.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.model.DictionaryListItem
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.enableIconColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun DictionaryListItemWidget(
    item: DictionaryListItem,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.flagRes != null) {
            ImageFlagWidget(flagRes = item.flagRes)
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_vocabulary),
                contentDescription = null,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = item.name,
            style = LexemeStyle.BodyL,
            color = MaterialTheme.colorScheme.secondary,
        )
        IconBoxed(
            iconRes = R.drawable.ic_trash,
            enabled = true,
            colorEnabled = enableIconColor,
            size = 44,
            onClick = onDeleteClick,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        DictionaryListItemWidget(
            item = DictionaryListItem(
                id = 1L,
                name = "English",
            ),
            onItemClick = {},
            onDeleteClick = {},
        )
    }
}
