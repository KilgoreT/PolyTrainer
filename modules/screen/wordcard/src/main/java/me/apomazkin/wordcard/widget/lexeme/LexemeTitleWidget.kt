package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.icondropdowned.IconDropdownWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.R
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.Msg

@Composable
fun LexemeTitleWidget(
    order: Int,
    state: LexemeState,
    sendMessage: (Msg) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.word_card_lexeme_order_title, order),
            color = MaterialTheme.colorScheme.primary,
            style = LexemeStyle.BodyS
        )
        IconDropdownWidget(
            isDropDownOpen = state.isMenuOpen,
            onClickDropDown = {
                sendMessage(
                    Msg.ShowLexemeDropDown(
                        lexemeId = state.id,
                        isShow = true
                    )
                )
            },
            onDismissRequest = {
                sendMessage(
                    Msg.ShowLexemeDropDown(
                        lexemeId = state.id,
                        isShow = false,
                    )
                )
            },
            icon = {
                IconBoxed(
                    iconRes = R.drawable.ic_more_horizonral,
                    colorEnabled = MaterialTheme.colorScheme.primary,
                    enabled = true,
                    size = 24,
                )
            }
        ) {
            state.translation ?: AddTranslationLexemeMenuItem {
                sendMessage(Msg.AppendTranslation(lexemeId = state.id))
                sendMessage(Msg.ShowLexemeDropDown(lexemeId = state.id, isShow = false))
            }
            state.definition ?: AddDefinitionLexemeMenuItem {
                sendMessage(Msg.AppendDefinition(lexemeId = state.id))
                sendMessage(Msg.ShowLexemeDropDown(lexemeId = state.id, isShow = false))
            }
            DeleteLexemeMenuItem {
                sendMessage(Msg.DeleteLexeme(lexemeId = state.id))
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(24.dp),
        ) {
            LexemeTitleWidget(
                order = 1,
                state = LexemeState(),
                sendMessage = { }
            )
        }
    }
}