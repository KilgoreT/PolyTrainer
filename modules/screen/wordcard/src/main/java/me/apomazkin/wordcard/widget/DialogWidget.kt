package me.apomazkin.wordcard.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.gradientPrimary
import me.apomazkin.ui.btn.base.GradientButtonWidget
import me.apomazkin.ui.btn.base.OutlineButtonWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DialogWidget(
    @StringRes titleRes: Int,
    buttonGradient: Brush = gradientPrimary,
    onClick: () -> Unit,
    onClickEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onDismissEnabled: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = 24.dp,
                        vertical = 24.dp,
                    )
            ) {
                Text(
                    modifier = Modifier
                        .align(CenterHorizontally),
                    text = stringResource(id = titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                )
                content
                    ?.let { it() }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .align(End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, End),
                ) {
                    OutlineButtonWidget(
                        titleRes = R.string.button_cancel,
                        enabledColor = MaterialTheme.colorScheme.primary,
                        onClick = onDismiss,
                        enabled = onDismissEnabled,
                    )
                    GradientButtonWidget(
                        titleRes = R.string.button_ok,
                        gradient = buttonGradient,
                        onClick = onClick,
                        enabled = onClickEnabled,
                    )
                }
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        DialogWidget(
            titleRes = R.string.word_card_edit_word,
            onClick = {},
            onDismiss = {},
        )
    }
}