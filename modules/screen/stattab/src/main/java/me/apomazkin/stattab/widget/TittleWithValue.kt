package me.apomazkin.stattab.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.stattab.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun TitleWithValue(
        @StringRes titleResId: Int,
        value: String,
) {
    Row(
            modifier = Modifier
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
                text = stringResource(id = titleResId),
                style = LexemeStyle.BodyL.copy(
                        color = LexemeColor.secondary,
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
                text = value,
                style = LexemeStyle.BodyL.copy(
                        color = LexemeColor.secondary,
                )
        )
    }
}


@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        TitleWithValue(
                titleResId = R.string.logo_title,
                value = "127",
        )
    }
}