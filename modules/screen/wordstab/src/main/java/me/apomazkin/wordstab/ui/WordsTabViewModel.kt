package me.apomazkin.wordstab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.DatasourceEffectHandler
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.UiEffectHandler
import me.apomazkin.wordstab.logic.WordsTabReducer
import me.apomazkin.wordstab.logic.WordsTabSubHandler
import me.apomazkin.wordstab.logic.subscriptions
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * VM вкладки «Слова» — точка сборки цикла mate: reducer, обработчики
 * данных/UI и shared nav-handler приложения (навигационные эффекты экрана
 * резолвятся общей таблицей навигации, а не per-экранным navigator'ом).
 */
class WordsTabViewModel @AssistedInject constructor(
    logger: LexemeLogger,
    datasourceHandlerFactory: DatasourceEffectHandler.Factory,
    wordsTabSubHandler: WordsTabSubHandler,
    uiHandler: UiEffectHandler,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<WordsTabState, Msg> {

    private val stateHolder = Mate(
        initState = WordsTabState(),
        initEffects = setOf(DatasourceEffect.LoadTermFlow()),
        coroutineScope = viewModelScope,
        reducer = WordsTabReducer(logger = logger),
        effectHandlers = listOf(
            datasourceHandlerFactory.create(pagingScope = viewModelScope),
            uiHandler,
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(wordsTabSubHandler),
    )

    override val state: StateFlow<WordsTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): WordsTabViewModel
    }
}
