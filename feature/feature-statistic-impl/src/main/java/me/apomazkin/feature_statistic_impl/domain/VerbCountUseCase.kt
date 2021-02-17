package me.apomazkin.feature_statistic_impl.domain

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface VerbCountUseCase {
    fun exec(): Single<Int>

    class VerbCountUseCaseImpl @Inject constructor(private val api: CoreDbApi) : VerbCountUseCase {
        override fun exec() = api.getDefinitionTypeCount("verb")
    }
}