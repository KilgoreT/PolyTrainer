package me.apomazkin.dictionary.form

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.ReducerResult

class DictionaryFormReducer : MateReducer<DictionaryFormScreenState, DictionaryFormMsg, Effect> {
    override fun reduce(
        state: DictionaryFormScreenState,
        message: DictionaryFormMsg
    ): ReducerResult<DictionaryFormScreenState, Effect> {
        return when (message) {
            is DictionaryFormMsg.NameChanged -> state
                .updateName(message.value) to emptySet()

            is DictionaryFormMsg.FlagFilterChanged -> state
                .updateFlagFilter(message.query) to setOf(
                    FlagFilterEffect.FilterFlags(message.query)
                )

            is DictionaryFormMsg.SelectFlag -> {
                if (message.item == state.selectedFlag) {
                    state.deselectFlag() to emptySet()
                } else {
                    state.selectFlag(message.item) to emptySet()
                }
            }

            is DictionaryFormMsg.Save -> {
                val numericCode = state.selectedFlag?.numericCode
                if (state.editingDictionaryId != null) {
                    state to setOf(
                        DictionaryFormEffect.UpdateDictionary(
                            id = state.editingDictionaryId,
                            name = state.name,
                            numericCode = numericCode,
                        )
                    )
                } else {
                    state to setOf(
                        DictionaryFormEffect.SaveDictionary(
                            name = state.name,
                            numericCode = numericCode,
                        )
                    )
                }
            }

            is DictionaryFormMsg.Back -> state to setOf(NavigationEffect.Back)

            is DictionaryFormMsg.FlagsUpdated -> state
                .updateFlags(message.list) to emptySet()

            is DictionaryFormMsg.DictionaryLoaded -> state
                .prefillForEdit(message.name, message.flag) to emptySet()

            is DictionaryFormMsg.DictionarySaved -> state to setOf(NavigationEffect.Back)

            is DictionaryFormMsg.Empty -> state to emptySet()
        }
    }
}
