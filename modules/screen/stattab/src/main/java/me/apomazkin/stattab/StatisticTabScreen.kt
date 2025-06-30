package me.apomazkin.stattab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.stattab.deps.StatisticUiDeps
import me.apomazkin.stattab.deps.StatisticUseCase
import me.apomazkin.stattab.mate.Msg
import me.apomazkin.stattab.mate.StatisticState
import me.apomazkin.stattab.mate.UiMsg
import me.apomazkin.stattab.mate.toMateEvent
import me.apomazkin.ui.lifecycle.LifecycleEventHandler
import me.apomazkin.ui.logger.LexemeLogger

@Composable
fun StatisticTabScreen(
        statisticUseCase: StatisticUseCase,
        statisticUiDeps: StatisticUiDeps,
        logger: LexemeLogger,
        viewModel: StatisticViewModel = viewModel(
                factory = StatisticViewModel.Factory(logger)
        ),
) {

    LifecycleEventHandler(
            action = { viewModel.accept(UiMsg.LifeCycleEvent(it.toMateEvent())) }
    )
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
                statisticUiDeps.AppBar(titleResId = R.string.logo_title)
            },
    ) { paddings : PaddingValues ->

        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(paddings)
                        .background(Color.Magenta)
        )
    }
}