package me.apomazkin.core_interactor.useCase.statistic

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface GetWordClassCountUseCase {
    fun getCount(wordClass: String): Single<Int>

    class GetWordClassCountUseCaseImpl @Inject constructor(
        private val api: CoreDbApi
    ) : GetWordClassCountUseCase {
        override fun getCount(wordClass: String) = api.getDefinitionTypeCount(wordClass)
    }

}