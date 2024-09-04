package me.apomazkin.createdictionary.widget

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.apomazkin.createdictionary.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.White
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_HORIZONTAL_PADDING = 8

@Composable
fun LanguageItemWidget(
    @DrawableRes flagRes: Int,
    langName: String,
    langNumericCode: Int,
    isSelected: Boolean,
    onClick: (numericCode: Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick.invoke(langNumericCode) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageFlagWidget(
            flagRes = flagRes,
            modifier = Modifier
                .padding(start = DEFAULT_HORIZONTAL_PADDING.dp),
            contentDescription = langName,
        )
        Text(
            modifier = Modifier
                .weight(1F),
            text = langName,
            style = MaterialTheme.typography.bodyLarge
        )
        if (isSelected) {
            Icon(
                modifier = Modifier
                    .padding(end = DEFAULT_HORIZONTAL_PADDING.dp),
                painter = painterResource(id = R.drawable.ic_selected),
                tint = MaterialTheme.colorScheme.onSecondary,
                contentDescription = ""
            )
        }
    }
}

@Composable
@PreviewWidget
private fun PreviewNotSelect() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(White)
        ) {
            LanguageItemWidget(
                flagRes = R.drawable.example_ic_flag_gb,
                langName = "English",
                langNumericCode = 0,
                isSelected = false
            ) {}
        }
    }
}

@Composable
@PreviewWidget
private fun PreviewSelected() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(White)
        ) {
            LanguageItemWidget(
                flagRes = R.drawable.example_ic_flag_gb,
                langName = "English",
                langNumericCode = 0,
                isSelected = true
            ) {}
        }
    }
}