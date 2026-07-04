package me.apomazkin.components_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.components_manager.mate.AllUserDefinedTypesFlowHandler
import me.apomazkin.components_manager.mate.ComponentsManagerReducer
import me.apomazkin.components_manager.mate.ComponentsManagerScreenState
import me.apomazkin.components_manager.mate.DatasourceEffectHandler
import me.apomazkin.components_manager.mate.DictionariesFlowHandler
import me.apomazkin.components_manager.mate.Msg
import me.apomazkin.components_manager.mate.NavigationEffectHandler
import me.apomazkin.components_manager.mate.UiEffectHandler
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

/**
 * ViewModel экрана `ComponentsManagerScreen`. Собирает Mate из:
 * - [ComponentsManagerReducer] — pure reduction.
 * - [DatasourceEffectHandler] — Effect → UseCase → Msg.
 * - [AllUserDefinedTypesFlowHandler] — auto-subscribe на init Mate.
 * - [UiEffectHandler] — UiEffect → UiMsg.
 * - [NavigationEffectHandler] — Back уже в base Mate Nav handler.
 *
 * `initEffects = ∅` — flow handler стартует через `subscribe(scope, send)` на init Mate.
 */
class ComponentsManagerViewModel @AssistedInject constructor(
    @Assisted navigator: ComponentsManagerNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    flowHandler: AllUserDefinedTypesFlowHandler,
    dictionariesFlowHandler: DictionariesFlowHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: NavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<ComponentsManagerScreenState, Msg> {

    private val stateHolder = Mate(
        initState = ComponentsManagerScreenState(isLoading = true),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = ComponentsManagerReducer(logger = logger),
        effectHandlerSet = setOf(
            datasourceHandler,
            flowHandler,
            dictionariesFlowHandler,
            uiHandler,
            navHandlerFactory.create(navigator),
        ),
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
        fun create(navigator: ComponentsManagerNavigator): ComponentsManagerViewModel
    }
}
