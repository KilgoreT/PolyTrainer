package me.apomazkin.createdictionary.logic

import android.util.Log
import me.apomazkin.createdictionary.entity.PresetDictionaryUi
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult

internal class CreateDictionaryReducer : MateReducer<CreateDictionaryState, Msg, Effect> {
    override fun reduce(
        state: CreateDictionaryState,
        message: Msg
    ): ReducerResult<CreateDictionaryState, Effect> {
        Log.d("##MATE##", "Reduce --prevState--: $state ")
        Log.d("##MATE##", "Reduce ---message---: $message ")
        return when (message) {
            is Msg.ShowDictionaryList -> state.loadDictionary(message.list)
            is Msg.SelectDictionary -> state.selectDictionary(message.numericCode)
            is Msg.SaveDictionary -> state.saveDictionary(
                message.numericCode,
                message.dictionaryName
            )
            is Msg.Close -> state.closeScreen()
            Msg.Empty -> state to emptySet()
        }.also {
            Log.d("##MATE##", "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                Log.d("##MATE##", "Reduce --toEffect--: $effect ")
            }
        }
    }

    private fun CreateDictionaryState.loadDictionary(
        list: List<PresetDictionaryUi>
    ): Pair<CreateDictionaryState, Set<Effect>> {
        return this.copy(
            isLoading = false,
            dictionarySelectionState = dictionarySelectionState.copy(
                dictionaryList = list
            )
        ) to setOf()
    }

    private fun CreateDictionaryState.selectDictionary(
        numericCode: Int,
    ): Pair<CreateDictionaryState, Set<Effect>> {
        return copy(
            dictionarySelectionState = dictionarySelectionState.copy(
                selectedNumericCode =
                    if (dictionarySelectionState.selectedNumericCode == numericCode) null
                    else numericCode,
                addDictionaryButtonEnable =
                    dictionarySelectionState.selectedNumericCode != numericCode,
            )
        ) to setOf()
    }

    private fun CreateDictionaryState.saveDictionary(
        numericCode: Int,
        dictionaryName: String
    ): Pair<CreateDictionaryState, Set<Effect>> =
        copy(
            dictionarySelectionState = dictionarySelectionState.copy(
                addDictionaryButtonEnable = false
            )
        ) to setOf(
            DatasourceEffect.SaveDictionaryList(numericCode, dictionaryName)
        )

    private fun CreateDictionaryState.closeScreen(): Pair<CreateDictionaryState, Set<Effect>> =
        copy(needClose = true) to setOf()
}
