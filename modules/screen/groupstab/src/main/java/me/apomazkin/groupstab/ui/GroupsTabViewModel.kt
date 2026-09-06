package me.apomazkin.groupstab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.groupstab.logic.DatasourceEffectHandler
import me.apomazkin.groupstab.logic.GroupsTabReducer
import me.apomazkin.groupstab.logic.GroupsTabState
import me.apomazkin.groupstab.logic.Msg
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder

/**
 * IS493 Э2/Э3: VM вкладки «Группы». Навигация (тап → карточка) идёт напрямую
 * из UI через [GroupsNavigator] — эффектов навигации нет (read-only вкладка).
 */
class GroupsTabViewModel @AssistedInject constructor(
    datasourceHandler: DatasourceEffectHandler,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<GroupsTabState, Msg> {

    private val stateHolder = Mate(
        initState = GroupsTabState(),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = GroupsTabReducer(logger = logger),
        effectHandlers = listOf(datasourceHandler),
        flowHandlers = listOf(datasourceHandler),
    )

    override val state: StateFlow<GroupsTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): GroupsTabViewModel
    }
}
