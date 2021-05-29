package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_interactor.entity.WriteQuizStep
import javax.inject.Inject

interface UpdateWriteQuizUseCase {
    fun updateWriteQuiz(writeQuizStep: WriteQuizStep): Completable

    class UpdateWriteQuizUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : UpdateWriteQuizUseCase {
        override fun updateWriteQuiz(writeQuizStep: WriteQuizStep): Completable {
            return dbApi.updateWriteQuizList(
                WriteQuiz(
                    id = writeQuizStep.id,
                    definitionId = writeQuizStep.definitionId,
                    grade = writeQuizStep.grade,
                    score = writeQuizStep.score,
                    addDate = writeQuizStep.addDate,
                    lastSelectDate = writeQuizStep.lastSelectDate,
                )
            )
        }
    }
}

