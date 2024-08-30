@file:OptIn(ExperimentalLifecycleComposeApi::class)

package me.apomazkin.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageTitledWidget
import me.apomazkin.ui.StatusBarColorWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun SplashScreen(
    splashUseCase: SplashUseCase,
    viewModel: SplashViewModel = viewModel(
        factory = SplashViewModel.Factory(splashUseCase)
    ),
    onExit: (isInitLaunch: Boolean) -> Unit
) {

    val checkIfNeedInitLang by viewModel.checkIfNeedAddLang.collectAsStateWithLifecycle()
    LaunchedEffect(checkIfNeedInitLang) {
        delay(600)
        checkIfNeedInitLang?.let {
            onExit.invoke(it)
        }
    }

    StatusBarColorWidget(
        color = MaterialTheme.colorScheme.primary,
        statusBarDarkIcon = false,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        ImageTitledWidget(
            imageRes = R.drawable.ic_logo,
            titleRes = R.string.logo_title,
            textStyle = MaterialTheme.typography.titleLarge,
            textColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        SplashScreen(
            splashUseCase = object : SplashUseCase {
                override fun checkIfNeedAddLang(): Flow<Boolean> = flowOf(false)
            }
        ) {}
    }
}