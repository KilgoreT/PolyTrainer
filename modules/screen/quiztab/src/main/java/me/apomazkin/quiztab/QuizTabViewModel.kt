package me.apomazkin.quiztab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.quiztab.logic.Msg
import me.apomazkin.quiztab.logic.QuizTabState

/**
 * VM вкладки квизов: тонкая обёртка над [QuizTabAssembly] — сборка
 * раннера живёт там (общая с харнесом), VM даёт только viewModelScope
 * и продовые зависимости.
 */
class QuizTabViewModel @AssistedInject constructor(
    logger: LexemeLogger,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<QuizTabState, Msg> {

    private val stateHolder = QuizTabAssembly.create(
        logger = logger,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<QuizTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): QuizTabViewModel
    }
}
