package me.apomazkin.quiztab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.quiztab.logic.Msg
import me.apomazkin.quiztab.logic.QuizTabReducer
import me.apomazkin.quiztab.logic.QuizTabState
import me.apomazkin.quiztab.logic.UiEffectHandler

/**
 * VM вкладки квизов — точка сборки цикла mate. Навигация идёт через
 * shared nav-handler приложения, per-экранного navigator'а нет.
 */
class QuizTabViewModel @AssistedInject constructor(
    logger: LexemeLogger,
    uiHandler: UiEffectHandler,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<QuizTabState, Msg> {

    private val stateHolder = Mate(
        initState = QuizTabState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = QuizTabReducer(logger = logger),
        effectHandlers = listOf(
            uiHandler,
            navigationHandler,
        ),
    )

    override val state: StateFlow<QuizTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): QuizTabViewModel
    }
}
