package me.apomazkin.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.apomazkin.ui.btn.PrimaryFullButtonWidget

/**
 * IS481 (F163): generic error-state widget — text сообщение + Retry button.
 *
 * Renders centered Column с message и optional Retry button. Caller отвечает за i18n
 * (messageRes + retryRes); caller также прокидывает [onRetry] callback. Если onRetry == null —
 * button скрывается.
 */
@Composable
fun ErrorStateWidget(
    @StringRes messageRes: Int,
    onRetry: (() -> Unit)? = null,
    @StringRes retryRes: Int? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = messageRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null && retryRes != null) {
            PrimaryFullButtonWidget(
                titleRes = retryRes,
                enabled = true,
                onClick = onRetry,
            )
        }
    }
}
