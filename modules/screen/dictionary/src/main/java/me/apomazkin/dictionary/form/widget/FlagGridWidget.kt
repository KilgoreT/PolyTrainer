package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

private val FLAG_SIZE = 56.dp
private val RING_WIDTH = 2.5.dp
private val BADGE_SIZE = 24.dp
private val BADGE_BORDER = 2.dp
private val BADGE_ICON_SIZE = 11.dp

/**
 * Сетка флагов 4 колонки (Figma 5027:1140-1234). Выбранный флаг: кольцо + бейдж-галочка
 * + акцентная bold-подпись (5027:1142-1154). При активном фильтре без совпадений —
 * empty-state «Ничего не найдено» (производная приходит параметром [isFilterActive],
 * сам виджет о flagFilter не знает).
 */
@Composable
internal fun FlagGridWidget(
    flags: List<CountryFlagItem>,
    selectedFlag: CountryFlagItem?,
    isFilterActive: Boolean,
    onFlagClick: (CountryFlagItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (flags.isEmpty() && isFilterActive) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.dictionary_flags_not_found),
                style = LexemeStyle.BodyM,
                color = formTextSecondary,
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(flags) { flag ->
            val isSelected = selectedFlag?.numericCode == flag.numericCode
            FlagGridItem(
                flag = flag,
                isSelected = isSelected,
                onFlagClick = onFlagClick,
            )
        }
    }
}

@Composable
private fun FlagGridItem(
    flag: CountryFlagItem,
    isSelected: Boolean,
    onFlagClick: (CountryFlagItem) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = formBackground,
                onClick = { onFlagClick(flag) },
                modifier = Modifier
                    .size(FLAG_SIZE)
                    .let {
                        if (isSelected) it.border(
                            width = RING_WIDTH,
                            color = LexemeColor.primary,
                            shape = CircleShape,
                        ) else it
                    },
            ) {
                Box(
                    modifier = Modifier.padding(if (isSelected) RING_WIDTH + 2.dp else 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageFlagWidget(
                        flagRes = flag.flagRes,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(BADGE_SIZE)
                        .border(width = BADGE_BORDER, color = formBackground, shape = CircleShape)
                        .padding(BADGE_BORDER)
                        .background(color = LexemeColor.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_confirm),
                        contentDescription = null,
                        tint = whiteColor,
                        modifier = Modifier.size(BADGE_ICON_SIZE),
                    )
                }
            }
        }
        Text(
            text = flag.localizedName,
            style = if (isSelected) LexemeStyle.BodySBold else LexemeStyle.BodyS,
            color = if (isSelected) LexemeColor.primary else formTextTertiary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        FlagGridWidget(
            flags = emptyList(),
            selectedFlag = null,
            isFilterActive = false,
            onFlagClick = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewEmptySearch() {
    AppTheme {
        FlagGridWidget(
            flags = emptyList(),
            selectedFlag = null,
            isFilterActive = true,
            onFlagClick = {},
        )
    }
}
