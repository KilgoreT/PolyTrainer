package me.apomazkin.candidates.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.destructiveRed
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.AlarmButtonWidget
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.dialog.base.LexemeDialog

/** Страница лицензии данных источника (LFL-57.2). */
private const val LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/"

/**
 * Панель внизу окна и диалога (AWP-13): кнопка со счётом [countText];
 * причина неактивной кнопки и «N уже есть» над ней; «не добавилось» с
 * «повторить» (AWP-21, AWP-26). Отступ под системной панелью — внутри.
 */
@Composable
internal fun CandidatesBottomPanel(
    view: CandidatesView,
    onAction: (CandidateAction) -> Unit,
    countText: String,
) {
    val summary = view.summary
    val failure = view.addFailure
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(formBackground)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        HorizontalDivider(color = cardDivider)
        Spacer(modifier = Modifier.height(8.dp))
        if (summary.existingCount > 0) {
            PanelNote(
                text = pluralStringResource(
                    id = R.plurals.photo_add_existing_skipped,
                    count = summary.existingCount,
                    summary.existingCount,
                ),
            )
        }
        when {
            failure != null -> PanelNote(text = failureLabel(failure), isError = true)
            view.isAdding -> Unit
            summary.lacksCoreCount > 0 -> PanelNote(
                text = pluralStringResource(
                    id = R.plurals.photo_add_no_core_reason,
                    count = summary.lacksCoreCount,
                    summary.lacksCoreCount,
                ),
                isError = true,
            )
            summary.lexemeCount == 0 -> {
                PanelNote(text = stringResource(id = R.string.photo_add_nothing_to_add))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (failure != null) {
            PrimaryFullButtonWidget(
                modifier = Modifier.fillMaxWidth(),
                titleRes = R.string.photo_add_retry,
                enabled = summary.isEnabled && !view.isAdding,
                onClick = { onAction(CandidateAction.RetryAdd) },
            )
        } else {
            AddButton(view = view, onAction = onAction, countText = countText)
        }
    }
}

@Composable
private fun PanelNote(
    text: String,
    isError: Boolean = false,
) {
    Text(
        text = text,
        style = LexemeStyle.BodyS,
        color = if (isError) destructiveRed else formTextSecondary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/** Ресурс кнопки не умеет счёт, поэтому подпись подменяется через `titleOverride`. */
@Composable
private fun AddButton(
    view: CandidatesView,
    onAction: (CandidateAction) -> Unit,
    countText: String,
) {
    val summary = view.summary
    val title = when {
        view.isAdding -> stringResource(id = R.string.photo_add_adding)
        summary.isEnabled -> stringResource(id = R.string.photo_add_button, countText)
        else -> stringResource(id = R.string.photo_add_button_plain)
    }
    AlarmButtonWidget(
        modifier = Modifier.fillMaxWidth(),
        titleRes = R.string.photo_add_button_plain,
        titleOverride = title,
        enabled = summary.isEnabled && !view.isAdding,
        containerColor = LexemeColor.primary,
        contentColor = LexemeColor.onPrimary,
    ) { onAction(CandidateAction.RequestAdd) }
}

/** «Добавить N значений в M слов?» — всегда перед записью в окне (AWP-13, Д23). */
@Composable
internal fun AddConfirmDialog(
    view: CandidatesView,
    onAction: (CandidateAction) -> Unit,
) {
    val countText = countInWords(view.summary.lexemeCount, view.summary.wordCount)
    LexemeDialog(onDismissRequest = { onAction(CandidateAction.CancelAdd) }) {
        Text(
            text = stringResource(id = R.string.photo_add_confirm, countText),
            style = LexemeStyle.H6,
            color = formTextTertiary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CancelButtonWidget(
                modifier = Modifier.weight(1f),
            ) { onAction(CandidateAction.CancelAdd) }
            PrimaryFullButtonWidget(
                modifier = Modifier.weight(1f),
                titleRes = R.string.photo_add_yes,
                onClick = { onAction(CandidateAction.ConfirmAdd) },
            )
        }
    }
}

/** «Закрыть без добавления?» после удалений, правок и смены опций (AWP-25). */
@Composable
internal fun CloseConfirmDialog(onAction: (CandidateAction) -> Unit) {
    LexemeDialog(onDismissRequest = { onAction(CandidateAction.Stay) }) {
        Text(
            text = stringResource(id = R.string.photo_add_close_question),
            style = LexemeStyle.H6,
            color = formTextTertiary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CancelButtonWidget(
                modifier = Modifier.weight(1f),
                titleRes = R.string.photo_add_stay,
            ) { onAction(CandidateAction.Stay) }
            AlarmButtonWidget(
                modifier = Modifier.weight(1f),
                titleRes = R.string.photo_add_close,
            ) { onAction(CandidateAction.ConfirmClose) }
        }
    }
}

/** «Данные: Wiktionary, CC BY-SA 4.0», нажатие открывает лицензию (LFL-54, AWP-45). */
@Composable
fun Attribution() {
    val uriHandler = LocalUriHandler.current
    Text(
        text = stringResource(id = R.string.photo_words_attribution),
        style = LexemeStyle.BodyS,
        color = formTextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(LICENSE_URL) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
