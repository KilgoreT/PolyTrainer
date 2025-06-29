package me.apomazkin.polytrainer.di.module.quizchat

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.Definition
import me.apomazkin.quiz.chat.entity.Lexeme
import me.apomazkin.quiz.chat.entity.QuizType
import me.apomazkin.quiz.chat.entity.Translation
import me.apomazkin.quiz.chat.entity.Word
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity
import javax.inject.Inject

class QuizChatUseCaseImpl @Inject constructor(
    private val langApi: CoreDbApi.LangApi,
    private val quizApi: CoreDbApi.QuizApi,
    private val prefsProvider: PrefsProvider,
) : QuizChatUseCase {
    
    override suspend fun getCurrentLangId(): Long {
        prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
            ?.let { num ->
                langApi.getLang(numericCode = num)
                    ?.let { return it.id.toLong() }
            }
            ?: langApi.getLangList()
                .firstOrNull()
                ?.let {
                    prefsProvider.setInt(
                        PrefKey.CURRENT_LANG_NUMERIC_CODE_INT,
                        it.numericCode
                    )
                    return it.id.toLong()
                }
        
        throw IllegalStateException("Language not found")
    }
    
    override suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int {
        return quizApi.updateWriteQuiz(entity = entity.toApiEntity())
    }
    
    override suspend fun getRandomWriteQuizList(
        limit: Int,
        maxGrade: Int,
        langId: Long
    ): List<WriteQuiz> {
        
        val allByGrades: Map<Int, List<WriteQuiz>> = (0..maxGrade)
            .associateWith { grade ->
                quizApi.getRandomWriteQuizList(
                    grade = grade,
                    limit = limit,
                    langId = langId,
                ).shuffled().toDomainEntity(type = QuizType.GRADES)
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
                    .getEarliestWriteQuizList(limit, langId)
                    .shuffled()
                    .toDomainEntity(type = QuizType.EARLIEST)
                    .take(2)
            result += earliest
        }
        val isFrequentMistakesOn = prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN)
                ?: false
        if (isFrequentMistakesOn) {
            val frequentMistakes = quizApi
                .getFrequentMistakesWriteQuizList(limit, langId)
                .shuffled()
                .toDomainEntity(type = QuizType.ERRORS)
                .take(2)
            result += frequentMistakes
        }

        return result
            .shuffled()
    }
}

fun LexemeApiEntity.toDomainEntity() = Lexeme(
    id = id,
    translation = translation?.let { Translation(it.value) },
    definition = definition?.let { Definition(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)

fun WordApiEntity.toDomainEntity() = Word(
    id = id,
    value = value,
)

fun WriteQuizComplexEntity.toDomainEntity(type: QuizType?) = WriteQuiz(
        id = quizData.id,
        langId = quizData.langId,
        grade = quizData.grade,
        score = quizData.score,
        errorCount = quizData.errorCount,
        addDate = quizData.addDate,
        lastCorrectAnswerDate = quizData.lastCorrectAnswerDate,
        lexeme = lexemeData.toDomainEntity(),
        word = wordData.toDomainEntity(),
        type = type
)

fun List<WriteQuizComplexEntity>.toDomainEntity(
        type: QuizType?
) = map { it.toDomainEntity(type) }

fun WriteQuizUpsertEntity.toApiEntity() = WriteQuizUpsertApiEntity(
        id = id,
        langId = langId,
        lexemeId = lexemeId,
        grade = grade,
        score = score,
        errorCount = errorCount,
        addDate = addDate,
        lastCorrectAnswerDate = lastCorrectAnswerDate
)

fun List<WriteQuizUpsertEntity>.toApiEntity() = map { it.toApiEntity() }