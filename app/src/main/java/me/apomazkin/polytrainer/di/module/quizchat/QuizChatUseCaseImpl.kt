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
        val numericCode = prefsProvider
            .getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
        return langApi.getLang(numericCode = numericCode)?.id?.toLong()
            ?: throw IllegalStateException("Language not found")
    }
    
    override suspend fun updateWriteQuiz(list: List<WriteQuizUpsertEntity>): Int {
        return quizApi.updateWriteQuiz(entity = list.toApiEntity())
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
                ).shuffled().toDomainEntity()
            }
        val sortedGrades = allByGrades.toSortedMap()
        
        val result = mutableListOf<WriteQuiz>()
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
        return result
            .take(limit)
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

fun WriteQuizComplexEntity.toDomainEntity() = WriteQuiz(
    id = quizData.id,
    langId = quizData.langId,
    grade = quizData.grade,
    score = quizData.score,
    errorCount = quizData.errorCount,
    addDate = quizData.addDate,
    lastSelectDate = quizData.lastSelectDate,
    lexeme = lexemeData.toDomainEntity(),
    word = wordData.toDomainEntity(),
)

fun List<WriteQuizComplexEntity>.toDomainEntity() = map { it.toDomainEntity() }

fun WriteQuizUpsertEntity.toApiEntity() = WriteQuizUpsertApiEntity(
    id = id,
    langId = langId,
    lexemeId = lexemeId,
    grade = grade,
    score = score,
    errorCount = errorCount,
    addDate = addDate,
    lastSelectDate = lastSelectDate
)

fun List<WriteQuizUpsertEntity>.toApiEntity() = map { it.toApiEntity() }