package me.apomazkin.quiztab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import me.apomazkin.quiztab.logic.Msg
import me.apomazkin.quiztab.logic.QuizTabReducer
import me.apomazkin.quiztab.logic.QuizTabState
import me.apomazkin.quiztab.logic.UiEffectHandler

class QuizTabViewModel @AssistedInject constructor(
    @Assisted navigator: QuizTabNavigator,
    logger: LexemeLogger,
    uiHandler: UiEffectHandler,
    navHandlerFactory: QuizTabNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<QuizTabState, Msg> {

    private val stateHolder = Mate(
        initState = QuizTabState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = QuizTabReducer(logger = logger),
        effectHandlers = listOf(
            uiHandler,
            navHandlerFactory.create(navigator),
        ),
    )

    override val state: StateFlow<QuizTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: QuizTabNavigator): QuizTabViewModel
    }
}
