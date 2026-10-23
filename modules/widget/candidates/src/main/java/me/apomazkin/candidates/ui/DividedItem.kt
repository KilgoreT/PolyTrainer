package me.apomazkin.candidates.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.lemmaDivider

/** Толщина полосы между леммами: лемма от леммы отличима с первого взгляда, лексемы внутри — тонкой чертой. */
private val LEMMA_DIVIDER_THICKNESS = 10.dp

/** Элемент списка с разделителем под ним: один корень, порядок не зависит от раскладки списка. */
@Composable
internal fun DividedItem(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        content()
        HorizontalDivider(color = cardDivider)
    }
}

/** Карточка леммы с толстой полосой под ней (AWP-04.10): граница лемм — не черта, а полоса. */
@Composable
internal fun LemmaDividedItem(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        content()
        HorizontalDivider(color = lemmaDivider, thickness = LEMMA_DIVIDER_THICKNESS)
    }
}
