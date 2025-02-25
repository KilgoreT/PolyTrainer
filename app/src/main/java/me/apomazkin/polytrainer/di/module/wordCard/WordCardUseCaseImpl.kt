package me.apomazkin.polytrainer.di.module.wordCard

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.canRemoveDefinition
import me.apomazkin.core_db_api.entity.canRemoveTranslation
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
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
    private val langApi: CoreDbApi.LangApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val quizApi: CoreDbApi.QuizApi,
    private val prefsProvider: PrefsProvider,
) : WordCardUseCase {

    override suspend fun getTermById(wordId: Long): Term? {
        return termApi.getTermById(wordId)?.toDomainEntity()
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
        val numericCode = prefsProvider
            .getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
        val langId = langApi.getLang(numericCode = numericCode)?.id?.toLong()
            ?: throw IllegalStateException("Language not found")
        val lexemeId = lexemeApi.addLexeme(wordId)
        quizApi.addWriteQuiz(langId = langId, lexemeId = lexemeId)
        
        return lexemeApi.getLexemeById(lexemeId)?.toDomainEntity()
    }

    override suspend fun addLexemeTranslation(
        wordId: Long,
        lexemeId: Long?,
        translation: String
    ): Lexeme? {
        lexemeId?.let {
            lexemeApi.updateLexemeTranslation(
                lexemeId,
                TranslationApiEntity(translation)
            )
        }
        return lexemeId?.let { id ->
            lexemeApi.getLexemeById(id)?.toDomainEntity()
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
        lexemeId?.let {
            lexemeApi.updateLexemeDefinition(lexemeId, DefinitionApiEntity(definition))
        }
        return lexemeId?.let { id ->
            lexemeApi.getLexemeById(id)?.toDomainEntity()
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
        wordId = WordId(word.id),
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
    )
}