package me.apomazkin.dictionary.logic

import android.util.Log
import me.apomazkin.dictionary.entity.PresetDictionaryUi
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult

internal class DictionaryReducer : MateReducer<DictionaryState, Msg, Effect> {
    override fun reduce(
        state: DictionaryState,
        message: Msg
    ): ReducerResult<DictionaryState, Effect> {
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

    private fun DictionaryState.loadDictionary(
        list: List<PresetDictionaryUi>
    ): Pair<DictionaryState, Set<Effect>> {
        return this.copy(
            isLoading = false,
            dictionarySelectionState = dictionarySelectionState.copy(
                dictionaryList = list
            )
        ) to setOf()
    }

    private fun DictionaryState.selectDictionary(
        numericCode: Int,
    ): Pair<DictionaryState, Set<Effect>> {
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

    private fun DictionaryState.saveDictionary(
        numericCode: Int,
        dictionaryName: String
    ): Pair<DictionaryState, Set<Effect>> =
        copy(
            dictionarySelectionState = dictionarySelectionState.copy(
                addDictionaryButtonEnable = false
            )
        ) to setOf(
            DatasourceEffect.SaveDictionaryList(numericCode, dictionaryName)
        )

    private fun DictionaryState.closeScreen(): Pair<DictionaryState, Set<Effect>> =
        copy(needClose = true) to setOf()
}
