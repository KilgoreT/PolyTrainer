package me.apomazkin.scancandidates.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget

/**
 * Панели экрана до окна: индикатор с подписью фазы (WFP-13, AWP-24) и текст с
 * кнопками — «слов нет» (WFP-19), ошибка (WFP-20, WFP-26). Цвета явные, парой
 * из диалога выбора источника: фон formBackground, текст formTextTertiary,
 * акцент LexemeColor.primary. Что показать и какие кнопки, решает стейт.
 */
@Composable
internal fun ProgressPanel(
    @StringRes captionRes: Int,
    onCancel: () -> Unit,
) {
    PhasePanel {
        CircularProgressIndicator(color = LexemeColor.primary)
        Spacer(modifier = Modifier.height(16.dp))
        PanelText(textRes = captionRes)
        Spacer(modifier = Modifier.height(24.dp))
        CancelButtonWidget(
            modifier = Modifier.fillMaxWidth(),
            titleRes = R.string.button_cancel,
            onClick = onCancel,
        )
    }
}

/** Текст с «Закрыть» и, если есть [hasRetryButton], «Повторить» над ним. */
@Composable
internal fun MessagePanel(
    @StringRes textRes: Int,
    hasRetryButton: Boolean,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    PhasePanel {
        PanelText(textRes = textRes)
        Spacer(modifier = Modifier.height(24.dp))
        if (hasRetryButton) {
            PrimaryFullButtonWidget(
                modifier = Modifier.fillMaxWidth(),
                titleRes = R.string.photo_add_retry,
                onClick = onRetry,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        CancelButtonWidget(
            modifier = Modifier.fillMaxWidth(),
            titleRes = R.string.photo_words_close,
            onClick = onClose,
        )
    }
}

@Composable
private fun PhasePanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = formBackground,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

@Composable
private fun PanelText(@StringRes textRes: Int) {
    Text(
        text = stringResource(id = textRes),
        style = LexemeStyle.BodyL,
        color = formTextTertiary,
        textAlign = TextAlign.Center,
    )
}
