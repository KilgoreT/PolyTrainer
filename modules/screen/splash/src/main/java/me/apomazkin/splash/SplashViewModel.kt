package me.apomazkin.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import kotlinx.coroutines.flow.Flow

interface SplashUseCase {
    fun checkIfNeedAddDictionary(): Flow<Boolean>
}

/** Единственное решение сплэша: куда вести с холодного старта. */
sealed interface SplashMsg {
    data class InitChecked(val needSetup: Boolean) : SplashMsg
}

sealed interface SplashEffect : Effect {
    /** Разовый чек «нужна ли первичная настройка». */
    data object CheckInit : SplashEffect
}

/**
 * VM сплэша: тонкая обёртка над [SplashAssembly] — сборка мини-цикла
 * mate живёт там (общая с харнесом), VM даёт только viewModelScope и
 * продовые зависимости.
 */
class SplashViewModel @AssistedInject constructor(
    splashUseCase: SplashUseCase,
    navigationHandler: MateNavigationHandler,
) : ViewModel() {

    @Suppress("unused")
    private val stateHolder = SplashAssembly.create(
        useCase = splashUseCase,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    @AssistedFactory
    interface Factory {
        fun create(): SplashViewModel
    }
}
