package me.apomazkin.langpicker.widget

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.apomazkin.langpicker.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.SecondarySelected
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun LanguageItemWidget(
    @DrawableRes flagRes: Int,
    langName: String,
    langNumericCode: Int,
    isSelected: Boolean,
    onClick: (numericCode: Int) -> Unit,
) {
    val color = if (isSelected) {
        SecondarySelected
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .clickable { onClick.invoke(langNumericCode) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageFlagWidget(
            flagRes = flagRes,
            modifier = Modifier
                .padding(start = 12.dp),
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
                    .padding(end = 12.dp),
                painter = painterResource(id = R.drawable.ic_selected),
                tint = MaterialTheme.colorScheme.tertiary,
                contentDescription = ""
            )
        }
    }
}

@Composable
@PreviewWidget
private fun PreviewNotSelect() {
    AppTheme {
        LanguageItemWidget(
            flagRes = R.drawable.ic_more_on_primary,
            langName = "English",
            langNumericCode = 0,
            isSelected = false
        ) {}
    }
}

@Composable
@PreviewWidget
private fun PreviewSelected() {
    AppTheme {
        LanguageItemWidget(
            flagRes = R.drawable.ic_more_on_primary,
            langName = "English",
            langNumericCode = 0,
            isSelected = true
        ) {}
    }
}