package me.apomazkin.polytrainer.di.module.splash

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.splash.SplashUseCase
import javax.inject.Inject

class SplashUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi
) : SplashUseCase {
    override fun checkIfNeedAddDictionary(): Flow<Boolean> {
        return dictionaryApi.flowDictionaryList().transform {
            emit(!it.any())
        }
    }
}