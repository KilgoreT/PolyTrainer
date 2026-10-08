package me.apomazkin.wordstab.ui.widget.photo

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.logic.PhotoWordsState

/**
 * IS527: один диалог на три шага — выбор источника, распознавание, список
 * (или ошибка). Цвета явные, парой из LanguagePickerDialog (IS525):
 * фон formBackground, текст formTextTertiary, акцент LexemeColor.primary.
 * [isCameraAvailable] false — кнопки «Камера» нет, остаётся галерея.
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
                    state.isRecognizing -> Recognizing()
                    state.errorRes != null -> Message(textRes = state.errorRes)
                    !hasList -> Message(textRes = R.string.photo_words_empty)
                    else -> WordList(words = state.words, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                CancelButtonWidget(
                    modifier = Modifier.fillMaxWidth(),
                    titleRes = if (state.isChoosingSource || state.isRecognizing) {
                        R.string.button_cancel
                    } else {
                        R.string.photo_words_close
                    },
                ) { onDismiss() }
            }
        }
    }
}

@Composable
private fun title(state: PhotoWordsState): String = when {
    state.isRecognizing -> stringResource(id = R.string.photo_words_recognizing)
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
private fun Recognizing() {
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

@Composable
private fun WordList(words: List<String>, modifier: Modifier) {
    LazyColumn(modifier = modifier) {
        items(words, key = { it }) { word ->
            Text(
                text = word,
                style = LexemeStyle.BodyM,
                color = formTextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewList() {
    AppTheme {
        PhotoWordsDialog(
            state = PhotoWordsState(isOpen = true, words = listOf("hello", "world")),
            isCameraAvailable = true,
            onCamera = {},
            onGallery = {},
            onDismiss = {},
        )
    }
}
