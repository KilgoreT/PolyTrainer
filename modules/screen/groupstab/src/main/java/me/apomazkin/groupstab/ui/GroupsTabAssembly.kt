package me.apomazkin.groupstab.ui

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.groupstab.logic.DatasourceEffectHandler
import me.apomazkin.groupstab.logic.GroupsSubHandler
import me.apomazkin.groupstab.logic.GroupsTabReducer
import me.apomazkin.groupstab.logic.GroupsTabState
import me.apomazkin.groupstab.logic.Msg
import me.apomazkin.groupstab.logic.subscriptions
import me.apomazkin.logger.LexemeLogger

/**
 * ЕДИНСТВЕННОЕ место сборки раннера вкладки «Группы». Прод
 * ([GroupsTabViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный экран не может разойтись между ними; харнес
 * лишь подставляет стабы use case'ов, свой nav-handler и наблюдателя
 * ленты.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object GroupsTabAssembly {

    fun create(
        useCase: GroupsTabUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<GroupsTabState, Msg, Effect> = Mate(
        initState = GroupsTabState(),
        initEffects = emptySet(),
        coroutineScope = coroutineScope,
        reducer = GroupsTabReducer(logger = logger),
        effectHandlers = listOf(
            DatasourceEffectHandler(useCase = useCase, logger = logger, io = io),
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            GroupsSubHandler(useCase = useCase, logger = logger),
        ),
        observers = observers,
    )
}
