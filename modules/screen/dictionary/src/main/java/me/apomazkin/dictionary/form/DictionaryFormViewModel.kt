package me.apomazkin.dictionary.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

class DictionaryFormViewModel(
    dictionaryUseCase: DictionaryUseCase,
    editingDictionaryId: Long? = null,
    onBack: () -> Unit,
) : ViewModel(), MateStateHolder<DictionaryFormScreenState, DictionaryFormMsg> {

    private val stateHolder = Mate(
        initState = DictionaryFormScreenState(
            editingDictionaryId = editingDictionaryId,
        ),
        initEffects = if (editingDictionaryId != null) {
            setOf(DictionaryFormEffect.LoadDictionary(editingDictionaryId))
        } else {
            emptySet()
        },
        coroutineScope = viewModelScope,
        reducer = DictionaryFormReducer(),
        effectHandlerSet = setOf(
            DictionaryFormEffectHandler(dictionaryUseCase),
            FlagFilterFlowHandler(dictionaryUseCase),
            NavigationEffectHandler(onBack),
        ),
    )

    override val state: StateFlow<DictionaryFormScreenState>
        get() = stateHolder.state

    override fun accept(message: DictionaryFormMsg) = stateHolder.accept(message)

    class Factory(
        private val dictionaryUseCase: DictionaryUseCase,
        private val editingDictionaryId: Long? = null,
        private val onBack: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DictionaryFormViewModel(
                dictionaryUseCase,
                editingDictionaryId,
                onBack,
            ) as T
        }
    }
}
