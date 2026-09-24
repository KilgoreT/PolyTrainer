package me.apomazkin.components_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.components_manager.mate.ComponentsManagerScreenState
import me.apomazkin.components_manager.mate.Msg
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * VM экрана `ComponentsManagerScreen`: тонкая обёртка над
 * [ComponentsManagerAssembly] — сборка раннера живёт там (общая с
 * харнесом), VM даёт только viewModelScope и продовые зависимости.
 */
class ComponentsManagerViewModel @AssistedInject constructor(
    useCase: ComponentsManagerUseCase,
    logger: LexemeLogger,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<ComponentsManagerScreenState, Msg> {

    private val stateHolder = ComponentsManagerAssembly.create(
        useCase = useCase,
        logger = logger,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<ComponentsManagerScreenState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    override fun onCleared() {
        super.onCleared()
        stateHolder.dispose()
    }

    @AssistedFactory
    interface Factory {
        fun create(): ComponentsManagerViewModel
    }
}
