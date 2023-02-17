package me.apomazkin.main.widget

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.main.R
import me.apomazkin.theme.M3Black

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBarWidget() {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.item_title_vocabulary))
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = M3Black,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    )
}