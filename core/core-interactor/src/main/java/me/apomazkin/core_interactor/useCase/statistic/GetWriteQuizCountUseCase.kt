package me.apomazkin.core_interactor.useCase.statistic

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface GetWriteQuizCountUseCase {
    fun getCount(tier: Int, langId: Long): Single<Int>

    class Impl @Inject constructor(
        private val api: CoreDbApi
    ) : GetWriteQuizCountUseCase {
        override fun getCount(tier: Int, langId: Long) = api.getWriteQuizCountByGrade(tier, langId)
    }

}