package me.apomazkin.core_interactor.useCase.definition

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import javax.inject.Inject

@Deprecated("NotUsed")
interface GetDefinitionListUseCase {
    fun getDefinitionList(wordId: Long): Single<List<Definition>>

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetDefinitionListUseCase {
        override fun getDefinitionList(wordId: Long): Single<List<Definition>> {
            return dbApi.getDefinitionListByWordId(wordId)
        }
    }
}

