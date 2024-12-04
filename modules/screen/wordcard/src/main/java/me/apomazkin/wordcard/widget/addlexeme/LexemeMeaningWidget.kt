package me.apomazkin.wordcard.widget.addlexeme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.R

@Composable
internal fun LexemeMeaningWidget(
    @StringRes titleRes: Int,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = stringResource(id = titleRes),
            style = LexemeStyle.BodyL,
            color = MaterialTheme.colorScheme.secondary,
        )
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.onSecondary)
                .size(24.dp),
        ) {
            Checkbox(
                checked = isChecked,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    uncheckedColor = Color.Transparent,
                    checkmarkColor = whiteColor
                ),
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
        ) {
            LexemeMeaningWidget(
                titleRes = R.string.menu_item_title_add_dict,
                isChecked = enabled,
                onCheckedChange = {},
            )
        }
    }
}