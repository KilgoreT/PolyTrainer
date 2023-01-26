package me.apomazkin.polytrainer.di.module.splash

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.splash.SplashUseCase
import javax.inject.Inject

class SplashUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi
) : SplashUseCase {
    override suspend fun checkIfLangIsInit(): Boolean =
        dbApi.getLangSuspend().any()
}