package me.apomazkin.polytrainer.ui.screen.langSelection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.apomazkin.polytrainer.R
import me.apomazkin.polytrainer.ui.screen.langSelection.widget.ContinueButtonWidget
import me.apomazkin.polytrainer.ui.screen.langSelection.widget.languageListPreset
import me.apomazkin.polytrainer.ui.screen.langSelection.widget.listSubtitleWidget
import me.apomazkin.polytrainer.ui.screen.langSelection.widget.titleItemWidget
import me.apomazkin.polytrainer.ui.theme.AppTheme
import me.apomazkin.polytrainer.ui.widget.ImageRoundedWidget
import me.apomazkin.polytrainer.ui.widget.StatusBarColorWidget

@Composable
fun LanguageSelectionScreen(

) {
    val selectedLanguages = remember { mutableStateOf<String?>(null) }

    StatusBarColorWidget()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        ImageRoundedWidget(
            imageRes = R.drawable.image_lang_selection,
            bottomStart = 18,
            bottomEnd = 18,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1F),
        ) {
            titleItemWidget()
            languageListPreset(selected = selectedLanguages)
            listSubtitleWidget { }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ContinueButtonWidget(
            isEnable = selectedLanguages.value != null
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
@Preview(
    showBackground = true,
    locale = "Ru"
)
private fun Preview() {
    AppTheme {
        LanguageSelectionScreen()
    }
}