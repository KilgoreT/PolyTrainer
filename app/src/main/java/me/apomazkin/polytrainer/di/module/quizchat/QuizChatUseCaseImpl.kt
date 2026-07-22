package me.apomazkin.polytrainer.di.module.quizchat

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizPickerPrefKey
import me.apomazkin.quiz.chat.LogTags
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.QuizType
import me.apomazkin.quiz.chat.entity.Word
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity
import javax.inject.Inject

class QuizChatUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val quizApi: CoreDbApi.QuizApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) : QuizChatUseCase {
    
    override suspend fun getCurrentDictionaryId(): Long? {
        prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?.let { id ->
                dictionaryApi.getDictionaryById(id)
                    ?.let { return it.id }
            }
            ?: dictionaryApi.getDictionaryList()
                .firstOrNull()
                ?.let {
                    prefsProvider.setLong(
                        PrefKey.CURRENT_DICTIONARY_ID_LONG,
                        it.id
                    )
                    return it.id
                }

        // IS476: словарь отсутствует — null вместо throw
        return null
    }
    
    override suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int {
        return quizApi.updateWriteQuiz(entity = entity.toApiEntity())
    }
    
    override suspend fun getRandomWriteQuizList(
        limit: Int,
        maxGrade: Int,
        dictionaryId: Long
    ): List<WriteQuiz> {

        val allByGrades: Map<Int, List<WriteQuiz>> = (0..maxGrade)
            .associateWith { grade ->
                val ids = quizApi.getWriteQuizIds(grade = grade, dictionaryId = dictionaryId)
                val randomIds = ids.shuffled().take(limit)
                if (randomIds.isEmpty()) return@associateWith emptyList()
                quizApi.getWriteQuizByIds(randomIds)
                    .toDomainEntity(type = QuizType.GRADES)
            }
        val sortedGrades = allByGrades.toSortedMap()
        
        val result = mutableSetOf<WriteQuiz>()
        var remaining = limit
        
        for ((grade, list) in sortedGrades) {
            if (remaining <= 0) break
            val expectedCount = if (result.isEmpty()) {
                limit / 2
            } else {
                remaining / 2
            }.coerceAtLeast(1)
            
            val available = list.take(expectedCount)
            result += available
            remaining -= available.size
        }
        
        if (result.size < limit) {
            val leftovers = sortedGrades.values
                .flatten()
                .filterNot { it in result }
                .shuffled()
            result += leftovers.take(limit - result.size)
        }


        val isEarliestOn = prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN)
                ?: false
        if (isEarliestOn) {
            val earliest = quizApi
                    .getEarliestWriteQuizList(limit, dictionaryId)
                    .shuffled()
                    .toDomainEntity(type = QuizType.EARLIEST)
                    .take(2)
            result += earliest
        }
        val isFrequentMistakesOn = prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN)
                ?: false
        if (isFrequentMistakesOn) {
            val frequentMistakes = quizApi
                .getFrequentMistakesWriteQuizList(limit, dictionaryId)
                .shuffled()
                .toDomainEntity(type = QuizType.ERRORS)
                .take(2)
            result += frequentMistakes
        }

        return result
            .shuffled()
    }

    override suspend fun getQuizConfig(
        dictionaryId: Long,
        quizMode: String,
    ): QuizConfig? = try {
        lexemeApi.getQuizConfig(dictionaryId, quizMode)?.toDomain()
    } catch (e: Exception) {
        logger.e(tag = LogTags.CHAT, message = "getQuizConfig failed: ${e.message}")
        null
    }

    // ===== IS481 quiz picker (AGG-12) =====

    override suspend fun getAvailableTypes(dictionaryId: Long): List<ComponentType> {
        return lexemeApi.getComponentTypes(dictionaryId)
            .map { it.toDomain() }
            // IS486: CHOICE в квизах v1 не участвует (spec §9.6) — пикер не предлагает.
            .filter { it.template != ComponentTemplate.CHOICE }
    }

    override suspend fun getQuizPickerSelection(dictionaryId: Long): ComponentTypeRef? {
        val raw = prefsProvider.getStringByRawKey(quizPickerPrefKey(dictionaryId)) ?: return null
        return decodeRef(raw)
    }

    override suspend fun setQuizPickerSelection(dictionaryId: Long, ref: ComponentTypeRef) {
        prefsProvider.setStringByRawKey(quizPickerPrefKey(dictionaryId), encodeRef(ref))
    }
}

private const val PREFIX_BUILTIN = "builtin:"
private const val PREFIX_USER = "user:"

private fun encodeRef(ref: ComponentTypeRef): String = when (ref) {
    is ComponentTypeRef.BuiltIn -> "$PREFIX_BUILTIN${ref.key.key}"
    is ComponentTypeRef.UserDefined -> "$PREFIX_USER${ref.name}"
}

/**
 * Decode `builtin:<key>` / `user:<name>`. Anything else → null.
 *
 * - `builtin:<key>` — unknown key → null (future-proof для новых built-in типов).
 * - `user:<name>` — name восстанавливается через `substringAfter(':')`, корректно
 *   для names с `:`, unicode, любым `String` (включая пустую строку).
 * - Иной prefix (`USER:...`, `garbage`, `user`) → null.
 */
private fun decodeRef(raw: String): ComponentTypeRef? = when {
    raw.startsWith(PREFIX_BUILTIN) -> BuiltInComponent
        .fromKey(raw.substringAfter(':'))
        ?.let { ComponentTypeRef.BuiltIn(it) }
    raw.startsWith(PREFIX_USER) -> ComponentTypeRef.UserDefined(raw.substringAfter(':'))
    else -> null
}

fun WordApiEntity.toDomainEntity() = Word(
    id = id,
    value = value,
)

fun WriteQuizComplexEntity.toDomainEntity(type: QuizType?) = WriteQuiz(
        id = quizData.id,
        dictionaryId = quizData.dictionaryId,
        grade = quizData.grade,
        score = quizData.score,
        errorCount = quizData.errorCount,
        addDate = quizData.addDate,
        lastCorrectAnswerDate = quizData.lastCorrectAnswerDate,
        lexeme = lexemeData.toDomain(),
        word = wordData.toDomainEntity(),
        type = type
)

fun List<WriteQuizComplexEntity>.toDomainEntity(
        type: QuizType?
) = map { it.toDomainEntity(type) }

fun WriteQuizUpsertEntity.toApiEntity() = WriteQuizUpsertApiEntity(
        id = id,
        dictionaryId = dictionaryId,
        lexemeId = lexemeId,
        grade = grade,
        score = score,
        errorCount = errorCount,
        addDate = addDate,
        lastCorrectAnswerDate = lastCorrectAnswerDate
)

fun List<WriteQuizUpsertEntity>.toApiEntity() = map { it.toApiEntity() }