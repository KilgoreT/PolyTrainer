package me.apomazkin.quiz.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.quiz.chat.logic.ChatReducer
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.DatasourceEffectHandler
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.resource.ResourceManager

class ChatViewModel(
    resourceManager: ResourceManager,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<ChatScreenState, Msg> {
    
    private val stateHolder = Mate(
        initState = ChatScreenState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = ChatReducer(
            resourceManager = resourceManager,
            logger = logger,
        ),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(),
        )
    )
    
    override val state: StateFlow<ChatScreenState>
        get() = stateHolder.state
    
    override fun accept(message: Msg) = stateHolder.accept(message)
    
    class Factory(
        //        private val quizTabUseCase: QuizTabUseCase,
        private val resourceManager: ResourceManager,
        private val logger: LexemeLogger,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(
                //                quizTabUseCase,
                resourceManager,
                logger,
            ) as T
        }
    }
}
