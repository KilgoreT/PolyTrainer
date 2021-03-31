package me.apomazkin.core_interactor.useCase.statistic

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface GetWordCountUseCase {
    fun exec(): Single<Int>

    class GetWordCountUseCaseImpl @Inject constructor(
        private val api: CoreDbApi
    ) : GetWordCountUseCase {
        override fun exec() = api.wordCount()
    }
}