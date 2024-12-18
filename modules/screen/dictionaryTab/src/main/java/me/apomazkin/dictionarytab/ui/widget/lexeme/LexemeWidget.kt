package me.apomazkin.dictionarytab.ui.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionarytab.entity.DefinitionUiEntity
import me.apomazkin.dictionarytab.entity.LexemeUiItem
import me.apomazkin.dictionarytab.entity.TranslationUiEntity
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget
import java.util.Date

@Composable
fun LexemeWidget(
    modifier: Modifier = Modifier,
    lexeme: LexemeUiItem,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lexeme.translation?.let { translation ->
            TranslationWidget(
                modifier = Modifier,
                translation = translation,
            )
        }
        lexeme.definition?.let { definition ->
            DefinitionWidget(
                modifier = Modifier,
                definition = definition,
            )
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
        ) {
            LexemeWidget(
                lexeme = LexemeUiItem(
                    id = 1,
                    wordId = 1,
                    translation = TranslationUiEntity("translation"),
                    definition = DefinitionUiEntity("definition definition definition definition definition"),
                    addDate = Date(),
                    changeDate = null,
                    removeDate = null,
                )
            )
        }
    }
}