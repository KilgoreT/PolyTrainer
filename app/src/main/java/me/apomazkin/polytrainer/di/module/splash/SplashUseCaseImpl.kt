package me.apomazkin.polytrainer.di.module.splash

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.splash.SplashUseCase
import javax.inject.Inject

class SplashUseCaseImpl @Inject constructor(
    private val langApi: CoreDbApi.LangApi
) : SplashUseCase {
    override fun checkIfNeedAddLang(): Flow<Boolean> {
        return langApi.flowLangList().transform {
            emit(!it.any())
        }
    }
}