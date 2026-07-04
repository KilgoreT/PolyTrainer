package me.apomazkin.per_dictionary_components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.per_dictionary_components.mate.ComponentsForDictionaryFlowHandler
import me.apomazkin.per_dictionary_components.mate.DatasourceEffectHandler
import me.apomazkin.per_dictionary_components.mate.Msg
import me.apomazkin.per_dictionary_components.mate.NavigationEffectHandler
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsReducer
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsScreenState
import me.apomazkin.per_dictionary_components.mate.UiEffectHandler

/**
 * ViewModel экрана `PerDictionaryComponentsScreen`. Собирает Mate из:
 * - [PerDictionaryComponentsReducer] — pure reduction.
 * - [DatasourceEffectHandler] — Effect → UseCase → Msg.
 * - [ComponentsForDictionaryFlowHandler] — auto-subscribe на init Mate (assisted dictionaryId).
 * - [UiEffectHandler] — UiEffect → UiMsg.
 * - [NavigationEffectHandler] — Back уже в base Mate Nav handler.
 *
 * `initEffects = ∅` — flow handler стартует через `subscribe(scope, send)` на init Mate.
 */
class PerDictionaryComponentsViewModel @AssistedInject constructor(
    @Assisted dictionaryId: Long,
    @Assisted navigator: PerDictionaryComponentsNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    flowHandlerFactory: ComponentsForDictionaryFlowHandler.Factory,
    uiHandler: UiEffectHandler,
    navHandlerFactory: NavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<PerDictionaryComponentsScreenState, Msg> {

    private val stateHolder = Mate(
        initState = PerDictionaryComponentsScreenState(
            dictionaryId = dictionaryId,
            isLoading = true,
        ),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = PerDictionaryComponentsReducer(logger = logger),
        effectHandlerSet = setOf(
            datasourceHandler,
            flowHandlerFactory.create(dictionaryId),
            uiHandler,
            navHandlerFactory.create(navigator),
        ),
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
            navigator: PerDictionaryComponentsNavigator,
        ): PerDictionaryComponentsViewModel
    }
}
