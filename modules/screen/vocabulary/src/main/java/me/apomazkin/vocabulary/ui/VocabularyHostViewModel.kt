package me.apomazkin.vocabulary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import me.apomazkin.vocabulary.logic.CurrentDictFlowHandler
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.VocabularyHostReducer
import me.apomazkin.vocabulary.logic.VocabularyHostState

/**
 * IS493: VM host'а вкладок. Э2 (D9.3): переезд на Dagger — появилась первая
 * зависимость (подписка на текущий словарь), исключение Э1 «VM вне
 * AppComponent» закрыто. Factory без assisted-параметров — навигатора у
 * host'а нет.
 */
class VocabularyHostViewModel @AssistedInject constructor(
    currentDictFlowHandler: CurrentDictFlowHandler,
) : ViewModel(), MateStateHolder<VocabularyHostState, Msg> {

    private val stateHolder = Mate(
        initState = VocabularyHostState(),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = VocabularyHostReducer(),
        effectHandlers = emptyList(),
        flowHandlers = listOf(currentDictFlowHandler),
    )

    override val state: StateFlow<VocabularyHostState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): VocabularyHostViewModel
    }
}
