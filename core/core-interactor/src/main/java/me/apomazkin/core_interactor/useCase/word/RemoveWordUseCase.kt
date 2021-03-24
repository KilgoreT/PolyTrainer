package me.apomazkin.core_interactor.useCase.word

import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface RemoveWordUseCase {
    fun removeWord(id: Long)

    class RemoveWordUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : RemoveWordUseCase {
        override fun removeWord(id: Long) {
            dbApi.removeWord(id)
        }
    }
}

