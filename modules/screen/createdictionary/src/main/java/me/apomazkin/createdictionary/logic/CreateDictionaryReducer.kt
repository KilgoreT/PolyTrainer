package me.apomazkin.createdictionary.logic

import android.util.Log
import me.apomazkin.createdictionary.entity.PresetLangUi
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
            is Msg.ShowLangList -> state.loadLang(message.list)
            is Msg.SelectLang -> state.selectLang(message.numericCode)
            is Msg.SaveLang -> state.saveLang(message.numericCode, message.langName)
            is Msg.Close -> state.closeScreen()
            Msg.Empty -> state to emptySet()
        }.also {
            Log.d("##MATE##", "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                Log.d("##MATE##", "Reduce --toEffect--: $effect ")
            }
        }
    }

    private fun CreateDictionaryState.loadLang(
        list: List<PresetLangUi>
    ): Pair<CreateDictionaryState, Set<Effect>> {
        return this.copy(
            isLoading = false,
            langState = langState.copy(
                langList = list
            )
        ) to setOf()
    }

    private fun CreateDictionaryState.selectLang(
        numericCode: Int,
    ): Pair<CreateDictionaryState, Set<Effect>> {
        return copy(
            langState = langState.copy(
                selectedNumericCode = if (langState.selectedNumericCode == numericCode) null
                else numericCode,
                addLangButtonEnable = langState.selectedNumericCode != numericCode,
            )
        ) to setOf()
    }

    private fun CreateDictionaryState.saveLang(
        numericCode: Int,
        langName: String
    ): Pair<CreateDictionaryState, Set<Effect>> =
        copy(
            langState = langState.copy(
                addLangButtonEnable = false
            )
        ) to setOf(
            DatasourceEffect.SaveLangList(numericCode, langName)
        )

    private fun CreateDictionaryState.closeScreen(): Pair<CreateDictionaryState, Set<Effect>> =
        copy(needClose = true) to setOf()
}
