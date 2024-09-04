package me.apomazkin.createdictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.createdictionary.logic.CreateDictionaryReducer
import me.apomazkin.createdictionary.logic.CreateDictionaryState
import me.apomazkin.createdictionary.logic.DatasourceEffect
import me.apomazkin.createdictionary.logic.DatasourceEffectHandler
import me.apomazkin.createdictionary.logic.Msg
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

interface CreateDictionaryUseCase {
    suspend fun getFlagRes(numericCode: Int): Int
    suspend fun addLang(numericCode: Int, name: String): Long
    suspend fun saveCurrentLang(numericCode: Int)
}

class CreateDictionaryViewModel(
    createDictionaryUseCase: CreateDictionaryUseCase,
) : ViewModel(), MateStateHolder<CreateDictionaryState, Msg> {

    private val stateHolder = Mate(
        initState = CreateDictionaryState(),
        initEffects = setOf(DatasourceEffect.LoadLangList),
        coroutineScope = viewModelScope,
        reducer = CreateDictionaryReducer(),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(createDictionaryUseCase = createDictionaryUseCase),
        )
    )

    override val state: StateFlow<CreateDictionaryState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
        private val createDictionaryUseCase: CreateDictionaryUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateDictionaryViewModel(createDictionaryUseCase) as T
        }
    }

}