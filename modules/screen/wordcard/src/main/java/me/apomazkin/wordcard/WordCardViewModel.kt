package me.apomazkin.wordcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.mate.*

class WordCardViewModel(
    wordId: Long,
    wordCardUseCase: WordCardUseCase,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = viewModelScope,
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(wordCardUseCase = wordCardUseCase),
            UiEffectHandler()
        )
    )

    override val state: StateFlow<WordCardState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
        private val wordId: Long,
        private val wordCardUseCase: WordCardUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WordCardViewModel(wordId, wordCardUseCase) as T
        }
    }
}