package me.apomazkin.scancandidates.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.ui.Attribution
import me.apomazkin.candidates.ui.CandidatesSheets
import me.apomazkin.candidates.ui.CommonGroupHeader
import me.apomazkin.candidates.ui.ComponentMatchesSection
import me.apomazkin.candidates.ui.DividedItem
import me.apomazkin.candidates.ui.LemmaCardWidget
import me.apomazkin.candidates.ui.RemarksSection
import me.apomazkin.candidates.ui.SavedLexemeRows
import me.apomazkin.candidates.ui.failureLabel
import me.apomazkin.core_resources.R
import me.apomazkin.scancandidates.mate.CardPhase
import me.apomazkin.scancandidates.mate.ScanCandidatesState
import me.apomazkin.scancandidates.mate.ScanWordUiItem
import me.apomazkin.scancandidates.mate.WordCardUi
import me.apomazkin.scancandidates.mate.toView
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

/**
 * Окно снимка со списками (AWP-40): верхняя строка с «×» и заголовком, общая
 * группа (AWP-08), блок перевода свёрнутым — тап раскрывает (AWP-41), четыре
 * секции слов по порядку — смысловые, имена собственные, служебные, «не
 * разобрано»; пустая секция не рисуется (AWP-40.7), список прокручивается
 * целиком (AWP-40.8); у слова словаря пометка «в словаре» (AWP-39). Строка
 * слова кликабельна, под раскрытой строкой — блок карточки по фазе (AWP-42).
 * Крестик неактивен, пока идёт запись (AWP-51.12); вопрос при закрытии — по
 * флагу стейта (AWP-51). Листы правки, опций, «куда класть» и групп — общие
 * виджеты по карточкам всех слов. Окно показывает то, что в стейте, и ничего
 * не толкует. Цвета явные: фон formBackground, текст formTextTertiary, второй
 * план formTextSecondary, пометка и акцент — LexemeColor.primary, сбой —
 * destructiveRed.
 */
@Composable
internal fun ScanWordListsContent(
    state: ScanCandidatesState,
    onAction: (CandidateAction) -> Unit,
    onWordAction: (WordAction) -> Unit,
    onToggleTranslation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = state.toView()
    Surface(
        color = formBackground,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            TopBar(
                enabled = !state.isAdding,
                onClose = { onAction(CandidateAction.RequestClose) },
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val form = view.form
                if (form != null) {
                    item(key = "header") {
                        DividedItem { CommonGroupHeader(view = view, form = form, onAction = onAction) }
                    }
                }
                item(key = "translation") {
                    DividedItem {
                        TranslationBlock(
                            translation = state.translation,
                            isExpanded = state.isTranslationExpanded,
                            onToggle = onToggleTranslation,
                        )
                    }
                }
                wordListSection("lexical", R.string.scan_list_lexical, state.lists.lexical, form, onAction, onWordAction)
                wordListSection("proper", R.string.scan_list_proper, state.lists.proper, form, onAction, onWordAction)
                wordListSection("function", R.string.scan_list_function, state.lists.function, form, onAction, onWordAction)
                wordListSection("unparsed", R.string.scan_list_unparsed, state.lists.unparsed, form, onAction, onWordAction)
            }
        }
    }
    if (state.isConfirmingClose) {
        CloseQuestion(
            pendingWords = state.pendingWords,
            onStay = { onAction(CandidateAction.Stay) },
            onClose = { onAction(CandidateAction.ConfirmClose) },
        )
    }
    CandidatesSheets(view = view, onAction = onAction)
}

/** «×» и заголовок; крестик неактивен по [enabled] — пока идёт запись (AWP-51.12). */
@Composable
private fun TopBar(
    enabled: Boolean,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "×",
            style = LexemeStyle.H5,
            color = if (enabled) formTextTertiary else formTextSecondary,
            modifier = Modifier
                .clickable(enabled = enabled) { onClose() }
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

/** Блок перевода (AWP-41): строка «Перевод» с «▾»/«▴», по тапу — текст перевода. */
@Composable
private fun TranslationBlock(
    translation: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.scan_translation),
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
            Text(
                text = translation,
                style = LexemeStyle.BodyM,
                color = formTextSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
    }
}

