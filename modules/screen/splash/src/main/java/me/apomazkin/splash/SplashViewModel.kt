package me.apomazkin.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface SplashUseCase {
    fun checkIfNeedAddDictionary(): Flow<Boolean>
}

class SplashViewModel @AssistedInject constructor(
    splashUseCase: SplashUseCase,
    @Assisted navigator: SplashNavigator,
) : ViewModel() {

    init {
        viewModelScope.launch {
            val isInitLaunch = splashUseCase.checkIfNeedAddDictionary().first()
            if (isInitLaunch) {
                navigator.openDictionarySetup()
            } else {
                navigator.openMainScreen()
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: SplashNavigator): SplashViewModel
    }
}
