package me.apomazkin.core_interactor.useCase.language

import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Language
import javax.inject.Inject

interface GetLanguageUseCase {
    fun getAllLanguage(): Single<List<Language>>

    class Impl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetLanguageUseCase {
        override fun getAllLanguage(): Single<List<Language>> {
            return dbApi.getLang()
        }

    }
}

