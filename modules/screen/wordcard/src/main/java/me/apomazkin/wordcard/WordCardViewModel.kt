package me.apomazkin.wordcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.wordcard.deps.UiHost
import me.apomazkin.wordcard.mate.DatasourceEffect
import me.apomazkin.wordcard.mate.DatasourceEffectHandler
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.UiEffectHandler
import me.apomazkin.wordcard.mate.WordCardReducer
import me.apomazkin.wordcard.mate.WordCardState

class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    @Assisted uiHost: UiHost,
    datasourceHandler: DatasourceEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
    uiEffectHandlerFactory: UiEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = viewModelScope,
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(
            datasourceHandler,
            navHandlerFactory.create(navigator),
            uiEffectHandlerFactory.create(uiHost),
        )
    )

    override val state: StateFlow<WordCardState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(
            wordId: Long,
            navigator: WordCardNavigator,
            uiHost: UiHost,
        ): WordCardViewModel
    }
}
