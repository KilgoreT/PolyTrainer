package me.apomazkin.core_interactor.useCase.definition

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface RemoveDefinitionUseCase {
    fun removeDefinition(id: Long): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : RemoveDefinitionUseCase {
        override fun removeDefinition(id: Long): Completable {
            return dbApi.removeDefinition(id)
        }
    }
}

