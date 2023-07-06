package me.apomazkin.langpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.langpicker.logic.*
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

interface LangPickerUseCase {
    suspend fun getFlagRes(numericCode: Int): Int
    suspend fun addLang(numericCode: Int, name: String): Long
    suspend fun saveCurrentLang(numericCode: Int)
}

class LangPickerViewModel(
    langPickerUseCase: LangPickerUseCase,
) : ViewModel(), MateStateHolder<LangPickerState, Msg> {

    private val stateHolder = Mate(
        initState = LangPickerState(),
        initEffects = setOf(DatasourceEffect.LoadLangList),
        coroutineScope = viewModelScope,
        reducer = LangPickerReducer(),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(langPickerUseCase = langPickerUseCase),
        )
    )

    override val state: StateFlow<LangPickerState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
        private val langPickerUseCase: LangPickerUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LangPickerViewModel(langPickerUseCase) as T
        }
    }

}