package me.apomazkin.wordstab.ui.widget.photo

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import me.apomazkin.wordstab.logic.PhotoPhase
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.photo.PhotoFormError

/**
 * IS527/IS529/IS532: диалог запуска — выбор источника, индикатор с
 * подписью фазы (WFT-16; сборка окна — AWP-24), «слов не найдено»
 * (WFT-60), ошибка запуска, отказ сервера (WFT-61, WFT-63) или «окно не
 * собралось» (AWP-24). Собранное окно рисует [PhotoAddWindow].
 * Цвета явные, парой из LanguagePickerDialog (IS525): фон formBackground,
 * текст formTextTertiary, акцент LexemeColor.primary.
 * [isCameraAvailable] false — кнопки «Камера» нет.
 */
@Composable
internal fun PhotoWordsDialog(
    state: PhotoWordsState,
    isCameraAvailable: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = formBackground,
            modifier = Modifier.fillMaxWidth(),
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
                    state.formError != null -> Message(textRes = formErrorRes(state.formError))
                    else -> Message(textRes = R.string.photo_words_empty)
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

/** Подпись фазы (WFT-16.1, 16.2; AWP-24) или заголовок диалога. */
@Composable
private fun title(state: PhotoWordsState): String = when (state.phase) {
    PhotoPhase.RECOGNIZING -> stringResource(id = R.string.photo_words_recognizing)
    PhotoPhase.LOOKING -> stringResource(id = R.string.photo_words_looking)
    PhotoPhase.FORM -> stringResource(id = R.string.photo_add_form_loading)
    PhotoPhase.IDLE -> stringResource(id = R.string.photo_words_title)
}

@StringRes
private fun formErrorRes(error: PhotoFormError): Int = when (error) {
    PhotoFormError.DICTIONARY_MISSING -> R.string.photo_add_dictionary_missing
    PhotoFormError.LOAD_FAILED -> R.string.photo_add_form_failed
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

@PreviewWidget
@Composable
private fun PreviewChooser() {
    AppTheme {
        PhotoWordsDialog(
            state = PhotoWordsState(isOpen = true, isChoosingSource = true),
            isCameraAvailable = true,
            onCamera = {},
            onGallery = {},
            onDismiss = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewFormError() {
    AppTheme {
        PhotoWordsDialog(
            state = PhotoWordsState(isOpen = true, formError = PhotoFormError.DICTIONARY_MISSING),
            isCameraAvailable = true,
            onCamera = {},
            onGallery = {},
            onDismiss = {},
        )
    }
}
