package me.apomazkin.splash

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.ReducerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

/**
 * ЕДИНСТВЕННОЕ место сборки мини-раннера сплэша. Прод
 * ([SplashViewModel]) и сценарный харнес зовут одну и ту же фабрику —
 * собранный экран не может разойтись между ними; харнес лишь
 * подставляет стаб use case'а и свой nav-handler.
 *
 * Цикл: init-эффект [SplashEffect.CheckInit] → ответ базы →
 * навигационный эффект ([SplashNavigationEffect]), который довозит
 * nav-handler.
 */
object SplashAssembly {

    fun create(
        useCase: SplashUseCase,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<Unit, SplashMsg, Effect> = Mate(
        initState = Unit,
        initEffects = setOf(SplashEffect.CheckInit),
        coroutineScope = coroutineScope,
        reducer = Reducer,
        effectHandlers = listOf(
            CheckInitHandler(useCase = useCase),
            navigationHandler,
        ),
        observers = observers,
    )

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
}
