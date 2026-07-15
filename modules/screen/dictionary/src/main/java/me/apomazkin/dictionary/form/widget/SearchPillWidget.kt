package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextHint
import me.apomazkin.theme.searchPillColor
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Капсула поиска по флагам (Figma 5027:1135-1139): подложка searchPillColor,
 * radius 14, лупа + hint + clear при непустом вводе.
 */
@Composable
internal fun SearchPillWidget(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp),
        shape = RoundedCornerShape(14.dp),
        color = searchPillColor,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = formTextHint,
                modifier = Modifier.size(19.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.dictionary_filter_flags_hint),
                        style = LexemeStyle.BodyM,
                        color = formTextHint,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LexemeStyle.BodyM.copy(color = LexemeColor.secondary),
                    cursorBrush = SolidColor(LexemeColor.primary),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = formTextHint,
                    )
                }
            }
        }
    }
}

@Composable
@PreviewWidget
private fun PreviewEmpty() {
    AppTheme {
        SearchPillWidget(
            value = "",
            onValueChange = {},
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewFilled() {
    AppTheme {
        SearchPillWidget(
            value = "Англ",
            onValueChange = {},
        )
    }
}