/**
 * Секция списка: заголовок и слова столбиком, под раскрытой строкой — блок
 * карточки (AWP-42); пустая секция не рисуется (AWP-40.7).
 */
private fun LazyListScope.wordListSection(
    key: String,
    @StringRes titleRes: Int,
    items: List<ScanWordUiItem>,
    form: DictionaryFormUi?,
    onAction: (CandidateAction) -> Unit,
    onWordAction: (WordAction) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "$key-title") {
        Text(
            text = stringResource(id = titleRes),
            style = LexemeStyle.BodyMBold,
            color = formTextTertiary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
    itemsIndexed(items, key = { index, _ -> "$key-$index" }) { _, item ->
        Column(modifier = Modifier.fillMaxWidth()) {
            WordRow(item = item, onTap = { onWordAction(WordAction.Tap(item)) })
            val card = item.card
            if (item.isExpanded && card != null && form != null) {
                WordCardBlock(
                    text = item.text,
                    card = card,
                    form = form,
                    onAction = onAction,
                    onWordAction = onWordAction,
                )
            }
        }
    }
    item(key = "$key-divider") {
        HorizontalDivider(color = cardDivider)
    }
}

/** Слово списка: текст и, у слова словаря, пометка «в словаре» (AWP-39); тап — по состоянию строки (AWP-42). */
@Composable
private fun WordRow(
    item: ScanWordUiItem,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.text,
            style = LexemeStyle.BodyM,
            color = formTextTertiary,
        )
        if (item.isInDictionary) {
            Text(
                text = stringResource(id = R.string.scan_in_dictionary),
                style = LexemeStyle.BodyS,
                color = LexemeColor.primary,
            )
        }
    }
}

/**
 * Блок карточки под словом по фазе. У слова «в словаре» — лемма с «куда
 * класть» и своей группой, сохранённые лексемы выбранной леммы,
 * «Проверить по тексту» и «Удалить» (AWP-46, AWP-18, AWP-49); признак
 * загрузки (AWP-57); лексемы-кандидаты с разделами, «Добавить» и «Удалить»
 * или «не добавилось» с «Повторить» (AWP-43, AWP-47, AWP-21); «Не найдено»
 * или «Значений нет» с «Удалить» (AWP-55); причина с «Удалить» и, по флагу,
 * «Повторить» (AWP-61). Сохранённые лексемы, когда они есть, видны в любой
 * фазе.
 */
@Composable
private fun WordCardBlock(
    text: String,
    card: WordCardUi,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
    onWordAction: (WordAction) -> Unit,
) {
    val onDelete = { onWordAction(WordAction.Delete(text)) }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = cardDivider, modifier = Modifier.padding(start = 16.dp))
        when (card.phase) {
            CardPhase.SAVED -> {
                LemmaCardWidget(
                    card = card.lemma,
                    form = form,
                    onAction = onAction,
                    onDeleteCard = null,
                    existsMarkRes = R.string.scan_exists_mark,
                )
                SavedRows(card = card, form = form)
                CardButtons(
                    primaryRes = R.string.scan_check_by_text,
                    onPrimary = { onWordAction(WordAction.CheckByText(text)) },
                    onDelete = onDelete,
                )
            }
            CardPhase.LOADING -> {
                SavedRows(card = card, form = form)
                CardProgress()
            }
            CardPhase.CANDIDATES -> CandidatesBlock(
                text = text,
                card = card,
                form = form,
                onAction = onAction,
                onWordAction = onWordAction,
                onDelete = onDelete,
            )
            CardPhase.NOT_FOUND -> {
                SavedRows(card = card, form = form)
                card.errorRes?.let { CardNote(text = stringResource(id = it)) }
                CardButtons(primaryRes = null, onPrimary = {}, onDelete = onDelete)
            }
            CardPhase.ERROR -> {
                SavedRows(card = card, form = form)
                card.errorRes?.let { CardNote(text = stringResource(id = it), isError = true) }
                CardButtons(
                    primaryRes = R.string.photo_add_retry.takeIf { card.hasRetryButton },
                    onPrimary = { onWordAction(WordAction.RetryLookup(text)) },
                    onDelete = onDelete,
                )
            }
        }
    }
}

