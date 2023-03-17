package me.apomazkin.polytrainer.di.module.wordCard

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.*
import javax.inject.Inject

class WordCardUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi,
) : WordCardUseCase {

    override suspend fun getTermById(wordId: Long): Term {
        return dbApi.getTermById(wordId).let {
            it.defList
            Term(
                wordId = WordId(it.word.id ?: throw IllegalStateException("WordId is null")),
                word = Word(it.word.value ?: throw IllegalStateException("WordId is null")),
                lexemeList = it.defList
                    .map { lexeme ->
                        Lexeme(
                            lexemeId = LexemeId(lexeme.id),
                            definition = lexeme.value,
                            category = lexeme.category
                        )
                    }
            )
        }
    }

    override suspend fun deleteWord(wordId: Long): Int {
        return dbApi.deleteWordSuspend(wordId)
    }

    override suspend fun updateWord(wordId: Long, value: String): Int {
        return dbApi.updateWordSuspend(wordId, value)
    }

    override suspend fun deleteLexeme(lexemeId: Long): Int {
        return dbApi.deleteLexemeSuspend(lexemeId)
    }

    override suspend fun addLexeme(wordId: Long, category: String, definition: String): Long {
        return dbApi.addLexemeSuspend(wordId, category, definition)
    }

    override suspend fun updateLexicalDefinition(lexemeId: Long, value: String): Int {
        return dbApi.updateLexemeDefinition(lexemeId, value)
    }

    override suspend fun updateLexicalCategory(lexemeId: Long, category: String): Int {
        return dbApi.updateLexemeCategory(lexemeId, category)
    }
}