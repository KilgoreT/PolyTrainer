package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import javax.inject.Inject

interface GetWriteQuizByRandomUseCase {
    fun getRandomWriteQuiz(
        grade: Int,
        limit: Int,
        langId: Long
    ): Single<List<WriteQuizComplexEntity>>

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetWriteQuizByRandomUseCase {
        override fun getRandomWriteQuiz(
            grade: Int,
            limit: Int,
            langId: Long
        ): Single<List<WriteQuizComplexEntity>> {
            return dbApi.getRandomWriteQuizList(grade, limit, langId)
        }
    }
}

