package me.apomazkin.settingstab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateStore
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import me.apomazkin.settingstab.logic.Msg
import me.apomazkin.settingstab.logic.SettingsTabState

/**
 * VM вкладки настроек: тонкая обёртка над [SettingsTabAssembly] —
 * сборка раннера живёт там (общая с харнесом), VM даёт только
 * viewModelScope и продовые зависимости.
 */
class SettingsTabViewModel @AssistedInject constructor(
    logger: LexemeLogger,
    useCase: SettingsTabUseCase,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStore<SettingsTabState, Msg> {

    private val stateHolder = SettingsTabAssembly.create(
        useCase = useCase,
        logger = logger,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<SettingsTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): SettingsTabViewModel
    }
}
