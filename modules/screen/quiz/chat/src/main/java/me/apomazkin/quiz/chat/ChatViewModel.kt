package me.apomazkin.quiz.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.quiz.QuizGame
import me.apomazkin.ui.resource.ResourceManager

/**
 * VM экрана квиз-чата: тонкая обёртка над [ChatAssembly] — сборка
 * раннера живёт там (общая с харнесом), VM даёт только viewModelScope
 * и продовые зависимости.
 */
class ChatViewModel @AssistedInject constructor(
    useCase: QuizChatUseCase,
    quizGame: QuizGame,
    prefsProvider: PrefsProvider,
    resourceManager: ResourceManager,
    logger: LexemeLogger,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<ChatScreenState, Msg> {

    private val stateHolder = ChatAssembly.create(
        useCase = useCase,
        quizGame = quizGame,
        prefsProvider = prefsProvider,
        resourceManager = resourceManager,
        logger = logger,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<ChatScreenState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    override fun onCleared() {
        super.onCleared()
        stateHolder.dispose()
    }

    @AssistedFactory
    interface Factory {
        fun create(): ChatViewModel
    }
}
