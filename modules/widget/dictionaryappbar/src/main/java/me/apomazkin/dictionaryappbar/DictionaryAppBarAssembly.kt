package me.apomazkin.dictionaryappbar

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionaryappbar.mate.DatasourceEffectHandler
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarReducer
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarState
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarSubHandler
import me.apomazkin.dictionaryappbar.mate.Msg
import me.apomazkin.dictionaryappbar.mate.subscriptions
import me.apomazkin.logger.LexemeLogger

/**
 * ЕДИНСТВЕННОЕ место сборки раннера виджета app bar со словарём. Прод
 * ([DictionaryAppBarViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный виджет не может разойтись между ними; харнес
 * лишь подставляет стаб use case'а, свой nav-handler и наблюдателя
 * ленты.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object DictionaryAppBarAssembly {

    fun create(
        useCase: DictionaryAppBarUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<DictionaryAppBarState, Msg, Effect> = Mate(
        initState = DictionaryAppBarState(),
        initEffects = setOf(),
        coroutineScope = coroutineScope,
        reducer = DictionaryAppBarReducer(logger = logger),
        effectHandlers = listOf(
            DatasourceEffectHandler(useCase = useCase, logger = logger, io = io),
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            DictionaryAppBarSubHandler(useCase = useCase),
        ),
        observers = observers,
    )
}
