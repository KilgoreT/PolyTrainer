package me.apomazkin.main.widget

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.main.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.unselectedGreyColor
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun RowScope.BottomBarItem(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = isSelected,
        icon = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = stringResource(id = titleRes),
            )
        },
        label = {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.labelMedium,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = unselectedGreyColor,
            unselectedTextColor = unselectedGreyColor,
            indicatorColor = Color.White,
        ),
        onClick = onClick,
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Row(
            modifier = Modifier
                .height(80.dp)
        ) {
            BottomBarItem(
                titleRes = R.string.item_title_vocabulary,
                iconRes = R.drawable.ic_tab_vocabulary,
                isSelected = false
            ) {}
        }
    }
}

@Composable
@PreviewWidget
private fun PreviewSelected() {
    AppTheme {
        Row(
            modifier = Modifier
                .height(80.dp)
        ) {
            BottomBarItem(
                titleRes = R.string.item_title_vocabulary,
                iconRes = R.drawable.ic_tab_vocabulary,
                isSelected = true
            ) {}
        }
    }
}