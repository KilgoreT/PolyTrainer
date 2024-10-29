@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.wordcard.widget

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.enableIconColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.btn.PrimaryTextButtonWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun TopBarWidget(
    onBackPress: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconBoxed(
                iconRes = R.drawable.ic_back,
                enabled = true,
                colorEnabled = enableIconColor,
                size = 44,
            ) { onBackPress.invoke() }
        },
        title = {},
        actions = {
            PrimaryTextButtonWidget(
                title = R.string.word_card_save_title,
                enabled = true,
            ) {}
            IconBoxed(
                iconRes = R.drawable.ic_more,
                enabled = true,
                colorEnabled = enableIconColor,
                size = 44,
            ) {}
        }
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        TopBarWidget {}
    }
}