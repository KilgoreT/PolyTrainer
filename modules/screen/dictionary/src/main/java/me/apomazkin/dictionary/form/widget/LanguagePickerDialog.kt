package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.form.LanguagePickerState
import me.apomazkin.dictionary.form.LanguageTarget
import me.apomazkin.dictionary.model.LanguageItem
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextTertiary

/**
 * IS525, минимальный вид: заголовок, поиск, список; текущий выбор —
 * акцентным цветом. Оформление (лист с вкладками) — вторым заходом.
 */
@Composable
internal fun LanguagePickerDialog(
    state: LanguagePickerState,
    onQueryChange: (String) -> Unit,
    onSelect: (LanguageItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val titleRes = when (state.target) {
        LanguageTarget.LEARNING -> R.string.dictionary_language_learning_title
        LanguageTarget.TRANSLATION -> R.string.dictionary_language_translation_title
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = formBackground,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = titleRes),
                    style = LexemeStyle.BodyM,
                    color = formTextTertiary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SearchPillWidget(
                    value = state.query,
                    onValueChange = onQueryChange,
                    hintRes = R.string.dictionary_language_search_hint,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(state.visibleLanguages, key = { it.tag }) { item ->
                        val isSelected = item.tag == state.selectedTag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item) }
                                .padding(vertical = 12.dp),
                        ) {
                            Text(
                                text = item.name,
                                style = LexemeStyle.BodyM,
                                color = if (isSelected) LexemeColor.primary else formTextTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}
