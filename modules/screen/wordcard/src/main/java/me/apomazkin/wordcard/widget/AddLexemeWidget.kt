package me.apomazkin.wordcard.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.Tonal
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.wordcard.R

@Composable
fun AddLexemeWidget(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    onAddLexeme: () -> Unit,
) {
    Button(
        modifier = Modifier
            .height(40.dp)
            .then(modifier),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 16.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Tonal,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        enabled = enabled,
        onClick = {}
    ) {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_circled),
                contentDescription = ""
            )
            Text(
                text = stringResource(id = R.string.word_card_add_lexeme)
            )
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        AddLexemeWidget(
            enabled = true,
            onAddLexeme = {}
        )
    }
}
