package me.apomazkin.quiz.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import me.apomazkin.quiz.chat.logic.ChatReducer
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.ChatSubHandler
import me.apomazkin.quiz.chat.logic.DatasourceEffect
import me.apomazkin.quiz.chat.logic.DatasourceEffectHandler
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.logic.subscriptions
import me.apomazkin.ui.resource.ResourceManager

class ChatViewModel @AssistedInject constructor(
    @Assisted navigator: ChatNavigator,
    resourceManager: ResourceManager,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    chatSubHandler: ChatSubHandler,
    navHandlerFactory: ChatNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<ChatScreenState, Msg> {

    private val stateHolder = Mate(
        initState = ChatScreenState(),
        initEffects = setOf(DatasourceEffect.PrepareToStart),
        coroutineScope = viewModelScope,
        reducer = ChatReducer(
            logger = logger,
            resourceManager = resourceManager,
        ),
        effectHandlers = listOf(
            datasourceHandler,
            navHandlerFactory.create(navigator),
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(chatSubHandler),
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
        fun create(navigator: ChatNavigator): ChatViewModel
    }
}
