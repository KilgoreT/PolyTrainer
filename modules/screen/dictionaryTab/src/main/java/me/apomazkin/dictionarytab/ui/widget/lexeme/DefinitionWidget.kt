package me.apomazkin.dictionarytab.ui.widget.lexeme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.apomazkin.dictionarytab.entity.DefinitionUiEntity
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor

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