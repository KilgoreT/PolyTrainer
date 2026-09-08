package me.apomazkin.vocabulary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.VocabularyHostReducer
import me.apomazkin.vocabulary.logic.VocabularyHostState
import me.apomazkin.vocabulary.logic.VocabularyHostSubHandler
import me.apomazkin.vocabulary.logic.subscriptions

/**
 * VM host'а вкладок — точка сборки цикла mate: reducer и декларативная
 * подписка на текущий словарь (`subscriptions()` из state +
 * [VocabularyHostSubHandler]) на viewModelScope. Эффектов у host'а
 * нет. Factory без assisted-параметров — навигатора у host'а нет.
 */
class VocabularyHostViewModel @AssistedInject constructor(
    vocabularyHostSubHandler: VocabularyHostSubHandler,
) : ViewModel(), MateStateHolder<VocabularyHostState, Msg> {

    private val stateHolder = Mate(
        initState = VocabularyHostState(),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = VocabularyHostReducer(),
        effectHandlers = emptyList(),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(vocabularyHostSubHandler),
    )

    override val state: StateFlow<VocabularyHostState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): VocabularyHostViewModel
    }
}