/** Сохранённые лексемы выбранной леммы словаря под заголовком «В словаре», когда они есть (AWP-46). */
@Composable
private fun SavedRows(
    card: WordCardUi,
    form: DictionaryFormUi,
) {
    if (card.saved.isNotEmpty()) {
        SavedLexemeRows(saved = card.saved, form = form)
    }
}

/**
 * Лексемы-кандидаты: карточка леммы без своего крестика, разделы, источник
 * данных, кнопки. Пока идёт запись слова, правки, «куда класть» и своя группа
 * его карточки не открываются, как и кнопки (AWP-47.8).
 */
@Composable
private fun CandidatesBlock(
    text: String,
    card: WordCardUi,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
    onWordAction: (WordAction) -> Unit,
    onDelete: () -> Unit,
) {
    val cardAction: (CandidateAction) -> Unit = if (card.isAdding) { _ -> } else onAction
    LemmaCardWidget(
        card = card.lemma,
        form = form,
        onAction = cardAction,
        onDeleteCard = null,
        existsMarkRes = R.string.scan_exists_mark,
    )
    SavedRows(card = card, form = form)
    if (card.remarks.isNotEmpty()) {
        RemarksSection(remarks = card.remarks, form = form)
    }
    if (card.componentMatchLines.isNotEmpty()) {
        ComponentMatchesSection(
            lines = card.componentMatchLines,
            form = form,
            isExpanded = card.isComponentMatchesExpanded,
            onToggle = { onWordAction(WordAction.ToggleMatches(text)) },
        )
    }
    Attribution()
    val failure = card.addFailure
    if (failure != null) {
        CardNote(text = failureLabel(failure), isError = true)
        CardButtons(
            primaryRes = R.string.photo_add_retry,
            primaryEnabled = !card.isAdding,
            onPrimary = { onWordAction(WordAction.RetryAdd(text)) },
            deleteEnabled = !card.isAdding,
            onDelete = onDelete,
        )
    } else {
        CardButtons(
            primaryRes = if (card.isAdding) R.string.photo_add_adding else R.string.scan_add,
            primaryEnabled = card.canAdd && !card.isAdding,
            onPrimary = { onWordAction(WordAction.Add(text)) },
            deleteEnabled = !card.isAdding,
            onDelete = onDelete,
        )
    }
}

/** «Удалить» и, если есть [primaryRes], главная кнопка справа. */
@Composable
private fun CardButtons(
    @StringRes primaryRes: Int?,
    onPrimary: () -> Unit,
    onDelete: () -> Unit,
    primaryEnabled: Boolean = true,
    deleteEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CancelButtonWidget(
            modifier = Modifier.weight(1f),
            titleRes = R.string.scan_delete_word,
            enabled = deleteEnabled,
            onClick = onDelete,
        )
        if (primaryRes != null) {
            PrimaryFullButtonWidget(
                modifier = Modifier.weight(1f),
                titleRes = primaryRes,
                enabled = primaryEnabled,
                onClick = onPrimary,
            )
        }
    }
}

@Composable
private fun CardNote(
    text: String,
    isError: Boolean = false,
) {
    Text(
        text = text,
        style = LexemeStyle.BodyS,
        color = if (isError) destructiveRed else formTextSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
    )
}

/** Признак загрузки под словом (AWP-57). */
@Composable
private fun CardProgress() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            color = LexemeColor.primary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** «Закрыть без добавления?» с числом недобавленных слов, «Остаться» и «Закрыть» (AWP-51). */
@Composable
internal fun CloseQuestion(
    pendingWords: Int,
    onStay: () -> Unit,
    onClose: () -> Unit,
) {
    LexemeDialog(onDismissRequest = onStay) {
        Text(
            text = stringResource(id = R.string.photo_add_close_question),
            style = LexemeStyle.H6,
            color = formTextTertiary,
        )
        Text(
            text = stringResource(id = R.string.scan_close_pending, pendingWords),
            style = LexemeStyle.BodyM,
            color = formTextSecondary,
            modifier = Modifier.padding(top = 8.dp),
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
            ) { onStay() }
            AlarmButtonWidget(
                modifier = Modifier.weight(1f),
                titleRes = R.string.photo_add_close,
            ) { onClose() }
        }
    }
}
