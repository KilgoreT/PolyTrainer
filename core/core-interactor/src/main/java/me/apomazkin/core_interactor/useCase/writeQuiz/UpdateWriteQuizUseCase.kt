package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WriteQuiz
import javax.inject.Inject

interface UpdateWriteQuizUseCase {
    fun updateWriteQuiz(writeQuiz: WriteQuiz): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : UpdateWriteQuizUseCase {
        override fun updateWriteQuiz(writeQuiz: WriteQuiz): Completable {
            return dbApi.updateWriteQuizList(writeQuiz)
        }
    }
}

