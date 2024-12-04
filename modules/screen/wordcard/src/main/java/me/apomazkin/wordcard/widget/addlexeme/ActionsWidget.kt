package me.apomazkin.wordcard.widget.addlexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.SecondaryButtonWidget
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.R

@Composable
internal fun ActionsWidget(
    actionButtonEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CancelButtonWidget(
            modifier = Modifier
                .weight(1f),
            enabled = true,
            onClick = onDismiss,
        )
        SecondaryButtonWidget(
            modifier = Modifier
                .weight(1f),
            titleRes = R.string.word_card_bottom_title_append,
            enabled = actionButtonEnabled,
            onClick = onConfirm,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        ActionsWidget(
            actionButtonEnabled = enabled,
            onDismiss = {},
            onConfirm = {},
        )
    }
}