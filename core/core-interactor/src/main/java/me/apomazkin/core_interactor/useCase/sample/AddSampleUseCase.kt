package me.apomazkin.core_interactor.useCase.sample

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface AddSampleUseCase {
    fun addSample(definitionId: Long, value: String, source: String?): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : AddSampleUseCase {
        override fun addSample(definitionId: Long, value: String, source: String?): Completable {
            return dbApi.addSample(definitionId, value, source)
        }
    }
}

