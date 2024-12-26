@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.quiztab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.quiztab.deps.QuizTabUseCase
import me.apomazkin.quiztab.logic.Msg
import me.apomazkin.quiztab.logic.QuizTabState
import me.apomazkin.quiztab.logic.UiMsg
import me.apomazkin.quiztab.logic.processor.toMateEvent
import me.apomazkin.quiztab.widget.AppBarWidget
import me.apomazkin.quiztab.widget.QuizItemWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.lifecycle.LifecycleEventHandler
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.preview.PreviewScreen

@Composable
fun QuizTabScreen(
    quizTabUseCase: QuizTabUseCase,
    logger: LexemeLogger,
    viewModel: QuizTabViewModel = viewModel(
        factory = QuizTabViewModel.Factory(quizTabUseCase, logger)
    ),
    openAddDict: () -> Unit,
) {
    
    LifecycleEventHandler(
        action = { viewModel.accept(UiMsg.LifeCycleEvent(it.toMateEvent())) }
    )
    val state: QuizTabState by viewModel.state.collectAsStateWithLifecycle()
    QuizTabScreen(
        state = state,
        openAddDict = openAddDict,
    ) { viewModel.accept(it) }
    
}

@Composable
internal fun QuizTabScreen(
    state: QuizTabState,
    openAddDict: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    Scaffold(
        topBar = {
            AppBarWidget(
                state = state.topBarState.langPickerState,
                openAddDict = openAddDict,
                sendMessage = sendMessage,
            )
        },
    ) { paddings: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuizItemWidget(
                imageRes = R.drawable.ic_quiz_write,
                titleRes = R.string.quiz_item_title_write,
                subTitleRes = R.string.quiz_item_subtitle_write
            ) {}
            QuizItemWidget(
                imageRes = R.drawable.ic_quiz_write,
                titleRes = R.string.quiz_item_title_write,
                subTitleRes = R.string.quiz_item_subtitle_write
            ) {}
        }
    }
    
    
}

@PreviewScreen
@Composable
private fun Preview() {
    AppTheme {
        QuizTabScreen(
            state = QuizTabState(),
            openAddDict = {},
        ) {}
    }
}