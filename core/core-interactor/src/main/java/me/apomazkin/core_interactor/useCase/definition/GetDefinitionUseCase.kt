package me.apomazkin.core_interactor.useCase.definition

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import javax.inject.Inject

interface GetDefinitionUseCase {
    fun getDefinition(id: Long): Single<Definition>
    fun getDefinition(): Single<List<Definition>>

    class GetDefinitionUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetDefinitionUseCase {
        override fun getDefinition(id: Long): Single<Definition> {
            return dbApi.getDefinition(id)
                .onErrorResumeNext { ttt ->
                    Single.error(RuntimeException("With defID: $id : ${ttt.message}"))
                }
        }

        override fun getDefinition(): Single<List<Definition>> {
            return dbApi.getDefinitionAll()
        }
    }
}

