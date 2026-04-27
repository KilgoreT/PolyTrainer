package me.apomazkin.dictionary.form

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult

class DictionaryFormReducer : MateReducer<DictionaryFormScreenState, DictionaryFormMsg, Effect> {
    override fun reduce(
        state: DictionaryFormScreenState,
        message: DictionaryFormMsg
    ): ReducerResult<DictionaryFormScreenState, Effect> {
        return when (message) {
            is DictionaryFormMsg.NameChanged -> state
                .updateName(message.value) to emptySet()

            is DictionaryFormMsg.ToggleLanguageBound -> state
                .toggleLanguageBound() to emptySet()

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

            is DictionaryFormMsg.OpenLanguagePicker -> state
                .showLanguagePicker() to emptySet()

            is DictionaryFormMsg.CloseLanguagePicker -> state
                .hideLanguagePicker() to emptySet()

            is DictionaryFormMsg.LanguageQueryChanged -> state
                .filterLanguages(message.query) to emptySet()

            is DictionaryFormMsg.SelectLanguage -> state
                .selectLanguage(message.item) to setOf(
                    DictionaryFormEffect.LoadFlagsForLanguage(message.item.code)
                )

            is DictionaryFormMsg.SelectFlag -> state
                .selectFlag(message.item) to emptySet()

            is DictionaryFormMsg.LanguagesLoaded -> state
                .setLanguages(message.list) to emptySet()

            is DictionaryFormMsg.FlagsLoaded -> state
                .updateFlags(message.list) to emptySet()

            is DictionaryFormMsg.DictionarySaved -> state
                .close() to emptySet()

            is DictionaryFormMsg.Empty -> state to emptySet()
        }
    }
}
