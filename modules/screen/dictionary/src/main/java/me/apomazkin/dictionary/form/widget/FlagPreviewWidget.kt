package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.ui.FlagPlaceholderWidget
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

private val RING_SIZE = 58.dp
private val RING_WIDTH = 2.dp
private val GAP_WIDTH = 2.dp
private val CONTENT_SIZE = 50.dp

/**
 * Превью выбранного флага с двойным кольцом (кольцо 2dp + зазор 2dp, Figma 5027:1120).
 * Без выбранного флага — плейсхолдер с первой буквой имени (live), пустое имя — пустой круг.
 */
@Composable
internal fun FlagPreviewWidget(
    selectedFlag: CountryFlagItem?,
    name: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(RING_SIZE)
            .border(width = RING_WIDTH, color = LexemeColor.primary, shape = CircleShape)
            .padding(RING_WIDTH + GAP_WIDTH),
        contentAlignment = Alignment.Center,
    ) {
        if (selectedFlag != null) {
            ImageFlagWidget(
                flagRes = selectedFlag.flagRes,
                modifier = Modifier.size(CONTENT_SIZE),
            )
        } else {
            FlagPlaceholderWidget(
                letter = name.firstOrNull()?.toString() ?: "",
                modifier = Modifier.size(CONTENT_SIZE),
            )
        }
    }
}

@Composable
@PreviewWidget
private fun PreviewLetter() {
    AppTheme {
        FlagPreviewWidget(
            selectedFlag = null,
            name = "Английский",
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewEmpty() {
    AppTheme {
        FlagPreviewWidget(
            selectedFlag = null,
            name = "",
        )
    }
}
