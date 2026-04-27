@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.form.LanguagePickerState
import me.apomazkin.dictionary.model.LanguageItem
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun LanguagePickerBottomSheet(
    state: LanguagePickerState,
    onQueryChange: (String) -> Unit,
    onSelect: (LanguageItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = whiteColor,
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = LexemeStyle.BodyL,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                items(state.filteredLanguages) { language ->
                    Text(
                        text = language.displayName,
                        style = LexemeStyle.BodyL,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        LanguagePickerBottomSheet(
            state = LanguagePickerState(
                show = true,
                filteredLanguages = listOf(
                    LanguageItem("en", "English"),
                    LanguageItem("de", "German"),
                ),
            ),
            onQueryChange = {},
            onSelect = {},
            onDismiss = {},
        )
    }
}
