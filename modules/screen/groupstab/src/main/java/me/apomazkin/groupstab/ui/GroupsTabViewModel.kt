package me.apomazkin.groupstab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.groupstab.logic.DatasourceEffectHandler
import me.apomazkin.groupstab.logic.GroupsSubHandler
import me.apomazkin.groupstab.logic.GroupsTabReducer
import me.apomazkin.groupstab.logic.GroupsTabState
import me.apomazkin.groupstab.logic.Msg
import me.apomazkin.groupstab.logic.subscriptions
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * VM вкладки «Группы» — точка сборки цикла mate: reducer, исполнитель
 * мутаций ([DatasourceEffectHandler]), shared nav-handler приложения
 * (тап по слову → [GroupsNavigationEffect.OpenWordCard]) и
 * декларативные подписки (`subscriptions()` из state +
 * [GroupsSubHandler]) на viewModelScope.
 */
class GroupsTabViewModel @AssistedInject constructor(
    datasourceHandler: DatasourceEffectHandler,
    groupsSubHandler: GroupsSubHandler,
    navigationHandler: MateNavigationHandler,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<GroupsTabState, Msg> {

    private val stateHolder = Mate(
        initState = GroupsTabState(),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = GroupsTabReducer(logger = logger),
        effectHandlers = listOf(datasourceHandler, navigationHandler),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(groupsSubHandler),
    )

    override val state: StateFlow<GroupsTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): GroupsTabViewModel
    }
}
