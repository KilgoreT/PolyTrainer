package me.apomazkin.stattab

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineScope
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.stattab.deps.StatisticUseCase
import me.apomazkin.stattab.mate.Msg
import me.apomazkin.stattab.mate.StatSubHandler
import me.apomazkin.stattab.mate.StatisticReducer
import me.apomazkin.stattab.mate.StatisticState
import me.apomazkin.stattab.mate.subscriptions

/**
 * ЕДИНСТВЕННОЕ место сборки раннера вкладки статистики. Прод
 * ([StatisticViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный экран не может разойтись между ними; харнес
 * лишь подставляет стаб use case'а, свой nav-handler и наблюдателя
 * ленты.
 */
object StatisticAssembly {

    fun create(
        useCase: StatisticUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<StatisticState, Msg, Effect> = Mate(
        initState = StatisticState(),
        initEffects = setOf(),
        coroutineScope = coroutineScope,
        reducer = StatisticReducer(logger = logger),
        effectHandlers = listOf(
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            StatSubHandler(useCase = useCase),
        ),
        observers = observers,
    )
}
