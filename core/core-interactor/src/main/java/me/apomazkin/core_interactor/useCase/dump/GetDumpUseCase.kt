package me.apomazkin.core_interactor.useCase.dump

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Dump
import javax.inject.Inject

interface GetDumpUseCase {
    fun getDump(): Single<Dump>
    fun restore(dump: Dump)

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetDumpUseCase {
        override fun getDump(): Single<Dump> {
            return dbApi.getDump()
        }

        override fun restore(dump: Dump) {
            dbApi.restoreDump(dump)
        }
    }
}

