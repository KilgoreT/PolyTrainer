package me.apomazkin.dictionarytab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.logic.DatasourceEffectHandler
import me.apomazkin.dictionarytab.logic.DictionaryTabState
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.dictionarytab.logic.UiEffectHandler
import me.apomazkin.dictionarytab.logic.VocabularyTabReducer
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.ui.logger.LexemeLogger

class DictionaryTabViewModel(
    dictionaryTabUseCase: DictionaryTabUseCase,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<DictionaryTabState, Msg> {

    private val stateHolder = Mate(
        initState = DictionaryTabState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = VocabularyTabReducer(logger = logger),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(
                    dictionaryTabUseCase = dictionaryTabUseCase,
                    scope = viewModelScope,
            ),
            UiEffectHandler()
        )
    )
    
    override val state: StateFlow<DictionaryTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
        private val dictionaryTabUseCase: DictionaryTabUseCase,
        private val logger: LexemeLogger,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DictionaryTabViewModel(dictionaryTabUseCase, logger) as T
        }
    }
}
