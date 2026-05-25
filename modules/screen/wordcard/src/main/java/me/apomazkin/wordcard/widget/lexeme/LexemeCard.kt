package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.whiteColor

/**
 * Карточка лексемы: Surface + Column + торцевые отступы. Содержимое — slot.
 */
@Composable
internal fun LexemeCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = whiteColor,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            content()
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
