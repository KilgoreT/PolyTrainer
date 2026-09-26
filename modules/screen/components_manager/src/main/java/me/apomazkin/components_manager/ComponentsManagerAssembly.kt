package me.apomazkin.components_manager

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.components_manager.LogTags
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.components_manager.mate.ComponentsManagerReducer
import me.apomazkin.components_manager.mate.ComponentsManagerScreenState
import me.apomazkin.components_manager.mate.ComponentsManagerSubHandler
import me.apomazkin.components_manager.mate.DatasourceEffectHandler
import me.apomazkin.components_manager.mate.Msg
import me.apomazkin.components_manager.mate.UiEffectHandler
import me.apomazkin.components_manager.mate.subscriptions
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ErrorLoggingObserver

/**
 * ЕДИНСТВЕННОЕ место сборки раннера экрана менеджера компонентов. Прод
 * ([ComponentsManagerViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный экран не может разойтись между ними; харнес
 * лишь подставляет стаб use case'а, свой nav-handler и наблюдателя
 * ленты.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object ComponentsManagerAssembly {
    fun create(
        useCase: ComponentsManagerUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<ComponentsManagerScreenState, Msg, Effect> =
        Mate(
            initState = ComponentsManagerScreenState(isLoading = true),
            initEffects = emptySet(),
            coroutineScope = coroutineScope,
            reducer = ComponentsManagerReducer(logger = logger),
            effectHandlers = listOf(
                DatasourceEffectHandler(useCase = useCase, io = io),
                UiEffectHandler(),
                navigationHandler,
            ),
            subscriptions = { it.subscriptions() },
            subscriptionHandlers = listOf(
                ComponentsManagerSubHandler(useCase = useCase, logger = logger),
            ),
            observers = observers + ErrorLoggingObserver(logger, LogTags.ALL_COMPONENTS),
        )
}
