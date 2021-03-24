package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WriteQuiz
import javax.inject.Inject

interface GetWriteQuizUseCase {
    fun getWriteQuiz(grade: Int, limit: Int): Single<List<WriteQuiz>>

    class GetWriteQuizUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetWriteQuizUseCase {
        override fun getWriteQuiz(grade: Int, limit: Int): Single<List<WriteQuiz>> {
            return dbApi.getWriteQuizList(grade, limit)
        }
    }
}

