package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.dictionary.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Заголовочная зона формы. Заголовок показывается только в онбординге
 * (без AppBar); в create/edit заголовок живёт в [me.apomazkin.dictionary.widget.DictionaryAppBar].
 * Подзаголовок виден во всех режимах.
 */
@Composable
internal fun FormHeaderWidget(
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (showTitle) {
            Text(
                text = stringResource(id = R.string.dictionary_new),
                style = LexemeStyle.H4,
                color = LexemeColor.secondary,
            )
        }
        Text(
            text = stringResource(id = R.string.dictionary_form_subtitle),
            style = LexemeStyle.BodyM,
            color = formTextSecondary,
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewWithTitle() {
    AppTheme {
        FormHeaderWidget(showTitle = true)
    }
}

@Composable
@PreviewWidget
private fun PreviewSubtitleOnly() {
    AppTheme {
        FormHeaderWidget(showTitle = false)
    }
}
