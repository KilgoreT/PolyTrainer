package me.apomazkin.dictionary.list.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import me.apomazkin.dictionary.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun EmptyDictionaryWidget(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.dictionary_empty_title),
            style = LexemeStyle.H5,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.dictionary_empty_subtitle),
            style = LexemeStyle.BodyL,
            color = grayTextColor,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        EmptyDictionaryWidget()
    }
}
