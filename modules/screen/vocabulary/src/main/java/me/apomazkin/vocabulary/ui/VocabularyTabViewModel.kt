package me.apomazkin.vocabulary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.vocabulary.deps.VocabularyUseCase
import me.apomazkin.vocabulary.logic.*

class VocabularyTabViewModel(
    vocabularyUseCase: VocabularyUseCase,
) : ViewModel(), MateStateHolder<VocabularyTabState, Msg> {

    private val stateHolder = Mate(
        initState = VocabularyTabState(),
        initEffects = setOf(DatasourceEffect.LoadLangList),
        coroutineScope = viewModelScope,
        reducer = VocabularyTabReducer(),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(vocabularyUseCase = vocabularyUseCase),
            UiEffectHandler()
        )
    )

    override val state: StateFlow<VocabularyTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
        private val vocabularyUseCase: VocabularyUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VocabularyTabViewModel(vocabularyUseCase) as T
        }
    }
}
