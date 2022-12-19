package me.apomazkin.polytrainer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import me.apomazkin.polytrainer.R
import me.apomazkin.polytrainer.ui.theme.AppTheme
import me.apomazkin.polytrainer.ui.widget.ImageTitled

@Composable
fun SplashScreen(
    onExit: (isInitLaunch: Boolean) -> Unit
) {
    val systemUiController = rememberSystemUiController()
    val barColor = MaterialTheme.colorScheme.background

    LaunchedEffect(Unit) {
        delay(5000)
        onExit.invoke(true)
    }

    DisposableEffect(Unit) {
        systemUiController.setSystemBarsColor(
            color = barColor
        )
        onDispose {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        ImageTitled(
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