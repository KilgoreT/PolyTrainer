package me.apomazkin.core_interactor.useCase.statistic

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface GetDefinitionCountUseCase {
    fun exec(): Single<Int>

    class Impl @Inject constructor(
        private val api: CoreDbApi
    ) : GetDefinitionCountUseCase {
        override fun exec() = api.getDefinitionCount()
    }
}