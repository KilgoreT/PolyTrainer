package me.apomazkin.settingstab

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import me.apomazkin.settingstab.logic.DatasourceEffectHandler
import me.apomazkin.settingstab.logic.Msg
import me.apomazkin.settingstab.logic.SettingsTabReducer
import me.apomazkin.settingstab.logic.SettingsTabState
import me.apomazkin.settingstab.logic.UiEffectHandler

/**
 * ЕДИНСТВЕННОЕ место сборки раннера вкладки настроек. Прод
 * ([SettingsTabViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику; харнес подставляет стабы use case'ов, свой nav-handler,
 * наблюдателя ленты и тестовый диспатчер.
 */
object SettingsTabAssembly {

    /**
     * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
     *   в тестах можно подставить тестовый.
     */
    fun create(
        useCase: SettingsTabUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<SettingsTabState, Msg, Effect> = Mate(
        initState = SettingsTabState(),
        initEffects = setOf(),
        coroutineScope = coroutineScope,
        reducer = SettingsTabReducer(logger = logger),
        effectHandlers = listOf(
            DatasourceEffectHandler(
                settingsTabUseCase = useCase,
                logger = logger,
                io = io,
            ),
            UiEffectHandler(),
            navigationHandler,
        ),
        observers = observers,
    )
}
