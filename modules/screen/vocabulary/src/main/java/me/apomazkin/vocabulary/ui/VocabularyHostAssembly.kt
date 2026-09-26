package me.apomazkin.vocabulary.ui

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateObserver
import kotlinx.coroutines.CoroutineScope
import me.apomazkin.vocabulary.deps.VocabularyHostUseCase
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.VocabularyHostReducer
import me.apomazkin.vocabulary.logic.VocabularyHostState
import me.apomazkin.vocabulary.logic.VocabularyHostSubHandler
import me.apomazkin.vocabulary.logic.subscriptions

/**
 * ЕДИНСТВЕННОЕ место сборки раннера host'а вкладок. Прод
 * ([VocabularyHostViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный экран не может разойтись между ними; харнес
 * лишь подставляет стаб use case'а и наблюдателя ленты. Эффектов у
 * host'а нет — effectHandlers пуст.
 */
object VocabularyHostAssembly {
    fun create(
        useCase: VocabularyHostUseCase,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<VocabularyHostState, Msg, Effect> =
        Mate(
            initState = VocabularyHostState(),
            initEffects = emptySet(),
            coroutineScope = coroutineScope,
            reducer = VocabularyHostReducer(),
            effectHandlers = emptyList(),
            subscriptions = { it.subscriptions() },
            subscriptionHandlers = listOf(
                VocabularyHostSubHandler(useCase = useCase),
            ),
            observers = observers,
        )
}
