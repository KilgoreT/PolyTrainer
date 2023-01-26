package me.apomazkin.langpicker.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.apomazkin.langpicker.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.M3Black

fun LazyListScope.titleItemWidget() {
    item {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            text = stringResource(id = R.string.lang_selection_title),
            style = MaterialTheme.typography.titleLarge,
            color = M3Black,
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    locale = "Ru"
)
private fun Preview() {
    AppTheme {
        LazyColumn {
            titleItemWidget()
        }
    }
}