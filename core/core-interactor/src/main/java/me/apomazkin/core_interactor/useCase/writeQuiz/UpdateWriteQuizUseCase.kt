package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import javax.inject.Inject

interface UpdateWriteQuizUseCase {
    fun updateWriteQuiz(writeQuizComplexEntity: WriteQuizComplexEntity): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : UpdateWriteQuizUseCase {
        override fun updateWriteQuiz(writeQuizComplexEntity: WriteQuizComplexEntity): Completable {
            return dbApi.updateWriteQuizList(writeQuizComplexEntity)
        }
    }
}

