package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.vocabulary.entity.LexemeUiItem

@Composable
fun LexemeItem(
    lexeme: LexemeUiItem,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.2F)
        ) {
            Text(
                text = stringResource(id = lexeme.category.shortValueRes),
                color = Color.Blue,
            )
        }
        Text(
            modifier = Modifier
                .weight(1F),
            text = lexeme.definition
        )
    }
    Divider(
        modifier = Modifier
            .fillMaxWidth()
    )
}