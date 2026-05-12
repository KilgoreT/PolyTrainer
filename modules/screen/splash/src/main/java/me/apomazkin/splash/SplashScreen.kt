package me.apomazkin.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.di.viewModelFactory
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.preview.PreviewWidget

private val colorBackground = LexemeColor.primary

@Composable
fun SplashScreen(
    factory: SplashViewModel.Factory,
    navigator: SplashNavigator,
    @Suppress("UNUSED_PARAMETER")
    viewModel: SplashViewModel = viewModel(
        factory = viewModelFactory { factory.create(navigator) },
    ),
) {
    SystemBarsWidget(
        color = colorBackground,
        statusBarDarkIcon = false,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorBackground),
        contentAlignment = Alignment.Center,
    ) {
//        ImageTitledWidget(
//            imageRes = R.drawable.ic_logo,
//            titleRes = R.string.logo_title,
//            textStyle = MaterialTheme.typography.titleLarge,
//            textColor = MaterialTheme.colorScheme.onPrimary,
//        )
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorBackground),
            contentAlignment = Alignment.Center,
        ) {}
    }
}
