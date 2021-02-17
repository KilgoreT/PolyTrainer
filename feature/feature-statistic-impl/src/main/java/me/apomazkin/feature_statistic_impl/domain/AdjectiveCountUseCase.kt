package me.apomazkin.feature_statistic_impl.domain

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface AdjectiveCountUseCase {
    fun exec(): Single<Int>

    class AdjectiveCountUseCaseImpl @Inject constructor(private val api: CoreDbApi) :
        AdjectiveCountUseCase {
        override fun exec() = api.getDefinitionTypeCount("adjective")
    }

}