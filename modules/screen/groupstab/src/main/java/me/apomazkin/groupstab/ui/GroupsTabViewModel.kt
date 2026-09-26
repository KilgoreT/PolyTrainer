package me.apomazkin.groupstab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.MateStore
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.groupstab.logic.GroupsTabState
import me.apomazkin.groupstab.logic.Msg
import me.apomazkin.logger.LexemeLogger

/**
 * VM вкладки «Группы»: тонкая обёртка над [GroupsTabAssembly] —
 * сборка раннера живёт там (общая с харнесом), VM даёт только
 * viewModelScope и продовые зависимости.
 */
class GroupsTabViewModel
    @AssistedInject
    constructor(
        useCase: GroupsTabUseCase,
        navigationHandler: MateNavigationHandler,
        logger: LexemeLogger,
    ) : ViewModel(),
        MateStore<GroupsTabState, Msg> {
        private val stateHolder = GroupsTabAssembly.create(
            useCase = useCase,
            logger = logger,
            navigationHandler = navigationHandler,
            coroutineScope = viewModelScope,
        )

        override val state: StateFlow<GroupsTabState>
            get() = stateHolder.state

        override fun accept(message: Msg) = stateHolder.accept(message)

        @AssistedFactory
        interface Factory {
            fun create(): GroupsTabViewModel
        }
    }
