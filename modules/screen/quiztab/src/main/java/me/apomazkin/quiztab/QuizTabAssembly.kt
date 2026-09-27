package me.apomazkin.quiztab

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ErrorLoggingObserver
import me.apomazkin.quiztab.deps.QuizTabUseCase
import me.apomazkin.quiztab.logic.DatasourceEffectHandler
import me.apomazkin.quiztab.logic.Msg
import me.apomazkin.quiztab.logic.QuizTabReducer
import me.apomazkin.quiztab.logic.QuizTabState
import me.apomazkin.quiztab.logic.QuizTabSubHandler
import me.apomazkin.quiztab.logic.UiEffectHandler
import me.apomazkin.quiztab.logic.subscriptions

/**
 * ЕДИНСТВЕННОЕ место сборки раннера вкладки квизов. Прод
 * ([QuizTabViewModel]) и сценарный харнес зовут одну и ту же фабрику —
 * собранный экран не может разойтись между ними; харнес лишь
 * подставляет свой nav-handler и наблюдателя ленты.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object QuizTabAssembly {

    fun create(
        useCase: QuizTabUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<QuizTabState, Msg, Effect> = Mate(
        initState = QuizTabState(),
        initEffects = setOf(),
        coroutineScope = coroutineScope,
        reducer = QuizTabReducer(logger = logger),
        effectHandlers = listOf(
            UiEffectHandler(),
            DatasourceEffectHandler(useCase = useCase, logger = logger, io = io),
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            QuizTabSubHandler(useCase = useCase, logger = logger),
        ),
        // ErrorLoggingObserver — единственный источник стектрейсов
        // упавших эффектов/подписок: handler'ы ошибок не ловят.
        observers = observers + ErrorLoggingObserver(logger, LogTags.QUIZ),
    )
}
