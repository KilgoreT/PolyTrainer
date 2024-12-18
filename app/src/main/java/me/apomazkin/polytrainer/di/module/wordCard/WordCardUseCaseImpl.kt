package me.apomazkin.polytrainer.di.module.wordCard

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.canRemoveDefinition
import me.apomazkin.core_db_api.entity.canRemoveTranslation
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Definition
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Translation
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import javax.inject.Inject

class WordCardUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
) : WordCardUseCase {

    override suspend fun getTermById(wordId: Long): Term? {
        return termApi.getTermById(wordId)?.let { it.toDomainEntity() }
    }

    override suspend fun deleteWord(wordId: Long): Int {
        return dbApi.deleteWordSuspend(wordId)
    }

    override suspend fun updateWord(wordId: Long, value: String): Boolean {
        return dbApi.updateWordSuspend(wordId, value)
    }

    override suspend fun deleteLexeme(lexemeId: Long): Boolean {
        return lexemeApi.deleteLexeme(lexemeId) > 0
    }

    override suspend fun addLexeme(wordId: Long): Lexeme? {
        val lexemeId = lexemeApi.addLexeme(wordId)
        return lexemeApi.getLexemeById(lexemeId)?.let { it.toDomainEntity() }
    }

    // TODO: Убрать создание, потому что лексема уже создана
    override suspend fun addLexemeTranslation(
        wordId: Long,
        lexemeId: Long?,
        translation: String
    ): Lexeme? {
        val newId: Long? = if (lexemeId != null) {
            lexemeApi.updateLexemeTranslation(lexemeId, TranslationApiEntity(translation))
        } else {
            lexemeApi.addLexeme(wordId, TranslationApiEntity(translation))
        }
        return newId?.let { id ->
            lexemeApi.getLexemeById(id)?.let { it.toDomainEntity() }
        }
    }

    override suspend fun deleteLexemeTranslation(lexemeId: Long) {
        val lexeme = lexemeApi.getLexemeById(lexemeId)
        if (lexeme?.canRemoveTranslation() == true) {
            lexemeApi.updateLexemeTranslation(id = lexemeId, translation = null)
        } else {
            lexemeApi.deleteLexeme(id = lexemeId)
        }
    }

    override suspend fun addLexemeDefinition(
        wordId: Long,
        lexemeId: Long?,
        definition: String
    ): Lexeme? {
        val newId: Long? = if (lexemeId != null) {
            lexemeApi.updateLexemeDefinition(lexemeId, DefinitionApiEntity(definition))
        } else {
            lexemeApi.addLexeme(wordId, DefinitionApiEntity(definition))
        }
        return newId?.let { id ->
            lexemeApi.getLexemeById(id)?.let { it.toDomainEntity() }
        }
    }

    override suspend fun deleteLexemeDefinition(lexemeId: Long) {
        val lexeme = lexemeApi.getLexemeById(lexemeId)
        if (lexeme?.canRemoveDefinition() == true) {
            lexemeApi.updateLexemeDefinition(id = lexemeId, definition = null)
        } else {
            lexemeApi.deleteLexeme(id = lexemeId)
        }
    }
}

fun TermApiEntity.toDomainEntity(): Term {
    return Term(
        wordId = WordId(word.id ?: throw IllegalStateException("WordId is null")),
        word = Word(word.value),
        addedDate = word.addDate,
        changedDate = word.changeDate,
        removedDate = word.removeDate,
        lexemeList = lexemes.map { it.toDomainEntity() }
    )
}

fun LexemeApiEntity.toDomainEntity(): Lexeme {
    return Lexeme(
        lexemeId = LexemeId(id),
        translation = translation?.let { Translation(it.value) },
        definition = definition?.let { Definition(it.value) },
        category = null,
        addDate = addDate,
        changeDate = changeDate,
        removeDate = removeDate
    )
}