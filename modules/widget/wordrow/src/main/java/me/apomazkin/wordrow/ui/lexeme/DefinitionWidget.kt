package me.apomazkin.wordrow.ui.lexeme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.wordrow.entity.DefinitionUiEntity

@Composable
fun DefinitionWidget(
    modifier: Modifier = Modifier,
    definition: DefinitionUiEntity,
) {
    Text(
        modifier = modifier,
        text = definition.value,
        style = LexemeStyle.BodyM
            .copy(color = grayTextColor),
    )
}
