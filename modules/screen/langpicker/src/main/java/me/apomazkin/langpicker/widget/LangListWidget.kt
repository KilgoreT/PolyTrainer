@file:OptIn(ExperimentalFoundationApi::class)

package me.apomazkin.langpicker.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.langpicker.LanguageData
import me.apomazkin.langpicker.R
import me.apomazkin.langpicker.entity.PresetLangUi
import me.apomazkin.langpicker.logic.Msg
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun ColumnScope.LangListWidget(
    langList: List<PresetLangUi>,
    selectedNumericCode: Int?,
    sendMsg: (Msg) -> Unit,
) {
    Spacer(
        modifier = Modifier
            .weight(0.5F)
    )
    Surface(
        modifier = Modifier
            .weight(1F, false)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
    ) {
        Column {
            ListHeaderWidget(
                titleRes = R.string.lang_selection_title,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(
                    vertical = 16.dp,
                    horizontal = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(langList) { item ->
                    LanguageItemWidget(
                        flagRes = item.flagRes,
                        langName = stringResource(id = item.langNameRes),
                        langNumericCode = item.countryNumericCode,
                        isSelected = selectedNumericCode == item.countryNumericCode
                    ) { sendMsg(Msg.SelectLang(it)) }
                }
            }
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        Column {
            LangListWidget(
                langList = LanguageData.langPreviewList,
                selectedNumericCode = null,
            ) {}
        }
    }
}