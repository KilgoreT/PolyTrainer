package me.apomazkin.main.widget.bottom

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.main.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.M3Primary95
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun RowScope.MainBottomBarItem(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    @DrawableRes iconSelectedRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = isSelected,
        icon = {
            Icon(
                painter = painterResource(id = if (isSelected) iconSelectedRes else iconRes),
                contentDescription = stringResource(id = titleRes)
            )
        },
        label = {
            Text(
                text = stringResource(id = titleRes),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = M3Primary95
        ),
        onClick = onClick,
    )
}

@Composable
@PreviewWidgetRu
@PreviewWidgetEn
private fun Preview() {
    AppTheme {
        Row(
            modifier = Modifier
                .height(80.dp)
        ) {
            MainBottomBarItem(
                titleRes = R.string.item_title_vocabulary,
                iconRes = R.drawable.ic_tab_vocabulary,
                iconSelectedRes = R.drawable.ic_tab_vocabulary_selected,
                isSelected = false
            ) {}
        }
    }
}

@Composable
@PreviewWidgetRu
@PreviewWidgetEn
private fun PreviewSelected() {
    AppTheme {
        Row(
            modifier = Modifier
                .height(80.dp)
        ) {
            MainBottomBarItem(
                titleRes = R.string.item_title_vocabulary,
                iconRes = R.drawable.ic_tab_vocabulary,
                iconSelectedRes = R.drawable.ic_tab_vocabulary_selected,
                isSelected = true
            ) {}
        }
    }
}