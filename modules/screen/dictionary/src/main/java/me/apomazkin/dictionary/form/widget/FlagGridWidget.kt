package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.util.Locale

@Composable
internal fun FlagGridWidget(
    flags: List<CountryFlagItem>,
    selectedFlag: CountryFlagItem?,
    onFlagClick: (CountryFlagItem) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(flags) { flag ->
            val isSelected = selectedFlag?.numericCode == flag.numericCode
            val localizedName = getLocalizedCountryName(flag.countryName)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .padding(vertical = 4.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    border = if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    onClick = { onFlagClick(flag) },
                ) {
                    ImageFlagWidget(
                        flagRes = flag.flagRes,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Text(
                    text = localizedName,
                    style = LexemeStyle.BodyS,
                    color = grayTextColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun getLocalizedCountryName(englishName: String): String {
    val currentLocale = Locale.getDefault()
    return Locale.getISOCountries()
        .map { Locale("", it) }
        .firstOrNull { it.getDisplayCountry(Locale.ENGLISH).equals(englishName, ignoreCase = true) }
        ?.getDisplayCountry(currentLocale)
        ?: englishName
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
