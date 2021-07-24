package me.apomazkin.core_interactor.useCase.sample

import io.reactivex.Observable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Sample
import javax.inject.Inject

interface GetSampleUseCase {
    fun getSampleList(): Observable<List<Sample>>

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetSampleUseCase {
        override fun getSampleList(): Observable<List<Sample>> {
            return dbApi.getSampleList()
        }
    }
}

