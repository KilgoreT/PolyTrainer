package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface RemoveWriteQuizUseCase {
    fun removeWriteQuiz(definitionId: Long): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : RemoveWriteQuizUseCase {
        override fun removeWriteQuiz(definitionId: Long): Completable {
            return dbApi.removeWriteQuiz(definitionId)
        }
    }
}

