package me.apomazkin.core_interactor.useCase.word

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Word
import javax.inject.Inject

interface UpdateWordUseCase {
    fun updateWord(word: Word): Completable

    class UpdateWordUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : UpdateWordUseCase {
        override fun updateWord(word: Word): Completable {
            return dbApi.updateWord(word)
        }
    }
}

