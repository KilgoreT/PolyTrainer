package me.apomazkin.dictionary.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * VM формы словаря — точка сборки цикла mate. Навигация идёт через
 * shared nav-handler приложения, per-экранного navigator'а нет.
 */
class DictionaryFormViewModel @AssistedInject constructor(
    @Assisted editingDictionaryId: Long?,
    datasourceHandler: DictionaryFormEffectHandler,
    flagFilterHandler: FlagFilterEffectHandler,
    formSubHandler: DictionaryFormSubHandler,
    navigationHandler: MateNavigationHandler,
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
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(formSubHandler),
    )

    override val state: StateFlow<DictionaryFormScreenState>
        get() = stateHolder.state

    override fun accept(message: DictionaryFormMsg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(
            editingDictionaryId: Long?,
        ): DictionaryFormViewModel
    }
}
