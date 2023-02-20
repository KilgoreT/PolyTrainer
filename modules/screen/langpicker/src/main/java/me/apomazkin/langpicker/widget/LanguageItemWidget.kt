package me.apomazkin.langpicker.widget

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.apomazkin.langpicker.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun LanguageItemWidget(
    @DrawableRes flagRes: Int,
    value: String,
    isSelected: Boolean,
    onClick: (value: String) -> Unit,
) {
    val color = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .background(Color.Transparent)
            .clickable { onClick.invoke(value) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageFlagWidget(
            flagRes = flagRes,
            modifier = Modifier
                .padding(start = 16.dp),
            contentDescription = value,
        )
        Text(
            modifier = Modifier
                .padding(end = 16.dp),
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewNotSelect() {
    AppTheme {
        LanguageItemWidget(
            flagRes = R.drawable.ic_more_on_primary,
            value = "English",
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
            value = "English",
            isSelected = true
        ) {}
    }
}