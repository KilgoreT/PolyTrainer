package me.apomazkin.createdictionary.widget

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun BoxScope.LoadingWidget() {
    CircularProgressIndicator(
        modifier = Modifier
            .align(Alignment.Center)
    )
}