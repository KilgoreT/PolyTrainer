package me.apomazkin.main.widget.top

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.apomazkin.main.R
import me.apomazkin.main.TabPoint
import me.apomazkin.main.entity.LangUiEntity

class MainTopBarViewModel(
    navBackStackEntry: Flow<NavBackStackEntry>,
    private val mainTopBarUseCase: MainTopBarUseCase,
) : ViewModel() {

    val state: StateFlow<TabUiState> = combine(
        navBackStackEntry, mainTopBarUseCase.getCurrentLang(), mainTopBarUseCase.getAvailableLang()
    ) { tab, currentLang, availableLangList ->
        return@combine when (tab.destination.route) {
            TabPoint.VOCABULARY.route -> {
                val current = availableLangList
                    .first { it.numericCode == currentLang }
                val actionState = listOf(
                    ActionUiState.LangActionUiState(
                        currentLang = current,
                        langList = availableLangList
                            .filter { it.numericCode != currentLang }
                            .sortedBy { it.title },
                    )
                )
                return@combine TabUiState.Vocabulary(
                    actionState = actionState
                )
            }
            TabPoint.TRAINING.route -> {
                return@combine TabUiState.Training()
            }
            TabPoint.DASHBOARD.route -> {
                return@combine TabUiState.Dashboard()
            }
            else -> throw IllegalStateException("Tab name not found: $tab")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = TabUiState.Vocabulary()
    )

    fun changeLang(numericCode: Int) {
        viewModelScope.launch {
            mainTopBarUseCase.changeLang(numericCode)
        }
    }

    class Factory(
        private val navBackStackEntry: Flow<NavBackStackEntry>,
        private val mainTopBarUseCase: MainTopBarUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainTopBarViewModel(navBackStackEntry, mainTopBarUseCase) as T
        }
    }
}

sealed class TabUiState(
    @StringRes val titleRes: Int,
    val actionState: List<ActionUiState> = emptyList(),
) {
    class Vocabulary(
        actionState: List<ActionUiState> = emptyList(),
    ) : TabUiState(titleRes = R.string.item_title_vocabulary, actionState)

    class Training(
        actionState: List<ActionUiState> = emptyList(),
    ) : TabUiState(titleRes = R.string.item_title_training, actionState)

    class Dashboard(
        actionState: List<ActionUiState> = emptyList(),
    ) : TabUiState(titleRes = R.string.item_title_dashboard, actionState)
}

sealed interface ActionUiState {

    data class LangActionUiState(
        val currentLang: LangUiEntity,
        val langList: List<LangUiEntity> = emptyList(),
    ) : ActionUiState

    object SearchActionUiState : ActionUiState
}