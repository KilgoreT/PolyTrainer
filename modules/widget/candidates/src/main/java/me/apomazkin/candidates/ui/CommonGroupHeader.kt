package me.apomazkin.candidates.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextTertiary

/**
 * Шапка окна: общая группа на все леммы (AWP-08) — «Все» или группа словаря;
 * нажатие открывает лист выбора. Соответствий в шапке нет.
 */
@Composable
internal fun CommonGroupHeader(
    view: CandidatesView,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    val groupName = form.groups.firstOrNull { it.id == view.commonGroupId }?.name
        ?: stringResource(id = R.string.photo_add_group_all)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(CandidateAction.OpenSheet(CandidatesSheetUi.CommonGroup)) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.photo_add_common_group),
            style = LexemeStyle.BodyM,
            color = formTextTertiary,
        )
        Text(
            text = "$groupName ▾",
            style = LexemeStyle.BodyM,
            color = LexemeColor.primary,
        )
    }
}
