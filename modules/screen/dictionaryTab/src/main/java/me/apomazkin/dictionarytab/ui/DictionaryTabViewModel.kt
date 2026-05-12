package me.apomazkin.dictionarytab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionarytab.logic.DatasourceEffect
import me.apomazkin.dictionarytab.logic.DatasourceEffectHandler
import me.apomazkin.dictionarytab.logic.DictionaryTabState
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.dictionarytab.logic.UiEffectHandler
import me.apomazkin.dictionarytab.logic.VocabularyTabReducer
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

class DictionaryTabViewModel @AssistedInject constructor(
    @Assisted navigator: VocabularyNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: VocabularyNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<DictionaryTabState, Msg> {

    private val stateHolder = Mate(
        initState = DictionaryTabState(),
        initEffects = setOf(DatasourceEffect.LoadTermFlow()),
        coroutineScope = viewModelScope,
        reducer = VocabularyTabReducer(logger = logger),
        effectHandlerSet = setOf(
            datasourceHandler,
            uiHandler,
            navHandlerFactory.create(navigator),
        )
    )

    override val state: StateFlow<DictionaryTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: VocabularyNavigator): DictionaryTabViewModel
    }
}
