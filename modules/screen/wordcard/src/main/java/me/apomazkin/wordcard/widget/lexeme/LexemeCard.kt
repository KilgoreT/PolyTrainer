package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.cardShadowTint
import me.apomazkin.theme.draftBorder
import me.apomazkin.theme.whiteColor

private val CARD_SHAPE = RoundedCornerShape(18.dp)

/**
 * Карточка лексемы: Surface + Column + торцевые отступы. Содержимое — slot.
 * Черновик (isDraft) выделяется бордером и бейджем «Черновик» (Figma 5027:1435).
 */
@Composable
internal fun LexemeCard(
    isDraft: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = CARD_SHAPE,
                ambientColor = cardShadowTint,
                spotColor = cardShadowTint,
            ),
        shape = CARD_SHAPE,
        color = whiteColor,
        border = if (isDraft) BorderStroke(1.dp, draftBorder) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(if (isDraft) 16.dp else 14.dp))
            if (isDraft) {
                DraftBadge(modifier = Modifier.align(Alignment.End))
                Spacer(modifier = Modifier.height(10.dp))
            }
            content()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
