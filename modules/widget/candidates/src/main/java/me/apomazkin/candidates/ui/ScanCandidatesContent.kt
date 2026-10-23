package me.apomazkin.candidates.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.NotAddedWordUiItem
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary

/**
 * Содержимое окна лексем-кандидатов (AWP-03): верхняя строка с «×», шапка с
 * общей группой, карточки, разделы «Замечания» и «Сопоставление», блок «не
 * добавить», атрибуция, внизу панель с кнопкой. Листы и вопросы — здесь же.
 * `Dialog` нет: окно рисует экран снимка, системная «назад» — его
 * (AWP-19). Пока идёт запись, окно не принимает нажатий (AWP-13.4). Отступы
 * системных панелей — внутри. Цвета явные: фон formBackground, текст
 * formTextTertiary, второй план formTextSecondary, ошибка destructiveRed.
 */
@Composable
fun ScanCandidatesContent(
    view: CandidatesView,
    onAction: (CandidateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = formBackground,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                TopBar(onClose = { onAction(CandidateAction.RequestClose) })
                CandidatesList(
                    view = view,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                )
                if (view.cards.isNotEmpty()) {
                    CandidatesBottomPanel(
                        view = view,
                        onAction = onAction,
                        countText = countInWords(view.summary.lexemeCount, view.summary.wordCount),
                    )
                }
            }
            if (view.isAdding) {
                // Нажатия не доходят до окна, пока летит запись (AWP-13.4).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {},
                )
            }
        }
    }
    CandidatesSheets(view = view, onAction = onAction)
    if (view.isConfirmingAdd) {
        AddConfirmDialog(view = view, onAction = onAction)
    }
    if (view.isConfirmingClose) {
        CloseConfirmDialog(onAction = onAction)
    }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "×",
            style = LexemeStyle.H5,
            color = formTextTertiary,
            modifier = Modifier
                .clickable { onClose() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Text(
            text = stringResource(id = R.string.photo_words_title),
            style = LexemeStyle.BodyXLBold,
            color = formTextTertiary,
        )
    }
    HorizontalDivider(color = cardDivider)
}

/**
 * Шапка, карточки, «Замечания», «Сопоставление», блок «не добавить»,
 * атрибуция — одним списком (AWP-04.6); карточек нет — «добавить нечего».
 */
@Composable
private fun CandidatesList(
    view: CandidatesView,
    onAction: (CandidateAction) -> Unit,
    modifier: Modifier,
) {
    val form = view.form ?: return
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        if (view.cards.isNotEmpty()) {
            item(key = "header") {
                DividedItem { CommonGroupHeader(view = view, form = form, onAction = onAction) }
            }
        } else {
            item(key = "nothing") {
                DividedItem {
                    Text(
                        text = stringResource(id = R.string.photo_add_nothing),
                        style = LexemeStyle.BodyM,
                        color = formTextTertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }
        }
        items(view.cards, key = { it.lemma }) { card ->
            LemmaDividedItem { LemmaCardWidget(card = card, form = form, onAction = onAction) }
        }
        if (view.remarks.isNotEmpty()) {
            item(key = "remarks") {
                DividedItem { RemarksSection(remarks = view.remarks, form = form) }
            }
        }
        if (view.componentMatchLines.isNotEmpty()) {
            item(key = "component-matches") {
                DividedItem {
                    ComponentMatchesSection(
                        lines = view.componentMatchLines,
                        form = form,
                        isExpanded = view.isComponentMatchesExpanded,
                        onAction = onAction,
                    )
                }
            }
        }
        if (view.notAdded.isNotEmpty()) {
            item(key = "not-added") {
                DividedItem {
                    NotAddedBlock(
                        items = view.notAdded,
                        isExpanded = view.isNotAddedExpanded,
                        onAction = onAction,
                    )
                }
            }
        }
        item(key = "attribution") {
            Attribution()
        }
    }
}

/** Слова без добавления (AWP-12): блок в конце списка, раскрыт ли он, решает флаг стейта. */
@Composable
private fun NotAddedBlock(
    items: List<NotAddedWordUiItem>,
    isExpanded: Boolean,
    onAction: (CandidateAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAction(CandidateAction.ToggleNotAdded) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.photo_add_not_added, items.size),
                style = LexemeStyle.BodyM,
                color = formTextTertiary,
            )
            Text(
                text = if (isExpanded) "▴" else "▾",
                style = LexemeStyle.BodyM,
                color = formTextSecondary,
            )
        }
        if (isExpanded) {
            items.forEach { item ->
                Text(
                    text = "${item.text} — ${notAddedReasonLabel(item.reason)}",
                    style = LexemeStyle.BodyS,
                    color = formTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
