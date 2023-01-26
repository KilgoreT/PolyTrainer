package me.apomazkin.langpicker.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.apomazkin.langpicker.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.PrimaryButtonWidget

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
@Preview(showBackground = true)
private fun Preview() {
    AppTheme {
        ContinueButtonWidget(
            isEnable = true
        ) {}
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewDisable() {
    AppTheme {
        ContinueButtonWidget(
            isEnable = false
        ) {}
    }
}