package me.apomazkin.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

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
 * Мини-цикл mate сплэша: init-эффект [SplashEffect.CheckInit] →
 * ответ базы → навигационный эффект ([SplashNavigationEffect]),
 * который довозит shared nav-handler приложения.
 */
class SplashViewModel @AssistedInject constructor(
    splashUseCase: SplashUseCase,
    navigationHandler: MateNavigationHandler,
) : ViewModel() {

    private class CheckInitHandler(
        private val useCase: SplashUseCase,
    ) : MateEffectHandler<SplashMsg, SplashEffect> {
        override val effectFamily: KClass<SplashEffect> = SplashEffect::class

        override suspend fun runEffect(
            effect: SplashEffect,
            consumer: (SplashMsg) -> Unit,
        ) {
            val needSetup = useCase.checkIfNeedAddDictionary().first()
            consumer(SplashMsg.InitChecked(needSetup))
        }
    }

    private object Reducer : MateReducer<Unit, SplashMsg, Effect> {
        override fun reduce(
            state: Unit,
            message: SplashMsg,
        ): ReducerResult<Unit, Effect> = when (message) {
            is SplashMsg.InitChecked ->
                Unit to setOf(
                    if (message.needSetup) {
                        SplashNavigationEffect.OpenDictionarySetup
                    } else {
                        SplashNavigationEffect.OpenMainScreen
                    },
                )
        }
    }

    @Suppress("unused")
    private val stateHolder = Mate<Unit, SplashMsg, Effect>(
        initState = Unit,
        initEffects = setOf(SplashEffect.CheckInit),
        coroutineScope = viewModelScope,
        reducer = Reducer,
        effectHandlers = listOf(
            CheckInitHandler(splashUseCase),
            navigationHandler,
        ),
    )

    @AssistedFactory
    interface Factory {
        fun create(): SplashViewModel
    }
}
