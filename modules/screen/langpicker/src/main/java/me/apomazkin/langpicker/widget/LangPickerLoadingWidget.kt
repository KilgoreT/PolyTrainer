package me.apomazkin.langpicker.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LangPickerLoadingWidget(

) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Magenta)
    )
}