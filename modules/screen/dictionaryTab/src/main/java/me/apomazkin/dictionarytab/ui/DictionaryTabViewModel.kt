package me.apomazkin.dictionarytab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.logic.DatasourceEffectHandler
import me.apomazkin.dictionarytab.logic.DictionaryTabState
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.dictionarytab.logic.UiEffectHandler
import me.apomazkin.dictionarytab.logic.VocabularyTabReducer
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.ui.logger.LexemeLogger

class DictionaryTabViewModel(
    dictionaryTabUseCase: DictionaryTabUseCase,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<DictionaryTabState, Msg> {

//    init {
    // TODO: Info
    //  нужно реагирование на изменение данных из других источников,
    //  поэтому нужно где-то подписаться на flow, при этом не нарушая принипов TEA.
    //  Кажется, что такой способ не является наглядным и очевидным:
    //  1. Возможно, стоит вынести подписку в Mate, который будет отвечать за подписку на flow.
    //  2. Оставить этот кейс(реагирование на данные из потока) за пределами Mate.
    //  Какие еще есть размышления:
    //  1. эта подписка является по сути аналогом initEffects,
    //  то есть данный кейс нужно убрать из initEffects.
    //  2. не все initEffects по сути должны быть результатом данных из потока, или должны?
    //  потому что init данные могут меняться на других экранах и нужно реагировать на эти изменения.
    //  Т.е. init данные могут иметь сайд эффекты с других фич,
    //      а значит управление через initEffects уже не достигает цели.
    //  А если init данные не имеют сайд эффектов, то initEffect - решает задачу.

    // TODO: подписка здесь не решила задачу, так как когда Composable функция в фоне,
    //  в ее вьюмодели не работает подписка на флоу. Я хз почему.
    // Поэтому пока прокидываю жизненные события как MSG


//        viewModelScope.launch {
//            vocabularyUseCase
//                .flowAvailableLang()
//                .collectLatest { langList ->
//                    Log.d("###", "!!!!! => AvailableLang: $langList")
//                    accept(TopBarActionMsg.AvailableLang(langList))
//                }
//        }
//    }

    private val stateHolder = Mate(
        initState = DictionaryTabState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = VocabularyTabReducer(logger = logger),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(
                    dictionaryTabUseCase = dictionaryTabUseCase,
                    scope = viewModelScope,
            ),
            UiEffectHandler()
        )
    )
    
    override val state: StateFlow<DictionaryTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
        private val dictionaryTabUseCase: DictionaryTabUseCase,
        private val logger: LexemeLogger,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DictionaryTabViewModel(dictionaryTabUseCase, logger) as T
        }
    }
}
