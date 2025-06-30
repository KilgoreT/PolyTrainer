package me.apomazkin.dictionaryappbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionaryappbar.mate.DatasourceEffectHandler
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarReducer
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarState
import me.apomazkin.dictionaryappbar.mate.Msg
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.ui.logger.LexemeLogger

class DictionaryAppBarViewModel(
        logger: LexemeLogger,
        useCase: DictionaryAppBarUseCase,
) : ViewModel(), MateStateHolder<DictionaryAppBarState, Msg> {

    private val stateHolder = Mate(
            initState = DictionaryAppBarState(),
            initEffects = setOf(),
            coroutineScope = viewModelScope,
            reducer = DictionaryAppBarReducer(logger = logger),
            effectHandlerSet = setOf(
                    DatasourceEffectHandler(
                            useCase = useCase
                    )
            )
    )

    override val state: StateFlow<DictionaryAppBarState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
            private val useCase: DictionaryAppBarUseCase,
            private val logger: LexemeLogger,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DictionaryAppBarViewModel(
                    logger = logger,
                    useCase = useCase,
            ) as T
        }
    }
}
