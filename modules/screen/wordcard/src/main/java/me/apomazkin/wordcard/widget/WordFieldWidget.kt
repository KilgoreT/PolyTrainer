package me.apomazkin.wordcard.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.ui.text.base.LexemeEditableText
import me.apomazkin.wordcard.mate.WordState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Поле слова с inline-редактированием.
 *
 * @param loaded данные загруженного слова.
 * @param enabled false → ввод и переход в edit-mode заблокированы.
 */
@Composable
internal fun WordFieldWidget(
    loaded: WordState.Loaded,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCommit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                LexemeEditableText(
                    originValue = loaded.value,
                    changedValue = loaded.edited,
                    isEditMode = loaded.isEditMode,
                    textColor = blackColor,
                    textStyle = LexemeStyle.H5,
                    onTextChange = { if (enabled) onValueChange(it) },
                    onOpenEditMode = { if (enabled) onOpenEditMode() },
                    onFocusLost = { onCommit() },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.word_card_added_field),
                        style = LexemeStyle.BodyM,
                        color = grayTextColor,
                    )
                    Text(
                        text = getDate(date = loaded.added),
                        style = LexemeStyle.BodyMBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            ImageFlagWidget(
                flagRes = R.drawable.example_ic_flag_gb,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

private fun getDate(date: Date): String {
    val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    return dateFormat.format(date)
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.tertiary)
                .padding(12.dp)
        ) {
            WordFieldWidget(
                loaded = WordState.Loaded(
                    id = 1L,
                    added = Date(),
                    value = "apple",
                ),
                enabled = true,
                onValueChange = {},
                onOpenEditMode = {},
                onCommit = {},
            )
        }
    }
}
