package me.apomazkin.langpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.apomazkin.langpicker.entity.LangPresetUi
import me.apomazkin.langpicker.entity.LangUpdateUi

interface LangPickerUseCase {
    suspend fun getFlagRes(numericCode: Int): Int
    suspend fun addLang(numericCode: Int, name: String)
}

class LangPickerViewModel(
    private val langPickerUseCase: LangPickerUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<LangPickerState>(LangPickerState.LoadingState)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prepareData()
        }
    }

    private suspend fun prepareData() {
        LanguageData.langList.map {
            LangPresetUi(
                flagRes = langPickerUseCase.getFlagRes(it.numericCode),
                countryNumericCode = it.numericCode,
                langNameRes = it.numericCode.toLangNameRes()
            )
        }.also {
            _state.value = LangPickerState.PresetState(it)
        }
    }

    fun addNew(name: String) {
    }

    fun setupLang(lang: LangUpdateUi) {
        viewModelScope.launch(Dispatchers.IO) {
            langPickerUseCase.addLang(lang.countryNumericCode, lang.langName)
            when (val currentState = _state.value) {
                is LangPickerState.PresetState -> {
                    _state.value = currentState.copy(isSelected = true)
                }
                else -> {}
            }
        }
    }

    class Factory(
        private val langPickerUseCase: LangPickerUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LangPickerViewModel(langPickerUseCase) as T
        }
    }

}

sealed interface LangPickerState {
    object LoadingState : LangPickerState
    data class PresetState(
        val value: List<LangPresetUi>,
        val isSelected: Boolean = false
    ) : LangPickerState
}