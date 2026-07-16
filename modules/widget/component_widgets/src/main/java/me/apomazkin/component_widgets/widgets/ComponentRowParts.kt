package me.apomazkin.component_widgets.widgets

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.iconButtonBg
import me.apomazkin.theme.typeIconBg

/** Квадратная иконка типа компонента (Figma 5027:1549): 42dp r12 на лавандовом фоне. */
@Composable
internal fun ComponentTypeIcon(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(42.dp),
        shape = RoundedCornerShape(12.dp),
        color = typeIconBg,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(id = R.drawable.ic_text_lines),
                contentDescription = null,
                tint = LexemeColor.primary,
            )
        }
    }
}

/** Кнопка-иконка строки компонента (Figma 5027:1554/1557): 34dp r10 на серой плашке. */
@Composable
internal fun ComponentIconButton(
    @DrawableRes iconRes: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(34.dp),
        shape = RoundedCornerShape(10.dp),
        color = iconButtonBg,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tint,
            )
        }
    }
}
