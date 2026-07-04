package me.apomazkin.polytrainer.di.module.wordCard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.toRef
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.wordcard.LogTags
import me.apomazkin.wordcard.deps.AddComponentValueResult
import me.apomazkin.wordcard.deps.RemoveComponentResult
import me.apomazkin.wordcard.deps.RemoveLexemeResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import javax.inject.Inject

/**
 * IS481 generic UseCase impl (AGG-12). dictionaryId приходит параметром из reducer
 * (через Effect), не резолвится через prefs (A1). Перевод/определение — обычные компоненты.
 */
class WordCardUseCaseImpl @Inject constructor(
    private val wordApi: CoreDbApi.WordApi,
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) : WordCardUseCase {

    override suspend fun getTermById(wordId: Long): Term? =
        termApi.getTermById(wordId)?.toDomainEntity()

    override suspend fun deleteWord(wordId: Long): Int = wordApi.deleteWordSuspend(wordId)

    override suspend fun updateWord(wordId: Long, value: String): Boolean =
        wordApi.updateWordSuspend(wordId, value.trim())

    override suspend fun deleteLexeme(wordId: Long, lexemeId: Long): RemoveLexemeResult? = try {
        val snapshot = lexemeApi.getLexemeById(lexemeId)?.toDomain() ?: return null
        lexemeApi.deleteLexeme(lexemeId)
        RemoveLexemeResult.Removed(snapshot)
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "deleteLexeme failed: ${e.message}")
        null
    }

    override suspend fun addLexemeWithComponent(
        wordId: Long,
        dictionaryId: Long,
        ref: ComponentTypeRef,
        data: TemplateValues,
    ): Lexeme? = try {
        lexemeApi.addLexemeWithComponents(wordId, dictionaryId, listOf(ref to data.trimmed()))
            ?.let { id -> lexemeApi.getLexemeById(id)?.toDomain() }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "addLexemeWithComponent failed: ${e.message}")
        null
    }

    override suspend fun addComponentValue(
        lexemeId: Long,
        componentTypeId: ComponentTypeId,
        data: TemplateValues,
    ): AddComponentValueResult? = try {
        val newId = lexemeApi.addComponentValue(lexemeId, componentTypeId.id, data.trimmed())
        val lexeme = lexemeApi.getLexemeById(lexemeId)?.toDomain() ?: return null
        AddComponentValueResult(lexeme = lexeme, newComponentValueId = ComponentValueId(newId))
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "addComponentValue failed: ${e.message}")
        null
    }

    override suspend fun updateComponentValue(
        componentValueId: ComponentValueId,
        lexemeId: Long,
        data: TemplateValues,
    ): Lexeme? = try {
        val updated = lexemeApi.updateComponentValue(componentValueId.id, data.trimmed())
        if (updated <= 0) null else lexemeApi.getLexemeById(lexemeId)?.toDomain()
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "updateComponentValue failed: ${e.message}")
        null
    }

    override suspend fun deleteComponentValue(
        componentValueId: ComponentValueId,
        lexemeId: Long,
    ): RemoveComponentResult? = try {
        val before = lexemeApi.getLexemeById(lexemeId)?.toDomain() ?: return null
        val remaining = lexemeApi.deleteComponentValue(componentValueId.id)
        if (remaining > 0) {
            lexemeApi.getLexemeById(lexemeId)?.toDomain()?.let { RemoveComponentResult.ComponentRemoved(it) }
        } else {
            lexemeApi.deleteLexeme(lexemeId)
            RemoveComponentResult.LexemeCascadeRemoved(before)
        }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "deleteComponentValue failed: ${e.message}")
        null
    }

    override suspend fun restoreLexemeWithComponents(
        wordId: Long,
        dictionaryId: Long,
        snapshot: Lexeme,
    ): Lexeme? = try {
        val components = snapshot.components.map { it.type.toRef() to it.data }
        lexemeApi.addLexemeWithComponents(wordId, dictionaryId, components)
            ?.let { id -> lexemeApi.getLexemeById(id)?.toDomain() }
    } catch (e: Exception) {
        logger.e(tag = LogTags.WORDCARD, message = "restoreLexemeWithComponents failed: ${e.message}")
        null
    }

    override fun flowAvailableComponentTypes(dictionaryId: Long): Flow<List<ComponentType>> =
        lexemeApi.flowTypesForDictionary(dictionaryId).map { list -> list.map { it.toDomain() } }

    private fun TemplateValues.trimmed(): TemplateValues =
        (this as? TextValues)?.let { TextValues(Primitive.Text(it.value.value.trim())) } ?: this
}

fun TermApiEntity.toDomainEntity(): Term = Term(
    wordId = WordId(word.id),
    word = Word(word.value),
    dictionaryId = word.dictionaryId,
    addedDate = word.addDate,
    changedDate = word.changeDate,
    removedDate = word.removeDate,
    // newest-first для UI «новая лексема сверху».
    lexemeList = lexemes.sortedByDescending { it.addDate }.map { it.toDomain() },
)
