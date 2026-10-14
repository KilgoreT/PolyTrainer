package me.apomazkin.candidates.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.LemmaWordChoice
import me.apomazkin.candidates.PhotoCard
import me.apomazkin.candidates.PhotoDictionaryForm
import me.apomazkin.candidates.PhotoExistingLemma
import me.apomazkin.candidates.existingOf
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.PrimaryFullButtonWidget

/** Сколько значений слова-получателя показать в подписи переноса, когда одноимённых слов несколько (WCL-15.1). */
private const val LABEL_VALUES = 3

/**
 * Выбор у раздела леммы-формы (WCL-15): «переименовать слово в «correr»»
 * и по строке «перенести в «correr» … и удалить «corrían»» на каждое слово
 * словаря с текстом леммы — несколько одноимённых различаются значениями
 * в скобках; выбор один на диалог, маркер ●/○ как в листах. Под строками
 * — «в «correr» уже есть N таких же значений», если совпадения есть
 * (WCL-15.4).
 */
@Composable
private fun LemmaWordChoices(
    card: PhotoCard,
    wordText: String,
    view: CandidatesView,
    form: PhotoDictionaryForm,
    onAction: (CandidateAction) -> Unit,
) {
    val rename = LemmaWordChoice.Rename(card.lemma)
    ChoiceRow(
        text = stringResource(id = R.string.candidates_rename_to, card.lemma),
        selected = view.lemmaWordChoice == rename,
        onClick = { onAction(CandidateAction.ToggleLemmaWord(rename)) },
    )
    val words = form.existingOf(card.lemma)
    words.forEach { word ->
        val transfer = LemmaWordChoice.Transfer(card.lemma, word.wordId)
        ChoiceRow(
            text = transferLabel(word = word, lemma = card.lemma, wordText = wordText, distinguish = words.size > 1),
            selected = view.lemmaWordChoice == transfer,
            onClick = { onAction(CandidateAction.ToggleLemmaWord(transfer)) },
        )
    }
    val same = view.sameValues[card.lemma] ?: 0
    if (same > 0) {
        Text(
            text = pluralStringResource(id = R.plurals.candidates_same_values, count = same, card.lemma, same),
            style = LexemeStyle.BodyS,
            color = formTextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

/** Подпись переноса: одно одноимённое слово — без скобок; несколько — первые значения каждого или «без значений». */
@Composable
private fun transferLabel(
    word: PhotoExistingLemma,
    lemma: String,
    wordText: String,
    distinguish: Boolean,
): String {
    if (!distinguish) return stringResource(id = R.string.candidates_transfer_to, lemma, wordText)
    val values = when {
        word.coreValues.isEmpty() -> stringResource(id = R.string.candidates_no_values)
        word.coreValues.size > LABEL_VALUES -> word.coreValues.take(LABEL_VALUES).joinToString(", ") + "…"
        else -> word.coreValues.joinToString(", ")
    }
    return stringResource(id = R.string.candidates_transfer_to_values, lemma, values, wordText)
}

@Composable
private fun ChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) "●" else "○",
            style = LexemeStyle.BodyM,
            color = if (selected) LexemeColor.primary else formTextSecondary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = LexemeStyle.BodyM,
            color = if (selected) LexemeColor.primary else formTextTertiary,
        )
    }
}

/** Доля экрана под диалог: список прокручивается, кнопка всегда видна (WCL-07.3). */
private const val DIALOG_HEIGHT_FRACTION = 0.92f

/**
 * IS533: диалог «значения из словаря» в карточке слова (WCL-07) — те же
 * виджеты, что в окне снимка, без групп, «куда класть» и блока «не
 * добавить»: заголовок с текстом слова и крестик, строка «Куда пишем»,
 * разделы лемм (у леммы-формы заголовок «форма слова «…»» с пометкой
 * сервера), атрибуция, панель с кнопкой «Добавить N значений». Пока идёт
 * запрос — признак загрузки; причина вместо разделов — текстом (WCL-11).
 * Нажатие мимо и «назад» — как крестик (WCL-12). Цвета явные, парой из
 * окна снимка.
 */
