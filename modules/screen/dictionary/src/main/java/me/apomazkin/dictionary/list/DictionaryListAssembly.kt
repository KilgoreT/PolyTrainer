package me.apomazkin.dictionary.list

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.dictionary.DictionaryUseCase

/**
 * ЕДИНСТВЕННОЕ место сборки раннера списка словарей. Прод
 * ([DictionaryListViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный экран не может разойтись между ними; харнес
 * лишь подставляет стаб use case'а, свой nav-handler и наблюдателя
 * ленты.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object DictionaryListAssembly {

    fun create(
        useCase: DictionaryUseCase,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<DictionaryListScreenState, DictionaryListMsg, Effect> = Mate(
        initState = DictionaryListScreenState(),
        initEffects = emptySet(),
        coroutineScope = coroutineScope,
        reducer = DictionaryListReducer(),
        effectHandlers = listOf(
            DictionaryListEffectHandler(dictionaryUseCase = useCase, io = io),
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            DictionaryListSubHandler(dictionaryUseCase = useCase),
        ),
        observers = observers,
    )
}
