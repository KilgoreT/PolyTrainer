package me.apomazkin.quiztab

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineScope
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.quiztab.logic.Msg
import me.apomazkin.quiztab.logic.QuizTabReducer
import me.apomazkin.quiztab.logic.QuizTabState
import me.apomazkin.quiztab.logic.UiEffectHandler

/**
 * ЕДИНСТВЕННОЕ место сборки раннера вкладки квизов. Прод
 * ([QuizTabViewModel]) и сценарный харнес зовут одну и ту же фабрику —
 * собранный экран не может разойтись между ними; харнес лишь
 * подставляет свой nav-handler и наблюдателя ленты.
 */
object QuizTabAssembly {

    fun create(
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<QuizTabState, Msg, Effect> = Mate(
        initState = QuizTabState(),
        initEffects = setOf(),
        coroutineScope = coroutineScope,
        reducer = QuizTabReducer(logger = logger),
        effectHandlers = listOf(
            UiEffectHandler(),
            navigationHandler,
        ),
        observers = observers,
    )
}
