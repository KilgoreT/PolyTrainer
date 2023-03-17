package me.apomazkin.vocabulary.ui.widget.topBar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.R

@Composable
fun ItemLangMenuWidget(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                ImageFlagWidget(
                    flagRes = iconRes,
                    contentDescription = title
                )
            }
        },
        text = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        onClick = onClick
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Column {
            ItemLangMenuWidget(
                iconRes = R.drawable.example_ic_flag_gb,
                title = "Бритишь",
            ) {}
        }
    }
}