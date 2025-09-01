package me.apomazkin.wordcard.mate

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.wordcard.mate.DatasourceEffect.LoadWord

class WordCardReducer : MateReducer<WordCardState, Msg, Effect> {
    override fun reduce(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
        return when (message) {
            is Msg.LoadingWord -> state
                .showLoading() to setOf(LoadWord(wordId = state.wordState.id))

            is Msg.WordLoaded -> state
                .hideLoading()
                .setTerm(term = message.term)
                .setLexemeList(lexemes = message.term.lexemeList.map { it.toLexemeState() }) to setOf()

            is Msg.WordNotFound -> TODO("WordNotFound is not implemented")

            is Msg.OpenTopBarMenu -> state
                .showMenu() to setOf()

            is Msg.CloseTopBarMenu -> state
                .hideMenu() to setOf()

            is Msg.OpenDeleteWordDialog -> state
                .showWordWarningDialog() to setOf()

            is Msg.CloseDeleteWordDialog -> state
                .hideWordWarningDialog() to setOf()

            is Msg.RemoveWord -> state to setOf(DatasourceEffect.RemoveWord(message.wordId))

            is Msg.EnterWordEditMode -> state
                .enableWordEdit() to setOf()

            is Msg.UpdateWordInput -> state
                .updateWordEdited(edited = message.value) to setOf()

            is Msg.ExitWordEditMode -> state
                .disableWordEdit() to setOf()

            is Msg.CommitWordChanges -> state
                .setWordValue(value = state.wordState.edited) to setOf(
                DatasourceEffect.UpdateWord(
                    wordId = state.wordState.id,
                    value = state.wordState.edited,
                )
            )

            is Msg.OpenAddLexemeDialog -> state
                .showAddLexemeBottom()
                .setTranslationCheck(checked = false)
                .setDefinitionCheck(checked = false) to setOf()

            is Msg.CloseAddLexemeDialog -> state
                .hideAddLexemeBottom()
                .setTranslationCheck(checked = false)
                .setDefinitionCheck(checked = false) to setOf()

            is Msg.EnableTranslationCreation -> state
                .setTranslationCheck(checked = message.isAdded) to setOf()

            is Msg.EnableDefinitionCreation -> state
                .setDefinitionCheck(checked = message.isAdded) to setOf()

            is Msg.CreateLexeme -> state to setOf(
                DatasourceEffect.CreateLexeme(wordId = state.wordState.id)
            )

            is Msg.RefreshLexeme -> state
                .addLexeme(lexeme = 
                    LexemeState(
                        id = message.lexeme.lexemeId.id,
                        translation = if (state.addLexemeBottomState.isTranslationCheck) {
                            TextValueState(
                                origin = "",
                                isEdit = false,
                            )
                        } else {
                            null
                        },
                        definition = if (state.addLexemeBottomState.isDefinitionCheck) {
                            TextValueState(
                                origin = "",
                                isEdit = false,
                            )
                        } else {
                            null
                        }
                    )
                )
                .hideAddLexemeBottom()
                .setTranslationCheck(checked = false)
                .setDefinitionCheck(checked = false) to setOf()

            is Msg.RemoveLexeme -> state to setOf(
                DatasourceEffect.RemoveLexeme(message.lexemeId)
            )

            is Msg.OpenLexemeMenu -> state
                .setLexemeMenuOpen(lexemeId = message.lexemeId, isOpen = message.isShow) to setOf()

            is Msg.CreateTranslation -> state
                .createLexemeTranslation(lexemeId = message.lexemeId) to setOf()

            is Msg.UpdateTranslationInput -> state
                .updateLexemeTranslationText(lexemeId = message.lexemeId, text = message.value) to setOf()

            is Msg.EnterTranslationEditMode -> state
                .enableLexemeTranslationEdit(lexemeId = message.lexemeId) to setOf()

            is Msg.ExitTranslationEditMode -> state to setOf(
                DatasourceEffect.UpdateLexemeTranslation(
                    wordId = state.wordState.id,
                    lexemeId = message.lexemeId,
                    translation = state.lexemeList
                        .first { it.id == message.lexemeId }.translation?.edited
                        ?: ""
                )
            )

            is Msg.RefreshTranslation -> state
                .refreshLexemeTranslation(
                    lexemeId = message.lexeme.lexemeId.id,
                    newOrigin = message.lexeme.translation?.value ?: ""
                ) to setOf()

            is Msg.RemoveTranslation -> state to setOf(
                DatasourceEffect.RemoveTranslation(message.lexemeId)
            )

            is Msg.CreateDefinition -> state
                .createLexemeDefinition(lexemeId = message.lexemeId) to setOf()

            is Msg.UpdateDefinitionInput -> state
                .updateLexemeDefinitionText(lexemeId = message.lexemeId, text = message.value) to setOf()

            is Msg.EnterDefinitionEditMode -> state
                .enableLexemeDefinitionEdit(lexemeId = message.lexemeId) to setOf()

            is Msg.ExitDefinitionEditMode -> state to setOf(
                DatasourceEffect.UpdateLexemeDefinition(
                    wordId = state.wordState.id,
                    lexemeId = message.lexemeId,
                    definition = state.lexemeList.first { it.id == message.lexemeId }.definition?.edited
                        ?: ""
                )
            )

            is Msg.RefreshDefinition -> state
                .refreshLexemeDefinition(
                    lexemeId = message.lexeme.lexemeId.id,
                    newOrigin = message.lexeme.definition?.value ?: ""
                ) to setOf()

            is Msg.RemoveDefinition -> state to setOf(
                DatasourceEffect.RemoveDefinition(message.lexemeId)
            )

            is Msg.NavigateBack -> state
                .closeScreen() to setOf()

            is UiMsg.ShowNotification -> if (message.show) {
                state.showSnackbar(title = message.text) to setOf()
            } else {
                state.copy(
                    snackbarState = state.snackbarState.copy(
                        title = message.text,
                        show = false
                    )
                ) to setOf()
            }

            is Msg.NoOperation -> state to setOf()
        }
    }


}