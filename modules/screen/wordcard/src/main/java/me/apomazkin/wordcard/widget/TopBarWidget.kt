package me.apomazkin.wordcard.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBarWidget(
    onBackPress: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onBackPress.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier,
                    imageVector = Icons.Default.ArrowBack,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    contentDescription = ""
                )
            }
        },
        title = {
            Text(text = stringResource(id = R.string.word_card_toolbar_title))
        },
    )
}

@Composable
@PreviewWidgetRu
@PreviewWidgetEn
private fun Preview() {
    AppTheme {
        TopBarWidget {}
    }
}