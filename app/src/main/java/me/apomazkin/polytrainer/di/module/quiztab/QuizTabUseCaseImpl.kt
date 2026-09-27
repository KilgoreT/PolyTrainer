package me.apomazkin.polytrainer.di.module.quiztab

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.polytrainer.di.module.quizgroup.QuizGroupSelectionStore
import me.apomazkin.quiz.QuizGroupCount
import me.apomazkin.quiztab.deps.QuizTabUseCase
import javax.inject.Inject

/**
 * IS500. Тонкая делегация: словарь — [CurrentDictionaryProvider],
 * счётчики — QuizApi (маппинг Api-entity → доменный [QuizGroupCount]
 * здесь, core-db-api от domain/quiz не зависит), персист —
 * [QuizGroupSelectionStore].
 */
class QuizTabUseCaseImpl @Inject constructor(
    private val quizApi: CoreDbApi.QuizApi,
    private val currentDictionaryProvider: CurrentDictionaryProvider,
    private val quizGroupSelectionStore: QuizGroupSelectionStore,
) : QuizTabUseCase {

    override fun flowCurrentDictId(): Flow<Long?> =
        currentDictionaryProvider.flowCurrentDictId()

    override fun flowQuizGroupCounts(dictionaryId: Long): Flow<List<QuizGroupCount>> =
        quizApi.flowQuizGroupCounts(dictionaryId)
            .map { counts ->
                counts.map {
                    QuizGroupCount(
                        id = it.groupId,
                        name = it.name,
                        wordCount = it.wordCount,
                    )
                }
            }

    override fun flowDictionaryQuizWordCount(dictionaryId: Long): Flow<Int> =
        quizApi.flowDictionaryQuizWordCount(dictionaryId)

    override fun flowGroupSelection(quizType: String, dictionaryId: Long): Flow<Long?> =
        quizGroupSelectionStore.flowSelection(quizType = quizType, dictionaryId = dictionaryId)

    override suspend fun setGroupSelection(quizType: String, dictionaryId: Long, groupId: Long?) {
        quizGroupSelectionStore.setSelection(
            quizType = quizType,
            dictionaryId = dictionaryId,
            groupId = groupId,
        )
    }
}
