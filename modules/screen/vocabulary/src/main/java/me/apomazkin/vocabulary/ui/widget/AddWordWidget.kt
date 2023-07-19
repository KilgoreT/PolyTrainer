@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package me.apomazkin.vocabulary.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.bgAlfa
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.logic.AddWordDialogState
import me.apomazkin.vocabulary.logic.Msg

@Composable
internal fun AddWordWidget(
    state: AddWordDialogState,
    wordValue: String,
    checkValue: () -> Boolean,
    sendMessage: (Msg) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        modifier = Modifier,
        visible = state.isAddWordWidgetOpen,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgAlfa)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) { sendMessage(Msg.AddWordWidget(show = false)) },
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                shadowElevation = 16.dp,
            ) {
                Column {
                    WordEditWidget(
                        label = stringResource(id = R.string.vocabulary_enter_word),
                        wordValue = wordValue,
                        onWordValueChange = { sendMessage(Msg.WordValueChange(it)) },
                        onAddWord = {
                            if (it.trim().isNotEmpty()) {
                                sendMessage(Msg.AddWord(wordValue.trim()))
                            }
                            sendMessage(Msg.AddWordWidget(show = false))
                        },
                    )
                }
            }
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        AddWordWidget(
            state = AddWordDialogState(
                isAddWordWidgetOpen = true
            ),
            wordValue = "",
            checkValue = { true },
        ) {}
    }
}