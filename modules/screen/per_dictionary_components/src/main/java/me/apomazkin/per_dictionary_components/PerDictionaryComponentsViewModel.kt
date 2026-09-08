package me.apomazkin.per_dictionary_components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import me.apomazkin.per_dictionary_components.mate.DatasourceEffectHandler
import me.apomazkin.per_dictionary_components.mate.Msg
import me.apomazkin.per_dictionary_components.mate.NavigationEffectHandler
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsReducer
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsScreenState
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsSubHandler
import me.apomazkin.per_dictionary_components.mate.UiEffectHandler
import me.apomazkin.per_dictionary_components.mate.subscriptions

/**
 * ViewModel экрана `PerDictionaryComponentsScreen`. Собирает Mate из:
 * - [PerDictionaryComponentsReducer] — pure reduction.
 * - [DatasourceEffectHandler] — Effect → UseCase → Msg.
 * - подписки `subscriptions()` из state + [PerDictionaryComponentsSubHandler] —
 *   живой список компонентов (dictionaryId берётся из state).
 * - [UiEffectHandler] — UiEffect → UiMsg.
 * - [NavigationEffectHandler] — Back уже в base Mate Nav handler.
 */
class PerDictionaryComponentsViewModel @AssistedInject constructor(
    @Assisted dictionaryId: Long,
    @Assisted navigator: PerDictionaryComponentsNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    subHandler: PerDictionaryComponentsSubHandler,
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
        effectHandlers = listOf(
            datasourceHandler,
            uiHandler,
            navHandlerFactory.create(navigator),
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(subHandler),
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
