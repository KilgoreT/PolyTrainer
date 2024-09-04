@file:OptIn(ExperimentalFoundationApi::class)

package me.apomazkin.createdictionary.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.createdictionary.LanguageData
import me.apomazkin.createdictionary.R
import me.apomazkin.createdictionary.entity.PresetLangUi
import me.apomazkin.createdictionary.logic.Msg
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.White
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

private const val HEADER_HORIZONTAL_PADDING = 16
private const val HEADER_TOP_PADDING = 16
private const val LIST_HORIZONTAL_PADDING = 12
private const val LIST_VERTICAL_PADDING = 12
private const val LIST_VERTICAL_GAP = 4

@Composable
fun ColumnScope.LangListWidget(
    langList: List<PresetLangUi>,
    selectedNumericCode: Int?,
    sendMsg: (Msg) -> Unit,
) {
    ListHeaderWidget(
        titleRes = R.string.lang_selection_title,
        subTitleRes = R.string.lang_selection_subtitle,
        modifier = Modifier
            .padding(horizontal = HEADER_HORIZONTAL_PADDING.dp)
            .padding(top = HEADER_TOP_PADDING.dp)
    )

    LazyColumn(
        contentPadding = PaddingValues(
            vertical = LIST_VERTICAL_PADDING.dp,
            horizontal = LIST_HORIZONTAL_PADDING.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(LIST_VERTICAL_GAP.dp)
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

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
        ) {
            LangListWidget(
                langList = LanguageData.langPreviewList,
                selectedNumericCode = null,
            ) {}
        }
    }
}