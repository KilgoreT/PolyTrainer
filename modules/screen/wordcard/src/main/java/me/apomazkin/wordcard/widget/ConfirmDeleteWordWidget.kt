package me.apomazkin.wordcard.widget

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.dialog.AlarmDialogWidget
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Confirm-dialog удаления слова.
 */
@Composable
internal fun ConfirmDeleteWordWidget(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlarmDialogWidget(
        alarmButtonText = R.string.button_delete,
        onAlarmClick = onConfirm,
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = stringResource(id = R.string.vocabulary_delete_word),
            style = LexemeStyle.H6,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.word_card_delete_word_subheading),
            style = LexemeStyle.BodyL,
            color = grayTextColor,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        ConfirmDeleteWordWidget(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
