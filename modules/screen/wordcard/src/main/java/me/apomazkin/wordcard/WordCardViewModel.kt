package me.apomazkin.wordcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.wordcard.deps.UiHost
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.wordcard.mate.DatasourceEffect
import me.apomazkin.wordcard.mate.DatasourceEffectHandler
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.UiEffectHandler
import me.apomazkin.wordcard.mate.WordCardReducer
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordCardSubHandler
import me.apomazkin.wordcard.mate.subscriptions

/**
 * VM карточки слова — точка сборки цикла mate. Навигация (базовый Back)
 * идёт через shared nav-handler приложения, per-экранного navigator'а нет.
 */
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted uiHost: UiHost,
    datasourceHandler: DatasourceEffectHandler,
    wordCardSubHandler: WordCardSubHandler,
    navigationHandler: MateNavigationHandler,
    uiEffectHandlerFactory: UiEffectHandler.Factory,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = viewModelScope,
        reducer = WordCardReducer(logger),
        effectHandlers = listOf(
            datasourceHandler,
            navigationHandler,
            uiEffectHandlerFactory.create(uiHost),
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(wordCardSubHandler),
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
