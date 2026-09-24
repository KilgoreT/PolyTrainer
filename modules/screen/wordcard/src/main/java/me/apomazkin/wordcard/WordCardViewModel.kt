package me.apomazkin.wordcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.wordcard.deps.UiHost
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.WordCardState

/**
 * VM карточки слова: тонкая обёртка над [WordCardAssembly] — сборка
 * раннера живёт там (общая с харнесом), VM даёт только viewModelScope
 * и продовые зависимости.
 */
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted uiHost: UiHost,
    useCase: WordCardUseCase,
    logger: LexemeLogger,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    private val stateHolder = WordCardAssembly.create(
        useCase = useCase,
        logger = logger,
        navigationHandler = navigationHandler,
        wordId = wordId,
        uiHost = uiHost,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<WordCardState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    override fun onCleared() {
        stateHolder.dispose()
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            wordId: Long,
            uiHost: UiHost,
        ): WordCardViewModel
    }
}
