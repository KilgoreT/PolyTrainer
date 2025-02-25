package me.apomazkin.quiz.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.logic.ChatReducer
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.DatasourceEffect
import me.apomazkin.quiz.chat.logic.DatasourceEffectHandler
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.quiz.QuizGameImpl
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.resource.ResourceManager

class ChatViewModel(
    quizChatUseCase: QuizChatUseCase,
    resourceManager: ResourceManager,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<ChatScreenState, Msg> {
    
    private val stateHolder = Mate(
        initState = ChatScreenState(),
        initEffects = setOf(DatasourceEffect.PrepareToStart),
        coroutineScope = viewModelScope,
        reducer = ChatReducer(
            logger = logger,
            resourceManager = resourceManager,
        ),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(
                quizGame = QuizGameImpl(
                    quizChatUseCase = quizChatUseCase,
                    resourceManager = resourceManager,
                ),
                resourceManager = resourceManager,
            ),
        )
    )
    
    override val state: StateFlow<ChatScreenState>
        get() = stateHolder.state
    
    override fun accept(message: Msg) = stateHolder.accept(message)
    
    class Factory(
        private val quizChatUseCase: QuizChatUseCase,
        private val resourceManager: ResourceManager,
        private val logger: LexemeLogger,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(
                quizChatUseCase,
                resourceManager,
                logger,
            ) as T
        }
    }
}
