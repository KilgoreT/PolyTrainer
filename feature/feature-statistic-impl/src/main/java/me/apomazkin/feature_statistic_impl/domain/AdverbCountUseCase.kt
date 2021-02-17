package me.apomazkin.feature_statistic_impl.domain

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface AdverbCountUseCase {
    fun exec(): Single<Int>

    // TODO: 18.02.2021 "adverb" to const
    class AdverbCountUseCaseImpl @Inject constructor(private val api: CoreDbApi) :
        AdverbCountUseCase {
        override fun exec() = api.getDefinitionTypeCount("adverb")
    }

}