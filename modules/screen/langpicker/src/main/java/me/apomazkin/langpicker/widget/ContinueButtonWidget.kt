package me.apomazkin.langpicker.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.langpicker.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.PrimaryButtonWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun ContinueButtonWidget(
    isEnable: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        PrimaryButtonWidget(
            label = stringResource(id = R.string.lang_selection_button),
            enabled = isEnable,
        ) { onClick.invoke() }
    }
}

@Composable
@PreviewWidgetEn
@PreviewWidgetRu
private fun Preview() {
    AppTheme {
        ContinueButtonWidget(
            isEnable = true
        ) {}
    }
}

@Composable
@PreviewWidgetEn
@PreviewWidgetRu
private fun PreviewDisable() {
    AppTheme {
        ContinueButtonWidget(
            isEnable = false
        ) {}
    }
}