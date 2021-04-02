package me.apomazkin.core_interactor.useCase.definition

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import javax.inject.Inject

interface AddDefinitionUseCase {
    fun addDefinition(definition: Definition): Completable

    class AddDefinitionUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : AddDefinitionUseCase {
        override fun addDefinition(definition: Definition): Completable {
            return dbApi.addDefinition(definition)
        }
    }
}

