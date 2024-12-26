package me.apomazkin.quiztab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.quiztab.deps.QuizTabUseCase
import me.apomazkin.quiztab.logic.DatasourceEffectHandler
import me.apomazkin.quiztab.logic.Msg
import me.apomazkin.quiztab.logic.QuizTabReducer
import me.apomazkin.quiztab.logic.QuizTabState
import me.apomazkin.quiztab.logic.UiEffectHandler
import me.apomazkin.ui.logger.LexemeLogger

class QuizTabViewModel(
    quizTabUseCase: QuizTabUseCase,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<QuizTabState, Msg> {
    
    private val stateHolder = Mate(
        initState = QuizTabState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = QuizTabReducer(logger = logger),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(quizTabUseCase = quizTabUseCase),
            UiEffectHandler()
        )
    )
    
    override val state: StateFlow<QuizTabState>
        get() = stateHolder.state
    
    override fun accept(message: Msg) = stateHolder.accept(message)
    
    class Factory(
        private val quizTabUseCase: QuizTabUseCase,
        private val logger: LexemeLogger,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuizTabViewModel(quizTabUseCase, logger) as T
        }
    }
}
