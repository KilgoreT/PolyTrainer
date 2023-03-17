@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)

package me.apomazkin.vocabulary.ui.widget.detailDialog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.FFD9E3
import me.apomazkin.ui.preview.PreviewScreenEn
import me.apomazkin.ui.preview.PreviewScreenRu
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.WordDetailDialogState
import me.apomazkin.vocabulary.logic.WordDetailMsg

@Composable
fun WordDetailDialogWidget(
    state: WordDetailDialogState,
    sendMsg: (Msg) -> Unit,
) {
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
        onDismissRequest = {
            sendMsg(WordDetailMsg.Hide)
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Column {
                WordDetailHeaderWidget(
                    word = state.word,
                    onClose = { sendMsg(WordDetailMsg.Hide) },
                    onSave = {
                        sendMsg(WordDetailMsg.Save(state.wordId, state.lexemeList))
                        sendMsg(WordDetailMsg.Hide)
                    }
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FFD9E3)
                            .padding(horizontal = 8.dp),
                        text = state.word,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.weight(1F))
                    OutlinedButton(
                        modifier = Modifier
                            .padding(horizontal = 24.dp),
                        onClick = { sendMsg(WordDetailMsg.AddLexeme(state.wordId)) }
                    ) {
                        Text(text = "Добавить перевод")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    state.lexemeList.forEach {
                        Divider(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                        )
                        LexemeWidget(state = it, sendMsg = sendMsg)
                    }

                    Spacer(modifier = Modifier.weight(1F))
                    OutlinedButton(
                        modifier = Modifier
                            .align(CenterHorizontally)
                            .padding(top = 56.dp, bottom = 16.dp),
                        onClick = {
                            sendMsg(Msg.DeleteWord(wordId = state.wordId, wordValue = state.word))
                            sendMsg(WordDetailMsg.Hide)
                        }
                    ) {
                        Text(text = "Delete word")
                    }
                }
            }
        }
    }
}

@PreviewScreenRu
@PreviewScreenEn
@Composable
private fun Preview() {
    AppTheme {
        WordDetailDialogWidget(
            state = WordDetailDialogState(
                isOpen = true,
                word = "word"
            ),
        ) {}
    }
}