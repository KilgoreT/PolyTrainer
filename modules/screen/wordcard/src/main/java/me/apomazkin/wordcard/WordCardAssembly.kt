package me.apomazkin.wordcard

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineScope
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.wordcard.deps.UiHost
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.mate.DatasourceEffect
import me.apomazkin.wordcard.mate.DatasourceEffectHandler
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.UiEffectHandler
import me.apomazkin.wordcard.mate.WordCardReducer
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordCardSubHandler
import me.apomazkin.wordcard.mate.subscriptions

/**
 * ЕДИНСТВЕННОЕ место сборки раннера карточки слова. Прод
 * ([WordCardViewModel]) и сценарный харнес зовут одну и ту же
 * фабрику — собранный экран не может разойтись между ними; харнес
 * лишь подставляет стабы use case'а/[UiHost], свой nav-handler и
 * наблюдателя ленты.
 */
object WordCardAssembly {

    fun create(
        useCase: WordCardUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        wordId: Long,
        uiHost: UiHost,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<WordCardState, Msg, Effect> = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = coroutineScope,
        reducer = WordCardReducer(logger),
        effectHandlers = listOf(
            DatasourceEffectHandler(
                wordCardUseCase = useCase,
                logger = logger,
            ),
            navigationHandler,
            UiEffectHandler(uiHost = uiHost),
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            WordCardSubHandler(
                useCase = useCase,
                logger = logger,
            ),
        ),
        observers = observers,
    )
}
