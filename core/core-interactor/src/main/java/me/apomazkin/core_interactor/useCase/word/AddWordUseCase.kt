package me.apomazkin.core_interactor.useCase.word

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_interactor.LangGod
import javax.inject.Inject

interface AddWordUseCase {
    fun addWord(value: String): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : AddWordUseCase {
        override fun addWord(value: String): Completable {
            return dbApi.addWord(value, LangGod.langId)
        }
    }
}

