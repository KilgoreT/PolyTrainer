package me.apomazkin.dictionary.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder

class DictionaryFormViewModel @AssistedInject constructor(
    @Assisted editingDictionaryId: Long?,
    @Assisted navigator: FormNavigator,
    datasourceHandler: DictionaryFormEffectHandler,
    flagFilterHandler: FlagFilterFlowHandler,
    navHandlerFactory: FormNavigationEffectHandler.Factory,
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
        effectHandlers = listOf(
            datasourceHandler,
            flagFilterHandler,
            navHandlerFactory.create(navigator),
        ),
        flowHandlers = listOf(flagFilterHandler),
    )

    override val state: StateFlow<DictionaryFormScreenState>
        get() = stateHolder.state

    override fun accept(message: DictionaryFormMsg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(
            editingDictionaryId: Long?,
            navigator: FormNavigator,
        ): DictionaryFormViewModel
    }
}
