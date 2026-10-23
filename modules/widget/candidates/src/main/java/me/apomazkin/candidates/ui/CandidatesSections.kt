package me.apomazkin.candidates.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary

/**
 * Раздел «Замечания» под карточками (AWP-30, WCL-16): строка на замечание,
 * текст — по виду строки из ресурсов. Кнопок нет: компонент пользователь
 * заводит сам в конструкторе. Раздел рисуют, когда [remarks] не пуст.
 */
@Composable
internal fun RemarksSection(
    remarks: List<RemarkUiItem>,
    form: DictionaryFormUi,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.candidates_remarks_title),
            style = LexemeStyle.BodyM,
            color = formTextTertiary,
        )
        remarks.forEach { remark ->
            val text = remarkText(remark, form)
            if (text != null) {
                Text(
                    text = text,
                    style = LexemeStyle.BodyS,
                    color = formTextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Раздел «Сопоставление» (AWP-31, WCL-17): один на окно, свёрнут по
 * умолчанию; заголовок шлёт [CandidateAction.ToggleComponentMatches],
 * раскрытие — по [isExpanded]. Раздел рисуют, когда [lines] не пуст.
 */
@Composable
internal fun ComponentMatchesSection(
    lines: List<ComponentMatchLineUiItem>,
    form: DictionaryFormUi,
    isExpanded: Boolean,
    onAction: (CandidateAction) -> Unit,
) {
    val toggleRes = if (isExpanded) R.string.candidates_collapse else R.string.candidates_expand
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAction(CandidateAction.ToggleComponentMatches) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.candidates_component_matches_title),
                style = LexemeStyle.BodyM,
                color = formTextTertiary,
            )
            Text(
                text = stringResource(id = toggleRes),
                style = LexemeStyle.BodyS,
                color = LexemeColor.primary,
            )
        }
        if (isExpanded) {
            lines.forEach { line ->
                Text(
                    text = matchLineText(line, form),
                    style = LexemeStyle.BodyS,
                    color = formTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
