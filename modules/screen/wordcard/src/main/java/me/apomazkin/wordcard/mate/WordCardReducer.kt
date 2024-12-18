package me.apomazkin.wordcard.mate

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.tools.insertToEnd
import me.apomazkin.tools.modifyFiltered
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

class WordCardReducer : MateReducer<WordCardState, Msg, Effect> {
    override fun reduce(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
        return when (message) {
            is Msg.TermLoading -> onTermLoading(state)
            is Msg.TermLoaded -> onTermLoaded(state, message.term)
            is Msg.TermNotLoaded -> TODO("TermNotLoaded is not implemented")

            is Msg.ShowDropdownMenu -> onChangeDropdownMenu(state = state, isShow = true)
            is Msg.HideDropdownMenu -> onChangeDropdownMenu(state = state, isShow = false)
            is Msg.ShowDeleteWordDialog -> onShowDeleteWordDialog(state)
            is Msg.HideDeleteWordDialog -> onCloseDeleteWordDialog(state)
            is Msg.DeleteWord -> onWordDelete(state, message.wordId)

            is Msg.OpenEditWord -> onOpenEditWord(state)
            is Msg.ChangeWordValue -> onChangeWordValue(state, message.value)
            is Msg.CloseEditWord -> onCloseEditWord(state)
            is Msg.SaveWordValue -> onSaveWord(state)

            is Msg.ShowAddLexemeBottom -> onShowAddLexemeBottom(
                state = state,
                isShow = true
            )

            is Msg.HideAddLexemeBottom -> onShowAddLexemeBottom(state = state, isShow = false)
            is Msg.AddLexemeBottomTranslation -> onAddLexemeBottomTranslation(
                state = state,
                isAdded = message.isAdded
            )

            is Msg.AddLexemeBottomDefinition -> onAddLexemeBottomDefinition(
                state = state,
                isAdded = message.isAdded
            )

            is Msg.AddLexeme -> onAddLexeme(state)
            is Msg.LexemeUpdate -> onUpdateLexeme(state, message.lexeme)
            is Msg.DeleteLexeme -> onDeleteLexeme(state, message.lexemeId)
            is Msg.ShowLexemeDropDown -> onShowLexemeDropDown(
                state = state,
                lexemeId = message.lexemeId,
                isShow = message.isShow
            )

            is Msg.AppendTranslation -> onAppendTranslation(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.TranslationTextChange -> onTranslationTextChange(
                state = state,
                lexemeId = message.lexemeId,
                value = message.value
            )

            is Msg.TranslationOpenEdit -> onTranslationOpenEdit(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.TranslationEndEdit -> onTranslationEndEdit(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.TranslationUpdate -> onTranslationRefresh(
                state = state,
                lexeme = message.lexeme
            )

            is Msg.DeleteTranslation -> onTranslationDelete(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.AppendDefinition -> onAppendDefinition(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.DefinitionTextChange -> onDefinitionTextChange(
                state = state,
                lexemeId = message.lexemeId,
                value = message.value
            )

            is Msg.DefinitionOpenEdit -> onDefinitionOpenEdit(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.DefinitionEndEdit -> onDefinitionEndEdit(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.DefinitionUpdate -> onDefinitionRefresh(
                state = state,
                lexeme = message.lexeme
            )

            is Msg.DeleteDefinition -> onDefinitionDelete(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.CloseScreen -> onCloseScreen(state)
            is UiMsg.Snackbar -> onShowSnackbar(state, message.text, message.show)
            is Msg.Empty -> state to setOf()
        }
    }

    private fun onTermLoading(
        state: WordCardState
    ): ReducerResult<WordCardState, Effect> = state.copy(
        isLoading = true
    ) to setOf(DatasourceEffect.LoadWord(wordId = state.wordState.id))

    private fun onTermLoaded(
        state: WordCardState,
        term: Term,
    ): ReducerResult<WordCardState, Effect> = state.copy(
        isLoading = false,
        wordState = WordState(
            id = term.wordId.id,
            value = term.word.value,
            added = term.addedDate,
        ),
        lexemeList = term.lexemeList.map { lexeme: Lexeme ->
            LexemeState(
                id = lexeme.lexemeId.id,
                translation = lexeme.translation?.let { translation ->
                    TextValueState(
                        origin = translation.value,
                        isEdit = false,
                    )
                },
                definition = lexeme.definition?.let { definition ->
                    TextValueState(
                        origin = definition.value,
                        isEdit = false,
                    )
                }
            )
        }
    ) to setOf()

    private fun onChangeDropdownMenu(
        state: WordCardState,
        isShow: Boolean
    ): Pair<WordCardState, Set<Effect>> = state.copy(
        topBarState = state.topBarState.copy(isMenuOpen = isShow)
    ) to setOf()

    private fun onShowDeleteWordDialog(
        state: WordCardState,
    ): Pair<WordCardState, Set<Effect>> = state
        .copy(
            wordState = state.wordState.copy(
                showWarningDialog = true,
                deleteButtonEnabled = false,
            )
        ) to setOf()

    private fun onCloseDeleteWordDialog(
        state: WordCardState
    ): Pair<WordCardState, Set<Effect>> = state
        .copy(
            wordState = state.wordState.copy(
                showWarningDialog = false,
                deleteButtonEnabled = true,
            )
        ) to setOf()

    private fun onWordDelete(
        state: WordCardState,
        wordId: Long
    ): ReducerResult<WordCardState, Effect> = state to setOf(
        DatasourceEffect.DeleteWord(wordId)
    )

    private fun onOpenEditWord(
        state: WordCardState,
    ): ReducerResult<WordCardState, Effect> = state.copy(
        wordState = state.wordState.copy(
            edited = state.wordState.value,
            isEditMode = true
        )
    ) to setOf()

    private fun onChangeWordValue(
        state: WordCardState,
        value: String
    ): ReducerResult<WordCardState, Effect> = state.copy(
        wordState = state.wordState.copy(edited = value)
    ) to setOf()


    private fun onCloseEditWord(
        state: WordCardState,
    ): ReducerResult<WordCardState, Effect> = state.copy(
        wordState = state.wordState.copy(
            isEditMode = false,
        )
    ) to setOf()

    private fun onSaveWord(
        state: WordCardState
    ): ReducerResult<WordCardState, Effect> = state.copy(
        wordState = state.wordState.copy(
            value = state.wordState.edited,
        )
    ) to setOf(
        DatasourceEffect.SaveWord(
            wordId = state.wordState.id,
            value = state.wordState.edited,
        )
    )

    private fun onShowAddLexemeBottom(
        state: WordCardState,
        isShow: Boolean
    ): ReducerResult<WordCardState, Effect> = state.copy(
        addLexemeBottomState = state.addLexemeBottomState
            .copy(
                show = isShow,
                isDefinitionCheck = false,
                isTranslationCheck = false,
            )
    ) to setOf()

    private fun onAddLexemeBottomTranslation(
        state: WordCardState,
        isAdded: Boolean
    ): ReducerResult<WordCardState, Effect> = state.copy(
        addLexemeBottomState = state.addLexemeBottomState.copy(isTranslationCheck = isAdded)
    ) to setOf()

    private fun onAddLexemeBottomDefinition(
        state: WordCardState,
        isAdded: Boolean
    ): ReducerResult<WordCardState, Effect> = state.copy(
        addLexemeBottomState = state.addLexemeBottomState.copy(isDefinitionCheck = isAdded)
    ) to setOf()

    private fun onAddLexeme(
        state: WordCardState
    ): ReducerResult<WordCardState, Effect> = state to setOf(
        DatasourceEffect.AddLexeme(
            wordId = state.wordState.id,
        )
    )

    private fun onUpdateLexeme(
        state: WordCardState,
        lexeme: Lexeme
    ): ReducerResult<WordCardState, Effect> = state.copy(
        lexemeList = state.lexemeList
            .insertToEnd(
                LexemeState(
                    id = lexeme.lexemeId.id,
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
            ),
        addLexemeBottomState = state.addLexemeBottomState.copy(
            isDefinitionCheck = false,
            isTranslationCheck = false,
            show = false
        )
    ) to setOf()

    private fun onDeleteLexeme(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> =
        state to setOf(DatasourceEffect.DeleteLexeme(lexemeId))

    private fun onShowLexemeDropDown(
        state: WordCardState,
        lexemeId: Long,
        isShow: Boolean
    ): ReducerResult<WordCardState, Effect> = state.copy(
        lexemeList = state.lexemeList.modifyFiltered(
            predicate = { it.id == lexemeId },
            action = {
                it.copy(isMenuOpen = isShow)
            }
        )
    ) to setOf()

    private fun onAppendTranslation(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> = state.copy(
        lexemeList = state.lexemeList.modifyFiltered(
            predicate = { it.id == lexemeId },
            action = {
                it.copy(
                    translation = TextValueState(
                        origin = "",
                        isEdit = true
                    )
                )
            }
        )
    ) to setOf()

    private fun onTranslationTextChange(
        state: WordCardState,
        lexemeId: Long,
        value: String
    ): ReducerResult<WordCardState, Effect> {
        return state.copy(
            lexemeList = state.lexemeList.modifyFiltered(
                predicate = { it.id == lexemeId },
                action = {
                    it.copy(translation = it.translation?.copy(edited = value))
                }
            )
        ) to setOf()
    }

    private fun onTranslationOpenEdit(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> {
        return state.copy(
            lexemeList = state.lexemeList.modifyFiltered(
                predicate = { it.id == lexemeId },
                action = {
                    it.copy(
                        translation = it.translation?.copy(
                            isEdit = true
                        )
                    )
                }
            )
        ) to setOf()
    }

    private fun onTranslationEndEdit(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> = state to setOf(
        DatasourceEffect.SaveLexemeTranslation(
            wordId = state.wordState.id,
            lexemeId = lexemeId,
            translation = state.lexemeList.first { it.id == lexemeId }.translation?.edited ?: ""
        )
    )

    private fun onTranslationRefresh(
        state: WordCardState,
        lexeme: Lexeme
    ): ReducerResult<WordCardState, Effect> = state.copy(
        lexemeList = state.lexemeList.modifyFiltered(
            predicate = { it.id == lexeme.lexemeId.id },
            action = {
                it.copy(
                    translation = it.translation?.copy(
                        origin = lexeme.translation?.value ?: "",
                        isEdit = false
                    )
                )
            }
        )
    ) to setOf()

    private fun onTranslationDelete(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> = state to setOf(
        DatasourceEffect.DeleteTranslation(lexemeId)
    )

    private fun onAppendDefinition(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> = state.copy(
        lexemeList = state.lexemeList.modifyFiltered(
            predicate = { it.id == lexemeId },
            action = {
                it.copy(
                    definition = TextValueState(
                        origin = "",
                        isEdit = true
                    )
                )
            }
        )
    ) to setOf()

    private fun onDefinitionTextChange(
        state: WordCardState,
        lexemeId: Long,
        value: String
    ): ReducerResult<WordCardState, Effect> {
        return state.copy(
            lexemeList = state.lexemeList.modifyFiltered(
                predicate = { it.id == lexemeId },
                action = {
                    it.copy(definition = it.definition?.copy(edited = value))
                }
            )
        ) to setOf()
    }

    private fun onDefinitionOpenEdit(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> {
        return state.copy(
            lexemeList = state.lexemeList.modifyFiltered(
                predicate = { it.id == lexemeId },
                action = {
                    it.copy(
                        definition = it.definition?.copy(
                            isEdit = true
                        )
                    )
                }
            )
        ) to setOf()
    }

    private fun onDefinitionEndEdit(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> = state to setOf(
        DatasourceEffect.SaveLexemeDefinition(
            wordId = state.wordState.id,
            lexemeId = lexemeId,
            definition = state.lexemeList.first { it.id == lexemeId }.definition?.edited ?: ""
        )
    )

    private fun onDefinitionRefresh(
        state: WordCardState,
        lexeme: Lexeme
    ): ReducerResult<WordCardState, Effect> = state.copy(
        lexemeList = state.lexemeList.modifyFiltered(
            predicate = { it.id == lexeme.lexemeId.id },
            action = {
                it.copy(
                    definition = it.definition?.copy(
                        origin = lexeme.definition?.value ?: "",
                        isEdit = false
                    )
                )
            }
        )
    ) to setOf()

    private fun onDefinitionDelete(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> = state to setOf(
        DatasourceEffect.DeleteDefinition(lexemeId)
    )

    private fun onCloseScreen(
        state: WordCardState
    ): ReducerResult<WordCardState, Effect> = state.copy(closeScreen = true) to setOf()

    private fun onShowSnackbar(
        state: WordCardState,
        text: String,
        show: Boolean,
    ): ReducerResult<WordCardState, Effect> = state.copy(
        snackbarState = state.snackbarState
            .copy(title = text, show = show)
    ) to setOf()
}