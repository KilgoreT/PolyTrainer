package me.apomazkin.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*

interface SplashUseCase {
    fun checkIfNeedAddLang(): Flow<Boolean>
}

class SplashViewModel(
    splashUseCase: SplashUseCase
) : ViewModel() {

    val checkIfNeedAddLang: StateFlow<Boolean?> = splashUseCase
        .checkIfNeedAddLang()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    class Factory(
        private val splashUseCase: SplashUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SplashViewModel(splashUseCase) as T
        }
    }
}