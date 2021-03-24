package me.apomazkin.core_interactor.useCase.word

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Word
import javax.inject.Inject

interface GetWordUseCase {
    fun getWord(id: Long): Single<Word>

    class GetWordUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetWordUseCase {
        override fun getWord(id: Long): Single<Word> {
            return dbApi.getWord(id)
        }
    }
}

