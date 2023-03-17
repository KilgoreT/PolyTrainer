package me.apomazkin.core_interactor.useCase.definition

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import javax.inject.Inject

interface UpdateDefinitionUseCase {
    fun updateDefinition(definition: Definition): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : UpdateDefinitionUseCase {
        override fun updateDefinition(definition: Definition): Completable {
            return dbApi.updateLexemeDefinition(definition)
        }
    }
}

