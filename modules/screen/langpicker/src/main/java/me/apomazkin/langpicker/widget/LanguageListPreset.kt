package me.apomazkin.langpicker.widget

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.langpicker.Country
import me.apomazkin.langpicker.R
import me.apomazkin.langpicker.entity.LangPresetUi
import me.apomazkin.langpicker.entity.LangUpdateUi
import me.apomazkin.langpicker.toStringName
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

fun LazyListScope.languagesPreset(
    list: List<LangPresetUi>,
    selected: String?,
    onSelect: (lang: LangUpdateUi?) -> Unit,
) {
    items(list) { item: LangPresetUi ->
        LanguageItemWidget(
            flagRes = item.flagRes,
            value = stringResource(id = item.langNameRes),
            isSelected = selected == item.countryNumericCode.toStringName()
        ) {
            if (selected == it) {
                onSelect.invoke(null)
            } else {
                onSelect.invoke(
                    LangUpdateUi(
                        countryNumericCode = item.countryNumericCode,
                        langName = it
                    )
                )
            }
        }
    }
}

@Composable
@PreviewWidgetRu
@PreviewWidgetEn
private fun Preview() {
    AppTheme {
        LazyColumn {
            languagesPreset(
                list = listOf(
                    LangPresetUi(
                        flagRes = R.drawable.ic_more_on_primary,
                        countryNumericCode = Country.GERMAN.numericCode,
                        langNameRes = R.string.lang_english,
                    ),
                    LangPresetUi(
                        flagRes = R.drawable.ic_more_on_primary,
                        countryNumericCode = Country.ITALIAN.numericCode,
                        langNameRes = R.string.lang_italian,
                    ),
                ),
                selected = "Итальянский",
            ) {}
        }
    }
}