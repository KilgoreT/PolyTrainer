package me.apomazkin.dictionarytab.logic

import me.apomazkin.dictionarytab.logic.DatasourceEffect.AddWord
import me.apomazkin.dictionarytab.logic.DatasourceEffect.ChangeWord
import me.apomazkin.dictionarytab.logic.DatasourceEffect.DeleteWord
import me.apomazkin.dictionarytab.logic.DatasourceEffect.LoadTermFlow
import me.apomazkin.dictionarytab.logic.processor.processUiMessage
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.ui.logger.LexemeLogger

internal class VocabularyTabReducer(
        val logger: LexemeLogger,
) : MateReducer<DictionaryTabState, Msg, Effect> {
    override fun reduce(
            state: DictionaryTabState,
            message: Msg,
    ): ReducerResult<DictionaryTabState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {

            is Msg.ChangeDict -> state to setOf(LoadTermFlow())

            is Msg.TermDataLoaded -> state
                    .hideLoading()
                    .appendTermsFlow(
                            pattern = message.pattern,
                            termsFlow = message.termList,
                    ) to emptySet()

            is Msg.ShowActionMode -> state
                    .modifySelectedSet(message.targetWord)
                    .highlightWord(message.targetWord)
                    .showActionMode() to setOf()

            is Msg.HideActionMode -> state
                    .clearHighlighted()
                    .clearSelectedSet()
                    .hideActionMode() to setOf()

            is Msg.ModifySelectedInActionMode -> state
                    .modifySelectedSet(message.targetWord)
                    .highlightWord(message.targetWord)
                    .checkActionMode() to setOf()

            is Msg.ShowAddWordDialog -> state
                    .showAddWordDialog(
                            wordValue = message.wordValue,
                            wordId = null,
                    ) to setOf()

            is Msg.HideAddWordDialog -> state
                    .hideAddWordDialog()
                    .toDefaultTermsFlow() to setOf()

            is Msg.StartChangeWord -> state
                    .clearHighlighted()
                    .clearSelectedSet()
                    .hideActionMode()
                    .showAddWordDialog(
                            wordId = message.wordId,
                            wordValue = message.wordValue,
                    ) to setOf()

            is Msg.WordValueChange -> state.choose(
                    check = { it.addWordDialogState.isOpen },
                    yes = {
                        //TODO kilg 31.05.2025 01:30 добавить условие,
                        // что если "" уже есть, и паттерн "" -
                        // то не надо блять отправлять эффект создания потока
                        // https://github.com/KilgoreT/PolyTrainer/issues/377
                        it
                                .updateWordValue(message.value)
                                .retainDefaultAndCurrentFlow(message.value) to setOf(LoadTermFlow(message.value))
                    },
                    no = { it to emptySet() }
            )

            is Msg.AddWord -> state
                    .hideAddWordDialog()
                    .toDefaultTermsFlow() to setOf(AddWord(message.value))

            is Msg.ChangeWord -> state
                    .clearHighlighted()
                    .clearSelectedSet()
                    .hideActionMode()
                    .hideAddWordDialog()
                    .toDefaultTermsFlow() to setOf(
                    ChangeWord(
                            wordId = message.wordId,
                            value = message.value,
                    )
            )


            is Msg.ShowConfirmDeleteWordDialog -> state
                    .showConfirmDeleteDialog(message.wordIds) to setOf()

            is Msg.HideConfirmDeleteWordDialog -> state
                    .hideConfirmDeleteDialog() to setOf()

            is Msg.DeleteWord -> state
                    .hideConfirmDeleteDialog()
                    .clearHighlighted()
                    .clearSelectedSet()
                    .hideActionMode() to setOf(
                    DeleteWord(wordSet = message.wordIds),
                    UiEffect.ShowSnackbar(
                            title = "Слово удалено"
                    )
            )

            is UiMsg -> processUiMessage(state, message)
            Msg.Empty -> state to emptySet()
        }.also {
            logger.log(message = "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }
}