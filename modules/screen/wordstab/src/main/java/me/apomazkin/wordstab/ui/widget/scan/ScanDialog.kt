package me.apomazkin.wordstab.ui.widget.scan

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.logic.ScanDialogState

/**
 * Диалог выбора источника снимка: «Камера» и «Галерея» (WFP-06) или, если
 * камера не запустилась, текст причины и «Закрыть» (WFP-07). Что дальше
 * после снимка — на экране скана, не здесь.
 * Цвета явные, парой из LanguagePickerDialog: фон formBackground, текст
 * formTextTertiary. [isCameraAvailable] false — кнопки «Камера» нет
 * (WFP-06.3).
 */
@Composable
internal fun ScanDialog(
    state: ScanDialogState,
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
                    text = stringResource(id = R.string.photo_words_title),
                    style = LexemeStyle.BodyM,
                    color = formTextTertiary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (state.errorRes == null) {
                    SourceChooser(
                        isCameraAvailable = isCameraAvailable,
                        onCamera = onCamera,
                        onGallery = onGallery,
                    )
                } else {
                    Message(textRes = state.errorRes)
                }
                Spacer(modifier = Modifier.height(16.dp))
                CancelButtonWidget(
                    modifier = Modifier.fillMaxWidth(),
                    titleRes = if (state.errorRes == null) {
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
        ScanDialog(
            state = ScanDialogState(isOpen = true),
            isCameraAvailable = true,
            onCamera = {},
            onGallery = {},
            onDismiss = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewGalleryOnly() {
    AppTheme {
        ScanDialog(
            state = ScanDialogState(isOpen = true),
            isCameraAvailable = false,
            onCamera = {},
            onGallery = {},
            onDismiss = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewCameraError() {
    AppTheme {
        ScanDialog(
            state = ScanDialogState(isOpen = true, errorRes = R.string.photo_words_error_camera),
            isCameraAvailable = true,
            onCamera = {},
            onGallery = {},
            onDismiss = {},
        )
    }
}
