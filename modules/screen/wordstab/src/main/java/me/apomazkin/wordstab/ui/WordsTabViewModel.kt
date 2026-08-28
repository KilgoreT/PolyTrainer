package me.apomazkin.wordstab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.DatasourceEffectHandler
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.UiEffectHandler
import me.apomazkin.wordstab.logic.WordsTabReducer
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

class WordsTabViewModel @AssistedInject constructor(
    @Assisted navigator: WordsNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: WordsNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordsTabState, Msg> {

    private val stateHolder = Mate(
        initState = WordsTabState(),
        initEffects = setOf(DatasourceEffect.LoadTermFlow()),
        coroutineScope = viewModelScope,
        reducer = WordsTabReducer(logger = logger),
        effectHandlerSet = setOf(
            datasourceHandler,
            uiHandler,
            navHandlerFactory.create(navigator),
        )
    )

    override val state: StateFlow<WordsTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: WordsNavigator): WordsTabViewModel
    }
}
