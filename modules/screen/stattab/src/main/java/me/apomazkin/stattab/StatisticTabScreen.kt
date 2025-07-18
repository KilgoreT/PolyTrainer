package me.apomazkin.stattab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.stattab.deps.StatisticUiDeps
import me.apomazkin.stattab.deps.StatisticUseCase
import me.apomazkin.stattab.mate.Msg
import me.apomazkin.stattab.mate.StatisticState
import me.apomazkin.stattab.widget.GradeWidget
import me.apomazkin.stattab.widget.TitleWithValue
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.statInProcessBg
import me.apomazkin.theme.statInProcessFg
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.preview.PreviewScreen

@Composable
fun StatisticTabScreen(
    statisticUseCase: StatisticUseCase,
    statisticUiDeps: StatisticUiDeps,
    logger: LexemeLogger,
    viewModel: StatisticViewModel = viewModel(
        factory = StatisticViewModel.Factory(
            logger = logger,
            statisticUseCase = statisticUseCase,
        )
    ),
) {
    val state: StatisticState by viewModel.state.collectAsStateWithLifecycle()
    StatisticScreen(
        state = state,
        statisticUiDeps = statisticUiDeps,
    ) { viewModel.accept(it) }
}

@Composable
internal fun StatisticScreen(
    state: StatisticState,
    statisticUiDeps: StatisticUiDeps,
    sendMessage: (Msg) -> Unit,
) {

    Scaffold(
        topBar = {
            statisticUiDeps.AppBar(titleResId = R.string.stat_app_bar_title)
        },
    ) { paddings: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {
            if (state.isLoading) {

            } else {
                Spacer(modifier = Modifier.height(44.dp))
                TitleWithValue(
                    titleResId = R.string.stat_title_words_count,
                    state.wordCount.toString()
                )
                TitleWithValue(
                    titleResId = R.string.stat_title_lexeme_count,
                    state.lexemeCount.toString()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    state.quizState.quizStat.forEach { stat ->
                        GradeWidget(
                            value = stat.value.toString(),
                            grade = stat.processState.toString(),
                            fgColor = stat.processState.toFg(),
                            bgColor = stat.processState.toBg()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    state.quizState.quizGrades
                        .forEach { quizGrade ->
                            GradeWidget(
                                value = quizGrade.value.toString(),
                                grade = quizGrade.grade.toString(),
                                fgColor = statInProcessFg,
                                bgColor = statInProcessBg,
                            )
                        }
                }
            }
        }
    }
}

@PreviewScreen
@Composable
private fun Preview() {
    AppTheme {
        StatisticScreen(
            state = StatisticState(
                isLoading = false,
            ),
            statisticUiDeps = object : StatisticUiDeps {
                @Composable
                override fun AppBar(titleResId: Int) {
                }
            },
            sendMessage = { }
        )
    }
}