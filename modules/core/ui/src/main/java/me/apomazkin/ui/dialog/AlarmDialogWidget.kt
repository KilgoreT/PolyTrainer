package me.apomazkin.ui.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.AlarmButtonWidget
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.dialog.base.LexemeDialog
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_PADDING_ABOVE_BUTTONS = 16

@Composable
fun AlarmDialogWidget(
    @StringRes alarmButtonText: Int,
    onAlarmClick: () -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    LexemeDialog(
        onDismissRequest = onDismissRequest
    ) {
        content.invoke(this)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DEFAULT_PADDING_ABOVE_BUTTONS.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CancelButtonWidget(
                modifier = Modifier
                    .weight(1f),
            ) { onDismissRequest.invoke() }
            AlarmButtonWidget(
                modifier = Modifier
                    .weight(1f),
                titleRes = alarmButtonText
            ) { onAlarmClick.invoke() }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        AlarmDialogWidget(
            onDismissRequest = {},
            alarmButtonText = R.string.button_delete,
            onAlarmClick = {}
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(100.dp)
                    .background(color = Color.Cyan)
            )
        }
    }
}