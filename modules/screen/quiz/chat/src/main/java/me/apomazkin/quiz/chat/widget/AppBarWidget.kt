@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.quiz.chat.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.enableIconColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun AppBarWidget(
    onBackPress: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconBoxed(
                iconRes = R.drawable.ic_back,
                enabled = true,
                colorEnabled = enableIconColor,
                size = 44,
                onClick = onBackPress,
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.chat_quiz_app_bar_title),
                style = LexemeStyle.H5,
            )
        },
    )
}

@PreviewWidget
@Composable
fun AppBarWidgetPreview() {
    AppBarWidget {}
}
