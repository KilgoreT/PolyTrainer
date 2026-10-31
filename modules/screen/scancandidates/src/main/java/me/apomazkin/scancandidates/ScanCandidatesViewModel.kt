package me.apomazkin.scancandidates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.MateStore
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.scancandidates.deps.ScanCandidatesUseCase
import me.apomazkin.scancandidates.mate.Msg
import me.apomazkin.scancandidates.mate.ScanCandidatesState

/**
 * VM экрана лексем-кандидатов: тонкая обёртка над [ScanCandidatesAssembly] —
 * сборка цикла живёт там, VM даёт `viewModelScope` и продовые зависимости. Цикл
 * живёт один снимок: VM очищена — эффекты и ответы им умерли.
 */
class ScanCandidatesViewModel
    @AssistedInject
    constructor(
        @Assisted uri: String,
        useCase: ScanCandidatesUseCase,
        logger: LexemeLogger,
        navigationHandler: MateNavigationHandler,
    ) : ViewModel(),
        MateStore<ScanCandidatesState, Msg> {
        private val stateHolder = ScanCandidatesAssembly.create(
            useCase = useCase,
            logger = logger,
            navigationHandler = navigationHandler,
            uri = uri,
            coroutineScope = viewModelScope,
        )

        override val state: StateFlow<ScanCandidatesState>
            get() = stateHolder.state

        override fun accept(message: Msg) = stateHolder.accept(message)

        override fun onCleared() {
            stateHolder.dispose()
            super.onCleared()
        }

        @AssistedFactory
        interface Factory {
            fun create(uri: String): ScanCandidatesViewModel
        }
    }
