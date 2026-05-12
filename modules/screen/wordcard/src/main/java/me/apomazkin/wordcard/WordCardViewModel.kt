package me.apomazkin.wordcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.wordcard.mate.DatasourceEffect
import me.apomazkin.wordcard.mate.DatasourceEffectHandler
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.UiEffectHandler
import me.apomazkin.wordcard.mate.WordCardReducer
import me.apomazkin.wordcard.mate.WordCardState

class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = viewModelScope,
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(
            datasourceHandler,
            uiHandler,
            navHandlerFactory.create(navigator),
        )
    )

    override val state: StateFlow<WordCardState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(wordId: Long, navigator: WordCardNavigator): WordCardViewModel
    }
}
