@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.dictionary.widget

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.dictionary.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.enableIconColor
import me.apomazkin.ui.IconBoxed

@Composable
fun DictionaryAppBar(
    onBackPress: () -> Unit,
    @StringRes titleResId: Int = R.string.dictionary_selection_title,
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
                text = stringResource(id = titleResId),
                style = LexemeStyle.H5,
            )
        }
    )
}
