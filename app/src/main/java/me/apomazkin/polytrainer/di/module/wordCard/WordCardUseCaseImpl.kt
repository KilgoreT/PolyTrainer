package me.apomazkin.polytrainer.di.module.wordCard

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.canRemoveDefinition
import me.apomazkin.core_db_api.entity.canRemoveTranslation
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.wordcard.LogTags
import me.apomazkin.wordcard.deps.RemoveDefinitionResult
import me.apomazkin.wordcard.deps.RemoveTranslationResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import javax.inject.Inject

class WordCardUseCaseImpl @Inject constructor(
    private val wordApi: CoreDbApi.WordApi,
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) : WordCardUseCase {

    override suspend fun getTermById(wordId: Long): Term? {
        return termApi.getTermById(wordId)?.toDomainEntity()
    }

    override suspend fun deleteWord(wordId: Long): Int {
        return wordApi.deleteWordSuspend(wordId)
    }

    override suspend fun updateWord(wordId: Long, value: String): Boolean {
        return wordApi.updateWordSuspend(wordId, value.trim())
    }

    override suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>? = try {
        lexemeApi.deleteLexeme(lexemeId)
        termApi.getTermById(wordId)?.lexemes?.map { it.toDomain() }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "deleteLexeme failed: ${e.message}")
        null
    }

    override suspend fun addLexemeTranslation(
        wordId: Long,
        lexemeId: Long?,
        translation: String,
    ): Lexeme? = try {
        val trimmed = translation.trim()
        val id = if (lexemeId == null) {
            // ATOMIC: один INSERT с переводом через existing overload (CoreDbApi.LexemeApi).
            insertLexemeWithTranslation(wordId, trimmed)
        } else {
            lexemeApi.updateLexemeTranslation(lexemeId, TranslationApiEntity(trimmed))
            lexemeId
        }
        lexemeApi.getLexemeById(id)?.toDomain()
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "addLexemeTranslation failed: ${e.message}")
        null
    }

    override suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult? = try {
        val lexeme = lexemeApi.getLexemeById(lexemeId)
        if (lexeme?.canRemoveTranslation() == true) {
            lexemeApi.updateLexemeTranslation(id = lexemeId, translation = null)
            val updated = lexemeApi.getLexemeById(lexemeId)?.toDomain()
            updated?.let { RemoveTranslationResult.TranslationRemoved(it) }
        } else {
            lexemeApi.deleteLexeme(id = lexemeId)
            RemoveTranslationResult.LexemeCascadeRemoved
        }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "deleteLexemeTranslation failed: ${e.message}")
        null
    }

    override suspend fun addLexemeDefinition(
        wordId: Long,
        lexemeId: Long?,
        definition: String,
    ): Lexeme? = try {
        val trimmed = definition.trim()
        val id = if (lexemeId == null) {
            insertLexemeWithDefinition(wordId, trimmed)
        } else {
            lexemeApi.updateLexemeDefinition(lexemeId, DefinitionApiEntity(trimmed))
            lexemeId
        }
        lexemeApi.getLexemeById(id)?.toDomain()
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "addLexemeDefinition failed: ${e.message}")
        null
    }

    override suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult? = try {
        val lexeme = lexemeApi.getLexemeById(lexemeId)
        if (lexeme?.canRemoveDefinition() == true) {
            lexemeApi.updateLexemeDefinition(id = lexemeId, definition = null)
            val updated = lexemeApi.getLexemeById(lexemeId)?.toDomain()
            updated?.let { RemoveDefinitionResult.DefinitionRemoved(it) }
        } else {
            lexemeApi.deleteLexeme(id = lexemeId)
            RemoveDefinitionResult.LexemeCascadeRemoved
        }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "deleteLexemeDefinition failed: ${e.message}")
        null
    }

    override suspend fun restoreLexeme(
        wordId: Long,
        translation: String?,
        definition: String?,
    ): List<Lexeme>? {
        val trimmedTranslation = translation?.trim()
        val trimmedDefinition = definition?.trim()
        if (trimmedTranslation == null && trimmedDefinition == null) {
            logger.e(tag = LogTags.WORDCARD, message = "restoreLexeme: both translation and definition are null")
            return null
        }
        return try {
            when {
                trimmedTranslation != null && trimmedDefinition != null -> {
                    // Atomic insert обоих через единый API отсутствует — sequence из
                    // двух операций; rollback первой при провале второй.
                    val id = insertLexemeWithTranslation(wordId, trimmedTranslation)
                    try {
                        lexemeApi.updateLexemeDefinition(id, DefinitionApiEntity(trimmedDefinition))
                    } catch (e: Exception) {
                        lexemeApi.deleteLexeme(id)
                        throw e
                    }
                }
                trimmedTranslation != null -> {
                    insertLexemeWithTranslation(wordId, trimmedTranslation)
                }
                else -> {
                    insertLexemeWithDefinition(wordId, trimmedDefinition!!)
                }
            }
            termApi.getTermById(wordId)?.lexemes?.map { it.toDomain() }
        } catch (e: Exception) {
            logger.e(tag = LogTags.WORDCARD, message = "restoreLexeme failed: ${e.message}")
            null
        }
    }

    /**
     * Atomic INSERT lexeme + translation + write-quiz в одной Room-транзакции через
     * [CoreDbApi.LexemeApi.addLexemeWithTranslation]. Закрывает domain-инвариант
     * «у каждой лексемы есть write-quiz».
     */
    private suspend fun insertLexemeWithTranslation(wordId: Long, translation: String): Long {
        val currentDictionaryId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?: run {
                logger.e(tag = LogTags.WORDCARD, message = "insertLexemeWithTranslation: current dictionary id not in prefs")
                throw IllegalStateException("Dictionary not found")
            }
        val dictionaryId = dictionaryApi.getDictionaryById(currentDictionaryId)?.id
            ?: run {
                logger.e(tag = LogTags.WORDCARD, message = "insertLexemeWithTranslation: dictionary $currentDictionaryId not in DB")
                throw IllegalStateException("Dictionary not found")
            }
        return lexemeApi.addLexemeWithTranslation(
            wordId = wordId,
            dictionaryId = dictionaryId,
            translation = TranslationApiEntity(translation),
        )
    }

    private suspend fun insertLexemeWithDefinition(wordId: Long, definition: String): Long {
        val currentDictionaryId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?: run {
                logger.e(tag = LogTags.WORDCARD, message = "insertLexemeWithDefinition: current dictionary id not in prefs")
                throw IllegalStateException("Dictionary not found")
            }
        val dictionaryId = dictionaryApi.getDictionaryById(currentDictionaryId)?.id
            ?: run {
                logger.e(tag = LogTags.WORDCARD, message = "insertLexemeWithDefinition: dictionary $currentDictionaryId not in DB")
                throw IllegalStateException("Dictionary not found")
            }
        return lexemeApi.addLexemeWithDefinition(
            wordId = wordId,
            dictionaryId = dictionaryId,
            definition = DefinitionApiEntity(definition),
        )
    }
}

fun TermApiEntity.toDomainEntity(): Term {
    return Term(
        wordId = WordId(word.id),
        word = Word(word.value),
        addedDate = word.addDate,
        changedDate = word.changeDate,
        removedDate = word.removeDate,
        // Сортировка newest-first для UI-инварианта "новая лексема сверху".
        lexemeList = lexemes
            .sortedByDescending { it.addDate }
            .map { it.toDomain() }
    )
}
