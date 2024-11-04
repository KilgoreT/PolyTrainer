package me.apomazkin.ui.dialog.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_ROUND_CORNER = 16
private const val DEFAULT_PADDING = 24

@Composable
fun LexemeDialog(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    usePlatformDefaultWidth: Boolean = true,
    decorFitsSystemWindows: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = usePlatformDefaultWidth,
            decorFitsSystemWindows = decorFitsSystemWindows,
        ),
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(DEFAULT_ROUND_CORNER.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(DEFAULT_PADDING.dp)
            ) {
                content.invoke(this)
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        LexemeDialog(
            onDismissRequest = {},
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