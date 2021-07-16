package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WriteQuiz
import javax.inject.Inject

interface GetRandomWriteQuizUseCase {
    fun getRandomWriteQuiz(grade: Int, limit: Int): Single<List<WriteQuiz>>

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetRandomWriteQuizUseCase {
        override fun getRandomWriteQuiz(grade: Int, limit: Int): Single<List<WriteQuiz>> {
            return dbApi.getRandomWriteQuizList(grade, limit)
        }
    }
}

