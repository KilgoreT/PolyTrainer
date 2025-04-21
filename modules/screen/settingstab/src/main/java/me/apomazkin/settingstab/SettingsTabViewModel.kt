package me.apomazkin.settingstab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import me.apomazkin.settingstab.logic.DatasourceEffectHandler
import me.apomazkin.settingstab.logic.Msg
import me.apomazkin.settingstab.logic.SettingsTabReducer
import me.apomazkin.settingstab.logic.SettingsTabState
import me.apomazkin.settingstab.logic.UiEffectHandler
import me.apomazkin.ui.logger.LexemeLogger

class SettingsTabViewModel(
    settingsTabUseCase: SettingsTabUseCase,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<SettingsTabState, Msg> {
    
    private val stateHolder = Mate(
        initState = SettingsTabState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = SettingsTabReducer(logger = logger),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(settingsTabUseCase),
            UiEffectHandler()
        )
    )
    
    override val state: StateFlow<SettingsTabState>
        get() = stateHolder.state
    
    override fun accept(message: Msg) = stateHolder.accept(message)
    
    class Factory(
        private val settingsTabUseCase: SettingsTabUseCase,
        private val logger: LexemeLogger,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsTabViewModel(
                settingsTabUseCase = settingsTabUseCase,
                logger = logger
            ) as T
        }
    }
}
