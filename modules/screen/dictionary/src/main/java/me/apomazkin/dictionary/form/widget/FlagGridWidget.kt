package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun FlagGridWidget(
    flags: List<CountryFlagItem>,
    selectedFlag: CountryFlagItem?,
    onFlagClick: (CountryFlagItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(flags) { flag ->
            val isSelected = selectedFlag?.numericCode == flag.numericCode
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    border = if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    onClick = { onFlagClick(flag) },
                    modifier = Modifier.padding(4.dp),
                ) {
                    ImageFlagWidget(
                        flagRes = flag.flagRes,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Text(
                    text = flag.localizedName,
                    style = LexemeStyle.BodyS,
                    color = grayTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(56.dp),
                )
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        FlagGridWidget(
            flags = emptyList(),
            selectedFlag = null,
            onFlagClick = {},
        )
    }
}
