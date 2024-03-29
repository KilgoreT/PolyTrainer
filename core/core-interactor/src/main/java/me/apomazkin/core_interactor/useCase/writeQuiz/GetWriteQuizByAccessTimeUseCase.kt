package me.apomazkin.core_interactor.useCase.writeQuiz

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WriteQuiz
import javax.inject.Inject

interface GetWriteQuizByAccessTimeUseCase {
    fun getWriteQuizList(langId: Long): Single<List<WriteQuiz>>
    fun getWriteQuizByAccessTime(grade: Int, limit: Int, langId: Long): Single<List<WriteQuiz>>

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetWriteQuizByAccessTimeUseCase {
        override fun getWriteQuizList(langId: Long): Single<List<WriteQuiz>> {
            return dbApi.getWriteQuizList(langId)
        }

        override fun getWriteQuizByAccessTime(
            grade: Int,
            limit: Int,
            langId: Long
        ): Single<List<WriteQuiz>> {
            return dbApi.getWriteQuizListByAccessTime(grade, limit, langId)
        }
    }
}