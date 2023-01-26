package me.apomazkin.polytrainer.ui.screen.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import me.apomazkin.polytrainer.R
import me.apomazkin.polytrainer.ui.theme.AppTheme
import me.apomazkin.polytrainer.ui.widget.ImageTitledWidget
import me.apomazkin.polytrainer.ui.widget.StatusBarColorWidget

@Composable
fun SplashScreen(
    onExit: (isInitLaunch: Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        delay(5000)
        onExit.invoke(true)
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
        SplashScreen {}
    }
}