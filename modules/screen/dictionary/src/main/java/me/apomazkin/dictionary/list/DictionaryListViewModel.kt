package me.apomazkin.dictionary.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.dictionary.DictionaryUseCase

/**
 * VM списка словарей: тонкая обёртка над [DictionaryListAssembly] —
 * сборка раннера живёт там (общая с харнесом), VM даёт только
 * viewModelScope и продовые зависимости.
 */
class DictionaryListViewModel @AssistedInject constructor(
    useCase: DictionaryUseCase,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<DictionaryListScreenState, DictionaryListMsg> {

    private val stateHolder = DictionaryListAssembly.create(
        useCase = useCase,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<DictionaryListScreenState>
        get() = stateHolder.state

    override fun accept(message: DictionaryListMsg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): DictionaryListViewModel
    }
}
