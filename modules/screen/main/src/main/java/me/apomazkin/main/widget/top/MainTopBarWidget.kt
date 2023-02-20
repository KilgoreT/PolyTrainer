package me.apomazkin.main.widget.top

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.M3Black
import me.apomazkin.ui.preview.PreviewWidget

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLifecycleComposeApi::class)
@Composable
fun MainTopBarWidget(
    onAddLang: () -> Unit,
    navController: NavHostController,
    mainTopBarUseCase: MainTopBarUseCase,
    viewModel: MainTopBarViewModel = viewModel(
        factory = MainTopBarViewModel.Factory(
            navController.currentBackStackEntryFlow,
            mainTopBarUseCase
        )
    )
) {
    val state: TabUiState by viewModel.state.collectAsStateWithLifecycle()

    TopAppBar(
        title = {
            Text(text = stringResource(id = state.titleRes))
        },
        actions = {
            state.actionState.forEach {
                when (it) {
                    is ActionUiState.LangActionUiState -> {
                        LangActionWidget(
                            iconRes = it.currentLang.iconRes,
                            langList = it.langList,
                            onChangeLang = { numericCode -> viewModel.changeLang(numericCode) },
                            onAddLang = onAddLang
                        )
                    }
                    ActionUiState.SearchActionUiState -> {}
                }
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = M3Black,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        MainTopBarWidget(
            navController = rememberNavController(),
            mainTopBarUseCase = PreviewMainTopBarUseCase(),
            onAddLang = {},
        )
    }
}