package me.apomazkin.wordstab.ui.widget.photo

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.deps.PhotoWordStatus
import me.apomazkin.wordstab.logic.PhotoPhase
import me.apomazkin.wordstab.logic.PhotoWordsState

/** Страница лицензии данных источника (WFT-57.2). */
private const val LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/"

/**
 * IS527/IS529: один диалог на весь запуск — выбор источника, индикатор с
 * подписью фазы (WFT-16), список слов из ответа сервера с леммой первого
 * кандидата или пометкой (WFT-56, Д4), подпись источника с лицензией
 * (WFT-57), «слов не найдено» (WFT-60) или ошибка (WFT-61, WFT-63). Со
 * списком ничего нельзя делать, кроме прокрутки и закрытия (WFT-58).
 * Цвета явные, парой из LanguagePickerDialog (IS525): фон formBackground,
 * текст formTextTertiary, вторая строка formTextSecondary, акцент
 * LexemeColor.primary. [isCameraAvailable] false — кнопки «Камера» нет.
 */
@Composable
internal fun PhotoWordsDialog(
    state: PhotoWordsState,
    isCameraAvailable: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasList = state.words.isNotEmpty()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = formBackground,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasList) Modifier.fillMaxHeight(0.8f) else Modifier),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title(state),
                    style = LexemeStyle.BodyM,
                    color = formTextTertiary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    state.isChoosingSource -> SourceChooser(
                        isCameraAvailable = isCameraAvailable,
                        onCamera = onCamera,
                        onGallery = onGallery,
                    )
                    state.isBusy -> Busy()
                    state.errorRes != null -> Message(textRes = state.errorRes)
                    state.isEmptyResult || !hasList -> Message(textRes = R.string.photo_words_empty)
                    else -> {
                        WordList(words = state.words, modifier = Modifier.weight(1f))
                        Attribution()
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                CancelButtonWidget(
                    modifier = Modifier.fillMaxWidth(),
                    titleRes = if (state.isChoosingSource || state.isBusy) {
                        R.string.button_cancel
                    } else {
                        R.string.photo_words_close
                    },
                ) { onDismiss() }
            }
        }
    }
}

/** Подпись фазы (WFT-16.1, 16.2) или число слов (WFT-56.1). */
@Composable
private fun title(state: PhotoWordsState): String = when {
    state.phase == PhotoPhase.RECOGNIZING -> stringResource(id = R.string.photo_words_recognizing)
    state.phase == PhotoPhase.LOOKING -> stringResource(id = R.string.photo_words_looking)
    state.words.isNotEmpty() -> stringResource(id = R.string.photo_words_found, state.words.size)
    else -> stringResource(id = R.string.photo_words_title)
}

@Composable
private fun SourceChooser(
    isCameraAvailable: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    if (isCameraAvailable) {
        PrimaryFullButtonWidget(
            modifier = Modifier.fillMaxWidth(),
            titleRes = R.string.photo_words_camera,
            onClick = onCamera,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    PrimaryFullButtonWidget(
        modifier = Modifier.fillMaxWidth(),
        titleRes = R.string.photo_words_gallery,
        onClick = onGallery,
    )
}

@Composable
private fun Busy() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = LexemeColor.primary)
    }
}

@Composable
private fun Message(@StringRes textRes: Int) {
    Text(
        text = stringResource(id = textRes),
        style = LexemeStyle.BodyM,
        color = formTextTertiary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/** Элемент на слово ответа, порядок ответа; ключ — позиция (слова в ответе без повторов, WFT-04). */
@Composable
private fun WordList(words: List<PhotoWord>, modifier: Modifier) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(words, key = { index, _ -> index }) { _, word ->
            WordItem(word = word)
        }
    }
}

/**
 * Слово первой строкой; второй — «лемма, часть речи» первого кандидата (Д4)
 * или пометка; третьей — первое толкование на языке источника (Д5);
 * четвёртой — первое значение на языке перевода (Д6). Чего у кандидата
 * нет, той строки нет.
 */
@Composable
private fun WordItem(word: PhotoWord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = word.text,
            style = LexemeStyle.BodyM,
            color = formTextTertiary,
        )
        Text(
            text = secondLine(word),
            style = LexemeStyle.BodyS,
            color = formTextSecondary,
        )
        if (word.gloss != null) {
            Text(
                text = word.gloss,
                style = LexemeStyle.BodyS,
                color = formTextSecondary,
            )
        }
        if (word.meaning != null) {
            Text(
                text = word.meaning,
                style = LexemeStyle.BodyS,
                color = formTextTertiary,
            )
        }
    }
}

@Composable
private fun secondLine(word: PhotoWord): String = when (word.status) {
    PhotoWordStatus.FOUND -> listOfNotNull(word.lemma, word.partOfSpeech).joinToString(", ")
    PhotoWordStatus.NOT_FOUND -> stringResource(id = R.string.photo_words_not_found_mark)
    PhotoWordStatus.ERROR -> stringResource(id = R.string.photo_words_error_mark)
}

/** «Данные: Wiktionary, CC BY-SA 4.0», нажатие открывает лицензию (WFT-57). */
@Composable
private fun Attribution() {
    val uriHandler = LocalUriHandler.current
    Text(
        text = stringResource(id = R.string.photo_words_attribution),
        style = LexemeStyle.BodyS,
        color = formTextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(LICENSE_URL) }
            .padding(top = 12.dp, bottom = 4.dp),
    )
}

@PreviewWidget
@Composable
private fun PreviewList() {
    AppTheme {
        PhotoWordsDialog(
            state = PhotoWordsState(
                isOpen = true,
                words = listOf(
                    PhotoWord(text = "corrían", status = PhotoWordStatus.FOUND, lemma = "correr", partOfSpeech = "verb", meaning = "бегать; бежать", gloss = "to run, to jog"),
                    PhotoWord(text = "accorc", status = PhotoWordStatus.NOT_FOUND, lemma = null, partOfSpeech = null, meaning = null, gloss = null),
                    PhotoWord(text = "fuera", status = PhotoWordStatus.ERROR, lemma = null, partOfSpeech = null, meaning = null, gloss = null),
                ),
            ),
            isCameraAvailable = true,
            onCamera = {},
            onGallery = {},
            onDismiss = {},
        )
    }
}