@Composable
fun CandidatesDialog(
    wordText: String,
    isLoading: Boolean,
    @StringRes errorRes: Int?,
    view: CandidatesView,
    onAction: (CandidateAction) -> Unit,
) {
    Dialog(
        onDismissRequest = { onAction(CandidateAction.RequestClose) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
    ) {
        Surface(
            color = formBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .fillMaxHeight(DIALOG_HEIGHT_FRACTION),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DialogTopBar(wordText = wordText, onClose = { onAction(CandidateAction.RequestClose) })
                    when {
                        isLoading -> LoadingBlock()
                        errorRes != null -> ErrorBlock(
                            text = stringResource(id = errorRes, wordText),
                            onClose = { onAction(CandidateAction.RequestClose) },
                        )
                        else -> {
                            DialogList(wordText = wordText, view = view, onAction = onAction, modifier = Modifier.weight(1f))
                            if (view.cards.isNotEmpty()) {
                                PhotoBottomPanel(
                                    state = view,
                                    onAction = onAction,
                                    countText = lexemesCount(view.summary.lexemeCount),
                                )
                            }
                        }
                    }
                }
                if (view.isAdding) {
                    // Нажатия не доходят до диалога, пока летит запись (WCL-10.3).
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    )
                }
            }
        }
        PhotoSheets(state = view, onAction = onAction)
        if (view.isConfirmingClose) {
            CloseConfirmDialog(onAction = onAction)
        }
    }
}

@Composable
private fun DialogTopBar(
    wordText: String,
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
            color = formTextTertiary,
            modifier = Modifier
                .clickable { onClose() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Text(
            text = wordText,
            style = LexemeStyle.BodyXLBold,
            color = formTextTertiary,
        )
    }
    HorizontalDivider(color = cardDivider)
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = LexemeColor.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.photo_words_looking),
                style = LexemeStyle.BodyM,
                color = formTextSecondary,
            )
        }
    }
}

/** Причина вместо разделов (WCL-11): текст и кнопка закрытия. */
@Composable
private fun ErrorBlock(
    text: String,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = text,
            style = LexemeStyle.BodyM,
            color = formTextTertiary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryFullButtonWidget(
            modifier = Modifier.fillMaxWidth(),
            titleRes = R.string.photo_words_close,
            onClick = onClose,
        )
    }
}

/** «Куда пишем», разделы лемм, атрибуция — одним списком. */
@Composable
private fun DialogList(
    wordText: String,
    view: CandidatesView,
    onAction: (CandidateAction) -> Unit,
    modifier: Modifier,
) {
    val form = view.form ?: return
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item(key = "mapping") {
            if (view.isMappingExpanded) {
                MappingExpanded(state = view, form = form, onAction = onAction)
            } else {
                MappingSummary(state = view, form = form, onAction = onAction)
            }
            HorizontalDivider(color = cardDivider)
        }
        if (view.cards.isEmpty()) {
            item(key = "nothing") {
                Text(
                    text = stringResource(id = R.string.photo_add_nothing),
                    style = LexemeStyle.BodyM,
                    color = formTextTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )
                HorizontalDivider(color = cardDivider)
            }
        }
        items(view.cards, key = { it.lemma }) { card ->
            LemmaSection(card = card, wordText = wordText, view = view, form = form, onAction = onAction)
            HorizontalDivider(color = cardDivider)
        }
        item(key = "attribution") {
            Attribution()
        }
    }
}

/**
 * Раздел леммы (WCL-04): у леммы-формы заголовок «форма слова «run»»,
 * пометка сервера серым и выбор «переименовать / перенести» (WCL-15); у
 * прямой леммы заголовка нет — это слово диалога. Строки лексем-кандидатов
 * и «ещё N» — как в окне.
 */
@Composable
private fun LemmaSection(
    card: PhotoCard,
    wordText: String,
    view: CandidatesView,
    form: PhotoDictionaryForm,
    onAction: (CandidateAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val formNote = card.formNote
        if (formNote != null) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Text(
                    text = stringResource(id = R.string.candidates_form_of, card.lemma),
                    style = LexemeStyle.BodyMBold,
                    color = formTextTertiary,
                )
                Text(
                    text = formNote,
                    style = LexemeStyle.BodyS,
                    color = formTextSecondary,
                )
            }
            LemmaWordChoices(card = card, wordText = wordText, view = view, form = form, onAction = onAction)
        }
        card.lexemes.forEach { lexeme ->
            HorizontalDivider(color = cardDivider, modifier = Modifier.padding(start = 16.dp))
            LexemeRow(card = card, lexeme = lexeme, state = view, form = form, onAction = onAction)
        }
        if (card.hiddenCount > 0) {
            Text(
                text = stringResource(id = R.string.photo_add_more, lexemesCount(card.hiddenCount)),
                style = LexemeStyle.BodyS,
                color = formTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
