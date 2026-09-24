package me.apomazkin.dictionaryappbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarState
import me.apomazkin.dictionaryappbar.mate.Msg
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * VM виджета app bar со словарём: тонкая обёртка над
 * [DictionaryAppBarAssembly] — сборка раннера живёт там (общая с
 * харнесом), VM даёт только viewModelScope и продовые зависимости.
 */
class DictionaryAppBarViewModel @AssistedInject constructor(
    useCase: DictionaryAppBarUseCase,
    logger: LexemeLogger,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<DictionaryAppBarState, Msg> {

    private val stateHolder = DictionaryAppBarAssembly.create(
        useCase = useCase,
        logger = logger,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<DictionaryAppBarState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): DictionaryAppBarViewModel
    }
}
