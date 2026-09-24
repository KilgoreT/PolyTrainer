package me.apomazkin.vocabulary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.MateStateHolder
import me.apomazkin.vocabulary.deps.VocabularyHostUseCase
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.VocabularyHostState

/**
 * VM host'а вкладок: тонкая обёртка над [VocabularyHostAssembly] —
 * сборка раннера живёт там (общая с харнесом), VM даёт только
 * viewModelScope и продовые зависимости. Factory без
 * assisted-параметров — навигатора у host'а нет.
 */
class VocabularyHostViewModel @AssistedInject constructor(
    useCase: VocabularyHostUseCase,
) : ViewModel(), MateStateHolder<VocabularyHostState, Msg> {

    private val stateHolder = VocabularyHostAssembly.create(
        useCase = useCase,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<VocabularyHostState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): VocabularyHostViewModel
    }
}
