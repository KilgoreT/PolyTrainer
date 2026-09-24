package me.apomazkin.dictionary.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.dictionary.DictionaryUseCase

/**
 * VM формы словаря: тонкая обёртка над [DictionaryFormAssembly] —
 * сборка раннера живёт там (общая с харнесом), VM даёт только
 * viewModelScope и продовые зависимости.
 */
class DictionaryFormViewModel @AssistedInject constructor(
    @Assisted editingDictionaryId: Long?,
    useCase: DictionaryUseCase,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<DictionaryFormScreenState, DictionaryFormMsg> {

    private val stateHolder = DictionaryFormAssembly.create(
        useCase = useCase,
        navigationHandler = navigationHandler,
        editingDictionaryId = editingDictionaryId,
        coroutineScope = viewModelScope,
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
