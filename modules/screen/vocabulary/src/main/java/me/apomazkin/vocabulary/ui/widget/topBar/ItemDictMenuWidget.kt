package me.apomazkin.vocabulary.ui.widget.topBar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.vocabulary.R

@Composable
fun ItemDictMenuWidget(
    @DrawableRes iconRes: Int,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = {
            ImageFlagWidget(
                flagRes = iconRes,
                contentDescription = title
            )
        },
        text = {
            Text(
                text = title,
                style = LexemeStyle.BodyL,
                maxLines = 2,
            )
        },
        trailingIcon = {
            if (isSelected) {
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    painter = painterResource(id = R.drawable.ic_selected),
                    contentDescription = "Selected dictionary",
                )
            }
        },
        onClick = onClick,
    )
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        Column {
            ItemDictMenuWidget(
                iconRes = R.drawable.example_ic_flag_gb,
                title = "Бритишь",
                isSelected = true,
            ) {}
        }
    }
}