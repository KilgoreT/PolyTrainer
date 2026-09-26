package me.apomazkin.dictionary.form

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
 * ЕДИНСТВЕННОЕ место сборки раннера формы словаря. Прод
 * ([DictionaryFormViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный экран не может разойтись между ними; харнес
 * лишь подставляет стаб use case'а, свой nav-handler и наблюдателя
 * ленты.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object DictionaryFormAssembly {

    fun create(
        useCase: DictionaryUseCase,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        editingDictionaryId: Long?,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<DictionaryFormScreenState, DictionaryFormMsg, Effect> = Mate(
        initState = DictionaryFormScreenState(
            editingDictionaryId = editingDictionaryId,
        ),
        initEffects = if (editingDictionaryId != null) {
            setOf(DictionaryFormEffect.LoadDictionary(editingDictionaryId))
        } else {
            emptySet()
        },
        coroutineScope = coroutineScope,
        reducer = DictionaryFormReducer(),
        effectHandlers = listOf(
            DictionaryFormEffectHandler(dictionaryUseCase = useCase, io = io),
            FlagFilterEffectHandler(dictionaryUseCase = useCase),
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            DictionaryFormSubHandler(dictionaryUseCase = useCase),
        ),
        observers = observers,
    )
}
