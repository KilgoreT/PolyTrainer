package me.apomazkin.dictionary.list

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.ReducerResult

class DictionaryListReducer : MateReducer<DictionaryListScreenState, DictionaryListMsg, Effect> {
    override fun reduce(
        state: DictionaryListScreenState,
        message: DictionaryListMsg
    ): ReducerResult<DictionaryListScreenState, Effect> {
        return when (message) {
            is DictionaryListMsg.RequestBack -> {
                val effect: Effect = if (state.dictionaries.isEmpty()) {
                    ListNavigationEffect.ExitApp
                } else {
                    NavigationEffect.Back
                }
                state to setOf(effect)
            }

            is DictionaryListMsg.OpenNewDictionary -> state to setOf(
                ListNavigationEffect.OpenCreate
            )

            is DictionaryListMsg.RequestDelete -> state
                .showDeleteDialog(message.id, message.name) to emptySet()

            is DictionaryListMsg.ConfirmDelete -> {
                val id = state.deleteDialogState.dictionaryId
                state.hideDeleteDialog() to setOf(DictionaryListEffect.DeleteDictionary(id))
            }

            is DictionaryListMsg.DismissDelete -> state
                .hideDeleteDialog() to emptySet()

            is DictionaryListMsg.DictionariesLoaded -> state
                .stopLoading()
                .updateDictionaries(message.list) to emptySet()

            is DictionaryListMsg.DictionaryDeleted -> state to emptySet()

            is DictionaryListMsg.EditDictionary -> state to setOf(
                ListNavigationEffect.OpenEdit(message.id)
            )

            is DictionaryListMsg.Empty -> state to emptySet()
        }
    }
}
