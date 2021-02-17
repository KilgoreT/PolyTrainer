package me.apomazkin.feature_statistic_impl.domain

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface WordCountUseCase {
    fun exec(): Single<Int>

    class WordCountUseCaseImpl @Inject constructor(private val api: CoreDbApi) : WordCountUseCase {
        override fun exec() = api.wordCount()
    }
}