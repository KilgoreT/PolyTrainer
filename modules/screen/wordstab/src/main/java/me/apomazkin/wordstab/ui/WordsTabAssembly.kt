package me.apomazkin.wordstab.ui

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.DatasourceEffectHandler
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.UiEffectHandler
import me.apomazkin.wordstab.logic.WordsTabReducer
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.WordsTabSubHandler
import me.apomazkin.wordstab.logic.subscriptions

/**
 * ЕДИНСТВЕННОЕ место сборки раннера вкладки «Слова». Прод
 * ([WordsTabViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный экран не может разойтись между ними; харнес
 * лишь подставляет стаб use case'а, свой nav-handler и наблюдателя
 * ленты.
 *
 * @param pagingScope scope для `cachedIn` paging-потока без фильтра
 *   (переживает пересоздание подписчиков UI); в проде это
 *   viewModelScope.
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object WordsTabAssembly {

    fun create(
        useCase: WordsTabUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        pagingScope: CoroutineScope,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<WordsTabState, Msg, Effect> = Mate(
        initState = WordsTabState(),
        initEffects = setOf(DatasourceEffect.LoadTermFlow()),
        coroutineScope = coroutineScope,
        reducer = WordsTabReducer(logger = logger),
        effectHandlers = listOf(
            DatasourceEffectHandler(
                pagingScope = pagingScope,
                wordstabUseCase = useCase,
                logger = logger,
                io = io,
            ),
            UiEffectHandler(),
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            WordsTabSubHandler(useCase = useCase),
        ),
        observers = observers,
    )
}
