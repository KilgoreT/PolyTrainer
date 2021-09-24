package me.apomazkin.core_interactor.useCase.word

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface RemoveWordUseCase {
    fun removeWord(id: Long): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : RemoveWordUseCase {
        override fun removeWord(id: Long): Completable {
            return dbApi.removeWord(id)
        }
    }
}

