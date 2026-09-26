package me.apomazkin.per_dictionary_components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateStore
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import me.apomazkin.per_dictionary_components.mate.Msg
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsScreenState

/**
 * VM экрана `PerDictionaryComponentsScreen`: тонкая обёртка над
 * [PerDictionaryComponentsAssembly] — сборка раннера живёт там (общая
 * с харнесом), VM даёт только viewModelScope и продовые зависимости.
 */
class PerDictionaryComponentsViewModel @AssistedInject constructor(
    @Assisted dictionaryId: Long,
    useCase: PerDictionaryComponentsUseCase,
    logger: LexemeLogger,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStore<PerDictionaryComponentsScreenState, Msg> {

    private val stateHolder = PerDictionaryComponentsAssembly.create(
        useCase = useCase,
        logger = logger,
        navigationHandler = navigationHandler,
        dictionaryId = dictionaryId,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<PerDictionaryComponentsScreenState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    override fun onCleared() {
        super.onCleared()
        stateHolder.dispose()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            dictionaryId: Long,
        ): PerDictionaryComponentsViewModel
    }
}
