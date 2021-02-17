package me.apomazkin.feature_statistic_impl.domain

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface NounCountUseCase {
    fun exec(): Single<Int>

    class NounCountUseCaseImpl @Inject constructor(private val api: CoreDbApi) : NounCountUseCase {
        override fun exec() = api.getDefinitionTypeCount("noun")
    }
}