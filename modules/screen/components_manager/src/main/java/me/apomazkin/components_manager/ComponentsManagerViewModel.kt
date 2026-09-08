package me.apomazkin.components_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.components_manager.mate.ComponentsManagerReducer
import me.apomazkin.components_manager.mate.ComponentsManagerScreenState
import me.apomazkin.components_manager.mate.ComponentsManagerSubHandler
import me.apomazkin.components_manager.mate.DatasourceEffectHandler
import me.apomazkin.components_manager.mate.Msg
import me.apomazkin.components_manager.mate.UiEffectHandler
import me.apomazkin.components_manager.mate.subscriptions
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * ViewModel экрана `ComponentsManagerScreen`. Собирает Mate из:
 * - [ComponentsManagerReducer] — pure reduction.
 * - [DatasourceEffectHandler] — Effect → UseCase → Msg.
 * - подписки `subscriptions()` из state + [ComponentsManagerSubHandler] —
 *   живые списки типов и словарей.
 * - [UiEffectHandler] — UiEffect → UiMsg.
 * - shared nav-handler приложения — навигационные эффекты экрана
 *   (включая Back) резолвятся общей таблицей навигации.
 */
class ComponentsManagerViewModel @AssistedInject constructor(
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    subHandler: ComponentsManagerSubHandler,
    uiHandler: UiEffectHandler,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<ComponentsManagerScreenState, Msg> {

    private val stateHolder = Mate(
        initState = ComponentsManagerScreenState(isLoading = true),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = ComponentsManagerReducer(logger = logger),
        effectHandlers = listOf(
            datasourceHandler,
            uiHandler,
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(subHandler),
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
