package me.apomazkin.settingstab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import me.apomazkin.settingstab.logic.DatasourceEffectHandler
import me.apomazkin.settingstab.logic.Msg
import me.apomazkin.settingstab.logic.SettingsTabReducer
import me.apomazkin.settingstab.logic.SettingsTabState
import me.apomazkin.settingstab.logic.UiEffectHandler

class SettingsTabViewModel @AssistedInject constructor(
    @Assisted navigator: SettingsNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: SettingsNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<SettingsTabState, Msg> {

    private val stateHolder = Mate(
        initState = SettingsTabState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = SettingsTabReducer(logger = logger),
        effectHandlers = listOf(
            datasourceHandler,
            uiHandler,
            navHandlerFactory.create(navigator),
        ),
    )

    override val state: StateFlow<SettingsTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: SettingsNavigator): SettingsTabViewModel
    }
}
