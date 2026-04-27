package me.apomazkin.dictionary.list

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult

class DictionaryListReducer : MateReducer<DictionaryListScreenState, DictionaryListMsg, Effect> {
    override fun reduce(
        state: DictionaryListScreenState,
        message: DictionaryListMsg
    ): ReducerResult<DictionaryListScreenState, Effect> {
        return when (message) {
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

            is DictionaryListMsg.Empty -> state to emptySet()
        }
    }
}
