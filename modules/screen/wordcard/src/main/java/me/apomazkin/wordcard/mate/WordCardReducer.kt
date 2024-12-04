package me.apomazkin.wordcard.mate

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.tools.insertToBegin
import me.apomazkin.tools.modifyFiltered
import me.apomazkin.tools.modifyFilteredOrFirst
import me.apomazkin.wordcard.entity.Term

class WordCardReducer : MateReducer<WordCardState, Msg, Effect> {
    override fun reduce(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
        return when (message) {
            is Msg.TermLoading -> onTermLoading(state)
            is Msg.TermLoaded -> onTermLoaded(state, message.term)
            is Msg.ShowDropdownMenu -> onChangeDropdownMenu(state = state, isShow = true)
            is Msg.HideDropdownMenu -> onChangeDropdownMenu(state = state, isShow = false)
            is Msg.ShowDeleteWordDialog -> onShowDeleteWordDialog(state)
            is Msg.HideDeleteWordDialog -> onCloseDeleteWordDialog(state)
            is Msg.DeleteWord -> onWordDelete(state, message.wordId)
            is Msg.ChangeWordValue -> onChangeWordValue(state, message.value)
            is Msg.OpenEditWord -> onOpenEditWord(state)
            is Msg.CloseEditWord -> onCloseEditWord(state)
            is Msg.SaveWordValue -> onSaveWord(state)
            is Msg.ShowAddLexemeBottom -> onShowAddLexemeBottom(state = state, isShow = true)
            is Msg.HideAddLexemeBottom -> onShowAddLexemeBottom(state = state, isShow = false)
            is Msg.AddLexemeBottomDefinition -> onAddLexemeBottomDefinition(
                state = state,
                isAdded = message.isAdded
            )

            is Msg.AddLexemeBottomTranslation -> onAddLexemeBottomTranslation(
                state = state,
                isAdded = message.isAdded
            )

            is Msg.AddLexeme -> onAddLexeme(state)
            is Msg.TranslationOpenEdit -> onTranslationOpenEdit(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.TranslationTextChange -> onTranslationTextChange(
                state = state,
                lexemeId = message.lexemeId,
                value = message.value
            )

            is Msg.TranslationCloseEdit -> onTranslationCloseEdit(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.DefinitionOpenEdit -> onDefinitionOpenEdit(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.DefinitionTextChange -> onDefinitionTextChange(
                state = state,
                lexemeId = message.lexemeId,
                value = message.value
            )

            is Msg.DefinitionCloseEdit -> onDefinitionCloseEdit(
                state = state,
                lexemeId = message.lexemeId
            )

            is Msg.ResetLexeme -> onResetLexeme(state, message.lexemeId)
            is Msg.EditLexeme -> onEditLexeme(state, message.lexemeId)
            is Msg.DeleteLexeme -> onDeleteLexeme(state, message.lexemeId)
//            is Msg.LexicalCategoryChange -> onLexicalCategoryChange(state, message.lexemeId, message.category)
//            is Msg.LexicalCategoryReset -> onResetLexemeCategory(state, message.lexemeId)
//            is Msg.DefinitionChange -> onDefinitionChange(state, message.lexemeId, message.value)
//            is Msg.SaveLexeme -> onSaveLexeme(state, message.lexemeId)
            is Msg.CloseScreen -> onCloseScreen(state)
            is UiMsg.Snackbar -> onShowSnackbar(state, message.text, message.show)
            is Msg.Empty -> state to setOf()
        }
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

    private fun onTranslationCloseEdit(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> = state to setOf(
//        DatasourceEffect.SaveLexeme(
//            lexemeId = lexemeId,
//            value = state.lexemeList.first { it.id == lexemeId }.translation?.edited ?: ""
//        )
    )

    private fun onTranslationSave(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> {
        return state.copy(
            lexemeList = state.lexemeList.modifyFiltered(
                predicate = { it.id == lexemeId },
                action = {
                    it.copy(
                        translation = it.translation?.copy(
                            isEdit = false,
                            origin = it.translation.edited
                        )
                    )
                }
            )
        ) to setOf()
    }

    private fun onDefinitionCloseEdit(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> {
        return state.copy(
            lexemeList = state.lexemeList.modifyFiltered(
                predicate = { it.id == lexemeId },
                action = {
                    it.copy(
                        definition = it.definition?.copy(
                            isEdit = false,
                            origin = it.definition.edited
                        )
                    )
                }
            )
        ) to setOf()
    }

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
            added = term.added,
        ),
        lexemeList = term.lexemeList.map {
            LexemeState(
                id = it.lexemeId.id,
//                category = CategoryState(origin = it.category.toCategoryLabel()),
//                definition = DefinitionState(origin = it.definition)
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

    private fun onChangeWordValue(
        state: WordCardState,
        value: String
    ): ReducerResult<WordCardState, Effect> = state.copy(
        wordState = state.wordState.copy(edited = value)
    ) to setOf()

    private fun onOpenEditWord(
        state: WordCardState,
    ): ReducerResult<WordCardState, Effect> = state.copy(
        wordState = state.wordState.copy(
            edited = state.wordState.value,
            isEditMode = true
        )
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
//            isEdit = false,
            value = state.wordState.edited,
        )
    ) to setOf(
        DatasourceEffect.SaveWord(wordId = state.wordState.id, value = state.wordState.edited)
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

    private fun onAddLexemeBottomDefinition(
        state: WordCardState,
        isAdded: Boolean
    ): ReducerResult<WordCardState, Effect> = state.copy(
        addLexemeBottomState = state.addLexemeBottomState.copy(isDefinitionCheck = isAdded)
    ) to setOf()

    private fun onAddLexemeBottomTranslation(
        state: WordCardState,
        isAdded: Boolean
    ): ReducerResult<WordCardState, Effect> = state.copy(
        addLexemeBottomState = state.addLexemeBottomState.copy(isTranslationCheck = isAdded)
    ) to setOf()

    private fun onAddLexeme(
        state: WordCardState
    ): ReducerResult<WordCardState, Effect> = state.copy(
        lexemeList = state.lexemeList
            .insertToBegin(
                LexemeState(
                    translation = if (state.addLexemeBottomState.isTranslationCheck) {
                        TextValueState(
                            origin = "Перевод"
                        )
                    } else {
                        null
                    },
                    definition = if (state.addLexemeBottomState.isDefinitionCheck) {
                        TextValueState(
                            origin = "Определение"
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

    private fun onResetLexeme(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> {
        return if (lexemeId == NOT_IN_DB) {
            state.copy(
                lexemeList = state.lexemeList.filter { it.id != lexemeId }
            ) to setOf()
        } else {
            state.copy(
                lexemeList = state.lexemeList.modifyFiltered(
                    predicate = { it.id == lexemeId },
                    action = {
                        it.copy(
                            definition = it.definition?.copy(
                                isEdit = false,
                                edited = it.definition.origin
                            ),
//                            category = it.category.copy(edited = it.category.origin)
                        )
                    }
                )
            ) to setOf()
        }
    }

    private fun onEditLexeme(
        state: WordCardState,
        lexemeId: Long?
    ): ReducerResult<WordCardState, Effect> = state.copy(
        lexemeList = state.lexemeList.modifyFilteredOrFirst(
            predicate = { it.id == lexemeId },
            action = {
//                it.copy(isEdit = true)
                it
            }
        )
    ) to setOf()

    private fun onDeleteLexeme(
        state: WordCardState,
        lexemeId: Long
    ): ReducerResult<WordCardState, Effect> =
        state to setOf(DatasourceEffect.DeleteLexeme(lexemeId))

//    private fun onLexicalCategoryChange(
//        state: WordCardState,
//        lexemeId: Long,
//        category: CategoryLabel,
//    ): ReducerResult<WordCardState, Effect> = state
//        .copy(
//            lexemeList = state.lexemeList.modifyFilteredOrFirst(
//                predicate = { it.id == lexemeId },
//                action = {
//                    it.copy(
//                        category = it.category.copy(
//                            edited = category,
//                        )
//                    )
//                }
//            )
//        ) to setOf()

//    private fun onResetLexemeCategory(
//        state: WordCardState,
//        lexemeId: Long?,
//    ): ReducerResult<WordCardState, Effect> = state
//        .copy(
//            lexemeList = state.lexemeList.modifyFilteredOrFirst(
//                predicate = { it.id == lexemeId },
//                action = {
//                    it.copy(
//                        category = it.category.copy(
//                            edited = CategoryLabel.UNDEFINED
//                        )
//                    )
//                }
//            )
//        ) to setOf()

    private fun onDefinitionChange(
        state: WordCardState,
        lexemeId: Long,
        value: String
    ): ReducerResult<WordCardState, Effect> = state
        .copy(
            lexemeList = state.lexemeList.modifyFiltered(
                predicate = { it.id == lexemeId },
                action = {
                    it.copy(definition = it.definition?.copy(edited = value))
                }
            )
        ) to setOf()

//    private fun onSaveLexeme(
//        state: WordCardState,
//        lexemeId: Long
//    ): ReducerResult<WordCardState, Effect> {
//
//        val effectList = mutableListOf<DatasourceEffect>()
//        val currentLexeme = state.lexemeList.first { it.id == lexemeId }
//        if (lexemeId == NOT_IN_DB) {
//            effectList.add(
//                DatasourceEffect.SaveLexeme(
//                    wordId = state.wordState.id,
//                    category = currentLexeme.category,
//                    definition = currentLexeme.definition.edited,
//                )
//            )
//        } else {
//            currentLexeme.also {
//                if (it.category.isChanged()) {
//                    effectList.add(
//                        DatasourceEffect.ChangeLexicalCategory(
//                            lexemeId,
//                            it.category.edited
//                        )
//                    )
//                }
//                if (it.definition.isChanged()) {
//                    effectList.add(
//                        DatasourceEffect.ChangeDefinition(
//                            lexemeId,
//                            it.definition.edited
//                        )
//                    )
//                }
//            }
//        }
//
//        return state.copy(
//            canAddLexeme = true,
//            lexemeList = state.lexemeList.modifyFilteredOrFirst(
//                predicate = { it.id == lexemeId },
//                action = { it.copy(isEdit = false) }
//            ).filter { it.id != NOT_IN_DB }
//        ) to effectList.toList().toSet()
//    }

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