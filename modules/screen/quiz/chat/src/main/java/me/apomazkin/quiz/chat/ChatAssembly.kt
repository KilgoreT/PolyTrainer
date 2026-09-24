package me.apomazkin.quiz.chat

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.logic.ChatReducer
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.ChatSubHandler
import me.apomazkin.quiz.chat.logic.DatasourceEffect
import me.apomazkin.quiz.chat.logic.DatasourceEffectHandler
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.logic.subscriptions
import me.apomazkin.quiz.chat.quiz.QuizGame
import me.apomazkin.ui.resource.ResourceManager

/**
 * ЕДИНСТВЕННОЕ место сборки раннера экрана квиз-чата. Прод
 * ([ChatViewModel]) и сценарный харнес зовут одну и ту же фабрику —
 * собранный экран не может разойтись между ними; харнес лишь
 * подставляет стабы use case'а/игры/prefs, свой nav-handler и
 * наблюдателя ленты.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
object ChatAssembly {

    fun create(
        useCase: QuizChatUseCase,
        quizGame: QuizGame,
        prefsProvider: PrefsProvider,
        resourceManager: ResourceManager,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<ChatScreenState, Msg, Effect> = Mate(
        initState = ChatScreenState(),
        initEffects = setOf(DatasourceEffect.PrepareToStart),
        coroutineScope = coroutineScope,
        reducer = ChatReducer(
            logger = logger,
            resourceManager = resourceManager,
        ),
        effectHandlers = listOf(
            DatasourceEffectHandler(
                quizGame = quizGame,
                prefsProvider = prefsProvider,
                useCase = useCase,
                logger = logger,
                io = io,
            ),
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(
            ChatSubHandler(
                useCase = useCase,
                prefsProvider = prefsProvider,
            ),
        ),
        observers = observers,
    )
}
