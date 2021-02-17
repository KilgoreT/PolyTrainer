package me.apomazkin.feature_statistic_impl.domain

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface DefinitionCountUseCase {
    fun exec(): Single<Int>

    class DefinitionCountUseCaseImpl @Inject constructor(private val api: CoreDbApi) :
        DefinitionCountUseCase {
        override fun exec() = api.getDefinitionCount()
    }
}