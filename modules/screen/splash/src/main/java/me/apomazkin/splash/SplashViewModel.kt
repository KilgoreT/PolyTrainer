package me.apomazkin.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

interface SplashUseCase {
    suspend fun checkIfLangIsInit(): Boolean
}

class SplashViewModel(
    private val splashUseCase: SplashUseCase
) : ViewModel() {
    suspend fun checkInitLaunch(): Boolean = !splashUseCase.checkIfLangIsInit()

    class Factory(
        private val splashUseCase: SplashUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SplashViewModel(splashUseCase) as T
        }
    }
}