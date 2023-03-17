package me.apomazkin.vocabulary.logic.processor

import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerResult
import me.apomazkin.vocabulary.entity.LexemeLabel
import me.apomazkin.vocabulary.logic.*
import me.apomazkin.vocabulary.tools.insertToBegin
import me.apomazkin.vocabulary.tools.modifyFiltered
import me.apomazkin.vocabulary.tools.modifyFilteredOrFirst

internal fun processWordDetailMessage(
    state: VocabularyTabState,
    message: WordDetailMsg
): ReducerResult<VocabularyTabState, Effect> {
    return when (message) {
        is WordDetailMsg.Show -> onShowWordDetail(state, message.wordId)
        is WordDetailMsg.Hide -> onHideWordDetail(state)
        is WordDetailMsg.LexicalCategory -> onLexicalCategoryChange(
            state,
            message.lexemeId,
            message.category
        )
        is WordDetailMsg.ResetLexemeCategory -> onResetLexemeCategory(state, message.lexemeId)
        is WordDetailMsg.DefinitionUpdate -> onDefinitionUpdate(
            state,
            message.lexemeId,
            message.value
        )
        is WordDetailMsg.DefinitionEditStart -> onDefinitionEditStart(state, message.lexemeId)
        is WordDetailMsg.DefinitionEditFinish -> onDefinitionEditFinish(
            state,
            message.lexemeId,
            message.value
        )
        is WordDetailMsg.TranslationUpdate -> onTranslationUpdate(
            state,
            message.lexemeId,
            message.value
        )
        is WordDetailMsg.ExampleUpdate -> onExampleUpdate(state, message.value)
        is WordDetailMsg.AddLexeme -> onAddLexeme(state)
        is WordDetailMsg.Save -> onSaveLexemeList(state, message.wordId, message.lexemeList)
    }
}

private fun onShowWordDetail(
    state: VocabularyTabState,
    wordId: Long
): ReducerResult<VocabularyTabState, Effect> {
    val term = state.termList.first { it.id == wordId }
    val lexemeList = if (term.lexemeList.isEmpty()) {
        listOf(newLexemeState())
    } else {
        term.lexemeList.map {
            LexemeState(
                lexemeId = it.id,
                category = it.category,
                definition = EditableTextState(text = it.definition),
            )
        }
    }
    return state
        .copy(
            wordDetailDialogState = state.wordDetailDialogState.copy(
                isOpen = true,
                wordId = wordId,
                word = term.wordValue,
                lexemeList = lexemeList,
            )
        ) to setOf()
}

private fun onHideWordDetail(
    state: VocabularyTabState,
): ReducerResult<VocabularyTabState, Effect> = state
    .copy(
        wordDetailDialogState = state.wordDetailDialogState.copy(
            isOpen = false,
            wordId = -1,
            word = "",
            lexemeList = listOf()
        )
    ) to setOf()

private fun onLexicalCategoryChange(
    state: VocabularyTabState,
    lexemeId: Long?,
    category: LexemeLabel,
): ReducerResult<VocabularyTabState, Effect> = state.copy(
    wordDetailDialogState = state.wordDetailDialogState.copy(
        lexemeList = state.wordDetailDialogState.lexemeList
            .modifyFilteredOrFirst(
                predicate = { it.lexemeId == lexemeId },
                action = { it.copy(category = category) }
            )
    )
) to setOf()

private fun onResetLexemeCategory(
    state: VocabularyTabState,
    lexemeId: Long?,
): ReducerResult<VocabularyTabState, Effect> = state.copy(
    wordDetailDialogState = state.wordDetailDialogState.copy(
        lexemeList = state.wordDetailDialogState.lexemeList
            .modifyFilteredOrFirst(
                predicate = { it.lexemeId == lexemeId },
                action = { it.copy(category = LexemeLabel.UNDEFINED) }
            )
    )
) to setOf()

fun onDefinitionEditStart(
    state: VocabularyTabState,
    lexemeId: Long?
): ReducerResult<VocabularyTabState, Effect> = state.copy(
    wordDetailDialogState = state.wordDetailDialogState.copy(
        lexemeList = state.wordDetailDialogState.lexemeList.modifyFiltered(
            predicate = { it.lexemeId == lexemeId },
            action = {
                it.copy(
                    definition = it.definition
                        .copy(
                            readOnly = false,
                            editedText = it.definition.text
                        )
                )
            }
        )
    )
) to setOf()

fun onDefinitionEditFinish(
    state: VocabularyTabState,
    lexemeId: Long?,
    value: String
): ReducerResult<VocabularyTabState, Effect> = state to setOf()

private fun onDefinitionUpdate(
    state: VocabularyTabState,
    lexemeId: Long?,
    value: String,
): ReducerResult<VocabularyTabState, Effect> = state.copy(
    wordDetailDialogState = state.wordDetailDialogState.copy(
        lexemeList = state.wordDetailDialogState.lexemeList
            .modifyFilteredOrFirst(
                predicate = { it.lexemeId == lexemeId },
                action = { it.copy(definition = it.definition.copy(editedText = value)) }
            )
    )
) to setOf()

private fun onTranslationUpdate(
    state: VocabularyTabState,
    lexemeId: Long?,
    value: String,
): ReducerResult<VocabularyTabState, Effect> = state.copy(
    wordDetailDialogState = state.wordDetailDialogState.copy(
        lexemeList = state.wordDetailDialogState.lexemeList
            .modifyFilteredOrFirst(
                predicate = { it.lexemeId == lexemeId },
                action = { it.copy(translation = value) }
            )
    )
) to setOf()

private fun onExampleUpdate(
    state: VocabularyTabState,
    value: String,
): ReducerResult<VocabularyTabState, Effect> = state to setOf()

fun onAddLexeme(
    state: VocabularyTabState
): ReducerResult<VocabularyTabState, Effect> = state.copy(
    wordDetailDialogState = state.wordDetailDialogState.copy(
        lexemeList = state.wordDetailDialogState.lexemeList
            .insertToBegin(newLexemeState())
    )
) to setOf()

private fun onSaveLexemeList(
    state: VocabularyTabState,
    wordId: Long,
    lexemeList: List<LexemeState>,
): ReducerResult<VocabularyTabState, Effect> = state to setOf(
    DatasourceEffect.SaveLexeme(
        wordId = wordId,
        lexemeList = lexemeList,
    )
)