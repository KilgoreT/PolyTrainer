package me.apomazkin.per_dictionary_components

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
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import me.apomazkin.per_dictionary_components.mate.DatasourceEffectHandler
import me.apomazkin.per_dictionary_components.mate.Msg
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsReducer
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsScreenState
import me.apomazkin.per_dictionary_components.mate.PerDictionaryComponentsSubHandler
import me.apomazkin.per_dictionary_components.mate.UiEffectHandler
import me.apomazkin.per_dictionary_components.mate.subscriptions

/**
 * ЕДИНСТВЕННОЕ место сборки раннера экрана пословарных компонентов.
 * Прод ([PerDictionaryComponentsViewModel]) и сценарный харнес зовут
 * одну и ту же фабрику — собранный экран не может разойтись между
 * ними; харнес лишь подставляет стаб use case'а, свой nav-handler и
 * наблюдателя ленты.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object PerDictionaryComponentsAssembly {
    fun create(
        useCase: PerDictionaryComponentsUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        dictionaryId: Long,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<PerDictionaryComponentsScreenState, Msg, Effect> =
        Mate(
            initState = PerDictionaryComponentsScreenState(
                dictionaryId = dictionaryId,
                isLoading = true,
            ),
            initEffects = emptySet(),
            coroutineScope = coroutineScope,
            reducer = PerDictionaryComponentsReducer(logger = logger),
            effectHandlers = listOf(
                DatasourceEffectHandler(useCase = useCase, logger = logger, io = io),
                UiEffectHandler(),
                navigationHandler,
            ),
            subscriptions = { it.subscriptions() },
            subscriptionHandlers = listOf(
                PerDictionaryComponentsSubHandler(useCase = useCase, logger = logger),
            ),
            observers = observers + ErrorLoggingObserver(logger, LogTags.DICT_COMPONENTS),
        )
}
