package me.apomazkin.core_interactor.useCase.language

import io.reactivex.Completable
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

interface AddLanguageUseCase {
    fun addLanguage(code: String, name: String): Completable

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : AddLanguageUseCase {
        override fun addLanguage(code: String, name: String): Completable {
            return dbApi.addLang(code, name)
        }
    }
}

