package me.apomazkin.dictionary.form

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.ReducerResult

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
                            learningLanguage = state.learningLanguage.tag,
                            translationLanguage = state.translationLanguage.tag,
                        )
                    )
                } else {
                    state to setOf(
                        DictionaryFormEffect.SaveDictionary(
                            name = state.name,
                            numericCode = numericCode,
                            learningLanguage = state.learningLanguage.tag,
                            translationLanguage = state.translationLanguage.tag,
                        )
                    )
                }
            }

            is DictionaryFormMsg.Back -> state to setOf(NavigationEffect.Back)

            is DictionaryFormMsg.OpenLanguagePicker -> state
                .openLanguagePicker(message.target) to emptySet()

            is DictionaryFormMsg.CloseLanguagePicker -> state
                .closeLanguagePicker() to emptySet()

            is DictionaryFormMsg.LanguageQueryChanged -> state
                .updateLanguageQuery(message.query) to emptySet()

            is DictionaryFormMsg.SelectLanguage -> state
                .chooseLanguage(message.item) to emptySet()

            is DictionaryFormMsg.FlagsUpdated -> state
                .updateFlags(message.list) to emptySet()

            is DictionaryFormMsg.LanguagesLoaded -> state
                .applyAllLanguages(message.all) to emptySet()

            is DictionaryFormMsg.DictionaryLoaded -> state
                .prefillForEdit(
                    name = message.name,
                    flag = message.flag,
                    learningLanguage = message.learningLanguage,
                    translationLanguage = message.translationLanguage,
                ) to emptySet()

            is DictionaryFormMsg.DictionarySaved -> state to setOf(NavigationEffect.Back)

            is DictionaryFormMsg.Empty -> state to emptySet()
        }
    }
}
