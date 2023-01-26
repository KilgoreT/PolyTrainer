package me.apomazkin.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageTitledWidget
import me.apomazkin.ui.StatusBarColorWidget

@Composable
fun SplashScreen(
    splashUseCase: SplashUseCase,
    viewModel: SplashViewModel = viewModel(
        factory = SplashViewModel.Factory(splashUseCase)
    ),
    onExit: (isInitLaunch: Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        delay(600)
        val isInitLaunch = viewModel.checkInitLaunch()
        onExit.invoke(isInitLaunch)
    }

    StatusBarColorWidget(color = MaterialTheme.colorScheme.background)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        ImageTitledWidget(
            imageRes = R.drawable.ic_splash_logo,
            titleRes = R.string.logo_title
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun Preview() {
    AppTheme {
        SplashScreen(
            splashUseCase = object : SplashUseCase {
                override suspend fun checkIfLangIsInit(): Boolean = false
            }
        ) {}
    }
}